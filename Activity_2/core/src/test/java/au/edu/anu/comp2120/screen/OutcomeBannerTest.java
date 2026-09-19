package au.edu.anu.comp2120.screen;

import au.edu.anu.comp2120.ActiveEncounter;
import au.edu.anu.comp2120.BattleResult;
import au.edu.anu.comp2120.player.PlayerClass;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies return feedback timing and responsive sizing without creating graphics resources.
 * @author Daoyan Zhu (u7782042)
 */
final class OutcomeBannerTest {
    /** Creates an isolated, graphics-free fixture. */
    OutcomeBannerTest() { }

    /**
     * Victory and defeat are visible before three seconds and expire at the exact boundary.
     * @param outcome completed battle outcome
     */
    @ParameterizedTest @EnumSource(BattleResult.Outcome.class)
    void bothOutcomesLastThreeSeconds(BattleResult.Outcome outcome) {
        SceneArt art = new SceneArt();
        assertFalse(art.hasOutcome());
        art.showOutcome(result(outcome), PlayerClass.ARCHER);
        art.advanceOutcome(0);
        assertTrue(art.hasOutcome());
        art.advanceOutcome(2.5);
        assertTrue(art.hasOutcome(), "The previous two-second timeout must not apply");
        art.advanceOutcome(0.499);
        assertTrue(art.hasOutcome());
        art.advanceOutcome(0.001);
        assertFalse(art.hasOutcome());
        art.advanceOutcome(10);
        assertFalse(art.hasOutcome());
    }

    /** A newer settled result replaces the old snapshot and gets its own full display window. */
    @Test void newResultRestartsTimer() {
        SceneArt art = new SceneArt();
        art.showOutcome(result(BattleResult.Outcome.FAILURE), PlayerClass.MAGE);
        art.advanceOutcome(2.5);
        art.showOutcome(result(BattleResult.Outcome.SUCCESS), PlayerClass.KATANA);
        art.advanceOutcome(2.5);
        assertTrue(art.hasOutcome());
        art.advanceOutcome(0.5);
        assertFalse(art.hasOutcome());
    }

    /** Large frame gaps expire feedback once without leaving a stale result on the next render. */
    @Test void largeTimeStepExpiresNotice() {
        SceneArt art = new SceneArt();
        art.showOutcome(result(BattleResult.Outcome.SUCCESS), PlayerClass.MAGE);
        art.advanceOutcome(Double.MAX_VALUE);
        assertFalse(art.hasOutcome());
    }

    /**
     * Bad elapsed time is rejected before changing an active notice.
     * @param delta rejected time value
     */
    @ParameterizedTest @ValueSource(doubles = {-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void invalidTimeDoesNotCorruptNotice(double delta) {
        SceneArt art = new SceneArt();
        art.showOutcome(result(BattleResult.Outcome.FAILURE), PlayerClass.ARCHER);
        assertThrows(IllegalArgumentException.class, () -> art.advanceOutcome(delta));
        assertTrue(art.hasOutcome());
        art.advanceOutcome(3);
        assertFalse(art.hasOutcome());
    }

    /** Missing snapshots cannot partially replace or restart existing feedback. */
    @Test void nullSnapshotsAreRejectedAtomically() {
        SceneArt art = new SceneArt();
        BattleResult result = result(BattleResult.Outcome.FAILURE);
        art.showOutcome(result, PlayerClass.MAGE);
        art.advanceOutcome(2.5);
        assertThrows(NullPointerException.class, () -> art.showOutcome(null, PlayerClass.MAGE));
        assertThrows(NullPointerException.class, () -> art.showOutcome(result, null));
        art.advanceOutcome(0.5);
        assertFalse(art.hasOutcome());
    }

    /**
     * Feedback remains sized for small windows even when ordinary HUD details are hidden.
     * @param width logical window width
     * @param height logical window height
     * @param expected capped scale
     */
    @ParameterizedTest
    @CsvSource({"960,540,1", "1920,1080,1", "480,270,0.5", "960,270,0.5", "480,1080,0.5",
            "240,135,0.25", "0,540,0", "960,0,0", "-1,540,0"})
    void bannerFitsBothAxesWithoutHudVisibilityGate(int width, int height, float expected) {
        assertEquals(expected, SceneArt.outcomeScale(width, height), 0.00001f);
        if (width > 0 && height > 0) {
            assertTrue(600 * expected <= width);
            assertTrue(180 * expected <= height);
        }
    }

    /**
     * Supplies an immutable outcome without real actors, rewards or Quest mutations.
     * @param outcome completed outcome
     * @return presentation snapshot
     */
    private BattleResult result(BattleResult.Outcome outcome) {
        return new BattleResult(new ActiveEncounter("test", "battle", "entry", "portal"), 1, outcome);
    }
}
