package au.edu.anu.comp2120.combat;

/**
 * Defines the minimal contract used by combat code for objects that can take
 * damage. Combat rules should depend on this contract rather than on concrete
 * player or enemy classes.
 *
 * @author Daoyan Zhu (u7782042)
 */
public interface Damageable {

    /**
     * Applies non-negative integer damage. Zero is a no-op; negative values are rejected
     * before changing state. Implementations must not reduce HP below zero.
     *
     * @param damage amount of damage to apply
     * @throws IllegalArgumentException if damage is negative
     */
    void takeDamage(int damage);

    /**
     * Reports whether this object can continue participating in combat.
     *
     * @return {@code true} while the object is alive
     */
    boolean isAlive();
}
