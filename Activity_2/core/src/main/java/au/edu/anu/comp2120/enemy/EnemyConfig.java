package au.edu.anu.comp2120.enemy;

import au.edu.anu.comp2120.combat.AttackSpec;
import java.util.Objects;

/**
 * Immutable basic-enemy tuning. Distances are Euclidean centre-to-centre world units.
 * A larger loss radius than detection radius provides simple pursuit hysteresis.
 *
 * @param speed positive finite movement speed in world units per second
 * @param detectionRange positive finite distance at which an idle enemy acquires a target
 * @param loseRange finite distance beyond which pursuit stops, at least detectionRange
 * @param attackRange positive finite attack distance, no larger than detectionRange
 * @param attack shared combat tuning with a strictly positive cooldown for autonomous attacks
 * @author Daoyan Zhu (u7782042)
 */
public record EnemyConfig(double speed, double detectionRange, double loseRange,
        double attackRange, AttackSpec attack) {
    /**
     * Validates movement, nested ranges, and autonomous attack timing.
     * @param speed positive finite movement speed
     * @param detectionRange positive finite acquisition radius
     * @param loseRange finite pursuit-loss radius at least as large as detectionRange
     * @param attackRange positive finite attack radius no larger than detectionRange
     * @param attack validated attack specification with positive cooldown
     * @throws NullPointerException if attack is null
     * @throws IllegalArgumentException if speed/ranges are invalid or cooldown is zero
     */
    public EnemyConfig {
        Objects.requireNonNull(attack, "attack");
        if (!Double.isFinite(speed) || speed <= 0
                || !Double.isFinite(detectionRange) || detectionRange <= 0
                || !Double.isFinite(loseRange) || loseRange < detectionRange
                || !Double.isFinite(attackRange) || attackRange <= 0 || attackRange > detectionRange
                || !Double.isFinite(attackRange * 2) || attack.cooldownSeconds() <= 0) {
            throw new IllegalArgumentException("Enemy needs positive speed/ranges/cooldown and attack <= detection <= loss");
        }
    }
}
