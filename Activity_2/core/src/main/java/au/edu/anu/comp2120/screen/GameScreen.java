package au.edu.anu.comp2120.screen;

import au.edu.anu.comp2120.GameSession;
import au.edu.anu.comp2120.EncounterFlow;
import au.edu.anu.comp2120.npc.NPC;
import au.edu.anu.comp2120.player.Player;
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

import java.util.ArrayList;
import java.util.List;

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
        chestStorage = new Inventory();
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
                currentMap.getType() == MapType.HUB ? GameConfig.HUB_NPCS : List.of(),
                travel.getPortals(currentMap.getId()),
                combatDemo.getCombatants(), combatDemo.getActiveAttacks(), combatDemo.getDrops());
        drawDialogue();
        drawCoordinates();
        drawHealth();
        drawInventory();
        drawWeaponSlot();
        drawInteractionPrompt();
        drawMinimap();
        drawChest();


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
                chestOpen = false;
                activeNpc = null;
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
            bigMap = !bigMap;
        }
        if (inventoryOpen && Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
            handleInventoryRightClick();
        }
        if (chestOpen && Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
            handleChestRightClick();
        }
        if (!travelConsumed && Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            if (chestOpen) {
                chestOpen = false;
            } else if (dialogueOpen) {
                advanceDialogue();
            } else {
                NPC near = nearestNpc(session.getPlayer());
                if (near == null) {
                    travelStatus = "";
                } else if (GameConfig.STORAGE_CHEST.getId().equals(near.getId())) {
                    chestOpen = true;
                    dialogueOpen = false;
                    inventoryOpen = false;
                    activeNpc = null;
                } else {
                    dialogueOpen = true;
                    dialogueLine = 0;
                    activeNpc = near;
                }
            }
        }
        boolean frozen = travelConsumed || dialogueOpen || inventoryOpen || bigMap || chestOpen;
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
        if (activeNpc == null || dialogueLine >= dialogueLines().size()) {
            dialogueOpen = false;
            if (activeNpc != null && !activeNpc.hasInteracted()) {
                activeNpc.setInteracted();
                String reward = activeNpc.getRewardItemId();
                if (reward != null && !reward.isBlank()) {
                    inventory.addItem(reward, 1);
                    travelStatus = "Received: " + reward;
                } else {
                    travelStatus = "Talked to: " + activeNpc.getName();
                }
            } else if (activeNpc != null) {
                travelStatus = "Talked to: " + activeNpc.getName();
            }
            updateMapView();
            activeNpc = null;
        }
    }
    /**
     * Finds the closest NPC within interaction range on the Hub map.
     *
     * @param player the player used as the search origin
     * @return the nearest NPC in range, or null when none is close enough
     */
    private NPC nearestNpc(Player player) {
        if (session.getCurrentMap().getType() != MapType.HUB) {
            return null;
        }
        NPC best = null;
        double bestDistance = Double.MAX_VALUE;
        for (NPC npc : GameConfig.HUB_NPCS) {
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
     * Returns the script to show for the active NPC, appending a short
     * reminder when its one-time reward has already been collected.
     *
     * @return the visible dialogue lines, never null
     */
    private List<String> dialogueLines() {
        List<String> base = activeNpc.getLines();
        if (!activeNpc.hasInteracted() || activeNpc.getRewardItemId() == null) {
            return base;
        }
        List<String> withNotice = new ArrayList<>(base);
        withNotice.add("You already took the " + prettyName(activeNpc.getRewardItemId()) + ".");
        return withNotice;
    }
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
        String closeHint = activeNpc.getRewardItemId() == null || activeNpc.hasInteracted()
                ? "[E] Close"
                : "[E] Take reward";
        dialogueFont.draw(dialogueBatch,
                last ? closeHint : "[E] Next", x + pad, y + pad + 4f);
        dialogueBatch.end();
    }
    /** Draws the persistent weapon slot in the top-right corner. */
    private void drawWeaponSlot() {
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        float scale = Math.max(1f, h / 720f) * 1.4f;
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
            dialogueFont.draw(dialogueBatch, "(none)", x + pad, y + pad + 6f);
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
        dialogueFont.draw(dialogueBatch, "INVENTORY", p.x, p.y + p.boxH - p.pad);
        float cursor = p.y + p.boxH - p.pad - p.lineH * 1.6f;
        if (inventory.isEmpty()) {
            dialogueFont.setColor(0.85f, 0.85f, 0.85f, 1);
            dialogueFont.draw(dialogueBatch, "(empty)", p.x + p.pad, cursor);
        } else {
            for (Inventory.ItemStack stack : inventory.entries()) {
                boolean equipped = stack.materialId().equals(equippedWeapon);
                if (equipped) {
                    dialogueFont.setColor(0.40f, 1f, 0.55f, 1);
                } else if (isWeapon(stack.materialId())) {
                    dialogueFont.setColor(0.95f, 0.90f, 0.55f, 1);
                } else {
                    dialogueFont.setColor(0.85f, 0.85f, 0.85f, 1);
                }
                String label = prettyName(stack.materialId()) + "  x" + stack.quantity()
                        + (equipped ? "  [Equipped]" : "");
                dialogueFont.draw(dialogueBatch, label, p.x + p.pad, cursor);
                cursor -= p.lineH;
            }
        }
        dialogueFont.setColor(0.55f, 0.60f, 0.70f, 1);
        dialogueFont.draw(dialogueBatch, "[B] Close   [Right-click] Equip/Unequip",
                p.x + p.pad, p.y + p.pad + 4f);
        dialogueBatch.end();
    }
    /**
     * Draws a minimap in the bottom-right corner, or an enlarged centred map when M is held open.
     * Screen-space only; world geometry is scaled to fit the panel.
     */
    private void drawMinimap() {
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        float ui = Math.max(1f, h / 720f) * 1.4f;
        GameMap map = session.getCurrentMap();
        CollisionBox bounds = map.getBounds();

        float boxW = bigMap ? Math.min(w * 0.8f, 900f * ui) : 200f * ui;
        float boxH = bigMap ? Math.min(h * 0.8f, 600f * ui) : 140f * ui;
        float x = bigMap ? (w - boxW) / 2f : w - boxW - 12f * ui;
        float y = bigMap ? (h - boxH) / 2f : 12f * ui;
        float pad = 7f * ui;
        float s = Math.min((boxW - pad * 2) / (float) bounds.width(),
                (boxH - pad * 2) / (float) bounds.height());
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
        if (hub) {
            for (NPC npc : GameConfig.HUB_NPCS) {
                dialogueShapes.setColor(1f, 0.85f, 0.20f, 1);
                dialogueShapes.rect(ox + (float) (npc.getX() - bounds.x()) * s,
                        oy + (float) (npc.getY() - bounds.y()) * s,
                        Math.max((float) npc.getWidth() * s, 3f),
                        Math.max((float) npc.getHeight() * s, 3f));
            }
        }
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
        dialogueFont.draw(dialogueBatch, bigMap ? "[M] Close" : "[M] Expand",
                x + pad, y + pad + 2f);
        dialogueBatch.end();
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

    /** Recomputes the chest panel geometry from the current window size. */
    private ChestLayout chestLayout() {
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        float scale = Math.max(1f, h / 720f) * 1.4f;
        float boxW = Math.min(w * 0.75f, 760f * scale);
        float boxH = h * 0.6f;
        float pad = 16f * scale;
        float lineH = 26f * scale;
        return new ChestLayout((w - boxW) / 2f, (h - boxH) / 2f,
                boxW, boxH, pad, lineH, scale, (boxW - pad * 3) / 2f);
    }

    /**
     * Finds which item row sits under the cursor within one column.
     *
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

    /** Moves one stack between the inventory and the chest, unequipping weapons on deposit. */
    private void handleChestRightClick() {
        int h = Gdx.graphics.getHeight();
        float mouseX = Gdx.input.getX();
        float mouseY = h - Gdx.input.getY();
        ChestLayout p = chestLayout();
        float firstLine = p.y + p.boxH - p.pad - p.lineH * 1.6f;

        var bag = inventory.entries();
        int row = hitRow(mouseX, mouseY, p.x + p.pad, p.colW, firstLine, p.lineH, bag.size());
        if (row >= 0) {
            Inventory.ItemStack stack = bag.get(row);
            int moved = inventory.removeItem(stack.materialId(), stack.quantity());
            chestStorage.addItem(stack.materialId(), moved);
            if (stack.materialId().equals(equippedWeapon)) {
                equippedWeapon = "";
            }
            travelStatus = "Stored: " + prettyName(stack.materialId()) + " x" + moved;
            updateMapView();
            return;
        }

        var stored = chestStorage.entries();
        row = hitRow(mouseX, mouseY, p.x + p.pad * 2 + p.colW, p.colW, firstLine, p.lineH, stored.size());
        if (row >= 0) {
            Inventory.ItemStack stack = stored.get(row);
            int moved = chestStorage.removeItem(stack.materialId(), stack.quantity());
            inventory.addItem(stack.materialId(), moved);
            travelStatus = "Taken: " + prettyName(stack.materialId()) + " x" + moved;
            updateMapView();
        }
    }

    /** Draws the two-column chest transfer panel while it is open. */
    private void drawChest() {
        if (!chestOpen) {
            return;
        }
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        ChestLayout p = chestLayout();
        float top = p.y + p.boxH - p.pad;
        float rightCol = p.x + p.pad * 2 + p.colW;
        float firstLine = top - p.lineH * 1.6f;

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
        dialogueFont.draw(dialogueBatch, "STORAGE CHEST", p.x + p.pad, top);
        dialogueFont.setColor(0.60f, 0.90f, 1f, 1);
        dialogueFont.draw(dialogueBatch, "INVENTORY", p.x + p.pad, top - p.lineH * 1.2f);
        dialogueFont.setColor(0.95f, 0.80f, 0.40f, 1);
        dialogueFont.draw(dialogueBatch, "CHEST", rightCol, top - p.lineH * 1.2f);

        float cursor = firstLine;
        if (inventory.isEmpty()) {
            dialogueFont.setColor(0.85f, 0.85f, 0.85f, 1);
            dialogueFont.draw(dialogueBatch, "(empty)", p.x + p.pad, cursor);
        } else {
            for (Inventory.ItemStack stack : inventory.entries()) {
                boolean equipped = stack.materialId().equals(equippedWeapon);
                dialogueFont.setColor(equipped ? 0.40f : 0.85f,
                        equipped ? 1f : 0.85f, equipped ? 0.55f : 0.85f, 1);
                dialogueFont.draw(dialogueBatch, prettyName(stack.materialId())
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
            for (Inventory.ItemStack stack : chestStorage.entries()) {
                dialogueFont.setColor(0.95f, 0.90f, 0.55f, 1);
                dialogueFont.draw(dialogueBatch, prettyName(stack.materialId())
                        + "  x" + stack.quantity(), rightCol, cursor);
                cursor -= p.lineH;
            }
        }

        dialogueFont.setColor(0.55f, 0.60f, 0.70f, 1);
        dialogueFont.draw(dialogueBatch, "[Right-click] move item    [E/B] Close",
                p.x + p.pad, p.y + p.pad + 4f);
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
     * Recomputes the inventory panel geometry from the current window size.
     * Shared by drawing and hit testing so both stay in sync.
     *
     * @return the current panel layout
     */
    private PanelLayout inventoryLayout() {
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        float scale = Math.max(1f, h / 720f) * 1.4f;
        float boxW = Math.min(w * 0.5f, 420f * scale);
        float boxH = h * 0.5f;
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

    /**
     * Equips or unequips the weapon row under the mouse cursor.
     * Screen y is flipped because input reports a top-left origin.
     */
    private void handleInventoryRightClick() {
        int h = Gdx.graphics.getHeight();
        float mouseX = Gdx.input.getX();
        float mouseY = h - Gdx.input.getY();
        PanelLayout p = inventoryLayout();
        float firstLine = p.y + p.boxH - p.pad - p.lineH * 1.6f;
        var entries = inventory.entries();
        for (int i = 0; i < entries.size(); i++) {
            float baseline = firstLine - i * p.lineH;
            boolean insideRow = mouseX >= p.x && mouseX <= p.x + p.boxW
                    && mouseY <= baseline && mouseY >= baseline - p.lineH;
            if (!insideRow) {
                continue;
            }
            String id = entries.get(i).materialId();
            if (!isWeapon(id)) {
                travelStatus = prettyName(id) + " cannot be equipped";
            } else if (id.equals(equippedWeapon)) {
                equippedWeapon = "";
                travelStatus = "Unequipped: " + prettyName(id);
            } else {
                equippedWeapon = id;
                travelStatus = "Equipped: " + prettyName(id);
            }
            updateMapView();
            return;
        }
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
