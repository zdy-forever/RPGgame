package game;

import game.screen.GameScreen;
import com.badlogic.gdx.Game;

/**
 * Owns the LibGDX application lifecycle and selects the active game screen.
 *
 * @author Daoyan Zhu (u7782042)
 */
public final class MainGame extends Game {

    /**
     * Creates the game application object.
     */
    public MainGame() {
        // LibGDX performs application initialization in create().
    }

    /**
     * Creates the initial screen after LibGDX has initialized its subsystems.
     */
    @Override
    public void create() {
        setScreen(new GameScreen());
    }

    /**
     * Releases resources owned by the active screen when the game exits.
     */
    @Override
    public void dispose() {
        if (getScreen() != null) {
            getScreen().dispose();
        }
    }
}
