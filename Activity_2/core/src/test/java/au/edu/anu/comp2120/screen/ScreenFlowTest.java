package au.edu.anu.comp2120.screen;

import static org.junit.jupiter.api.Assertions.*;

import au.edu.anu.comp2120.GameSession;
import au.edu.anu.comp2120.MainGame;
import au.edu.anu.comp2120.npc.NPC;
import au.edu.anu.comp2120.npc.PortalGuide;
import au.edu.anu.comp2120.inventory.Inventory;
import au.edu.anu.comp2120.inventory.ItemCatalog;
import au.edu.anu.comp2120.inventory.EquipmentSlot;
import au.edu.anu.comp2120.enemy.BossChallenge;
import au.edu.anu.comp2120.world.MapCatalog;
import au.edu.anu.comp2120.world.TravelCatalog;
import au.edu.anu.comp2120.TravelSystem;
import au.edu.anu.comp2120.quest.BossQuests;
import au.edu.anu.comp2120.player.PlayerClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import au.edu.anu.comp2120.save.SaveSlot;
import com.badlogic.gdx.*;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.utils.GdxNativesLoader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;

/**
 * Exercises real screens, assets and input dispatch with recording GPU/audio boundaries.
 * These tests validate application state and rendering commands, not actual pixels or sound.
 * @author OpenAI Codex
 */
@ResourceLock("libgdx-globals")
@ResourceLock("java.lang.System.properties")
final class ScreenFlowTest {
    /** Disposable save/settings root. */ @TempDir Path directory;
    /** Previous process home, restored after each scenario. */ private String previousHome;
    /** Previous LibGDX application. */ private Application previousApp;
    /** Previous input backend. */ private Input previousInput;
    /** Previous graphics backend. */ private Graphics previousGraphics;
    /** Previous file backend. */ private Files previousFiles;
    /** Previous audio backend. */ private Audio previousAudio;
    /** Previous primary GL boundary. */ private GL20 previousGl;
    /** Previous GL20 boundary. */ private GL20 previousGl20;
    /** Real screen-owning game. */ private MainGame game;
    /** Held and edge-triggered keys for the current frame. */ private final Set<Integer> keys = new HashSet<>();
    /** Deferred screen changes, executed after rendering. */ private final List<Runnable> queued = new ArrayList<>();
    /** Backend calls retained for resource and rendering assertions. */ private final List<String> calls = new ArrayList<>();
    /** NPC interaction flags shared by the production catalogue. */ private final Map<NPC, Boolean> npcFlags = new HashMap<>();
    /** Current input processor installed by the debug console. */ private InputProcessor processor;
    /** Logical and backbuffer width. */ private int width = 960;
    /** Logical and backbuffer height. */ private int height = 540;
    /** Pointer x coordinate. */ private int mouseX;
    /** Pointer y from the top edge. */ private int mouseY;
    /** Mouse button edge and held state for one frame. */ private boolean click;
    /** Button associated with the current pointer event. */ private int mouseButton = Input.Buttons.LEFT;
    /** Backend fullscreen state. */ private boolean fullscreen;
    /** Preference values restored after the scenario. */ private int previousVolume;
    /** Original fullscreen preference. */ private boolean previousFullscreen;
    /** Unique fake GPU resource identities. */ private int nextHandle = 1;
    /** Submitted exit requests. */ private int exits;

    /** Creates an isolated screen scenario. */ ScreenFlowTest() { }

    /**
     * Installs only device boundaries; game screens, font parsing and image decoding remain real.
     */
    @BeforeEach void installDevices() {
        previousHome = System.getProperty("user.home");
        previousVolume = GameSettings.musicVolume(); previousFullscreen = GameSettings.fullscreen();
        GameSettings.setFullscreen(false);
        previousApp = Gdx.app; previousInput = Gdx.input; previousGraphics = Gdx.graphics;
        previousFiles = Gdx.files; previousAudio = Gdx.audio; previousGl = Gdx.gl; previousGl20 = Gdx.gl20;
        for (NPC npc : GameConfig.HUB_NPCS) { npcFlags.put(npc, npc.hasInteracted()); }
        npcFlags.put(GameConfig.GUIDE, GameConfig.GUIDE.hasInteracted());
        npcFlags.put(GameConfig.TRAINING_EXIT, GameConfig.TRAINING_EXIT.hasInteracted());
        System.setProperty("user.home", directory.toString());
        GdxNativesLoader.load();
        game = new MainGame();
        Gdx.gl = Gdx.gl20 = proxy(GL20.class, (object, method, args) -> {
            String name = method.getName(); calls.add(name);
            if (name.equals("glGetShaderiv")) { ((IntBuffer) args[2]).put(0, 1); }
            if (name.equals("glGetProgramiv")) {
                int property = (int) args[1];
                ((IntBuffer) args[2]).put(0, property == GL20.GL_LINK_STATUS || property == GL20.GL_VALIDATE_STATUS ? 1 : 0);
            }
            if (name.equals("glGetIntegerv")) { ((IntBuffer) args[1]).put(0, 4096); }
            if (name.startsWith("glCreate") || name.equals("glGenTexture") || name.equals("glGenBuffer")) { return nextHandle++; }
            if (method.getReturnType() == String.class) { return ""; }
            return defaultValue(method.getReturnType());
        });
        Gdx.graphics = proxy(Graphics.class, (object, method, args) -> switch (method.getName()) {
            case "getWidth", "getBackBufferWidth" -> width;
            case "getHeight", "getBackBufferHeight" -> height;
            case "getDeltaTime", "getRawDeltaTime" -> 0f;
            case "getGL20" -> Gdx.gl20;
            case "isFullscreen" -> fullscreen;
            case "setFullscreenMode" -> { fullscreen = true; calls.add("fullscreen"); yield true; }
            case "setWindowedMode" -> { fullscreen = false; calls.add("windowed"); yield true; }
            default -> defaultValue(method.getReturnType());
        });
        Gdx.input = proxy(Input.class, (object, method, args) -> switch (method.getName()) {
            case "isKeyPressed", "isKeyJustPressed" -> keys.contains((int) args[0]);
            case "isButtonPressed", "isButtonJustPressed" -> click && (int) args[0] == mouseButton;
            case "getX" -> mouseX;
            case "getY" -> mouseY;
            case "getInputProcessor" -> processor;
            case "setInputProcessor" -> { processor = (InputProcessor) args[0]; yield null; }
            default -> defaultValue(method.getReturnType());
        });
        Path assets = java.nio.file.Files.isDirectory(Path.of("assets")) ? Path.of("assets") : Path.of("..", "assets");
        Gdx.files = proxy(Files.class, (object, method, args) -> {
            if (method.getName().equals("classpath")) {
                return new FileHandle((String) args[0], Files.FileType.Classpath) { };
            }
            if (method.getName().equals("internal")) { return new FileHandle(assets.resolve((String) args[0]).toFile()); }
            throw new UnsupportedOperationException(method.getName());
        });
        Gdx.audio = proxy(Audio.class, (object, method, args) -> {
            if (!method.getName().equals("newMusic")) { throw new UnsupportedOperationException(method.getName()); }
            assertTrue(((FileHandle) args[0]).exists(), "Music resource must be packaged");
            return proxy(Music.class, (music, operation, parameters) -> {
                calls.add("music:" + operation.getName());
                return defaultValue(operation.getReturnType());
            });
        });
        Gdx.app = proxy(Application.class, (object, method, args) -> switch (method.getName()) {
            case "getType" -> Application.ApplicationType.Desktop;
            case "getApplicationListener" -> game;
            case "getGraphics" -> Gdx.graphics;
            case "getFiles" -> Gdx.files;
            case "postRunnable" -> { queued.add((Runnable) args[0]); yield null; }
            case "exit" -> { exits++; yield null; }
            default -> defaultValue(method.getReturnType());
        });
    }

    /** Releases native buffers and restores every changed global even when assertions fail. */
    @AfterEach void restoreDevices() {
        try {
            if (game != null) { game.dispose(); }
        } finally {
            Gdx.app = previousApp; Gdx.input = previousInput; Gdx.graphics = previousGraphics;
            Gdx.files = previousFiles; Gdx.audio = previousAudio; Gdx.gl = previousGl; Gdx.gl20 = previousGl20;
            if (previousHome != null) { System.setProperty("user.home", previousHome); }
            npcFlags.forEach(NPC::restoreInteracted);
            GameSettings.setMusicVolume(previousVolume); GameSettings.setFullscreen(previousFullscreen);
        }
    }

    /** Title navigation skips unavailable Continue and opens settings only after the frame completes. */
    @Test void titleSettingsAndQuitUseRealInputDispatch() {
        game.create();
        assertInstanceOf(MainMenuScreen.class, game.getScreen());
        frame(0, Input.Keys.DOWN);
        frame(0, Input.Keys.ENTER);
        assertInstanceOf(SettingsScreen.class, game.getScreen());
        frame(0, Input.Keys.DOWN);
        frame(0, Input.Keys.ENTER);
        assertTrue(fullscreen);
        frame(0, Input.Keys.LEFT);
        assertFalse(fullscreen);
        frame(0, Input.Keys.DOWN);
        frame(0, Input.Keys.ENTER);
        assertInstanceOf(MainMenuScreen.class, game.getScreen());
        frame(0, Input.Keys.UP);
        frame(0, Input.Keys.ENTER);
        assertEquals(1, exits);
        assertTrue(calls.contains("glDrawElements"), "UI should submit geometry");
    }

    /** A real rendered bag/map/pause menu consumes movement until it is closed.
     * @throws Exception if the inspected screen state changes
     */
    @Test void gameplayPanelsPauseMovementAndRenderAssets() throws Exception {
        game.setScreen(new GameScreen());
        frame(0);
        GameSession session = field(game.getScreen(), "session", GameSession.class);
        for (int key : new int[] {Input.Keys.B, Input.Keys.M, Input.Keys.ESCAPE}) {
            var before = session.getPlayer().getBounds();
            frame(0.1f, key);
            frame(0.1f, Input.Keys.D);
            assertEquals(before, session.getPlayer().getBounds());
            frame(0, Input.Keys.ESCAPE);
        }
        double before = session.getPlayer().getX();
        frame(0.1f, Input.Keys.D);
        assertTrue(session.getPlayer().getX() > before);
        assertTrue(calls.contains("glTexImage2D"), "Real decoded assets should reach the upload boundary");
        assertTrue(calls.contains("music:play"));
    }

    /**
     * Bag actions equip, unequip and consume only eligible potions at several window sizes.
     * @param screenWidth window width
     * @param screenHeight window height
     * @throws Exception if inspected screen state changes
     */
    @ParameterizedTest @CsvSource({"960,540", "480,270", "1280,720"})
    void bagActionsAndPaginationPreserveItems(int screenWidth, int screenHeight) throws Exception {
        width = screenWidth; height = screenHeight;
        GameScreen screen = startGameplay();
        var player = state(screen).getPlayer(); var bag = player.getInventory();
        bag.addItem("guardian_hood"); bag.addItem(ItemCatalog.POTION, 2); bag.addItem("ore", 3);
        frame(0, Input.Keys.B); frame(0, Input.Keys.ENTER);
        assertEquals("guardian_hood", player.getEquipment().itemAt(EquipmentSlot.HEAD));
        frame(0, Input.Keys.ENTER);
        assertEquals("", player.getEquipment().itemAt(EquipmentSlot.HEAD));
        player.takeDamage(50);
        frame(0, Input.Keys.DOWN); frame(0, Input.Keys.ENTER);
        assertEquals(80, player.getHealth().getCurrentHp());
        assertEquals(1, bag.quantityOf(ItemCatalog.POTION));
        frame(0, Input.Keys.DOWN); frame(0, Input.Keys.ENTER);
        assertEquals(3, bag.quantityOf("ore"));
        for (int i = 0; i < 30; i++) { bag.addItem("material-" + i); }
        frame(0, Input.Keys.R);
        assertEquals(1, field(screen, "inventoryPage", Integer.class));
        frame(0, Input.Keys.Q);
        assertEquals(0, field(screen, "inventoryPage", Integer.class));
        frame(0, Input.Keys.ESCAPE);
        assertFalse(field(screen, "inventoryOpen", Boolean.class));
    }

    /**
     * Chest transfers move whole stacks without loss, while destination overflow preserves both stores.
     * @throws Exception if inspected screen state changes
     */
    @Test void chestTransfersAndOverflowUseRealMenu() throws Exception {
        GameScreen screen = startGameplay();
        var player = state(screen).getPlayer(); var bag = player.getInventory();
        var chest = field(screen, "chestStorage", Inventory.class);
        chest.clear(); bag.addItem("ore", 7);
        approach(screen, GameConfig.STORAGE_CHEST); frame(0, Input.Keys.E);
        assertTrue(field(screen, "chestOpen", Boolean.class));
        frame(0, Input.Keys.ENTER);
        assertEquals(0, bag.quantityOf("ore")); assertEquals(7, chest.quantityOf("ore"));
        frame(0, Input.Keys.RIGHT); frame(0, Input.Keys.ENTER);
        assertEquals(7, bag.quantityOf("ore")); assertEquals(0, chest.quantityOf("ore"));
        chest.addItem("ore", Integer.MAX_VALUE);
        frame(0, Input.Keys.LEFT); frame(0, Input.Keys.ENTER);
        assertEquals(7, bag.quantityOf("ore")); assertEquals(Integer.MAX_VALUE, chest.quantityOf("ore"));
        frame(0, Input.Keys.ESCAPE);
        assertFalse(field(screen, "chestOpen", Boolean.class));
    }

    /**
     * Merchant grants one weapon and upgrades only after a second explicit confirmation.
     * @throws Exception if inspected screen state changes
     */
    @Test void merchantUpgradeCanBeCancelledAndConfirmed() throws Exception {
        GameScreen screen = startGameplay();
        var bag = state(screen).getPlayer().getInventory();
        approach(screen, GameConfig.WEAPON_MERCHANT); frame(0, Input.Keys.E);
        frame(0, Input.Keys.ENTER); frame(0, Input.Keys.ENTER);
        assertEquals(1, bag.quantityOf("weapon_lightning_staff"));
        bag.addItem("sentinel-core", 2);
        frame(0, Input.Keys.UP); frame(0, Input.Keys.ENTER);
        assertTrue(field(screen, "upgradeMenu", Boolean.class));
        frame(0, Input.Keys.ENTER); frame(0, Input.Keys.ESCAPE);
        assertEquals(2, bag.quantityOf("sentinel-core"));
        assertEquals(1, bag.quantityOf("weapon_lightning_staff"));
        frame(0, Input.Keys.ENTER); frame(0, Input.Keys.ENTER);
        assertEquals(1, bag.quantityOf("sentinel-core"));
        assertEquals(1, bag.quantityOf("weapon_lightning_staff+1"));
        frame(0, Input.Keys.ENTER); frame(0, Input.Keys.ENTER);
        assertEquals(1, bag.quantityOf("weapon_lightning_staff+2"));
        frame(0, Input.Keys.ENTER);
        assertEquals("", field(screen, "pendingUpgrade", String.class));
        frame(0, Input.Keys.ESCAPE); frame(0, Input.Keys.ESCAPE);
        assertFalse(field(screen, "dialogueOpen", Boolean.class));
    }

    /**
     * NPC input chooses a class, enters Training through the portal and returns through the exit NPC.
     * @throws Exception if inspected screen state changes
     */
    @Test void trainerAndTrainingRoutePreserveChosenClass() throws Exception {
        GameScreen screen = startGameplay();
        approach(screen, GameConfig.CLASS_MASTER); frame(0, Input.Keys.E);
        for (int guard = 0; field(screen, "trainerChoice", Integer.class) != 1 && guard < 5; guard++) { frame(0, Input.Keys.DOWN); }
        frame(0, Input.Keys.ENTER);
        assertEquals(PlayerClass.MAGE, state(screen).getPlayer().getPlayerClass());
        frame(0, Input.Keys.ESCAPE);
        selectPortal(screen, PortalGuide.Destination.TRAINING);
        enterPortal(screen);
        assertEquals(MapCatalog.TRAINING, state(screen).getCurrentMap().getId());
        frame(0.1f, Input.Keys.J); frame(0.1f); frame(0, Input.Keys.R);
        assertTrue(field(screen, "combatDemo", CombatDemo.class).getTrainingDummy().isPresent());
        approach(screen, GameConfig.TRAINING_EXIT); frame(0, Input.Keys.E);
        frame(0, Input.Keys.ENTER);
        assertEquals(MapCatalog.HUB, state(screen).getCurrentMap().getId());
        assertEquals(PlayerClass.MAGE, state(screen).getPlayer().getPlayerClass());
    }

    /**
     * Five fixture victories exercise screen settlement, autosave, Hall ending and Credits navigation.
     * Boss HP is arranged directly here; model-level combat tests separately verify real attack victories.
     * @throws Exception if inspected screen state or isolated save files change unexpectedly
     */
    @Test void campaignSettlementUnlocksHallEndingAndCredits() throws Exception {
        GameScreen screen = startGameplay();
        var quests = field(screen, "quests", BossQuests.class);
        for (BossChallenge challenge : BossChallenge.values()) {
            approach(screen, GameConfig.QUEST_KEEPER); frame(0, Input.Keys.E);
            for (int guard = 0; field(screen, "questChoice", Integer.class) != challenge.ordinal() && guard < 5; guard++) {
                frame(0, Input.Keys.DOWN);
            }
            frame(0, Input.Keys.ENTER); frame(0, Input.Keys.ESCAPE);
            selectPortal(screen, PortalGuide.Destination.BATTLE); enterPortal(screen);
            assertEquals(challenge.mapId(), state(screen).getCurrentMap().getId());
            var demo = field(screen, "combatDemo", CombatDemo.class);
            frame(0.1f);
            demo.getBoss().orElseThrow().getHealth().takeDamage(Integer.MAX_VALUE);
            frame(0);
            assertEquals(MapCatalog.HUB, state(screen).getCurrentMap().getId());
            assertTrue(quests.getCompleted().contains(challenge));
            assertEquals(1, screen.getEncounterFlow().getSuccessCount(challenge.mapId()));
        }
        assertEquals(5, quests.getCompleted().size());
        finishConversation(screen, GameConfig.HUB_GUIDE);
        assertTrue(quests.isMemorialPathRevealed());
        selectPortal(screen, PortalGuide.Destination.MEMORIAL); enterPortal(screen);
        approach(screen, GameConfig.GUIDE); frame(0, Input.Keys.E);
        assertTrue(field(screen, "endingOpen", Boolean.class));
        for (int i = 0; i < BossQuests.ENDING_LINES.size(); i++) { frame(0, Input.Keys.ENTER); }
        var credits = field(screen, "credits", CreditsDialogue.class);
        assertTrue(credits.isOpen());
        clickChoice(screen, 0, Input.Buttons.RIGHT);
        for (String page : CreditsDialogue.PAGES) {
            assertEquals(page, credits.text());
            frame(0, Input.Keys.ENTER);
        }
        assertFalse(credits.isOpen());
        assertTrue(quests.hasEndingStarted());
        assertEquals(5, quests.getCompleted().size());
        finishConversation(screen, GameConfig.GUIDE);
        assertTrue(credits.isOpen(), "The completed Sage should offer Credits again");
        frame(0, Input.Keys.ESCAPE);
        assertFalse(credits.isOpen());
    }

    /**
     * Left-click selects a stack, right-click activates it, and outside clicks cannot consume items.
     * Footer controls paginate and close the real bag without leaking input into movement.
     * @throws Exception if inspected state changes
     */
    @Test void mouseBagSelectionActivationAndFooter() throws Exception {
        GameScreen screen = startGameplay();
        var player = state(screen).getPlayer(); var bag = player.getInventory();
        bag.addItem("guardian_hood"); bag.addItem(ItemCatalog.POTION, 2);
        player.takeDamage(50);
        frame(0, Input.Keys.B);
        // Interior points in the rendered 960 x 540 bag: rows at y=435 and y=411.
        clickAt(210, 411, Input.Buttons.LEFT);
        assertEquals(1, field(screen, "bagRow", Integer.class));
        assertEquals(2, bag.quantityOf(ItemCatalog.POTION));
        clickAt(1, 411, Input.Buttons.RIGHT);
        assertEquals(2, bag.quantityOf(ItemCatalog.POTION));
        clickAt(210, 411, Input.Buttons.RIGHT);
        assertEquals(80, player.getHealth().getCurrentHp());
        clickAt(210, 435, Input.Buttons.RIGHT);
        assertEquals("guardian_hood", player.getEquipment().itemAt(EquipmentSlot.HEAD));
        clickAt(550, 40, Input.Buttons.LEFT); // Use selected equipment again.
        assertEquals("", player.getEquipment().itemAt(EquipmentSlot.HEAD));
        for (int i = 0; i < 30; i++) { bag.addItem("material-" + i); }
        clickAt(400, 40, Input.Buttons.LEFT);
        assertEquals(1, field(screen, "inventoryPage", Integer.class));
        clickAt(250, 40, Input.Buttons.LEFT);
        assertEquals(0, field(screen, "inventoryPage", Integer.class));
        clickAt(700, 40, Input.Buttons.LEFT);
        assertFalse(field(screen, "inventoryOpen", Boolean.class));
        frame(0, Input.Keys.P);
        assertEquals(100, player.getHealth().getCurrentHp());
        assertEquals(0, bag.quantityOf(ItemCatalog.POTION));
    }

    /**
     * Mouse chest selection, transfer, paging and closing preserve exact stack quantities.
     * @throws Exception if inspected state changes
     */
    @Test void mouseChestTransfersBothDirections() throws Exception {
        GameScreen screen = startGameplay();
        var bag = state(screen).getPlayer().getInventory();
        var chest = field(screen, "chestStorage", Inventory.class);
        chest.clear(); bag.addItem("ore", 7);
        approach(screen, GameConfig.STORAGE_CHEST); frame(0, Input.Keys.E);
        clickAt(130, 400, Input.Buttons.LEFT);
        assertEquals(7, bag.quantityOf("ore"));
        clickAt(130, 400, Input.Buttons.RIGHT);
        assertEquals(0, bag.quantityOf("ore")); assertEquals(7, chest.quantityOf("ore"));
        clickAt(530, 400, Input.Buttons.LEFT);
        assertTrue(field(screen, "storageRight", Boolean.class));
        clickAt(570, 60, Input.Buttons.LEFT);
        assertEquals(7, bag.quantityOf("ore")); assertEquals(0, chest.quantityOf("ore"));
        clickAt(1, 400, Input.Buttons.RIGHT);
        assertEquals(7, bag.quantityOf("ore"));
        for (int i = 0; i < 30; i++) { bag.addItem("material-" + i); }
        clickAt(390, 60, Input.Buttons.LEFT);
        assertEquals(1, field(screen, "chestPage", Integer.class));
        clickAt(220, 60, Input.Buttons.LEFT);
        assertEquals(0, field(screen, "chestPage", Integer.class));
        clickAt(760, 60, Input.Buttons.LEFT);
        assertFalse(field(screen, "chestOpen", Boolean.class));
    }

    /**
     * Rendered dialogue hit boxes select classes and routes; unavailable Battle is rejected safely.
     * @throws Exception if inspected dialogue state changes
     */
    @Test void mouseDialogueChoicesAndBackAreModal() throws Exception {
        GameScreen screen = startGameplay();
        approach(screen, GameConfig.CLASS_MASTER); frame(0, Input.Keys.E);
        clickChoice(screen, 2, Input.Buttons.LEFT);
        assertEquals(2, field(screen, "trainerChoice", Integer.class));
        clickChoice(screen, 2, Input.Buttons.RIGHT);
        assertEquals(PlayerClass.KATANA, state(screen).getPlayer().getPlayerClass());
        clickDialogueFooter(screen, true);
        assertFalse(field(screen, "dialogueOpen", Boolean.class));
        approach(screen, GameConfig.PORTAL_GUIDE); frame(0, Input.Keys.E);
        clickChoice(screen, PortalGuide.Destination.BATTLE.ordinal(), Input.Buttons.RIGHT);
        assertTrue(field(screen, "dialogueOpen", Boolean.class), "Battle requires an accepted quest");
        assertEquals(MapCatalog.HUB, state(screen).getCurrentMap().getId());
        clickChoice(screen, PortalGuide.Destination.TRAINING.ordinal(), Input.Buttons.LEFT);
        clickDialogueFooter(screen, false);
        assertFalse(field(screen, "dialogueOpen", Boolean.class));
        enterPortal(screen);
        assertEquals(MapCatalog.TRAINING, state(screen).getCurrentMap().getId());
    }

    /**
     * Completing and repeating Hub guidance records interaction without duplicating inventory rewards.
     * @throws Exception if inspected state changes
     */
    @Test void ordinaryDialogueCompletesAndRepeatsWithoutRewards() throws Exception {
        GameScreen screen = startGameplay();
        var bag = state(screen).getPlayer().getInventory();
        bag.addItem("ore", 3);
        finishConversation(screen, GameConfig.HUB_GUIDE);
        assertTrue(GameConfig.HUB_GUIDE.hasInteracted());
        finishConversation(screen, GameConfig.HUB_GUIDE);
        finishConversation(screen, GameConfig.NOTICE_BOARD);
        assertTrue(GameConfig.NOTICE_BOARD.hasInteracted());
        assertEquals(3, bag.quantityOf("ore"));
        assertFalse(field(screen, "dialogueOpen", Boolean.class));
    }

    /**
     * The title's disabled Continue ignores pointer activation; New Game creates a playable session.
     * @throws Exception if inspected state changes
     */
    @Test void titlePointerStartsFreshGameAndDirectLoadRestoresSave() throws Exception {
        game.create();
        var layout = SaveMenu.layout(width, height);
        clickAt(layout.x() + 100 * layout.scale(), layout.y() + 320 * layout.scale(), Input.Buttons.LEFT);
        assertInstanceOf(MainMenuScreen.class, game.getScreen());
        clickAt(layout.x() + 100 * layout.scale(), layout.y() + 375 * layout.scale(), Input.Buttons.LEFT);
        assertInstanceOf(GameScreen.class, game.getScreen());
        frame(0);
        GameScreen screen = (GameScreen) game.getScreen();
        frame(0, Input.Keys.F5);
        state(screen).getPlayer().takeDamage(40);
        screen.loadFrom(null);
        assertEquals(60, state(screen).getPlayer().getHealth().getCurrentHp());
        screen.loadFrom(SaveSlot.MANUAL);
        assertEquals(100, state(screen).getPlayer().getHealth().getCurrentHp());
    }

    /**
     * Settings input updates both persistence and the live audio/display boundaries.
     * @throws Exception if inspected state changes
     */
    @Test void titleAndPauseSettingsApplyDeviceChanges() throws Exception {
        GameSettings.setFullscreen(true); game.create(); assertTrue(fullscreen);
        frame(0, Input.Keys.DOWN); frame(0, Input.Keys.ENTER);
        GameSettings.setMusicVolume(50);
        frame(0, Input.Keys.RIGHT); assertEquals(52, GameSettings.musicVolume());
        var layout = SaveMenu.layout(width, height);
        clickAt(layout.x() + 32 * layout.scale(), layout.y() + 370 * layout.scale(), Input.Buttons.LEFT);
        assertEquals(0, GameSettings.musicVolume());
        clickAt(layout.x() + 200 * layout.scale(), layout.y() + 320 * layout.scale(), Input.Buttons.LEFT);
        assertFalse(fullscreen);
        clickAt(layout.x() + 200 * layout.scale(), layout.y() + 260 * layout.scale(), Input.Buttons.LEFT);
        assertInstanceOf(MainMenuScreen.class, game.getScreen());
        frame(0, Input.Keys.ENTER); frame(0);
        frame(0, Input.Keys.ESCAPE);
        for (int i = 0; i < 4; i++) { frame(0, Input.Keys.DOWN); }
        frame(0, Input.Keys.ENTER); frame(0, Input.Keys.RIGHT);
        assertEquals(2, GameSettings.musicVolume());
        assertTrue(calls.contains("music:setVolume"));
        frame(0, Input.Keys.DOWN); frame(0, Input.Keys.ENTER);
        assertTrue(fullscreen);
        frame(0, Input.Keys.ESCAPE); frame(0, Input.Keys.ESCAPE);
        assertFalse(field(game.getScreen(), "saveMenu", SaveMenu.class).isOpen());
    }

    /**
     * All six class/mode combinations execute attacks in rendered Training without a native window.
     * Real ability timers, projectile/field damage and asset decoding are exercised together.
     * @param type chosen profession
     * @param modeB whether to select the second ultimate
     * @throws Exception if inspected gameplay state changes
     */
    @ParameterizedTest @CsvSource({"ARCHER,false", "ARCHER,true", "MAGE,false", "MAGE,true", "KATANA,false", "KATANA,true"})
    void renderedClassCombatDamagesTrainingTarget(PlayerClass type, boolean modeB) throws Exception {
        GameScreen screen = startGameplay();
        approach(screen, GameConfig.CLASS_MASTER); frame(0, Input.Keys.E);
        clickChoice(screen, type.ordinal(), Input.Buttons.RIGHT);
        clickChoice(screen, modeB ? 4 : 3, Input.Buttons.RIGHT);
        frame(0, Input.Keys.ESCAPE);
        assertEquals(type, state(screen).getPlayer().getPlayerClass());
        selectPortal(screen, PortalGuide.Destination.TRAINING); enterPortal(screen);
        var demo = field(screen, "combatDemo", CombatDemo.class);
        var dummy = demo.getTrainingDummy().orElseThrow();
        var player = state(screen).getPlayer();
        var targetBounds = dummy.getCombatant().getHurtbox();
        player.enterArea(state(screen).getCurrentMap(), targetBounds.x() - 45, targetBounds.y());
        frame(0, Input.Keys.D);
        for (int i = 0; i < 50; i++) {
            frame(0.1f, Input.Keys.J, i == 5 ? Input.Keys.O : Input.Keys.UNKNOWN);
        }
        assertTrue(dummy.getTotalDamage() > 0, "Real attacks should reach the practice target");
        assertTrue(dummy.isAlive(), "Practice damage cannot kill the immortal target");
        frame(0, Input.Keys.K); frame(0.1f); frame(0, Input.Keys.L); frame(0.1f);
        assertTrue(calls.contains("glDrawElements"));
    }

    /**
     * Saving, cancelling overwrite, loading and restarting preserve the existing save file.
     * @throws Exception if inspected screen state or isolated save IO fails
     */
    @Test void saveLoadAndRestartUseRealPauseMenu() throws Exception {
        GameScreen screen = startGameplay();
        frame(0, Input.Keys.F5);
        Path save = directory.resolve(".comp2120-game/saves").resolve(SaveSlot.MANUAL.fileName());
        byte[] before = java.nio.file.Files.readAllBytes(save);
        state(screen).getPlayer().takeDamage(30);
        frame(0, Input.Keys.F5); frame(0, Input.Keys.ESCAPE); frame(0, Input.Keys.ESCAPE);
        assertArrayEquals(before, java.nio.file.Files.readAllBytes(save));
        frame(0, Input.Keys.F9); frame(0, Input.Keys.NUM_2); frame(0, Input.Keys.ENTER);
        assertEquals(100, state(screen).getPlayer().getHealth().getCurrentHp());
        frame(0, Input.Keys.ESCAPE);
        for (int i = 0; i < 3; i++) { frame(0, Input.Keys.DOWN); }
        frame(0, Input.Keys.ENTER); frame(0, Input.Keys.ENTER);
        assertNotSame(screen, game.getScreen());
        assertArrayEquals(before, java.nio.file.Files.readAllBytes(save));
        frame(0);
        assertEquals(MapCatalog.HUB, state((GameScreen) game.getScreen()).getCurrentMap().getId());
    }

    /**
     * Pause/resume consumes the first resumed frame and the debug console owns movement while open.
     * @throws Exception if inspected screen state changes
     */
    @Test void lifecycleAndConsoleDoNotLeakMovement() throws Exception {
        GameScreen screen = startGameplay();
        var player = state(screen).getPlayer(); var before = player.getBounds();
        screen.pause(); frame(0.1f, Input.Keys.D); assertEquals(before, player.getBounds());
        screen.resume(); frame(0.1f, Input.Keys.D); assertEquals(before, player.getBounds());
        frame(0, Input.Keys.F10); assertNotNull(processor);
        frame(0.1f, Input.Keys.D); assertEquals(before, player.getBounds());
        for (char letter : "status".toCharArray()) { processor.keyTyped(letter); }
        frame(0, Input.Keys.ENTER); frame(0, Input.Keys.ESCAPE);
        assertNull(processor);
        frame(0.1f, Input.Keys.D); assertTrue(player.getX() > before.x());
        assertTrue(calls.contains("music:pause"));
    }

    /**
     * Constructs and renders the real gameplay screen before sending input.
     * @return screen bound to the test application
     */
    private GameScreen startGameplay() {
        GameScreen screen = new GameScreen(); game.setScreen(screen); frame(0); return screen;
    }

    /**
     * Reads the persistent session only for setup and assertions.
     * @param screen screen under test
     * @return existing session
     * @throws ReflectiveOperationException if its field contract changes
     */
    private GameSession state(GameScreen screen) throws ReflectiveOperationException {
        return field(screen, "session", GameSession.class);
    }

    /**
     * Arranges proximity without bypassing the actual interaction key handling.
     * @param screen screen under test
     * @param npc intended interaction target
     * @throws ReflectiveOperationException if its field contract changes
     */
    private void approach(GameScreen screen, NPC npc) throws ReflectiveOperationException {
        var session = state(screen);
        session.getPlayer().enterArea(session.getCurrentMap(), npc.getX(), Math.max(0, npc.getY() - 35));
        frame(0);
    }

    /**
     * Advances a bounded real conversation, stopping when Credits replaces ordinary dialogue.
     * @param screen screen under test
     * @param npc conversation target
     * @throws Exception if state cannot be inspected
     */
    private void finishConversation(GameScreen screen, NPC npc) throws Exception {
        approach(screen, npc); frame(0, Input.Keys.E);
        for (int i = 0; i < 30 && field(screen, "dialogueOpen", Boolean.class)
                && !field(screen, "credits", CreditsDialogue.class).isOpen(); i++) {
            frame(0, Input.Keys.ENTER);
        }
        assertTrue(!field(screen, "dialogueOpen", Boolean.class)
                || field(screen, "credits", CreditsDialogue.class).isOpen());
    }

    /**
     * Clicks the centre of an option's last rendered hit box.
     * @param screen screen under test
     * @param choice option index
     * @param button left selects, right confirms
     * @throws Exception if the rendered hit state changes
     */
    private void clickChoice(GameScreen screen, int choice, int button) throws Exception {
        var layout = field(screen, "hitLayout", DialogueLayout.class);
        List<?> hits = field(screen, "dialogueHits", List.class);
        Object hit = hits.get(choice);
        float y = (field(hit, "top", Float.class) + field(hit, "bottom", Float.class)) / 2;
        clickAt(layout.x() + layout.padding() + 20, y, button);
    }

    /**
     * Uses the rendered dialogue footer's confirm/back half.
     * @param screen screen under test
     * @param back whether to cancel rather than confirm
     * @throws Exception if the rendered layout changes
     */
    private void clickDialogueFooter(GameScreen screen, boolean back) throws Exception {
        var layout = field(screen, "hitLayout", DialogueLayout.class);
        clickAt(layout.x() + layout.padding() + layout.textWidth() * (back ? 0.75f : 0.25f),
                layout.hintTop() - layout.hintHeight() / 2, Input.Buttons.LEFT);
    }

    /**
     * Sends one pointer event through the real screen's public frame.
     * @param x horizontal screen coordinate
     * @param y vertical coordinate measured from the bottom
     * @param button button to activate
     */
    private void clickAt(float x, float y, int button) {
        mouseX = Math.round(x); mouseY = height - Math.round(y); mouseButton = button; click = true;
        frame(0);
    }

    /**
     * Selects a route through the Portal Guide's rendered dialogue.
     * @param screen screen under test
     * @param destination desired route
     * @throws Exception if fixture state cannot be inspected
     */
    private void selectPortal(GameScreen screen, PortalGuide.Destination destination) throws Exception {
        approach(screen, GameConfig.PORTAL_GUIDE); frame(0, Input.Keys.E);
        for (int guard = 0; field(screen, "portalChoice", PortalGuide.Destination.class) != destination && guard < 3; guard++) {
            frame(0, Input.Keys.DOWN);
        }
        frame(0, Input.Keys.ENTER);
        assertFalse(field(screen, "dialogueOpen", Boolean.class));
    }

    /**
     * Arranges the player on the selected trigger, then lets the public frame perform validated travel.
     * @param screen screen under test
     * @throws Exception if fixture state cannot be inspected
     */
    private void enterPortal(GameScreen screen) throws Exception {
        var session = state(screen);
        var trigger = field(screen, "travel", TravelSystem.class).getPortal(TravelCatalog.QUEST_PORTAL_ID).trigger();
        session.getPlayer().enterArea(session.getCurrentMap(), trigger.x(), trigger.y());
        frame(0); frame(0);
    }

    /**
     * Drives one public render frame and then processes deferred screen transitions.
     * @param delta elapsed simulation seconds
     * @param pressed held/edge keys for this frame
     */
    private void frame(float delta, int... pressed) {
        keys.clear(); for (int key : pressed) { keys.add(key); }
        game.getScreen().render(delta);
        keys.clear(); click = false;
        List<Runnable> pending = List.copyOf(queued); queued.clear(); pending.forEach(Runnable::run);
    }

    /**
     * Reads fixture state for assertions without replacing production behavior.
     * @param owner production object
     * @param name field name
     * @param type expected field type
     * @param <T> state type
     * @return existing state instance
     * @throws ReflectiveOperationException if the inspected field contract changes
     */
    private static <T> T field(Object owner, String name, Class<T> type) throws ReflectiveOperationException {
        var value = owner.getClass().getDeclaredField(name); value.setAccessible(true);
        return type.cast(value.get(owner));
    }

    /**
     * Constructs a recording interface boundary.
     * @param type backend interface
     * @param handler device behavior
     * @param <T> backend type
     * @return recording backend
     */
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler));
    }

    /**
     * Supplies unused backend return values; no gameplay decision uses this fallback.
     * @param type declared return type
     * @return zero, false or null
     */
    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) { return false; }
        if (type == int.class) { return 0; }
        if (type == long.class) { return 0L; }
        if (type == float.class) { return 0f; }
        if (type == double.class) { return 0d; }
        return null;
    }
}
