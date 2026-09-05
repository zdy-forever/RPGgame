package au.edu.anu.comp2120.screen;

import au.edu.anu.comp2120.world.CollisionBox;
import java.util.List;

/**
 * Central tuning and geometry for the temporary, asset-free movement demonstration.
 * World units are independent of window pixels; x points right and y points up.
 *
 * @author Daoyan Zhu (u7782042)
 */
public final class MovementDemoConfig {
    /** Fixed world width in world units. */
    public static final float WORLD_WIDTH = 960;
    /** Fixed world height in world units. */
    public static final float WORLD_HEIGHT = 540;
    /** Player movement speed in world units per second. */
    public static final double PLAYER_SPEED = 180;
    /** Initial player position and collision size. */
    public static final CollisionBox PLAYER_SPAWN = new CollisionBox(64, 64, 28, 28);
    /** Largest rendered-frame time simulated; excess stall/resume time is discarded. */
    public static final double MAX_FRAME_DELTA = 0.1;
    /** Temporary walls rendered directly from the same immutable collision data. */
    public static final List<CollisionBox> WALLS = List.of(
            new CollisionBox(320, 100, 32, 280),
            new CollisionBox(520, 320, 220, 24),
            new CollisionBox(600, 80, 100, 100));

    /** Prevents construction of this configuration holder. */
    private MovementDemoConfig() {
    }
}
