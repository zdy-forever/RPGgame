package au.edu.anu.comp2120.screen;

import au.edu.anu.comp2120.combat.AttackSpec;
import au.edu.anu.comp2120.enemy.EnemyConfig;
import au.edu.anu.comp2120.enemy.BossConfig;
import au.edu.anu.comp2120.enemy.BossChallenge;
import au.edu.anu.comp2120.npc.NPC;
import au.edu.anu.comp2120.player.AttackShape;
import au.edu.anu.comp2120.world.CollisionBox;
import java.util.List;
import java.util.Map;
import au.edu.anu.comp2120.player.AbilityLoadout;
import au.edu.anu.comp2120.player.PlayerClasses;

/**
 * Shared player, camera, combat tuning and scene-layout configuration.
 * World units are independent of window pixels; x points right and y points up.
 *
 * @author Daoyan Zhu (u7782042)
 * @author Jiaqi Chen (u7851266)
 */
public final class GameConfig {
    /** Window width at which persistent HUD elements reach their maximum size. */
    public static final int HUD_REFERENCE_WIDTH = 960;
    /** Window height at which persistent HUD elements reach their maximum size. */
    public static final int HUD_REFERENCE_HEIGHT = 540;
    /** Existing HP/coordinates/map/weapon drawing scale at the reference window size. */
    public static final float HUD_MAX_SCALE = 1.4f;
    /** Below this width, coordinates, minimap and weapon HUD are hidden. */
    public static final int HUD_DETAILS_MIN_WIDTH = 640;
    /** Below this height, coordinates, minimap and weapon HUD are hidden. */
    public static final int HUD_DETAILS_MIN_HEIGHT = 360;
    /** Below this width, even the HP bar is hidden. */
    public static final int HUD_HEALTH_MIN_WIDTH = 320;
    /** Below this height, even the HP bar is hidden. */
    public static final int HUD_HEALTH_MIN_HEIGHT = 180;
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
    /** Collision size shared by the Boss and stationary training target, independent of artwork. */
    public static final double ENCOUNTER_BODY_SIZE = 64;
    /** Safe fallback offset when the exact map centre is blocked or occupied by the player. */
    private static final double ENCOUNTER_SPAWN_OFFSET = 160;

    /**
     * Places the target's body centre at the map centre, with nearby safety fallbacks for custom maps/resets.
     * The coordinator checks each candidate against collision and the current player before spawning.
     * @param map current map definition
     * @return centre-first immutable candidates, never coordinates derived from a background image
     * @throws NullPointerException if map is null
     */
    public static List<CollisionBox> encounterPlacements(au.edu.anu.comp2120.world.GameMap map) {
        CollisionBox bounds = map.getBounds();
        double x = bounds.x() + (bounds.width() - ENCOUNTER_BODY_SIZE) / 2;
        double y = bounds.y() + (bounds.height() - ENCOUNTER_BODY_SIZE) / 2;
        return List.of(new CollisionBox(x, y, ENCOUNTER_BODY_SIZE, ENCOUNTER_BODY_SIZE),
                new CollisionBox(x + ENCOUNTER_SPAWN_OFFSET, y, ENCOUNTER_BODY_SIZE, ENCOUNTER_BODY_SIZE),
                new CollisionBox(x - ENCOUNTER_SPAWN_OFFSET, y, ENCOUNTER_BODY_SIZE, ENCOUNTER_BODY_SIZE),
                new CollisionBox(x, y + ENCOUNTER_SPAWN_OFFSET, ENCOUNTER_BODY_SIZE, ENCOUNTER_BODY_SIZE),
                new CollisionBox(x, y - ENCOUNTER_SPAWN_OFFSET, ENCOUNTER_BODY_SIZE, ENCOUNTER_BODY_SIZE));
    }
    /** Provisional two-phase sentinel: shared pursuit, alternating sweep/slam and one material reward. */
    public static final BossConfig SENTINEL = new BossConfig(400,
            new EnemyConfig(70, 500, 650, 96, new AttackSpec("boss-navigation-unused", 0, 0.2, 1)),
            0.5, 0.75,
            List.of(new BossConfig.Pattern(new AttackSpec("boss-sweep", 15, 0.2, 1.2), 96, AttackShape.FORWARD),
                    new BossConfig.Pattern(new AttackSpec("boss-slam", 20, 0.3, 1.6), 128, AttackShape.AREA)),
            List.of(new BossConfig.Pattern(new AttackSpec("boss-heavy-sweep", 20, 0.2, 0.8), 120, AttackShape.FORWARD),
                    new BossConfig.Pattern(new AttackSpec("boss-heavy-slam", 30, 0.3, 1.1), 160, AttackShape.AREA)),
            "sentinel-core", 1);
    /** Hub merchant granting one weapon and repeatable shared bag/chest potion top-ups. */
    public static final NPC WEAPON_MERCHANT = new NPC(
            "weapon_merchant", "Weapon Merchant",
            650.0, 250.0, 24.0, 24.0,
            List.of("I have a Lightning Staff for your first visit.",
                    "Choose weapon collection or potion supplies from my service menu."),
            "weapon_lightning_staff"
    );
    /** Decorative forge retains its original (+40, 0) offset from the merchant. */
    public static final CollisionBox FORGE_BOUNDS = new CollisionBox(
            WEAPON_MERCHANT.getX() + 40, WEAPON_MERCHANT.getY(), 40, 40);
    /** Visual-only magic circle on the right side of the one-screen Memorial Hall. */
    public static final CollisionBox MEMORIAL_CIRCLE = new CollisionBox(326, 24, 264, 264);
    /** Five socket-aligned stone pictures, keyed by guardian identity rather than texture colour. */
    public static final Map<BossChallenge, CollisionBox> MEMORIAL_STONES = Map.of(
            BossChallenge.FIRE, new CollisionBox(432, 214, 52, 52),
            BossChallenge.WATER, new CollisionBox(510, 155, 52, 52),
            BossChallenge.LIGHTNING, new CollisionBox(481, 67, 52, 52),
            BossChallenge.ICE, new CollisionBox(382, 67, 52, 52),
            BossChallenge.WIND, new CollisionBox(350, 155, 52, 52));
    /** Memorial Hall Sage on the left, hosting the final conversation without an inventory reward. */
    public static final NPC GUIDE = new NPC(
            "guide", "Old Sage",
            100.0, 170.0, 24.0, 24.0,
            List.of(
                    "Welcome to Memorial Hall. Each fallen guardian awakens its elemental stone.",
                    "Defeat all five guardians, then speak to me to complete your journey."),
            null
    );
    /** Hub-local Sage offers repeatable gameplay and save/load guidance, with no reward or ending trigger. */
    public static final NPC HUB_GUIDE = new NPC(
            "hub_guide", "Old Sage",
            184.0, 221.0, 24.0, 24.0,
            List.of(
                    "Welcome, adventurer! Five elemental cores lie corrupted. Your path: choose a class with the Class Master, take a trial from the Quest Keeper, then pass through the Portal Guide's gate. Press E to read the next tip.",
                    "Use WASD to move. J is your light attack, K your class skill, L your dash, and O your ultimate.",
                    "Before you leave: close this dialogue, then press Esc to open the pause menu. Choose Save manually to save, or Manage saves to load an automatic or manual save.",
                    "F5 requests a manual save; F9 opens Manage saves. Close any dialogue, bag or other panel first. Use W/S or Up/Down to select and E/Enter to confirm.",
                    "Save and load only while alive in Hub or Memorial Hall, after active skills finish. Battle and Training do not allow manual save/load.",
                    "Boss victory triggers an automatic save after settlement. Defeat does not autosave. Automatic and manual saves are separate; check the save-complete message.",
                    "Loading replaces unsaved progress, including Boss wins and items, with the selected save. Replacing a manual save or loading requires confirmation; Esc cancels.",
                    "Visit the Class Master to choose Archer, Mage or Katana and an O mode. Class and O mode selection are Hub-only.",
                    "Want to practise? Ask the Portal Guide for Training, then enter the portal. The target stays still and fires upward; the scoreboard shows damage and movement speed. Step aside for five seconds to recover.",
                    "For a real battle, accept a Boss task from the Quest Keeper FIRST. Then ask the Portal Guide to select Battle and open the portal. Accepting a task alone does not open it.",
                    "Each Battle Map has one Boss. Watch for the flashing warning and red melee area, dodge attacks, and use your skills between openings.",
                    "Victory or defeat returns you here with full HP. Failure lets you retry the same task. After victory, accept any Boss again: each guardian gains health and attacks slightly faster as your wins against it grow, up to a limit.",
                    "Each defeated guardian awakens one magic stone in Memorial Hall. Ask the Portal Guide to take you there. Once all five stones are lit, speak to me IN THE HALL to finish your journey."),
            null
    );
    /** Repeatable controls, save/load and NPC travel instructions, excluding removed debug travel shortcuts. */
    public static final NPC NOTICE_BOARD = new NPC(
            "notice_board", "Notice Board",
            250.0, 220.0, 24.0, 32.0,
            List.of(
                    "=== HUB NOTICE BOARD ===",
                    "",
                    "-- CONTROLS --",
                    "WASD move | E interact | B bag | M map",
                    "Menus: W/S or Up/Down select | E/Enter confirm | Esc back",
                    "Storage: A/D or Left/Right column | Q/R or PgUp/PgDn page",
                    "Items/NPC options: left-click select, right-click confirm",
                    "J light attack | K class skill | L dash | O ultimate",
                    "P use health potion",
                    "Travel: speak to the Portal Guide, then enter the portal.",
                    "Battle: accept a task from the Quest Keeper first.",
                    "",
                    "-- SAVE / LOAD --",
                    "Close other panels, then Esc opens the pause menu.",
                    "Save manually: save the current game. F5 is the shortcut.",
                    "Manage saves: Load automatic or Load manual. F9 opens this menu.",
                    "Save/load requires a living player in Hub or Memorial Hall with no active skills.",
                    "No manual save/load in Battle or Training.",
                    "Boss victory triggers an automatic save after settlement; defeat does not autosave.",
                    "Automatic and manual saves are separate. Check the save-complete message.",
                    "Loading replaces unsaved progress, including Boss wins and items.",
                    "Overwrite/load confirmation: E/Enter confirms; Esc cancels.",
                    "",
                    "-- CLASS ABILITIES --",
                    "ARCHER: J arrow | K spread shot | L dash",
                    "Archer O: Arrow Rain / Untargetable (not invincible)",
                    "MAGE: J elemental orb | K switch element | L dash",
                    "Mage O: fixed Magic Circle / Rapid Element Barrage",
                    "KATANA: J slash | K parry into counter dash | L dash",
                    "Katana O: Watermoon Stance / Rising Slash",
                    "Choose your class and O mode at the Class Master in Hub only.",
                    "",
                    "-- IN THE HUB --",
                    "Old Sage - start here | Quest Keeper - boss tasks",
                    "Portal Guide - Training, accepted Battle or Memorial Hall",
                    "Weapon Merchant - equipment upgrades and potion supplies",
                    "",
                    "P uses one carried potion: up to 30 HP. Full HP consumes none.",
                    "The quickbar counts carried potions only, not Storage Chest stock.",
                    "Open B to manage items. Menus pause battle; close them to resume.",
                    "Victory or defeat returns you to Hub with full HP.",
                    "",
                    "Defeat all five guardians to awaken the Memorial Hall stones.",
                    "Then speak to Old Sage in Memorial Hall to complete your journey."),
            null
    );
    /** Storage chest that accepts and returns whatever the player carries. */
    public static final NPC STORAGE_CHEST = new NPC(
            "storage_chest", "Storage Chest",
            590.0, 120.0, 30.0, 24.0,
            List.of("A sturdy chest. It holds whatever you leave here."),
            null
    );
    /** Destination selector beside, but not overlapping, the single Hub portal. */
    public static final NPC PORTAL_GUIDE = new NPC(
            "portal_guide", "Portal Guide",
            765.0, 245.0, 24.0, 24.0,
            List.of(
                    "I open the gates. Training is always open; Battle needs a quest from the Quest Keeper.",
                    "Tell me your destination and step through - the guardian awaits."),
            null
    );
    /** Class and ultimate trainer, clear of the portal and other interaction points. */
    public static final NPC CLASS_MASTER = new NPC(
            "class_master", "Class Master", 320.0, 130.0, 24.0, 24.0,
            List.of(
                    "Choose your class and its O ultimate mode. J/K/L remain class-defined.",
                    "A worthy start. Now seek the Quest Keeper - they will set your first trial against a guardian."),
            null);
    /** Practice-only exit replacing the removed debug fast-travel keys. */
    public static final NPC TRAINING_EXIT = new NPC(
            "training_exit", "Return to Hub", 128.0, 144.0, 24.0, 24.0,
            "Return to Hub? Your practice score and active effects will be cleared.", null);
    /** Hub quest giver offering the five distinct Boss challenges. */
    public static final NPC QUEST_KEEPER = new NPC(
            "quest_keeper", "Quest Keeper", 347.0, 240.0, 24.0, 24.0,
            List.of(
                    "Accept a Boss challenge from me - each guardian guards a corrupted elemental core.",
                    "Once you have taken a trial, bring it to the Portal Guide. They alone open the way."),
            null);
    /** Every NPC present on the Hub map. */
    public static final List<NPC> HUB_NPCS =
            List.of(HUB_GUIDE, WEAPON_MERCHANT, NOTICE_BOARD, STORAGE_CHEST, PORTAL_GUIDE, CLASS_MASTER, QUEST_KEEPER);
    /** Prevents construction of this configuration holder. */
    private GameConfig() {
    }
}
