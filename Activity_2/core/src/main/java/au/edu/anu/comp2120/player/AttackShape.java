package au.edu.anu.comp2120.player;

import au.edu.anu.comp2120.world.CollisionBox;
import java.util.Objects;

/**
 * Small geometry strategy for common attack slots; cannot apply damage or mutate the player.
 * Implementations must be deterministic, side-effect-free and return valid immutable geometry.
 * This is a hitbox extension point, not a projectile/animation/complete skill execution interface.
 * @author Daoyan Zhu (u7782042)
 */
@FunctionalInterface
public interface AttackShape {
    /** All-direction rectangle used by the original common abilities and placeholder area spells. */
    AttackShape AREA = AttackShape::area;
    /** Rectangle covering the source and its normalized forward reach; not a moving projectile. */
    AttackShape FORWARD = AttackShape::forward;

    /**
     * Builds one attack's fixed hitbox using world geometry rather than rendering dimensions.
     * @param source independent source hurtbox
     * @param range positive finite configured reach
     * @param facingX finite horizontal direction component
     * @param facingY finite vertical direction component; the direction must be nonzero
     * @return non-null immutable attack rectangle
     * @throws NullPointerException if source is null
     * @throws IllegalArgumentException if input or resulting geometry is invalid
     */
    CollisionBox create(CollisionBox source, double range, double facingX, double facingY);

    /**
     * Expands all sides without coupling hit detection to class identity.
     * @param source independent source hurtbox
     * @param range extension on each side
     * @param facingX validated horizontal intent, unused by this strategy
     * @param facingY validated vertical intent, unused by this strategy
     * @return expanded rectangle
     * @throws NullPointerException if source is null
     * @throws IllegalArgumentException if inputs or resulting rectangle are invalid
     */
    private static CollisionBox area(CollisionBox source, double range, double facingX, double facingY) {
        validate(source, range, facingX, facingY);
        return new CollisionBox(source.x() - range, source.y() - range,
                source.width() + 2 * range, source.height() + 2 * range);
    }

    /**
     * Creates an axis-aligned envelope extending in the normalized facing direction.
     * Diagonal envelopes are conservative rectangles, not rotated blades or swept projectiles.
     * @param source independent source hurtbox
     * @param range forward reach
     * @param facingX horizontal intent
     * @param facingY vertical intent
     * @return forward rectangle including the source hurtbox
     * @throws NullPointerException if source is null
     * @throws IllegalArgumentException if inputs or resulting rectangle are invalid
     */
    private static CollisionBox forward(CollisionBox source, double range, double facingX, double facingY) {
        validate(source, range, facingX, facingY);
        double length = Math.hypot(facingX, facingY);
        double dx = facingX / length * range;
        double dy = facingY / length * range;
        return new CollisionBox(source.x() + Math.min(0, dx), source.y() + Math.min(0, dy),
                source.width() + Math.abs(dx), source.height() + Math.abs(dy));
    }

    /**
     * Checks geometry inputs before any strategy constructs a rectangle.
     * @param source source hurtbox
     * @param range positive reach
     * @param facingX horizontal component
     * @param facingY vertical component
     * @throws NullPointerException if source is null
     * @throws IllegalArgumentException if reach or direction is invalid
     */
    private static void validate(CollisionBox source, double range, double facingX, double facingY) {
        Objects.requireNonNull(source, "source");
        double length = Math.hypot(facingX, facingY);
        if (!Double.isFinite(range) || range <= 0 || !Double.isFinite(2 * range)
                || !Double.isFinite(length) || length == 0) {
            throw new IllegalArgumentException("Attack shape needs finite positive reach and nonzero finite facing");
        }
    }
}
