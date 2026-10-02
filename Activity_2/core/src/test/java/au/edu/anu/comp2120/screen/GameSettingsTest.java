package au.edu.anu.comp2120.screen;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies music-volume clamping and fullscreen persistence of {@link GameSettings}.
 * The real settings file is backed up and restored so the test does not clobber user preferences.
 *
 * @author Jiaqi Chen (u7851266)
 */
final class GameSettingsTest {

    /** Path to the real settings file on disk. */
    private static final Path FILE = Path.of(System.getProperty("user.home"),
            ".comp2120-game", "settings.properties");
    /** Snapshot of the settings file before tests run. */
    private static String backup;
    /** Whether the settings file existed before tests ran. */
    private static boolean existed;

    /** Creates the test instance. */
    GameSettingsTest() { }

    /** Captures the current settings file so it can be restored after tests.
     * @throws IOException if the existing file cannot be read
     */
    @BeforeAll
    static void backupRealSettings() throws IOException {
        existed = Files.exists(FILE);
        if (existed) { backup = Files.readString(FILE); }
    }

    /** Restores the original settings file, or deletes it if it did not exist before the tests.
     * @throws IOException if the file cannot be written or deleted
     */
    @AfterAll
    static void restoreRealSettings() throws IOException {
        if (existed) {
            Files.writeString(FILE, backup);
        } else if (Files.exists(FILE)) {
            Files.delete(FILE);
        }
    }

    /** Volume above the range is clamped to 100. */
    @Test
    void clampsVolumeToMaximum() {
        GameSettings.setMusicVolume(150);
        assertEquals(100, GameSettings.musicVolume());
    }

    /** Volume below the range is clamped to 0. */
    @Test
    void clampsVolumeToMinimum() {
        GameSettings.setMusicVolume(-5);
        assertEquals(0, GameSettings.musicVolume());
    }

    /** Volume inside the range is stored unchanged. */
    @Test
    void keepsVolumeInRange() {
        GameSettings.setMusicVolume(40);
        assertEquals(40, GameSettings.musicVolume());
    }

    /** Fullscreen preference can be toggled and read back. */
    @Test
    void fullscreenToggles() {
        boolean before = GameSettings.fullscreen();
        GameSettings.setFullscreen(!before);
        assertEquals(!before, GameSettings.fullscreen());
        GameSettings.setFullscreen(before);
        assertEquals(before, GameSettings.fullscreen());
    }
}
