package au.edu.anu.comp2120.save;

import au.edu.anu.comp2120.GameSession;
import au.edu.anu.comp2120.combat.Health;
import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.GameMap;
import au.edu.anu.comp2120.world.MapCatalog;
import au.edu.anu.comp2120.world.MapType;
import au.edu.anu.comp2120.world.SpawnPoint;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Verifies exact saved positions, safe destinations and health restoration without graphics.
 * @author Hao Liu (u7998420)
 */
final class LocationSnapshotTest {
    /** Isolated disk directory. */
    @TempDir Path directory;
    /** Creates a test instance. */
    LocationSnapshotTest() { }

    /** Creates the production map registry.
     * @return session in Hub
     */
    private GameSession session() {
        return new GameSession(MapCatalog.createMaps(), MapCatalog.HUB, 28, 28, 180);
    }

    /** Disk loads restore fractional positions and injured HP while preserving player identity.
     * @throws IOException if temporary save IO fails
     */
    @Test
    void diskRoundTripRestoresExactPositionAndHealth() throws IOException {
        var source = session();
        source.getPlayer().moveBy(12.5, 18.25);
        source.getPlayer().takeDamage(37);
        var expected = LocationSnapshot.capture(source);
        var store = new SaveStore(directory);
        store.save(SaveSlot.MANUAL, expected.toFields());
        var target = session();
        target.switchMap(MapCatalog.MEMORIAL_HALL);
        var player = target.getPlayer();
        var version = target.getTransitionVersion();
        var damageVersion = player.getHealth().getDamageVersion();
        var loaded = LocationSnapshot.fromFields(store.load(SaveSlot.MANUAL));
        loaded.restore(target);
        assertSame(player, target.getPlayer());
        assertSame(target.getCurrentMap(), player.getCollisionArea());
        assertEquals(expected, LocationSnapshot.capture(target));
        assertEquals(version + 1, target.getTransitionVersion());
        assertEquals(damageVersion, player.getHealth().getDamageVersion());
        loaded.restore(target);
        assertEquals(expected, LocationSnapshot.capture(target));
        assertEquals(63, player.getHealth().getCurrentHp());
    }

    /** Memorial is supported, but battle, training and dead players cannot be captured. */
    @Test
    void captureAcceptsOnlyLivingPlayersInSafeMaps() {
        var game = session();
        game.switchMap(MapCatalog.MEMORIAL_HALL);
        assertEquals(MapCatalog.MEMORIAL_HALL, LocationSnapshot.capture(game).mapId());
        for (String map : List.of(MapCatalog.TRAINING, MapCatalog.CROSSROADS)) {
            game.switchMap(map);
            assertThrows(IllegalArgumentException.class, () -> LocationSnapshot.capture(game));
        }
        game.switchMap(MapCatalog.HUB);
        game.getPlayer().takeDamage(100);
        assertThrows(IllegalArgumentException.class, () -> LocationSnapshot.capture(game));
    }

    /** Invalid destinations and capacities leave map, position, HP and transition version intact. */
    @Test
    void failedRestoreIsAtomic() {
        var game = session();
        game.getPlayer().takeDamage(20);
        var before = LocationSnapshot.capture(game);
        long version = game.getTransitionVersion();
        for (var invalid : List.of(new LocationSnapshot("missing", 64, 64, 90, 100),
                new LocationSnapshot(MapCatalog.HUB, 1070, 64, 90, 100),
                new LocationSnapshot(MapCatalog.HUB, 64, 64, 90, 200),
                new LocationSnapshot(MapCatalog.TRAINING, 80, 80, 90, 100),
                new LocationSnapshot(MapCatalog.WATER_ARENA, 80, 80, 90, 100))) {
            assertThrows(IllegalArgumentException.class, () -> invalid.restore(game));
            assertEquals(before, LocationSnapshot.capture(game));
            assertEquals(version, game.getTransitionVersion());
            assertSame(game.getCurrentMap(), game.getPlayer().getCollisionArea());
        }
    }

    /** A destination overlapping a wall fails even when the anchor alone is walkable. */
    @Test
    void collisionChecksUseEntireBodyAndProtectSessionPrimitive() {
        var map = new GameMap("safe", "Safe", MapType.HUB, 200, 200,
                List.of(new CollisionBox(100, 0, 10, 200)),
                Map.of("entry", new SpawnPoint(10, 10)), "entry");
        var game = new GameSession(List.of(map), "safe", 28, 28, 180);
        var before = LocationSnapshot.capture(game);
        assertThrows(IllegalArgumentException.class,
                () -> new LocationSnapshot("safe", 80, 40, 50, 100).restore(game));
        assertThrows(IllegalArgumentException.class, () -> game.restoreLocation("safe", 80, 40));
        assertEquals(before, LocationSnapshot.capture(game));
        assertEquals(0, game.getTransitionVersion());
        game.restoreLocation("safe", 120, 40);
        game.getPlayer().moveBy(-100, 0);
        assertEquals(110, game.getPlayer().getX());
    }

    /** Non-finite coordinates, invalid HP, missing and unknown fields never become valid data. */
    @Test
    void malformedFieldsAreRejected() {
        var original = LocationSnapshot.capture(session());
        for (var invalid : Map.of("location.x", "NaN", "location.y", "Infinity",
                "location.hp", "0", "location.maxHp", "99", "location.extra", "x").entrySet()) {
            var fields = new HashMap<>(original.toFields());
            fields.put(invalid.getKey(), invalid.getValue());
            assertThrows(IllegalArgumentException.class, () -> LocationSnapshot.fromFields(fields));
        }
        var fields = new HashMap<>(original.toFields());
        fields.remove("location.map");
        assertThrows(IllegalArgumentException.class, () -> LocationSnapshot.fromFields(fields));
        assertThrows(IllegalArgumentException.class,
                () -> new LocationSnapshot(MapCatalog.HUB, -1, 64, 100, 100));
    }

    /** Explicit HP restoration validates before changing state and never fabricates damage events. */
    @Test
    void healthRestorationValidatesAndPreservesDamageCounter() {
        var health = new Health(100);
        health.takeDamage(50);
        long version = health.getDamageVersion();
        for (int hp : new int[] {1, 100, 0, 63}) {
            health.restoreSavedHp(hp);
            assertEquals(hp, health.getCurrentHp());
            assertEquals(hp > 0, health.isAlive());
            assertEquals(version, health.getDamageVersion());
        }
        assertThrows(IllegalArgumentException.class, () -> health.restoreSavedHp(-1));
        assertThrows(IllegalArgumentException.class, () -> health.restoreSavedHp(101));
        assertEquals(63, health.getCurrentHp());
    }
}
