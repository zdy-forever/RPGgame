package au.edu.anu.comp2120;

import static org.junit.jupiter.api.Assertions.*;

import au.edu.anu.comp2120.player.Player;
import au.edu.anu.comp2120.screen.GameConfig;
import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.GameMap;
import au.edu.anu.comp2120.world.MapCatalog;
import au.edu.anu.comp2120.world.MapType;
import au.edu.anu.comp2120.world.SpawnPoint;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Exercises map entry, persistent player identity, active collisions, and atomic failure.
 *
 * @author Daoyan Zhu (u7782042)
 */
final class GameSessionTest {
    /** Creates the session test fixture. */
    GameSessionTest() {
    }

    /** Every built-in named spawn can accommodate the actual configured demo player. */
    @Test
    void entersEveryBuiltInMapAndSpawnWithoutGraphics() {
        List<GameMap> maps = MapCatalog.createMaps();
        assertEquals(1, maps.stream().filter(map -> map.getType() == MapType.HUB).count());
        assertEquals(5, maps.stream().filter(map -> map.getType() == MapType.BATTLE).count());
        assertEquals(1, maps.stream().filter(map -> map.getType() == MapType.TRAINING).count());
        GameSession session = new GameSession(maps, MapCatalog.HUB,
                GameConfig.PLAYER_WIDTH, GameConfig.PLAYER_HEIGHT, GameConfig.PLAYER_SPEED);
        Player original = session.getPlayer();
        assertEquals(MapCatalog.HUB, session.getCurrentMap().getId());
        for (GameMap map : maps) {
            for (Map.Entry<String, SpawnPoint> entry : map.getSpawnPoints().entrySet()) {
                session.switchMap(map.getId(), entry.getKey());
                assertSame(map, session.getCurrentMap());
                assertSame(original, session.getPlayer());
                assertEquals(entry.getValue().x(), original.getX());
                assertEquals(entry.getValue().y(), original.getY());
                assertEquals(GameConfig.PLAYER_WIDTH, original.getBounds().width());
                assertEquals(GameConfig.PLAYER_HEIGHT, original.getBounds().height());
                assertEquals(GameConfig.PLAYER_SPEED, original.getSpeed());
                assertTrue(map.isWalkable(original.getBounds()));
                original.updateMovement(1, 1, 0.1);
                assertTrue(map.isWalkable(original.getBounds()));
            }
        }
    }

    /** Default entry and re-entry restore the declared spawn, including a return to Hub. */
    @Test
    void switchesUsingDefaultSpawnAndReentersCurrentMap() {
        GameSession session = demo();
        session.switchMap(MapCatalog.TRAINING, "far-side");
        assertEquals(680, session.getPlayer().getX());
        session.switchMap(MapCatalog.TRAINING);
        assertEquals(80, session.getPlayer().getX());
        assertEquals(80, session.getPlayer().getY());
        session.getPlayer().updateMovement(0, 1, 0.5);
        session.switchMap(MapCatalog.HUB);
        assertEquals(64, session.getPlayer().getX());
        assertEquals(64, session.getPlayer().getY());
        assertEquals(MapType.HUB, session.getCurrentMap().getType());
    }

    /** Changing maps replaces both the boundary and the obstacles governing subsequent movement. */
    @Test
    void movementUsesDestinationCollisionsAndDimensions() {
        GameMap hub = smallHub();
        GameMap battle = new GameMap("battle", "Battle", MapType.BATTLE, 200, 150,
                List.of(new CollisionBox(120, 0, 10, 150)),
                Map.of("entry", new SpawnPoint(10, 10)), "entry");
        GameSession session = new GameSession(List.of(hub, battle), "hub", 10, 10, 100);
        Player player = session.getPlayer();
        player.updateMovement(1, 0, 10);
        assertEquals(30, player.getX());
        session.switchMap("battle");
        player.updateMovement(1, 0, 10);
        assertEquals(110, player.getX());
        player.updateMovement(0, 1, 10);
        assertEquals(140, player.getY());
        session.switchMap("hub");
        player.updateMovement(0, 1, 10);
        assertEquals(90, player.getY());
        assertTrue(hub.isWalkable(player.getBounds()));
    }

    /** Unknown destinations and spawn IDs preserve every part of the active state. */
    @Test
    void missingIdsFailWithoutChangingState() {
        GameSession session = demo();
        Player player = session.getPlayer();
        CollisionBox before = player.getBounds();
        GameMap map = session.getCurrentMap();
        IllegalArgumentException missingMap = assertThrows(IllegalArgumentException.class,
                () -> session.switchMap("missing-map"));
        assertTrue(missingMap.getMessage().contains("missing-map"));
        IllegalArgumentException missingSpawn = assertThrows(IllegalArgumentException.class,
                () -> session.switchMap(MapCatalog.TRAINING, "missing-spawn"));
        assertTrue(missingSpawn.getMessage().contains("missing-spawn"));
        assertThrows(NullPointerException.class, () -> session.switchMap(null));
        assertThrows(NullPointerException.class, () -> session.switchMap(MapCatalog.TRAINING, null));
        assertSame(map, session.getCurrentMap());
        assertSame(player, session.getPlayer());
        assertEquals(before, player.getBounds());
    }

    /** A walkable point alone is insufficient when the player's full body exceeds map edges. */
    @Test
    void oversizedSpawnFailureRetainsOldCollisions() {
        GameMap hub = smallHub();
        GameMap invalidEntry = new GameMap("small", "Small", MapType.BATTLE, 100, 100, List.of(),
                Map.of("entry", new SpawnPoint(95, 95)), "entry");
        GameSession session = new GameSession(List.of(hub, invalidEntry), "hub", 10, 10, 100);
        CollisionBox before = session.getPlayer().getBounds();
        assertThrows(IllegalArgumentException.class, () -> session.switchMap("small"));
        assertSame(hub, session.getCurrentMap());
        assertEquals(before, session.getPlayer().getBounds());
        session.getPlayer().updateMovement(1, 0, 10);
        assertEquals(30, session.getPlayer().getX());
    }

    /** A spawn anchor outside a wall is rejected for entry if the body overlaps that wall. */
    @Test
    void partiallyBlockedSpawnFailureIsAtomic() {
        GameMap hub = smallHub();
        GameMap target = new GameMap("blocked", "Blocked entry", MapType.BATTLE, 100, 100,
                List.of(new CollisionBox(25, 20, 10, 10)),
                Map.of("entry", new SpawnPoint(20, 20)), "entry");
        GameSession session = new GameSession(List.of(hub, target), "hub", 10, 10, 100);
        CollisionBox before = session.getPlayer().getBounds();
        assertThrows(IllegalArgumentException.class, () -> session.switchMap("blocked"));
        assertSame(hub, session.getCurrentMap());
        assertEquals(before, session.getPlayer().getBounds());
        assertThrows(IllegalArgumentException.class,
                () -> new GameSession(List.of(target), "blocked", 10, 10, 100));
    }

    /** A registry rejects duplicate IDs, empty or missing initial maps, and null entries. */
    @Test
    void validatesRegistry() {
        GameMap hub = smallHub();
        GameMap sameId = new GameMap("hub", "Another hub", MapType.HUB, 100, 100, List.of(),
                Map.of("entry", new SpawnPoint(0, 0)), "entry");
        assertThrows(IllegalArgumentException.class,
                () -> new GameSession(List.of(hub, sameId), "hub", 10, 10, 100));
        assertThrows(IllegalArgumentException.class,
                () -> new GameSession(List.of(), "hub", 10, 10, 100));
        assertThrows(IllegalArgumentException.class,
                () -> new GameSession(List.of(hub), "missing", 10, 10, 100));
        assertThrows(NullPointerException.class,
                () -> new GameSession(null, "hub", 10, 10, 100));
        List<GameMap> withNull = new ArrayList<>();
        withNull.add(null);
        assertThrows(NullPointerException.class,
                () -> new GameSession(withNull, "hub", 10, 10, 100));
    }

    /** Mutating the caller's collection cannot remove a registered map. */
    @Test
    void protectsRegisteredMapsFromCallerMutation() {
        List<GameMap> maps = new ArrayList<>(MapCatalog.createMaps());
        GameSession session = new GameSession(maps, MapCatalog.HUB, 28, 28, 180);
        maps.clear();
        session.switchMap(MapCatalog.CROSSROADS);
        assertEquals(MapCatalog.CROSSROADS, session.getCurrentMap().getId());
    }

    /** The transition stamp changes on same-map success but never on a rejected transition. */
    @Test
    void transitionVersionTracksOnlySuccessfulEntry() {
        GameSession session = demo();
        assertEquals(0, session.getTransitionVersion());
        session.switchMap(MapCatalog.HUB);
        assertEquals(1, session.getTransitionVersion());
        session.switchMap(MapCatalog.HUB);
        assertEquals(2, session.getTransitionVersion());
        assertThrows(IllegalArgumentException.class, () -> session.switchMap("missing"));
        assertThrows(IllegalArgumentException.class, () -> session.switchMap(MapCatalog.HUB, "missing"));
        assertEquals(2, session.getTransitionVersion());
    }

    /**
     * Creates the same world session used by the screen without any LibGDX initialization.
     *
     * @return playable built-in session
     */
    private GameSession demo() {
        return new GameSession(MapCatalog.createMaps(), MapCatalog.HUB,
                GameConfig.PLAYER_WIDTH, GameConfig.PLAYER_HEIGHT, GameConfig.PLAYER_SPEED);
    }

    /**
     * Creates a small hub with a nearby full-height wall.
     *
     * @return map for collision-switch and rollback tests
     */
    private GameMap smallHub() {
        return new GameMap("hub", "Hub", MapType.HUB, 100, 100,
                List.of(new CollisionBox(40, 0, 10, 100)),
                Map.of("entry", new SpawnPoint(10, 10)), "entry");
    }
}
