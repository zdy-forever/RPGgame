package au.edu.anu.comp2120.world;

import java.util.List;
import java.util.Map;

/**
 * Built-in open maps without temporary test obstacles; artwork is supplied by the separate view.
 * New maps require another definition, not another map subclass.
 * No file loading, artwork, or graphics initialization is required.
 *
 * @author Daoyan Zhu (u7782042)
 */
public final class MapCatalog {
    /** Shared open-map width, larger than the camera view. */
    public static final int MAP_WIDTH = 1600;
    /** Shared open-map height; matches the elemental backgrounds' 4:3 aspect ratio. */
    public static final int MAP_HEIGHT = 1200;
    /** Stable identifier for the starting base. */
    public static final String HUB = "hub";
    /** Stable identifier for the practice map; historical ID retained for travel compatibility. */
    public static final String TRAINING = "battle-training";
    /** Stable identifier for the second battle map. */
    public static final String CROSSROADS = "battle-crossroads";
    /** Water guardian arena ID. */
    public static final String WATER_ARENA = "battle-water";
    /** Lightning guardian arena ID. */
    public static final String LIGHTNING_ARENA = "battle-lightning";
    /** Ice guardian arena ID. */
    public static final String ICE_ARENA = "battle-ice";
    /** Wind guardian arena ID. */
    public static final String WIND_ARENA = "battle-wind";

    /** Prevents construction of the built-in catalogue utility. */
    private MapCatalog() {
    }

    /**
     * Creates a Hub, a Training area and five guardian arenas with named spawns.
     * Interiors are unobstructed; outer map boundaries still constrain movement.
     * All geometry uses bottom-left world coordinates and is independent of rendering.
     *
     * @return immutable list of validated map definitions
     */
    public static List<GameMap> createMaps() {
        return List.of(
                new GameMap(HUB, "Hub", MapType.HUB, MAP_WIDTH, MAP_HEIGHT,
                        List.of(),
                        Map.of("entry", new SpawnPoint(64, 64),
                                "north", new SpawnPoint(64, 440)), "entry"),
                new GameMap(TRAINING, "Training Ground", MapType.TRAINING, MAP_WIDTH, MAP_HEIGHT,
                        List.of(),
                        Map.of("entry", new SpawnPoint(80, 80),
                                "far-side", new SpawnPoint(680, 480)), "entry"),
                new GameMap(CROSSROADS, "Crossroads", MapType.BATTLE, MAP_WIDTH, MAP_HEIGHT,
                        List.of(),
                        Map.of("entry", new SpawnPoint(80, 80),
                                "east", new SpawnPoint(1080, 80)), "entry"),
                guardianArena(WATER_ARENA, "Water Arena"),
                guardianArena(LIGHTNING_ARENA, "Lightning Arena"),
                guardianArena(ICE_ARENA, "Ice Arena"),
                guardianArena(WIND_ARENA, "Wind Arena"));
    }

    /**
     * Defines an open guardian arena using the same collision/spawn model as Crossroads.
     * @param id unique arena ID
     * @param name readable arena name
     * @return independent immutable map definition
     */
    private static GameMap guardianArena(String id, String name) {
        return new GameMap(id, name, MapType.BATTLE, MAP_WIDTH, MAP_HEIGHT, List.of(),
                Map.of("entry", new SpawnPoint(80, 80), "east", new SpawnPoint(1080, 80)), "entry");
    }
}
