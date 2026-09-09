package au.edu.anu.comp2120.world;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Checks continuous interior-entry geometry and its integration with static world bounds/walls.
 * @author Daoyan Zhu (u7782042)
 */
final class CollisionSweepTest {
    /** Common small moving body. */
    private final CollisionBox body = new CollisionBox(10, 10, 2, 2);

    /** Creates a pure geometry fixture. */
    CollisionSweepTest() {
    }

    /**
     * Positive/negative/cardinal/diagonal segments return the first interior entry fraction.
     * @param dx horizontal displacement
     * @param dy vertical displacement
     * @param x target x
     * @param y target y
     * @param expected normalized first entry
     */
    @ParameterizedTest
    @CsvSource({"10,0,17,10,0.5", "-10,0,3,10,0.5", "0,10,10,17,0.5",
            "0,-10,10,3,0.5", "10,10,17,17,0.5", "-10,-10,3,3,0.5",
            "10,-10,17,3,0.5", "-10,10,3,17,0.5"})
    void findsStraightPathEntry(double dx, double dy, double x, double y, double expected) {
        assertEquals(expected, CollisionSweep.entry(body, dx, dy, new CollisionBox(x, y, 2, 2)));
    }

    /** Tangents, point-only corner contact, moving away and endpoint-only contact are not hits. */
    @Test
    void excludesGrazingAndEndpointOnlyContact() {
        assertEquals(Double.POSITIVE_INFINITY, CollisionSweep.entry(body, 10, 0, new CollisionBox(17, 12, 2, 2)));
        assertEquals(Double.POSITIVE_INFINITY, CollisionSweep.entry(body, 10, 10, new CollisionBox(16, 12, 2, 2)));
        assertEquals(Double.POSITIVE_INFINITY, CollisionSweep.entry(body, -10, 0, new CollisionBox(12, 10, 2, 2)));
        assertEquals(Double.POSITIVE_INFINITY, CollisionSweep.entry(body, 10, 0, new CollisionBox(22, 10, 2, 2)));
        assertEquals(Double.POSITIVE_INFINITY, CollisionSweep.entry(body, 0, 0, new CollisionBox(12, 10, 2, 2)));
        assertEquals(0, CollisionSweep.entry(body, 1, 0, new CollisionBox(12, 10, 2, 2)));
        assertEquals(0, CollisionSweep.entry(body, 0, 0, body));
    }

    /** World sweep stops at the first obstacle but does not change ordinary actor movement rules. */
    @Test
    void mapCombinesWallsAndWorldBounds() {
        GameMap map = TestMaps.rectangle(30, 30, List.of(new CollisionBox(17, 0, 0.1, 20)));
        assertEquals(0.5, map.firstBlock(body, 10, 0));
        assertEquals(0.5, map.firstBlock(body, -20, 0));
        assertEquals(0.9, map.firstBlock(body, 0, 20));
        assertEquals(Double.POSITIVE_INFINITY, map.firstBlock(body, 0, 0));
        assertEquals(Double.POSITIVE_INFINITY, map.firstBlock(body, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> map.firstBlock(new CollisionBox(-1, 0, 2, 2), 1, 0));
        assertThrows(IllegalArgumentException.class, () -> map.firstBlock(body, Double.NaN, 0));
        assertThrows(NullPointerException.class, () -> map.firstBlock(null, 0, 0));
    }

    /** Invalid arguments fail explicitly instead of propagating NaN collision times. */
    @Test
    void rejectsInvalidSweepArguments() {
        assertThrows(IllegalArgumentException.class, () -> CollisionSweep.entry(body, Double.NaN, 0, body));
        assertThrows(IllegalArgumentException.class, () -> CollisionSweep.entry(body, 0, Double.POSITIVE_INFINITY, body));
        assertThrows(NullPointerException.class, () -> CollisionSweep.entry(null, 0, 0, body));
        assertThrows(NullPointerException.class, () -> CollisionSweep.entry(body, 0, 0, null));
    }
}
