package au.edu.anu.comp2120.combat;

import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.WalkableArea;
import java.util.Objects;

/**
 * One straight-moving non-piercing projectile's geometry and flight budget, owned by CombatSystem.
 * Public access is read-only; target selection and damage remain in the shared pipeline.
 * @author Daoyan Zhu (u7782042)
 */
public final class Projectile {
    /** Current square collision geometry, also exposed for projectile presentation. */
    private CollisionBox bounds;
    /** Unit horizontal flight direction. */
    private final double directionX;
    /** Unit vertical flight direction. */
    private final double directionY;
    /** Positive finite flight speed. */
    private final double speed;
    /** Collision environment captured at launch, never switched during flight. */
    private final WalkableArea area;
    /** Remaining maximum travel distance. */
    private double remainingRange;
    /** Remaining flight seconds. */
    private double remainingSeconds;

    /**
     * Creates a validated shot before the pipeline mutates IDs, registrations or cooldowns.
     * @param bounds walkable initial square
     * @param spec validated flight tuning
     * @param range positive finite maximum distance
     * @param directionX nonzero direction's horizontal component
     * @param directionY nonzero direction's vertical component
     * @param area collision environment
     * @throws NullPointerException if bounds, spec or area is null
     * @throws IllegalArgumentException if direction, range, placement or endpoints are invalid
     */
    Projectile(CollisionBox bounds, ProjectileSpec spec, double range,
            double directionX, double directionY, WalkableArea area) {
        this.bounds = Objects.requireNonNull(bounds, "bounds");
        Objects.requireNonNull(spec, "projectile specification");
        this.area = Objects.requireNonNull(area, "area");
        double length = Math.hypot(directionX, directionY);
        if (!Double.isFinite(length) || length == 0 || !Double.isFinite(range) || range <= 0
                || !area.isWalkable(bounds)) {
            throw new IllegalArgumentException("Projectile needs finite direction/range and a walkable origin");
        }
        this.directionX = directionX / length;
        this.directionY = directionY / length;
        speed = spec.speed();
        remainingRange = range;
        remainingSeconds = spec.lifetime();
        double endX = bounds.x() + this.directionX * range;
        double endY = bounds.y() + this.directionY * range;
        if (!Double.isFinite(endX) || !Double.isFinite(endY)
                || !Double.isFinite(endX + bounds.width()) || endX + bounds.width() <= endX
                || !Double.isFinite(endY + bounds.height()) || endY + bounds.height() <= endY) {
            throw new IllegalArgumentException("Projectile endpoint must preserve finite collision dimensions");
        }
    }

    /**
     * Returns current immutable geometry, not a texture or sprite rectangle.
     * @return current projectile body
     */
    public CollisionBox getBounds() {
        return bounds;
    }

    /**
     * Returns the captured horizontal direction.
     * @return normalized horizontal component
     */
    public double getDirectionX() {
        return directionX;
    }

    /**
     * Returns the captured vertical direction.
     * @return normalized vertical component
     */
    public double getDirectionY() {
        return directionY;
    }

    /**
     * Returns remaining permitted travel.
     * @return non-negative world distance
     */
    public double getRemainingRange() {
        return remainingRange;
    }

    /**
     * Returns remaining flight time.
     * @return non-negative seconds; zero after cancellation
     */
    public double getRemainingSeconds() {
        return remainingSeconds;
    }

    /**
     * Reports whether this projectile can still move or settle a hit.
     * @return true while both lifetime and range remain
     */
    public boolean isActive() {
        return remainingSeconds > 0 && remainingRange > 0;
    }

    /**
     * Caps travel by both remaining time and range before multiplying large frame deltas.
     * @param delta validated non-negative finite elapsed seconds
     * @return non-negative finite path length for this step
     */
    double stepDistance(double delta) {
        return Math.min(remainingRange, speed * Math.min(delta, remainingSeconds));
    }

    /**
     * Queries first wall/world contact along a straight flight segment.
     * @param distance validated step distance
     * @return first blocking fraction, or infinity
     */
    double firstBlock(double distance) {
        return area.firstBlock(bounds, directionX * distance, directionY * distance);
    }

    /**
     * Moves to the chosen contact/end point and consumes flight budgets; never resolves damage.
     * Corrects contact rounding toward the last valid position. If a collision provider still rejects
     * the result, cancels at that last position instead of retaining illegal geometry.
     * @param distance validated distance travelled in this step
     * @param delta validated elapsed simulation seconds
     */
    void advance(double distance, double delta) {
        CollisionBox next = new CollisionBox(bounds.x() + directionX * distance, bounds.y() + directionY * distance,
                bounds.width(), bounds.height());
        if (!area.isWalkable(next)) {
            next = new CollisionBox(Math.nextAfter(next.x(), bounds.x()), Math.nextAfter(next.y(), bounds.y()),
                    bounds.width(), bounds.height());
        }
        if (area.isWalkable(next)) {
            bounds = next;
        } else {
            cancel();
        }
        remainingRange = Math.max(0, remainingRange - distance);
        remainingSeconds = Math.max(0, remainingSeconds - delta);
        if (remainingRange == 0) {
            cancel();
        }
    }

    /** Cancels flight; retained observational references also report inactive. */
    void cancel() {
        remainingSeconds = 0;
    }
}
