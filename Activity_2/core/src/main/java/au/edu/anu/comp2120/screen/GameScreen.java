package au.edu.anu.comp2120.screen;

import au.edu.anu.comp2120.GameSession;
import au.edu.anu.comp2120.TravelSystem;
import au.edu.anu.comp2120.player.PlayerInput;
import au.edu.anu.comp2120.player.PlayerCommand;
import au.edu.anu.comp2120.world.GameMap;
import au.edu.anu.comp2120.world.MapCatalog;
import au.edu.anu.comp2120.world.TravelCatalog;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;

/**
 * Coordinates the map session, movement input, combat debug adapter, and geometry-only presentation.
 *
 * @author Daoyan Zhu (u7782042)
 */
public final class GameScreen extends ScreenAdapter {

    /** Current-map and player coordination, independent of this screen's lifecycle. */
    private final GameSession session;
    /** Portal trigger/unlock state; delegates every map transition to the existing session. */
    private final TravelSystem travel;
    /** Graphics-independent debug adapter; all damage goes through the shared combat pipeline. */
    private final CombatDemo combatDemo;
    /** Last HP/key feedback applied to the native title, avoiding redundant title updates. */
    private String combatStatus = "";
    /** Brief travel feedback shown in the native window title without a font asset. */
    private String travelStatus = "Walk into purple portals; U unlocks fast travel (debug)";
    /** Adapter for the application's keyboard input. */
    private final PlayerInput input;
    /** Geometry-only view owned by this screen. */
    private final PlaceholderRenderer renderer;
    /** Whether lifecycle pause has suspended movement. */
    private boolean paused;
    /** Discards the first frame's time after startup or resume. */
    private boolean skipNextUpdate = true;

    /**
     * Creates the demo models and drawing resources after graphics initialization.
     */
    public GameScreen() {
        session = new GameSession(MapCatalog.createMaps(), MapCatalog.HUB,
                GameConfig.PLAYER_WIDTH, GameConfig.PLAYER_HEIGHT, GameConfig.PLAYER_SPEED);
        travel = new TravelSystem(session, TravelCatalog.createPortals(), TravelCatalog.createDestinations());
        combatDemo = new CombatDemo(session);
        input = new PlayerInput(Gdx.input);
        renderer = new PlaceholderRenderer(GameConfig.CAMERA_VIEW_WIDTH, GameConfig.CAMERA_VIEW_HEIGHT);
        updateMapView();
    }

    /**
     * Samples input, updates movement/abilities and combat, then checks arrival portals before drawing.
     * Fast travel consumes commands; portal arrival immediately clears old-map attacks and dash.
     * Frame stalls are capped and all simulation timers pause with this screen.
     *
     * @param delta seconds elapsed since the previous frame
     */
    @Override
    public void render(float delta) {
        if (!paused && !skipNextUpdate && Float.isFinite(delta) && delta >= 0) {
            boolean travelConsumed;
            try {
                travelConsumed = handleTravelInput();
            } catch (IllegalArgumentException | IllegalStateException failure) {
                travelStatus = failure.getMessage();
                travelConsumed = true;
            }
            combatDemo.update(Math.min(delta, GameConfig.MAX_FRAME_DELTA),
                    travelConsumed ? PlayerCommand.NONE : input.sample(),
                    !travelConsumed && Gdx.input.isKeyJustPressed(Input.Keys.R),
                    travelConsumed ? null : input.sampleClassSelection().orElse(null));
            if (travelConsumed) {
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
        skipNextUpdate = false;
        renderer.render(session.getCurrentMap(), session.getPlayer(),
                travel.getPortals(session.getCurrentMap().getId()),
                combatDemo.getCombatants(), combatDemo.getActiveAttacks(), combatDemo.getDrops());
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
        Gdx.graphics.setTitle("COMP2120 | " + map.getName() + " [" + map.getId()
                + "] | " + combatStatus + " | " + travelStatus);
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
    }
}
