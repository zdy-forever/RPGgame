package au.edu.anu.comp2120.screen;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Verifies capped HUD scaling and both visibility thresholds without a graphics context.
 * @author Daoyan Zhu (u7782042)
 * @author Jiaqi Chen (u7851266)
 */
final class HudLayoutTest {
    /** Allowed rounding error for float UI scales. */
    private static final float EPSILON = 0.00001f;

    /** Creates an independent HUD policy test fixture. */
    HudLayoutTest() { }

    /**
     * Quantity growth cannot create labels wider than four glyphs.
     * @param quantity bag quantity
     * @param expected compact HUD text
     */
    @ParameterizedTest
    @CsvSource({"0,x0", "1,x1", "9,x9", "10,x10", "99,x99", "100,x100", "999,x999",
            "1000,999+", "2147483647,999+"})
    void potionLabelsStayCompact(int quantity, String expected) {
        assertEquals(expected, HudLayout.potionCountLabel(quantity));
        assertTrue(HudLayout.potionCountLabel(quantity).length() <= 4);
    }

    /** Invalid model data is rejected instead of displayed as a plausible stock count. */
    @Test void negativePotionCountIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> HudLayout.potionCountLabel(-1));
    }

    /** Class/element-slot changes cannot drift the bar or alter the shared icon scale. */
    @Test void switchingClassLayoutsIsReversible() {
        HudLayout hud = HudLayout.forWindow(960, 540);
        float archerLeft = hud.skillSlotLeft(960, 0, false);
        float mageLeft = hud.skillSlotLeft(960, 0, true);
        assertTrue(mageLeft < archerLeft);
        assertEquals(archerLeft - mageLeft,
                hud.skillSlotLeft(960, 6, true) - hud.skillSlotLeft(960, 5, false), 0.001);
        assertEquals(archerLeft, hud.skillSlotLeft(960, 0, false));
    }

    /** Window resize/hide/restore uses a fresh policy with no remembered potion-slot offset. */
    @Test void quickbarResizeAndVisibilityRecoverWithoutDrift() {
        HudLayout initial = HudLayout.forWindow(960, 540);
        for (int[] window : new int[][] {{640, 360}, {639, 360}, {960, 359}, {0, 0}, {1920, 1080}}) {
            HudLayout changed = HudLayout.forWindow(window[0], window[1]);
            assertTrue(Float.isFinite(changed.skillIconSize()));
            assertTrue(changed.skillIconSize() >= 0);
            assertEquals(window[0] >= 640 && window[1] >= 360, changed.detailsVisible());
            for (boolean mage : new boolean[] {false, true}) {
                int potion = mage ? 6 : 5;
                assertTrue(Float.isFinite(changed.skillSlotLeft(window[0], potion, mage)));
                assertEquals(initial.skillSlotLeft(960, potion, mage),
                        HudLayout.forWindow(960, 540).skillSlotLeft(960, potion, mage));
            }
        }
    }

    /**
     * All class layouts keep the potion at the right end, centre the whole bar and avoid overlap.
     * @param width logical width
     * @param height logical height
     * @param elementVisible Mage's extra element slot
     */
    @ParameterizedTest
    @CsvSource({"640,360,false", "640,360,true", "960,540,false", "960,540,true",
            "1920,1080,false", "1920,1080,true"})
    void quickbarFitsPotionAndOptionalElement(int width, int height, boolean elementVisible) {
        HudLayout hud = HudLayout.forWindow(width, height);
        int potionIndex = elementVisible ? 6 : 5;
        float size = hud.skillIconSize();
        float first = hud.skillSlotLeft(width, 0, elementVisible);
        float right = hud.skillSlotLeft(width, potionIndex, elementVisible) + size;
        assertTrue(first >= 0);
        assertTrue(right <= width);
        assertEquals(width, first + right, 0.001);
        for (int index = 1; index <= potionIndex; index++) {
            assertTrue(hud.skillSlotLeft(width, index, elementVisible)
                    > hud.skillSlotLeft(width, index - 1, elementVisible) + size);
        }
        assertThrows(IllegalArgumentException.class, () -> hud.skillSlotLeft(width, -1, elementVisible));
        assertThrows(IllegalArgumentException.class, () -> hud.skillSlotLeft(width, potionIndex + 1, elementVisible));
    }

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
        assertTrue(layout.skillScale() >= layout.scale());
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
        assertEquals(new HudLayout(0, false, false, 0), HudLayout.forWindow(width, height));
    }

    /** Enlarging again restores the exact original policy without retaining hidden-state history. */
    @Test
    void restoresAfterShrinking() {
        HudLayout initial = HudLayout.forWindow(960, 540);
        assertFalse(HudLayout.forWindow(500, 300).detailsVisible());
        assertFalse(HudLayout.forWindow(200, 100).healthVisible());
        assertEquals(initial, HudLayout.forWindow(960, 540));
        HudLayout enlarged = HudLayout.forWindow(1920, 1080);
        assertEquals(initial.scale(), enlarged.scale(), EPSILON);
        assertTrue(enlarged.healthVisible());
        assertTrue(enlarged.detailsVisible());
        assertTrue(enlarged.skillScale() >= initial.skillScale());
    }
}
