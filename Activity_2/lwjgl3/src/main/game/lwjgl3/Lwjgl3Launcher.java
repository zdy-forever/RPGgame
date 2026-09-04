package game.lwjgl3;

import game.MainGame;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

/**
 * Starts the desktop version of the game using the LWJGL3 backend.
 *
 * @author Daoyan Zhu (u7782042)
 */
public final class Lwjgl3Launcher {

    private static final int WINDOW_WIDTH = 960;
    private static final int WINDOW_HEIGHT = 540;

    /**
     * Prevents construction of the desktop launcher utility class.
     */
    private Lwjgl3Launcher() {
        // This utility class is not instantiated.
    }

    /**
     * Creates the desktop window and starts the LibGDX application.
     *
     * @param args command-line arguments; currently unused
     */
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration configuration =
                new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("COMP2120 Game");
        configuration.setWindowedMode(WINDOW_WIDTH, WINDOW_HEIGHT);
        configuration.useVsync(true);
        configuration.setForegroundFPS(60);

        new Lwjgl3Application(new MainGame(), configuration);
    }
}
