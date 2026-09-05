package au.edu.anu.comp2120.screen;

import au.edu.anu.comp2120.player.Player;
import au.edu.anu.comp2120.player.PlayerInput;
import au.edu.anu.comp2120.world.GameMap;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;

/**
 * Coordinates input, movement, and placeholder rendering for the first playable screen.
 *
 * @author Daoyan Zhu (u7782042)
 */
public final class GameScreen extends ScreenAdapter {

    /** Static collision world for the movement demonstration. */
    private final GameMap map;
    /** Player model; gameplay state does not depend on this screen. */
    private final Player player;
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
        map = new GameMap(MovementDemoConfig.WORLD_WIDTH, MovementDemoConfig.WORLD_HEIGHT,
                MovementDemoConfig.WALLS);
        player = new Player(MovementDemoConfig.PLAYER_SPAWN, MovementDemoConfig.PLAYER_SPEED, map);
        input = new PlayerInput(Gdx.input);
        renderer = new PlaceholderRenderer(MovementDemoConfig.WORLD_WIDTH, MovementDemoConfig.WORLD_HEIGHT);
        renderer.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    /**
     * Updates movement before drawing. Frame stalls are capped to avoid a large resume jump.
     *
     * @param delta seconds elapsed since the previous frame
     */
    @Override
    public void render(float delta) {
        if (!paused && !skipNextUpdate && Float.isFinite(delta) && delta >= 0) {
            input.update(player, Math.min(delta, MovementDemoConfig.MAX_FRAME_DELTA));
        }
        skipNextUpdate = false;
        renderer.render(map, player);
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
