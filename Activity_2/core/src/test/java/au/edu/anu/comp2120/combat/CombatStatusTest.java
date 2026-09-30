package au.edu.anu.comp2120.combat;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Exercises the shared per-combatant reaction/vulnerability state machine via its public
 * and package-private entry points.
 * @author COMP2120 Assignment 3 group
 */
final class CombatStatusTest {
    /** Creates a new {@code CombatStatusTest} instance. */
    CombatStatusTest() {
        // Test instances are created by JUnit.
    }

    /** Independent status per test. */
    private final CombatStatus status = new CombatStatus();

    /** A fresh status reports no active effects. */
    @Test void startsClear() {
        assertFalse(status.isFrozen());
        assertFalse(status.blocksActions());
        assertFalse(status.isInvulnerable());
        assertEquals(0, status.getFrozenRemaining());
        assertEquals(0, status.getStaggerRemaining());
        assertEquals(0, status.getResistanceRemaining());
        assertEquals(0, status.getInvulnerableRemaining());
        assertEquals(1.0, status.physicalMultiplier());
        assertEquals(0, status.getChargedRemaining());
        assertTrue(status.getPoise() > 0);
    }

    /** Poise capacity must be a positive finite value. */
    @Test void configurePoiseSetsCapacityAndCurrent() {
        status.configurePoise(50);
        assertEquals(50, status.getPoise());
        assertThrows(IllegalArgumentException.class, () -> status.configurePoise(0));
        assertThrows(IllegalArgumentException.class, () -> status.configurePoise(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> status.configurePoise(Double.POSITIVE_INFINITY));
    }

    /** Freeze keeps the longer remaining duration rather than stacking. */
    @Test void freezeRefreshesWithoutStacking() {
        status.applyFreeze(2);
        assertTrue(status.isFrozen());
        assertEquals(2, status.getFrozenRemaining());
        status.applyFreeze(1);
        assertEquals(2, status.getFrozenRemaining());
        status.applyFreeze(5);
        assertEquals(5, status.getFrozenRemaining());
        assertThrows(IllegalArgumentException.class, () -> status.applyFreeze(0));
        assertThrows(IllegalArgumentException.class, () -> status.applyFreeze(-1));
    }

    /** Stagger suppresses actions and is observable. */
    @Test void staggerBlocksActions() {
        status.applyStagger(3);
        assertTrue(status.blocksActions());
        assertEquals(3, status.getStaggerRemaining());
    }

    /** Invulnerability suppresses confirmed hits. */
    @Test void invulnerabilitySuppressesHits() {
        status.applyInvulnerability(4);
        assertTrue(status.isInvulnerable());
        assertEquals(4, status.getInvulnerableRemaining());
    }

    /** Superconduct opens a finite physical-vulnerability window that raises the multiplier. */
    @Test void superconductRaisesPhysicalMultiplier() {
        status.superconduct();
        assertTrue(status.getResistanceRemaining() > 0);
        assertTrue(status.physicalMultiplier() > 1.0);
    }

    /** Advance ages every timed effect once. */
    @Test void advanceAgesActiveState() {
        status.applyFreeze(5);
        status.applyStagger(5);
        status.applyInvulnerability(5);
        int ticks = status.advance(2);
        assertEquals(3, status.getFrozenRemaining());
        assertEquals(3, status.getStaggerRemaining());
        assertEquals(3, status.getInvulnerableRemaining());
        assertEquals(0, ticks);
    }

    /** A charge tracks its source and direct parent until cleared. */
    @Test void chargeTracksSourceAndParent() {
        status.charge(null, 7);
        assertTrue(status.getChargedRemaining() > 0);
        assertNull(status.chargedSource());
        assertEquals(7, status.chargedParent());
        assertTrue(status.advance(0.001) >= 0);
        status.clearCharge();
        assertEquals(0, status.getChargedRemaining());
    }

    /** Push queues a normalized displacement that replaces any pending one. */
    @Test void pushQueuesNormalizedDisplacement() {
        status.applyPush(3, 4, 10);
        assertEquals(6, status.getKnockbackX(), 1e-9);
        assertEquals(8, status.getKnockbackY(), 1e-9);
        status.clearKnockback();
        assertEquals(0, status.getKnockbackX(), 1e-9);
        status.applyPush(-1, 0, 5);
        assertEquals(-5, status.getKnockbackX(), 1e-9);
        assertThrows(IllegalArgumentException.class, () -> status.applyPush(0, 0, 5));
        assertThrows(IllegalArgumentException.class, () -> status.applyPush(1, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> status.applyPush(1, 1, -1));
    }

    /** Clearing resets temporary effects but keeps the configured poise capacity. */
    @Test void clearResetsTemporaryState() {
        status.applyFreeze(5);
        status.applyInvulnerability(5);
        status.configurePoise(40);
        status.clear();
        assertFalse(status.isFrozen());
        assertFalse(status.isInvulnerable());
        assertEquals(40, status.getPoise());
    }
}
