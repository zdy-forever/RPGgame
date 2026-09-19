package au.edu.anu.comp2120.world;

import java.util.List;
import java.util.Map;

/**
 * Built-in open placeholder maps without temporary test obstacles.
 * New maps require another definition, not another map subclass.
 * No file loading, artwork, or graphics initialization is required.
 *
 * @author Daoyan Zhu (u7782042)
 */
public final class MapCatalog {
    /** Stable identifier for the starting base. */
    public static final String HUB = "hub";
    /** Stable identifier for the practice map; historical ID retained for travel compatibility. */
    public static final String TRAINING = "battle-training";
    /** Stable identifier for the second battle map. */
    public static final String CROSSROADS = "battle-crossroads";

    /** Prevents construction of the built-in catalogue utility. */
    private MapCatalog() {
    }

    /**
     * Creates a Hub, a Training area and a Battle map with different dimensions and named spawns.
     * Interiors are unobstructed; outer map boundaries still constrain movement.
     * All geometry uses bottom-left world coordinates and is independent of rendering.
     *
     * @return immutable list of validated map definitions
     */
    public static List<GameMap> createMaps() {
        return List.of(
                new GameMap(HUB, "Hub", MapType.HUB, 960, 540,
                        List.of(),
                        Map.of("entry", new SpawnPoint(64, 64),
                                "north", new SpawnPoint(64, 440)), "entry"),
                new GameMap(TRAINING, "Training Ground", MapType.TRAINING, 800, 600,
                        List.of(),
                        Map.of("entry", new SpawnPoint(80, 80),
                                "far-side", new SpawnPoint(680, 480)), "entry"),
                new GameMap(CROSSROADS, "Crossroads", MapType.BATTLE, 1200, 720,
                        List.of(),
                        Map.of("entry", new SpawnPoint(80, 80),
                                "east", new SpawnPoint(1080, 80)), "entry"));
    }
}
