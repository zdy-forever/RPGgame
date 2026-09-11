package au.edu.anu.comp2120.world;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests exact static segment obstruction separately from projectile body sweeps.
 * @author Daoyan Zhu (u7782042)
 */
final class WorldOcclusionTest {
    /** Solid open-interior square with explicit testable edges. */
    private final CollisionBox wall = new CollisionBox(40, 40, 20, 20);
    /** Bounded collision-only arena. */
    private final GameMap map = TestMaps.rectangle(100, 100, List.of(wall));

    /** Creates the pure geometry fixture. */
    WorldOcclusionTest() {
    }

    /**
     * Slab intersection is symmetric and distinguishes crossings from tangency and endpoint-only contact.
     * @param x start x
     * @param y start y
     * @param tx end x
     * @param ty end y
     * @param clear expected visibility
     */
    @ParameterizedTest
    @CsvSource({"10,50,90,50,false", "50,10,50,90,false", "10,10,90,90,false",
            "10,90,90,10,false", "10,50,30,50,true", "70,50,90,50,true",
            "10,40,90,40,true", "10,60,90,60,true", "40,10,40,90,true",
            "60,10,60,90,true", "20,60,60,20,true", "10,50,40,50,true",
            "40,50,60,50,false", "40,50,50,50,false", "50,50,50,50,false",
            "40,50,40,50,true", "10,10,10,10,true", "0,0,100,0,true",
            "0,100,100,100,true", "10,50,50,50,false"})
    void segmentBoundaryRules(double x, double y, double tx, double ty, boolean clear) {
        assertEquals(clear, map.hasLineOfSight(x, y, tx, ty));
        assertEquals(clear, map.hasLineOfSight(tx, ty, x, y));
        assertEquals(!clear, CollisionSweep.segmentCrossesInterior(x, y, tx, ty, wall));
    }

    /** Subpixel walls cannot be skipped by stepping, and paths just outside do not acquire artificial thickness. */
    @Test
    void thinWallsAndNearTangencyHaveNoSamplingEpsilon() {
        GameMap thin = TestMaps.rectangle(100, 100, List.of(new CollisionBox(50, 20, 1e-8, 60)));
        assertFalse(thin.hasLineOfSight(0, 50, 100, 50));
        assertTrue(thin.hasLineOfSight(0, Math.nextDown(20.0), 100, Math.nextDown(20.0)));
        assertFalse(thin.hasLineOfSight(0, Math.nextUp(20.0), 100, Math.nextUp(20.0)));
        assertTrue(thin.hasLineOfSight(0, 20, 100, 20));
    }

    /** World bounds remain blockers even when the map has no obstacles. */
    @Test
    void emptyMapStillRejectsOutsideEndpoints() {
        GameMap empty = TestMaps.rectangle(100, 100, List.of());
        assertTrue(empty.hasLineOfSight(0, 0, 100, 100));
        assertFalse(empty.hasLineOfSight(-1, 0, 10, 10));
        assertFalse(empty.hasLineOfSight(0, 0, 101, 10));
        assertFalse(empty.hasLineOfSight(0, 0, 10, -1));
        assertFalse(empty.hasLineOfSight(0, 101, 10, 10));
    }

    /** Non-finite coordinates and unrepresentable displacements fail explicitly instead of leaking a hit. */
    @Test
    void invalidGeometryIsRejected() {
        for (double bad : new double[] {Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> map.hasLineOfSight(bad, 0, 1, 1));
            assertThrows(IllegalArgumentException.class, () -> map.hasLineOfSight(0, bad, 1, 1));
            assertThrows(IllegalArgumentException.class, () -> map.hasLineOfSight(0, 0, bad, 1));
            assertThrows(IllegalArgumentException.class, () -> map.hasLineOfSight(0, 0, 1, bad));
        }
        assertThrows(IllegalArgumentException.class, () -> CollisionSweep.segmentCrossesInterior(
                -Double.MAX_VALUE, 0, Double.MAX_VALUE, 0, wall));
        assertThrows(NullPointerException.class, () -> CollisionSweep.segmentCrossesInterior(0, 0, 1, 1, null));
    }
}
