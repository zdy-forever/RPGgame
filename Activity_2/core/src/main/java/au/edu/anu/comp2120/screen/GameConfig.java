package au.edu.anu.comp2120.screen;

/**
 * Shared player, camera, and timing configuration for the asset-free map demonstration.
 * World units are independent of window pixels; x points right and y points up.
 *
 * @author Daoyan Zhu (u7782042)
 */
public final class GameConfig {
    /** Fixed camera view width in world units, independent of map and window sizes. */
    public static final float CAMERA_VIEW_WIDTH = 640;
    /** Fixed camera view height in world units; window resizing preserves this aspect ratio. */
    public static final float CAMERA_VIEW_HEIGHT = 360;
    /** Player movement speed in world units per second. */
    public static final double PLAYER_SPEED = 180;
    /** Player collision width in world units. */
    public static final double PLAYER_WIDTH = 28;
    /** Player collision height in world units. */
    public static final double PLAYER_HEIGHT = 28;
    /** Largest rendered-frame time simulated; excess stall/resume time is discarded. */
    public static final double MAX_FRAME_DELTA = 0.1;
    /** Prevents construction of this configuration holder. */
    private GameConfig() {
    }
}
