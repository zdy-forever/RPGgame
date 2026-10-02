package au.edu.anu.comp2120.world;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Validates the built-in map catalogue dimensions and contents.
 * @author COMP2120 Assignment 3 group
 * @author Jiaqi Chen (u7851266)
 */
final class MapCatalogTest {
    /** Creates a new {@code MapCatalogTest} instance. */
    MapCatalogTest() {
        // Test instances are created by JUnit.
    }

    /** Catalog dimensions are positive. */
    @Test void catalogExposesDimensions() {
        assertTrue(MapCatalog.MAP_WIDTH > 0);
        assertTrue(MapCatalog.MAP_HEIGHT > 0);
        assertTrue(MapCatalog.HUB_WIDTH > 0);
        assertTrue(MapCatalog.HUB_HEIGHT > 0);
    }

    /** The catalogue builds the Hub, training area and five guardian arenas. */
    @Test void createsExpectedMaps() {
        List<GameMap> maps = MapCatalog.createMaps();
        assertEquals(8, maps.size());
        assertTrue(maps.stream().anyMatch(m -> m.getId().equals(MapCatalog.HUB)));
        assertTrue(maps.stream().anyMatch(m -> m.getId().equals(MapCatalog.TRAINING)));
        assertTrue(maps.stream().anyMatch(m -> m.getId().equals(MapCatalog.WATER_ARENA)));
        assertTrue(maps.stream().anyMatch(m -> m.getId().equals(MapCatalog.WIND_ARENA)));
    }

    /** The Hub defines an entry spawn. */
    @Test void hubHasEntrySpawn() {
        GameMap hub = MapCatalog.createMaps().stream()
                .filter(m -> m.getId().equals(MapCatalog.HUB)).findFirst().get();
        assertTrue(hub.getSpawnPoints().containsKey("entry"));
    }
}
