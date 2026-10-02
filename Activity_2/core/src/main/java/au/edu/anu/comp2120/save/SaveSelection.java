package au.edu.anu.comp2120.save;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

/**
 * Picks which save slot a "Continue" action should restore, based solely on on-disk metadata.
 * Neither slot is a fallback for the other; the newest valid snapshot wins. Corrupt or missing
 * slots are skipped so the caller can disable Continue when nothing is readable.
 *
 * @author Daoyan Zhu (u7782042)
 */
public final class SaveSelection {

    /** Prevents construction of the selection utility. */
    private SaveSelection() { }

    /** Reads both slots and returns the one with the newest creation timestamp.
     * @param directory directory holding the two slot files
     * @return the newest valid slot, or empty when neither slot is present or readable
     * @throws NullPointerException if directory is null
     */
    public static Optional<SaveSlot> latestValid(Path directory) {
        SaveStore store = new SaveStore(directory);
        SaveSlot best = null;
        Instant bestTime = null;
        for (SaveSlot slot : SaveSlot.values()) {
            try {
                var fields = store.load(slot);
                var metadata = SaveMetadata.fromFields(fields, slot);
                if (bestTime == null || metadata.savedAt().isAfter(bestTime)) {
                    bestTime = metadata.savedAt();
                    best = slot;
                }
            } catch (IOException | IllegalArgumentException ignored) {
                // Skip slots that are missing, corrupt or belong to another identity.
            }
        }
        return Optional.ofNullable(best);
    }
}
