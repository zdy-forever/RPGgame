package au.edu.anu.comp2120.screen;

import au.edu.anu.comp2120.GameSession;
import au.edu.anu.comp2120.EncounterFlow;
import au.edu.anu.comp2120.npc.NPC;
import au.edu.anu.comp2120.world.*;
import au.edu.anu.comp2120.TravelSystem;
import au.edu.anu.comp2120.player.PlayerInput;
import au.edu.anu.comp2120.player.PlayerCommand;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Align;
import au.edu.anu.comp2120.inventory.Inventory;
/**
 * Coordinates the map session, movement input, combat debug adapter, and geometry-only presentation.
 *
 * @author Daoyan Zhu (u7782042)
 * @author Jiaqi Chen (u7851266)
 */
public final class GameScreen extends ScreenAdapter {

    /** Current-map and player coordination, independent of this screen's lifecycle. */
    private final GameSession session;
    /** Portal trigger/unlock state; delegates every map transition to the existing session. */
    private final TravelSystem travel;
    /** Graphics-independent debug adapter; all damage goes through the shared combat pipeline. */
    private final CombatDemo combatDemo;
    /** Stable external acceptance/result/reward boundary; contains no Quest or Inventory logic. */
    private final EncounterFlow encounterFlow;
    /** Last HP/key feedback applied to the native title, avoiding redundant title updates. */
    private String combatStatus = "";
    /** Brief travel feedback shown in the native window title without a font asset. */
    private String travelStatus = "Awaiting external encounter acceptance; task portals closed";
    /** Adapter for the application's keyboard input. */
    private final PlayerInput input;
    /** Geometry-only view owned by this screen. */
    private final PlaceholderRenderer renderer;
    /** Whether lifecycle pause has suspended movement. */
    private boolean paused;
    /** Discards the first frame's time after startup or resume. */
    private boolean skipNextUpdate = true;
    /** Whether an in-screen dialogue is open; freezes movement while true. */
    private boolean dialogueOpen;
    /** Index of the current dialogue line. */
    private int dialogueLine;
    /** NPC currently talking. */
    private NPC activeNpc;
    /** Maximum HP used to scale the placeholder health bar. */
    private static final int MAX_HP = 100;
    /** Own screen-space batch used only for the dialogue box. */
    private final SpriteBatch dialogueBatch;
    /** Own built-in font for dialogue text; no art files required. */
    private final BitmapFont dialogueFont;
    /** Own shape renderer for the dialogue box background and border. */
    private final ShapeRenderer dialogueShapes;
    /** Player item storage owned by this screen; survives map changes. */
    private final Inventory inventory;
    /** Whether the inventory panel is open; freezes movement while true. */
    private boolean inventoryOpen;
    /**
     * Creates the demo models and drawing resources after graphics initialization.
     */
    public GameScreen() {
        session = new GameSession(MapCatalog.createMaps(), MapCatalog.HUB,
                GameConfig.PLAYER_WIDTH, GameConfig.PLAYER_HEIGHT, GameConfig.PLAYER_SPEED);
        var questPortals = TravelCatalog.createQuestPortals();
        travel = new TravelSystem(session, questPortals, TravelCatalog.createDestinations());
        encounterFlow = new EncounterFlow(session, travel, questPortals.stream().map(Portal::id).toList());
        combatDemo = new CombatDemo(session, encounterFlow);
        input = new PlayerInput(Gdx.input);
        renderer = new PlaceholderRenderer(GameConfig.CAMERA_VIEW_WIDTH, GameConfig.CAMERA_VIEW_HEIGHT);
        updateMapView();
        dialogueBatch = new SpriteBatch();
        dialogueFont = new BitmapFont();
        dialogueShapes = new ShapeRenderer();
        inventory = new Inventory();
    }

    /**
     * Gives external Quest/Inventory adapters acceptance, result and one-time reward APIs.
     * All calls must run on the game-loop thread; no NPC or Quest implementation is provided here.
     * Intentionally exposed for external adapter integration, even before a local caller exists.
     * @return this screen's persistent gameplay integration boundary
     */
    @SuppressWarnings("unused") // Public cross-module integration endpoint, not dead implementation code.
    public EncounterFlow getEncounterFlow() {
        return encounterFlow;
    }

    /**
     * Samples input, updates movement/abilities and combat, then checks arrival portals before drawing.
     * Fast travel consumes commands; portal arrival immediately clears old-map attacks and dash.
     * Success/failure recovery consumes the rest of the frame before any Hub movement or portal check.
     * Frame stalls are capped and all simulation timers pause with this screen.
     *
     * @param delta seconds elapsed since the previous frame
     */
    @Override
    public void render(float delta) {
        if (!paused && !skipNextUpdate && Float.isFinite(delta) && delta >= 0) {
            updateGameplay(Math.min(delta, GameConfig.MAX_FRAME_DELTA));
        }
        skipNextUpdate = false;
        GameMap currentMap = session.getCurrentMap();
        renderer.render(currentMap, session.getPlayer(),
                currentMap.getType() == MapType.HUB ? GameConfig.WEAPON_MERCHANT : null,
                travel.getPortals(currentMap.getId()),
                combatDemo.getCombatants(), combatDemo.getActiveAttacks(), combatDemo.getDrops());
        drawDialogue();
        drawCoordinates();
        drawHealth();
        drawInventory();


    }

    /**
     * Arbitrates one frame, settling player death then Boss victory before travel and debug input.
     * A post-combat outcome return skips all remaining portal/input processing in the arrival frame.
     * TravelSystem observes the normal session stamp on its next check and arms arrival protection.
     * @param delta validated capped non-negative simulation seconds
     * @throws IllegalArgumentException if the configured failure Hub/spawn is invalid
     */
    private void updateGameplay(double delta) {
        if (combatDemo.returnToHubIfBattleEnded()) {
            showBattleReturn();
            return;
        }
        boolean travelConsumed;
        try {
            travelConsumed = handleTravelInput();
        } catch (IllegalArgumentException | IllegalStateException failure) {
            travelStatus = failure.getMessage();
            travelConsumed = true;
        }
        long beforeCombat = session.getTransitionVersion();
        if (Gdx.input.isKeyJustPressed(Input.Keys.B)) {
            inventoryOpen = !inventoryOpen;
            if (inventoryOpen) {
                dialogueOpen = false;
                activeNpc = null;
            }
        }
        if (!travelConsumed && Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            if (dialogueOpen) {
                advanceDialogue();
            } else {
                var player = session.getPlayer();
                NPC merchant = GameConfig.WEAPON_MERCHANT;
                if (merchant.isAdjacent(player.getX(), player.getY(), 70.0)) {
                    if (!merchant.hasInteracted()) {
                        dialogueOpen = true;
                        dialogueLine = 0;
                        activeNpc = merchant;
                    } else {
                        travelStatus = "Merchant: \"Come back later, adventurer.\"";
                        updateMapView();
                    }
                }
            }
        }
        boolean frozen = travelConsumed || dialogueOpen || inventoryOpen;
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
                    travelStatus = "Arrived by portal; leave the portal area before re-entry";
                    updateMapView();
                }
            } catch (IllegalArgumentException | IllegalStateException failure) {
                travelStatus = failure.getMessage();
                updateMapView();
            }
        }
        if (!combatStatus.equals(combatDemo.getStatus())) {
            updateMapView();
        }
    }

    /** Updates only placeholder feedback after a completed return; no Quest state is read or changed. */
    private void showBattleReturn() {
        travelStatus = "Challenge " + encounterFlow.getLastResult()
                .filter(result -> result.encounterVersion() == session.getTransitionVersion() - 1)
                .map(result -> result.outcome().name())
                .orElse("FAILURE") + "; returned to Hub with full HP";
        updateMapView();
    }


    /** Advances the dialogue, or closes it and grants the reward on the final line. */
    private void advanceDialogue() {
        dialogueLine++;
        if (activeNpc == null || dialogueLine >= activeNpc.getLines().size()) {
            dialogueOpen = false;
            if (activeNpc != null && !activeNpc.hasInteracted()) {
                activeNpc.setInteracted();
                inventory.addItem(activeNpc.getRewardItemId(), 1);
                travelStatus = "Received: " + activeNpc.getRewardItemId();
                updateMapView();
            }
            activeNpc = null;
        }
    }
    /**
     * Draws the player's HP at the top-left corner, above the coordinate line.
     * Uses this screen's own font and batch, so no renderer change is needed.
     */
    /**
     * Draws the player's health bar (red background, green fill) at the top-left corner,
     * with the numeric value underneath. Uses this screen's own resources.
     */
    /**
     * Draws the player's health bar (red background, green fill) at the top-left corner,
     * with the numeric value centred inside the bar. Uses this screen's own resources.
     */
    private void drawHealth() {
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        float scale = Math.max(1f, h / 720f) * 1.4f;
        float x = 12f * scale;
        float barW = 180f * scale;
        float barH = 18f * scale;
        float barY = h - 10f * scale - barH;
        float ratio = currentHp() / (float) MAX_HP;

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
        dialogueFont.draw(dialogueBatch, currentHpText() + " / " + MAX_HP,
                x, baseline, barW, Align.center, false);
        dialogueBatch.end();
    }
    /**
     * Parses the current HP number from the combat status text.
     *
     * @return the parsed HP clamped to 0..MAX_HP, or MAX_HP when unreadable
     */
    private int currentHp() {
        String segment = currentHpText();
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < segment.length(); i++) {
            char c = segment.charAt(i);
            if (Character.isDigit(c)) {
                digits.append(c);
            } else if (digits.length() > 0) {
                break;
            }
        }
        if (digits.length() == 0) {
            return MAX_HP;
        }
        return Math.max(0, Math.min(Integer.parseInt(digits.toString()), MAX_HP));
    }
    /**
     * Extracts the HP segment from the combat status, for example "HP 100".
     *
     * @return the leading status segment before the first separator
     */
    private String currentHpText() {
        String status = combatDemo.getStatus();
        int bar = status.indexOf('|');
        return (bar > 0 ? status.substring(0, bar) : status).trim();
    }
    /**
     * Draws the live player coordinate in the top-left corner.
     * Uses this screen's own font and batch, so no renderer change is needed.
     */
    private void drawCoordinates() {
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        float scale = Math.max(1f, h / 720f) * 1.4f;
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
    private void drawDialogue() {
        if (!dialogueOpen || activeNpc == null) {
            return;
        }
        var lines = activeNpc.getLines();
        if (dialogueLine >= lines.size()) {
            return;
        }
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        float boxW = w * 0.7f;
        float boxH = h * 0.22f;
        float x = (w - boxW) / 2f;
        float y = 18f;
        float scale = Math.max(1f, h / 720f) * 1.4f;
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

        float pad = 14f * scale;
        boolean last = dialogueLine >= lines.size() - 1;
        dialogueBatch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, w, h));
        dialogueBatch.begin();
        dialogueFont.setColor(1f, 0.90f, 0.40f, 1);
        dialogueFont.draw(dialogueBatch, activeNpc.getName(), x + pad, y + boxH - pad);
        dialogueFont.setColor(1f, 1f, 1f, 1);
        dialogueFont.draw(dialogueBatch, lines.get(dialogueLine),
                x + pad, y + boxH - pad - 26f * scale, boxW - 2 * pad, Align.left, true);
        dialogueFont.setColor(0.60f, 0.90f, 1f, 1);
        dialogueFont.draw(dialogueBatch,
                last ? "[E] Take reward & close" : "[E] Next", x + pad, y + pad + 4f);
        dialogueBatch.end();
    }
    /** Draws the inventory panel in the centre of the screen while it is open. */
    private void drawInventory() {
        if (!inventoryOpen) {
            return;
        }
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        float scale = Math.max(1f, h / 720f) * 1.4f;
        float boxW = Math.min(w * 0.5f, 420f * scale);
        float boxH = h * 0.5f;
        float x = (w - boxW) / 2f;
        float y = (h - boxH) / 2f;
        float pad = 16f * scale;
        float lineH = 26f * scale;

        dialogueShapes.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, w, h));
        dialogueShapes.begin(ShapeRenderer.ShapeType.Filled);
        dialogueShapes.setColor(0.04f, 0.05f, 0.08f, 0.94f);
        dialogueShapes.rect(x, y, boxW, boxH);
        dialogueShapes.end();
        dialogueShapes.begin(ShapeRenderer.ShapeType.Line);
        dialogueShapes.setColor(0.55f, 0.85f, 1f, 1);
        dialogueShapes.rect(x, y, boxW, boxH);
        dialogueShapes.end();

        dialogueFont.getData().setScale(scale);
        dialogueBatch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, w, h));
        dialogueBatch.begin();
        dialogueFont.setColor(0.60f, 0.90f, 1f, 1);
        dialogueFont.draw(dialogueBatch, "INVENTORY", x, y + boxH - pad);
        dialogueFont.setColor(0.85f, 0.85f, 0.85f, 1);
        float cursor = y + boxH - pad - lineH * 1.6f;
        if (inventory.isEmpty()) {
            dialogueFont.draw(dialogueBatch, "(empty)", x + pad, cursor);
        } else {
            for (Inventory.ItemStack stack : inventory.entries()) {
                dialogueFont.draw(dialogueBatch,
                        prettyName(stack.materialId()) + "  x" + stack.quantity(),
                        x + pad, cursor);
                cursor -= lineH;
            }
        }
        dialogueFont.setColor(0.55f, 0.60f, 0.70f, 1);
        dialogueFont.draw(dialogueBatch, "[B] Close", x + pad, y + pad + 4f);
        dialogueBatch.end();
    }

    /** Turns a raw material id such as weapon_lightning_staff into a readable name. */
    private String prettyName(String materialId) {
        String cleaned = materialId.replace("weapon_", "").replace('_', ' ').trim();
        if (cleaned.isEmpty()) {
            return materialId;
        }
        return Character.toUpperCase(cleaned.charAt(0)) + cleaned.substring(1);
    }
    /**
     * Handles fast-travel shortcuts and an explicit temporary debug unlock key.
     *
     * @return true if a travel command consumed this frame's movement update
     * @throws IllegalArgumentException if a configured destination or spawn is invalid
     * @throws IllegalStateException if a selected destination is locked
     */
    private boolean handleTravelInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.U)) {
            travel.unlockDestination(TravelCatalog.TRAINING_ENTRY);
            travel.unlockDestination(TravelCatalog.CROSSROADS_ENTRY);
            travelStatus = "Fast travel unlocked for this session (debug)";
            return true;
        }
        String destination;
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
            destination = TravelCatalog.HUB_ENTRY;
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
            destination = TravelCatalog.TRAINING_ENTRY;
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
            destination = TravelCatalog.CROSSROADS_ENTRY;
        } else {
            return false;
        }
        travel.fastTravel(destination);
        travelStatus = "Fast travel: " + travel.getDestination(destination).name();
        return true;
    }

    /** Refreshes the window viewport and title; the renderer follows the new spawn on the next draw. */
    private void updateMapView() {
        GameMap map = session.getCurrentMap();
        combatStatus = combatDemo.getStatus();
        renderer.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.graphics.setTitle(map.getName() + " | " + combatStatus + " | " + travelStatus);
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

    /** Suspends simulation while the application is paused. */
    @Override
    public void pause() {
        paused = true;
    }

    /** Resumes simulation without accumulating time spent paused. */
    @Override
    public void resume() {
        paused = false;
        skipNextUpdate = true;
    }

    /**
     * Releases geometry rendering resources owned by this screen.
     */
    @Override
    public void dispose() {
        renderer.dispose();
        dialogueBatch.dispose();
        dialogueFont.dispose();
        dialogueShapes.dispose();
    }
}
