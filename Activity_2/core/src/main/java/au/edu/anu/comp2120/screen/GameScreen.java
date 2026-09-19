package au.edu.anu.comp2120.screen;

import au.edu.anu.comp2120.GameSession;
import au.edu.anu.comp2120.audio.BackgroundMusic;
import au.edu.anu.comp2120.EncounterFlow;
import au.edu.anu.comp2120.npc.NPC;
import au.edu.anu.comp2120.npc.PortalGuide;
import au.edu.anu.comp2120.npc.ClassTrainer;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Coordinates the map session, input, combat adapter and read-only art presentation.
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
        var questPortals = TravelCatalog.createGameplayPortals();
        travel = new TravelSystem(session, questPortals, TravelCatalog.createDestinations());
        encounterFlow = new EncounterFlow(session, travel, List.of(TravelCatalog.QUEST_PORTAL_ID));
        quests = new BossQuests(session, encounterFlow);
        music = new BackgroundMusic();
        portalGuide = new PortalGuide(session, travel, encounterFlow);
        combatDemo = new CombatDemo(session, encounterFlow);
        classTrainer = new ClassTrainer(session, combatDemo.getAbilities());
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
        quests.synchronize();
        music.update(session.getCurrentMap().getType(), quests.getCompleted().size() == BossChallenge.values().length);
        GameMap currentMap = session.getCurrentMap();
        renderer.render(currentMap, session.getPlayer(),
                currentNpcs(),
                travel.getPortals(currentMap.getId()),
                combatDemo, paused || dialogueOpen || inventoryOpen || chestOpen || bigMap
                        || !Float.isFinite(delta) ? 0 : Math.max(0, Math.min(delta, GameConfig.MAX_FRAME_DELTA)),
                combatDemo.getDrops(), quests.getCompleted());
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        drawCoordinates(hud);
        drawHealth(hud);
        drawTrainingScore();
        drawInventory();
        drawWeaponSlot(hud);
        drawInteractionPrompt();
        drawMinimap(hud);
        drawChest();
        drawSkillHud(hud);
        drawDialogue();


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
                Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        dialogueBatch.end();
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
        if (endingOpen) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                dialogueLine++;
                if (dialogueLine >= BossQuests.ENDING_LINES.size()) {
                    endingOpen = false;
                    dialogueOpen = false;
                    activeNpc = null;
                    travelStatus = "All five guardians defeated - journey complete";
                }
            }
            updateMapView();
            return;
        }
        if (isQuestDialogueOpen()) {
            handleQuestDialogueInput();
            combatDemo.update(delta, PlayerCommand.NONE, false);
            updateMapView();
            return;
        }
        if (isPortalDialogueOpen()) {
            handlePortalDialogueInput();
            combatDemo.update(delta, PlayerCommand.NONE, false);
            updateMapView();
            return;
        }
        if (isTrainerDialogueOpen()) {
            handleTrainerDialogueInput();
            combatDemo.update(delta, PlayerCommand.NONE, false);
            updateMapView();
            return;
        }
        if (isTrainingExitDialogueOpen()) {
            handleTrainingExitInput();
            combatDemo.update(delta, PlayerCommand.NONE, false);
            updateMapView();
            return;
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
        if (session.getCurrentMap().getType() != MapType.MEMORIAL
                && HudLayout.forWindow(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()).detailsVisible()
                && Gdx.input.isKeyJustPressed(Input.Keys.M)) {
            bigMap = !bigMap;
        }
        if (inventoryOpen && Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
            handleInventoryRightClick();
        }
        if (chestOpen && Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
            handleChestRightClick();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
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
                        questFeedback = "E accepts the highlighted unfinished Boss; Esc closes.";
                    }
                }
            }
        }
        boolean frozen = dialogueOpen || inventoryOpen || bigMap || chestOpen;
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

    /** Accepts the highlighted target through quest/encounter APIs without teleporting or healing. */
    private void handleQuestDialogueInput() {
        if (session.getCurrentMap().getType() != MapType.HUB || !session.getPlayer().isAlive()
                || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            dialogueOpen = false;
            activeNpc = null;
            return;
        }
        var targets = BossChallenge.values();
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            questChoice = Math.floorMod(questChoice - 1, targets.length);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            questChoice = (questChoice + 1) % targets.length;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            try {
                quests.accept(targets[questChoice]);
                questFeedback = "Accepted: " + targets[questChoice].displayName() + ". Talk to the Portal Guide.";
            } catch (IllegalArgumentException | IllegalStateException failure) {
                questFeedback = failure.getMessage();
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
                || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            dialogueOpen = false;
            activeNpc = null;
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            try {
                portalGuide.returnFromTraining();
                dialogueOpen = false;
                activeNpc = null;
                travelStatus = "Practice finished; returned to Hub";
            } catch (IllegalArgumentException | IllegalStateException failure) {
                travelStatus = failure.getMessage();
            }
        }
    }

    /** Handles the modal trainer menu before travel, debug keys or combat input. */
    private void handleTrainerDialogueInput() {
        if (session.getCurrentMap().getType() != MapType.HUB || !session.getPlayer().isAlive()
                || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            dialogueOpen = false;
            activeNpc = null;
            return;
        }
        var choices = ClassTrainer.Choice.values();
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            trainerChoice = Math.floorMod(trainerChoice - 1, choices.length);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            trainerChoice = (trainerChoice + 1) % choices.length;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            trainerFeedback = classTrainer.select(choices[trainerChoice])
                    ? "Applied: " + classTrainer.label(choices[trainerChoice])
                    : "Unchanged: already selected or an ability is still active.";
        }
    }

    /**
     * Handles the modal choice before travel shortcuts or ability input can consume the same keys.
     * Arrows change the highlight, E confirms through PortalGuide, and Escape cancels unchanged.
     * Confirmation configures a route only; entering the portal on a later frame performs travel.
     */
    private void handlePortalDialogueInput() {
        if (session.getCurrentMap().getType() != MapType.HUB || !session.getPlayer().isAlive()
                || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            dialogueOpen = false;
            activeNpc = null;
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            var choices = PortalGuide.Destination.values();
            portalChoice = choices[Math.floorMod(portalChoice.ordinal() - 1, choices.length)];
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            var choices = PortalGuide.Destination.values();
            portalChoice = choices[(portalChoice.ordinal() + 1) % choices.length];
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            try {
                portalGuide.select(portalChoice);
                travelStatus = "Portal ready: " + portalGuide.destinationLabel(portalChoice) + "; step into the portal";
                dialogueOpen = false;
                activeNpc = null;
            } catch (IllegalArgumentException | IllegalStateException failure) {
                travelStatus = failure.getMessage();
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

    /** Draws final delivered damage totals for the current stationary practice target only. */
    private void drawTrainingScore() {
        var practice = combatDemo.getTrainingDummy();
        if (practice.isEmpty()) {
            return;
        }
        var dummy = practice.orElseThrow();
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        float scale = Math.max(1f, h / 720f);
        float x = 12f * scale;
        float top = h - 85f * scale;
        float boxW = 280f * scale;
        float boxH = 150f * scale;
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
                        + "[R] Reset practice\nTalk to Return to Hub NPC [E] to leave",
                x + 10f * scale, top - 12f * scale, boxW - 20f * scale, Align.left, true);
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
            } else if (!digits.isEmpty()) {
                break;
            }
        }
        if (digits.isEmpty()) {
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
        if (endingOpen) {
            return BossQuests.ENDING_LINES;
        }
        if (isQuestDialogueOpen()) {
            StringBuilder text = new StringBuilder("Defeated: ").append(quests.getCompleted().size())
                    .append(" / ").append(BossChallenge.values().length).append('\n');
            var targets = BossChallenge.values();
            for (int i = 0; i < targets.length; i++) {
                text.append(i == questChoice ? ">     " : "      ").append(targets[i].displayName());
                if (quests.getActive().orElse(null) == targets[i]) {
                    text.append(" (ACTIVE)");
                }
                text.append('\n');
            }
            return List.of(text.append(questFeedback).toString());
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
        boolean last = dialogueLine >= lines.size() - 1;
        String closeHint = activeNpc.getRewardItemId() == null || activeNpc.hasInteracted()
                ? "[E] Close" : "[E] Take reward";
        String hint = endingOpen ? (last ? "[E] Finish" : "[E] Continue")
                : isQuestDialogueOpen() ? "[Up/Down] Choose   [E] Accept   [Esc] Close"
                : isTrainingExitDialogueOpen() ? "[E] Return to Hub   [Esc] Cancel"
                : isTrainerDialogueOpen() ? "[Up/Down] Choose   [E] Apply   [Esc] Close"
                : isPortalDialogueOpen() ? "[Up/Down] Choose   [E] Confirm   [Esc] Cancel"
                : last ? closeHint : "[E] Next";
        String body = lines.get(dialogueLine);
        DialogueLayout layout = DialogueLayout.fit(w, h, isTrainerDialogueOpen() || isQuestDialogueOpen(),
                activeNpc.getName(), body, hint, this::measureDialogueText);
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
        dialogueFont.draw(dialogueBatch, hint, x + pad, layout.hintTop(), layout.textWidth(), Align.left, true);
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
     * Screen-space only; both panels and text stop growing at the reference window size.
     * Memorial Hall fits one view and omits the minimap so that all five stones remain visible.
     * @param hud current size and visibility policy
     */
    private void drawMinimap(HudLayout hud) {
        if (!hud.detailsVisible() || session.getCurrentMap().getType() == MapType.MEMORIAL) {
            return;
        }
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        float ui = hud.scale();
        GameMap map = session.getCurrentMap();
        CollisionBox bounds = map.getBounds();

        float boxW = bigMap ? GameConfig.HUD_REFERENCE_WIDTH * 0.8f * ui / GameConfig.HUD_MAX_SCALE : 200f * ui;
        float boxH = bigMap ? GameConfig.HUD_REFERENCE_HEIGHT * 0.8f * ui / GameConfig.HUD_MAX_SCALE : 140f * ui;
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
        for (NPC npc : currentNpcs()) {
            dialogueShapes.setColor(1f, 0.85f, 0.20f, 1);
            dialogueShapes.rect(ox + (float) (npc.getX() - bounds.x()) * s,
                    oy + (float) (npc.getY() - bounds.y()) * s,
                    Math.max((float) npc.getWidth() * s, 3f),
                    Math.max((float) npc.getHeight() * s, 3f));
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

    /**
     * Recomputes the chest panel geometry from the current window size.
     * @return layout shared by chest rendering and hit testing
     */
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
    /**
     * Turns a raw material id such as weapon_lightning_staff into a readable name.
     * @param materialId non-null material identifier
     * @return formatted name, or the original identifier when no name remains
     * @throws NullPointerException if materialId is null
     */
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
        music.dispose();
        renderer.dispose();
        dialogueBatch.dispose();
        dialogueFont.dispose();
        dialogueShapes.dispose();
    }
}
