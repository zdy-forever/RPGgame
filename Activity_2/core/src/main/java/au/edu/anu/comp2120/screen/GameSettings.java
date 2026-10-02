package au.edu.anu.comp2120.screen;

import au.edu.anu.comp2120.audio.BackgroundMusic;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Single-user preferences persisted next to the save directory, independent of any running game.
 * Holds the music volume and fullscreen preference so the main menu and in-game pause menu share one source.
 * Failures to read or write fall back to sensible defaults without interrupting startup.
 *
 * @author Daoyan Zhu (u7782042)
 */
public final class GameSettings {

    /** Settings file location, beside the save slots. */
    private static final Path FILE = Path.of(System.getProperty("user.home"),
            ".comp2120-game", "settings.properties");
    /** Default music percentage when no file exists. */
    private static final int DEFAULT_VOLUME = Math.round(BackgroundMusic.VOLUME * 100);

    private static int musicVolume = DEFAULT_VOLUME;
    private static boolean fullscreen = false;

    static {
        load();
    }

    /** Prevents construction of the settings utility. */
    private GameSettings() { }

    /** Reads the persisted music percentage.
     * @return integer percentage in zero through one hundred
     */
    public static int musicVolume() { return musicVolume; }

    /** Persists and clamps the music percentage.
     * @param percent desired percentage, clamped to zero through one hundred
     */
    public static void setMusicVolume(int percent) {
        musicVolume = Math.max(0, Math.min(100, percent));
        save();
    }

    /** Reads the persisted fullscreen preference.
     * @return whether the game should launch in fullscreen
     */
    public static boolean fullscreen() { return fullscreen; }

    /** Persists the fullscreen preference.
     * @param value whether fullscreen is wanted
     */
    public static void setFullscreen(boolean value) {
        fullscreen = value;
        save();
    }

    /** Loads preferences once, ignoring a missing or malformed file. */
    private static void load() {
        if (!Files.exists(FILE)) { return; }
        try (InputStream in = Files.newInputStream(FILE)) {
            Properties properties = new Properties();
            properties.load(in);
            musicVolume = clampInt(properties.getProperty("music.volume"), DEFAULT_VOLUME);
            fullscreen = Boolean.parseBoolean(properties.getProperty("fullscreen", "false"));
        } catch (IOException | NumberFormatException ignored) {
            // Keep defaults if the file is unreadable or malformed.
        }
    }

    /** Writes preferences atomically enough for a local single-user file. */
    private static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Properties properties = new Properties();
            properties.setProperty("music.volume", Integer.toString(musicVolume));
            properties.setProperty("fullscreen", Boolean.toString(fullscreen));
            try (OutputStream out = Files.newOutputStream(FILE)) {
                properties.store(out, "COMP2120 game settings");
            }
        } catch (IOException ignored) {
            // Settings are non-critical; ignore write failures.
        }
    }

    /** Parses a percentage, clamped to the valid range.
     * @param value raw property text
     * @param fallback default when missing or invalid
     * @return clamped percentage
     */
    private static int clampInt(String value, int fallback) {
        if (value == null) { return fallback; }
        try {
            int parsed = Integer.parseInt(value.trim());
            return Math.max(0, Math.min(100, parsed));
        } catch (NumberFormatException error) {
            return fallback;
        }
    }
}
