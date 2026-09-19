package au.edu.anu.comp2120.enemy;

import au.edu.anu.comp2120.combat.Combatant;
import au.edu.anu.comp2120.combat.Damageable;
import au.edu.anu.comp2120.combat.Faction;
import au.edu.anu.comp2120.combat.Health;
import au.edu.anu.comp2120.world.CollisionBox;
import java.util.Objects;

/**
 * Stationary, non-attacking practice receiver that counts damage delivered by CombatSystem.
 * Shared Health validates and absorbs each hit, then immediately refills for uninterrupted practice.
 * It never moves, submits attacks, switches maps or grants rewards from a damage callback.
 * Positive final damage includes reactions; misses, blocked hits and zero damage do not score.
 * @author Daoyan Zhu (u7782042)
 */
public final class TrainingDummy implements Damageable {
    /** Full integer capacity lets every valid single delivered hit be counted without clipping. */
    private final Health health = new Health(Integer.MAX_VALUE);
    /** Canonical hostile receiver with fixed geometry and no AI controller. */
    private final Combatant combatant;
    /** Saturating sum of positive delivered HP loss in the current practice scope. */
    private long totalDamage;
    /** Saturating number of positive damage deliveries, including finite reaction ticks. */
    private long hitCount;
    /** Most recent positive final damage delivery. */
    private int lastDamage;

    /**
     * Creates a practice target; the encounter coordinator owns registration and placement validation.
     * @param bounds fixed world hurtbox, independent of presentation
     * @throws NullPointerException if bounds is null
     */
    public TrainingDummy(CollisionBox bounds) {
        combatant = new Combatant(this, Faction.ENEMY, Objects.requireNonNull(bounds, "bounds"));
    }

    /**
     * Counts validated damage after the shared pipeline has calculated reactions and resistance.
     * Immediate refill is local Health state only, with no combat or travel re-entry.
     * @param damage non-negative final damage delivered by the shared engine
     * @throws IllegalArgumentException if damage is negative
     */
    @Override
    public void takeDamage(int damage) {
        int before = health.getCurrentHp();
        health.takeDamage(damage);
        int actual = before - health.getCurrentHp();
        if (actual > 0) {
            lastDamage = actual;
            totalDamage += Math.min(actual, Long.MAX_VALUE - totalDamage);
            hitCount += hitCount < Long.MAX_VALUE ? 1 : 0;
        }
        health.restoreFull();
    }

    /**
     * Remains available after each hit because practice replenishes shared Health.
     * @return current shared Health life state
     */
    @Override
    public boolean isAlive() { return health.isAlive(); }

    /**
     * Exposes the receiver for registration and read-only presentation.
     * @return canonical practice adapter
     */
    public Combatant getCombatant() { return combatant; }

    /**
     * Reads accumulated final damage in this practice scope.
     * @return non-negative total, saturating at Long.MAX_VALUE
     */
    public long getTotalDamage() { return totalDamage; }

    /**
     * Reads the number of positive damage deliveries, not button presses or projectiles launched.
     * @return non-negative hit count
     */
    public long getHitCount() { return hitCount; }

    /**
     * Reads the last positive final hit.
     * @return damage, or zero before the first scored hit
     */
    public int getLastDamage() { return lastDamage; }
}
