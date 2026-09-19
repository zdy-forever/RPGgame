package au.edu.anu.comp2120.screen;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Verifies capped HUD scaling and both visibility thresholds without a graphics context.
 * @author Daoyan Zhu (u7782042)
 */
final class HudLayoutTest {
    /** Allowed rounding error for float UI scales. */
    private static final float EPSILON = 0.00001f;

    /** Creates an independent HUD policy test fixture. */
    HudLayoutTest() { }

    /**
     * A window at or above both reference dimensions cannot enlarge any persistent HUD element.
     * @param width window width
     * @param height window height
     */
    @ParameterizedTest
    @CsvSource({"960,540", "1920,1080", "3840,2160", "960,2160", "3840,540", "2147483647,2147483647"})
    void capsAtReferenceSize(int width, int height) {
        HudLayout layout = HudLayout.forWindow(width, height);
        assertEquals(GameConfig.HUD_MAX_SCALE, layout.scale());
        assertTrue(layout.healthVisible());
        assertTrue(layout.detailsVisible());
        assertEquals(HudLayout.forWindow(960, 540), layout);
    }

    /**
     * The smaller relative axis controls proportional scaling, including narrow and short windows.
     * @param width window width
     * @param height window height
     * @param ratio expected scale relative to maximum
     */
    @ParameterizedTest
    @CsvSource({"480,270,0.5", "480,1080,0.5", "1920,270,0.5",
            "720,405,0.75", "720,1080,0.75", "1920,405,0.75", "640,360,0.666666667"})
    void scalesWithBothAxes(int width, int height, float ratio) {
        assertEquals(GameConfig.HUD_MAX_SCALE * ratio, HudLayout.forWindow(width, height).scale(), EPSILON);
    }

    /**
     * Equality remains visible; one unit below either threshold hides the corresponding elements.
     * @param width window width
     * @param height window height
     * @param health expected HP visibility
     * @param details expected coordinates/map/weapon visibility
     */
    @ParameterizedTest
    @CsvSource({"640,360,true,true", "639,360,true,false", "640,359,true,false",
            "320,180,true,false", "319,540,false,false", "960,179,false,false",
            "480,1000,true,false", "1000,270,true,false", "1,1,false,false"})
    void usesTwoThresholds(int width, int height, boolean health, boolean details) {
        HudLayout layout = HudLayout.forWindow(width, height);
        assertEquals(health, layout.healthVisible());
        assertEquals(details, layout.detailsVisible());
        assertTrue(Float.isFinite(layout.scale()));
        assertTrue(layout.scale() > 0);
    }

    /**
     * Minimized or invalid-size callbacks never produce a negative/NaN scale or visible HUD.
     * @param width non-positive width or companion dimension
     * @param height non-positive height or companion dimension
     */
    @ParameterizedTest
    @CsvSource({"0,540", "960,0", "0,0", "-1,540", "960,-1", "-2147483648,-2147483648"})
    void hidesForNonPositiveDimensions(int width, int height) {
        assertEquals(new HudLayout(0, false, false), HudLayout.forWindow(width, height));
    }

    /** Enlarging again restores the exact original policy without retaining hidden-state history. */
    @Test
    void restoresAfterShrinking() {
        HudLayout initial = HudLayout.forWindow(960, 540);
        assertFalse(HudLayout.forWindow(500, 300).detailsVisible());
        assertFalse(HudLayout.forWindow(200, 100).healthVisible());
        assertEquals(initial, HudLayout.forWindow(960, 540));
        assertEquals(initial, HudLayout.forWindow(1920, 1080));
    }
}
