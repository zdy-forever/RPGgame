package au.edu.anu.comp2120;

import au.edu.anu.comp2120.screen.GameScreen;
import au.edu.anu.comp2120.screen.GameSettings;
import au.edu.anu.comp2120.screen.MainMenuScreen;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;

/**
 * Owns the LibGDX application lifecycle and selects the active game screen.
 *
 * @author Daoyan Zhu (u7782042)
 * @author Hao Liu (u7998420)
 * @author Jiaqi Chen (u7851266)
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
        if (GameSettings.fullscreen() && !Gdx.graphics.isFullscreen()) {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        }
        setScreen(new MainMenuScreen());
    }

    /** Defers a fresh session until the old screen has finished its render call.
     * Save files are neither loaded nor deleted. The old screen releases its graphics/audio once.
     * @param volume music gain retained across the restart
     */
    public void restart(float volume) {
        Gdx.app.postRunnable(() -> {
            var previous = getScreen();
            setScreen(new GameScreen(volume, true));
            if (previous != null) { previous.dispose(); }
        });
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
