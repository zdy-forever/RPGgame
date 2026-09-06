package au.edu.anu.comp2120.world;

import java.util.List;
import java.util.Map;

/**
 * Built-in placeholder map data. New maps require another definition, not another map subclass.
 * No file loading, artwork, or graphics initialization is required.
 *
 * @author Daoyan Zhu (u7782042)
 */
public final class MapCatalog {
    /** Stable identifier for the starting base. */
    public static final String HUB = "hub";
    /** Stable identifier for the first battle map. */
    public static final String TRAINING = "battle-training";
    /** Stable identifier for the second battle map. */
    public static final String CROSSROADS = "battle-crossroads";

    /** Prevents construction of the built-in catalogue utility. */
    private MapCatalog() {
    }

    /**
     * Creates one hub and two battle maps with different dimensions, walls, and named spawns.
     * All geometry uses bottom-left world coordinates and is independent of rendering.
     *
     * @return immutable list of validated map definitions
     */
    public static List<GameMap> createMaps() {
        return List.of(
                new GameMap(HUB, "Hub", MapType.HUB, 960, 540,
                        List.of(new CollisionBox(320, 100, 32, 280),
                                new CollisionBox(520, 320, 220, 24),
                                new CollisionBox(600, 80, 100, 100)),
                        Map.of("entry", new SpawnPoint(64, 64),
                                "north", new SpawnPoint(64, 440)), "entry"),
                new GameMap(TRAINING, "Training Ground", MapType.BATTLE, 800, 600,
                        List.of(new CollisionBox(240, 0, 24, 380),
                                new CollisionBox(480, 220, 24, 380)),
                        Map.of("entry", new SpawnPoint(80, 80),
                                "far-side", new SpawnPoint(680, 480)), "entry"),
                new GameMap(CROSSROADS, "Crossroads", MapType.BATTLE, 1200, 720,
                        List.of(new CollisionBox(320, 260, 480, 32),
                                new CollisionBox(580, 400, 32, 220)),
                        Map.of("entry", new SpawnPoint(80, 80),
                                "east", new SpawnPoint(1080, 80)), "entry"));
    }
}
