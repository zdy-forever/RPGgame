package au.edu.anu.comp2120.screen;

/**
 * Graphics-free resize policy shared by the persistent HUD and class/ability/potion quickbar.
 * Both window axes constrain scaling; growth beyond the reference size never enlarges the HUD.
 * Hidden expanded maps must also release their modal input lock in the screen.
 * @param scale uniform font/geometry scale; zero for non-positive window dimensions
 * @param healthVisible whether HP can be shown
 * @param detailsVisible whether coordinates, minimap, weapon and quickbar can be shown
 * @param skillScale dedicated scale for the class/ability/potion quickbar; grows on large windows
 * @author Daoyan Zhu (u7782042)
 * @author Jiaqi Chen (u7851266)
 */
public record HudLayout(float scale, boolean healthVisible, boolean detailsVisible, float skillScale) {
    /** Icon side length before the common capped HUD scale is applied. */
    private static final float SKILL_ICON_SIZE = 30;
    /** Extra scale added to the skill bar for each unit of window ratio above the reference size. */
    private static final float SKILL_EXTRA_SCALE_PER_RATIO = 0.2f;
    /** Hard ceiling for the skill bar scale so very large monitors do not inflate the quickbar excessively. */
    private static final float SKILL_MAX_SCALE = 1.8f;
    /** Panel/map scale multiplier relative to the skill scale, making the objective tracker and minimap smaller. */
    private static final float PANEL_SCALE_RATIO = 0.8f;
    /** Centre spacing in icon widths, preserving a small gap between neighbouring slots. */
    private static final float SKILL_SLOT_SPACING = 1.15f;
    /** Largest exact count shown on the quickbar; larger stacks remain exact in the bag. */
    private static final int QUICKBAR_COUNT_LIMIT = 999;

    /**
     * Keeps potion labels at most four characters so large stacks do not shrink into unreadable text.
     * @param quantity non-negative carried potion count
     * @return x0 through x999, or 999+ for larger stacks
     * @throws IllegalArgumentException if quantity is negative
     */
    public static String potionCountLabel(int quantity) {
        if (quantity < 0) { throw new IllegalArgumentException("Potion count cannot be negative"); }
        return quantity > QUICKBAR_COUNT_LIMIT ? "999+" : "x" + quantity;
    }

    /**
     * Sizes every quickbar icon, including the shared potion slot.
     * @return scaled icon side length
     */
    public float skillIconSize() { return SKILL_ICON_SIZE * skillScale; }

    /**
     * Scales persistent panels (objective tracker and minimap) slightly smaller than the quickbar.
     * @return reduced scale for non-skill HUD elements
     */
    public float panelScale() { return skillScale * PANEL_SCALE_RATIO; }

    /**
     * Centres the entire quickbar, including its potion slot and optional Mage element slot.
     * @param windowWidth logical screen width
     * @param index zero-based slot: class, J, K, L, O, optional element, then potion
     * @param elementVisible whether the Mage element slot is present
     * @return left edge of this slot in screen coordinates
     * @throws IllegalArgumentException if the slot index is outside this layout
     */
    public float skillSlotLeft(int windowWidth, int index, boolean elementVisible) {
        int count = elementVisible ? 7 : 6;
        if (index < 0 || index >= count) { throw new IllegalArgumentException("Invalid quickbar slot"); }
        float size = skillIconSize();
        float width = size * (1 + (count - 1) * SKILL_SLOT_SPACING);
        return (windowWidth - width) / 2 + index * size * SKILL_SLOT_SPACING;
    }

    /**
     * Computes visibility and size using logical window units, not framebuffer pixels.
     * Threshold equality keeps an element visible; crossing either axis below hides it.
     * Non-positive/minimized windows return a fully hidden, zero-scale layout.
     * @param width current logical window width
     * @param height current logical window height
     * @return deterministic layout independent of previous window sizes
     */
    public static HudLayout forWindow(int width, int height) {
        if (width <= 0 || height <= 0) {
            return new HudLayout(0, false, false, 0);
        }
        float uncappedRatio = Math.min(width / (float) GameConfig.HUD_REFERENCE_WIDTH,
                height / (float) GameConfig.HUD_REFERENCE_HEIGHT);
        float ratio = Math.min(1f, uncappedRatio);
        float scale = GameConfig.HUD_MAX_SCALE * ratio;
        float skillScale = Math.min(SKILL_MAX_SCALE,
                scale + Math.max(0f, GameConfig.HUD_MAX_SCALE * (uncappedRatio - 1f) * SKILL_EXTRA_SCALE_PER_RATIO));
        return new HudLayout(scale,
                width >= GameConfig.HUD_HEALTH_MIN_WIDTH && height >= GameConfig.HUD_HEALTH_MIN_HEIGHT,
                width >= GameConfig.HUD_DETAILS_MIN_WIDTH && height >= GameConfig.HUD_DETAILS_MIN_HEIGHT,
                skillScale);
    }
}
