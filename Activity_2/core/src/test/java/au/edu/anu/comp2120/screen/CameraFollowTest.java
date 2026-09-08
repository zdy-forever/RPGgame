package au.edu.anu.comp2120.screen;

import static org.junit.jupiter.api.Assertions.*;

import au.edu.anu.comp2120.GameSession;
import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.GameMap;
import au.edu.anu.comp2120.world.MapCatalog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Verifies pure follow/clamp mathematics and map-transition integration without graphics.
 *
 * @author Daoyan Zhu (u7782042)
 */
final class CameraFollowTest {
    /** Numerical tolerance for fractional coordinates. */
    private static final double EPSILON = 1e-9;
    /** Large test map so the camera can follow freely away from its edges. */
    private final CollisionBox map = new CollisionBox(0, 0, 1000, 800);

    /** Creates a camera mathematics test fixture. */
    CameraFollowTest() {
    }

    /** The target is the subject's centre, not its bottom-left origin. */
    @Test
    void followsSubjectCentreInMapInterior() {
        CollisionBox subject = new CollisionBox(450, 350, 40, 60);
        CollisionBox view = CameraFollow.viewFor(subject, map, 400, 200);
        assertEquals(new CollisionBox(270, 280, 400, 200), view);
        assertEquals(470, view.x() + view.width() / 2);
        assertEquals(380, view.y() + view.height() / 2);
    }

    /**
     * All four edges and corners keep a large-map viewport within the map bounds.
     *
     * @param x subject origin x
     * @param y subject origin y
     * @param expectedX clamped view origin x
     * @param expectedY clamped view origin y
     */
    @ParameterizedTest
    @CsvSource({"0,390,0,300", "980,390,600,300", "490,0,300,0", "490,780,300,600",
            "0,0,0,0", "980,0,600,0", "0,780,0,600", "980,780,600,600"})
    void clampsMapEdgesAndCorners(double x, double y, double expectedX, double expectedY) {
        CollisionBox view = CameraFollow.viewFor(new CollisionBox(x, y, 20, 20), map, 400, 200);
        assertEquals(expectedX, view.x());
        assertEquals(expectedY, view.y());
        assertTrue(view.x() >= map.x());
        assertTrue(view.y() >= map.y());
        assertTrue(view.right() <= map.right());
        assertTrue(view.top() <= map.top());
    }

    /**
     * An out-of-map target cannot force the camera beyond valid map boundaries.
     *
     * @param x out-of-map subject origin x
     * @param y out-of-map subject origin y
     * @param expectedX clamped view origin x
     * @param expectedY clamped view origin y
     */
    @ParameterizedTest
    @CsvSource({"-100,-100,0,0", "2000,2000,600,600", "-100,2000,0,600", "2000,-100,600,0"})
    void clampsTargetsOutsideMap(double x, double y, double expectedX, double expectedY) {
        CollisionBox view = CameraFollow.viewFor(new CollisionBox(x, y, 20, 20), map, 400, 200);
        assertEquals(expectedX, view.x());
        assertEquals(expectedY, view.y());
    }

    /** Maps smaller than the viewport remain centred for every player position. */
    @Test
    void smallMapIsStableWithEqualMargins() {
        CollisionBox small = new CollisionBox(0, 0, 100, 60);
        CollisionBox expected = new CollisionBox(-150, -70, 400, 200);
        for (int x = 0; x <= 90; x += 10) {
            for (int y = 0; y <= 50; y += 10) {
                CollisionBox view = CameraFollow.viewFor(new CollisionBox(x, y, 10, 10), small, 400, 200);
                assertEquals(expected, view);
                assertEquals(50, view.x() + view.width() / 2);
                assertEquals(30, view.y() + view.height() / 2);
                assertEquals(small.x() - view.x(), view.right() - small.right());
                assertEquals(small.y() - view.y(), view.top() - small.top());
            }
        }
    }

    /**
     * A small/equal axis centres independently while a larger axis continues to scroll.
     *
     * @param mapWidth test map width
     * @param mapHeight test map height
     * @param x subject x
     * @param y subject y
     * @param expectedX view x
     * @param expectedY view y
     */
    @ParameterizedTest
    @CsvSource({"100,800,20,390,-150,300", "1000,60,490,20,300,-70",
            "400,800,20,390,0,300", "1000,200,490,20,300,0", "400,200,20,20,0,0"})
    void handlesMixedAndEqualMapSizes(double mapWidth, double mapHeight, double x, double y,
            double expectedX, double expectedY) {
        CollisionBox view = CameraFollow.viewFor(new CollisionBox(x, y, 20, 20),
                new CollisionBox(0, 0, mapWidth, mapHeight), 400, 200);
        assertEquals(expectedX, view.x());
        assertEquals(expectedY, view.y());
    }

    /** Map origins are respected for large and small maps without assuming (0,0). */
    @Test
    void handlesOffsetMapOrigins() {
        CollisionBox offset = new CollisionBox(100, -50, 1000, 800);
        CollisionBox view = CameraFollow.viewFor(new CollisionBox(1080, 730, 20, 20), offset, 400, 200);
        assertEquals(new CollisionBox(700, 550, 400, 200), view);
        CollisionBox small = new CollisionBox(100, 50, 100, 60);
        assertEquals(new CollisionBox(-50, -20, 400, 200),
                CameraFollow.viewFor(new CollisionBox(110, 60, 10, 10), small, 400, 200));
    }

    /** Effective world viewport dimensions, including non-16:9 views, drive the clamp interval. */
    @Test
    void respectsSuppliedViewportDimensions() {
        CollisionBox subject = new CollisionBox(980, 780, 20, 20);
        assertEquals(new CollisionBox(360, 440, 640, 360), CameraFollow.viewFor(subject, map, 640, 360));
        assertEquals(new CollisionBox(600, 200, 400, 600), CameraFollow.viewFor(subject, map, 400, 600));
        assertEquals(new CollisionBox(-100, -100, 1200, 1000),
                CameraFollow.viewFor(subject, map, 1200, 1000));
    }

    /** Stationary targets and repeated calls never accumulate drift or timing-dependent lag. */
    @Test
    void repeatedUpdatesAreStableAndMovementFollowsImmediately() {
        CollisionBox subject = new CollisionBox(450.25, 350.75, 20, 20);
        CollisionBox expected = CameraFollow.viewFor(subject, map, 400, 200);
        for (int i = 0; i < 1000; i++) {
            assertEquals(expected, CameraFollow.viewFor(subject, map, 400, 200));
        }
        CollisionBox moved = CameraFollow.viewFor(new CollisionBox(470.25, 360.75, 20, 20), map, 400, 200);
        assertEquals(expected.x() + 20, moved.x(), EPSILON);
        assertEquals(expected.y() + 10, moved.y(), EPSILON);
    }

    /** Switching among actual maps and spawns recalculates the view without mutating gameplay state. */
    @Test
    void snapsToEveryBuiltInSpawnWithoutChangingPlayerOrSession() {
        var maps = MapCatalog.createMaps();
        GameSession session = new GameSession(maps, MapCatalog.HUB,
                GameConfig.PLAYER_WIDTH, GameConfig.PLAYER_HEIGHT, GameConfig.PLAYER_SPEED);
        for (GameMap level : maps) {
            for (String spawnId : level.getSpawnPoints().keySet()) {
                session.switchMap(level.getId(), spawnId);
                CollisionBox body = session.getPlayer().getBounds();
                long version = session.getTransitionVersion();
                CollisionBox view = CameraFollow.viewFor(body, level.getBounds(),
                        GameConfig.CAMERA_VIEW_WIDTH, GameConfig.CAMERA_VIEW_HEIGHT);
                assertTrue(view.x() >= level.getBounds().x());
                assertTrue(view.y() >= level.getBounds().y());
                assertTrue(view.right() <= level.getBounds().right());
                assertTrue(view.top() <= level.getBounds().top());
                assertTrue(body.x() >= view.x() && body.right() <= view.right());
                assertTrue(body.y() >= view.y() && body.top() <= view.top());
                assertSame(body, session.getPlayer().getBounds());
                assertSame(level, session.getCurrentMap());
                assertEquals(version, session.getTransitionVersion());
            }
        }
    }

    /** A large-map corner view is immediately replaced by a centred small-map view on arrival. */
    @Test
    void snapsFromLargeMapToSmallMapWithoutStaleClamp() {
        CameraFollow.viewFor(new CollisionBox(980, 780, 20, 20), map, 400, 200);
        CollisionBox view = CameraFollow.viewFor(new CollisionBox(10, 10, 10, 10),
                new CollisionBox(0, 0, 100, 60), 400, 200);
        assertEquals(new CollisionBox(-150, -70, 400, 200), view);
    }

    /**
     * Invalid viewport extents fail explicitly rather than producing NaN camera state.
     *
     * @param size invalid dimension
     */
    @ParameterizedTest
    @ValueSource(doubles = {0, -1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsInvalidViewportSizes(double size) {
        CollisionBox subject = new CollisionBox(20, 20, 20, 20);
        assertThrows(IllegalArgumentException.class, () -> CameraFollow.viewFor(subject, map, size, 200));
        assertThrows(IllegalArgumentException.class, () -> CameraFollow.viewFor(subject, map, 400, size));
    }

    /** Missing subject/map geometry is rejected without requiring graphics initialization. */
    @Test
    void rejectsMissingGeometry() {
        assertThrows(NullPointerException.class, () -> CameraFollow.viewFor(null, map, 400, 200));
        assertThrows(NullPointerException.class, () -> CameraFollow.viewFor(map, null, 400, 200));
    }
}
