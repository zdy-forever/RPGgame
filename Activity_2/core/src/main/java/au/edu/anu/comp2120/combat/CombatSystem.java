package au.edu.anu.comp2120.combat;

import au.edu.anu.comp2120.world.CollisionBox;
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
        for (Attack attack : attacks) {
            if (attack.getSource() == combatant) {
                attack.cancel();
            }
        }
        attacks.removeIf(attack -> !attack.isActive());
        return true;
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
     * Samples currently active hitboxes against current hurtboxes, then advances time by delta.
     * The active interval includes creation and excludes expiry. Even if delta spans the whole
     * duration, the start-of-step sample is resolved once; this is not swept collision detection.
     * Repeated zero-time updates cannot repeat a hit. Hits use attack order then registration order;
     * a source killed earlier in the same step cannot deliver a later attack.
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
                for (Combatant target : cooldowns.keySet()) {
                    if (canHit(attack, target)) {
                        int damage = calculateDamage(attack);
                        target.getEntity().takeDamage(damage);
                        attack.recordHit(target.getEntity());
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
        Combatant source = attack.getSource();
        return source.getEntity() != target.getEntity()
                && source.getFaction() != target.getFaction()
                && target.getEntity().isAlive()
                && !attack.hasHit(target.getEntity())
                && attack.getHitbox().overlaps(target.getHurtbox());
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
