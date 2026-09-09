package au.edu.anu.comp2120.enemy;

import static org.junit.jupiter.api.Assertions.*;

import au.edu.anu.comp2120.combat.AttackSpec;
import au.edu.anu.comp2120.inventory.MaterialDrop;
import au.edu.anu.comp2120.player.AttackShape;
import au.edu.anu.comp2120.screen.GameConfig;
import au.edu.anu.comp2120.world.CollisionBox;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Checks immutable Boss tuning and material data boundaries without an arena or assets.
 * @author Daoyan Zhu (u7782042)
 */
final class BossConfigTest {
    /** Valid production configuration reused only as immutable data. */
    private final BossConfig valid = GameConfig.SENTINEL;

    /** Creates an isolated validation fixture. */
    BossConfigTest() {
    }

    /**
     * Thresholds must represent an attainable intermediate phase, not zero, full HP or NaN.
     * @param fraction invalid HP fraction
     */
    @ParameterizedTest
    @ValueSource(doubles = {-1, 0, 1, 2, Double.NaN, Double.POSITIVE_INFINITY})
    void rejectsInvalidThreshold(double fraction) {
        assertThrows(IllegalArgumentException.class, () -> new BossConfig(400, valid.navigation(), fraction,
                1, valid.phaseOne(), valid.phaseTwo(), "core", 1));
    }

    /**
     * Phase transition duration must be finite and positive.
     * @param seconds invalid duration
     */
    @ParameterizedTest
    @ValueSource(doubles = {-1, 0, Double.NaN, Double.POSITIVE_INFINITY})
    void rejectsInvalidTransitionDuration(double seconds) {
        assertThrows(IllegalArgumentException.class, () -> new BossConfig(400, valid.navigation(), 0.5,
                seconds, valid.phaseOne(), valid.phaseTwo(), "core", 1));
    }

    /** Pattern lists are copied and must be non-empty, non-null and long enough to reach the target. */
    @Test
    void validatesAndCopiesPatternCycles() {
        List<BossConfig.Pattern> mutable = new ArrayList<>(valid.phaseOne());
        BossConfig copied = new BossConfig(400, valid.navigation(), 0.5, 1, mutable, mutable, "core", 1);
        mutable.clear();
        assertEquals(2, copied.phaseOne().size());
        assertThrows(UnsupportedOperationException.class, () -> copied.phaseOne().clear());
        assertThrows(IllegalArgumentException.class, () -> new BossConfig(400, valid.navigation(), 0.5, 1,
                List.of(), valid.phaseTwo(), "core", 1));
        assertThrows(NullPointerException.class, () -> new BossConfig(400, valid.navigation(), 0.5, 1,
                null, valid.phaseTwo(), "core", 1));
        BossConfig.Pattern tooShort = new BossConfig.Pattern(new AttackSpec("short", 1, 0.1, 1), 1, AttackShape.AREA);
        assertThrows(IllegalArgumentException.class, () -> new BossConfig(400, valid.navigation(), 0.5, 1,
                List.of(tooShort), valid.phaseTwo(), "core", 1));
    }

    /** Zero cooldown and unrepresentable ranges cannot produce a per-frame autonomous damage loop. */
    @Test
    void patternsRejectInvalidTimingAndGeometry() {
        assertThrows(IllegalArgumentException.class, () -> new BossConfig.Pattern(
                new AttackSpec("bad", 1, 0.2, 0), 10, AttackShape.AREA));
        assertThrows(IllegalArgumentException.class, () -> new BossConfig.Pattern(
                new AttackSpec("bad", 1, 0.2, 0.1), 10, AttackShape.AREA));
        for (double range : new double[] {0, -1, Double.NaN, Double.POSITIVE_INFINITY, Double.MAX_VALUE}) {
            assertThrows(IllegalArgumentException.class, () -> new BossConfig.Pattern(
                    new AttackSpec("bad", 1, 0.2, 1), range, AttackShape.AREA));
        }
        assertThrows(NullPointerException.class, () -> new BossConfig.Pattern(null, 10, AttackShape.AREA));
        assertThrows(NullPointerException.class, () -> new BossConfig.Pattern(new AttackSpec("bad", 1, 0.2, 1), 10, null));
    }

    /** Invalid HP and reward metadata fail when configuration is constructed, not at Boss death. */
    @Test
    void validatesHealthAndRewardBeforeEncounter() {
        assertThrows(IllegalArgumentException.class, () -> new BossConfig(0, valid.navigation(), 0.5, 1,
                valid.phaseOne(), valid.phaseTwo(), "core", 1));
        assertThrows(IllegalArgumentException.class, () -> new BossConfig(400, valid.navigation(), 0.5, 1,
                valid.phaseOne(), valid.phaseTwo(), " ", 1));
        assertThrows(IllegalArgumentException.class, () -> new BossConfig(400, valid.navigation(), 0.5, 1,
                valid.phaseOne(), valid.phaseTwo(), "core", 0));
        assertThrows(NullPointerException.class, () -> new BossConfig(400, null, 0.5, 1,
                valid.phaseOne(), valid.phaseTwo(), "core", 1));
    }

    /** Material data rejects malformed IDs/counts/geometry without requiring an Inventory implementation. */
    @Test
    void validatesMaterialDropContract() {
        CollisionBox bounds = new CollisionBox(0, 0, 12, 12);
        MaterialDrop drop = new MaterialDrop("instance", "core", 2, "map", bounds);
        assertEquals(2, drop.quantity());
        assertEquals(bounds, drop.bounds());
        assertThrows(IllegalArgumentException.class, () -> new MaterialDrop(" ", "core", 1, "map", bounds));
        assertThrows(IllegalArgumentException.class, () -> new MaterialDrop("id", " ", 1, "map", bounds));
        assertThrows(IllegalArgumentException.class, () -> new MaterialDrop("id", "core", 0, "map", bounds));
        assertThrows(IllegalArgumentException.class, () -> new MaterialDrop("id", "core", 1, " ", bounds));
        assertThrows(NullPointerException.class, () -> new MaterialDrop("id", "core", 1, "map", null));
    }
}
