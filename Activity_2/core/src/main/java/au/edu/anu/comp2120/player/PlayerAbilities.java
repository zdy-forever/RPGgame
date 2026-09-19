package au.edu.anu.comp2120.player;

import au.edu.anu.comp2120.combat.Attack;
import au.edu.anu.comp2120.combat.AttackSpec;
import au.edu.anu.comp2120.combat.CombatSystem;
import au.edu.anu.comp2120.combat.Combatant;
import au.edu.anu.comp2120.combat.Faction;
import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.GameMap;
import au.edu.anu.comp2120.world.MapType;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Objects;
import java.util.Optional;
import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import au.edu.anu.comp2120.combat.ElementType;
import au.edu.anu.comp2120.combat.ProjectileSpec;

/**
 * Common ability execution, slot cooldowns and collision-safe dash, independent of input/rendering.
 * The owner calls update once, advances the shared CombatSystem once for all actors, then calls
 * afterCombat before encounter outcome coordination to settle deferred skill responses.
 * Slot cooldowns replace the pipeline's per-source delay for these attacks: submissions use zero
 * source delay, preserving one canonical player receiver and all existing hit/death rules.
 * Use one controller per player, called only on the game-loop thread.
 * @author Daoyan Zhu (u7782042)
 */
public final class PlayerAbilities {
    /**
     * Reads horizontal facing for presentation; artwork never controls aim.
     * @return -1, 0 or 1
     */
    public int getFacingX() { return facingX; }
    /**
     * Reads vertical facing for presentation.
     * @return -1, 0 or 1
     */
    public int getFacingY() { return facingY; }
    /**
     * Observes a fixed cast/field area between finite damage ticks.
     * @return immutable skill geometry while casting or a field is active
     */
    public Optional<au.edu.anu.comp2120.world.CollisionBox> getSkillFieldBounds() {
        return skills.fieldBounds();
    }
    /** Existing player; owns position, collision environment and health. */
    private final Player player;
    /** Shared arena pipeline; this controller never directly damages a target. */
    private final CombatSystem combat;
    /** Production encounter policy; isolated ability demonstrations may select anywhere. */
    private final boolean hubOnlySelection;
    /** Bounded stateful executions composed with the common input/cooldown/movement controller. */
    private final ClassSkills skills;
    /** Remaining per-slot seconds, measured from successful triggering. */
    private final double[] cooldowns = new double[AbilitySlot.values().length];
    /** Short projectile launch windows, separate from longer flight lifetimes and slot cooldowns. */
    private final double[] launchWindows = new double[AbilitySlot.values().length];
    /** Last area invocation or leading volley member; flight lifecycle is owned by CombatSystem. */
    private final EnumMap<AbilitySlot, Attack> attacks = new EnumMap<>(AbilitySlot.class);
    /** Current canonical player adapter, replaced by the existing map coordinator. */
    private Combatant source;
    /** Horizontal inset from the player's movement body to its independent hurtbox. */
    private double insetX;
    /** Vertical inset from the player's movement body to its independent hurtbox. */
    private double insetY;
    /** Preserved hurtbox width, independent of placeholder rendering. */
    private double hurtWidth;
    /** Preserved hurtbox height, independent of placeholder rendering. */
    private double hurtHeight;
    /** Last horizontal intent; facing right is the initial default. */
    private int facingX = 1;
    /** Last vertical intent. */
    private int facingY;
    /** Normalized horizontal direction captured when a dash starts. */
    private double dashX;
    /** Normalized vertical direction captured when a dash starts. */
    private double dashY;
    /** Remaining dash movement time; a wall may shorten distance but not refund cooldown. */
    private double dashRemaining;

    /**
     * Uses the class definition already selected when the shared Player was constructed.
     * @param player existing shared player
     * @param source registered canonical player adapter
     * @param combat shared combat pipeline
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if source is not this player's registered PLAYER adapter or its world conflicts
     */
    public PlayerAbilities(Player player, Combatant source, CombatSystem combat) {
        this(player, source, combat, Objects.requireNonNull(player, "player").getClassDefinition().abilities());
    }

    /**
     * Attaches abilities and overrides initial tuning while preserving the player's class identity.
     * Retained for code-configured loadouts; use selectClass for changes to a running controller.
     * @param player existing player model
     * @param source registered player combat adapter
     * @param combat shared combat pipeline
     * @param loadout validated common ability tuning
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if source is not this player's registered PLAYER adapter or its world conflicts
     */
    public PlayerAbilities(Player player, Combatant source, CombatSystem combat, AbilityLoadout loadout) {
        this(player, source, combat, loadout, false);
    }

    /**
     * Adds a production selection policy without coupling ability execution to Quest state.
     * Definition changes pass through selectClass; A/B choices use Hub-only selectUltimateMode.
     * @param player existing shared player
     * @param source canonical registered adapter
     * @param combat shared pipeline
     * @param loadout initial tuning
     * @param hubOnlySelection whether runtime class/loadout selection requires a Hub map
     * @throws NullPointerException if required data is null
     * @throws IllegalArgumentException if source registration or world binding is invalid
     */
    public PlayerAbilities(Player player, Combatant source, CombatSystem combat,
            AbilityLoadout loadout, boolean hubOnlySelection) {
        this(player, source, combat, loadout, hubOnlySelection, new Random());
    }

    /**
     * Allows deterministic independent projectile elements without a global random source.
     * @param player persistent owner
     * @param source canonical adapter
     * @param combat shared pipeline
     * @param loadout initial tuning
     * @param hubOnlySelection whether class/loadout changes require Hub
     * @param random per-controller random source; supply a fixed seed in tests
     * @throws NullPointerException if required data is null
     * @throws IllegalArgumentException if registration/world binding is invalid
     */
    public PlayerAbilities(Player player, Combatant source, CombatSystem combat,
            AbilityLoadout loadout, boolean hubOnlySelection, Random random) {
        this.player = Objects.requireNonNull(player, "player");
        this.combat = Objects.requireNonNull(combat, "combat");
        this.hubOnlySelection = hubOnlySelection;
        skills = new ClassSkills(player, combat, Objects.requireNonNull(random, "random"));
        Objects.requireNonNull(loadout, "loadout");
        bindAfterTransition(source);
        ClassDefinition previous = player.getClassDefinition();
        player.applyClassDefinition(new ClassDefinition(player.getPlayerClass(), loadout,
                loadout.equals(previous.abilities()) ? previous.alternateUltimate() : loadout.ultimate()));
    }

    /**
     * Selects the current class's remembered O mode only in Hub, including debug/test controllers.
     * Map transitions and recovery never call this method or reset the stored choices.
     * @param mode requested A/B mode
     * @return true if changed; false outside Hub, while unavailable/busy or already selected
     * @throws NullPointerException if mode is null
     */
    public boolean selectUltimateMode(UltimateMode mode) {
        Objects.requireNonNull(mode, "mode");
        if (!(player.getCollisionArea() instanceof GameMap map) || map.getType() != MapType.HUB
                || !available() || skills.busy() || dashRemaining > 0
                || combat.getActiveAttacks().stream().anyMatch(attack -> attack.getSource() == source)
                || Arrays.stream(launchWindows).anyMatch(remaining -> remaining > 0)
                || mode == player.getUltimateMode()) {
            return false;
        }
        player.applyUltimateMode(mode);
        return true;
    }

    /**
     * Completes one submitted frame after the shared combat update, before encounter outcome handling.
     * Observes effective cast damage and executes deferred defensive responses outside callbacks.
     * Repeated calls without another update are harmless.
     */
    public void afterCombat() {
        skills.afterCombat();
    }

    /**
     * Exposes stateful execution for placeholder diagnostics and tests.
     * @return phase name, independent of rendering
     */
    public String getSkillStatus() {
        return skills.status();
    }

    /**
     * Selects a definition without replacing Player, Health, combat registration or cooldown clocks.
     * Same-definition, dead/stale-source, active-attack and active-dash requests are rejected.
     * Production controllers additionally require Hub for any class or O/loadout change.
     * Projectile launch windows and any still-flying arrow also prevent class switching.
     * Remaining cooldown seconds, facing, position, movement speed and HP are preserved exactly.
     * This is a debug/code selection foundation, not a class-unlock or selection-UI workflow.
     * @param definition validated next class definition
     * @return true if applied; false if unchanged or currently unsafe to switch
     * @throws NullPointerException if definition is null
     */
    public boolean selectClass(ClassDefinition definition) {
        Objects.requireNonNull(definition, "class definition");
        if (hubOnlySelection && (!(player.getCollisionArea() instanceof GameMap map)
                || map.getType() != MapType.HUB)) {
            return false;
        }
        if (definition.equals(player.getClassDefinition()) || !available() || skills.busy() || dashRemaining > 0
                || Arrays.stream(launchWindows).anyMatch(remaining -> remaining > 0)
                || combat.getActiveAttacks().stream().anyMatch(attack -> attack.getSource() == source)) {
            return false;
        }
        player.applyClassDefinition(definition);
        return true;
    }

    /**
     * Consumes one command: dash has priority, otherwise the first ready O/K/J attack wins.
     * An active dash ignores attacks and steering; other attack slots may overlap across frames.
     * Requests are not buffered. Timers age after requests, matching combat-core's start-step sample.
     * Normal movement uses only the frame time left after a dash; there is no double movement.
     * Dead/unregistered owners cannot move or trigger abilities; cooldown time still passes.
     * Projectile readiness uses the short launch window and cooldown, not the whole flight lifetime.
     * @param delta finite non-negative simulation seconds with representable ordinary movement
     * @param command immutable frame intent
     * @return accepted slot (the triggered event), or empty if no request succeeded
     * @throws NullPointerException if command is null or a custom shape incorrectly returns null
     * @throws IllegalArgumentException if delta is invalid or attack geometry is unrepresentable
     */
    public Optional<AbilitySlot> update(double delta, PlayerCommand command) {
        Objects.requireNonNull(command, "command");
        if (!Double.isFinite(delta) || delta < 0 || !Double.isFinite(player.getSpeed() * delta)) {
            throw new IllegalArgumentException("Ability delta must give finite non-negative movement");
        }
        Optional<AbilitySlot> triggered = Optional.empty();
        AbilityLoadout loadout = player.getClassDefinition().abilities();
        if (available()) {
            boolean wasDashing = dashRemaining > 0;
            if (!wasDashing && !skills.locksInput() && (command.horizontal() != 0 || command.vertical() != 0)) {
                facingX = command.horizontal();
                facingY = command.vertical();
            }
            if (!wasDashing && !skills.locksInput() && command.dash() && getState(AbilitySlot.DASH) == AbilityState.READY) {
                double length = Math.hypot(facingX, facingY);
                dashX = facingX / length;
                dashY = facingY / length;
                dashRemaining = loadout.dash().duration();
                cooldowns[AbilitySlot.DASH.ordinal()] = loadout.dash().cooldown();
                triggered = Optional.of(AbilitySlot.DASH);
            }
            boolean dashFrame = dashRemaining > 0;
            AbilityLoadout.AreaAttack ultimate = player.getClassDefinition().ultimate(player.getUltimateMode());
            if (!dashFrame && !skills.locksInput()) {
                if (command.ultimate() && isStateful(ultimate) && triggerAttack(AbilitySlot.ULTIMATE, ultimate)) {
                    triggered = Optional.of(AbilitySlot.ULTIMATE);
                } else if (!(command.ultimate() && !isStateful(ultimate) && getState(AbilitySlot.ULTIMATE) == AbilityState.READY)
                        && command.special() && isStateful(loadout.special())
                        && triggerAttack(AbilitySlot.SPECIAL, loadout.special())) {
                    triggered = Optional.of(AbilitySlot.SPECIAL);
                }
            }
            double dashTime = Math.min(delta, dashRemaining);
            if (dashTime > 0) {
                double distance = loadout.dash().speed() * dashTime;
                player.moveBy(dashX * distance, dashY * distance);
                dashRemaining = Math.max(0, dashRemaining - dashTime);
            }
            if (!skills.locksInput()) {
                player.updateMovement(command.horizontal(), command.vertical(), delta - dashTime);
            }
            skills.update(delta);
            syncHurtbox();
            if (!dashFrame && !skills.locksInput() && triggered.isEmpty()) {
                if (command.ultimate() && !isStateful(ultimate) && triggerAttack(AbilitySlot.ULTIMATE, ultimate)) {
                    triggered = Optional.of(AbilitySlot.ULTIMATE);
                } else if (command.special() && !isStateful(loadout.special()) && triggerAttack(AbilitySlot.SPECIAL, loadout.special())) {
                    triggered = Optional.of(AbilitySlot.SPECIAL);
                } else if (command.light() && triggerAttack(AbilitySlot.LIGHT, loadout.light())) {
                    triggered = Optional.of(AbilitySlot.LIGHT);
                }
            }
        } else {
            dashRemaining = 0;
            skills.clear();
        }
        for (int i = 0; i < cooldowns.length; i++) {
            cooldowns[i] = Math.max(0, cooldowns[i] - delta);
            launchWindows[i] = Math.max(0, launchWindows[i] - delta);
        }
        return triggered;
    }

    /**
     * Reports a slot's current lifecycle, giving death/unregistration precedence over active time.
     * @param slot ability slot
     * @return current lifecycle state; triggered is returned by update instead
     * @throws NullPointerException if slot is null
     */
    public AbilityState getState(AbilitySlot slot) {
        Objects.requireNonNull(slot, "slot");
        if (!available()) {
            return AbilityState.UNAVAILABLE;
        }
        if (skills.locksInput() && !skills.active(slot)) {
            return AbilityState.UNAVAILABLE;
        }
        Attack attack = attacks.get(slot);
        if (skills.active(slot) || (slot == AbilitySlot.DASH && dashRemaining > 0) || launchWindows[slot.ordinal()] > 0
                || (attack != null && attack.getProjectile() == null && attack.isActive())) {
            return AbilityState.ACTIVE;
        }
        return getCooldownRemaining(slot) > 0 ? AbilityState.COOLDOWN : AbilityState.READY;
    }

    /**
     * Returns time until this slot's cooldown expires; active duration is reported separately.
     * @param slot ability slot
     * @return remaining simulation seconds
     * @throws NullPointerException if slot is null
     */
    public double getCooldownRemaining(AbilitySlot slot) {
        return cooldowns[Objects.requireNonNull(slot, "slot").ordinal()];
    }

    /**
     * Rebinds after the owner clears the old combat scope and registers the new player adapter.
     * Cancels dash/active observations, preserves cooldowns to prevent travel bypass, resets facing.
     * The map coordinator must cancel old pipeline attacks before calling this method.
     * Binds the player's current collision world so fixed spells and derived attacks share its blockers.
     * @param next new canonical adapter with the new spawn's independent hurtbox
     * @throws NullPointerException if next is null
     * @throws IllegalArgumentException if next is not this player's registered PLAYER adapter or its world conflicts
     */
    public void bindAfterTransition(Combatant next) {
        Objects.requireNonNull(next, "source");
        if (next.getEntity() != player || next.getFaction() != Faction.PLAYER
                || !combat.getCombatants().contains(next)) {
            throw new IllegalArgumentException("Abilities require the canonical registered player adapter");
        }
        combat.bindWorld(player.getCollisionArea());
        source = next;
        skills.bind(next);
        insetX = next.getHurtbox().x() - player.getX();
        insetY = next.getHurtbox().y() - player.getY();
        hurtWidth = next.getHurtbox().width();
        hurtHeight = next.getHurtbox().height();
        attacks.clear();
        Arrays.fill(launchWindows, 0);
        dashRemaining = 0;
        facingX = 1;
        facingY = 0;
    }

    /**
     * Explicit debug/respawn reset: cancels this source's attacks and all ability timers.
     * Does not restore HP, reposition the player, or reset other actors' cooldowns.
     * @throws IllegalArgumentException if the source is no longer registered
     */
    public void reset() {
        combat.getCooldownRemaining(source);
        combat.unregister(source);
        combat.register(source);
        skills.bind(source);
        Arrays.fill(cooldowns, 0);
        Arrays.fill(launchWindows, 0);
        attacks.clear();
        dashRemaining = 0;
        facingX = 1;
        facingY = 0;
    }

    /**
     * Submits shape/projectile delivery with its element and captured shape-selected origin, then starts cooldown.
     * @param slot attack slot
     * @param area common attack tuning and optional flight mode
     * @return true if combat accepted a new invocation
     * @throws IllegalArgumentException if the computed geometry or projectile spawn is invalid
     * @throws NullPointerException if a custom shape incorrectly returns null
     */
    private boolean triggerAttack(AbilitySlot slot, AbilityLoadout.AreaAttack area) {
        if (getState(slot) != AbilityState.READY) {
            return false;
        }
        AttackSpec spec = area.attack();
        if (isStateful(area)) {
            if (area.kind() == SkillKind.ELEMENT_CYCLE && launchWindows[AbilitySlot.LIGHT.ordinal()] > 0) {
                return false;
            }
            if (!skills.start(slot, area)) {
                return false;
            }
            cooldowns[slot.ordinal()] = spec.cooldownSeconds();
            return true;
        }
        boolean rapid = area.kind() == SkillKind.ELEMENT_PROJECTILE && skills.rapid();
        ElementType element = area.kind() == SkillKind.ELEMENT_PROJECTILE ? player.getMageElement() : spec.element();
        double active = rapid ? SkillTuning.RAPID_LAUNCH : spec.activeSeconds();
        AttackSpec invocation = new AttackSpec(spec.type(), spec.damage(), active, 0, element, spec.blockable(), area.kind() == SkillKind.ELEMENT_PROJECTILE
                ? au.edu.anu.comp2120.combat.DamageType.ELEMENTAL : spec.damageType());
        Optional<Attack> accepted;
        if (area.projectile() != null) {
            ProjectileSpec flight = area.projectile();
            List<Attack> volley;
            if (rapid) {
                flight = new ProjectileSpec(flight.speed(), flight.size(), flight.lifetime(),
                        SkillTuning.RAPID_COUNT, SkillTuning.RAPID_SPREAD);
                List<AttackSpec> specifications = new ArrayList<>();
                for (int i = 0; i < SkillTuning.RAPID_COUNT; i++) {
                    specifications.add(new AttackSpec(spec.type(), spec.damage(), active, 0,
                            skills.randomElement(), spec.blockable(), au.edu.anu.comp2120.combat.DamageType.ELEMENTAL));
                }
                volley = combat.tryProjectiles(source, specifications, flight, area.range(),
                        facingX, facingY, player.getCollisionArea());
            } else {
                volley = combat.tryProjectiles(source, invocation, flight, area.range(),
                        facingX, facingY, player.getCollisionArea());
            }
            accepted = volley.stream().findFirst();
        } else {
            CollisionBox hitbox = Objects.requireNonNull(area.shape().create(source.getHurtbox(), area.range(),
                    facingX, facingY), "attack shape result");
            accepted = combat.tryAttack(source, invocation, hitbox, area.shape().origin(source.getHurtbox(), hitbox));
        }
        if (accepted.isEmpty()) {
            return false;
        }
        attacks.put(slot, accepted.get());
        if (area.projectile() != null) {
            launchWindows[slot.ordinal()] = active;
        }
        cooldowns[slot.ordinal()] = rapid ? SkillTuning.RAPID_COOLDOWN : spec.cooldownSeconds();
        return true;
    }

    /**
     * Distinguishes stateful activation from ordinary immediate shape/projectile delivery.
     * @param area immutable slot
     * @return true when ClassSkills owns execution
     */
    private static boolean isStateful(AbilityLoadout.AreaAttack area) {
        return area.kind() != SkillKind.DIRECT && area.kind() != SkillKind.ELEMENT_PROJECTILE;
    }

    /**
     * Checks life and scope identity without constructing an alias receiver.
     * @return true if this source may act in the current arena
     */
    private boolean available() {
        return player.isAlive() && combat.getCombatants().contains(source);
    }

    /** Updates independent hurtbox geometry after normal or dash movement. */
    private void syncHurtbox() {
        source.setHurtbox(new CollisionBox(player.getX() + insetX, player.getY() + insetY,
                hurtWidth, hurtHeight));
    }
}
