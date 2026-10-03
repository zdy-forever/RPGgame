package au.edu.anu.comp2120.lwjgl3;

import au.edu.anu.comp2120.MainGame;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

/**
 * Starts the desktop version of the game using the LWJGL3 backend.
 *
 * @author Daoyan Zhu (u7782042)
 */
public final class Lwjgl3Launcher {

    /** Initial desktop window width in logical screen units. */
    private static final int WINDOW_WIDTH = 960;
    /** Initial desktop window height in logical screen units. */
    private static final int WINDOW_HEIGHT = 540;
    /** Explicit initial offset avoids backend auto-centering through a possibly missing primary monitor handle. */
    private static final int WINDOW_OFFSET = 80;

    /**
     * Prevents construction of the desktop launcher utility class.
     */
    private Lwjgl3Launcher() {
        // This utility class is not instantiated.
    }

    /**
     * Creates the desktop window and starts the LibGDX application.
     * Explicit placement bypasses the backend's unchecked primary-monitor auto-centering path.
     * A valid graphical session and OpenGL context are still required.
     *
     * @param args command-line arguments; currently unused
     */
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration configuration =
                new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("COMP2120 Game");
        configuration.setWindowedMode(WINDOW_WIDTH, WINDOW_HEIGHT);
        configuration.setWindowPosition(WINDOW_OFFSET, WINDOW_OFFSET);
        configuration.useVsync(true);
        configuration.setForegroundFPS(60);

        new Lwjgl3Application(new MainGame(), configuration);
    }
}
