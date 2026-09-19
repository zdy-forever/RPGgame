package au.edu.anu.comp2120.screen;

/**
 * Graphics-free resize policy shared by the four persistent HUD elements.
 * Both window axes constrain scaling; growth beyond the reference size never enlarges the HUD.
 * Hidden expanded maps must also release their modal input lock in the screen.
 * @param scale uniform font/geometry scale; zero for non-positive window dimensions
 * @param healthVisible whether HP can be shown
 * @param detailsVisible whether coordinates, minimap and weapon can be shown
 * @author Daoyan Zhu (u7782042)
 */
public record HudLayout(float scale, boolean healthVisible, boolean detailsVisible) {
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
            return new HudLayout(0, false, false);
        }
        float ratio = Math.min(1f, Math.min(width / (float) GameConfig.HUD_REFERENCE_WIDTH,
                height / (float) GameConfig.HUD_REFERENCE_HEIGHT));
        return new HudLayout(GameConfig.HUD_MAX_SCALE * ratio,
                width >= GameConfig.HUD_HEALTH_MIN_WIDTH && height >= GameConfig.HUD_HEALTH_MIN_HEIGHT,
                width >= GameConfig.HUD_DETAILS_MIN_WIDTH && height >= GameConfig.HUD_DETAILS_MIN_HEIGHT);
    }
}
