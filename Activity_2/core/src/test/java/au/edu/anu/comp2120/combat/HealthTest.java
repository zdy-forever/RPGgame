package au.edu.anu.comp2120.combat;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Checks hit-point invariants, overflow protection, and explicit revival without graphics.
 *
 * @author Daoyan Zhu (u7782042)
 */
final class HealthTest {
    /** Creates a health-model test fixture. */
    HealthTest() {
    }

    /**
     * Accepts the full positive integer range and starts alive at full health.
     *
     * @param maximum valid capacity, including both endpoints
     */
    @ParameterizedTest
    @ValueSource(ints = {1, 100, Integer.MAX_VALUE})
    void startsAtFullHealth(int maximum) {
        Health health = new Health(maximum);
        assertEquals(maximum, health.getMaxHp());
        assertEquals(maximum, health.getCurrentHp());
        assertTrue(health.isAlive());
        assertFalse(health.isDead());
    }

    /**
     * Rejects capacities that could not represent a living entity.
     *
     * @param maximum invalid capacity
     */
    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void rejectsInvalidMaximum(int maximum) {
        assertThrows(IllegalArgumentException.class, () -> new Health(maximum));
    }

    /** Ordinary and repeated damage reduce current HP without changing the maximum. */
    @Test
    void takesRepeatedDamage() {
        Health health = new Health(100);
        health.takeDamage(25);
        assertEquals(75, health.getCurrentHp());
        health.takeDamage(10);
        assertEquals(65, health.getCurrentHp());
        assertEquals(100, health.getMaxHp());
        assertTrue(health.isAlive());
        assertFalse(health.isDead());
    }

    /**
     * Lethal and overkill damage stop at zero, including repeated damage after death.
     *
     * @param damage lethal damage amount
     */
    @ParameterizedTest
    @ValueSource(ints = {100, 101, Integer.MAX_VALUE})
    void lethalDamageCannotUnderflow(int damage) {
        Health health = new Health(100);
        health.takeDamage(damage);
        assertEquals(0, health.getCurrentHp());
        assertFalse(health.isAlive());
        assertTrue(health.isDead());
        health.takeDamage(Integer.MAX_VALUE);
        assertEquals(0, health.getCurrentHp());
        assertEquals(100, health.getMaxHp());
    }

    /** Healing reports the actual restored amount and never exceeds full HP. */
    @Test
    void healsOnlyMissingHitPoints() {
        Health health = new Health(100);
        health.takeDamage(60);
        assertEquals(20, health.heal(20));
        assertEquals(60, health.getCurrentHp());
        assertEquals(40, health.heal(80));
        assertEquals(100, health.getCurrentHp());
        assertEquals(0, health.heal(Integer.MAX_VALUE));
        assertEquals(100, health.getCurrentHp());
        assertTrue(health.isAlive());
    }

    /** Maximum-size healing cannot overflow even when current HP is already near the limit. */
    @Test
    void handlesMaximumIntegerOperationsWithoutOverflow() {
        Health health = new Health(Integer.MAX_VALUE);
        health.takeDamage(1);
        assertEquals(1, health.heal(Integer.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE, health.getCurrentHp());
        health.takeDamage(Integer.MAX_VALUE - 1);
        assertEquals(1, health.getCurrentHp());
        assertEquals(Integer.MAX_VALUE - 1, health.heal(Integer.MAX_VALUE));
        health.takeDamage(Integer.MAX_VALUE);
        assertTrue(health.isDead());
        health.restoreFull();
        assertEquals(Integer.MAX_VALUE, health.getCurrentHp());
    }

    /**
     * Zero damage and healing do nothing at full, injured, and dead states.
     *
     * @param damage prior damage selecting the health state
     */
    @ParameterizedTest
    @ValueSource(ints = {0, 30, 100})
    void zeroOperationsPreserveState(int damage) {
        Health health = new Health(100);
        health.takeDamage(damage);
        int before = health.getCurrentHp();
        health.takeDamage(0);
        assertEquals(0, health.heal(0));
        assertEquals(before, health.getCurrentHp());
        assertEquals(before > 0, health.isAlive());
    }

    /**
     * Negative input fails before mutation, even for dead/full entities or the minimum integer.
     *
     * @param invalid rejected damage and healing amount
     */
    @ParameterizedTest
    @ValueSource(ints = {-1, -100, Integer.MIN_VALUE})
    void rejectsInvalidOperationsAtomically(int invalid) {
        for (int damage : new int[] {0, 30, 100}) {
            Health health = new Health(100);
            health.takeDamage(damage);
            int before = health.getCurrentHp();
            assertThrows(IllegalArgumentException.class, () -> health.takeDamage(invalid));
            assertThrows(IllegalArgumentException.class, () -> health.heal(invalid));
            assertEquals(before, health.getCurrentHp());
            assertEquals(100, health.getMaxHp());
            assertEquals(before > 0, health.isAlive());
            assertEquals(before == 0, health.isDead());
        }
    }

    /** Ordinary healing does not revive; explicit restoration does, repeatedly and predictably. */
    @Test
    void requiresExplicitRestorationToRevive() {
        Health health = new Health(1);
        health.takeDamage(1);
        assertEquals(0, health.heal(Integer.MAX_VALUE));
        assertEquals(0, health.getCurrentHp());
        assertTrue(health.isDead());
        health.restoreFull();
        assertEquals(1, health.getCurrentHp());
        assertTrue(health.isAlive());
        assertFalse(health.isDead());
        health.restoreFull();
        assertEquals(1, health.getCurrentHp());
        health.takeDamage(1);
        assertTrue(health.isDead());
    }

    /** Reset is also useful for living, injured entities and preserves capacity. */
    @Test
    void restoresLivingEntityToFullHealth() {
        Health health = new Health(100);
        health.takeDamage(99);
        health.restoreFull();
        assertEquals(100, health.getCurrentHp());
        assertEquals(100, health.getMaxHp());
        assertTrue(health.isAlive());
    }
}
