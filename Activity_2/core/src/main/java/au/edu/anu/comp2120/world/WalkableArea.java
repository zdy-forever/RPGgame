package au.edu.anu.comp2120.world;

/**
 * Exposes map collision checks without revealing tile storage or map-loader
 * details to player movement code.
 *
 * @author Daoyan Zhu (u7782042)
 */
public interface WalkableArea {

    /**
     * Checks whether a world-space position can be occupied.
     *
     * @param x horizontal world coordinate
     * @param y vertical world coordinate
     * @return {@code true} when the position is walkable
     */
    boolean isWalkable(float x, float y);
}
