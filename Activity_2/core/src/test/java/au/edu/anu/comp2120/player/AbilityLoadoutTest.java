package au.edu.anu.comp2120.player;

import static org.junit.jupiter.api.Assertions.*;

import au.edu.anu.comp2120.combat.AttackSpec;
import au.edu.anu.comp2120.screen.GameConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests validated, replaceable ability tuning and numerical constraints.
 * @author Daoyan Zhu (u7782042)
 */
final class AbilityLoadoutTest {
    /** Creates an independent immutable-configuration fixture. */
    AbilityLoadoutTest() {
    }

    /**
     * Invalid or unrepresentable ranges, speeds, and durations cannot enter ability execution.
     * @param value invalid numeric tuning
     */
    @ParameterizedTest
    @ValueSource(doubles = {0, -1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.MAX_VALUE})
    void rejectsInvalidGeometryAndTiming(double value) {
        AttackSpec spec = new AttackSpec("test", 1, 1, 2);
        assertThrows(IllegalArgumentException.class, () -> new AbilityLoadout.AreaAttack(spec, value));
        assertThrows(IllegalArgumentException.class, () -> new AbilityLoadout.Dash(value, 2, 3));
        assertThrows(IllegalArgumentException.class, () -> new AbilityLoadout.Dash(2, value, 3));
    }

    /** Positive cooldowns must cover the active window; every loadout slot must be configured. */
    @Test
    void validatesRequiredSlotsAndCooldowns() {
        AbilityLoadout defaults = GameConfig.PLAYER_ABILITIES;
        assertThrows(NullPointerException.class, () -> new AbilityLoadout(null, defaults.special(),
                defaults.dash(), defaults.ultimate()));
        assertThrows(NullPointerException.class, () -> new AbilityLoadout.AreaAttack(null, 1));
        assertThrows(IllegalArgumentException.class, () -> new AbilityLoadout.AreaAttack(
                new AttackSpec("test", 1, 1, 0), 1));
        for (double cooldown : new double[] {-1, 0, 0.1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> new AbilityLoadout.Dash(100, 0.2, cooldown));
        }
        assertThrows(IllegalArgumentException.class, () -> new AbilityLoadout.Dash(Double.MIN_VALUE,
                Double.MIN_VALUE, 1));
        assertEquals(1, new AbilityLoadout.Dash(100, 1, 1).cooldown());
        assertEquals(2, new AbilityLoadout.AreaAttack(new AttackSpec("test", 1, 2, 2), 10)
                .attack().cooldownSeconds());
    }
}
