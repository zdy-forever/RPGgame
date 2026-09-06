package au.edu.anu.comp2120.world;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Verifies map identity, role, dimensions, named spawns, and immutable definition validation.
 *
 * @author Daoyan Zhu (u7782042)
 */
final class MapDefinitionTest {
    /** Creates a definition test fixture. */
    MapDefinitionTest() {
    }

    /** All metadata and named spawns remain accessible without loading graphics resources. */
    @Test
    void exposesMetadataAndNamedSpawns() {
        GameMap map = definition(Map.of("entry", new SpawnPoint(10, 10),
                "other", new SpawnPoint(70, 70)), "other");
        assertEquals("level", map.getId());
        assertEquals("Test level", map.getName());
        assertEquals(MapType.BATTLE, map.getType());
        assertEquals(100, map.getBounds().width());
        assertEquals(100, map.getBounds().height());
        assertEquals("other", map.getDefaultSpawnId());
        assertEquals(new SpawnPoint(70, 70), map.getSpawnPoint("other"));
        assertEquals(2, map.getSpawnPoints().size());
        assertFalse(map.isWalkable(new CollisionBox(95, 95, 10, 10)));
    }

    /**
     * A map rejects blank IDs and display names.
     *
     * @param text blank metadata value
     */
    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    void rejectsBlankIdentity(String text) {
        Map<String, SpawnPoint> spawns = Map.of("entry", new SpawnPoint(10, 10));
        assertThrows(IllegalArgumentException.class,
                () -> new GameMap(text, "Name", MapType.HUB, 100, 100, List.of(), spawns, "entry"));
        assertThrows(IllegalArgumentException.class,
                () -> new GameMap("id", text, MapType.HUB, 100, 100, List.of(), spawns, "entry"));
        assertThrows(IllegalArgumentException.class,
                () -> definition(Map.of(text, new SpawnPoint(10, 10)), text));
    }

    /**
     * Spawn coordinates must be finite and non-negative on both axes.
     *
     * @param coordinate invalid coordinate
     */
    @ParameterizedTest
    @ValueSource(doubles = {-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsInvalidSpawnCoordinates(double coordinate) {
        assertThrows(IllegalArgumentException.class, () -> new SpawnPoint(coordinate, 0));
        assertThrows(IllegalArgumentException.class, () -> new SpawnPoint(0, coordinate));
    }

    /**
     * Spawn anchors must lie inside the map and outside obstacles.
     *
     * @param x invalid anchor x
     * @param y invalid anchor y
     */
    @ParameterizedTest
    @CsvSource({"100,0", "0,100", "101,101", "40,40", "45,45"})
    void rejectsOutsideOrBlockedSpawns(double x, double y) {
        assertThrows(IllegalArgumentException.class,
                () -> definition(Map.of("entry", new SpawnPoint(x, y)), "entry"));
    }

    /** A point just outside a wall is checked at double precision without rounding into it. */
    @Test
    void preservesSpawnCoordinatePrecision() {
        double x = Math.nextDown(40.0);
        GameMap map = definition(Map.of("entry", new SpawnPoint(x, 40)), "entry");
        assertEquals(x, map.getSpawnPoint("entry").x());
    }

    /** Empty spawns, absent defaults, and absent lookups fail with clear errors. */
    @Test
    void requiresExistingDefaultSpawn() {
        assertThrows(IllegalArgumentException.class, () -> definition(Map.of(), "entry"));
        assertThrows(IllegalArgumentException.class,
                () -> definition(Map.of("entry", new SpawnPoint(10, 10)), "missing"));
        GameMap map = definition(Map.of("entry", new SpawnPoint(10, 10)), "entry");
        assertThrows(IllegalArgumentException.class, () -> map.getSpawnPoint("missing"));
        assertThrows(NullPointerException.class, () -> map.getSpawnPoint(null));
    }

    /** Missing metadata and null spawn entries are rejected. */
    @Test
    void rejectsNullDefinitionValues() {
        Map<String, SpawnPoint> spawns = Map.of("entry", new SpawnPoint(10, 10));
        assertThrows(NullPointerException.class,
                () -> new GameMap(null, "Name", MapType.HUB, 100, 100, List.of(), spawns, "entry"));
        assertThrows(NullPointerException.class,
                () -> new GameMap("id", null, MapType.HUB, 100, 100, List.of(), spawns, "entry"));
        assertThrows(NullPointerException.class,
                () -> new GameMap("id", "Name", null, 100, 100, List.of(), spawns, "entry"));
        assertThrows(NullPointerException.class, () -> definition(null, "entry"));
        Map<String, SpawnPoint> nullSpawn = new HashMap<>();
        nullSpawn.put("entry", null);
        assertThrows(NullPointerException.class, () -> definition(nullSpawn, "entry"));
    }

    /** Caller mutation cannot change registered spawn points. */
    @Test
    void protectsSpawnData() {
        Map<String, SpawnPoint> source = new HashMap<>();
        source.put("entry", new SpawnPoint(10, 10));
        GameMap map = definition(source, "entry");
        source.put("entry", new SpawnPoint(70, 70));
        assertEquals(new SpawnPoint(10, 10), map.getSpawnPoint("entry"));
        assertThrows(UnsupportedOperationException.class, () -> map.getSpawnPoints().clear());
    }

    /**
     * Creates a definition with one central obstacle for spawn validation.
     *
     * @param spawns named spawn anchors
     * @param defaultSpawn default spawn ID
     * @return validated map definition
     * @throws NullPointerException if required spawn data is null
     * @throws IllegalArgumentException if spawn data is invalid
     */
    private GameMap definition(Map<String, SpawnPoint> spawns, String defaultSpawn) {
        return new GameMap("level", "Test level", MapType.BATTLE, 100, 100,
                List.of(new CollisionBox(40, 40, 20, 20)), spawns, defaultSpawn);
    }
}
