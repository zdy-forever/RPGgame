package au.edu.anu.comp2120.screen;

import au.edu.anu.comp2120.combat.AttackSpec;
import au.edu.anu.comp2120.enemy.EnemyConfig;

/**
 * Shared player, camera, combat-debug, and timing configuration for the asset-free demonstration.
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
    /** Raw damage of each temporary debug strike. */
    public static final int COMBAT_DAMAGE = 25;
    /** Active duration of the debug strike and its rectangle outline. */
    public static final double COMBAT_ACTIVE_SECONDS = 0.25;
    /** Per-attacker delay between debug strikes in simulation seconds. */
    public static final double COMBAT_COOLDOWN_SECONDS = 0.5;
    /** Debug strike extension on all sides of the source hurtbox; not a final skill range. */
    public static final double COMBAT_RANGE = 96;
    /** Inset separating the player's hurtbox from its movement collision/placeholder body. */
    public static final double COMBAT_HURTBOX_INSET = 4;
    /** Enemy collision and placeholder width/height in world units. */
    public static final double ENEMY_SIZE = 28;
    /** Distance from entry used for candidate enemy placements. */
    public static final double ENEMY_SPAWN_OFFSET = 96;
    /** Maximum number of temporary autonomous enemies created in each battle arena. */
    public static final int ENEMY_COUNT = 2;
    /** Basic AI tuning: speed, detection/loss/attack radii, and shared attack timing/damage. */
    public static final EnemyConfig BASIC_ENEMY = new EnemyConfig(80, 200, 260, 44,
            new AttackSpec("enemy-basic", 10, 0.2, 1));
    /** Prevents construction of this configuration holder. */
    private GameConfig() {
    }
}
