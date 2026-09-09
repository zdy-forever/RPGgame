package au.edu.anu.comp2120.world;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Named level with immutable spawn/collision data and swept axis-separated collision resolution.
 * Walkable space is the rectangular boundary minus the obstacle interiors.
 * No images or graphics context are needed to construct or query a map.
 *
 * @author Daoyan Zhu (u7782042)
 */
public final class GameMap implements WalkableArea {
    /** Stable map identifier, unique within a game session. */
    private final String id;
    /** Human-readable map name. */
    private final String name;
    /** Gameplay role, independent of its placeholder appearance. */
    private final MapType type;
    /** Outer boundary, anchored at the world origin. */
    private final CollisionBox bounds;
    /** Immutable obstacle data, also usable by a placeholder renderer. */
    private final List<CollisionBox> obstacles;
    /** Immutable named placement anchors. */
    private final Map<String, SpawnPoint> spawnPoints;
    /** Name of the spawn used when entry does not specify one. */
    private final String defaultSpawnId;

    /**
     * Creates and validates a level definition. Spawn anchors must be inside walkable space;
     * callers must still check whether an entering player's full body fits at the anchor.
     *
     * @param id non-blank stable identifier
     * @param name non-blank display name
     * @param type gameplay role
     * @param width positive, finite world width
     * @param height positive, finite world height
     * @param obstacles rectangles entirely inside the world
     * @param spawnPoints non-empty mapping of non-blank spawn IDs to anchors
     * @param defaultSpawnId ID of an existing spawn
     * @throws NullPointerException if any required value or collection entry is null
     * @throws IllegalArgumentException if identity, geometry, or spawn data is invalid
     */
    public GameMap(String id, String name, MapType type, double width, double height,
            List<CollisionBox> obstacles, Map<String, SpawnPoint> spawnPoints, String defaultSpawnId) {
        this.id = requireText(id, "map ID");
        this.name = requireText(name, "map name");
        this.type = Objects.requireNonNull(type, "map type");
        bounds = new CollisionBox(0, 0, width, height);
        this.obstacles = List.copyOf(obstacles);
        for (CollisionBox obstacle : this.obstacles) {
            if (!contains(obstacle)) {
                throw new IllegalArgumentException("Obstacles must fit inside the map");
            }
        }
        this.spawnPoints = Map.copyOf(spawnPoints);
        this.defaultSpawnId = requireText(defaultSpawnId, "default spawn ID");
        if (!this.spawnPoints.containsKey(defaultSpawnId)) {
            throw new IllegalArgumentException("Map " + id + " requires an existing default spawn");
        }
        this.spawnPoints.forEach((spawnId, point) -> {
            requireText(spawnId, "spawn ID");
            if (!isWalkablePoint(point.x(), point.y())) {
                throw new IllegalArgumentException("Blocked or outside spawn " + spawnId + " in map " + id);
            }
        });
    }

    /**
     * Returns the stable identifier used for map lookup.
     *
     * @return map ID
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the display name.
     *
     * @return human-readable map name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the map's gameplay role.
     *
     * @return hub or battle role
     */
    public MapType getType() {
        return type;
    }

    /**
     * Returns all named placement anchors.
     *
     * @return immutable spawn mapping
     */
    public Map<String, SpawnPoint> getSpawnPoints() {
        return spawnPoints;
    }

    /**
     * Returns the spawn selected for default entry.
     *
     * @return default spawn ID
     */
    public String getDefaultSpawnId() {
        return defaultSpawnId;
    }

    /**
     * Looks up a named spawn without silently falling back to another location.
     *
     * @param spawnId spawn identifier
     * @return matching immutable anchor
     * @throws NullPointerException if spawnId is null
     * @throws IllegalArgumentException if the spawn does not exist
     */
    public SpawnPoint getSpawnPoint(String spawnId) {
        SpawnPoint point = spawnPoints.get(Objects.requireNonNull(spawnId, "spawn ID"));
        if (point == null) {
            throw new IllegalArgumentException("Unknown spawn " + spawnId + " in map " + id);
        }
        return point;
    }

    /**
     * Returns world dimensions and origin.
     *
     * @return immutable world boundary
     */
    @Override
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
        return isWalkablePoint(x, y);
    }

    /**
     * Tests point occupancy without reducing spawn coordinates to float precision.
     *
     * @param x horizontal world coordinate
     * @param y vertical world coordinate
     * @return true if the point is inside the world and outside obstacles
     */
    private boolean isWalkablePoint(double x, double y) {
        if (!Double.isFinite(x) || !Double.isFinite(y)
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

    /**
     * Validates a required identifier or display label without changing its spelling.
     *
     * @param value text to validate
     * @param label field name used in errors
     * @return original non-blank value
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if value is blank
     */
    private static String requireText(String value, String label) {
        Objects.requireNonNull(value, label);
        if (value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value;
    }
}
