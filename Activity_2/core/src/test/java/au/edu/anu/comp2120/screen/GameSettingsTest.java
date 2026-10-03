package au.edu.anu.comp2120.screen;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies startup parsing, persistence and IO failure handling in isolated settings homes.
 * A fresh class loader exercises real static initialization without touching user preferences.
 * @author Jiaqi Chen (u7851266)
 */
@ResourceLock("java.lang.System.properties")
final class GameSettingsTest {
    /** Fresh home directory per test. */ @TempDir Path home;
    /** Loader retaining only this scenario's static settings state. */ private URLClassLoader loader;
    /** Real production class loaded separately from the other test fixtures. */ private Class<?> settings;

    /** Creates the test instance. */ GameSettingsTest() { }

    /** Releases the class loader after each independent startup.
     * @throws IOException if the loader cannot be closed
     */
    @AfterEach void closeLoader() throws IOException {
        if (loader != null) { loader.close(); }
    }

    /** Missing settings leave defaults and do not create a file merely by starting.
     * @throws Exception if fixture setup or reflective invocation fails
     */
    @Test void missingFileKeepsDefaultsWithoutWriting() throws Exception {
        load();
        assertEquals(50, invoke("musicVolume"));
        assertEquals(false, invoke("fullscreen"));
        assertFalse(Files.exists(file()));
    }

    /** Startup clamps integer values, handles malformed numbers and accepts an absent volume.
     * @param value serialized music percentage or empty text
     * @param expected expected percentage after startup
     * @throws Exception if fixture setup or reflective invocation fails
     */
    @ParameterizedTest
    @CsvSource(value = {"80|80", " 42 |42", "-20|0", "101|100", "0|0", "100|100",
            "abc|50", "999999999999999999999|50", "42.5|50", "''|50"}, delimiter = '|')
    void loadsAndValidatesPersistedVolume(String value, int expected) throws Exception {
        Files.createDirectories(file().getParent());
        Files.writeString(file(), "music.volume=" + value + "\nfullscreen=TrUe\n");
        load();
        assertEquals(expected, invoke("musicVolume"));
        assertEquals(true, invoke("fullscreen"));
    }

    /** Missing properties use defaults while an explicit fullscreen setting still loads.
     * @throws Exception if fixture setup or reflective invocation fails
     */
    @Test void absentVolumeUsesDefaultAlongsideFullscreen() throws Exception {
        Files.createDirectories(file().getParent()); Files.writeString(file(), "fullscreen=true\n");
        load(); assertEquals(50, invoke("musicVolume")); assertEquals(true, invoke("fullscreen"));
    }

    /** A directory occupying the settings-file path must not prevent startup or in-memory updates.
     * @throws Exception if fixture setup or reflective invocation fails
     */
    @Test void unreadableFileAndFailedWriteKeepInMemorySettings() throws Exception {
        Files.createDirectories(file());
        load(); assertEquals(50, invoke("musicVolume"));
        invoke("setMusicVolume", int.class, 73); invoke("setFullscreen", boolean.class, true);
        assertEquals(73, invoke("musicVolume")); assertEquals(true, invoke("fullscreen"));
        assertTrue(Files.isDirectory(file()));
    }

    /** A file occupying the parent directory must not crash a noncritical preference change.
     * @throws Exception if fixture setup or reflective invocation fails
     */
    @Test void unavailableDirectoryDoesNotCrashSetters() throws Exception {
        Files.writeString(home.resolve(".comp2120-game"), "keep");
        load(); invoke("setMusicVolume", int.class, 61);
        assertEquals(61, invoke("musicVolume"));
        assertEquals("keep", Files.readString(home.resolve(".comp2120-game")));
    }

    /** A high requested percentage is stored as 100, not only clamped in memory.
     * @throws Exception if fixture setup or reflective invocation fails
     */
    @Test void clampsVolumeToMaximum() throws Exception {
        load(); invoke("setMusicVolume", int.class, 150);
        assertEquals(100, invoke("musicVolume")); assertEquals("100", persisted().getProperty("music.volume"));
    }

    /** A negative requested percentage is stored as zero.
     * @throws Exception if fixture setup or reflective invocation fails
     */
    @Test void clampsVolumeToMinimum() throws Exception {
        load(); invoke("setMusicVolume", int.class, -5);
        assertEquals(0, invoke("musicVolume")); assertEquals("0", persisted().getProperty("music.volume"));
    }

    /** Valid percentages are preserved on disk alongside the current fullscreen preference.
     * @throws Exception if fixture setup or reflective invocation fails
     */
    @Test void keepsVolumeInRange() throws Exception {
        load(); invoke("setMusicVolume", int.class, 40);
        assertEquals(40, invoke("musicVolume")); assertEquals("40", persisted().getProperty("music.volume"));
    }

    /** Fullscreen changes persist without resetting the chosen volume.
     * @throws Exception if fixture setup or reflective invocation fails
     */
    @Test void fullscreenToggles() throws Exception {
        load(); invoke("setMusicVolume", int.class, 65);
        invoke("setFullscreen", boolean.class, true);
        assertEquals("true", persisted().getProperty("fullscreen"));
        invoke("setFullscreen", boolean.class, false);
        assertEquals(false, invoke("fullscreen"));
        assertEquals("false", persisted().getProperty("fullscreen"));
        assertEquals("65", persisted().getProperty("music.volume"));
    }

    /** Locates this test's isolated settings file.
     * @return temporary preferences path
     */
    private Path file() { return home.resolve(".comp2120-game/settings.properties"); }

    /** Initializes the real class once using a temporary home, restoring the global property immediately.
     * @throws Exception if loading or initialization fails
     */
    private void load() throws Exception {
        String previous = System.getProperty("user.home");
        loader = new URLClassLoader(new URL[] {GameSettings.class.getProtectionDomain().getCodeSource().getLocation()},
                ClassLoader.getPlatformClassLoader());
        try {
            System.setProperty("user.home", home.toString());
            settings = Class.forName(GameSettings.class.getName(), true, loader);
        } finally {
            if (previous == null) { System.clearProperty("user.home"); }
            else { System.setProperty("user.home", previous); }
        }
    }

    /** Calls a public settings query without exposing private implementation methods.
     * @param method public query name
     * @return actual query result
     * @throws Exception if reflection or the query fails
     */
    private Object invoke(String method) throws Exception { return settings.getMethod(method).invoke(null); }

    /** Calls a public setter on the isolated production class.
     * @param method setter name
     * @param type setter parameter type
     * @param value requested setting
     * @throws Exception if reflection or the setter fails
     */
    private void invoke(String method, Class<?> type, Object value) throws Exception {
        settings.getMethod(method, type).invoke(null, value);
    }

    /** Reads actual persisted properties for independent round-trip assertions.
     * @return decoded properties
     * @throws IOException if the expected settings file cannot be read
     */
    private Properties persisted() throws IOException {
        var properties = new Properties();
        try (var stream = Files.newInputStream(file())) { properties.load(stream); }
        return properties;
    }
}
