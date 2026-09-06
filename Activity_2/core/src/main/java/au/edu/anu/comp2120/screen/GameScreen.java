package au.edu.anu.comp2120.screen;

import au.edu.anu.comp2120.GameSession;
import au.edu.anu.comp2120.player.PlayerInput;
import au.edu.anu.comp2120.world.GameMap;
import au.edu.anu.comp2120.world.MapCatalog;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;

/**
 * Coordinates the map session, existing movement input, and geometry-only map presentation.
 *
 * @author Daoyan Zhu (u7782042)
 */
public final class GameScreen extends ScreenAdapter {

    /** Current-map and player coordination, independent of this screen's lifecycle. */
    private final GameSession session;
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
        input = new PlayerInput(Gdx.input);
        GameMap map = session.getCurrentMap();
        renderer = new PlaceholderRenderer((float) map.getBounds().width(), (float) map.getBounds().height());
        updateMapView();
    }

    /**
     * Updates movement before drawing. Frame stalls are capped to avoid a large resume jump.
     *
     * @param delta seconds elapsed since the previous frame
     */
    @Override
    public void render(float delta) {
        if (!paused && !skipNextUpdate && Float.isFinite(delta) && delta >= 0) {
            if (handleMapInput()) {
                updateMapView();
            } else {
                input.update(session.getPlayer(), Math.min(delta, GameConfig.MAX_FRAME_DELTA));
            }
        }
        skipNextUpdate = false;
        renderer.render(session.getCurrentMap(), session.getPlayer());
    }

    /**
     * Handles temporary map-selection shortcuts; a switch consumes the movement update.
     *
     * @return true if a map/spawn selection was applied this frame
     */
    private boolean handleMapInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
            session.switchMap(MapCatalog.HUB);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
            session.switchMap(MapCatalog.TRAINING);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
            session.switchMap(MapCatalog.CROSSROADS);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) {
            session.switchMap(MapCatalog.HUB, "north");
        } else {
            return false;
        }
        return true;
    }

    /** Updates viewport dimensions and the native window title after a map change. */
    private void updateMapView() {
        GameMap map = session.getCurrentMap();
        renderer.showMap(map, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.graphics.setTitle("COMP2120 | " + map.getName() + " [" + map.getId()
                + "] | WASD move | 1 Hub | 2 Training | 3 Crossroads | 4 Hub north");
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
