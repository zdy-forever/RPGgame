package au.edu.anu.comp2120.screen;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import java.io.InputStream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Measures the actual bundled HUD font without uploading textures or claiming native visual QA.
 * Glyph metrics and kerning use LibGDX; only texture-coordinate setup is omitted.
 * @author Daoyan Zhu (u7782042)
 */
final class PotionFontLayoutTest {
    /** Default font used by BitmapFont() in the project's LibGDX version. */
    private static final String FONT = "com/badlogic/gdx/utils/lsans-15.fnt";

    /** Creates independent font-layout checks. */
    PotionFontLayoutTest() { }

    /**
     * Builds the default bitmap font's real metrics with no OpenGL context.
     * @return measurement-only font, with no owned texture
     */
    private BitmapFont font() {
        FileHandle resource = new FileHandle(FONT) {
            /**
             * Reads the same bundled font definition as the runtime default font.
             * @return required classpath stream
             */
            @Override public InputStream read() {
                InputStream stream = BitmapFont.class.getResourceAsStream("/" + FONT);
                assertNotNull(stream, "Bundled HUD font must exist");
                return stream;
            }
        };
        return new BitmapFont(new BitmapFont.BitmapFontData(resource, false), new TextureRegion(), false) {
            /**
             * Skips texture coordinates, which do not participate in glyph measurements.
             * @param data already parsed default font metrics
             */
            @Override protected void load(BitmapFontData data) { }
        };
    }

    /**
     * Exact and compact counts fit inside the bottle's reserved text band at visible sizes.
     * @param width logical window width
     * @param height logical window height
     */
    @ParameterizedTest
    @CsvSource({"640,360", "960,540", "1920,1080", "640,1080", "1920,360"})
    void defaultFontFitsCountWithoutQuantityDependentShrinking(int width, int height) {
        HudLayout hud = HudLayout.forWindow(width, height);
        BitmapFont font = font();
        try {
            font.getData().setScale(0.6f * hud.scale());
            for (int count : new int[] {0, 1, 9, 10, 99, 100, 999, 1000, Integer.MAX_VALUE}) {
                GlyphLayout text = new GlyphLayout(font, HudLayout.potionCountLabel(count));
                assertTrue(text.width <= hud.skillIconSize() - 4 * hud.scale(), "count=" + count);
                assertTrue(text.height <= hud.skillIconSize() * 0.32f - hud.scale(), "count=" + count);
                assertEquals(0.6f * hud.scale(), font.getData().scaleX);
            }
        } finally {
            font.dispose();
        }
    }
}
