package au.edu.anu.comp2120.world;

import java.util.Objects;

/**
 * Continuous axis-aligned rectangle intersection for straight projectile paths, without rendering.
 * Tangential edge/corner contact is not an interior hit; entry must occur before the step ends.
 * @author Daoyan Zhu (u7782042)
 */
public final class CollisionSweep {
    /** Prevents construction of the pure geometry utility. */
    private CollisionSweep() {
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
