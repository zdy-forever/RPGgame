package au.edu.anu.comp2120.enemy;

import static org.junit.jupiter.api.Assertions.*;

import au.edu.anu.comp2120.world.CollisionBox;
import org.junit.jupiter.api.Test;

/**
 * Checks practice-score bounds and Health reuse independently of graphics or AI.
 * @author Daoyan Zhu (u7782042)
 */
final class TrainingDummyTest {
    /** Fixed target with no movement/AI controller. */
    private final TrainingDummy dummy = new TrainingDummy(new CollisionBox(100, 100, 64, 64));

    /** Creates a fresh zero-score target. */
    TrainingDummyTest() { }

    /** Repeated maximum integer hits remain countable, alive and do not overflow a signed int total. */
    @Test
    void countsFinalDamageAndRefillsSharedHealth() {
        dummy.takeDamage(Integer.MAX_VALUE);
        dummy.takeDamage(Integer.MAX_VALUE);
        assertTrue(dummy.isAlive());
        assertEquals(2L * Integer.MAX_VALUE, dummy.getTotalDamage());
        assertEquals(2, dummy.getHitCount());
        assertEquals(Integer.MAX_VALUE, dummy.getLastDamage());
        assertEquals(new CollisionBox(100, 100, 64, 64), dummy.getCombatant().getHurtbox());
    }

    /** Rejected or harmless damage cannot create phantom score or overwrite the last positive hit. */
    @Test
    void zeroAndInvalidDamageDoNotScore() {
        dummy.takeDamage(25);
        dummy.takeDamage(0);
        assertThrows(IllegalArgumentException.class, () -> dummy.takeDamage(-1));
        assertEquals(25, dummy.getTotalDamage());
        assertEquals(1, dummy.getHitCount());
        assertEquals(25, dummy.getLastDamage());
        assertTrue(dummy.isAlive());
        assertThrows(NullPointerException.class, () -> new TrainingDummy(null));
    }
}
