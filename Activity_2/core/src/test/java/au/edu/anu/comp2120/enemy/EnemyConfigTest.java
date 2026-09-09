package au.edu.anu.comp2120.enemy;

import static org.junit.jupiter.api.Assertions.*;

import au.edu.anu.comp2120.combat.AttackSpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Validates configurable enemy movement, nested ranges, and autonomous attack timing.
 *
 * @author Daoyan Zhu (u7782042)
 */
final class EnemyConfigTest {
    /** Valid attack shared by parameter-validation fixtures. */
    private final AttackSpec attack = new AttackSpec("basic", 10, 0.25, 1);

    /** Creates the configuration test fixture. */
    EnemyConfigTest() {
    }

    /** Equal radii are permitted when hysteresis is not desired. */
    @Test
    void acceptsEqualRangeBoundaries() {
        EnemyConfig config = new EnemyConfig(80, 40, 40, 40, attack);
        assertEquals(80, config.speed());
        assertEquals(40, config.detectionRange());
        assertEquals(40, config.loseRange());
        assertEquals(40, config.attackRange());
        assertSame(attack, config.attack());
        assertEquals(16, config.pathCellSize());
        assertEquals(0.5, config.repathSeconds());
    }

    /**
     * Non-positive/non-finite values are invalid for movement and all positive radii.
     * @param invalid invalid speed or radius
     */
    @ParameterizedTest
    @ValueSource(doubles = {0, -1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsInvalidSpeedAndRanges(double invalid) {
        assertThrows(IllegalArgumentException.class, () -> new EnemyConfig(invalid, 200, 260, 40, attack));
        assertThrows(IllegalArgumentException.class, () -> new EnemyConfig(80, invalid, 260, 40, attack));
        assertThrows(IllegalArgumentException.class, () -> new EnemyConfig(80, 200, invalid, 40, attack));
        assertThrows(IllegalArgumentException.class, () -> new EnemyConfig(80, 200, 260, invalid, attack));
        assertThrows(IllegalArgumentException.class, () -> new EnemyConfig(80, 200, 260, 40, attack, invalid, 0.5));
        assertThrows(IllegalArgumentException.class, () -> new EnemyConfig(80, 200, 260, 40, attack, 16, invalid));
    }

    /** Ranges must nest and their square hitbox diameter must remain representable. */
    @Test
    void rejectsInconsistentOrOverflowingRanges() {
        assertThrows(IllegalArgumentException.class, () -> new EnemyConfig(80, 30, 260, 40, attack));
        assertThrows(IllegalArgumentException.class, () -> new EnemyConfig(80, 200, 199, 40, attack));
        assertThrows(IllegalArgumentException.class,
                () -> new EnemyConfig(80, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, attack));
    }

    /** Autonomous repeated requests require a positive cooldown and a real attack specification. */
    @Test
    void rejectsMissingAttackAndZeroCooldown() {
        assertThrows(NullPointerException.class, () -> new EnemyConfig(80, 200, 260, 40, null));
        AttackSpec noCooldown = new AttackSpec("zero", 10, 0.25, 0);
        assertThrows(IllegalArgumentException.class, () -> new EnemyConfig(80, 200, 260, 40, noCooldown));
    }
}
