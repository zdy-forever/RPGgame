package au.edu.anu.comp2120.player;

import au.edu.anu.comp2120.combat.AttackSpec;
import au.edu.anu.comp2120.combat.ProjectileSpec;
import java.util.Objects;

/**
 * Immutable tuning for the common placeholder abilities, injectable without changing input code.
 * Attack geometry is replaceable; these placeholders are not final class skill implementations.
 * @param light light attack tuning
 * @param special special attack tuning
 * @param dash movement burst tuning
 * @param ultimate ultimate attack tuning
 * @author Daoyan Zhu (u7782042)
 */
public record AbilityLoadout(AreaAttack light, AreaAttack special, Dash dash, AreaAttack ultimate) {
    /**
     * Requires a complete loadout.
     * @param light light attack
     * @param special special attack
     * @param dash movement burst
     * @param ultimate ultimate attack
     * @throws NullPointerException if a slot is null
     */
    public AbilityLoadout {
        Objects.requireNonNull(light, "light");
        Objects.requireNonNull(special, "special");
        Objects.requireNonNull(dash, "dash");
        Objects.requireNonNull(ultimate, "ultimate");
    }

    /**
     * Shared attack tuning with shape or projectile delivery and slot-owned cooldown.
     * The historical AreaAttack name is retained for compatibility with existing class definitions.
     * Projectile flight uses range as its distance cap and ProjectileSpec.lifetime as its expiry;
     * AttackSpec.activeSeconds is only the launch window in that mode. Shape is ignored in flight mode.
     * @param attack damage, label, active duration and slot cooldown
     * @param range positive finite reach interpreted by the selected shape strategy
     * @param shape side-effect-free strategy interpreting range as reach or area extension
     * @param projectile flight configuration, or null for a shape-generated area attack
     * @author Daoyan Zhu (u7782042)
     */
    public record AreaAttack(AttackSpec attack, double range, AttackShape shape, ProjectileSpec projectile) {
        /**
         * Preserves existing shape-only class definitions without changing their execution.
         * @param attack combat tuning
         * @param range shape reach
         * @param shape geometry strategy
         * @throws NullPointerException if attack or shape is null
         * @throws IllegalArgumentException if range or timing is invalid
         */
        public AreaAttack(AttackSpec attack, double range, AttackShape shape) {
            this(attack, range, shape, null);
        }

        /**
         * Preserves the original all-direction placeholder constructor.
         * @param attack validated combat specification
         * @param range extension on all sides
         * @throws NullPointerException if attack is null
         * @throws IllegalArgumentException if range or timing is invalid
         */
        public AreaAttack(AttackSpec attack, double range) {
            this(attack, range, AttackShape.AREA);
        }

        /**
         * Validates timing and representable range before use for either delivery mode.
         * @param attack validated combat specification
         * @param range reach in world units interpreted by the strategy
         * @param shape hitbox construction strategy
         * @param projectile optional flight mode; shape is used only when this is null
         * @throws NullPointerException if attack or shape is null
         * @throws IllegalArgumentException if range is invalid or cooldown is shorter than active time
         */
        public AreaAttack {
            Objects.requireNonNull(attack, "attack");
            Objects.requireNonNull(shape, "shape");
            if (!Double.isFinite(range) || range <= 0 || !Double.isFinite(2 * range)
                    || attack.cooldownSeconds() < attack.activeSeconds()) {
                throw new IllegalArgumentException("Area attack needs positive range and cooldown >= active time");
            }
        }
    }

    /**
     * Fixed-direction burst with no invulnerability or damage attached.
     * @param speed positive world units per second
     * @param duration positive active seconds
     * @param cooldown seconds from trigger, at least duration
     * @author Daoyan Zhu (u7782042)
     */
    public record Dash(double speed, double duration, double cooldown) {
        /**
         * Validates finite positive travel and timing.
         * @param speed burst speed
         * @param duration burst duration
         * @param cooldown slot cooldown from trigger
         * @throws IllegalArgumentException if timing, speed or total distance is invalid
         */
        public Dash {
            if (!Double.isFinite(speed) || speed <= 0 || !Double.isFinite(duration) || duration <= 0
                    || !Double.isFinite(cooldown) || cooldown < duration
                    || !Double.isFinite(speed * duration) || speed * duration <= 0) {
                throw new IllegalArgumentException("Dash needs positive finite distance and cooldown >= duration");
            }
        }
    }
}
