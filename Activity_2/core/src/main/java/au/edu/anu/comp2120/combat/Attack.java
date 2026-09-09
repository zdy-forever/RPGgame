package au.edu.anu.comp2120.combat;

import au.edu.anu.comp2120.world.CollisionBox;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * One system-created attack invocation with a fixed hitbox and a lifetime hit ledger.
 * Public access is observational; only the owning combat system advances or resolves it.
 * Identity, rather than type, distinguishes repeated attacks using the same specification.
 *
 * @author Daoyan Zhu (u7782042)
 */
public final class Attack {
    /** Monotonic identifier within the owning system, never reset by clearing a map. */
    private final long id;
    /** Registered source adapter retained for the lifetime of this invocation. */
    private final Combatant source;
    /** Validated reusable tuning. */
    private final AttackSpec specification;
    /** Fixed world-space attack geometry sampled at creation, not tied to a sprite or source motion. */
    private final CollisionBox hitbox;
    /** Receivers already settled by this invocation, tracked by object identity. */
    private final Set<Damageable> hitEntities = Collections.newSetFromMap(new IdentityHashMap<>());
    /** Time remaining in the active interval; zero means expired or cancelled. */
    private double remainingSeconds;

    /**
     * Creates an invocation after validation by the owning system.
     * @param id system-issued positive identifier
     * @param source validated registered source
     * @param specification validated tuning
     * @param hitbox validated immutable hitbox
     */
    Attack(long id, Combatant source, AttackSpec specification, CollisionBox hitbox) {
        this.id = id;
        this.source = source;
        this.specification = specification;
        this.hitbox = hitbox;
        remainingSeconds = specification.activeSeconds();
    }

    /**
     * Returns the invocation identifier.
     * @return positive identifier unique within the owning system
     */
    public long getId() {
        return id;
    }

    /**
     * Returns the source adapter.
     * @return attack source
     */
    public Combatant getSource() {
        return source;
    }

    /**
     * Returns immutable attack tuning, including its type and raw damage.
     * @return reusable specification
     */
    public AttackSpec getSpecification() {
        return specification;
    }

    /**
     * Returns immutable detection geometry, also suitable for debug outlines.
     * @return fixed world-space hitbox
     */
    public CollisionBox getHitbox() {
        return hitbox;
    }

    /**
     * Returns the remaining active time, not wall-clock time.
     * @return finite non-negative seconds
     */
    public double getRemainingSeconds() {
        return remainingSeconds;
    }

    /**
     * Reports whether this invocation is still active.
     * @return true while remaining duration is positive
     */
    public boolean isActive() {
        return remainingSeconds > 0;
    }

    /**
     * Checks receiver identity against this invocation's lifetime ledger.
     * @param entity receiver to check
     * @return true if this invocation already settled that receiver
     */
    boolean hasHit(Damageable entity) {
        return hitEntities.contains(entity);
    }

    /**
     * Records a receiver after successful damage delivery, including zero damage.
     * @param entity receiver that accepted damage
     */
    void recordHit(Damageable entity) {
        hitEntities.add(entity);
    }

    /**
     * Ages an invocation after hit sampling, clamping at zero without accumulating a global clock.
     * @param delta validated finite non-negative elapsed seconds
     */
    void advance(double delta) {
        remainingSeconds = Math.max(0, remainingSeconds - delta);
    }

    /** Cancels this invocation so retained public references also report expiration. */
    void cancel() {
        remainingSeconds = 0;
    }
}
