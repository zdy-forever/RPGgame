package au.edu.anu.comp2120.screen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies content-sized, capped dialogue geometry without a graphics context.
 * A deterministic wrapped-font metric isolates sizing from GPU-dependent bitmap rendering.
 * @author Daoyan Zhu (u7782042)
 */
final class DialogueLayoutTest {
    /** The three-line Sage page that previously overlapped the E-close hint. */
    private static final String SAGE = """
            Awakened stones: 0 / 5.
            Defeat the remaining guardians, then speak to me again.
            The portal at the lower left returns to Hub.""";
    /** Float rounding tolerance in logical UI units. */
    private static final float EPSILON = 0.001f;

    /** Creates a GPU-independent geometry fixture. */
    DialogueLayoutTest() { }

    /**
     * Narrow, short, tiny and enlarged windows keep every band inside the panel without overlap.
     * @param width window width
     * @param height window height
     */
    @ParameterizedTest
    @CsvSource({"960,540", "640,360", "480,270", "320,180", "240,135", "320,720",
            "1280,200", "1920,1080", "1,1"})
    void resizeKeepsTitleBodyAndHintSeparate(int width, int height) {
        for (boolean wide : new boolean[] {false, true}) {
            var layout = DialogueLayout.fit(width, height, wide, "Old Sage", SAGE,
                    "[Up/Down] Choose   [E] Confirm   [Esc] Cancel", DialogueLayoutTest::measure);
            assertContained(layout, width, height);
            assertTrue(layout.scale() <= GameConfig.HUD_MAX_SCALE);
            assertEquals(measure(SAGE, layout.scale(), layout.textWidth()), layout.bodyHeight(), EPSILON);
        }
    }

    /** The regression page expands its panel rather than running into the fixed footer. */
    @Test void sagePageGrowsBeyondOldFixedHeight() {
        var layout = DialogueLayout.fit(960, 540, false, "Old Sage", SAGE, "[E] Close", DialogueLayoutTest::measure);
        assertTrue(layout.height() > 540 * 0.22f);
        assertEquals(GameConfig.HUD_MAX_SCALE, layout.scale());
        assertContained(layout, 960, 540);
    }

    /** Long pages reduce font size to fit vertically; controls/title are measured and wrapped too. */
    @Test void longTextShrinksWithoutDroppingAnyBand() {
        String text = SAGE.repeat(30);
        var layout = DialogueLayout.fit(480, 270, false, "A very long speaker name ".repeat(8), text,
                "[E] Continue to the next dialogue page ".repeat(10), DialogueLayoutTest::measure);
        assertTrue(layout.scale() < HudLayout.forWindow(480, 270).scale());
        assertTrue(layout.titleHeight() > 0);
        assertTrue(layout.hintHeight() > 0);
        assertEquals(measure(text, layout.scale(), layout.textWidth()), layout.bodyHeight(), EPSILON);
        assertContained(layout, 480, 270);
    }

    /** Reference-sized text/panel widths stay capped even when the window doubles. */
    @Test void largeWindowsDoNotEnlargeTextOrPanel() {
        var reference = DialogueLayout.fit(960, 540, false, "Old Sage", SAGE, "[E] Close", DialogueLayoutTest::measure);
        var enlarged = DialogueLayout.fit(1920, 1080, false, "Old Sage", SAGE, "[E] Close", DialogueLayoutTest::measure);
        assertEquals(reference.scale(), enlarged.scale());
        assertEquals(reference.width(), enlarged.width());
        assertEquals(reference.height(), enlarged.height());
        assertEquals((1920 - enlarged.width()) / 2, enlarged.x());
    }

    /** Invalid window/font data is rejected instead of generating NaN/off-screen geometry. */
    @Test void rejectsInvalidMeasurementsAndDimensions() {
        assertThrows(IllegalArgumentException.class, () -> DialogueLayout.fit(0, 540, false,
                "Sage", SAGE, "[E]", DialogueLayoutTest::measure));
        assertThrows(IllegalArgumentException.class, () -> DialogueLayout.fit(960, -1, false,
                "Sage", SAGE, "[E]", DialogueLayoutTest::measure));
        for (float invalid : new float[] {-1, Float.NaN, Float.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> DialogueLayout.fit(960, 540, false,
                    "Sage", SAGE, "[E]", (text, scale, width) -> invalid));
        }
        assertThrows(NullPointerException.class, () -> DialogueLayout.fit(960, 540, false,
                "Sage", SAGE, "[E]", null));
    }

    /**
     * Checks window containment and the gap reserved between each successive text band.
     * @param layout computed dialogue layout
     * @param width current window width
     * @param height current window height
     */
    private static void assertContained(DialogueLayout layout, int width, int height) {
        assertTrue(layout.x() >= 0 && layout.y() >= 0);
        assertTrue(layout.x() + layout.width() <= width + EPSILON);
        assertTrue(layout.y() + layout.height() <= height + EPSILON);
        assertTrue(layout.textWidth() > 0 && layout.scale() > 0);
        assertEquals(layout.gap(), layout.titleTop() - layout.titleHeight() - layout.bodyTop(), EPSILON);
        assertEquals(layout.gap(), layout.bodyTop() - layout.bodyHeight() - layout.hintTop(), EPSILON);
        assertEquals(layout.y() + layout.padding(), layout.hintTop() - layout.hintHeight(), EPSILON);
    }

    /**
     * Models deterministic wrapped monospaced glyphs including explicit newlines and long words.
     * @param text measured content
     * @param scale font scale
     * @param width wrap width
     * @return cap height plus line advances, as in a bitmap glyph layout
     */
    private static float measure(String text, float scale, float width) {
        int columns = Math.max(1, (int) (width / (8 * scale)));
        int rows = 0;
        for (String line : text.split("\n", -1)) {
            rows += Math.max(1, (line.length() + columns - 1) / columns);
        }
        return (12 + 16 * (rows - 1)) * scale;
    }
}
