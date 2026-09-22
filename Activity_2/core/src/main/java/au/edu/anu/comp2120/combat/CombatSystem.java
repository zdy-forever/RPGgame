package au.edu.anu.comp2120.combat;

import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.CollisionSweep;
import au.edu.anu.comp2120.world.WalkableArea;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Shared attack creation, filtering, hit detection, damage calculation, and delivery pipeline.
 * Scope one instance to one simulated arena; register stable adapters and clear on map transitions.
 * Uses deterministic insertion order and simulation time, not rendering or wall-clock APIs.
 * Call on one game-loop thread; receivers must not re-enter or mutate this system during damage.
 *
 * @author Hao Liu (u7998420)
 * @author Daoyan Zhu (u7782042)
 */
public final class CombatSystem {
    /** Bounded observational mailbox, never used to drive damage or gameplay. */
    private final java.util.ArrayDeque<HitFeedback> feedback = new java.util.ArrayDeque<>();
    /** Limits retained presentation observations when no renderer consumes them. */
    private static final int MAX_FEEDBACK = 128;

    /**
     * Transfers confirmed-hit observations once without changing combat state.
     * @return immutable observations since the last drain or scope clear
     */
    public List<HitFeedback> drainHitFeedback() {
        List<HitFeedback> result = List.copyOf(feedback);
        feedback.clear();
        return result;
    }

    /**
     * Retains a bounded immutable observation; oldest feedback is disposable presentation data.
     * @param hit settled hit snapshot
     */
    private void recordFeedback(HitFeedback hit) {
        if (feedback.size() == MAX_FEEDBACK) { feedback.removeFirst(); }
        feedback.addLast(hit);
    }
    /** Maximum simultaneously retained projectiles; a volley is accepted or rejected as a whole. */
    public static final int MAX_PROJECTILES = 256;
    /** Maximum live derived invocations, bounding reaction fan-out across updates. */
    public static final int MAX_DERIVED_ATTACKS = 64;
    /** Canonical registered adapters and their remaining per-source cooldowns. */
    private final Map<Combatant, Double> cooldowns = new LinkedHashMap<>();
    /** Active invocations in creation order. */
    private final List<Attack> attacks = new ArrayList<>();
    /** Sparse current-scope attachments keyed by canonical receiver adapters, never shared across maps. */
    private final Map<Combatant, ElementStatus> elements = new LinkedHashMap<>();
    /** Fixed ground fields owned by this combat scope, never the renderer. */
    private final List<ElementField> fields = new ArrayList<>();
    /** Pure immutable reaction table and attachment duration, replaceable without class-specific logic. */
    private final ElementRules elementRules;
    /** Static world for this scope; null only for standalone map-free combat calculations. */
    private WalkableArea world;
    /** Last issued identifier, retained across arena clears. */
    private long lastAttackId;

    /** Creates an empty map-free scope; gameplay controllers bind their world before settlement. */
    public CombatSystem() {
        this(ElementRules.DEFAULT);
    }

    /**
     * Creates a map-free combat scope with explicit centralized elemental rules; bind its world for gameplay.
     * @param elementRules immutable attachment/reaction configuration
     * @throws NullPointerException if elementRules is null
     */
    public CombatSystem(ElementRules elementRules) {
        this.elementRules = Objects.requireNonNull(elementRules, "element rules");
    }

    /**
     * Binds the collision world shared by all fixed attacks and derived effects in this scope.
     * Player/AI controllers and projectile launches supply it automatically. Standalone map-free
     * calculations have no blockers until bound. Rebinding the same instance is harmless; changing
     * maps requires clear first so active attacks cannot borrow another world's collision data.
     * @param area static collision provider for the current arena
     * @throws NullPointerException if area is null
     * @throws IllegalArgumentException if a different world is already bound
     */
    public void bindWorld(WalkableArea area) {
        Objects.requireNonNull(area, "world");
        if (world != null && world != area) {
            throw new IllegalArgumentException("Clear the combat scope before changing its world");
        }
        world = area;
    }

    /**
     * Registers one stable adapter per underlying receiver with an initially ready cooldown.
     * Duplicate registration and aliases of the same receiver are rejected before mutation.
     * @param combatant actor participating in this arena
     * @throws NullPointerException if combatant is null
     * @throws IllegalArgumentException if the receiver is already registered
     */
    public void register(Combatant combatant) {
        Objects.requireNonNull(combatant, "combatant");
        for (Combatant existing : cooldowns.keySet()) {
            if (existing.getEntity() == combatant.getEntity()) {
                throw new IllegalArgumentException("Damage receiver is already registered");
            }
        }
        cooldowns.put(combatant, 0.0);
    }

    /**
     * Removes a canonical actor, clears its status and cancels source-owned attacks/fields/charged effects. Other attacks retain their
     * hit ledger, so removing/re-registering a target cannot make it take the same hit again.
     * @param combatant canonical actor to remove
     * @return true if registered, false if unknown or a different adapter
     * @throws NullPointerException if combatant is null
     */
    public boolean unregister(Combatant combatant) {
        Objects.requireNonNull(combatant, "combatant");
        if (cooldowns.remove(combatant) == null) {
            return false;
        }
        elements.remove(combatant);
        cooldowns.keySet().forEach(actor -> {
            if (actor.getStatus().chargedSource() == combatant) { actor.getStatus().clearCharge(); }
        });
        combatant.clearCapabilities();
        cancelAttacks(combatant);
        return true;
    }

    /**
     * Cancels source-owned invocations, fields and charged effects without resetting registration, health or cooldown.
     * Used between combat updates for encounter transitions/death; never call from a damage receiver.
     * An unknown source has no attacks and is harmless.
     * @param combatant source whose fixed and moving attacks should stop
     * @throws NullPointerException if combatant is null
     */
    public void cancelAttacks(Combatant combatant) {
        Objects.requireNonNull(combatant, "combatant");
        for (Attack attack : attacks) {
            if (attack.getSource() == combatant) {
                attack.cancel();
            }
        }
        attacks.removeIf(attack -> !attack.isActive());
        fields.stream().filter(field -> field.getSource() == combatant).forEach(ElementField::cancel);
        fields.removeIf(field -> !field.isActive());
        cooldowns.keySet().forEach(actor -> {
            if (actor.getStatus().chargedSource() == combatant) { actor.getStatus().clearCharge(); }
        });
    }

    /**
     * Attempts a new attack. A dead source or positive cooldown returns empty without mutation.
     * All arguments are validated first, even if the source is dead or cooling down.
     * New invocations are sampled by the next update, including an update with zero delta.
     * Captures the source hurtbox centre as origin; fixed effects use the explicit-origin overload.
     * @param source canonical registered attacker
     * @param specification reusable validated attack tuning
     * @param hitbox immutable world-space geometry for this invocation
     * @return new invocation, or empty if the source cannot currently attack
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if source is not registered in this scope
     * @throws IllegalStateException if the system has exhausted its identifier range
     */
    public Optional<Attack> tryAttack(Combatant source, AttackSpec specification, CollisionBox hitbox) {
        Objects.requireNonNull(source, "source");
        return tryAttack(source, specification, hitbox, source.getHurtbox());
    }

    /**
     * Creates a fixed invocation with explicit cast-time origin, without class-specific damage rules.
     * Use the attacker hurtbox for melee; use hitbox itself for fixed AoE/field centres.
     * Origin and hitbox remain fixed when the caster moves. Occluded candidates do not consume
     * hit ledgers, apply elements or create reactions, and may become eligible later in the active window.
     * @param source registered canonical attacker
     * @param specification validated damage/timing/element tuning
     * @param hitbox fixed hit region
     * @param origin fixed anchor whose centre is queried against each target centre
     * @return accepted invocation, or empty for a dead/cooling source
     * @throws NullPointerException if an argument is null
     * @throws IllegalArgumentException if source is unregistered
     * @throws IllegalStateException if attack identifiers are exhausted
     */
    public Optional<Attack> tryAttack(Combatant source, AttackSpec specification, CollisionBox hitbox, CollisionBox origin) {
        Objects.requireNonNull(specification, "specification");
        Objects.requireNonNull(hitbox, "hitbox");
        Objects.requireNonNull(origin, "origin");
        double remainingCooldown = getCooldownRemaining(source);
        if (!source.getEntity().isAlive() || remainingCooldown > 0) {
            return Optional.empty();
        }
        if (lastAttackId == Long.MAX_VALUE) {
            throw new IllegalStateException("Attack identifier range exhausted");
        }
        Attack attack = new Attack(++lastAttackId, source, specification, hitbox, null, 0, origin);
        attacks.add(attack);
        cooldowns.put(source, specification.cooldownSeconds());
        return Optional.of(attack);
    }

    /**
     * Launches one validated symmetric volley using the existing canonical source and attack IDs.
     * Each arrow is non-piercing and can damage only its first valid target. The source cooldown is
     * charged once per volley; common player slots pass zero and own their independent cooldowns.
     * Flight begins at the source hurtbox centre, avoiding unchecked spawn offsets through walls.
     * @param source canonical registered attacker
     * @param attack shared damage/type/source-cooldown specification
     * @param flight validated flight and volley configuration
     * @param range positive finite maximum range per arrow
     * @param directionX horizontal nonzero direction component
     * @param directionY vertical nonzero direction component
     * @param area current collision environment
     * @return immutable accepted volley; empty for dead/cooling source or exhausted projectile capacity
     * @throws NullPointerException if a required argument is null
     * @throws IllegalArgumentException if source, range, direction, spawn or current-world binding is invalid
     * @throws IllegalStateException if there are not enough remaining attack identifiers
     */
    public List<Attack> tryProjectiles(Combatant source, AttackSpec attack, ProjectileSpec flight,
            double range, double directionX, double directionY, WalkableArea area) {
        Objects.requireNonNull(attack, "attack");
        Objects.requireNonNull(flight, "flight");
        return tryProjectiles(source, java.util.Collections.nCopies(flight.count(), attack), flight,
                range, directionX, directionY, area);
    }

    /**
     * Atomically launches a volley with per-shot damage/element metadata through the same flight pipeline.
     * Validation/capacity failure cannot create a partial volley; source cooldown is the maximum supplied.
     * @param source canonical attacker
     * @param specifications one validated specification per projectile
     * @param flight shared flight/volley geometry
     * @param range positive maximum distance
     * @param directionX facing x component
     * @param directionY facing y component
     * @param area current collision world
     * @return complete immutable volley, or empty for cooldown/death/capacity rejection
     * @throws NullPointerException if required data is null
     * @throws IllegalArgumentException if count, geometry, world or source is invalid
     * @throws IllegalStateException if attack identifiers are exhausted
     */
    public List<Attack> tryProjectiles(Combatant source, List<AttackSpec> specifications, ProjectileSpec flight,
            double range, double directionX, double directionY, WalkableArea area) {
        Objects.requireNonNull(flight, "flight");
        List<AttackSpec> specs = List.copyOf(specifications);
        if (specs.size() != flight.count()) {
            throw new IllegalArgumentException("Each projectile requires one attack specification");
        }
        Objects.requireNonNull(area, "area");
        double cooldown = getCooldownRemaining(source);
        double length = Math.hypot(directionX, directionY);
        if (!Double.isFinite(length) || length == 0) {
            throw new IllegalArgumentException("Projectile direction must be nonzero and finite");
        }
        CollisionBox body = source.getHurtbox();
        CollisionBox origin = new CollisionBox(body.x() + body.width() / 2 - flight.size() / 2,
                body.y() + body.height() / 2 - flight.size() / 2, flight.size(), flight.size());
        List<Projectile> shots = new ArrayList<>();
        for (int i = 0; i < flight.count(); i++) {
            double degrees = flight.count() == 1 ? 0
                    : -flight.spreadDegrees() / 2 + flight.spreadDegrees() * i / (flight.count() - 1);
            double radians = Math.toRadians(degrees);
            double dx = directionX / length * Math.cos(radians) - directionY / length * Math.sin(radians);
            double dy = directionX / length * Math.sin(radians) + directionY / length * Math.cos(radians);
            shots.add(new Projectile(origin, flight, range, dx, dy, area));
        }
        bindWorld(area);
        if (!source.getEntity().isAlive() || cooldown > 0
                || attacks.stream().filter(a -> a.isActive() && a.getProjectile() != null).count()
                + flight.count() > MAX_PROJECTILES) {
            return List.of();
        }
        if (lastAttackId > Long.MAX_VALUE - flight.count()) {
            throw new IllegalStateException("Attack identifier range exhausted");
        }
        List<Attack> volley = new ArrayList<>();
        for (int i = 0; i < shots.size(); i++) {
            volley.add(new Attack(++lastAttackId, source, specs.get(i), origin, shots.get(i)));
        }
        attacks.addAll(volley);
        cooldowns.put(source, specs.stream().mapToDouble(AttackSpec::cooldownSeconds).max().orElseThrow());
        return List.copyOf(volley);
    }

    /**
     * Samples currently active hitboxes against current hurtboxes, then advances time by delta.
     * The active interval includes creation and excludes expiry. Even if delta spans the whole
     * duration, the start-of-step sample is resolved once; this is not swept collision detection.
     * Repeated zero-time updates cannot repeat a hit. Previously scheduled target-local ticks settle first;
     * other hits use attack creation order then registration order, including newly queued ground ticks.
     * a source killed earlier in the same step cannot deliver a later attack.
     * Fixed attacks also require a clear static-world segment from captured origin to target centre,
     * before damage, hit-ledger consumption, elemental attachment or reaction creation.
     * Projectiles instead sweep their capped flight segment against current target hurtboxes and
     * static walls, choosing the nearest hit. Walls win equal-time ties; targets tie by registration.
     * Attachments are sampled during hit settlement and aged once afterwards, including new attachments.
     * Derived attacks created this step are queued in the same list but first resolve next update;
     * they cannot create reactions or children. Periodic reaction clocks advance before direct settlement;
     * newly applied reaction statuses/fields start aging next update. Ground ticks are retired after one sample.
     * Zero-time updates are valid but do not repeat settled hits.
     * @param delta finite non-negative elapsed simulation seconds
     * @throws IllegalArgumentException if delta is negative or non-finite; no state changes occur
     */
    public void update(double delta) {
        if (!Double.isFinite(delta) || delta < 0) {
            throw new IllegalArgumentException("Combat delta must be finite and non-negative");
        }
        List<Attack> fieldTicks = advanceReactions(delta);
        for (Attack attack : List.copyOf(attacks)) {
            if (!attack.getSource().getEntity().isAlive()) {
                attack.cancel();
                continue;
            }
            if (attack.isActive()) {
                if (attack.getProjectile() != null) {
                    updateProjectile(attack, delta);
                    continue;
                }
                for (Combatant target : cooldowns.keySet()) {
                    if (canHit(attack, target)) {
                        deliverDamage(attack, target);
                    }
                }
                attack.advance(delta);
            }
        }
        for (Attack attack : attacks) {
            if (!attack.getSource().getEntity().isAlive()) {
                attack.cancel();
            }
        }
        // Periodic ground ticks sample once; a tiny frame must not leave a hitbox after field expiry.
        fieldTicks.forEach(Attack::cancel);
        attacks.removeIf(attack -> !attack.isActive());
        elements.replaceAll((target, status) -> status.advance(delta));
        elements.entrySet().removeIf(entry -> !entry.getKey().getEntity().isAlive()
                || entry.getValue().element() == ElementType.NONE);
        cooldowns.replaceAll((source, remaining) -> Math.max(0, remaining - delta));
        cleanupReactions();
    }

    /**
     * Returns a canonical actor's remaining cooldown.
     * @param source registered actor
     * @return finite non-negative seconds until a new attack is permitted
     * @throws NullPointerException if source is null
     * @throws IllegalArgumentException if source is not registered
     */
    public double getCooldownRemaining(Combatant source) {
        Objects.requireNonNull(source, "source");
        Double remaining = cooldowns.get(source);
        if (remaining == null) {
            throw new IllegalArgumentException("Combat source is not registered");
        }
        return remaining;
    }

    /**
     * Returns a read-only attachment snapshot; dead actors always report no attachment.
     * @param target canonical registered target
     * @return current status or EMPTY
     * @throws NullPointerException if target is null
     * @throws IllegalArgumentException if target is not registered
     */
    public ElementStatus getElementStatus(Combatant target) {
        getCooldownRemaining(target);
        return target.getEntity().isAlive() ? elements.getOrDefault(target, ElementStatus.EMPTY) : ElementStatus.EMPTY;
    }

    /**
     * Returns an immutable list of registered actor adapters in deterministic order.
     * @return membership snapshot; the actors retain their live health and geometry
     */
    public List<Combatant> getCombatants() {
        return List.copyOf(cooldowns.keySet());
    }

    /**
     * Returns invocations still retained by this simulation for read-only debugging.
     * @return immutable membership snapshot; invocation lifetimes remain live
     */
    public List<Attack> getActiveAttacks() {
        return List.copyOf(attacks);
    }

    /**
     * Cancels attacks/fields and removes registrations/cooldowns/attachments/reaction statuses without changing HP.
     * Also unbinds the old world. At transitions, bind the new world and register its actors afterward.
     */
    public void clear() {
        feedback.clear();
        attacks.forEach(Attack::cancel);
        attacks.clear();
        cooldowns.keySet().forEach(Combatant::clearCapabilities);
        cooldowns.clear();
        elements.clear();
        fields.forEach(ElementField::cancel);
        fields.clear();
        world = null;
    }

    /**
     * Applies identity, faction, life, repeat-hit, overlap and static-world occlusion rules.
     * @param attack active invocation
     * @param target registered candidate
     * @return true only for an overlapping fresh hostile living receiver not occluded by static walls
     */
    private boolean canHit(Attack attack, Combatant target) {
        if (!isValidTarget(attack, target) || !attack.getHitbox().overlaps(target.getHurtbox())) {
            return false;
        }
        CollisionBox origin = attack.getOrigin();
        CollisionBox hurtbox = target.getHurtbox();
        return world == null || world.hasLineOfSight(origin.x() + origin.width() / 2,
                origin.y() + origin.height() / 2, hurtbox.x() + hurtbox.width() / 2,
                hurtbox.y() + hurtbox.height() / 2);
    }

    /**
     * Applies receiver identity, faction, life and repeat-hit rules for both attack delivery modes.
     * @param attack current invocation
     * @param target canonical registered candidate
     * @return true if geometry may be tested against this target
     */
    private boolean isValidTarget(Attack attack, Combatant target) {
        Combatant source = attack.getSource();
        return source.getEntity() != target.getEntity()
                && source.getFaction() != target.getFaction()
                && target.getEntity().isAlive()
                && !attack.hasHit(target.getEntity());
    }

    /**
     * Advances one non-piercing shot to its nearest valid hit or capped endpoint.
     * Target bodies are sampled at their current post-movement positions, not swept themselves.
     * @param attack projectile-backed invocation
     * @param delta validated elapsed seconds
     */
    private void updateProjectile(Attack attack, double delta) {
        Projectile shot = attack.getProjectile();
        double distance = shot.stepDistance(delta);
        double first = shot.firstBlock(distance);
        Combatant hit = null;
        for (Combatant target : cooldowns.keySet()) {
            if (isValidTarget(attack, target)) {
                double entry = CollisionSweep.entry(shot.getBounds(), shot.getDirectionX() * distance,
                        shot.getDirectionY() * distance, target.getHurtbox());
                if (entry < first) {
                    first = entry;
                    hit = target;
                }
            }
        }
        shot.advance(distance * Math.min(1, first), delta);
        if (hit != null) {
            deliverDamage(attack, hit);
        }
        if (Double.isFinite(first)) {
            attack.cancel();
        }
    }

    /**
     * Delivers a geometrically confirmed hit through the one shared damage/health pipeline.
     * @param attack invocation whose target filtering and detection succeeded
     * @param target valid receiver
     */
    private void deliverDamage(Attack attack, Combatant target) {
        if (target.intercepts(attack)) {
            recordFeedback(new HitFeedback(target.getHurtbox(), attack.getSpecification().element(), "", true));
            attack.recordHit(target.getEntity());
            return;
        }
        ElementType incoming = attack.getSpecification().element();
        ElementRules.Reaction reaction = attack.isDerived() || incoming == ElementType.NONE ? null
                : elementRules.find(getElementStatus(target).element(), incoming).orElse(null);
        target.getEntity().takeDamage(calculateDamage(attack, target, reaction));
        recordFeedback(new HitFeedback(target.getHurtbox(), incoming, reaction == null ? "" : reaction.id(), false));
        attack.recordLandedHit(target.getEntity());
        attack.recordHit(target.getEntity());
        if (!target.getEntity().isAlive() || reaction != null) {
            elements.remove(target);
        } else if (!attack.isDerived() && incoming != ElementType.NONE) {
            elements.put(target, new ElementStatus(incoming, elementRules.attachmentSeconds()));
        }
        if (reaction != null) {
            if (reaction.derived() != null) { enqueueDerived(attack, target, reaction); }
            applyReaction(attack, target, reaction);
        }
        if (!target.getEntity().isAlive()) { target.getStatus().clear(); }
    }

    /**
     * Central damage calculation boundary for ordinary, elemental and derived hits.
     * Applies reactions, physical vulnerability and equipment multipliers; floors once and saturates.
     * Baseline physical resistance is zero; only PHYSICAL damage receives the temporary reduction.
     * Health remains responsible for saturating lethal damage at zero.
     * @param attack invocation that passed detection
     * @param target confirmed receiver whose physical vulnerability may modify this hit
     * @param reaction matched rule, or null for ordinary/non-reacting/derived hits
     * @return non-negative damage to deliver through Damageable
     */
    private int calculateDamage(Attack attack, Combatant target, ElementRules.Reaction reaction) {
        double total = (double) attack.getSpecification().damage() * (reaction == null ? 1 : reaction.multiplier())
                + (reaction == null ? 0 : reaction.bonusDamage());
        if (attack.getSpecification().damageType() == DamageType.PHYSICAL) {
            total *= target.getStatus().physicalMultiplier();
        }
        if (attack.getSource().getEntity() instanceof EquipmentModifiers equipment) {
            total *= equipment.equipmentAttackMultiplier();
        }
        if (target.getEntity() instanceof EquipmentModifiers equipment) {
            total *= equipment.equipmentDefenceMultiplier();
        }
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    /**
     * Queues one-generation geometry for a confirmed reaction; never recursively delivers damage.
     * Children retain the incoming attacker, have their own ID/ledger, and do not charge cooldown.
     * Derived ground effects are non-blockable; targetability never grants immunity to them.
     * Their occlusion origin is the newly captured effect centre, not the original attacker's position.
     * Full capacity, exhausted IDs or unrepresentable effect geometry skip only the optional child;
     * the triggering damage/attachment consumption has already settled and is never retried.
     * @param parent confirmed direct elemental invocation
     * @param target reacting target, including one killed by the triggering hit
     * @param reaction matched rule containing a derived effect
     */
    private void enqueueDerived(Attack parent, Combatant target, ElementRules.Reaction reaction) {
        if (parent.isDerived() || lastAttackId == Long.MAX_VALUE
                || attacks.stream().filter(attack -> attack.isActive() && attack.isDerived()).count() >= MAX_DERIVED_ATTACKS) {
            return;
        }
        ElementRules.DerivedEffect effect = reaction.derived();
        CollisionBox body = target.getHurtbox();
        CollisionBox hitbox;
        try {
            hitbox = new CollisionBox(body.x() + body.width() / 2 - effect.radius(),
                    body.y() + body.height() / 2 - effect.radius(), effect.radius() * 2, effect.radius() * 2);
        } catch (IllegalArgumentException invalidGeometry) {
            return;
        }
        AttackSpec specification = new AttackSpec("reaction-" + reaction.id(), effect.damage(), effect.activeSeconds(),
                0, effect.element(), false, DamageType.ELEMENTAL);
        attacks.add(new Attack(++lastAttackId, parent.getSource(), specification, hitbox, null, parent.getId(), hitbox));
    }

    /**
     * Exposes current fixed fields without permitting list mutation or timer updates.
     * @return immutable active-field membership snapshot
     */
    public List<ElementField> getElementFields() {
        return List.copyOf(fields);
    }

    /**
     * Applies control/status or creates a fixed field after confirmed direct damage.
     * State is latched here; movement owners resolve displacement outside this settlement.
     * @param parent direct invocation with a consumed reaction event
     * @param target reacting receiver
     * @param reaction ordered rule
     */
    private void applyReaction(Attack parent, Combatant target, ElementRules.Reaction reaction) {
        if (reaction.kind() == ElementRules.Kind.SWIRL) {
            if (fields.size() >= ReactionTuning.MAX_FIELDS) { return; }
            CollisionBox body = target.getHurtbox();
            var definition = new ElementField.Definition(reaction.attachedElement(), ReactionTuning.FIELD_DAMAGE,
                    ReactionTuning.FIELD_RADIUS, ReactionTuning.FIELD_SECONDS, ReactionTuning.FIELD_INTERVAL);
            try {
                fields.add(new ElementField(definition, body.x() + body.width() / 2,
                        body.y() + body.height() / 2, parent.getSource(), parent.getId()));
            } catch (IllegalArgumentException unrepresentableGeometry) {
                // Optional geometry cannot undo already confirmed damage or consumed attachment.
            }
            return;
        }
        if (!target.getEntity().isAlive()) { return; }
        CombatStatus status = target.getStatus();
        switch (reaction.kind()) {
            case OVERLOAD -> {
                CollisionBox from = parent.getOrigin();
                CollisionBox to = target.getHurtbox();
                double dx = to.x() + to.width() / 2 - (from.x() + from.width() / 2);
                double dy = to.y() + to.height() / 2 - (from.y() + from.height() / 2);
                double length = Math.hypot(dx, dy);
                status.overload(length == 0 || !Double.isFinite(length) ? 1 : dx / length,
                        length == 0 || !Double.isFinite(length) ? 0 : dy / length);
            }
            case ELECTRO_CHARGED -> status.charge(parent.getSource(), parent.getId());
            case FREEZE -> status.freeze();
            case SUPERCONDUCT -> status.superconduct();
            case DAMAGE -> { } // SWIRL was handled and returned before target-status dispatch.
        }
    }

    /**
     * Advances previously created reaction schedules before this step's direct hit snapshot.
     * New reactions start their clocks next update; zero time never emits another periodic tick.
     * Charged pulses settle only their afflicted target, never chain to neighbours. Ground ticks
     * enter the ordinary attack list and use the shared overlap/occlusion gate for all recipients.
     * @param delta validated elapsed time
     * @return this step's ground pulses to retire immediately after their one shared hit sample
     */
    private List<Attack> advanceReactions(double delta) {
        List<Attack> fieldTicks = new ArrayList<>();
        cleanupReactions();
        for (Combatant target : cooldowns.keySet()) {
            CombatStatus status = target.getStatus();
            int due = status.advance(delta);
            Combatant source = status.chargedSource();
            for (int i = 0; i < due && target.getEntity().isAlive(); i++) {
                CollisionBox box = target.getHurtbox();
                Attack pulse = reactionPulse(source, status.chargedParent(), "reaction-electro-charged",
                        ElementType.LIGHTNING, ReactionTuning.CHARGED_DAMAGE, box);
                if (pulse != null) {
                    if (canHit(pulse, target)) { deliverDamage(pulse, target); }
                    pulse.cancel();
                }
            }
            if (status.getChargedRemaining() == 0) { status.clearCharge(); }
        }
        for (ElementField field : fields) {
            int due = field.advance(delta);
            for (int i = 0; i < due; i++) {
                var definition = field.getDefinition();
                Attack pulse = reactionPulse(field.getSource(), field.getParentAttackId(), "reaction-field",
                        definition.element(), definition.damage(), field.getBounds());
                if (pulse != null) { attacks.add(pulse); fieldTicks.add(pulse); }
            }
        }
        fields.removeIf(field -> !field.isActive());
        return fieldTicks;
    }

    /**
     * Builds one bounded non-blockable, non-recursive elemental reaction invocation.
     * @param source original direct attacker
     * @param parent direct attack ID
     * @param label diagnostic type
     * @param element damage tag, never attached by this child
     * @param damage base damage
     * @param box fixed effect centre and geometry
     * @return child, or null for dead/detached source, exhausted IDs or live capacity
     */
    private Attack reactionPulse(Combatant source, long parent, String label, ElementType element,
            int damage, CollisionBox box) {
        if (source == null || !source.getEntity().isAlive() || !cooldowns.containsKey(source)
                || lastAttackId == Long.MAX_VALUE
                || attacks.stream().filter(a -> a.isActive() && a.isDerived()).count() >= MAX_DERIVED_ATTACKS) {
            return null;
        }
        return new Attack(++lastAttackId, source, new AttackSpec(label, damage, ReactionTuning.PULSE_ACTIVE,
                0, element, false, DamageType.ELEMENTAL), box, null, parent, box);
    }

    /** Clears terminal actor statuses and source-owned effects without retaining old-map entities. */
    private void cleanupReactions() {
        for (Combatant actor : cooldowns.keySet()) {
            CombatStatus status = actor.getStatus();
            Combatant source = status.chargedSource();
            if (source != null && (!source.getEntity().isAlive() || !cooldowns.containsKey(source))) {
                status.clearCharge();
            }
        }
        fields.stream().filter(field -> !field.getSource().getEntity().isAlive()
                || !cooldowns.containsKey(field.getSource())).forEach(ElementField::cancel);
        fields.removeIf(field -> !field.isActive());
    }
}
