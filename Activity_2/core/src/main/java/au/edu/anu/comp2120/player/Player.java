package au.edu.anu.comp2120.player;

import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.WalkableArea;
import java.util.Objects;

/**
 * Holds player geometry and applies time-based eight-direction movement without graphics or input APIs.
 *
 * @author Daoyan Zhu (u7782042)
 */
public final class Player {
    /** Current immutable collision rectangle, anchored at the bottom-left corner. */
    private CollisionBox bounds;
    /** Movement speed in world units per second. */
    private final double speed;
    /** Collision environment used to validate spawning and resolve movement. */
    private WalkableArea area;

    /**
     * Creates a player entirely inside a walkable region.
     *
     * @param bounds initial position and collision dimensions
     * @param speed positive, finite speed in world units per second
     * @param area collision environment
     * @throws NullPointerException if bounds or area is null
     * @throws IllegalArgumentException if speed is invalid or the spawn is blocked
     */
    public Player(CollisionBox bounds, double speed, WalkableArea area) {
        this.bounds = Objects.requireNonNull(bounds, "bounds");
        this.area = Objects.requireNonNull(area, "area");
        if (!Double.isFinite(speed) || speed <= 0 || !area.isWalkable(bounds)) {
            throw new IllegalArgumentException("Speed must be positive and finite; spawn must be walkable");
        }
        this.speed = speed;
    }

    /**
     * Applies normalized movement. Opposing keys should cancel before this call; when both
     * axes are nonzero, normalization keeps diagonal speed equal to cardinal speed.
     * Zero delta/input does nothing. Collision may allow sliding along a wall.
     * Invalid arguments are rejected before changing state.
     *
     * @param horizontal horizontal intent: -1 left, 0 idle, 1 right
     * @param vertical vertical intent: -1 down, 0 idle, 1 up
     * @param delta finite, non-negative simulation time in seconds
     * @throws IllegalArgumentException if input, delta, or resulting distance is invalid
     */
    public void updateMovement(int horizontal, int vertical, double delta) {
        if (horizontal < -1 || horizontal > 1 || vertical < -1 || vertical > 1
                || !Double.isFinite(delta) || delta < 0 || !Double.isFinite(speed * delta)) {
            throw new IllegalArgumentException("Expected axis inputs in [-1, 1] and finite non-negative distance");
        }
        double distance = speed * delta;
        double length = Math.hypot(horizontal, vertical);
        if (length != 0 && distance != 0) {
            bounds = area.move(bounds, horizontal / length * distance, vertical / length * distance);
        }
    }

    /**
     * Enters a collision environment at a new bottom-left position, preserving size and speed.
     * Session coordination should use this method when changing maps. All validation occurs
     * before either the position or collision environment changes.
     *
     * @param destination target collision environment
     * @param x destination horizontal coordinate
     * @param y destination vertical coordinate
     * @throws NullPointerException if destination is null
     * @throws IllegalArgumentException if the full body cannot occupy the destination
     */
    public void enterArea(WalkableArea destination, double x, double y) {
        Objects.requireNonNull(destination, "destination");
        CollisionBox nextBounds = new CollisionBox(x, y, bounds.width(), bounds.height());
        if (!destination.isWalkable(nextBounds)) {
            throw new IllegalArgumentException("Player does not fit at destination (" + x + ", " + y + ")");
        }
        area = destination;
        bounds = nextBounds;
    }

    /**
     * Returns immutable geometry for read-only consumers such as rendering.
     *
     * @return current collision rectangle
     */
    public CollisionBox getBounds() {
        return bounds;
    }

    /**
     * Returns the bottom-left horizontal position.
     *
     * @return horizontal world coordinate
     */
    public double getX() {
        return bounds.x();
    }

    /**
     * Returns the bottom-left vertical position.
     *
     * @return vertical world coordinate
     */
    public double getY() {
        return bounds.y();
    }

    /**
     * Returns the configured movement speed.
     *
     * @return world units travelled per second when unobstructed
     */
    public double getSpeed() {
        return speed;
    }
}
