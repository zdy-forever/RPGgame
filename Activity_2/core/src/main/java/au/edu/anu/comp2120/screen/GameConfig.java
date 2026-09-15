package au.edu.anu.comp2120.screen;

import au.edu.anu.comp2120.combat.AttackSpec;
import au.edu.anu.comp2120.enemy.EnemyConfig;
import au.edu.anu.comp2120.enemy.BossConfig;
import au.edu.anu.comp2120.npc.NPC;
import au.edu.anu.comp2120.player.AttackShape;
import au.edu.anu.comp2120.world.CollisionBox;
import java.util.List;
import au.edu.anu.comp2120.player.AbilityLoadout;
import au.edu.anu.comp2120.player.PlayerClasses;

/**
 * Shared player, camera, combat-debug, and timing configuration for the asset-free demonstration.
 * World units are independent of window pixels; x points right and y points up.
 *
 * @author Daoyan Zhu (u7782042)
 * @author Jiaqi Chen (u7851266)
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
    /** Compatibility view of the default class; all class tuning is centralized in PlayerClasses. */
    public static final AbilityLoadout PLAYER_ABILITIES = PlayerClasses.get(PlayerClasses.DEFAULT).abilities();
    /** Inset separating the player's hurtbox from its movement collision/placeholder body. */
    public static final double COMBAT_HURTBOX_INSET = 4;
    /** Walkable candidates for each battle map's single Boss, checked against the player before placement. */
    public static final List<CollisionBox> BOSS_PLACEMENTS = List.of(
            new CollisionBox(360, 100, 64, 64), new CollisionBox(840, 400, 64, 64),
            new CollisionBox(840, 100, 64, 64), new CollisionBox(360, 400, 64, 64));
    /** Provisional two-phase sentinel: shared pursuit, alternating sweep/slam and one material reward. */
    public static final BossConfig SENTINEL = new BossConfig(400,
            new EnemyConfig(70, 500, 650, 96, new AttackSpec("boss-navigation-unused", 0, 0.2, 1)),
            0.5, 0.75,
            List.of(new BossConfig.Pattern(new AttackSpec("boss-sweep", 15, 0.2, 1.2), 96, AttackShape.FORWARD),
                    new BossConfig.Pattern(new AttackSpec("boss-slam", 20, 0.3, 1.6), 128, AttackShape.AREA)),
            List.of(new BossConfig.Pattern(new AttackSpec("boss-heavy-sweep", 20, 0.2, 0.8), 120, AttackShape.FORWARD),
                    new BossConfig.Pattern(new AttackSpec("boss-heavy-slam", 30, 0.3, 1.1), 160, AttackShape.AREA)),
            "sentinel-core", 1);
    public static final NPC WEAPON_MERCHANT = new NPC(
            "weapon_merchant", "Weapon Merchant",
            320.0, 250.0, 24.0, 24.0,
            "Take this Lightning Staff! It will serve you well.",
            "weapon_lightning_staff"
    );
    /** Prevents construction of this configuration holder. */
    private GameConfig() {
    }
}
