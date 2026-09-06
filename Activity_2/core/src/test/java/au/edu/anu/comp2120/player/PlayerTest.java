package au.edu.anu.comp2120.player;

import static org.junit.jupiter.api.Assertions.*;

import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.GameMap;
import au.edu.anu.comp2120.world.TestMaps;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Exercises player movement and state validation without a graphics context.
 *
 * @author Daoyan Zhu (u7782042)
 */
final class PlayerTest {
    /** Tolerance for accumulated decimal time steps. */
    private static final double EPSILON = 1e-9;
    /** Empty test arena. */
    private final GameMap map = TestMaps.rectangle(1000, 1000, List.of());

    /** Creates a movement test fixture. */
    PlayerTest() {
    }

    /**
     * Checks all four cardinal directions with elapsed time and configured speed.
     *
     * @param horizontal horizontal intent
     * @param vertical vertical intent
     * @param expectedX expected final x
     * @param expectedY expected final y
     */
    @ParameterizedTest
    @CsvSource({"1,0,250,200", "-1,0,150,200", "0,1,200,250", "0,-1,200,150"})
    void movesInFourDirections(int horizontal, int vertical, double expectedX, double expectedY) {
        Player player = player();
        player.updateMovement(horizontal, vertical, 0.5);
        assertEquals(expectedX, player.getX(), EPSILON);
        assertEquals(expectedY, player.getY(), EPSILON);
        assertEquals(100, player.getSpeed());
        assertEquals(20, player.getBounds().width());
        assertEquals(20, player.getBounds().height());
    }

    /**
     * Confirms both axes move and total speed stays unchanged for every diagonal intent.
     *
     * @param horizontal horizontal intent
     * @param vertical vertical intent
     */
    @ParameterizedTest
    @CsvSource({"1,1", "-1,1", "1,-1", "-1,-1"})
    void diagonalIntentHasNormalizedSpeed(int horizontal, int vertical) {
        Player player = player();
        player.updateMovement(horizontal, vertical, 0.5);
        assertEquals(200 + horizontal * 50 / Math.sqrt(2), player.getX(), EPSILON);
        assertEquals(200 + vertical * 50 / Math.sqrt(2), player.getY(), EPSILON);
        assertEquals(50, Math.hypot(player.getX() - 200, player.getY() - 200), EPSILON);
    }

    /** Diagonal input slides vertically when a wall prevents its horizontal component. */
    @Test
    void diagonalMovementSlidesAlongWall() {
        GameMap wallMap = TestMaps.rectangle(100, 100, List.of(new CollisionBox(40, 0, 10, 100)));
        Player player = new Player(new CollisionBox(30, 20, 10, 10), 10, wallMap);
        player.updateMovement(1, 1, 1);
        assertEquals(30, player.getX());
        assertEquals(20 + 10 / Math.sqrt(2), player.getY(), EPSILON);
        assertTrue(wallMap.isWalkable(player.getBounds()));
    }

    /** A diagonal move stops at both outer boundaries without leaving the world. */
    @Test
    void diagonalMovementStopsAtMapCorner() {
        Player player = player();
        player.updateMovement(1, 1, 1000);
        assertEquals(980, player.getX());
        assertEquals(980, player.getY());
        assertTrue(map.isWalkable(player.getBounds()));
    }

    /** Equal elapsed simulation time yields equal movement across different update rates. */
    @Test
    void movementIsTimeBased() {
        Player singleStep = player();
        Player manySteps = player();
        singleStep.updateMovement(1, 0, 1);
        for (int i = 0; i < 100; i++) {
            manySteps.updateMovement(1, 0, 0.01);
        }
        assertEquals(singleStep.getX(), manySteps.getX(), EPSILON);
    }

    /** Idle and zero-time updates preserve the current position. */
    @Test
    void idleAndZeroTimeDoNotMove() {
        Player player = player();
        CollisionBox before = player.getBounds();
        player.updateMovement(0, 0, 1);
        player.updateMovement(1, 0, 0);
        assertEquals(before, player.getBounds());
    }

    /**
     * Clamps the full player body at each map edge, including a very large finite delta.
     *
     * @param horizontal horizontal intent
     * @param vertical vertical intent
     * @param expectedX expected contact x
     * @param expectedY expected contact y
     */
    @ParameterizedTest
    @CsvSource({"1,0,980,200", "-1,0,0,200", "0,1,200,980", "0,-1,200,0"})
    void stopsAtMapBoundary(int horizontal, int vertical, double expectedX, double expectedY) {
        Player player = player();
        player.updateMovement(horizontal, vertical, 1e200);
        assertEquals(expectedX, player.getX());
        assertEquals(expectedY, player.getY());
        assertTrue(map.isWalkable(player.getBounds()));
    }

    /**
     * Invalid times fail atomically.
     *
     * @param delta invalid elapsed time
     */
    @ParameterizedTest
    @ValueSource(doubles = {-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
            Double.MAX_VALUE})
    void rejectsInvalidTimeWithoutChangingState(double delta) {
        Player player = player();
        CollisionBox before = player.getBounds();
        assertThrows(IllegalArgumentException.class, () -> player.updateMovement(1, 0, delta));
        assertEquals(before, player.getBounds());
    }

    /** Out-of-range directional intents fail before changing player state. */
    @Test
    void rejectsInvalidDirection() {
        Player player = player();
        CollisionBox before = player.getBounds();
        assertThrows(IllegalArgumentException.class, () -> player.updateMovement(2, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> player.updateMovement(0, -2, 1));
        assertEquals(before, player.getBounds());
    }

    /**
     * Invalid speeds cannot create a player.
     *
     * @param speed invalid movement speed
     */
    @ParameterizedTest
    @ValueSource(doubles = {0, -1, Double.NaN, Double.POSITIVE_INFINITY})
    void rejectsInvalidSpeed(double speed) {
        assertThrows(IllegalArgumentException.class,
                () -> new Player(new CollisionBox(20, 20, 20, 20), speed, map));
    }

    /** Blocked, outside, oversized, and null spawns are rejected. */
    @Test
    void validatesSpawn() {
        GameMap blockedMap = TestMaps.rectangle(100, 100, List.of(new CollisionBox(30, 30, 10, 10)));
        assertThrows(IllegalArgumentException.class,
                () -> new Player(new CollisionBox(25, 25, 20, 20), 100, blockedMap));
        assertThrows(IllegalArgumentException.class,
                () -> new Player(new CollisionBox(-1, 0, 20, 20), 100, map));
        assertThrows(IllegalArgumentException.class,
                () -> new Player(new CollisionBox(990, 0, 20, 20), 100, map));
        assertThrows(IllegalArgumentException.class,
                () -> new Player(new CollisionBox(0, 0, 2000, 20), 100, map));
        assertThrows(NullPointerException.class, () -> new Player(null, 100, map));
        assertThrows(NullPointerException.class,
                () -> new Player(new CollisionBox(0, 0, 20, 20), 100, null));
    }

    /** Invalid area entry leaves both position and the previous collision environment intact. */
    @Test
    void rejectsInvalidAreaEntryWithoutMutation() {
        Player player = player();
        CollisionBox before = player.getBounds();
        GameMap destination = TestMaps.rectangle(100, 100, List.of());
        assertThrows(NullPointerException.class, () -> player.enterArea(null, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> player.enterArea(destination, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> player.enterArea(destination, Double.NaN, 0));
        assertThrows(IllegalArgumentException.class, () -> player.enterArea(destination, 90, 90));
        assertEquals(before, player.getBounds());
        player.updateMovement(1, 0, 10);
        assertEquals(980, player.getX());
    }

    /**
     * Creates a player away from all outer boundaries.
     *
     * @return test player with known position, size, and speed
     */
    private Player player() {
        return new Player(new CollisionBox(200, 200, 20, 20), 100, map);
    }
}
