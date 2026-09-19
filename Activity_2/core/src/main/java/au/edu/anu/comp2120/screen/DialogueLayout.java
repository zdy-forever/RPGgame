package au.edu.anu.comp2120.screen;

import java.util.Objects;

/**
 * Content-sized dialogue geometry with separate title, body and control-hint bands.
 * Actual wrapped font measurements are supplied by the view, keeping resize math GPU-independent.
 * @param x panel left edge
 * @param y panel bottom edge
 * @param width panel width
 * @param height panel height
 * @param scale uniform font scale, capped at the reference-window size
 * @param padding outer text inset
 * @param gap separation between text bands
 * @param titleHeight measured wrapped title height
 * @param bodyHeight measured wrapped body height
 * @param hintHeight measured wrapped controls height
 * @author Daoyan Zhu (u7782042)
 */
public record DialogueLayout(float x, float y, float width, float height, float scale,
        float padding, float gap, float titleHeight, float bodyHeight, float hintHeight) {
    /**
     * Measures text after scaling and wrapping without imposing a rendering dependency.
     * @author Daoyan Zhu (u7782042)
     */
    @FunctionalInterface
    public interface TextHeight {
        /**
         * Gets the height occupied by wrapped text at the requested font scale.
         * @param text text including explicit newlines
         * @param scale uniform font scale
         * @param width available positive wrap width
         * @return finite non-negative measured height
         */
        float measure(String text, float scale, float width);
    }

    /**
     * Fits all three bands within the window, reducing font size only if content exceeds its height.
     * Width and initial scale stop growing above 960 x 540; small windows shrink using both axes.
     * @param windowWidth positive logical window width
     * @param windowHeight positive logical window height
     * @param wide whether this is a wider quest/class menu
     * @param title speaker name
     * @param body current dialogue page or choice list
     * @param hint current control instructions
     * @param measure wrapped text measurement supplied by the font-owning view
     * @return fully contained panel with non-overlapping text bands
     * @throws IllegalArgumentException if dimensions or measured heights are invalid, or text cannot fit
     * @throws NullPointerException if text or measurement is null
     */
    public static DialogueLayout fit(int windowWidth, int windowHeight, boolean wide,
            String title, String body, String hint, TextHeight measure) {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(hint, "hint");
        Objects.requireNonNull(measure, "measure");
        if (windowWidth <= 0 || windowHeight <= 0) {
            throw new IllegalArgumentException("Dialogue requires a positive window size");
        }
        float scale = HudLayout.forWindow(windowWidth, windowHeight).scale();
        float margin = 18 * scale / GameConfig.HUD_MAX_SCALE;
        float width = Math.min(windowWidth, GameConfig.HUD_REFERENCE_WIDTH) * (wide ? 0.9f : 0.7f);
        float availableHeight = windowHeight - 2 * margin;
        for (int attempt = 0; attempt < 64; attempt++) {
            float padding = 14 * scale;
            float gap = 12 * scale;
            float textWidth = width - 2 * padding;
            float titleHeight = checkedHeight(measure.measure(title, scale, textWidth));
            float bodyHeight = checkedHeight(measure.measure(body, scale, textWidth));
            float hintHeight = checkedHeight(measure.measure(hint, scale, textWidth));
            float height = 2 * padding + 2 * gap + titleHeight + bodyHeight + hintHeight;
            if (height <= availableHeight) {
                return new DialogueLayout((windowWidth - width) / 2, margin, width, height, scale,
                        padding, gap, titleHeight, bodyHeight, hintHeight);
            }
            scale *= Math.min(0.9f, availableHeight / height * 0.95f);
        }
        throw new IllegalArgumentException("Text measurement cannot fit dialogue within window");
    }

    /**
     * Rejects invalid font measurements before they can corrupt panel geometry.
     * @param height measured height
     * @return validated height
     * @throws IllegalArgumentException if height is negative or non-finite
     */
    private static float checkedHeight(float height) {
        if (!Float.isFinite(height) || height < 0) {
            throw new IllegalArgumentException("Invalid dialogue text height");
        }
        return height;
    }

    /**
     * Gets the wrap width inside the panel padding.
     * @return positive text width shared by measuring and drawing
     */
    public float textWidth() { return width - 2 * padding; }
    /**
     * Positions the speaker name below the panel's top inset.
     * @return top of the speaker-name band
     */
    public float titleTop() { return y + height - padding; }
    /**
     * Positions the page below the wrapped speaker name.
     * @return top of the body band, below the title and its gap
     */
    public float bodyTop() { return titleTop() - titleHeight - gap; }
    /**
     * Anchors controls above the panel's bottom inset.
     * @return top of the controls band, below the body and its gap
     */
    public float hintTop() { return y + padding + hintHeight; }
}
