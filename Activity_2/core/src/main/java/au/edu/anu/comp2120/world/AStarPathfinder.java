package au.edu.anu.comp2120.world;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;

/**
 * Bounded four-neighbour A* over world-space body placements, not point-sized actors or art tiles.
 * Full-body swept envelopes validate every edge, including connectors to off-grid start/goal poses.
 * Cardinal grid edges cannot cut diagonal corners or jump across thin walls. Clear direct approaches
 * skip the grid. Searches are deterministic and limited by a maximum grid size.
 * Construct for static collision data and one body size; different actors may share this instance.
 *
 * @author Daoyan Zhu (u7782042)
 */
public final class AStarPathfinder {
    /** Hard cap preventing accidental huge-map searches from exhausting memory or blocking the loop. */
    public static final int MAX_GRID_NODES = 20000;
    /** Static collision environment shared with actual movement. */
    private final WalkableArea area;
    /** Outer world bounds, including non-zero origins. */
    private final CollisionBox world;
    /** Body width used to account for passage clearance. */
    private final double bodyWidth;
    /** Body height used to account for passage clearance. */
    private final double bodyHeight;
    /** Maximum spacing between neighbouring body-origin samples. */
    private final double cellSize;
    /** Number of horizontal samples, including the far boundary. */
    private final int columns;
    /** Number of vertical samples, including the far boundary. */
    private final int rows;
    /** Cached body occupancy for the immutable map geometry. */
    private final boolean[] walkable;

    /**
     * Creates a navigation grid whose samples represent the entire moving body.
     * @param area static collision environment
     * @param bodyWidth positive finite actor width fitting inside the world
     * @param bodyHeight positive finite actor height fitting inside the world
     * @param cellSize positive finite grid spacing in world units
     * @throws NullPointerException if area or its bounds is null
     * @throws IllegalArgumentException if dimensions are invalid or the grid exceeds MAX_GRID_NODES
     */
    public AStarPathfinder(WalkableArea area, double bodyWidth, double bodyHeight, double cellSize) {
        this.area = Objects.requireNonNull(area, "area");
        world = Objects.requireNonNull(area.getBounds(), "world bounds");
        if (!Double.isFinite(bodyWidth) || bodyWidth <= 0 || bodyWidth > world.width()
                || !Double.isFinite(bodyHeight) || bodyHeight <= 0 || bodyHeight > world.height()
                || !Double.isFinite(cellSize) || cellSize <= 0) {
            throw new IllegalArgumentException("Navigation requires a fitting positive body and finite positive grid spacing");
        }
        double cols = Math.ceil((world.width() - bodyWidth) / cellSize) + 1;
        double rowCount = Math.ceil((world.height() - bodyHeight) / cellSize) + 1;
        if (!Double.isFinite(cols * rowCount) || cols * rowCount > MAX_GRID_NODES) {
            throw new IllegalArgumentException("Navigation grid exceeds " + MAX_GRID_NODES + " nodes");
        }
        this.bodyWidth = bodyWidth;
        this.bodyHeight = bodyHeight;
        this.cellSize = cellSize;
        columns = (int) cols;
        rows = (int) rowCount;
        walkable = new boolean[columns * rows];
        for (int i = 0; i < walkable.length; i++) {
            walkable[i] = area.isWalkable(bodyAt(i));
        }
    }

    /**
     * Finds immutable waypoint bodies excluding the start, except an already-reached goal returns it.
     * Clear arbitrary-angle final connectors stop at the requested centre distance. Every segment is
     * full-body clear. Empty means no route on this grid; coarse grids may miss narrow valid routes.
     * @param start current walkable body of the configured size
     * @param target target hurtbox, not necessarily the same size as the moving body
     * @param stopDistance finite non-negative desired centre separation
     * @return immutable route, or empty for an unreachable/outside/blocked target
     * @throws NullPointerException if start or target is null
     * @throws IllegalArgumentException if start geometry or stopDistance is invalid
     */
    public List<CollisionBox> findPath(CollisionBox start, CollisionBox target, double stopDistance) {
        validate(start, target, stopDistance);
        if (!area.isWalkable(target)) {
            return List.of();
        }
        Optional<CollisionBox> direct = findDirectDestination(start, target, stopDistance);
        if (direct.isPresent()) {
            return List.of(direct.get());
        }
        double[] costs = new double[walkable.length];
        Arrays.fill(costs, Double.POSITIVE_INFINITY);
        int[] parents = new int[walkable.length];
        Arrays.fill(parents, -1);
        boolean[] closed = new boolean[walkable.length];
        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(Node::estimate)
                .thenComparingInt(Node::index));
        int baseX = Math.min(columns - 1, Math.max(0, (int) Math.floor((start.x() - world.x()) / cellSize)));
        int baseY = Math.min(rows - 1, Math.max(0, (int) Math.floor((start.y() - world.y()) / cellSize)));
        for (int y = baseY; y <= Math.min(baseY + 1, rows - 1); y++) {
            for (int x = baseX; x <= Math.min(baseX + 1, columns - 1); x++) {
                int index = y * columns + x;
                CollisionBox body = bodyAt(index);
                if (walkable[index] && hasClearCorridor(start, body)) {
                    costs[index] = distance(start, body);
                    open.add(new Node(index, costs[index], costs[index] + heuristic(body, target, stopDistance)));
                }
            }
        }
        while (!open.isEmpty()) {
            Node node = open.remove();
            int index = node.index();
            if (closed[index] || node.cost() != costs[index]) {
                continue;
            }
            closed[index] = true;
            CollisionBox body = bodyAt(index);
            Optional<CollisionBox> finish = findDirectDestination(body, target, stopDistance);
            if (finish.isPresent()) {
                List<CollisionBox> route = new ArrayList<>();
                for (int current = index; current != -1; current = parents[current]) {
                    route.add(bodyAt(current));
                }
                Collections.reverse(route);
                if (!route.get(route.size() - 1).equals(finish.get())) {
                    route.add(finish.get());
                }
                return List.copyOf(route);
            }
            int x = index % columns;
            int y = index / columns;
            int[] neighbours = {x > 0 ? index - 1 : -1, x + 1 < columns ? index + 1 : -1,
                    y > 0 ? index - columns : -1, y + 1 < rows ? index + columns : -1};
            for (int next : neighbours) {
                if (next < 0 || closed[next] || !walkable[next]) {
                    continue;
                }
                CollisionBox destination = bodyAt(next);
                double cost = costs[index] + distance(body, destination);
                if (cost < costs[next] && hasClearCorridor(body, destination)) {
                    costs[next] = cost;
                    parents[next] = index;
                    open.add(new Node(next, cost, cost + heuristic(destination, target, stopDistance)));
                }
            }
        }
        return List.of();
    }

    /**
     * Computes a clear straight approach ending at stopDistance, or returns empty if walls block it.
     * The conservative rectangular corridor also prevents choosing an attack pose through a wall.
     * @param start current walkable body of the configured size
     * @param target target hurtbox
     * @param stopDistance finite non-negative centre distance to preserve
     * @return fitting destination, including start when already close enough, or empty
     * @throws NullPointerException if start or target is null
     * @throws IllegalArgumentException if start or stopDistance is invalid
     */
    public Optional<CollisionBox> findDirectDestination(CollisionBox start, CollisionBox target, double stopDistance) {
        validate(start, target, stopDistance);
        if (!hasClearCorridor(start, target)) {
            return Optional.empty();
        }
        double length = distance(start, target);
        if (!Double.isFinite(length)) {
            return Optional.empty();
        }
        double fraction = length <= stopDistance ? 0 : (length - stopDistance) / length;
        CollisionBox destination = new CollisionBox(start.x()
                + (target.x() + target.width() / 2 - start.x() - start.width() / 2) * fraction,
                start.y() + (target.y() + target.height() / 2 - start.y() - start.height() / 2) * fraction,
                bodyWidth, bodyHeight);
        return hasClearCorridor(start, destination) ? Optional.of(destination) : Optional.empty();
    }

    /**
     * Conservatively tests the whole axis-aligned envelope between two rectangles.
     * Cardinal equal-size segments are exact sweeps; diagonal envelopes deliberately forbid corner cuts.
     * @param from first rectangle
     * @param to second rectangle
     * @return true when the entire envelope is walkable
     * @throws NullPointerException if either rectangle is null
     */
    public boolean hasClearCorridor(CollisionBox from, CollisionBox to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        double x = Math.min(from.x(), to.x());
        double y = Math.min(from.y(), to.y());
        double width = Math.max(from.right(), to.right()) - x;
        double height = Math.max(from.top(), to.top()) - y;
        return Double.isFinite(width) && Double.isFinite(height)
                && area.isWalkable(new CollisionBox(x, y, width, height));
    }

    /**
     * Converts a grid index into a full actor body, sampling the exact far map boundary as well.
     * Integer division selects the row before conversion to world coordinates.
     * @param index valid flattened grid index
     * @return immutable placement rectangle
     */
    private CollisionBox bodyAt(int index) {
        int column = index % columns;
        int row = index / columns;
        double x = Math.min(world.x() + column * cellSize, world.right() - bodyWidth);
        double y = Math.min(world.y() + row * cellSize, world.top() - bodyHeight);
        if (x + bodyWidth > world.right()) {
            x = Math.nextDown(x);
        }
        if (y + bodyHeight > world.top()) {
            y = Math.nextDown(y);
        }
        return new CollisionBox(x, y, bodyWidth, bodyHeight);
    }

    /**
     * Measures centre separation for edge costs and goal estimates.
     * @param from first rectangle
     * @param to second rectangle
     * @return Euclidean centre distance
     */
    private static double distance(CollisionBox from, CollisionBox to) {
        return Math.hypot(to.x() + to.width() / 2 - from.x() - from.width() / 2,
                to.y() + to.height() / 2 - from.y() - from.height() / 2);
    }

    /**
     * Lower-bounds remaining travel without considering obstacles.
     * @param body grid placement
     * @param target target hurtbox
     * @param stopDistance permitted centre separation
     * @return admissible non-negative remaining-distance estimate
     */
    private static double heuristic(CollisionBox body, CollisionBox target, double stopDistance) {
        return Math.max(0, distance(body, target) - stopDistance);
    }

    /**
     * Validates query inputs before allocating search state.
     * @param start actor body
     * @param target target hurtbox
     * @param stopDistance stopping distance
     * @throws NullPointerException if start or target is null
     * @throws IllegalArgumentException if start does not fit or distance is invalid
     */
    private void validate(CollisionBox start, CollisionBox target, double stopDistance) {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(target, "target");
        if (start.width() != bodyWidth || start.height() != bodyHeight || !area.isWalkable(start)
                || !Double.isFinite(stopDistance) || stopDistance < 0) {
            throw new IllegalArgumentException("Navigation needs a walkable matching body and finite non-negative stop distance");
        }
    }

    /**
     * Immutable priority-queue entry; old entries are discarded if a cheaper route supersedes them.
     * @param index flattened grid index
     * @param cost known start-to-node cost
     * @param estimate cost plus admissible remaining-distance estimate
     * @author Daoyan Zhu (u7782042)
     */
    private record Node(int index, double cost, double estimate) {
    }
}
