package au.edu.anu.comp2120.enemy;

import au.edu.anu.comp2120.combat.Damageable;
import au.edu.anu.comp2120.combat.Health;

/**
 * Hostile entity owning shared health. EnemyAI composes this model with movement and decisions;
 * health and damage remain independent of AI, geometry, and rendering.
 *
 * @author Daoyan Zhu (u7782042)
 */
public final class Enemy implements Damageable {
    /** Default HP capacity, overridable per enemy through its constructor. */
    public static final int DEFAULT_MAX_HP = 100;
    /** Independent health owned by this enemy. */
    private final Health health;

    /**
     * Creates an enemy at full default health without graphics or AI initialization.
     */
    public Enemy() {
        this(DEFAULT_MAX_HP);
    }

    /**
     * Creates an enemy with a configurable HP capacity.
     *
     * @param maxHp positive initial and maximum hit points
     * @throws IllegalArgumentException if maxHp is zero or negative
     */
    public Enemy(int maxHp) {
        health = new Health(maxHp);
    }

    /**
     * Returns this enemy's health for validated healing/restoration and display queries.
     * Mutations belong on the game-loop thread.
     *
     * @return this enemy's persistent health model
     */
    public Health getHealth() {
        return health;
    }

    /**
     * Delegates damage to the shared model without triggering AI, loot, or visual effects.
     *
     * @param damage non-negative hit points to remove
     * @throws IllegalArgumentException if damage is negative
     */
    @Override
    public void takeDamage(int damage) {
        health.takeDamage(damage);
    }

    /**
     * Reports whether the shared model has remaining HP.
     *
     * @return true while this enemy has positive HP
     */
    @Override
    public boolean isAlive() {
        return health.isAlive();
    }
}
