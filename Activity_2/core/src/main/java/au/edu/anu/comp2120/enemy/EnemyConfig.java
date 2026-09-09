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
 * @param pathCellSize positive finite navigation-grid spacing
 * @param repathSeconds positive finite minimum interval between obstructed-path searches
 * @author Daoyan Zhu (u7782042)
 */
public record EnemyConfig(double speed, double detectionRange, double loseRange,
        double attackRange, AttackSpec attack, double pathCellSize, double repathSeconds) {
    /** Default world-unit spacing for full-body A* navigation. */
    public static final double DEFAULT_PATH_CELL_SIZE = 16;
    /** Default seconds between blocked-path retries or moving-target replans. */
    public static final double DEFAULT_REPATH_SECONDS = 0.5;

    /**
     * Preserves existing enemy configuration calls using the default navigation tuning.
     * @param speed positive finite movement speed
     * @param detectionRange positive finite acquisition radius
     * @param loseRange pursuit-loss radius at least detectionRange
     * @param attackRange positive attack radius no larger than detectionRange
     * @param attack shared specification with positive cooldown
     * @throws NullPointerException if attack is null
     * @throws IllegalArgumentException if movement, ranges, or attack timing is invalid
     */
    public EnemyConfig(double speed, double detectionRange, double loseRange, double attackRange, AttackSpec attack) {
        this(speed, detectionRange, loseRange, attackRange, attack, DEFAULT_PATH_CELL_SIZE, DEFAULT_REPATH_SECONDS);
    }
    /**
     * Validates movement, nested ranges, and autonomous attack timing.
     * @param speed positive finite movement speed
     * @param detectionRange positive finite acquisition radius
     * @param loseRange finite pursuit-loss radius at least as large as detectionRange
     * @param attackRange positive finite attack radius no larger than detectionRange
     * @param attack validated attack specification with positive cooldown
     * @param pathCellSize positive finite navigation-grid spacing
     * @param repathSeconds positive finite replan interval
     * @throws NullPointerException if attack is null
     * @throws IllegalArgumentException if speed/ranges/navigation timing are invalid or cooldown is zero
     */
    public EnemyConfig {
        Objects.requireNonNull(attack, "attack");
        if (!Double.isFinite(speed) || speed <= 0
                || !Double.isFinite(detectionRange) || detectionRange <= 0
                || !Double.isFinite(loseRange) || loseRange < detectionRange
                || !Double.isFinite(attackRange) || attackRange <= 0 || attackRange > detectionRange
                || !Double.isFinite(attackRange * 2) || attack.cooldownSeconds() <= 0
                || !Double.isFinite(pathCellSize) || pathCellSize <= 0
                || !Double.isFinite(repathSeconds) || repathSeconds <= 0) {
            throw new IllegalArgumentException("Enemy needs positive finite movement/navigation tuning, positive cooldown and attack <= detection <= loss");
        }
    }
}
