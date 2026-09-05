package au.edu.anu.comp2120.world;

import java.util.List;
import java.util.Objects;

/**
 * Rectangular world with immutable static obstacles and swept axis-separated collision resolution.
 * No images or graphics context are needed to construct or query a map.
 *
 * @author Daoyan Zhu (u7782042)
 */
public final class GameMap implements WalkableArea {
    /** Outer boundary, anchored at the world origin. */
    private final CollisionBox bounds;
    /** Immutable obstacle data, also usable by a placeholder renderer. */
    private final List<CollisionBox> obstacles;

    /**
     * Creates a map with collision geometry supplied by its caller.
     *
     * @param width positive, finite world width
     * @param height positive, finite world height
     * @param obstacles rectangles entirely inside the world
     * @throws NullPointerException if the list or an obstacle is null
     * @throws IllegalArgumentException if map dimensions or obstacle placement are invalid
     */
    public GameMap(double width, double height, List<CollisionBox> obstacles) {
        bounds = new CollisionBox(0, 0, width, height);
        this.obstacles = List.copyOf(obstacles);
        for (CollisionBox obstacle : this.obstacles) {
            if (!contains(obstacle)) {
                throw new IllegalArgumentException("Obstacles must fit inside the map");
            }
        }
    }

    /**
     * Returns world dimensions and origin.
     *
     * @return immutable world boundary
     */
    public CollisionBox getBounds() {
        return bounds;
    }

    /**
     * Returns static collision geometry for read-only consumers.
     *
     * @return immutable obstacle list
     */
    public List<CollisionBox> getObstacles() {
        return obstacles;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isWalkable(float x, float y) {
        if (!Float.isFinite(x) || !Float.isFinite(y)
                || x < 0 || y < 0 || x >= bounds.width() || y >= bounds.height()) {
            return false;
        }
        return obstacles.stream().noneMatch(wall -> x >= wall.x() && x < wall.right()
                && y >= wall.y() && y < wall.top());
    }

    /** {@inheritDoc} */
    @Override
    public boolean isWalkable(CollisionBox body) {
        Objects.requireNonNull(body, "body");
        return contains(body) && obstacles.stream().noneMatch(body::overlaps);
    }

    /** {@inheritDoc} */
    @Override
    public CollisionBox move(CollisionBox body, double deltaX, double deltaY) {
        if (!isWalkable(body) || !Double.isFinite(deltaX) || !Double.isFinite(deltaY)) {
            throw new IllegalArgumentException("Movement requires a walkable body and finite displacement");
        }
        double x = body.x();
        double y = body.y();
        if (deltaX != 0) {
            x = Math.max(0, Math.min(fitBefore(bounds.width(), body.width()), x + deltaX));
            for (CollisionBox wall : obstacles) {
                if (body.y() < wall.top() && body.top() > wall.y()) {
                    if (deltaX > 0 && body.right() <= wall.x()) {
                        x = Math.min(x, fitBefore(wall.x(), body.width()));
                    } else if (deltaX < 0 && body.x() >= wall.right()) {
                        x = Math.max(x, wall.right());
                    }
                }
            }
        }
        if (deltaY != 0) {
            y = Math.max(0, Math.min(fitBefore(bounds.height(), body.height()), y + deltaY));
            for (CollisionBox wall : obstacles) {
                if (x < wall.right() && x + body.width() > wall.x()) {
                    if (deltaY > 0 && body.top() <= wall.y()) {
                        y = Math.min(y, fitBefore(wall.y(), body.height()));
                    } else if (deltaY < 0 && body.y() >= wall.top()) {
                        y = Math.max(y, wall.top());
                    }
                }
            }
        }
        return new CollisionBox(x, y, body.width(), body.height());
    }

    /**
     * Checks full containment in the outer map boundary.
     *
     * @param body collision rectangle
     * @return true if every edge is within the world
     */
    private boolean contains(CollisionBox body) {
        return body.x() >= 0 && body.y() >= 0
                && body.right() <= bounds.width() && body.top() <= bounds.height();
    }

    /**
     * Finds a contact coordinate, correcting floating-point rounding into a boundary.
     *
     * @param edge blocking upper or right edge
     * @param size moving body's dimension on this axis
     * @return origin whose far edge does not exceed the blocking edge
     */
    private static double fitBefore(double edge, double size) {
        double origin = edge - size;
        return origin + size > edge ? Math.nextDown(origin) : origin;
    }
}
