package au.edu.anu.comp2120.world;

/**
 * Exposes point/body occupancy and swept axis-separated collision resolution without map internals.
 *
 * @author Daoyan Zhu (u7782042)
 */
public interface WalkableArea {
    /**
     * Returns the finite outer region used to bound navigation searches.
     * @return immutable world boundary; obstacles may occupy part of this region
     */
    CollisionBox getBounds();


    /**
     * Checks a world point. The outer right/top boundaries are excluded.
     *
     * @param x horizontal world coordinate
     * @param y vertical world coordinate
     * @return true if the finite point is inside the map and outside obstacles
     */
    boolean isWalkable(float x, float y);

    /**
     * Checks the full collision body; touching a boundary is permitted.
     *
     * @param bounds body to test
     * @return true if the body fits without overlapping obstacles
     * @throws NullPointerException if bounds is null
     */
    boolean isWalkable(CollisionBox bounds);

    /**
     * Resolves horizontal movement first, then vertical movement from the resolved x position.
     * Each axis checks its entire travelled path and stops at first contact, permitting wall sliding.
     * Implementations must return a walkable body of unchanged dimensions.
     *
     * @param bounds current walkable body
     * @param deltaX finite horizontal displacement
     * @param deltaY finite vertical displacement
     * @return resolved body, possibly unchanged when blocked
     * @throws NullPointerException if bounds is null
     * @throws IllegalArgumentException if the starting body or displacement is invalid
     */
    CollisionBox move(CollisionBox bounds, double deltaX, double deltaY);

    /**
     * Finds the first blocking contact along a straight path, without axis sliding.
     * Used by projectiles; ordinary actor movement still uses move.
     * @param bounds starting full body, which must be walkable
     * @param deltaX finite horizontal displacement
     * @param deltaY finite vertical displacement
     * @return fraction in [0, 1] at a blocking contact, or positive infinity for a clear step
     * @throws NullPointerException if bounds is null
     * @throws IllegalArgumentException if the starting body or displacement is invalid
     */
    double firstBlock(CollisionBox bounds, double deltaX, double deltaY);
}
