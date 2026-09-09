package au.edu.anu.comp2120.player;

import au.edu.anu.comp2120.combat.Damageable;
import au.edu.anu.comp2120.combat.Health;
import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.WalkableArea;
import java.util.Objects;

/**
 * Holds player geometry and health, with time-based eight-direction movement independent of graphics.
 * Damage delegates to the shared health model; death consequences are a future gameplay policy.
 *
 * @author Daoyan Zhu (u7782042)
 */
public final class Player implements Damageable {
    /** Default player HP capacity; callers can override it with the explicit-capacity constructor. */
    public static final int DEFAULT_MAX_HP = 100;
    /** Per-player health retained during movement and map transitions. */
    private final Health health;
    /** Current immutable collision rectangle, anchored at the bottom-left corner. */
    private CollisionBox bounds;
    /** Movement speed in world units per second. */
    private final double speed;
    /** Collision environment used to validate spawning and resolve movement. */
    private WalkableArea area;

    /**
     * Creates a player entirely inside a walkable region with default full health.
     *
     * @param bounds initial position and collision dimensions
     * @param speed positive, finite speed in world units per second
     * @param area collision environment
     * @throws NullPointerException if bounds or area is null
     * @throws IllegalArgumentException if speed is invalid or the spawn is blocked
     */
    public Player(CollisionBox bounds, double speed, WalkableArea area) {
        this(bounds, speed, area, DEFAULT_MAX_HP);
    }

    /**
     * Creates a player with configurable full health without sharing mutable HP with other entities.
     *
     * @param bounds initial position and collision dimensions
     * @param speed positive, finite speed in world units per second
     * @param area collision environment
     * @param maxHp positive initial and maximum hit points
     * @throws NullPointerException if bounds or area is null
     * @throws IllegalArgumentException if speed, spawn, or maxHp is invalid
     */
    public Player(CollisionBox bounds, double speed, WalkableArea area, int maxHp) {
        this.bounds = Objects.requireNonNull(bounds, "bounds");
        this.area = Objects.requireNonNull(area, "area");
        if (!Double.isFinite(speed) || speed <= 0 || !area.isWalkable(bounds)) {
            throw new IllegalArgumentException("Speed must be positive and finite; spawn must be walkable");
        }
        this.speed = speed;
        health = new Health(maxHp);
    }

    /**
     * Returns this player's health for healing, explicit restoration, or read-only display queries.
     * Mutate it only through its validated operations on the game-loop thread.
     *
     * @return this player's persistent health model
     */
    public Health getHealth() {
        return health;
    }

    /**
     * Delegates damage to the shared model without changing position or triggering visual effects.
     *
     * @param damage non-negative hit points to remove
     * @throws IllegalArgumentException if damage is negative
     */
    @Override
    public void takeDamage(int damage) {
        health.takeDamage(damage);
    }

    /**
     * Reports whether the shared model has remaining HP.
     *
     * @return true while this player has positive HP
     */
    @Override
    public boolean isAlive() {
        return health.isAlive();
    }

    /**
     * Applies normalized movement. Opposing keys should cancel before this call; when both
     * axes are nonzero, normalization keeps diagonal speed equal to cardinal speed.
     * Zero delta/input does nothing. Collision may allow sliding along a wall.
     * This low-level movement operation does not enforce death-related gameplay restrictions.
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
     * Applies a finite displacement through the same swept collision resolver as ordinary movement.
     * Ability controllers own timing and permission; this method grants neither teleportation nor immunity.
     * @param deltaX horizontal world displacement
     * @param deltaY vertical world displacement
     * @throws IllegalArgumentException if either displacement is not finite
     */
    public void moveBy(double deltaX, double deltaY) {
        bounds = area.move(bounds, deltaX, deltaY);
    }

    /**
     * Enters a collision environment at a new bottom-left position, preserving size, speed, and HP.
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
