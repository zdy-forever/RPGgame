package au.edu.anu.comp2120.world;

import static org.junit.jupiter.api.Assertions.*;

import au.edu.anu.comp2120.player.Player;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Verifies full-body collision, swept contact, immutable geometry, and invalid map data.
 *
 * @author Daoyan Zhu (u7782042)
 */
final class GameMapTest {
    /** Creates a map test fixture. */
    GameMapTest() {
    }

    /**
     * Stops the player at the first wall face from each direction.
     *
     * @param x initial player x
     * @param y initial player y
     * @param horizontal horizontal intent
     * @param vertical vertical intent
     * @param expectedX final contact x
     * @param expectedY final contact y
     */
    @ParameterizedTest
    @CsvSource({"10,45,1,0,30,45", "80,45,-1,0,60,45",
            "45,10,0,1,45,30", "45,80,0,-1,45,60"})
    void stopsAtEveryWallFace(double x, double y, int horizontal, int vertical,
            double expectedX, double expectedY) {
        GameMap map = TestMaps.rectangle(100, 100, List.of(new CollisionBox(40, 40, 20, 20)));
        Player player = new Player(new CollisionBox(x, y, 10, 10), 100, map);
        player.updateMovement(horizontal, vertical, 10);
        assertEquals(expectedX, player.getX());
        assertEquals(expectedY, player.getY());
        assertTrue(map.isWalkable(player.getBounds()));
        CollisionBox contact = player.getBounds();
        player.updateMovement(horizontal, vertical, 1);
        assertEquals(contact, player.getBounds());
        player.updateMovement(-horizontal, -vertical, 0.1);
        assertEquals(expectedX - horizontal * 10, player.getX());
        assertEquals(expectedY - vertical * 10, player.getY());
    }

    /** Thin obstacles stop huge moves even if the endpoint is beyond the wall. */
    @Test
    void doesNotTunnelThroughThinWalls() {
        GameMap map = TestMaps.rectangle(1000, 1000, List.of(new CollisionBox(400, 0, 0.001, 1000)));
        CollisionBox start = new CollisionBox(20, 20, 10, 10);
        CollisionBox result = map.move(start, Double.MAX_VALUE, 0);
        assertEquals(390, result.x());
        assertTrue(map.isWalkable(result));
    }

    /** Partial body overlap is blocked even when its origin lies outside the wall. */
    @Test
    void checksWholeBodyAndAllowsEdgeContact() {
        GameMap map = TestMaps.rectangle(100, 100, List.of(new CollisionBox(40, 40, 20, 20)));
        assertFalse(map.isWalkable(new CollisionBox(35, 35, 10, 10)));
        assertTrue(map.isWalkable(new CollisionBox(30, 40, 10, 10)));
        CollisionBox alongEdge = map.move(new CollisionBox(10, 30, 10, 10), 70, 0);
        assertEquals(80, alongEdge.x());
        assertTrue(map.isWalkable(alongEdge));
        assertTrue(map.isWalkable(new CollisionBox(90, 90, 10, 10)));
    }

    /** Wall order does not change which obstacle stops a move. */
    @Test
    void selectsNearestObstacle() {
        CollisionBox near = new CollisionBox(40, 0, 10, 100);
        CollisionBox far = new CollisionBox(80, 0, 10, 100);
        CollisionBox start = new CollisionBox(10, 20, 10, 10);
        for (List<CollisionBox> walls : List.of(List.of(near, far), List.of(far, near))) {
            GameMap map = TestMaps.rectangle(100, 100, walls);
            assertEquals(30, map.move(start, 100, 0).x());
        }
    }

    /** Point queries reject invalid values and respect map and wall extents. */
    @Test
    void validatesPointQueries() {
        GameMap map = TestMaps.rectangle(100, 100, List.of(new CollisionBox(40, 40, 20, 20)));
        assertTrue(map.isWalkable(0, 0));
        assertFalse(map.isWalkable(40, 40));
        assertTrue(map.isWalkable(60, 60));
        assertFalse(map.isWalkable(100, 0));
        assertFalse(map.isWalkable(0, 100));
        assertFalse(map.isWalkable(-1, 20));
        assertFalse(map.isWalkable(Float.NaN, 0));
        assertFalse(map.isWalkable(0, Float.POSITIVE_INFINITY));
    }

    /** Invalid map data and movement requests fail explicitly. */
    @Test
    void rejectsInvalidMapAndMovement() {
        assertThrows(IllegalArgumentException.class, () -> TestMaps.rectangle(0, 100, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> TestMaps.rectangle(100, 100, List.of(new CollisionBox(95, 0, 10, 10))));
        assertThrows(NullPointerException.class, () -> TestMaps.rectangle(100, 100, null));
        GameMap map = TestMaps.rectangle(100, 100, List.of());
        CollisionBox body = new CollisionBox(0, 0, 10, 10);
        assertThrows(IllegalArgumentException.class, () -> map.move(body, Double.NaN, 0));
        assertThrows(IllegalArgumentException.class, () -> map.move(body, 0, Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class,
                () -> map.move(new CollisionBox(-1, 0, 10, 10), 1, 0));
        assertThrows(NullPointerException.class, () -> map.move(null, 0, 0));
    }

    /** Callers cannot mutate the world's collision list after construction. */
    @Test
    void protectsObstacleData() {
        List<CollisionBox> source = new ArrayList<>();
        source.add(new CollisionBox(40, 40, 20, 20));
        GameMap map = TestMaps.rectangle(100, 100, source);
        source.clear();
        assertEquals(1, map.getObstacles().size());
        assertThrows(UnsupportedOperationException.class, () -> map.getObstacles().clear());
    }

    /** Thousands of deterministic moves preserve full-body occupancy and dimensions. */
    @Test
    void repeatedMovesPreserveWalkability() {
        GameMap map = TestMaps.rectangle(100, 100, List.of(
                new CollisionBox(30.1, 20.2, 0.003, 50.7),
                new CollisionBox(60.3, 70.4, 25.5, 0.001)));
        CollisionBox body = new CollisionBox(10.2, 10.3, 7.1, 6.2);
        Random random = new Random(6120);
        for (int i = 0; i < 10000; i++) {
            double distance = (random.nextDouble() - 0.5) * 300;
            double vertical = (random.nextDouble() - 0.5) * 300;
            body = map.move(body, distance, vertical);
            assertTrue(map.isWalkable(body), "Invalid result: " + body);
            assertEquals(7.1, body.width());
            assertEquals(6.2, body.height());
        }
    }

    /** Vertical collision uses the resolved horizontal position, preventing corner penetration. */
    @Test
    void diagonalMovementUsesResolvedPositionForSecondAxis() {
        GameMap map = TestMaps.rectangle(100, 100, List.of(new CollisionBox(40, 40, 20, 20)));
        CollisionBox result = map.move(new CollisionBox(10, 10, 10, 10), 35, 35);
        assertEquals(45, result.x());
        assertEquals(30, result.y());
        assertTrue(map.isWalkable(result));
        CollisionBox corner = map.move(new CollisionBox(30, 30, 10, 10), 5, 5);
        assertEquals(35, corner.x());
        assertEquals(30, corner.y());
        assertTrue(map.isWalkable(corner));
    }
}
