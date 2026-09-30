package au.edu.anu.comp2120.world;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Validates immutable portal definitions and their identifier rules.
 * @author COMP2120 Assignment 3 group
 */
final class PortalTest {
    /** A valid trigger rectangle for tests. */
    private final CollisionBox trigger = new CollisionBox(0, 0, 10, 10);

    /** A valid portal exposes every component. */
    @Test void buildsWithValidData() {
        var portal = new Portal("p1", "hub", trigger, "battle-training", "entry", true);
        assertEquals("p1", portal.id());
        assertEquals("hub", portal.sourceMapId());
        assertEquals(trigger, portal.trigger());
        assertEquals("battle-training", portal.destinationMapId());
        assertEquals("entry", portal.destinationSpawnId());
        assertTrue(portal.enabled());
    }

    /** Any null component is rejected. */
    @Test void rejectsNulls() {
        assertThrows(NullPointerException.class, () -> new Portal(null, "m", trigger, "d", "s", true));
        assertThrows(NullPointerException.class, () -> new Portal("i", null, trigger, "d", "s", true));
        assertThrows(NullPointerException.class, () -> new Portal("i", "m", null, "d", "s", true));
        assertThrows(NullPointerException.class, () -> new Portal("i", "m", trigger, null, "s", true));
        assertThrows(NullPointerException.class, () -> new Portal("i", "m", trigger, "d", null, true));
    }

    /** Any blank identifier is rejected. */
    @Test void rejectsBlankIds() {
        assertThrows(IllegalArgumentException.class, () -> new Portal(" ", "m", trigger, "d", "s", true));
        assertThrows(IllegalArgumentException.class, () -> new Portal("i", "", trigger, "d", "s", true));
        assertThrows(IllegalArgumentException.class, () -> new Portal("i", "m", trigger, " ", "s", true));
        assertThrows(IllegalArgumentException.class, () -> new Portal("i", "m", trigger, "d", " ", true));
    }

    /** A disabled portal reports as not enabled. */
    @Test void disabledPortalReportsNotEnabled() {
        var portal = new Portal("p2", "hub", trigger, "battle-training", "entry", false);
        assertFalse(portal.enabled());
    }
}
