package au.edu.anu.comp2120.world;

import java.util.Objects;

/**
 * Continuous rectangle sweeps and point-segment obstruction against static rectangles, without rendering.
 * Tangential edge/corner contact is not an interior hit; entry must occur before the step ends.
 * @author Daoyan Zhu (u7782042)
 */
public final class CollisionSweep {
    /** Prevents construction of the pure geometry utility. */
    private CollisionSweep() {
    }

    /**
     * Tests a zero-width closed segment against an open rectangle using slab intervals.
     * No epsilon, sampling or graphics size is used. Pure tangency and endpoint-only boundary
     * contact are clear; a stationary point strictly inside the blocker is obstructed.
     * @param fromX segment start x
     * @param fromY segment start y
     * @param toX segment end x
     * @param toY segment end y
     * @param blocker static rectangle whose interior is solid
     * @return true if any part of the segment lies strictly inside the blocker
     * @throws NullPointerException if blocker is null
     * @throws IllegalArgumentException if coordinates or their differences are non-finite
     */
    public static boolean segmentCrossesInterior(double fromX, double fromY, double toX, double toY,
            CollisionBox blocker) {
        Objects.requireNonNull(blocker, "blocker");
        double dx = toX - fromX;
        double dy = toY - fromY;
        if (!Double.isFinite(fromX) || !Double.isFinite(fromY) || !Double.isFinite(toX)
                || !Double.isFinite(toY) || !Double.isFinite(dx) || !Double.isFinite(dy)) {
            throw new IllegalArgumentException("Segment coordinates and displacement must be finite");
        }
        if ((dx == 0 && (fromX <= blocker.x() || fromX >= blocker.right()))
                || (dy == 0 && (fromY <= blocker.y() || fromY >= blocker.top()))) {
            return false;
        }
        double xEnter = dx == 0 ? Double.NEGATIVE_INFINITY
                : (dx > 0 ? blocker.x() - fromX : blocker.right() - fromX) / dx;
        double xExit = dx == 0 ? Double.POSITIVE_INFINITY
                : (dx > 0 ? blocker.right() - fromX : blocker.x() - fromX) / dx;
        double yEnter = dy == 0 ? Double.NEGATIVE_INFINITY
                : (dy > 0 ? blocker.y() - fromY : blocker.top() - fromY) / dy;
        double yExit = dy == 0 ? Double.POSITIVE_INFINITY
                : (dy > 0 ? blocker.top() - fromY : blocker.y() - fromY) / dy;
        return Math.max(0, Math.max(xEnter, yEnter)) < Math.min(1, Math.min(xExit, yExit));
    }

    /**
     * Finds the first time of interior entry during a normalized linear step.
     * Initial interior overlap returns zero, even for a stationary body. Endpoint-only contact
     * and tangential motion return infinity, consistent with CollisionBox.overlaps.
     * @param moving initial moving rectangle
     * @param dx finite horizontal displacement
     * @param dy finite vertical displacement
     * @param target stationary candidate rectangle
     * @return entry fraction in [0, 1), or positive infinity if no interior intersection occurs
     * @throws NullPointerException if either rectangle is null
     * @throws IllegalArgumentException if displacement is non-finite
     */
    public static double entry(CollisionBox moving, double dx, double dy, CollisionBox target) {
        Objects.requireNonNull(moving, "moving");
        Objects.requireNonNull(target, "target");
        if (!Double.isFinite(dx) || !Double.isFinite(dy)) {
            throw new IllegalArgumentException("Sweep displacement must be finite");
        }
        if (moving.overlaps(target)) {
            return 0;
        }
        if ((dx == 0 && (moving.right() <= target.x() || moving.x() >= target.right()))
                || (dy == 0 && (moving.top() <= target.y() || moving.y() >= target.top()))) {
            return Double.POSITIVE_INFINITY;
        }
        double xEnter = dx == 0 ? Double.NEGATIVE_INFINITY
                : (dx > 0 ? target.x() - moving.right() : target.right() - moving.x()) / dx;
        double xExit = dx == 0 ? Double.POSITIVE_INFINITY
                : (dx > 0 ? target.right() - moving.x() : target.x() - moving.right()) / dx;
        double yEnter = dy == 0 ? Double.NEGATIVE_INFINITY
                : (dy > 0 ? target.y() - moving.top() : target.top() - moving.y()) / dy;
        double yExit = dy == 0 ? Double.POSITIVE_INFINITY
                : (dy > 0 ? target.top() - moving.y() : target.y() - moving.top()) / dy;
        double enter = Math.max(0, Math.max(xEnter, yEnter));
        double exit = Math.min(xExit, yExit);
        return enter < exit && enter < 1 ? enter : Double.POSITIVE_INFINITY;
    }
}
