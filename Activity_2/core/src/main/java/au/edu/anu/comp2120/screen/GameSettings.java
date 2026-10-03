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
 * Missing files and read IO failures retain defaults; invalid volume text uses the default volume.
 * Writes are best-effort: an IO failure leaves the current in-memory preferences in effect.
 *
 * @author Daoyan Zhu (u7782042)
 * @author Jiaqi Chen (u7851266)
 */
public final class GameSettings {

    /** Settings file location, beside the save slots. */
    private static final Path FILE = Path.of(System.getProperty("user.home"),
            ".comp2120-game", "settings.properties");
    /** Default music percentage when no file exists. */
    private static final int DEFAULT_VOLUME = Math.round(BackgroundMusic.VOLUME * 100);
    /** Current music percentage, clamped to [0, 100]. */
    private static int musicVolume = DEFAULT_VOLUME;
    /** Whether the game should launch in fullscreen mode. */
    private static boolean fullscreen = false;

    static {
        load();
    }

    /** Prevents construction of the settings utility. */
    private GameSettings() { }

    /** Reads the current in-memory music percentage.
     * @return integer percentage in zero through one hundred
     */
    public static int musicVolume() { return musicVolume; }

    /** Clamps the in-memory music percentage and attempts to persist both preferences.
     * @param percent desired percentage, clamped to zero through one hundred
     */
    public static void setMusicVolume(int percent) {
        musicVolume = Math.max(0, Math.min(100, percent));
        save();
    }

    /** Reads the current in-memory fullscreen preference.
     * @return whether the game should launch in fullscreen
     */
    public static boolean fullscreen() { return fullscreen; }

    /** Updates the in-memory fullscreen preference and attempts to persist both preferences.
     * @param value whether fullscreen is wanted
     */
    public static void setFullscreen(boolean value) {
        fullscreen = value;
        save();
    }

    /** Loads preferences once, retaining defaults for missing files and read IO failures.
     * Invalid volume text falls back to the default; only the text "true" (ignoring case) enables fullscreen.
     * @throws IllegalArgumentException if the properties file contains a malformed Unicode escape
     */
    private static void load() {
        if (!Files.exists(FILE)) { return; }
        try (InputStream in = Files.newInputStream(FILE)) {
            Properties properties = new Properties();
            properties.load(in);
            musicVolume = clampInt(properties.getProperty("music.volume"));
            fullscreen = Boolean.parseBoolean(properties.getProperty("fullscreen", "false"));
        } catch (IOException | NumberFormatException ignored) {
            // Keep defaults for IO failures or invalid numeric data.
        }
    }

    /** Overwrites the local preferences file directly, ignoring IO failures; replacement is not atomic. */
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

    /** Parses a percentage, using the default volume for missing or invalid text.
     * @param value raw property text
     * @return clamped percentage
     */
    private static int clampInt(String value) {
        if (value == null) { return DEFAULT_VOLUME; }
        try {
            int parsed = Integer.parseInt(value.trim());
            return Math.max(0, Math.min(100, parsed));
        } catch (NumberFormatException error) {
            return DEFAULT_VOLUME;
        }
    }
}
