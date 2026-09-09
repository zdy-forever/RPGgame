package au.edu.anu.comp2120.combat;

import au.edu.anu.comp2120.world.CollisionBox;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * One system-created attack invocation with a lifetime hit ledger and fixed or projectile geometry.
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
    /** Initial/fixed geometry; projectile invocations expose their live flight geometry instead. */
    private final CollisionBox hitbox;
    /** Optional straight flight owned by the same pipeline; null for unchanged area attacks. */
    private final Projectile projectile;
    /** Receivers already settled by this invocation, tracked by object identity. */
    private final Set<Damageable> hitEntities = Collections.newSetFromMap(new IdentityHashMap<>());
    /** Fixed-area active time; projectile invocations use their own flight lifetime instead. */
    private double remainingSeconds;

    /**
     * Creates an invocation after validation by the owning system.
     * @param id system-issued positive identifier
     * @param source validated registered source
     * @param specification validated tuning
     * @param hitbox validated immutable hitbox
     */
    Attack(long id, Combatant source, AttackSpec specification, CollisionBox hitbox) {
        this(id, source, specification, hitbox, null);
    }

    /**
     * Creates a fixed or projectile-backed invocation after pipeline validation.
     * @param id unique positive ID
     * @param source canonical registered source
     * @param specification damage and timing tuning
     * @param hitbox initial world geometry
     * @param projectile flight model, or null for an area attack
     */
    Attack(long id, Combatant source, AttackSpec specification, CollisionBox hitbox, Projectile projectile) {
        this.id = id;
        this.source = source;
        this.specification = specification;
        this.hitbox = hitbox;
        this.projectile = projectile;
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
     * Returns immutable current detection geometry, also suitable for debug outlines or arrow bodies.
     * @return current projectile body or fixed area hitbox
     */
    public CollisionBox getHitbox() {
        return projectile == null ? hitbox : projectile.getBounds();
    }

    /**
     * Exposes projectile observations without granting movement or damage mutation.
     * @return flight model, or null for a fixed area attack
     */
    public Projectile getProjectile() {
        return projectile;
    }

    /**
     * Returns remaining area active time or projectile flight lifetime, not wall-clock time.
     * A projectile's flight time is independent of its specification's short launch window.
     * @return finite non-negative seconds
     */
    public double getRemainingSeconds() {
        return projectile == null ? remainingSeconds : projectile.getRemainingSeconds();
    }

    /**
     * Reports whether this invocation is still active.
     * @return true while remaining duration is positive
     */
    public boolean isActive() {
        return projectile == null ? remainingSeconds > 0 : projectile.isActive();
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
        if (projectile != null) {
            projectile.cancel();
        }
    }
}
