package au.edu.anu.comp2120.screen;

import au.edu.anu.comp2120.world.CollisionBox;
import java.util.Objects;

/**
 * Pure camera-follow mathematics with no player, graphics, or time-dependent state.
 * Computes the visible rectangle around a subject centre and clamps each axis independently.
 *
 * @author Daoyan Zhu (u7782042)
 */
public final class CameraFollow {
    /** Prevents instantiation of this stateless calculation utility. */
    private CameraFollow() {
    }

    /**
     * Computes a fixed-size view around the subject's centre. If the map fits inside the view
     * on an axis, centres that map axis instead of constructing an inverted clamp interval.
     * Small maps can therefore have negative view origins: these represent equal background
     * margins, not a change to map or player coordinates. Map changes snap immediately.
     *
     * @param subject immutable world-space bounds of the followed object
     * @param map immutable world-space map bounds
     * @param viewWidth positive finite visible width in world units, not window pixels
     * @param viewHeight positive finite visible height in world units, not window pixels
     * @return immutable visible rectangle; its centre is the desired camera position
     * @throws NullPointerException if subject or map is null
     * @throws IllegalArgumentException if view dimensions or computed extents are not representable
     */
    public static CollisionBox viewFor(CollisionBox subject, CollisionBox map,
            double viewWidth, double viewHeight) {
        Objects.requireNonNull(subject, "subject bounds");
        Objects.requireNonNull(map, "map bounds");
        if (!Double.isFinite(viewWidth) || !Double.isFinite(viewHeight)
                || viewWidth <= 0 || viewHeight <= 0) {
            throw new IllegalArgumentException("Camera view dimensions must be positive and finite");
        }
        double x = originFor(subject.x() + subject.width() / 2, map.x(), map.width(), viewWidth);
        double y = originFor(subject.y() + subject.height() / 2, map.y(), map.height(), viewHeight);
        return new CollisionBox(x, y, viewWidth, viewHeight);
    }

    /**
     * Clamps a view origin on a large map axis or centres a small map axis.
     *
     * @param targetCentre followed subject centre on this axis
     * @param mapStart minimum map coordinate
     * @param mapSize map extent on this axis
     * @param viewSize visible extent on this axis
     * @return bottom or left coordinate of the visible interval
     */
    private static double originFor(double targetCentre, double mapStart, double mapSize, double viewSize) {
        if (mapSize <= viewSize) {
            return mapStart + (mapSize - viewSize) / 2;
        }
        double maximum = mapStart + mapSize - viewSize;
        if (maximum + viewSize > mapStart + mapSize) {
            maximum = Math.nextDown(maximum);
        }
        return Math.max(mapStart, Math.min(maximum, targetCentre - viewSize / 2));
    }
}
