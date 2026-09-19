package au.edu.anu.comp2120.enemy;

import au.edu.anu.comp2120.combat.AttackSpec;
import java.util.List;
import java.util.Objects;

/**
 * Capped logarithmic rematch tuning applied once to a fresh encounter, never to an active Boss.
 * Health grows faster than attack frequency; damage, movement and warning duration remain unchanged.
 * @author Daoyan Zhu (u7782042)
 */
public final class BossScaling {
    /** Added health multiplier per log10(successes + 1). */
    public static final double HEALTH_GROWTH = 1;
    /** Maximum health relative to the unscaled definition. */
    public static final double MAX_HEALTH_MULTIPLIER = 4;
    /** Added attack frequency multiplier per log10(successes + 1). */
    public static final double ATTACK_RATE_GROWTH = 0.25;
    /** Cooldowns cannot become shorter than half their unscaled value. */
    public static final double MAX_ATTACK_RATE = 2;

    /** Prevents instances of this pure tuning utility. */
    private BossScaling() { }

    /**
     * Computes health growth without integer overflow in the logarithm argument.
     * @param successes previous victories against this Boss, not attempts or other guardians
     * @return multiplier in [1, 4]
     * @throws IllegalArgumentException if successes is negative
     */
    public static double healthMultiplier(int successes) {
        return Math.min(MAX_HEALTH_MULTIPLIER, 1 + HEALTH_GROWTH * growth(successes));
    }

    /**
     * Computes attack frequency growth; intervals are divided by this multiplier.
     * @param successes previous victories against this Boss
     * @return frequency multiplier in [1, 2]
     * @throws IllegalArgumentException if successes is negative
     */
    public static double attackRate(int successes) {
        return Math.min(MAX_ATTACK_RATE, 1 + ATTACK_RATE_GROWTH * growth(successes));
    }

    /**
     * Derives a new immutable definition from the original, avoiding compounded per-retry scaling.
     * Active hitbox windows are preserved, so a cooldown is also clamped to its active window.
     * @param base original unscaled definition
     * @param successes previous victories against this Boss
     * @return capped encounter definition; the original instance when successes is zero
     * @throws NullPointerException if base is null
     * @throws IllegalArgumentException if successes is negative
     */
    public static BossConfig apply(BossConfig base, int successes) {
        Objects.requireNonNull(base, "base Boss configuration");
        double health = healthMultiplier(successes), rate = attackRate(successes);
        if (successes == 0) { return base; }
        int hp = (int) Math.min(Integer.MAX_VALUE, Math.round(base.maxHp() * health));
        return new BossConfig(hp, base.navigation(), base.phaseTwoFraction(), base.transitionSeconds(),
                patterns(base.phaseOne(), rate), patterns(base.phaseTwo(), rate),
                base.materialId(), base.materialQuantity());
    }

    /**
     * Copies only pattern cooldowns, preserving shared damage categories and geometry.
     * @param original immutable phase patterns
     * @param rate bounded frequency multiplier
     * @return new immutable pattern list
     */
    private static List<BossConfig.Pattern> patterns(List<BossConfig.Pattern> original, double rate) {
        return original.stream().map(pattern -> {
            AttackSpec attack = pattern.attack();
            AttackSpec scaled = new AttackSpec(attack.type(), attack.damage(), attack.activeSeconds(),
                    Math.max(attack.activeSeconds(), attack.cooldownSeconds() / rate),
                    attack.element(), attack.blockable(), attack.damageType());
            return new BossConfig.Pattern(scaled, pattern.range(), pattern.shape());
        }).toList();
    }

    /**
     * Evaluates the shared log10 curve after validating its count.
     * @param successes non-negative victory count
     * @return non-negative finite logarithmic growth
     * @throws IllegalArgumentException if successes is negative
     */
    private static double growth(int successes) {
        if (successes < 0) { throw new IllegalArgumentException("Boss successes must not be negative"); }
        return Math.log10(1.0 + successes);
    }
}
