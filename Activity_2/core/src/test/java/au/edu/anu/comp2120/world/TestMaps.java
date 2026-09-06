package au.edu.anu.comp2120.world;

import java.util.List;
import java.util.Map;

/**
 * Supplies map metadata for existing collision tests so their geometry assertions stay focused.
 *
 * @author Daoyan Zhu (u7782042)
 */
public final class TestMaps {
    /** Prevents construction of this test utility. */
    private TestMaps() {
    }

    /**
     * Creates a battle fixture whose origin is a spawn anchor.
     *
     * @param width map width
     * @param height map height
     * @param obstacles test collision rectangles, leaving the origin walkable
     * @return validated map with a known ID and default spawn
     * @throws NullPointerException if obstacles or any obstacle is null
     * @throws IllegalArgumentException if geometry is invalid or the origin is blocked
     */
    public static GameMap rectangle(double width, double height, List<CollisionBox> obstacles) {
        return new GameMap("test", "Test map", MapType.BATTLE, width, height, obstacles,
                Map.of("entry", new SpawnPoint(0, 0)), "entry");
    }
}
