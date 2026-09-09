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
 * @author Daoyan Zhu (u7782042)
 */
public final class CombatSystem {
    /** Maximum simultaneously retained projectiles; a volley is accepted or rejected as a whole. */
    public static final int MAX_PROJECTILES = 256;
    /** Canonical registered adapters and their remaining per-source cooldowns. */
    private final Map<Combatant, Double> cooldowns = new LinkedHashMap<>();
    /** Active invocations in creation order. */
    private final List<Attack> attacks = new ArrayList<>();
    /** Last issued identifier, retained across arena clears. */
    private long lastAttackId;

    /** Creates an empty combat scope with no graphics resources. */
    public CombatSystem() {
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
     * Removes a canonical actor and cancels its active attacks. Other attacks retain their
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
        cancelAttacks(combatant);
        return true;
    }

    /**
     * Cancels a source's invocations without resetting registration, health or remaining cooldown.
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
    }

    /**
     * Attempts a new attack. A dead source or positive cooldown returns empty without mutation.
     * All arguments are validated first, even if the source is dead or cooling down.
     * New invocations are sampled by the next update, including an update with zero delta.
     * @param source canonical registered attacker
     * @param specification reusable validated attack tuning
     * @param hitbox immutable world-space geometry for this invocation
     * @return new invocation, or empty if the source cannot currently attack
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if source is not registered in this scope
     * @throws IllegalStateException if the system has exhausted its identifier range
     */
    public Optional<Attack> tryAttack(Combatant source, AttackSpec specification, CollisionBox hitbox) {
        Objects.requireNonNull(specification, "specification");
        Objects.requireNonNull(hitbox, "hitbox");
        double remainingCooldown = getCooldownRemaining(source);
        if (!source.getEntity().isAlive() || remainingCooldown > 0) {
            return Optional.empty();
        }
        if (lastAttackId == Long.MAX_VALUE) {
            throw new IllegalStateException("Attack identifier range exhausted");
        }
        Attack attack = new Attack(++lastAttackId, source, specification, hitbox);
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
     * @throws IllegalArgumentException if source, range, direction or full spawn geometry is invalid
     * @throws IllegalStateException if there are not enough remaining attack identifiers
     */
    public List<Attack> tryProjectiles(Combatant source, AttackSpec attack, ProjectileSpec flight,
            double range, double directionX, double directionY, WalkableArea area) {
        Objects.requireNonNull(attack, "attack");
        Objects.requireNonNull(flight, "flight");
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
        if (!source.getEntity().isAlive() || cooldown > 0
                || attacks.stream().filter(a -> a.isActive() && a.getProjectile() != null).count()
                + flight.count() > MAX_PROJECTILES) {
            return List.of();
        }
        if (lastAttackId > Long.MAX_VALUE - flight.count()) {
            throw new IllegalStateException("Attack identifier range exhausted");
        }
        List<Attack> volley = new ArrayList<>();
        for (Projectile shot : shots) {
            volley.add(new Attack(++lastAttackId, source, attack, origin, shot));
        }
        attacks.addAll(volley);
        cooldowns.put(source, attack.cooldownSeconds());
        return List.copyOf(volley);
    }

    /**
     * Samples currently active hitboxes against current hurtboxes, then advances time by delta.
     * The active interval includes creation and excludes expiry. Even if delta spans the whole
     * duration, the start-of-step sample is resolved once; this is not swept collision detection.
     * Repeated zero-time updates cannot repeat a hit. Hits use attack order then registration order;
     * a source killed earlier in the same step cannot deliver a later attack.
     * Projectiles instead sweep their capped flight segment against current target hurtboxes and
     * static walls, choosing the nearest hit. Walls win equal-time ties; targets tie by registration.
     * @param delta finite non-negative elapsed simulation seconds
     * @throws IllegalArgumentException if delta is negative or non-finite; no state changes occur
     */
    public void update(double delta) {
        if (!Double.isFinite(delta) || delta < 0) {
            throw new IllegalArgumentException("Combat delta must be finite and non-negative");
        }
        for (Attack attack : attacks) {
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
        attacks.removeIf(attack -> !attack.isActive());
        cooldowns.replaceAll((source, remaining) -> Math.max(0, remaining - delta));
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
     * Cancels all attacks and removes registrations/cooldowns without changing any entity health.
     * Use at scene/map transitions; callers must register the new scope's actors afterward.
     */
    public void clear() {
        attacks.forEach(Attack::cancel);
        attacks.clear();
        cooldowns.clear();
    }

    /**
     * Applies identity, faction, life, repeat-hit, and positive-area overlap rules.
     * @param attack active invocation
     * @param target registered candidate
     * @return true only for a fresh hostile living receiver whose hurtbox overlaps
     */
    private boolean canHit(Attack attack, Combatant target) {
        return isValidTarget(attack, target) && attack.getHitbox().overlaps(target.getHurtbox());
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
        target.getEntity().takeDamage(calculateDamage(attack));
        attack.recordHit(target.getEntity());
    }

    /**
     * Central damage calculation boundary. Current rules use validated raw damage unchanged;
     * future armour or modifiers belong here, not in individual character attack implementations.
     * Health remains responsible for saturating lethal damage at zero.
     * @param attack invocation that passed detection
     * @return non-negative damage to deliver through Damageable
     */
    private int calculateDamage(Attack attack) {
        return attack.getSpecification().damage();
    }
}
