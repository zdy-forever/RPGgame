package au.edu.anu.comp2120.save;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Checks slot isolation and failure preservation using real temporary files.
 * @author Hao Liu (u7998420)
 */
final class SaveStoreTest {
    /** JUnit-owned directory; tests never touch user save slots. */
    @TempDir
    Path directory;

    /** Creates an independent test fixture. */
    SaveStoreTest() { }

    /** Startup does not create slots; loading an empty slot cannot fall back to another.
     * @throws IOException if fixture file access unexpectedly fails
     */
    @Test
    void startupAndMissingLoadDoNotWrite() throws IOException {
        Path nested = directory.resolve("not-created");
        SaveStore store = new SaveStore(nested);
        assertFalse(Files.exists(nested));
        assertThrows(IOException.class, () -> store.load(SaveSlot.AUTO));
        assertFalse(Files.exists(nested));
        store.save(SaveSlot.MANUAL, Map.of("progress", "manual"));
        assertThrows(IOException.class, () -> store.load(SaveSlot.AUTO));
        assertEquals(Map.of("progress", "manual"), store.load(SaveSlot.MANUAL));
    }

    /** Overwriting either slot preserves the other file byte-for-byte.
     * @throws IOException if fixture file access unexpectedly fails
     */
    @Test
    void slotsNeverOverwriteEachOther() throws IOException {
        SaveStore store = new SaveStore(directory);
        store.save(SaveSlot.MANUAL, Map.of("wins", "2"));
        byte[] manual = Files.readAllBytes(store.path(SaveSlot.MANUAL));
        store.save(SaveSlot.AUTO, Map.of("wins", "3"));
        store.save(SaveSlot.AUTO, Map.of("wins", "4"));
        assertArrayEquals(manual, Files.readAllBytes(store.path(SaveSlot.MANUAL)));
        byte[] automatic = Files.readAllBytes(store.path(SaveSlot.AUTO));
        store.save(SaveSlot.MANUAL, Map.of("wins", "5"));
        assertArrayEquals(automatic, Files.readAllBytes(store.path(SaveSlot.AUTO)));
        assertEquals("4", store.load(SaveSlot.AUTO).get("wins"));
        assertEquals("5", store.load(SaveSlot.MANUAL).get("wins"));
    }

    /** Loading an old manual snapshot neither merges progress nor writes the automatic slot.
     * @throws IOException if fixture file access unexpectedly fails
     */
    @Test
    void readingOldProgressDoesNotModifyFiles() throws IOException {
        SaveStore store = new SaveStore(directory);
        store.save(SaveSlot.MANUAL, Map.of("wins", "1"));
        store.save(SaveSlot.AUTO, Map.of("wins", "4"));
        byte[] automatic = Files.readAllBytes(store.path(SaveSlot.AUTO));
        byte[] manual = Files.readAllBytes(store.path(SaveSlot.MANUAL));
        Map<String, String> loaded = store.load(SaveSlot.MANUAL);
        assertEquals(Map.of("wins", "1"), loaded);
        assertThrows(UnsupportedOperationException.class, () -> loaded.put("wins", "9"));
        assertArrayEquals(automatic, Files.readAllBytes(store.path(SaveSlot.AUTO)));
        assertArrayEquals(manual, Files.readAllBytes(store.path(SaveSlot.MANUAL)));
        store.save(SaveSlot.AUTO, Map.of("wins", "2"));
        assertEquals("2", store.load(SaveSlot.AUTO).get("wins"));
        assertArrayEquals(manual, Files.readAllBytes(store.path(SaveSlot.MANUAL)));
    }

    /** A broken automatic file does not prevent reading the manual snapshot.
     * @throws IOException if fixture file access unexpectedly fails
     */
    @Test
    void corruptSlotDoesNotAffectOtherSlot() throws IOException {
        SaveStore store = new SaveStore(directory);
        store.save(SaveSlot.MANUAL, Map.of("map", "hub"));
        Files.writeString(store.path(SaveSlot.AUTO), "not a game save");
        assertThrows(IOException.class, () -> store.load(SaveSlot.AUTO));
        assertEquals(Map.of("map", "hub"), store.load(SaveSlot.MANUAL));
    }

    /** Rejected snapshot inputs leave an existing valid save unchanged.
     * @throws IOException if fixture file access unexpectedly fails
     */
    @Test
    void invalidSavePreservesPreviousFile() throws IOException {
        SaveStore store = new SaveStore(directory);
        store.save(SaveSlot.AUTO, Map.of("wins", "3"));
        byte[] before = Files.readAllBytes(store.path(SaveSlot.AUTO));
        assertThrows(IllegalArgumentException.class,
                () -> store.save(SaveSlot.AUTO, Map.of("save.version", "99")));
        assertArrayEquals(before, Files.readAllBytes(store.path(SaveSlot.AUTO)));
        assertThrows(IllegalArgumentException.class,
                () -> store.save(SaveSlot.AUTO, Map.of("huge", "x".repeat(1024 * 1024))));
        assertArrayEquals(before, Files.readAllBytes(store.path(SaveSlot.AUTO)));
    }

    /** Disk replacement failure does not touch the other slot or retain partial temporary files.
     * @throws IOException if fixture file access unexpectedly fails
     */
    @Test
    void failedReplacementPreservesOtherSlotAndCleansTemporaryFile() throws IOException {
        SaveStore store = new SaveStore(directory);
        store.save(SaveSlot.MANUAL, Map.of("wins", "2"));
        byte[] before = Files.readAllBytes(store.path(SaveSlot.MANUAL));
        Files.createDirectory(store.path(SaveSlot.AUTO));
        Files.writeString(store.path(SaveSlot.AUTO).resolve("occupied"), "keep");
        assertThrows(IOException.class, () -> store.save(SaveSlot.AUTO, Map.of("wins", "3")));
        assertArrayEquals(before, Files.readAllBytes(store.path(SaveSlot.MANUAL)));
        assertEquals("keep", Files.readString(store.path(SaveSlot.AUTO).resolve("occupied")));
        try (var paths = Files.list(directory)) {
            assertFalse(paths.anyMatch(path -> path.getFileName().toString().endsWith(".tmp")));
        }
    }

    /** Duplicate fields and unsupported versions cannot be silently accepted.
     * @throws IOException if fixture file access unexpectedly fails
     */
    @Test
    void rejectsDuplicateFieldsAndUnsupportedVersion() throws IOException {
        SaveStore store = new SaveStore(directory);
        String header = "save.format=comp2120-game\nsave.version=1\n";
        Files.writeString(store.path(SaveSlot.AUTO), header + "wins=1\nwins=2\n");
        assertThrows(IOException.class, () -> store.load(SaveSlot.AUTO));
        Files.writeString(store.path(SaveSlot.AUTO), header.replace("version=1", "version=99"));
        assertThrows(IOException.class, () -> store.load(SaveSlot.AUTO));
        Files.writeString(store.path(SaveSlot.AUTO), "x".repeat(1024 * 1024 + 1));
        assertThrows(IOException.class, () -> store.load(SaveSlot.AUTO));
    }

    /** Escaped characters and non-ASCII names round-trip without losing data.
     * @throws IOException if fixture file access unexpectedly fails
     */
    @Test
    void unicodeAndEscapesRoundTrip() throws IOException {
        SaveStore store = new SaveStore(directory);
        var snapshot = Map.of("物品", "法杖", "text", "line1\nline2\\path=: ");
        store.save(SaveSlot.MANUAL, snapshot);
        assertEquals(snapshot, store.load(SaveSlot.MANUAL));
    }
}
