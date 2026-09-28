package au.edu.anu.comp2120.screen;

import au.edu.anu.comp2120.GameSession;
import au.edu.anu.comp2120.audio.BackgroundMusic;
import au.edu.anu.comp2120.EncounterFlow;
import au.edu.anu.comp2120.npc.NPC;
import au.edu.anu.comp2120.npc.PortalGuide;
import au.edu.anu.comp2120.npc.ClassTrainer;
import au.edu.anu.comp2120.enemy.Boss;
import au.edu.anu.comp2120.enemy.BossChallenge;
import au.edu.anu.comp2120.quest.BossQuests;
import au.edu.anu.comp2120.player.Player;
import au.edu.anu.comp2120.world.*;
import au.edu.anu.comp2120.TravelSystem;
import au.edu.anu.comp2120.player.PlayerInput;
import au.edu.anu.comp2120.player.PlayerCommand;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Align;
import au.edu.anu.comp2120.inventory.Inventory;
import au.edu.anu.comp2120.inventory.Equipment;
import au.edu.anu.comp2120.inventory.EquipmentSlot;
import au.edu.anu.comp2120.inventory.ItemCatalog;
import au.edu.anu.comp2120.inventory.InventoryRewards;
import au.edu.anu.comp2120.npc.MerchantServices;
import au.edu.anu.comp2120.inventory.GearUpgrades;

import java.util.ArrayList;
import java.util.List;

/**
 * Coordinates the map session, input, combat adapter and read-only art presentation.
 *
 * @author Daoyan Zhu (u7782042)
 * @author Jiaqi Chen (u7851266)
 * @author Hao Liu (u7998420)
 */
public final class GameScreen extends ScreenAdapter {

    /** Current-map and player coordination, independent of this screen's lifecycle. */
    private final GameSession session;
    /** Full gameplay persistence coordinator, confined to the render thread. */
    private final au.edu.anu.comp2120.save.GameSaves saves;
    /** Confirmation and two-slot selection overlay. */
    private final SaveMenu saveMenu;
    /** Portal trigger/unlock state; delegates every map transition to the existing session. */
    private final TravelSystem travel;
    /** Graphics-independent debug adapter; all damage goes through the shared combat pipeline. */
    private final CombatDemo combatDemo;
    /** Stable external acceptance/result/reward boundary; contains no Quest or Inventory logic. */
    private final EncounterFlow encounterFlow;
    /** NPC selection adapter; routes all acceptance through the existing encounter boundary. */
    private final PortalGuide portalGuide;
    /** Session-local task progress consuming the existing settled encounter results. */
    private final BossQuests quests;
    /** Screen-owned streams survive travel and encounter cleanup, preserving same-song continuity. */
    private final BackgroundMusic music;
    /** Highlighted guardian in the quest NPC menu. */
    private int questChoice;
    /** Quest acceptance/completion feedback shown in the same NPC menu. */
    private String questFeedback = "Choose one Boss. You can replace an unfinished quest in Hub.";
    /** Whether the one-time all-guardians ending currently owns dialogue input. */
    private boolean endingOpen;
    /** Optional credits after completed Hall dialogue; never available from the Hub tutorial. */
    private final CreditsDialogue credits = new CreditsDialogue();
    /** Highlighted destination while the portal NPC's choice dialogue is open. */
    private PortalGuide.Destination portalChoice = PortalGuide.Destination.TRAINING;
    /** Hub-only NPC adapter for existing class and O mode selection. */
    private final ClassTrainer classTrainer;
    /** Highlighted trainer choice; changing it alone never changes gameplay state. */
    private int trainerChoice;
    /** Feedback displayed within the trainer dialogue after confirmation. */
    private String trainerFeedback = "J/K/L follow your class; choose your O mode below.";
    /** Last HP/key feedback applied to the native title, avoiding redundant title updates. */
    private String combatStatus = "";
    /** Brief travel feedback shown in the native window title without a font asset. */
    private String travelStatus = "Talk to the Portal Guide [E]: Training, Battle or Memorial Hall";
    /** Adapter for the application's keyboard input. */
    private final PlayerInput input;
    /** Art-backed world view owned by this screen, independent of combat rules. */
    private final PlaceholderRenderer renderer;
    /** Whether lifecycle pause has suspended movement. */
    private boolean paused;
    /** Discards the first frame's time after startup or resume. */
    private boolean skipNextUpdate = true;
    /** Whether an in-screen dialogue is open; freezes movement while true. */
    private boolean dialogueOpen;
    /** Index of the current dialogue line. */
    private int dialogueLine;
    /** Selected merchant service, independent of the once-only weapon flag. */
    private int merchantChoice;
    /** Whether the merchant equipment submenu owns input. */
    private boolean upgradeMenu;
    /** Selected equipment position. */
    private int upgradeChoice;
    /** Carried item explicitly awaiting upgrade confirmation. */
    private String pendingUpgrade = "";
    /** Feedback beneath the merchant service list. */
    private String merchantFeedback = "Choose a service. Potions are topped up to 3 across bag and chest.";
    /** NPC currently talking. */
    private NPC activeNpc;
    /** Own screen-space batch used only for the dialogue box. */
    private final SpriteBatch dialogueBatch;
    /** Own built-in font for dialogue text; no art files required. */
    private final BitmapFont dialogueFont;
    /** Own shape renderer for the dialogue box background and border. */
    private final ShapeRenderer dialogueShapes;
    /** Player item storage owned by this screen; survives map changes. */
    private final Inventory inventory;
    /** Persistent player equipment shared with combat. */
    private final Equipment equipment;
    /** At-most-once adapter for settled Boss rewards. */
    private final InventoryRewards inventoryRewards;
    /** Zero-based bag page. */
    private int inventoryPage;
    /** Keyboard-selected visible bag row. */
    private int bagRow;
    /** Keyboard-selected visible storage row. */
    private int storageRow;
    /** Whether the chest column, rather than the carried column, owns selection. */
    private boolean storageRight;
    /** Pointer activation sampled once for the current modal frame. */
    private boolean pointerConfirm;
    /** Testable modal input and presentation state for the debug console. */
    private final DebugConsole debugConsole = new DebugConsole();
    /** Pointer back action sampled once for the current modal frame. */
    private boolean pointerBack;
    /** Choice hitboxes from the last rendered dialogue, in logical window coordinates. */
    private final List<DialogueHit> dialogueHits = new ArrayList<>();
    /** Exact rendered content used to reject obsolete hitboxes after a page change. */
    private String hitBody = "";
    /** NPC owning the last rendered click targets. */
    private String hitNpc = "";
    /** Last rendered dialogue width. */
    private int hitWidth;
    /** Last rendered dialogue height. */
    private int hitHeight;
    /** Cached rendered panel with wrapped text geometry. */
    private DialogueLayout hitLayout;
    /** One wrapped option's vertical interval.
     * @param top upper edge
     * @param bottom lower edge
     * @param choice option index
     * @author Hao Liu (u7998420)
     */
    private record DialogueHit(float top, float bottom, int choice) { }

    /** Zero-based shared page for the two chest columns. */
    private int chestPage;
    /** Last item operation shown inside the bag. */
    private String inventoryFeedback = "Right-click: equip, unequip or use";
    /** Whether the inventory panel is open; pauses the entire combat simulation while true. */
    private boolean inventoryOpen;
    /** Whether the enlarged map panel is open; freezes movement while true. */
    private boolean bigMap;
    /** Shared chest storage, separate from the player inventory. */
    private final Inventory chestStorage;
    /** Whether the chest transfer panel is open; freezes movement while true. */
    private boolean chestOpen;
    /** Material id of the currently equipped weapon, empty when none is equipped. */
    private String equippedWeapon = "";
    /** Prefix marking a material id as an equippable weapon. */
    private static final String WEAPON_PREFIX = "weapon_";

    /**
     * Screen-space geometry of the inventory panel.
     *
     * @param x      left edge in screen units
     * @param y      bottom edge in screen units
     * @param boxW   panel width
     * @param boxH   panel height
     * @param pad    inner padding
     * @param lineH  height of one item row
     * @param scale  font scale factor
     */
    private record PanelLayout(float x, float y, float boxW, float boxH,
                               float pad, float lineH, float scale) {
    }
    /**
     * Creates the demo models and drawing resources after graphics initialization.
     */
    public GameScreen() { this(BackgroundMusic.VOLUME, false); }

    /** Creates fresh gameplay while preserving application music preferences.
     * @param volume initial music gain between zero and one
     * @param restarted whether to display the confirmed restart notification
     */
    public GameScreen(float volume, boolean restarted) {
        session = new GameSession(MapCatalog.createMaps(), MapCatalog.HUB,
                GameConfig.PLAYER_WIDTH, GameConfig.PLAYER_HEIGHT, GameConfig.PLAYER_SPEED);
        var questPortals = TravelCatalog.createGameplayPortals();
        travel = new TravelSystem(session, questPortals, TravelCatalog.createDestinations());
        encounterFlow = new EncounterFlow(session, travel, List.of(TravelCatalog.QUEST_PORTAL_ID));
        quests = new BossQuests(session, encounterFlow);
        music = new BackgroundMusic();
        music.setVolume(volume);
        portalGuide = new PortalGuide(session, travel, encounterFlow);
        combatDemo = new CombatDemo(session, encounterFlow);
        classTrainer = new ClassTrainer(session, combatDemo.getAbilities());
        input = new PlayerInput(Gdx.input);
        renderer = new PlaceholderRenderer(GameConfig.CAMERA_VIEW_WIDTH, GameConfig.CAMERA_VIEW_HEIGHT);
        updateMapView();
        dialogueBatch = new SpriteBatch();
        dialogueFont = new BitmapFont();
        dialogueShapes = new ShapeRenderer();
        inventory = session.getPlayer().getInventory();
        equipment = session.getPlayer().getEquipment();
        inventoryRewards = new InventoryRewards(encounterFlow, inventory);
        chestStorage = new Inventory();
        for (var item : ItemCatalog.equipment()) {
            if (item.slot() != EquipmentSlot.WEAPON) { chestStorage.addItem(item.id()); }
        }
        chestStorage.addItem(ItemCatalog.POTION, 3);
        var savedNpcs = new java.util.ArrayList<>(GameConfig.HUB_NPCS);
        savedNpcs.add(GameConfig.GUIDE);
        savedNpcs.add(GameConfig.TRAINING_EXIT);
        savedNpcs.forEach(npc -> npc.restoreInteracted(false));
        saves = new au.edu.anu.comp2120.save.GameSaves(session, travel, encounterFlow, quests,
                combatDemo, chestStorage, savedNpcs,
                new au.edu.anu.comp2120.save.SaveStore(java.nio.file.Path.of(
                        System.getProperty("user.home"), ".comp2120-game", "saves")));
        saveMenu = new SaveMenu(saves, () -> Math.round(music.getVolume() * 100),
                percent -> music.setVolume(percent / 100f),
                () -> ((au.edu.anu.comp2120.MainGame) Gdx.app.getApplicationListener()).restart(music.getVolume()));
        if (restarted) { saveMenu.notify("New game started. Both save files were kept."); }
    }

    /**
     * Gives external Quest/Inventory adapters acceptance, result and one-time reward APIs.
     * All calls must run on the game-loop thread; the local Portal Guide uses this same boundary.
     * Quest progress and Inventory reward consumption remain external responsibilities.
     * Intentionally exposed for external adapter integration, even before a local caller exists.
     * @return this screen's persistent gameplay integration boundary
     */
    @SuppressWarnings("unused") // Public cross-module integration endpoint, not dead implementation code.
    public EncounterFlow getEncounterFlow() {
        return encounterFlow;
    }

    /**
     * Samples input, updates movement/abilities and combat, then checks arrival portals before drawing.
     * NPC returns consume commands; portal arrival immediately clears old-map attacks and dash.
     * Success/failure recovery consumes the rest of the frame before any Hub movement or portal check.
     * Frame stalls are capped and all simulation timers pause with this screen.
     * Screen-space UI restores the full framebuffer viewport after the letterboxed world view.
     *
     * @param delta seconds elapsed since the previous frame
     */
    @Override
    public void render(float delta) {
        HudLayout hud = HudLayout.forWindow(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        if (!hud.detailsVisible() || session.getCurrentMap().getType() == MapType.MEMORIAL) {
            bigMap = false;
        }
        if (!paused && !skipNextUpdate && Float.isFinite(delta) && delta >= 0) {
            updateGameplay(Math.min(delta, GameConfig.MAX_FRAME_DELTA));
        }
        skipNextUpdate = false;
        int rewards = inventoryRewards.collect();
        if (rewards > 0) { inventoryFeedback = "Boss rewards added to your bag"; }
        equipment.reconcile();
        equippedWeapon = equipment.itemAt(EquipmentSlot.WEAPON);
        quests.synchronize();
        saves.afterSettlement();
        music.update(session.getCurrentMap().getType(), quests.getCompleted().size() == BossChallenge.values().length);
        GameMap currentMap = session.getCurrentMap();
        renderer.render(currentMap, session.getPlayer(),
                currentNpcs(),
                travel.getPortals(currentMap.getId()),
                combatDemo, paused || debugConsole.isOpen() || saveMenu.isOpen() || dialogueOpen || inventoryOpen || chestOpen || bigMap
                        || !Float.isFinite(delta) ? 0 : Math.max(0, Math.min(delta, GameConfig.MAX_FRAME_DELTA)),
                combatDemo.getDrops(), quests.getCompleted());
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        // Legacy coordinate HUD disabled; retain the original renderer for reference.
        // drawCoordinates(hud);
        drawHealth(hud);
        drawTrainingScore();
        drawInventory();
        // Legacy weapon HUD disabled; retain its renderer for reference. Equipment is shown in the bag.
        // drawWeaponSlot(hud);
        drawInteractionPrompt();
        drawMinimap(hud);
        drawChest();
        drawSkillHud(hud);
        drawDialogue();
        drawOutcome(delta);
        saveMenu.draw(dialogueBatch, dialogueFont, dialogueShapes);
        drawDebugInput();
    }

    /**
     * Displays return feedback above all HUD layers without pausing input or gameplay.
     * Uses real UI time rather than the capped/paused world animation clock.
     * @param delta elapsed frame seconds; invalid values are treated as zero
     */
    private void drawOutcome(float delta) {
        int width = Gdx.graphics.getWidth(), height = Gdx.graphics.getHeight();
        if (width <= 0 || height <= 0) { return; }
        dialogueBatch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, width, height));
        renderer.drawOutcome(dialogueBatch, dialogueFont, dialogueShapes, width, height,
                Float.isFinite(delta) ? Math.max(0, delta) : 0);
    }

    /**
     * Adds art-backed skill icons under modal dialogue without enlarging the capped HUD.
     * @param hud current window visibility and scale
     */
    private void drawSkillHud(HudLayout hud) {
        if (!hud.detailsVisible() || inventoryOpen || chestOpen || bigMap) { return; }
        dialogueBatch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        dialogueBatch.begin();
        renderer.drawSkillHud(dialogueBatch, dialogueFont, session.getPlayer(), combatDemo, hud,
                Gdx.graphics.getWidth());
        dialogueBatch.end();
    }

    /** Opens the console and captures input without relying on native dialogs. */
    private void openDebugInput() { debugConsole.open(Gdx.input, session.getPlayer().getDebugOptions()); }

    /** Restores the input processor when the console closes or the screen is disposed. */
    private void closeDebugInput() { debugConsole.close(); }

    /** Draws the modal command entry, command reference and latest response. */
    private void drawDebugInput() {
        if (!debugConsole.isOpen()) { return; }
        int w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
        if (w <= 0 || h <= 0) { return; }
        float scale = Math.min(1.4f, Math.min(w / 720f, h / 440f));
        float width = w * 0.92f, height = h * 0.8f;
        float x = (w - width) / 2, y = (h - height) / 2, pad = 16 * scale;
        String text = debugConsole.text(session.getPlayer().getDebugOptions());
        boolean previousMarkup = dialogueFont.getData().markupEnabled;
        dialogueFont.getData().markupEnabled = true;
        dialogueFont.getData().setScale(scale);
        while (scale > 0.02f && new GlyphLayout(dialogueFont, text,
                com.badlogic.gdx.graphics.Color.WHITE, width - 2 * pad, Align.left, true).height > height - 2 * pad) {
            scale *= 0.9f; dialogueFont.getData().setScale(scale);
        }
        dialogueShapes.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, w, h));
        dialogueShapes.begin(ShapeRenderer.ShapeType.Filled);
        dialogueShapes.setColor(0.025f, 0.035f, 0.055f, 1);
        dialogueShapes.rect(x, y, width, height);
        dialogueShapes.end();
        dialogueBatch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, w, h));
        dialogueBatch.begin();
        dialogueFont.setColor(1f, 1f, 1f, 1);
        dialogueFont.draw(dialogueBatch, text, x + pad, y + height - pad, width - 2 * pad, Align.left, true);
        dialogueBatch.end();
        dialogueFont.getData().markupEnabled = previousMarkup;
    }

    /**
     * Arbitrates one frame, settling player death then Boss victory before travel and debug input.
     * A post-combat outcome return skips all remaining portal/input processing in the arrival frame.
     * TravelSystem observes the normal session stamp on its next check and arms arrival protection.
     * @param delta validated capped non-negative simulation seconds
     * @throws IllegalArgumentException if the configured failure Hub/spawn is invalid
     */
    private void updateGameplay(double delta) {
        boolean consoleKey = Gdx.input.isKeyJustPressed(Input.Keys.F10) || Gdx.input.isKeyJustPressed(Input.Keys.GRAVE);
        if (debugConsole.isOpen()) {
            if (consoleKey || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) { closeDebugInput(); }
            else if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                debugConsole.submit(session.getPlayer().getDebugOptions());
                combatDemo.getAbilities().applyDebugCooldowns();
                updateMapView();
            }
            return;
        }
        if (consoleKey) { openDebugInput(); return; }
        sampleDialoguePointer();
        if (isMerchantDialogueOpen() && menuBack()) {
            if (!pendingUpgrade.isEmpty()) { pendingUpgrade = ""; }
            else if (upgradeMenu) { upgradeMenu = false; }
            else { dialogueOpen = false; activeNpc = null; }
            updateMapView(); return;
        }
        // Escape closes an ordinary panel first; a second press opens the pause menu.
        if (!saveMenu.isOpen() && !endingOpen && !credits.isOpen()
                && (dialogueOpen || inventoryOpen || chestOpen || bigMap)
                && menuBack()) {
            dialogueOpen = false; inventoryOpen = false; chestOpen = false; bigMap = false;
            activeNpc = null;
            updateMapView();
            return;
        }
        if (saveMenu.update(dialogueOpen || inventoryOpen || chestOpen || bigMap || endingOpen || credits.isOpen())) {
            equippedWeapon = equipment.itemAt(EquipmentSlot.WEAPON);
            inventoryPage = 0; chestPage = 0;
            updateMapView();
            return;
        }
        if (combatDemo.returnToHubIfBattleEnded()) {
            showBattleReturn();
            return;
        }
        if (credits.isOpen()) {
            handleCreditsInput();
            updateMapView();
            return;
        }
        if (endingOpen) {
            if (menuBack()) { endingOpen = false; dialogueOpen = false; activeNpc = null; updateMapView(); return; }
            if (menuConfirm()) {
                dialogueLine++;
                if (dialogueLine >= BossQuests.ENDING_LINES.size()) {
                    endingOpen = false;
                    dialogueLine = 0;
                    credits.open();
                    saveMenu.notify("All five guardians defeated - journey complete");
                }
            }
            updateMapView();
            return;
        }
        if (isMerchantDialogueOpen()) {
            handleMerchantInput();
            updateMapView();
            return;
        }
        if (isQuestDialogueOpen()) {
            handleQuestDialogueInput();
            combatDemo.synchronizePausedMap();
            updateMapView();
            return;
        }
        if (isPortalDialogueOpen()) {
            handlePortalDialogueInput();
            combatDemo.synchronizePausedMap();
            updateMapView();
            return;
        }
        if (isTrainerDialogueOpen()) {
            handleTrainerDialogueInput();
            combatDemo.synchronizePausedMap();
            updateMapView();
            return;
        }
        if (isTrainingExitDialogueOpen()) {
            handleTrainingExitInput();
            combatDemo.synchronizePausedMap();
            updateMapView();
            return;
        }
        long beforeCombat = session.getTransitionVersion();
        if (Gdx.input.isKeyJustPressed(Input.Keys.B)) {
            inventoryOpen = !inventoryOpen;
            if (!inventoryOpen) { updateMapView(); return; }
            bagRow = 0;
            if (inventoryOpen) {
                bigMap = false;
                dialogueOpen = false;
                chestOpen = false;
                activeNpc = null;
            }
            updateMapView(); return;
        }
        if (!inventoryOpen && !chestOpen && session.getCurrentMap().getType() != MapType.MEMORIAL
                && HudLayout.forWindow(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()).detailsVisible()
                && Gdx.input.isKeyJustPressed(Input.Keys.M)) {
            bigMap = !bigMap;
            updateMapView(); return;
        }
        if (inventoryOpen || chestOpen) {
            handleItemPanelInput();
            updateMapView();
            return;
        }
        if (dialogueOpen) {
            if (menuConfirm()) { advanceDialogue(); }
            updateMapView(); return;
        }
        if (bigMap) { updateMapView(); return; }
        if ((dialogueOpen && menuConfirm()) || (!dialogueOpen && Gdx.input.isKeyJustPressed(Input.Keys.E))) {
            if (chestOpen) {
                chestOpen = false;
            } else if (dialogueOpen) {
                advanceDialogue();
            } else {
                NPC near = nearestNpc(session.getPlayer());
                if (near == null) {
                    travelStatus = "";
                } else if (GameConfig.STORAGE_CHEST.getId().equals(near.getId())) {
                    chestOpen = true; storageRow = 0; storageRight = false; chestPage = 0;
                    dialogueOpen = false;
                    inventoryOpen = false;
                    activeNpc = null;
                } else {
                    dialogueOpen = true;
                    dialogueLine = 0;
                    activeNpc = near;
                    if (isMerchantDialogueOpen()) {
                        merchantChoice = 0; upgradeMenu = false; upgradeChoice = 0; pendingUpgrade = "";
                        merchantFeedback = "Choose a service. Supplies count both bag and chest.";
                        inventoryOpen = false; bigMap = false;
                    }
                    if (near.getId().equals(GameConfig.GUIDE.getId())
                            && session.getCurrentMap().getType() == MapType.MEMORIAL) {
                        endingOpen = quests.consumeEnding();
                        inventoryOpen = false;
                        bigMap = false;
                    }
                    if (isPortalDialogueOpen()) {
                        inventoryOpen = false;
                        bigMap = false;
                        portalChoice = switch (travel.getPortal(TravelCatalog.QUEST_PORTAL_ID).destinationMapId()) {
                            case MapCatalog.TRAINING -> PortalGuide.Destination.TRAINING;
                            case MapCatalog.MEMORIAL_HALL -> PortalGuide.Destination.MEMORIAL;
                            default -> PortalGuide.Destination.BATTLE;
                        };
                    } else if (isTrainerDialogueOpen()) {
                        inventoryOpen = false;
                        bigMap = false;
                        trainerChoice = session.getPlayer().getPlayerClass().ordinal();
                        trainerFeedback = "J/K/L follow your class; choose your O mode below.";
                    } else if (isTrainingExitDialogueOpen()) {
                        inventoryOpen = false;
                        bigMap = false;
                    } else if (isQuestDialogueOpen()) {
                        inventoryOpen = false;
                        bigMap = false;
                        quests.synchronize();
                        questChoice = quests.getActive().map(Enum::ordinal).orElse(0);
                        questFeedback = "E accepts any Boss, including rematches; Esc closes.";
                    }
                }
            }
        }
        // Inventory input remains active, but enemies, projectiles, damage and cooldowns
        // must not advance while the player is managing equipment or items.
        if (inventoryOpen || chestOpen || dialogueOpen || bigMap) {
            updateMapView();
            return;
        }
        boolean frozen = dialogueOpen || bigMap || chestOpen;
        if (!frozen && input.samplePotionUse()) {
            useHealthPotion();
        }
        combatDemo.update(delta,
                frozen ? PlayerCommand.NONE : input.sample(),
                !frozen && Gdx.input.isKeyJustPressed(Input.Keys.R),
                frozen ? null : input.sampleClassSelection().orElse(null),
                !frozen && input.sampleElementCycle(),
                !frozen && input.sampleUltimateModeCycle());
        if (session.getTransitionVersion() != beforeCombat) {
            showBattleReturn();
            return;
        }
        if (frozen) {
            updateMapView();
        } else {
            try {
                if (travel.updatePortals()) {
                    combatDemo.update(0, PlayerCommand.NONE, false);
                    saveMenu.notify("Arrived by portal; leave the portal area before re-entry");
                    updateMapView();
                }
            } catch (IllegalArgumentException | IllegalStateException failure) {
                saveMenu.notify(failure.getMessage());
                updateMapView();
            }
        }
        if (!combatStatus.equals(combatDemo.getStatus())) {
            updateMapView();
        }
    }

    /**
     * Identifies the destination-choice dialogue without changing generic NPC scripts.
     * @return true only while the Portal Guide is the active speaker
     */
    private boolean isPortalDialogueOpen() {
        return dialogueOpen && activeNpc != null
                && activeNpc.getId().equals(GameConfig.PORTAL_GUIDE.getId());
    }

    /**
     * Distinguishes the quest menu from its one-time ending dialogue.
     * @return true while the quest giver's selection menu is open
     */
    private boolean isQuestDialogueOpen() {
        return !endingOpen && dialogueOpen && activeNpc != null
                && activeNpc.getId().equals(GameConfig.QUEST_KEEPER.getId());
    }

    /** Reads a dialogue activation without changing world interaction bindings.
     * @return whether the current UI selection should activate
     */
    private boolean menuConfirm() { return pointerConfirm || MenuKeys.confirm(); }

    /** Reads modal back without allowing the same event to reopen a different panel.
     * @return Escape or the rendered back button
     */
    private boolean menuBack() { return pointerBack || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE); }

    /** Matches clicks to the exact last rendered layout, including wrapped option heights. */
    private void sampleDialoguePointer() {
        pointerConfirm = false; pointerBack = false;
        if (!dialogueOpen || activeNpc == null || hitLayout == null || !activeNpc.getId().equals(hitNpc)) { return; }
        boolean left = Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);
        boolean right = Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT);
        if ((!left && !right) || hitWidth != Gdx.graphics.getWidth() || hitHeight != Gdx.graphics.getHeight()) { return; }
        var lines = dialogueLines();
        if (dialogueLine >= lines.size() || !hitBody.equals(lines.get(dialogueLine))) { return; }
        float x = Gdx.input.getX(), y = hitHeight - Gdx.input.getY();
        float start = hitLayout.x() + hitLayout.padding(), width = hitLayout.textWidth();
        if (x < start || x > start + width) { return; }
        if (left && y <= hitLayout.hintTop() && y >= hitLayout.hintTop() - hitLayout.hintHeight()) {
            pointerBack = x >= start + width / 2;
            pointerConfirm = !pointerBack;
            return;
        }
        for (var hit : dialogueHits) {
            if (y > hit.top() || y < hit.bottom()) { continue; }
            if (credits.isOpen()) { credits.selectIndex(hit.choice()); }
            else if (isMerchantDialogueOpen()) {
                if (upgradeMenu) { upgradeChoice = hit.choice(); } else { merchantChoice = hit.choice(); }
            } else if (isQuestDialogueOpen()) { questChoice = hit.choice(); }
            else if (isTrainerDialogueOpen()) { trainerChoice = hit.choice(); }
            else if (isPortalDialogueOpen()) { portalChoice = PortalGuide.Destination.values()[hit.choice()]; }
            pointerConfirm = right;
            return;
        }
    }

    /** Records option bounds using the same font scale and wrapping as dialogue rendering.
     * @param layout rendered geometry
     * @param body rendered text
     */
    private void cacheDialogueHits(DialogueLayout layout, String body) {
        hitLayout = layout; hitBody = body; hitNpc = activeNpc.getId();
        hitWidth = Gdx.graphics.getWidth(); hitHeight = Gdx.graphics.getHeight(); dialogueHits.clear();
        boolean menu = !endingOpen && (credits.isOpen() || isQuestDialogueOpen() || isTrainerDialogueOpen()
                || isPortalDialogueOpen() || (isMerchantDialogueOpen() && pendingUpgrade.isEmpty()));
        if (!menu) { return; }
        String[] rows = body.split("\n", -1);
        int option = 0;
        for (int i = 0; i < rows.length; i++) {
            if (!rows[i].startsWith("> ") && !rows[i].startsWith("  ")) { continue; }
            String before = String.join("\n", java.util.Arrays.copyOfRange(rows, 0, i));
            float preceding = i == 0 ? 0 : measureDialogueText(before, layout.scale(), layout.textWidth())
                    + dialogueFont.getLineHeight() - dialogueFont.getCapHeight();
            float rowHeight = measureDialogueText(rows[i], layout.scale(), layout.textWidth());
            float top = layout.bodyTop() - preceding;
            dialogueHits.add(new DialogueHit(top, top - rowHeight - (dialogueFont.getLineHeight() - dialogueFont.getCapHeight()) / 2, option++));
        }
    }

    /** Accepts the highlighted target through quest/encounter APIs without teleporting or healing. */
    private void handleQuestDialogueInput() {
        if (session.getCurrentMap().getType() != MapType.HUB || !session.getPlayer().isAlive()
                || menuBack()) {
            dialogueOpen = false;
            activeNpc = null;
            return;
        }
        var targets = BossChallenge.values();
        if (MenuKeys.up()) {
            questChoice = Math.floorMod(questChoice - 1, targets.length);
        } else if (MenuKeys.down()) {
            questChoice = (questChoice + 1) % targets.length;
        }
        if (menuConfirm()) {
            try {
                quests.accept(targets[questChoice]);
                saveMenu.notify("Accepted: " + targets[questChoice].displayName() + ". Talk to the Portal Guide.");
            } catch (IllegalArgumentException | IllegalStateException failure) {
                saveMenu.notify(failure.getMessage());
            }
        }
    }

    /**
     * Identifies the class-selection dialogue without changing ordinary NPC scripts.
     * @return true while the Class Master is the active speaker
     */
    private boolean isTrainerDialogueOpen() {
        return dialogueOpen && activeNpc != null
                && activeNpc.getId().equals(GameConfig.CLASS_MASTER.getId());
    }

    /**
     * Identifies the explicit practice-return confirmation.
     * @return true while the Training exit NPC is the active speaker
     */
    private boolean isTrainingExitDialogueOpen() {
        return dialogueOpen && activeNpc != null
                && activeNpc.getId().equals(GameConfig.TRAINING_EXIT.getId());
    }

    /** Confirms E or cancels Escape without adding any global travel shortcut. */
    private void handleTrainingExitInput() {
        if (session.getCurrentMap().getType() != MapType.TRAINING || !session.getPlayer().isAlive()
                || menuBack()) {
            dialogueOpen = false;
            activeNpc = null;
        } else if (menuConfirm()) {
            try {
                portalGuide.returnFromTraining();
                dialogueOpen = false;
                activeNpc = null;
                saveMenu.notify("Practice finished; returned to Hub");
            } catch (IllegalArgumentException | IllegalStateException failure) {
                saveMenu.notify(failure.getMessage());
            }
        }
    }

    /** Identifies the merchant service menu.
     * @return whether the Hub merchant owns dialogue input
     */
    private boolean isMerchantDialogueOpen() {
        return dialogueOpen && activeNpc == GameConfig.WEAPON_MERCHANT;
    }

    /** Processes one service without closing the menu or triggering the other reward. */
    private void handleMerchantInput() {
        if (session.getCurrentMap().getType() != MapType.HUB || !session.getPlayer().isAlive()) {
            dialogueOpen = false; activeNpc = null; return;
        }
        if (upgradeMenu) {
            if (!pendingUpgrade.isEmpty()) {
                if (menuConfirm()) {
                    saveMenu.notify(GearUpgrades.upgrade(pendingUpgrade, inventory, chestStorage, equipment, session.getPlayer().getDebugOptions().freeUpgrade()));
                    pendingUpgrade = "";
                }
                return;
            }
            if (MenuKeys.up()) { upgradeChoice = Math.floorMod(upgradeChoice - 1, 5); }
            if (MenuKeys.down()) { upgradeChoice = (upgradeChoice + 1) % 5; }
            if (menuConfirm()) {
                String id = GearUpgrades.candidate(EquipmentSlot.values()[upgradeChoice], inventory, equipment);
                if (id.isEmpty()) { saveMenu.notify("Move the equipment into your bag first."); }
                else if (GearUpgrades.level(id) == GearUpgrades.MAX_LEVEL) { saveMenu.notify("Already at maximum level +2."); }
                else { pendingUpgrade = id; }
            }
            return;
        }
        var choices = MerchantServices.Choice.values();
        if (MenuKeys.up()) {
            merchantChoice = Math.floorMod(merchantChoice - 1, choices.length);
        } else if (MenuKeys.down()) {
            merchantChoice = (merchantChoice + 1) % choices.length;
        }
        if (menuConfirm()) {
            if (choices[merchantChoice] == MerchantServices.Choice.UPGRADES) {
                upgradeMenu = true; merchantFeedback = "Worn item first; otherwise highest-level carried item. Cost: 1 core.";
            } else { saveMenu.notify(MerchantServices.select(choices[merchantChoice], activeNpc, inventory, chestStorage)); }
        }
    }

    /** Handles the modal trainer menu before travel, debug keys or combat input. */
    private void handleTrainerDialogueInput() {
        if (session.getCurrentMap().getType() != MapType.HUB || !session.getPlayer().isAlive()
                || menuBack()) {
            dialogueOpen = false;
            activeNpc = null;
            return;
        }
        var choices = ClassTrainer.Choice.values();
        if (MenuKeys.up()) {
            trainerChoice = Math.floorMod(trainerChoice - 1, choices.length);
        } else if (MenuKeys.down()) {
            trainerChoice = (trainerChoice + 1) % choices.length;
        }
        if (menuConfirm()) {
            saveMenu.notify(classTrainer.select(choices[trainerChoice])
                    ? "Applied: " + classTrainer.label(choices[trainerChoice])
                    : "Unchanged: already selected or an ability is still active.");
        }
    }

    /**
     * Handles the modal choice before travel shortcuts or ability input can consume the same keys.
     * Arrows change the highlight, E confirms through PortalGuide, and Escape cancels unchanged.
     * Confirmation configures a route only; entering the portal on a later frame performs travel.
     */
    private void handlePortalDialogueInput() {
        if (session.getCurrentMap().getType() != MapType.HUB || !session.getPlayer().isAlive()
                || menuBack()) {
            dialogueOpen = false;
            activeNpc = null;
            return;
        }
        if (MenuKeys.up()) {
            var choices = PortalGuide.Destination.values();
            portalChoice = choices[Math.floorMod(portalChoice.ordinal() - 1, choices.length)];
        } else if (MenuKeys.down()) {
            var choices = PortalGuide.Destination.values();
            portalChoice = choices[(portalChoice.ordinal() + 1) % choices.length];
        }
        if (menuConfirm()) {
            try {
                portalGuide.select(portalChoice);
                saveMenu.notify("Portal ready: " + portalGuide.destinationLabel(portalChoice) + "; step into the portal");
                dialogueOpen = false;
                activeNpc = null;
            } catch (IllegalArgumentException | IllegalStateException failure) {
                saveMenu.notify(failure.getMessage());
            }
        }
    }

    /** Updates non-blocking visual feedback after a completed return; no Quest/reward state is changed. */
    private void showBattleReturn() {
        encounterFlow.getLastResult()
                .filter(result -> result.encounterVersion() == session.getTransitionVersion() - 1)
                .ifPresent(result -> renderer.showOutcome(result, session.getPlayer().getPlayerClass()));
        travelStatus = "Challenge " + encounterFlow.getLastResult()
                .filter(result -> result.encounterVersion() == session.getTransitionVersion() - 1)
                .map(result -> result.outcome().name())
                .orElse("FAILURE") + "; returned to Hub with full HP";
        updateMapView();
    }


    /** Handles the completed Sage's optional credits without changing quest or reward state. */
    private void handleCreditsInput() {
        if (session.getCurrentMap().getType() != MapType.MEMORIAL
                || menuBack()) {
            credits.close();
        } else {
            credits.select(MenuKeys.up(), MenuKeys.down());
            if (menuConfirm()) { credits.confirm(); }
        }
        if (!credits.isOpen()) {
            dialogueOpen = false;
            dialogueLine = 0;
            activeNpc = null;
        }
    }

    /** Advances dialogue, offers optional Hall credits after completion, or closes and grants an NPC reward. */
    private void advanceDialogue() {
        dialogueLine++;
        if (activeNpc == GameConfig.GUIDE && session.getCurrentMap().getType() == MapType.MEMORIAL
                && quests.getCompleted().size() == BossChallenge.values().length
                && dialogueLine >= dialogueLines().size()) {
            dialogueLine = 0;
            credits.open();
            return;
        }
        if (activeNpc == null || dialogueLine >= dialogueLines().size()) {
            dialogueOpen = false;
            if (activeNpc != null && !activeNpc.hasInteracted()) {
                activeNpc.setInteracted();
                String reward = activeNpc.getRewardItemId();
                if (reward != null && !reward.isBlank()) {
                    inventory.addItem(reward, 1);
                    saveMenu.notify("Received: " + reward);
                } else {
                    saveMenu.notify("Talked to: " + activeNpc.getName());
                }
            } else if (activeNpc != null) {
                saveMenu.notify("Talked to: " + activeNpc.getName());
            }
            updateMapView();
            activeNpc = null;
        }
    }
    /**
     * Finds the closest NPC within interaction range on the current map.
     *
     * @param player the player used as the search origin
     * @return the nearest NPC in range, or null when none is close enough
     */
    private NPC nearestNpc(Player player) {
        NPC best = null;
        double bestDistance = Double.MAX_VALUE;
        for (NPC npc : currentNpcs()) {
            double dx = player.getX() - npc.getX();
            double dy = player.getY() - npc.getY();
            double distance = Math.sqrt(dx * dx + dy * dy);
            if (distance <= 70.0 && distance < bestDistance) {
                best = npc;
                bestDistance = distance;
            }
        }
        return best;
    }

    /**
     * Supplies map-local NPCs; real battles continue to contain only the player and one Boss.
     * @return Hub services, Training exit, Memorial Sage, or no Battle NPCs
     */
    private List<NPC> currentNpcs() {
        return switch (session.getCurrentMap().getType()) {
            case HUB -> GameConfig.HUB_NPCS;
            case TRAINING -> List.of(GameConfig.TRAINING_EXIT);
            case MEMORIAL -> List.of(GameConfig.GUIDE);
            case BATTLE -> List.of();
        };
    }
    /**
     * Draws the player's health bar (red background, green fill) at the top-left corner,
     * with the numeric value centred inside the bar. Uses this screen's own resources.
     * @param hud current size and visibility policy
     */
    private void drawHealth(HudLayout hud) {
        if (!hud.healthVisible()) {
            return;
        }
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        float scale = hud.scale();
        float x = 12f * scale;
        float barW = 180f * scale;
        float barH = 18f * scale;
        float barY = h - 10f * scale - barH;
        float ratio = currentHp() / (float) session.getPlayer().getHealth().getMaxHp();

        dialogueFont.getData().setScale(scale);
        dialogueShapes.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, w, h));
        dialogueShapes.begin(ShapeRenderer.ShapeType.Filled);
        dialogueShapes.setColor(0.75f, 0.15f, 0.15f, 1);
        dialogueShapes.rect(x, barY, barW, barH);
        dialogueShapes.setColor(0.20f, 0.85f, 0.35f, 1);
        dialogueShapes.rect(x, barY, barW * ratio, barH);
        dialogueShapes.end();

        float baseline = barY + barH / 2f + dialogueFont.getCapHeight() / 2f;
        dialogueBatch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, w, h));
        dialogueBatch.begin();
        dialogueFont.setColor(0.1f, 0.1f, 0.1f, 1);
        dialogueFont.draw(dialogueBatch, currentHpText() + " / " + session.getPlayer().getHealth().getMaxHp(),
                x, baseline, barW, Align.center, false);
        dialogueBatch.end();
    }

    /** Draws final delivered damage totals for the current stationary practice target only. */
    private void drawTrainingScore() {
        var practice = combatDemo.getTrainingDummy();
        if (practice.isEmpty()) {
            return;
        }
        var dummy = practice.orElseThrow();
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        float scale = Math.max(0.1f, Math.min(1.4f, Math.min(w / 960f, h / 720f)));
        float x = 12f * scale;
        float top = h - 85f * scale;
        float boxW = 330f * scale;
        float boxH = 225f * scale;
        dialogueShapes.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, w, h));
        dialogueShapes.begin(ShapeRenderer.ShapeType.Filled);
        dialogueShapes.setColor(0.04f, 0.07f, 0.10f, 1);
        dialogueShapes.rect(x, top - boxH, boxW, boxH);
        dialogueShapes.end();
        dialogueFont.getData().setScale(scale);
        dialogueBatch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, w, h));
        dialogueBatch.begin();
        dialogueFont.setColor(0.85f, 0.95f, 1f, 1);
        dialogueFont.draw(dialogueBatch, "TRAINING DAMAGE SCORE\n"
                        + "Total damage: " + dummy.getTotalDamage() + "\n"
                        + "Last damage: " + dummy.getLastDamage() + "\n"
                        + "Damage hits: " + dummy.getHitCount() + "\n"
                        + String.format(java.util.Locale.ROOT, "Move speed: %.1f units/s\n", session.getPlayer().getSpeed())
                        + "Incoming hit: " + session.getPlayer().getTrainingLastDamage() + " HP\n"
                        + "Target fires UP: 25 base damage / 2s\n"
                        + "No damage for 5s: full recovery\n"
                        + "[R] Reset practice\nTalk to Return to Hub NPC [E] to leave",
                x + 10f * scale, top - 12f * scale, boxW - 20f * scale, Align.left, true);
        dialogueBatch.end();
    }
    /** Reads current HP directly from the model rather than parsing stale status text.
     * @return current player HP
     */
    private int currentHp() { return session.getPlayer().getHealth().getCurrentHp(); }

    /** Formats the live HP numerator.
     * @return player-facing HP text
     */
    private String currentHpText() { return "HP " + currentHp(); }
    /**
     * Draws the live player coordinate in the top-left corner.
     * Uses this screen's own font and batch, so no renderer change is needed.
     * @param hud current size and visibility policy
     */
    private void drawCoordinates(HudLayout hud) {
        if (!hud.detailsVisible()) {
            return;
        }
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        float scale = hud.scale();
        dialogueFont.getData().setScale(scale);
        dialogueBatch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, w, h));
        dialogueBatch.begin();
        dialogueFont.setColor(0.20f, 0.95f, 0.55f, 1);
        dialogueFont.draw(dialogueBatch,
                "Player x=" + (int) session.getPlayer().getX()
                        + "  y=" + (int) session.getPlayer().getY(),
                12f * scale, h - 40f * scale);
        dialogueBatch.end();
    }
    /**
     * Draws an "[E] Talk" hint above the dialogue box while an NPC is within range.
     * Hidden while a dialogue or the inventory panel is open.
     */
    private void drawInteractionPrompt() {
        if (dialogueOpen || inventoryOpen || chestOpen || bigMap) {
            return;
        }
        NPC near = nearestNpc(session.getPlayer());
        if (near == null) {
            return;
        }
        String hint = switch (near.getId()) {
            case "portal_guide" -> "[E] Choose portal destination";
            case "class_master" -> "[E] Choose class / O ultimate mode";
            case "quest_keeper" -> "[E] Accept Boss quest / view completion";
            case "training_exit" -> "[E] Return to Hub";
            case "notice_board" -> "[E] Read " + near.getName();
            case "healing_spring" -> "[E] Drink from " + near.getName();
            case "storage_chest" -> "[E] Open " + near.getName();
            default -> "[E] Talk to " + near.getName();
        };
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        float scale = Math.max(1f, h / 720f) * 1.4f;
        dialogueFont.getData().setScale(scale);
        dialogueBatch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, w, h));
        dialogueBatch.begin();
        dialogueFont.setColor(1f, 0.85f, 0.30f, 1);
        dialogueFont.draw(dialogueBatch, hint, 0, 120f * scale, w, Align.center, false);
        dialogueBatch.end();
    }
    /**
     * Returns the active NPC script, the Portal Guide choice list, or a one-time reward reminder.
     *
     * @return the visible dialogue lines, never null
     */
    private List<String> dialogueLines() {
        if (credits.isOpen()) { return List.of(credits.text()); }
        if (endingOpen) {
            return BossQuests.ENDING_LINES;
        }
        if (isQuestDialogueOpen()) {
            StringBuilder text = new StringBuilder("Defeated: ").append(quests.getCompleted().size())
                    .append(" / ").append(BossChallenge.values().length).append('\n');
            var targets = BossChallenge.values();
            for (int i = 0; i < targets.length; i++) {
                text.append(i == questChoice ? ">     " : "      ").append(targets[i].displayName());
                text.append(" [wins: ").append(quests.getSuccessCount(targets[i])).append(']');
                if (quests.getActive().orElse(null) == targets[i]) {
                    text.append(" (ACTIVE)");
                }
                text.append('\n');
            }
            return List.of(text.append(questFeedback).toString());
        }
        if (isMerchantDialogueOpen()) {
            if (upgradeMenu) { return List.of(upgradeText()); }
            StringBuilder text = new StringBuilder("Welcome! Choose a service:\n\n");
            var choices = MerchantServices.Choice.values();
            for (int i = 0; i < choices.length; i++) {
                text.append(i == merchantChoice ? "> " : "  ")
                        .append(MerchantServices.label(choices[i], activeNpc, inventory, chestStorage)).append("\n\n");
            }
            return List.of(text.append(merchantFeedback).toString());
        }
        if (isTrainerDialogueOpen()) {
            StringBuilder text = new StringBuilder("Current: ")
                    .append(session.getPlayer().getPlayerClass().getDisplayName())
                    .append(" | O mode ").append(session.getPlayer().getUltimateMode()).append('\n');
            var choices = ClassTrainer.Choice.values();
            for (int i = 0; i < choices.length; i++) {
                text.append(i == trainerChoice ? "> " : "  ").append(classTrainer.label(choices[i])).append('\n');
            }
            return List.of(text.append(trainerFeedback).toString());
        }
        if (isPortalDialogueOpen()) {
            StringBuilder text = new StringBuilder();
            for (var choice : PortalGuide.Destination.values()) {
                text.append(choice == portalChoice ? "> " : "  ")
                        .append(portalGuide.destinationLabel(choice)).append('\n');
            }
            return List.of(text.toString());
        }
        if (activeNpc.getId().equals(GameConfig.GUIDE.getId())
                && session.getCurrentMap().getType() == MapType.MEMORIAL) {
            return List.of("Awakened stones: " + quests.getCompleted().size() + " / 5.\n"
                    + (quests.getCompleted().size() == BossChallenge.values().length
                    ? "The seal is restored. Your deeds will be remembered here."
                    : "Defeat the remaining guardians, then speak to me again.\nThe portal at the lower left returns to Hub."));
        }
        List<String> base = activeNpc.getLines();
        if (!activeNpc.hasInteracted() || activeNpc.getRewardItemId() == null) {
            return base;
        }
        List<String> withNotice = new ArrayList<>(base);
        withNotice.add("You already took the " + prettyName(activeNpc.getRewardItemId()) + ".");
        return withNotice;
    }
    /** Formats upgrade previews and explicit material-spending confirmation.
     * @return current merchant upgrade page
     */
    private String upgradeText() {
        if (!pendingUpgrade.isEmpty()) {
            String material = "sentinel-core";
            int level = GearUpgrades.level(pendingUpgrade);
            String base = level == 0 ? pendingUpgrade : pendingUpgrade.substring(0, pendingUpgrade.lastIndexOf('+'));
            return "Confirm upgrade: " + ItemCatalog.name(pendingUpgrade) + " -> +" + (level + 1)
                    + "\n" + ItemCatalog.effect(pendingUpgrade) + " -> " + ItemCatalog.effect(base + "+" + (level + 1))
                    + (session.getPlayer().getDebugOptions().freeUpgrade() ? "\nDEBUG: free upgrade" : "\nCost: 1 " + ItemCatalog.name(material))
                    + "\nAvailable: " + GearUpgrades.count(material, inventory, chestStorage)
                    + "\nBag before chest.\n[E] Spend material and upgrade  [Esc] Cancel";
        }
        StringBuilder text = new StringBuilder("UPGRADE EQUIPMENT - max +2\n");
        for (var slot : EquipmentSlot.values()) {
            String id = GearUpgrades.candidate(slot, inventory, equipment);
            text.append(slot.ordinal() == upgradeChoice ? "> " : "  ").append(slot).append(": ")
                    .append(id.isEmpty() ? "Not in bag" : ItemCatalog.name(id) + " | " + ItemCatalog.effect(id))
                    .append("\n");
        }
        return text.append("\nShared cores: ").append(GearUpgrades.count("sentinel-core", inventory, chestStorage))
                .append(" (Bag: ").append(inventory.quantityOf("sentinel-core"))
                .append(" | Chest: ").append(chestStorage.quantityOf("sentinel-core")).append(")")
                .append("\n").append(merchantFeedback).toString();
    }

    /** Measures wrapped text to fit all dialogue bands before drawing, including on window resize. */
    private void drawDialogue() {
        if (!dialogueOpen || activeNpc == null) {
            return;
        }
        var lines = dialogueLines();
        if (dialogueLine >= lines.size()) {
            return;
        }
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        if (w <= 0 || h <= 0) { return; }
        String confirmHint = "E/Enter: Confirm / Next\nW/S or Up/Down: select";
        String backHint = "Esc: Back / Close\nClick: select; right-click: confirm";
        String hint = confirmHint + backHint;
        String body = lines.get(dialogueLine);
        DialogueLayout layout = DialogueLayout.fit(w, h, isMerchantDialogueOpen() || isTrainerDialogueOpen() || isQuestDialogueOpen() || credits.isOpen(),
                activeNpc.getName(), body, hint, (value, scale, width) -> value.equals(hint)
                        ? Math.max(measureDialogueText(confirmHint, scale, width / 2),
                                measureDialogueText(backHint, scale, width / 2))
                        : measureDialogueText(value, scale, width));
        cacheDialogueHits(layout, body);
        float boxW = layout.width(), boxH = layout.height(), x = layout.x(), y = layout.y();
        float scale = layout.scale();
        dialogueFont.getData().setScale(scale);

        dialogueShapes.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, w, h));
        dialogueShapes.begin(ShapeRenderer.ShapeType.Filled);
        dialogueShapes.setColor(0.05f, 0.06f, 0.09f, 0.92f);
        dialogueShapes.rect(x, y, boxW, boxH);
        dialogueShapes.end();
        dialogueShapes.begin(ShapeRenderer.ShapeType.Line);
        dialogueShapes.setColor(0.95f, 0.85f, 0.35f, 1);
        dialogueShapes.rect(x, y, boxW, boxH);
        dialogueShapes.end();

        float pad = layout.padding();
        dialogueBatch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, w, h));
        dialogueBatch.begin();
        dialogueFont.setColor(1f, 0.90f, 0.40f, 1);
        dialogueFont.draw(dialogueBatch, activeNpc.getName(), x + pad, layout.titleTop(),
                layout.textWidth(), Align.left, true);
        dialogueFont.setColor(1f, 1f, 1f, 1);
        dialogueFont.draw(dialogueBatch, body,
                x + pad, layout.bodyTop(), layout.textWidth(), Align.left, true);
        dialogueFont.setColor(0.60f, 0.90f, 1f, 1);
        dialogueFont.draw(dialogueBatch, confirmHint, x + pad, layout.hintTop(), layout.textWidth() / 2, Align.left, true);
        dialogueFont.draw(dialogueBatch, backHint, x + pad + layout.textWidth() / 2, layout.hintTop(), layout.textWidth() / 2, Align.right, true);
        dialogueBatch.end();
        if (isQuestDialogueOpen()) {
            drawQuestChecks(x + pad, layout.bodyTop(), scale, body, layout.textWidth());
        }
    }

    /**
     * Measures with exactly the font scale and wrap width subsequently used for drawing.
     * @param text current text including explicit line breaks
     * @param scale requested uniform font scale
     * @param width available wrap width
     * @return actual wrapped glyph height
     */
    private float measureDialogueText(String text, float scale, float width) {
        dialogueFont.getData().setScale(scale);
        return new GlyphLayout(dialogueFont, text, com.badlogic.gdx.graphics.Color.WHITE,
                width, Align.left, true).height;
    }

    /**
     * Draws actual green check marks before completed Boss names, without relying on font glyph coverage.
     * @param x left edge of the menu text
     * @param baseline baseline of the first text line (the completion count)
     * @param scale current dialogue font scale
     * @param body menu text, used to account for wrapped rows before each check mark
     * @param width body wrap width, identical to the actual drawing width
     */
    private void drawQuestChecks(float x, float baseline, float scale, String body, float width) {
        dialogueShapes.begin(ShapeRenderer.ShapeType.Line);
        dialogueShapes.setColor(0.25f, 1f, 0.4f, 1);
        var targets = BossChallenge.values();
        String[] rows = body.split("\n", -1);
        for (int i = 0; i < targets.length; i++) {
            if (quests.getCompleted().contains(targets[i])) {
                String prefix = String.join("\n", java.util.Arrays.copyOfRange(rows, 0, i + 1));
                float precedingHeight = measureDialogueText(prefix, scale, width)
                        + dialogueFont.getLineHeight() - dialogueFont.getCapHeight();
                float y = baseline - precedingHeight - dialogueFont.getCapHeight() / 2;
                float left = x + 13f * scale;
                dialogueShapes.line(left, y, left + 4f * scale, y - 4f * scale);
                dialogueShapes.line(left + 4f * scale, y - 4f * scale, left + 12f * scale, y + 5f * scale);
            }
        }
        dialogueShapes.end();
    }
    /**
     * Draws the persistent weapon slot in the top-right corner, within the capped HUD size.
     * @param hud current size and visibility policy
     */
    private void drawWeaponSlot(HudLayout hud) {
        if (!hud.detailsVisible()) {
            return;
        }
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        float scale = hud.scale();
        float boxW = 200f * scale;
        float boxH = 58f * scale;
        float x = w - boxW - 12f * scale;
        float y = h - boxH - 10f * scale;
        float pad = 12f * scale;

        dialogueShapes.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, w, h));
        dialogueShapes.begin(ShapeRenderer.ShapeType.Filled);
        dialogueShapes.setColor(0.05f, 0.06f, 0.10f, 0.88f);
        dialogueShapes.rect(x, y, boxW, boxH);
        dialogueShapes.end();
        dialogueShapes.begin(ShapeRenderer.ShapeType.Line);
        dialogueShapes.setColor(0.95f, 0.80f, 0.35f, 1);
        dialogueShapes.rect(x, y, boxW, boxH);
        dialogueShapes.end();

        dialogueFont.getData().setScale(scale);
        dialogueBatch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, w, h));
        dialogueBatch.begin();
        dialogueFont.setColor(0.95f, 0.80f, 0.35f, 1);
        dialogueFont.draw(dialogueBatch, "WEAPON", x + pad, y + boxH - pad);
        if (equippedWeapon.isEmpty()) {
            dialogueFont.setColor(0.55f, 0.58f, 0.65f, 1);
            dialogueFont.draw(dialogueBatch, "No weapon equipped", x + pad, y + pad + 6f);
        } else {
            dialogueFont.setColor(0.40f, 1f, 0.55f, 1);
            dialogueFont.draw(dialogueBatch, prettyName(equippedWeapon),
                    x + pad, y + pad + 6f);
        }
        dialogueBatch.end();
    }
    /** Draws the inventory panel in the centre of the screen while it is open. */
    private void drawInventory() {
        if (!inventoryOpen) {
            return;
        }
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        PanelLayout p = inventoryLayout();

        dialogueShapes.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, w, h));
        dialogueShapes.begin(ShapeRenderer.ShapeType.Filled);
        dialogueShapes.setColor(0.04f, 0.05f, 0.08f, 0.94f);
        dialogueShapes.rect(p.x, p.y, p.boxW, p.boxH);
        dialogueShapes.end();
        dialogueShapes.begin(ShapeRenderer.ShapeType.Line);
        dialogueShapes.setColor(0.55f, 0.85f, 1f, 1);
        dialogueShapes.rect(p.x, p.y, p.boxW, p.boxH);
        dialogueShapes.end();

        dialogueFont.getData().setScale(p.scale);
        dialogueBatch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, w, h));
        dialogueBatch.begin();
        dialogueFont.setColor(0.60f, 0.90f, 1f, 1);
        dialogueFont.draw(dialogueBatch, "BAG " + (inventoryPage + 1) + "/" + pageCount(false) + " | W/S or Up/Down: select", p.x, p.y + p.boxH - p.pad);
        float cursor = p.y + p.boxH - p.pad - p.lineH * 1.6f;
        if (inventory.isEmpty()) {
            dialogueFont.setColor(0.85f, 0.85f, 0.85f, 1);
            dialogueFont.draw(dialogueBatch, "(empty)", p.x + p.pad, cursor);
        } else {
            int row = 0;
            for (Inventory.ItemStack stack : visibleEntries(inventory, false)) {
                boolean equipped = equipment.isEquipped(stack.materialId());
                if (equipped) {
                    dialogueFont.setColor(0.40f, 1f, 0.55f, 1);
                } else if (isWeapon(stack.materialId()) || ItemCatalog.gear(stack.materialId()).isPresent()) {
                    dialogueFont.setColor(0.95f, 0.90f, 0.55f, 1);
                } else {
                    dialogueFont.setColor(0.85f, 0.85f, 0.85f, 1);
                }
                String label = (row++ == bagRow ? "> " : "  ") + prettyName(stack.materialId()) + "  x" + stack.quantity()
                        + (equipped ? " [Worn]" : "") + "  " + ItemCatalog.effect(stack.materialId());
                dialogueFont.draw(dialogueBatch, label, p.x + p.pad, cursor);
                cursor -= p.lineH;
            }
        }
        dialogueFont.setColor(0.55f, 0.60f, 0.70f, 1);
        drawItemFooter(p.x, p.y, p.boxW, p.pad);
        dialogueFont.setColor(0.9f, 0.85f, 0.6f, 1);
        dialogueFont.draw(dialogueBatch, "ATK +" + equipment.attackPercent() + "%  DEF "
                + equipment.defencePercent() + "%  Set " + equipment.setPieces() + "/4",
                p.x + p.pad, p.y + p.pad + p.lineH * 3);
        dialogueFont.draw(dialogueBatch, "Set: 2 = DEF +5%; 4 = ATK +10%",
                p.x + p.pad, p.y + p.pad + p.lineH * 2);
        dialogueFont.draw(dialogueBatch, inventoryFeedback,
                p.x + p.pad, p.y + p.pad + p.lineH);
        dialogueBatch.end();
    }
    /**
     * Draws a minimap in the bottom-right corner, or an enlarged centred map when M is held open.
     * Screen-space only; the expanded panel follows window size while text stays readable.
     * Memorial Hall fits one view and omits the minimap so that all five stones remain visible.
     * A living Boss or training target is marked with a red square;
     * the green player square is drawn last so the two never hide each other while closing in.
     * @param hud current size and visibility policy
     */
    private void drawMinimap(HudLayout hud) {
        if (inventoryOpen || chestOpen) { return; }
        if (!hud.detailsVisible() || session.getCurrentMap().getType() == MapType.MEMORIAL) {
            return;
        }
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        float ui = hud.scale();
        GameMap map = session.getCurrentMap();
        CollisionBox bounds = map.getBounds();

        float boxW = bigMap ? w * 0.92f : 200f * ui;
        float boxH = bigMap ? h * 0.88f : 140f * ui;
        float x = bigMap ? (w - boxW) / 2f : w - boxW - 12f * ui;
        float y = bigMap ? (h - boxH) / 2f : 12f * ui;
        float pad = 7f * ui;
        float band = bigMap ? 32f * ui : 0;
        float s = Math.min((boxW - pad * 2) / (float) bounds.width(),
                (boxH - pad * 2 - 2 * band) / (float) bounds.height());
        float ox = x + pad + ((boxW - pad * 2) - (float) bounds.width() * s) / 2f;
        float oy = y + pad + ((boxH - pad * 2) - (float) bounds.height() * s) / 2f;

        dialogueShapes.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, w, h));
        dialogueShapes.begin(ShapeRenderer.ShapeType.Filled);
        dialogueShapes.setColor(0.03f, 0.04f, 0.06f, bigMap ? 0.97f : 0.82f);
        dialogueShapes.rect(x, y, boxW, boxH);
        boolean hub = map.getType() == MapType.HUB;
        dialogueShapes.setColor(hub ? 0.08f : 0.24f, hub ? 0.18f : 0.11f,
                hub ? 0.22f : 0.10f, 1);
        dialogueShapes.rect(ox, oy, (float) bounds.width() * s, (float) bounds.height() * s);

        dialogueShapes.setColor(0.55f, 0.58f, 0.65f, 1);
        for (CollisionBox obstacle : map.getObstacles()) {
            dialogueShapes.rect(ox + (float) (obstacle.x() - bounds.x()) * s,
                    oy + (float) (obstacle.y() - bounds.y()) * s,
                    (float) obstacle.width() * s, (float) obstacle.height() * s);
        }
        for (Portal portal : travel.getPortals(map.getId())) {
            dialogueShapes.setColor(portal.enabled() ? 0.65f : 0.28f,
                    0.25f, portal.enabled() ? 0.85f : 0.32f, 1);
            CollisionBox trigger = portal.trigger();
            dialogueShapes.rect(ox + (float) (trigger.x() - bounds.x()) * s,
                    oy + (float) (trigger.y() - bounds.y()) * s,
                    Math.max((float) trigger.width() * s, 3f),
                    Math.max((float) trigger.height() * s, 3f));
        }
        for (NPC npc : currentNpcs()) {
            dialogueShapes.setColor(1f, 0.85f, 0.20f, 1);
            dialogueShapes.rect(ox + (float) (npc.getX() - bounds.x()) * s,
                    oy + (float) (npc.getY() - bounds.y()) * s,
                    Math.max((float) npc.getWidth() * s, 3f),
                    Math.max((float) npc.getHeight() * s, 3f));
        }
        combatDemo.getMapTargetBounds().ifPresent(bossBody -> {
            dialogueShapes.setColor(0.95f, 0.16f, 0.16f, 1);
            dialogueShapes.rect(ox + (float) (bossBody.x() - bounds.x()) * s,
                    oy + (float) (bossBody.y() - bounds.y()) * s,
                    Math.max((float) bossBody.width() * s, 5f),
                    Math.max((float) bossBody.height() * s, 5f));
        });
        CollisionBox body = session.getPlayer().getBounds();
        dialogueShapes.setColor(0.20f, 0.95f, 0.45f, 1);
        dialogueShapes.rect(ox + (float) (body.x() - bounds.x()) * s,
                oy + (float) (body.y() - bounds.y()) * s,
                Math.max((float) body.width() * s, 4f),
                Math.max((float) body.height() * s, 4f));
        dialogueShapes.end();

        dialogueShapes.begin(ShapeRenderer.ShapeType.Line);
        dialogueShapes.setColor(0.55f, 0.85f, 1f, 1);
        dialogueShapes.rect(x, y, boxW, boxH);
        dialogueShapes.end();

        dialogueFont.getData().setScale(ui);
        dialogueBatch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, w, h));
        dialogueBatch.begin();
        dialogueFont.setColor(0.60f, 0.90f, 1f, 1);
        dialogueFont.draw(dialogueBatch, map.getName(), x + pad, y + boxH - pad);
        dialogueFont.setColor(0.55f, 0.60f, 0.70f, 1);
        dialogueFont.draw(dialogueBatch, bigMap ? "[M / Esc] Close   |   Yellow: NPC   Green: You   Red: Boss/Target   Purple: Portal" : "[M] Expand",
                x + pad, y + pad + dialogueFont.getCapHeight());
        if (bigMap) {
            drawMapNpcLabels(bounds, s, ox, oy, x + pad, y + pad + band,
                    boxW - 2 * pad, boxH - 2 * pad - 2 * band, ui);
        }
        dialogueBatch.end();
    }
    /** Draws NPC names beside markers, shifting nearby labels to avoid each other.
     * @param bounds world bounds
     * @param zoom world-to-map scale
     * @param ox map origin x
     * @param oy map origin y
     * @param left label area left
     * @param bottom label area bottom
     * @param width label area width
     * @param height label area height
     * @param ui text scale
     */
    private void drawMapNpcLabels(CollisionBox bounds, float zoom, float ox, float oy,
            float left, float bottom, float width, float height, float ui) {
        var occupied = new ArrayList<com.badlogic.gdx.math.Rectangle>();
        float gap = 5 * ui;
        dialogueFont.setColor(1f, 0.90f, 0.40f, 1f);
        for (NPC npc : currentNpcs()) {
            var text = new com.badlogic.gdx.graphics.g2d.GlyphLayout(dialogueFont, npc.getName());
            float labelWidth = Math.min(text.width, width);
            float markerX = ox + (float) (npc.getX() + npc.getWidth() / 2 - bounds.x()) * zoom;
            float markerTop = oy + (float) (npc.getY() + npc.getHeight() - bounds.y()) * zoom;
            float labelX = Math.max(left, Math.min(left + width - labelWidth, markerX - labelWidth / 2));
            float labelY = Math.max(bottom, Math.min(bottom + height - text.height, markerTop + gap));
            var box = new com.badlogic.gdx.math.Rectangle(labelX - gap, labelY - gap,
                    labelWidth + 2 * gap, text.height + 2 * gap);
            for (int attempt = 0; attempt < currentNpcs().size() * 2; attempt++) {
                boolean overlap = false;
                for (var previous : occupied) { if (box.overlaps(previous)) { overlap = true; break; } }
                if (!overlap) { break; }
                float direction = attempt % 2 == 0 ? 1 : -1;
                float offset = (attempt / 2 + 1) * (text.height + 3 * gap) * direction;
                box.y = Math.max(bottom, Math.min(bottom + height - text.height, labelY + offset)) - gap;
            }
            occupied.add(box);
            dialogueFont.draw(dialogueBatch, npc.getName(), labelX, box.y + gap + text.height,
                    labelWidth, Align.left, false);
        }
    }

    /**
     * Screen-space geometry of the chest transfer panel.
     *
     * @param x      left edge
     * @param y      bottom edge
     * @param boxW   panel width
     * @param boxH   panel height
     * @param pad    inner padding
     * @param lineH  height of one item row
     * @param scale  font scale factor
     * @param colW   width of one of the two columns
     */
    private record ChestLayout(float x, float y, float boxW, float boxH,
                               float pad, float lineH, float scale, float colW) {
    }

    /**
     * Recomputes the chest panel geometry from the current window size.
     * @return layout shared by chest rendering and hit testing
     */
    private ChestLayout chestLayout() {
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        float scale = Math.max(0.1f, Math.min(1.2f, Math.min(w / 900f, h / 600f)));
        float boxW = Math.min(w * 0.94f, 860f * scale);
        float boxH = h * 0.8f;
        float pad = 16f * scale;
        float lineH = 26f * scale;
        return new ChestLayout((w - boxW) / 2f, (h - boxH) / 2f,
                boxW, boxH, pad, lineH, scale, (boxW - pad * 3) / 2f);
    }

    /**
     * Finds which item row sits under the cursor within one column.
     *
     * @param mouseX cursor x in screen units
     * @param mouseY cursor y measured from the bottom of the window
     * @param colX column's left edge
     * @param colW column width
     * @param firstLine baseline of the first row
     * @param lineH height allocated to each row
     * @param count number of available rows
     * @return zero-based row index, or -1 when no row is hit
     */
    private static int hitRow(float mouseX, float mouseY, float colX, float colW,
                              float firstLine, float lineH, int count) {
        for (int i = 0; i < count; i++) {
            float baseline = firstLine - i * lineH;
            if (mouseX >= colX && mouseX <= colX + colW
                    && mouseY <= baseline && mouseY >= baseline - lineH) {
                return i;
            }
        }
        return -1;
    }

    /** Moves the selected full stack through the same path for keyboard and mouse.
     * @param row visible row
     * @param fromChest whether to withdraw instead of deposit
     */
    private void transferItem(int row, boolean fromChest) {
        Inventory source = fromChest ? chestStorage : inventory;
        Inventory target = fromChest ? inventory : chestStorage;
        var entries = visibleEntries(source, true);
        if (row < 0 || row >= entries.size()) { return; }
        var stack = entries.get(row);
        if ((long) target.quantityOf(stack.materialId()) + stack.quantity() > Integer.MAX_VALUE) {
            saveMenu.notify("Destination stack is full"); return;
        }
        int moved = source.removeItem(stack.materialId(), stack.quantity());
        target.addItem(stack.materialId(), moved);
        equipment.reconcile();
        saveMenu.notify((fromChest ? "Taken: " : "Stored: ") + prettyName(stack.materialId()) + " x" + moved);
    }

    /** Executes one clicked footer button for pagination, activation or closing.
     * @return whether a click was consumed
     */
    private boolean itemFooterClick() {
        if (!Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) { return false; }
        float x, y, width, pad, line;
        if (chestOpen) { var p = chestLayout(); x = p.x; y = p.y; width = p.boxW; pad = p.pad; line = p.lineH; }
        else { var p = inventoryLayout(); x = p.x; y = p.y; width = p.boxW; pad = p.pad; line = p.lineH; }
        float mx = Gdx.input.getX(), my = Gdx.graphics.getHeight() - Gdx.input.getY();
        if (mx < x + pad || mx >= x + width - pad || my > y + pad + 4 || my < y + pad + 4 - line) { return false; }
        int button = (int) ((mx - x - pad) / ((width - 2 * pad) / 4));
        if (button < 2) {
            int step = button == 0 ? -1 : 1;
            if (chestOpen) { chestPage = Math.max(0, Math.min(pageCount(true) - 1, chestPage + step)); storageRow = 0; }
            else { inventoryPage = Math.max(0, Math.min(pageCount(false) - 1, inventoryPage + step)); bagRow = 0; }
        } else if (button == 2) {
            if (chestOpen) { transferItem(storageRow, storageRight); } else { useBagRow(bagRow); }
        } else { chestOpen = false; inventoryOpen = false; }
        return true;
    }

    /** Draws four equally spaced buttons sharing itemFooterClick geometry.
     * @param x panel left
     * @param y panel bottom
     * @param width panel width
     * @param pad panel padding
     */
    private void drawItemFooter(float x, float y, float width, float pad) {
        String[] names = {"Prev [Q]", "Next [R]", "Use [E]", "Close [Esc]"};
        float buttonWidth = (width - 2 * pad) / 4;
        for (int i = 0; i < names.length; i++) {
            dialogueFont.draw(dialogueBatch, names[i], x + pad + i * buttonWidth, y + pad + 4,
                    buttonWidth, Align.center, false);
        }
    }

    /** Owns all bag/storage input, preventing actions from reaching gameplay that frame. */
    private void handleItemPanelInput() {
        if (itemFooterClick()) { return; }
        int pages = pageCount(chestOpen);
        if (chestOpen) {
            chestPage = Math.max(0, Math.min(pages - 1, chestPage + MenuKeys.page()));
            if (MenuKeys.left()) { storageRight = false; }
            if (MenuKeys.right()) { storageRight = true; }
        } else { inventoryPage = Math.max(0, Math.min(pages - 1, inventoryPage + MenuKeys.page())); }
        var entries = visibleEntries(chestOpen && storageRight ? chestStorage : inventory, chestOpen);
        int row = chestOpen ? storageRow : bagRow;
        row = Math.max(0, Math.min(entries.size() - 1, row));
        if (!entries.isEmpty()) {
            if (MenuKeys.up()) { row = Math.floorMod(row - 1, entries.size()); }
            else if (MenuKeys.down()) { row = (row + 1) % entries.size(); }
        }
        if (chestOpen) { storageRow = row; } else { bagRow = row; }
        boolean left = Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);
        boolean right = Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT);
        boolean activate = MenuKeys.confirm();
        if (left || right) {
            int mouseX = Gdx.input.getX(), mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
            int hit;
            if (chestOpen) {
                var p = chestLayout();
                float first = p.y + p.boxH - p.pad - p.lineH * 2.5f;
                hit = hitRow(mouseX, mouseY, p.x + p.pad, p.colW, first, p.lineH, visibleEntries(inventory, true).size());
                if (hit >= 0) { storageRight = false; }
                else {
                    hit = hitRow(mouseX, mouseY, p.x + p.pad * 2 + p.colW, p.colW, first, p.lineH, visibleEntries(chestStorage, true).size());
                    if (hit >= 0) { storageRight = true; }
                }
                if (hit >= 0) { storageRow = hit; }
            } else {
                var p = inventoryLayout();
                hit = hitRow(mouseX, mouseY, p.x + p.pad, p.boxW - 2 * p.pad,
                        p.y + p.boxH - p.pad - p.lineH * 1.6f, p.lineH, entries.size());
                if (hit >= 0) { bagRow = hit; }
            }
            activate = hit >= 0 && right;
        }
        if (activate) {
            if (chestOpen) { transferItem(storageRow, storageRight); }
            else { useBagRow(bagRow); }
        }
        bagRow = Math.max(0, Math.min(visibleEntries(inventory, false).size() - 1, bagRow));
        storageRow = Math.max(0, Math.min(visibleEntries(storageRight ? chestStorage : inventory, true).size() - 1, storageRow));
    }

    /** Draws the chest transfer panel, distinguishing storage transfers from equipping in the B bag. */
    private void drawChest() {
        if (!chestOpen) {
            return;
        }
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        ChestLayout p = chestLayout();
        float top = p.y + p.boxH - p.pad;
        float rightCol = p.x + p.pad * 2 + p.colW;
        float firstLine = top - p.lineH * 2.5f;

        dialogueShapes.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, w, h));
        dialogueShapes.begin(ShapeRenderer.ShapeType.Filled);
        dialogueShapes.setColor(0.04f, 0.05f, 0.08f, 0.96f);
        dialogueShapes.rect(p.x, p.y, p.boxW, p.boxH);
        dialogueShapes.end();
        dialogueShapes.begin(ShapeRenderer.ShapeType.Line);
        dialogueShapes.setColor(0.85f, 0.65f, 0.30f, 1);
        dialogueShapes.rect(p.x, p.y, p.boxW, p.boxH);
        dialogueShapes.end();

        dialogueFont.getData().setScale(p.scale);
        dialogueBatch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, w, h));
        dialogueBatch.begin();
        dialogueFont.setColor(0.85f, 0.65f, 0.30f, 1);
        dialogueFont.draw(dialogueBatch, "STORAGE " + (chestPage + 1) + "/" + pageCount(true) + " | W/S: row | A/D: column", p.x + p.pad, top);
        dialogueFont.setColor(0.60f, 0.90f, 1f, 1);
        dialogueFont.draw(dialogueBatch, "INVENTORY", p.x + p.pad, top - p.lineH * 1.2f);
        dialogueFont.setColor(0.95f, 0.80f, 0.40f, 1);
        dialogueFont.draw(dialogueBatch, "CHEST", rightCol, top - p.lineH * 1.2f);

        float cursor = firstLine;
        if (inventory.isEmpty()) {
            dialogueFont.setColor(0.85f, 0.85f, 0.85f, 1);
            dialogueFont.draw(dialogueBatch, "(empty)", p.x + p.pad, cursor);
        } else {
            int row = 0;
            for (Inventory.ItemStack stack : visibleEntries(inventory, true)) {
                boolean equipped = equipment.isEquipped(stack.materialId());
                dialogueFont.setColor(equipped ? 0.40f : 0.85f,
                        equipped ? 1f : 0.85f, equipped ? 0.55f : 0.85f, 1);
                dialogueFont.draw(dialogueBatch, (row++ == storageRow && !storageRight ? "> " : "  ") + prettyName(stack.materialId())
                                + "  x" + stack.quantity() + (equipped ? "  [Worn]" : ""),
                        p.x + p.pad, cursor);
                cursor -= p.lineH;
            }
        }

        cursor = firstLine;
        if (chestStorage.isEmpty()) {
            dialogueFont.setColor(0.85f, 0.85f, 0.85f, 1);
            dialogueFont.draw(dialogueBatch, "(empty)", rightCol, cursor);
        } else {
            int row = 0;
            for (Inventory.ItemStack stack : visibleEntries(chestStorage, true)) {
                dialogueFont.setColor(0.95f, 0.90f, 0.55f, 1);
                dialogueFont.draw(dialogueBatch, (row++ == storageRow && storageRight ? "> " : "  ") + prettyName(stack.materialId())
                        + "  x" + stack.quantity(), rightCol, cursor);
                cursor -= p.lineH;
            }
        }

        dialogueFont.setColor(0.55f, 0.60f, 0.70f, 1);
        drawItemFooter(p.x, p.y, p.boxW, p.pad);
        dialogueBatch.end();
    }
    /**
     * Turns a raw material id such as weapon_lightning_staff into a readable name.
     * @param materialId non-null material identifier
     * @return formatted name, or the original identifier when no name remains
     * @throws NullPointerException if materialId is null
     */
    private String prettyName(String materialId) {
        if (ItemCatalog.gear(materialId).isPresent() || ItemCatalog.POTION.equals(materialId)) {
            return ItemCatalog.name(materialId);
        }
        String cleaned = materialId.replace("weapon_", "").replace('_', ' ').trim();
        if (cleaned.isEmpty()) {
            return materialId;
        }
        return Character.toUpperCase(cleaned.charAt(0)) + cleaned.substring(1);
    }
    /**
     * Recomputes the inventory panel geometry from the current window size.
     * Shared by drawing and hit testing so both stay in sync.
     *
     * @return the current panel layout
     */
    private PanelLayout inventoryLayout() {
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        float scale = Math.max(0.1f, Math.min(1.2f, Math.min(w / 720f, h / 600f)));
        float boxW = Math.min(w * 0.94f, 680f * scale);
        float boxH = h * 0.88f;
        return new PanelLayout((w - boxW) / 2f, (h - boxH) / 2f,
                boxW, boxH, 16f * scale, 26f * scale, scale);
    }

    /**
     * Reports whether a material id represents an equippable weapon.
     *
     * @param materialId material type to test
     * @return true when the id carries the weapon prefix
     */
    private static boolean isWeapon(String materialId) {
        return materialId != null && materialId.startsWith(WEAPON_PREFIX);
    }

    /** Uses the shared potion rule for keyboard and bag actions, retaining feedback for the bag. */
    private void useHealthPotion() {
        inventoryFeedback = session.getPlayer().useHealthPotion() ? "Recovered up to " + ItemCatalog.POTION_HEAL + " HP"
                : "Requires a health potion and a living, injured player";
    }

    /** Applies one selected bag item, shared by keyboard and mouse.
     * @param row visible item index
     */
    private void useBagRow(int row) {
        var entries = visibleEntries(inventory, false);
        if (row < 0 || row >= entries.size()) { return; }
        String id = entries.get(row).materialId();
        if (ItemCatalog.POTION.equals(id)) { useHealthPotion(); }
        else if (ItemCatalog.gear(id).isEmpty()) { inventoryFeedback = prettyName(id) + " cannot be equipped"; }
        else {
            boolean wasWorn = equipment.isEquipped(id);
            equipment.toggle(id);
            inventoryFeedback = (wasWorn ? "Unequipped: " : "Equipped: ") + prettyName(id);
        }
        equippedWeapon = equipment.itemAt(EquipmentSlot.WEAPON);
        saveMenu.notify(inventoryFeedback);
    }

    /** Calculates rows that fit above the panel footer.
     * @param chest whether the two-column chest layout is active
     * @return positive number of visible rows
     */
    private int rowsPerPage(boolean chest) {
        if (chest) {
            ChestLayout p = chestLayout();
            return Math.max(1, (int) ((p.boxH - 2 * p.pad) / p.lineH - 4));
        }
        PanelLayout p = inventoryLayout();
        return Math.max(1, (int) ((p.boxH - 2 * p.pad) / p.lineH - 6));
    }

    /** Counts pages including one page for empty storage.
     * @param chest whether both chest columns participate
     * @return positive page count
     */
    private int pageCount(boolean chest) {
        int count = chest ? Math.max(inventory.distinctTypes(), chestStorage.distinctTypes()) : inventory.distinctTypes();
        return Math.max(1, (count + rowsPerPage(chest) - 1) / rowsPerPage(chest));
    }

    /** Selects identical rows for rendering and mouse hit testing.
     * @param storage bag or chest being displayed
     * @param chest whether the chest layout is active
     * @return immutable slice of the current stack snapshot
     */
    private List<Inventory.ItemStack> visibleEntries(Inventory storage, boolean chest) {
        int page = Math.min(chest ? chestPage : inventoryPage, pageCount(chest) - 1);
        if (chest) { chestPage = page; } else { inventoryPage = page; }
        var entries = storage.entries();
        int from = Math.min(entries.size(), page * rowsPerPage(chest));
        return entries.subList(from, Math.min(entries.size(), from + rowsPerPage(chest)));
    }

    /** Refreshes the window viewport and title; the renderer follows the new spawn on the next draw. */
    private void updateMapView() {
        if (equipment != null) { equippedWeapon = equipment.itemAt(EquipmentSlot.WEAPON); }
        GameMap map = session.getCurrentMap();
        combatStatus = combatDemo.getStatus();
        renderer.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.graphics.setTitle(map.getName() + (session.getPlayer().getDebugOptions().blocksSaving() ? " | DEBUG - SAVING DISABLED" : "") + " | " + combatStatus + " | " + travelStatus);
    }

    /**
     * Updates the view without changing player or world coordinates.
     *
     * @param width new window width in pixels
     * @param height new window height in pixels
     */
    @Override
    public void resize(int width, int height) {
        renderer.resize(width, height);
    }

    /** Suspends simulation and music while the application is paused, retaining playback position. */
    @Override
    public void pause() {
        paused = true;
        music.pause();
    }

    /** Resumes simulation and music without accumulating paused time or resetting the song. */
    @Override
    public void resume() {
        paused = false;
        skipNextUpdate = true;
        music.resume();
    }

    /**
     * Releases rendering resources and all streamed music owned by this screen.
     */
    @Override
    public void dispose() {
        closeDebugInput();
        music.dispose();
        renderer.dispose();
        dialogueBatch.dispose();
        dialogueFont.dispose();
        dialogueShapes.dispose();
    }
}
