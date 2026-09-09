package au.edu.anu.comp2120.world;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Checks actual route clearance, finite searches, off-grid connectors, and built-in map navigation.
 *
 * @author Daoyan Zhu (u7782042)
 */
final class AStarPathfinderTest {
    /** Creates a graphics-independent pathfinding fixture. */
    AStarPathfinderTest() {
    }

    /** Clear off-grid targets use one straight connector with an exact stand-off distance. */
    @Test
    void takesDirectRouteAndHonoursStoppingDistance() {
        GameMap map = TestMaps.rectangle(300, 200, List.of());
        AStarPathfinder finder = new AStarPathfinder(map, 20, 20, 16);
        CollisionBox start = new CollisionBox(13, 17, 20, 20);
        CollisionBox target = new CollisionBox(113, 17, 20, 20);
        List<CollisionBox> route = finder.findPath(start, target, 40);
        assertEquals(List.of(new CollisionBox(73, 17, 20, 20)), route);
        assertEquals(List.of(start), finder.findPath(start, target, 120));
        assertEquals(List.of(target), finder.findPath(start, target, 0));
        assertThrows(UnsupportedOperationException.class, () -> route.clear());
    }

    /** A partial thin wall requires a detour; every route segment remains full-body walkable. */
    @Test
    void routesAroundWallWithOffGridEndpoints() {
        GameMap map = TestMaps.rectangle(320, 240, List.of(new CollisionBox(120, 0, 2, 160)));
        AStarPathfinder finder = new AStarPathfinder(map, 20, 20, 16);
        CollisionBox start = new CollisionBox(43, 29, 20, 20);
        CollisionBox target = new CollisionBox(203, 37, 20, 20);
        assertTrue(finder.findDirectDestination(start, target, 20).isEmpty());
        List<CollisionBox> route = finder.findPath(start, target, 20);
        assertTrue(route.stream().anyMatch(box -> box.y() >= 160));
        verifyRoute(map, finder, start, route);
        assertEquals(20, centreDistance(route.get(route.size() - 1), target), 1e-9);
        assertEquals(route, finder.findPath(start, target, 20));
    }

    /** Closed barriers cannot be crossed, even when thinner than grid spacing. */
    @Test
    void reportsNoRouteAcrossSealedThinWall() {
        GameMap map = TestMaps.rectangle(200, 100, List.of(new CollisionBox(99, 0, 1, 100)));
        AStarPathfinder finder = new AStarPathfinder(map, 10, 10, 32);
        CollisionBox start = new CollisionBox(20, 40, 10, 10);
        CollisionBox target = new CollisionBox(150, 40, 10, 10);
        assertTrue(finder.findPath(start, target, 5).isEmpty());
    }

    /** Diagonally touching free spaces cannot be joined by cutting the shared blocked corner. */
    @Test
    void forbidsDiagonalCornerCutting() {
        GameMap map = TestMaps.rectangle(100, 100, List.of(new CollisionBox(50, 0, 50, 50),
                new CollisionBox(0, 50, 50, 50)));
        AStarPathfinder finder = new AStarPathfinder(map, 10, 10, 10);
        assertTrue(finder.findPath(new CollisionBox(20, 20, 10, 10),
                new CollisionBox(70, 70, 10, 10), 0).isEmpty());
    }

    /** Clearance uses actor dimensions: a small actor fits a corridor that a larger actor cannot enter. */
    @Test
    void respectsBodySizeInNarrowPassage() {
        GameMap map = TestMaps.rectangle(240, 160, List.of(new CollisionBox(100, 0, 20, 64),
                new CollisionBox(100, 80, 20, 80)));
        AStarPathfinder small = new AStarPathfinder(map, 12, 12, 4);
        CollisionBox start = new CollisionBox(20, 66, 12, 12);
        CollisionBox target = new CollisionBox(190, 66, 12, 12);
        verifyRoute(map, small, start, small.findPath(start, target, 0));
        AStarPathfinder large = new AStarPathfinder(map, 20, 20, 8);
        assertTrue(large.findPath(new CollisionBox(20, 60, 20, 20),
                new CollisionBox(190, 60, 20, 20), 0).isEmpty());
    }

    /**
     * Routes can reach all four map corners without truncating a non-multiple final grid cell.
     * @param x destination origin x
     * @param y destination origin y
     */
    @ParameterizedTest
    @CsvSource({"0,0", "93,0", "0,77", "93,77"})
    void supportsExactMapEdges(double x, double y) {
        GameMap map = TestMaps.rectangle(113, 97, List.of(new CollisionBox(45, 30, 20, 20)));
        AStarPathfinder finder = new AStarPathfinder(map, 20, 20, 16);
        CollisionBox start = new CollisionBox(0, 60, 20, 20);
        CollisionBox target = new CollisionBox(x, y, 20, 20);
        List<CollisionBox> route = finder.findPath(start, target, 0);
        verifyRoute(map, finder, start, route);
        assertEquals(target, route.get(route.size() - 1));
    }

    /** A map fitting only one body produces a one-node grid, not negative extents or division by zero. */
    @Test
    void handlesSinglePlacementMap() {
        GameMap map = TestMaps.rectangle(20, 20, List.of());
        AStarPathfinder finder = new AStarPathfinder(map, 20, 20, 16);
        CollisionBox body = new CollisionBox(0, 0, 20, 20);
        assertEquals(List.of(body), finder.findPath(body, body, 0));
    }

    /** Both alternating walls in the actual Training map can be navigated with a 28-unit body. */
    @Test
    void navigatesActualTrainingMap() {
        GameMap map = MapCatalog.createMaps().stream().filter(level -> level.getId().equals(MapCatalog.TRAINING))
                .findFirst().orElseThrow();
        AStarPathfinder finder = new AStarPathfinder(map, 28, 28, 16);
        CollisionBox start = new CollisionBox(80, 80, 28, 28);
        CollisionBox target = new CollisionBox(680, 480, 28, 28);
        List<CollisionBox> route = finder.findPath(start, target, 40);
        verifyRoute(map, finder, start, route);
        assertTrue(route.stream().anyMatch(body -> body.y() >= 380));
        assertTrue(route.stream().anyMatch(body -> body.x() >= 264 && body.right() <= 480 && body.top() <= 220));
    }

    /** Invalid/outside targets fail without a route; malformed query parameters are explicit errors. */
    @Test
    void validatesQueriesAndBoundsSearchAllocation() {
        GameMap map = TestMaps.rectangle(200, 100, List.of(new CollisionBox(90, 20, 20, 20)));
        AStarPathfinder finder = new AStarPathfinder(map, 20, 20, 16);
        CollisionBox start = new CollisionBox(0, 0, 20, 20);
        assertTrue(finder.findPath(start, new CollisionBox(-20, 0, 20, 20), 0).isEmpty());
        assertTrue(finder.findPath(start, new CollisionBox(90, 20, 20, 20), 0).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> finder.findPath(new CollisionBox(0, 0, 10, 10), start, 0));
        assertThrows(IllegalArgumentException.class, () -> finder.findPath(new CollisionBox(-1, 0, 20, 20), start, 0));
        assertThrows(NullPointerException.class, () -> finder.findPath(null, start, 0));
        assertThrows(NullPointerException.class, () -> finder.findPath(start, null, 0));
        assertThrows(NullPointerException.class, () -> new AStarPathfinder(null, 20, 20, 16));
        assertThrows(IllegalArgumentException.class, () -> new AStarPathfinder(map, 201, 20, 16));
        assertThrows(IllegalArgumentException.class, () -> new AStarPathfinder(map, 20, 20, 0.001));
    }

    /**
     * Invalid dimensions, spacing, and stopping distance are rejected before searching.
     * @param invalid invalid non-positive or non-finite value
     */
    @ParameterizedTest
    @ValueSource(doubles = {-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsInvalidNumericArguments(double invalid) {
        GameMap map = TestMaps.rectangle(100, 100, List.of());
        CollisionBox body = new CollisionBox(0, 0, 20, 20);
        assertThrows(IllegalArgumentException.class, () -> new AStarPathfinder(map, invalid, 20, 16));
        assertThrows(IllegalArgumentException.class, () -> new AStarPathfinder(map, 20, invalid, 16));
        assertThrows(IllegalArgumentException.class, () -> new AStarPathfinder(map, 20, 20, invalid));
        AStarPathfinder finder = new AStarPathfinder(map, 20, 20, 16);
        assertThrows(IllegalArgumentException.class, () -> finder.findPath(body, body, invalid));
    }

    /**
     * Verifies both waypoint occupancy and the entire movement corridor, not only the final position.
     * @param map static map
     * @param finder matching navigation grid
     * @param start initial body
     * @param route expected non-empty route
     */
    private void verifyRoute(GameMap map, AStarPathfinder finder, CollisionBox start, List<CollisionBox> route) {
        assertFalse(route.isEmpty());
        CollisionBox previous = start;
        for (CollisionBox body : route) {
            assertTrue(map.isWalkable(body));
            assertTrue(finder.hasClearCorridor(previous, body));
            assertEquals(body, map.move(previous, body.x() - previous.x(), body.y() - previous.y()));
            previous = body;
        }
    }

    /**
     * Measures final stand-off distance for assertions.
     * @param from first body
     * @param to second body
     * @return centre distance
     */
    private double centreDistance(CollisionBox from, CollisionBox to) {
        return Math.hypot(from.x() + from.width() / 2 - to.x() - to.width() / 2,
                from.y() + from.height() / 2 - to.y() - to.height() / 2);
    }
}
