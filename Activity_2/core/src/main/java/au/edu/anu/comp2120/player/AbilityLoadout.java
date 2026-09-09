package au.edu.anu.comp2120.player;

import au.edu.anu.comp2120.combat.AttackSpec;
import java.util.Objects;

/**
 * Immutable tuning for the common placeholder abilities, injectable without changing input code.
 * These area attacks are not final Archer, Mage, or Katana implementations.
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
     * Shared rectangular area attack with cooldown owned by its ability slot.
     * @param attack damage, label, active duration and slot cooldown
     * @param range positive finite extension on each side of the source hurtbox
     * @author Daoyan Zhu (u7782042)
     */
    public record AreaAttack(AttackSpec attack, double range) {
        /**
         * Validates timing and representable range before use.
         * @param attack validated combat specification
         * @param range extension in world units
         * @throws NullPointerException if attack is null
         * @throws IllegalArgumentException if range is invalid or cooldown is shorter than active time
         */
        public AreaAttack {
            Objects.requireNonNull(attack, "attack");
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
