package au.edu.anu.comp2120.enemy;

import au.edu.anu.comp2120.combat.AttackSpec;
import au.edu.anu.comp2120.player.AttackShape;
import java.util.List;
import java.util.Objects;

/**
 * Immutable two-phase Boss tuning using existing enemy navigation and combat attack data.
 * @param maxHp positive shared health capacity
 * @param navigation existing detection, pursuit, stopping and A* tuning; basic attack is not submitted
 * @param phaseTwoFraction HP fraction in (0,1) that enters phase two, inclusive at the threshold
 * @param transitionSeconds positive finite one-time positioning/attack pause
 * @param phaseOne non-empty ordered attack cycle for phase one
 * @param phaseTwo non-empty ordered attack cycle for phase two
 * @param materialId stable non-blank material identifier for future inventory integration
 * @param materialQuantity positive quantity dropped once per encounter
 * @author Daoyan Zhu (u7782042)
 */
public record BossConfig(int maxHp, EnemyConfig navigation, double phaseTwoFraction,
        double transitionSeconds, List<Pattern> phaseOne, List<Pattern> phaseTwo,
        String materialId, int materialQuantity) {
    /**
     * Validates and defensively copies encounter data before any actor is registered.
     * @param maxHp positive health capacity
     * @param navigation shared enemy decision configuration
     * @param phaseTwoFraction finite threshold strictly between zero and one
     * @param transitionSeconds positive finite transition duration
     * @param phaseOne ordered first-phase patterns
     * @param phaseTwo ordered second-phase patterns
     * @param materialId non-blank material identity
     * @param materialQuantity positive reward quantity
     * @throws NullPointerException if required data or any pattern is null
     * @throws IllegalArgumentException if health, timing, threshold, reward or attack cycles are invalid
     */
    public BossConfig {
        Objects.requireNonNull(navigation, "navigation");
        Objects.requireNonNull(materialId, "material ID");
        phaseOne = List.copyOf(phaseOne);
        phaseTwo = List.copyOf(phaseTwo);
        if (maxHp <= 0 || !Double.isFinite(phaseTwoFraction) || phaseTwoFraction <= 0 || phaseTwoFraction >= 1
                || !Double.isFinite(transitionSeconds) || transitionSeconds <= 0
                || materialId.isBlank() || materialQuantity <= 0 || phaseOne.isEmpty() || phaseTwo.isEmpty()) {
            throw new IllegalArgumentException("Boss requires legal health, phase timing, non-empty cycles and reward");
        }
        for (List<Pattern> cycle : List.of(phaseOne, phaseTwo)) {
            if (cycle.stream().anyMatch(pattern -> pattern.range() < navigation.attackRange())) {
                throw new IllegalArgumentException("Boss pattern reach must cover its navigation stopping distance");
            }
        }
    }

    /**
     * One replaceable fixed-hitbox attack pattern; timing and damage remain combat-core data.
     * @param attack shared specification, with cooldown at least its active window
     * @param range positive finite geometry reach
     * @param shape pure existing attack geometry strategy
     * @author Daoyan Zhu (u7782042)
     */
    public record Pattern(AttackSpec attack, double range, AttackShape shape) {
        /**
         * Validates an autonomous non-overlapping attack definition.
         * @param attack specification with a positive cooldown no shorter than its active window
         * @param range positive finite reach with representable double extent
         * @param shape pure geometry strategy
         * @throws NullPointerException if attack or shape is null
         * @throws IllegalArgumentException if range or timing is invalid
         */
        public Pattern {
            Objects.requireNonNull(attack, "attack");
            Objects.requireNonNull(shape, "shape");
            if (!Double.isFinite(range) || range <= 0 || !Double.isFinite(2 * range)
                    || attack.cooldownSeconds() < attack.activeSeconds()) {
                throw new IllegalArgumentException("Boss pattern requires finite positive reach and cooldown >= active time");
            }
        }
    }
}
