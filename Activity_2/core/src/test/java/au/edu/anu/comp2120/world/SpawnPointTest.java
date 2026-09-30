package au.edu.anu.comp2120.world;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Validates finite, non-negative spawn anchors.
 * @author COMP2120 Assignment 3 group
 */
final class SpawnPointTest {
    /** Valid coordinates are exposed. */
    @Test void buildsWithValidCoordinates() {
        var spawn = new SpawnPoint(10, 20);
        assertEquals(10, spawn.x());
        assertEquals(20, spawn.y());
    }

    /** Negative coordinates are rejected. */
    @Test void rejectsNegativeCoordinates() {
        assertThrows(IllegalArgumentException.class, () -> new SpawnPoint(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new SpawnPoint(0, -1));
    }

    /** Non-finite coordinates are rejected. */
    @Test void rejectsNonFiniteCoordinates() {
        assertThrows(IllegalArgumentException.class, () -> new SpawnPoint(Double.NaN, 0));
        assertThrows(IllegalArgumentException.class, () -> new SpawnPoint(0, Double.POSITIVE_INFINITY));
    }
}
