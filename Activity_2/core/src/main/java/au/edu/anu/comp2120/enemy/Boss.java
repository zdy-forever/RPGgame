package au.edu.anu.comp2120.enemy;

import au.edu.anu.comp2120.combat.Attack;
import au.edu.anu.comp2120.combat.Combatant;
import au.edu.anu.comp2120.combat.CombatSystem;
import au.edu.anu.comp2120.combat.DamageType;
import au.edu.anu.comp2120.combat.Health;
import au.edu.anu.comp2120.combat.AttackSpec;
import au.edu.anu.comp2120.combat.ElementType;
import au.edu.anu.comp2120.inventory.MaterialDrop;
import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.GameMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * One Boss encounter composed of existing Enemy health, EnemyAI navigation and shared combat.
 * The canonical damage receiver is the composed Enemy, never a second HP model or adapter.
 * The arena's guardian identity also selects one BossTrait, whose signature effect is layered on
 * top of these shared rules; arenas outside the campaign use the no-op trait and are unchanged.
 * Call update before the arena's single combat step, then synchronizeOutcome afterwards.
 * Defeat is terminal for this encounter; explicit debug respawn creates a new Boss instance.
 * All methods belong on the game-loop thread, outside damage receiver callbacks.
 * @author Daoyan Zhu (u7782042)
 */
public final class Boss {
    /** Bound on retained own attacks, so a stalled observer cannot grow this list without limit. */
    private static final int MAX_AWAITING_IMPACT = 16;
    /** Active window of one trait affliction pulse, matching the shared reaction pulse length. */
    private static final double AFFLICTION_ACTIVE = 0.05;
    /** Reusable non-blank Boss identity, distinct from a reward instance ID. */
    private final String id;
    /** Debug display name, independent of artwork. */
    private final String name;
    /** Immutable encounter tuning, already adjusted by this guardian's trait. */
    private final BossConfig config;
    /** This arena's signature behaviour; stateful for one encounter and never shared. */
    private final BossTrait trait;
    /** Own attacks whose landed hits the trait has not observed yet; pruned every decision step. */
    private final List<Attack> awaitingImpact = new ArrayList<>();
    /** Health seen at the previous decision step, used to derive the trait's damage signal. */
    private int observedHp;
    /** Ranged cooldown derived once from this guardian's previous wins, never from its current HP. */
    private final double boltCooldown;
    /** Fixed map owning this encounter and its loot. */
    private final GameMap map;
    /** Existing health-owning hostile entity. */
    private final Enemy enemy;
    /** Existing cached A* pursuit and collision controller. */
    private final EnemyAI navigation;
    /** Shared pipeline, advanced only by the arena owner. */
    private final CombatSystem combat;
    /** One-way phase index, initially one. */
    private int phase = 1;
    /** Index of the next pattern within the current phase. */
    private int nextPattern;
    /** Remaining one-time phase-change pause, measured in simulation seconds. */
    private double transitionRemaining;
    /** Latched defeat prevents revival or repeated drops on the same encounter. */
    private boolean defeated;
    /** Pending reward; null before death or after its one-time claim. */
    private MaterialDrop drop;
    /** Prepared melee pattern, null outside the cancellable windup window. */
    private BossConfig.Pattern pendingPattern;
    /** Locked world hitbox shown to the player before submission; never tracks a dodging target. */
    private CollisionBox pendingHitbox;
    /** Locked attack origin for the shared wall-occlusion contract. */
    private CollisionBox pendingOrigin;
    /** Remaining simulation time until the prepared melee strike is submitted. */
    private double windupRemaining;

    /**
     * Creates and registers an encounter using shared Enemy health and navigation.
     * @param id non-blank Boss identity
     * @param name non-blank display name
     * @param bounds fully walkable collision/hurtbox rectangle, independent of final visuals
     * @param map owning map
     * @param config immutable encounter definition
     * @param combat shared arena pipeline
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if identity, placement, navigation geometry or combat world binding is invalid
     */
    public Boss(String id, String name, CollisionBox bounds, GameMap map, BossConfig config, CombatSystem combat) {
        this(id, name, bounds, map, config, combat, 0);
    }

    /**
     * Creates a fresh rematch using capped tuning based on earlier successful challenges.
     * @param id non-blank Boss identity
     * @param name non-blank display name
     * @param bounds fully walkable body
     * @param map encounter map
     * @param config original unscaled encounter definition
     * @param combat shared arena pipeline
     * @param successes earlier wins against this guardian; failure does not increase this value
     * @throws NullPointerException if a required argument is null
     * @throws IllegalArgumentException if count, identity, placement or combat world binding is invalid
     */
    public Boss(String id, String name, CollisionBox bounds, GameMap map, BossConfig config,
                CombatSystem combat, int successes) {
        this.id = Objects.requireNonNull(id, "Boss ID");
        this.name = Objects.requireNonNull(name, "Boss name");
        this.map = Objects.requireNonNull(map, "map");
        trait = BossTraits.forMap(map.getId());
        this.config = BossScaling.apply(trait.tune(Objects.requireNonNull(config, "config")), successes);
        observedHp = this.config.maxHp();
        boltCooldown = BossConfig.BOLT_COOLDOWN / BossScaling.attackRate(successes);
        this.combat = Objects.requireNonNull(combat, "combat");
        if (id.isBlank() || name.isBlank()) {
            throw new IllegalArgumentException("Boss identity and name must not be blank");
        }
        enemy = new Enemy(this.config.maxHp());
        navigation = new EnemyAI(enemy, bounds, map, this.config.navigation(), combat);
    }

    /**
     * Advances encounter decisions, never combat time or damage directly.
     * Pattern index advances only on an accepted attack; shared source cooldown blocks spam.
     * Melee locks a harmless warning rectangle, stops pursuit, then submits it after the windup.
     * Elemental campaign guardians may fire shared projectiles while pursuing distant targets.
     * Captures the pattern-selected origin; combat-core rechecks wall occlusion when each target settles.
     * Transition frames consume time without attacks/movement, discarding any excess frame time.
     * Generic freeze/stagger pauses decisions; navigation still resolves queued collision-safe knockback.
     * A living attached encounter then advances its guardian trait, before those same suppression
     * rules apply, so afflictions already delivered continue while this Boss itself cannot act.
     * A trait relocation consumes the current decision step: no pursuit or new attack can undo
     * its safe landing distance or recreate its cancelled melee warning in the arrival frame.
     * @param target eligible canonical hostile target, or null
     * @param delta finite non-negative simulation seconds with representable movement
     * @throws IllegalArgumentException if time or resulting geometry is invalid
     * @throws NullPointerException if a custom pattern returns null geometry
     */
    public void update(Combatant target, double delta) {
        if (!Double.isFinite(delta) || delta < 0 || !Double.isFinite(config.navigation().speed() * delta)) {
            throw new IllegalArgumentException("Boss delta must give finite non-negative movement");
        }
        synchronizeOutcome();
        if (defeated || !registered()) {
            cancelWindup();
            awaitingImpact.clear();
            return;
        }
        CollisionBox beforeTrait = getBounds();
        advanceTrait(target, delta);
        if (!getBounds().equals(beforeTrait)) { return; }
        if (getCombatant().getStatus().blocksActions()) {
            cancelWindup();
            navigation.update(null, delta, false);
            return;
        }
        if (transitionRemaining > 0) {
            transitionRemaining = Math.max(0, transitionRemaining - delta);
            return;
        }
        if (pendingPattern != null) {
            if (getCombatant().getStatus().getKnockbackX() != 0 || getCombatant().getStatus().getKnockbackY() != 0) {
                // A displaced attacker must prepare again at its new collision-safe position.
                cancelWindup();
                navigation.update(null, delta, false);
                return;
            }
            if (target == null || !combat.getCombatants().contains(target) || !target.isTargetable()
                    || target.getFaction() == getCombatant().getFaction()) {
                cancelWindup();
                navigation.update(null, delta, false);
                return;
            }
            windupRemaining = Math.max(0, windupRemaining - delta);
            if (windupRemaining == 0) {
                Optional<Attack> submitted =
                        combat.tryAttack(getCombatant(), pendingPattern.attack(), pendingHitbox, pendingOrigin);
                if (submitted.isPresent()) {
                    retainForTrait(submitted.get());
                    nextPattern = (nextPattern + 1) % (phase == 1 ? config.phaseOne() : config.phaseTwo()).size();
                }
                cancelWindup();
            }
            return;
        }
        navigation.update(target, delta, false);
        if (combat.getCooldownRemaining(getCombatant()) > 0) { return; }
        if (navigation.getState() == EnemyState.CHASE) {
            tryElementBolt(target);
            return;
        }
        if (navigation.getState() != EnemyState.ATTACK) {
            return;
        }
        List<BossConfig.Pattern> cycle = phase == 1 ? config.phaseOne() : config.phaseTwo();
        BossConfig.Pattern pattern = cycle.get(nextPattern);
        CollisionBox body = getBounds();
        CollisionBox hurtbox = target.getHurtbox();
        double dx = hurtbox.x() + hurtbox.width() / 2 - (body.x() + body.width() / 2);
        double dy = hurtbox.y() + hurtbox.height() / 2 - (body.y() + body.height() / 2);
        if (dx == 0 && dy == 0) {
            dx = 1;
        }
        CollisionBox hitbox = Objects.requireNonNull(pattern.shape().create(body, pattern.range(), dx, dy),
                "Boss pattern geometry");
        pendingPattern = pattern;
        pendingHitbox = hitbox;
        pendingOrigin = pattern.shape().origin(body, hitbox);
        windupRemaining = BossConfig.MELEE_WINDUP;
    }

    /**
     * Fires an aimed non-homing elemental bolt during pursuit through shared projectile settlement.
     * Custom non-elemental maps retain navigation/melee only; official maps define guardian elements.
     * @param target registered target already validated by EnemyAI
     */
    private void tryElementBolt(Combatant target) {
        var guardian = BossChallenge.forMap(map.getId());
        if (guardian.isEmpty()) { return; }
        CollisionBox body = getBounds(), targetBox = target.getHurtbox();
        double dx = targetBox.x() + targetBox.width() / 2 - body.x() - body.width() / 2;
        double dy = targetBox.y() + targetBox.height() / 2 - body.y() - body.height() / 2;
        if (Math.hypot(dx, dy) > BossConfig.BOLT_RANGE || (dx == 0 && dy == 0)) { return; }
        ElementType element = switch (guardian.orElseThrow()) {
            case FIRE -> ElementType.FIRE;
            case WATER -> ElementType.WATER;
            case LIGHTNING -> ElementType.LIGHTNING;
            case ICE -> ElementType.ICE;
            case WIND -> ElementType.WIND;
        };
        combat.tryProjectiles(getCombatant(), new AttackSpec("boss-element-bolt", BossConfig.BOLT_DAMAGE,
                BossConfig.BOLT_ACTIVE_SECONDS, boltCooldown, element), BossConfig.ELEMENT_BOLT,
                BossConfig.BOLT_RANGE, dx, dy, map).forEach(this::retainForTrait);
    }

    /**
     * Feeds this guardian's trait the events it reacts to, then advances its own timers.
     * Impacts are reported one decision step after the shared pipeline settled them, so no trait
     * runs inside damage settlement. The damage signal is derived from settled health rather than
     * a callback, which also covers reaction pulses, fields and explicit debug damage.
     * @param target current eligible target, or null when none is present
     * @param delta validated finite non-negative seconds
     */
    private void advanceTrait(Combatant target, double delta) {
        if (!awaitingImpact.isEmpty()) {
            // Collect first and report afterwards, so a trait is free to clear this list itself.
            List<Attack> landed = new ArrayList<>(awaitingImpact.size());
            for (Iterator<Attack> pending = awaitingImpact.iterator(); pending.hasNext();) {
                Attack own = pending.next();
                if (target != null && own.hasLandedHit(target.getEntity())) {
                    landed.add(own);
                    pending.remove();
                } else if (!own.isActive()) {
                    pending.remove();
                }
            }
            for (Attack own : landed) {
                trait.onLandedHit(this, target, own);
            }
        }
        int taken = Math.max(0, observedHp - getHealth().getCurrentHp());
        observedHp = getHealth().getCurrentHp();
        if (taken > 0) {
            trait.onDamageTaken(this, target, taken);
        }
        trait.update(this, target, delta);
    }

    /**
     * Retains one accepted own attack until its impact is observed or its window closes.
     * The oldest entry is discarded at capacity; retention is observational and never damages.
     * @param attack invocation the shared pipeline accepted for this Boss
     */
    private void retainForTrait(Attack attack) {
        if (trait == BossTrait.NONE) {
            return;
        }
        if (awaitingImpact.size() >= MAX_AWAITING_IMPACT) {
            awaitingImpact.remove(0);
        }
        awaitingImpact.add(attack);
    }

    /**
     * Submits one trait affliction on the target's own hurtbox through the shared pipeline.
     * Traits must use this rather than a health model, so faction, life, hit-ledger and occlusion
     * rules still apply. It never consumes this Boss's melee or ranged cooldown.
     * @param target current registered receiver
     * @param type non-blank diagnostic label
     * @param damage non-negative affliction damage
     * @param element damage tag; derived provenance prevents attachment and further reactions
     * @return true when the pipeline accepted the pulse
     */
    boolean afflict(Combatant target, String type, int damage, ElementType element) {
        return combat.tryAffliction(getCombatant(), new AttackSpec(type, damage, AFFLICTION_ACTIVE,
                0, element, false, DamageType.ELEMENTAL), target.getHurtbox()).isPresent();
    }

    /**
     * Relocates this encounter for an evasive trait, dropping its prepared strike and route.
     * Own active attacks are cancelled so no hitbox is left behind, exactly as a phase change does.
     * Health, registration, phase, cooldowns and the pending reward are unchanged.
     * @param x destination left edge
     * @param y destination bottom edge
     * @return true only when the destination accepted the full body
     */
    boolean blinkTo(double x, double y) {
        if (!navigation.teleportTo(x, y)) {
            return false;
        }
        cancelWindup();
        navigation.resetAwareness();
        combat.cancelAttacks(getCombatant());
        awaitingImpact.clear();
        return true;
    }

    /**
     * Reports whether this encounter may currently start a new action of its own.
     * Traits representing Boss agency, rather than an already delivered effect, must check this.
     * @return true while alive, attached, not control-suppressed and not in a phase transition
     */
    boolean canAct() {
        return isAlive() && registered() && !getCombatant().getStatus().blocksActions()
                && transitionRemaining == 0;
    }

    /**
     * Describes this guardian's trait state for the existing diagnostics line.
     * @return short trait label, or empty for arenas without a signature behaviour
     */
    public String getTraitStatus() {
        return trait.status();
    }

    /**
     * Names the affliction this guardian currently maintains on its target, for display above it.
     * Shared control and vulnerability statuses are read from the target's own CombatStatus instead.
     * @return prominent uppercase label, or empty when no trait affliction is running
     */
    public String getAfflictionLabel() {
        return isAlive() && registered() ? trait.afflictionLabel() : "";
    }

    /** Clears preparation without creating damage or altering the shared cooldown. */
    private void cancelWindup() {
        pendingPattern = null;
        pendingHitbox = null;
        pendingOrigin = null;
        windupRemaining = 0;
    }

    /**
     * Exposes the exact fixed melee geometry before impact, without renderer-owned timing.
     * @return harmless warning rectangle, empty if dead, interrupted or detached
     */
    public Optional<CollisionBox> getMeleeWarning() {
        return isAlive() && registered() && !getCombatant().getStatus().blocksActions()
                ? Optional.ofNullable(pendingHitbox) : Optional.empty();
    }

    /**
     * Reads windup progress, used for accelerated warning flashes.
     * @return elapsed fraction from zero to one, or zero outside windup
     */
    public double getWindupProgress() {
        return getMeleeWarning().isEmpty() ? 0 : 1 - windupRemaining / BossConfig.MELEE_WINDUP;
    }

    /**
     * Observes settled HP after combat, latching defeat/drop or the one-way second phase.
     * Death takes precedence over phase change. Repeated calls never duplicate rewards or restart
     * transition time. Detached old-map encounters do not generate loot. Cancelling attacks preserves
     * remaining cooldowns and never damages another entity.
     */
    public void synchronizeOutcome() {
        if (defeated || !registered()) {
            cancelWindup();
            return;
        }
        if (!enemy.isAlive()) {
            defeated = true;
            cancelWindup();
            transitionRemaining = 0;
            combat.cancelAttacks(getCombatant());
            CollisionBox body = getBounds();
            double size = Math.min(12, Math.min(body.width(), body.height()));
            CollisionBox placement = new CollisionBox(body.x() + (body.width() - size) / 2,
                    body.y() + (body.height() - size) / 2, size, size);
            drop = new MaterialDrop(UUID.randomUUID().toString(), config.materialId(), config.materialQuantity(),
                    map.getId(), placement);
        } else if (phase == 1 && getHealth().getCurrentHp() / (double) getHealth().getMaxHp() <= config.phaseTwoFraction()) {
            phase = 2;
            cancelWindup();
            nextPattern = 0;
            transitionRemaining = config.transitionSeconds();
            navigation.resetAwareness();
            combat.cancelAttacks(getCombatant());
        }
    }

    /**
     * Returns the current decision with immediate health-aware death reporting.
     * @return encounter state
     */
    public BossState getState() {
        if (!isAlive()) {
            return BossState.DEAD;
        }
        if (!registered()) {
            return BossState.IDLE;
        }
        if (getCombatant().getStatus().isFrozen()) {
            return BossState.FROZEN;
        }
        if (getCombatant().getStatus().blocksActions()) {
            return BossState.STAGGERED;
        }
        if (transitionRemaining > 0) {
            return BossState.PHASE_TRANSITION;
        }
        if (pendingPattern != null) { return BossState.WINDUP; }
        return switch (navigation.getState()) {
            case CHASE -> BossState.CHASE;
            case ATTACK -> BossState.ATTACK;
            case IDLE, DEAD -> BossState.IDLE;
        };
    }

    /**
     * Returns the reusable identity.
     * @return stable Boss identity
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the display label.
     * @return debug name independent of assets
     */
    public String getName() {
        return name;
    }

    /**
     * Exposes validated health operations.
     * @return shared health owned by the composed Enemy, not a copy
     */
    public Health getHealth() {
        return enemy.getHealth();
    }

    /**
     * Reports encounter life status.
     * @return true while HP remains and this encounter has not been defeated
     */
    public boolean isAlive() {
        return !defeated && enemy.isAlive();
    }

    /**
     * Exposes current position and size.
     * @return current collision body and canonical hurtbox
     */
    public CollisionBox getBounds() {
        return navigation.getBounds();
    }

    /**
     * Returns the shared damage receiver.
     * @return canonical Enemy adapter registered with combat-core
     */
    public Combatant getCombatant() {
        return navigation.getCombatant();
    }

    /**
     * Reports the entered HP phase.
     * @return one-way phase number, one or two
     */
    public int getPhase() {
        return phase;
    }

    /**
     * Reports remaining transition time.
     * @return non-negative remaining transition seconds
     */
    public double getTransitionRemaining() {
        return transitionRemaining;
    }

    /**
     * Returns the unclaimed reward only while this encounter belongs to its arena.
     * @return pending immutable reward, empty before defeat, after claiming or after detachment
     */
    public Optional<MaterialDrop> getDrop() {
        return registered() ? Optional.ofNullable(drop) : Optional.empty();
    }

    /**
     * Clears and returns the pending reward at most once.
     * The encounter coordinator records the reward in its result outbox before calling this method
     * on the game-loop thread. This method alone neither stores items nor persists rewards.
     * @return reward transferred to the caller, or empty when none remains
     */
    public Optional<MaterialDrop> claimDrop() {
        Optional<MaterialDrop> pending = getDrop();
        drop = null;
        return pending;
    }

    /**
     * Checks arena membership.
     * @return whether this encounter still belongs to the active combat scope
     */
    private boolean registered() {
        return combat.getCombatants().contains(getCombatant());
    }
}
