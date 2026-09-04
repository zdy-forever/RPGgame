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
     * Applies a non-negative amount of damage.
     *
     * @param damage amount of damage to apply
     */
    void takeDamage(int damage);

    /**
     * Reports whether this object can continue participating in combat.
     *
     * @return {@code true} while the object is alive
     */
    boolean isAlive();
}
