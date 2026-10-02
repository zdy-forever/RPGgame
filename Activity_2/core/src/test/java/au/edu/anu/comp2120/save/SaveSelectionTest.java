package au.edu.anu.comp2120.save;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies the newest-valid-slot selection used by the main menu Continue action. */
final class SaveSelectionTest {

    /** Creates the test instance. */
    SaveSelectionTest() { }

    /** Picks the newer of two valid save slots.
     * @throws IOException if a temporary directory cannot be created
     */
    @Test
    void picksNewerOfTwoValidSlots() throws IOException {
        Path dir = Files.createTempDirectory("saves");
        SaveStore store = new SaveStore(dir);
        store.save(SaveSlot.AUTO, new SaveMetadata(SaveSlot.AUTO, Instant.parse("2026-01-01T00:00:00Z")).toFields());
        store.save(SaveSlot.MANUAL, new SaveMetadata(SaveSlot.MANUAL, Instant.parse("2026-02-01T00:00:00Z")).toFields());
        assertEquals(SaveSlot.MANUAL, SaveSelection.latestValid(dir).orElse(null));
    }

    /** Returns empty when the save directory has no valid saves.
     * @throws IOException if a temporary directory cannot be created
     */
    @Test
    void emptyWhenNoSaves() throws IOException {
        Path dir = Files.createTempDirectory("saves");
        new SaveStore(dir);
        assertTrue(SaveSelection.latestValid(dir).isEmpty());
    }

    /** Skips a corrupt slot and keeps the remaining valid one.
     * @throws IOException if a temporary directory cannot be created
     */
    @Test
    void skipsCorruptSlotAndKeepsValidOne() throws IOException {
        Path dir = Files.createTempDirectory("saves");
        SaveStore store = new SaveStore(dir);
        store.save(SaveSlot.MANUAL, new SaveMetadata(SaveSlot.MANUAL, Instant.parse("2026-02-01T00:00:00Z")).toFields());
        Files.writeString(store.path(SaveSlot.AUTO), "this is not a valid save file");
        assertEquals(SaveSlot.MANUAL, SaveSelection.latestValid(dir).orElse(null));
    }
}
