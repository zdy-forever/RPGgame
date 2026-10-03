package au.edu.anu.comp2120.save;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

/**
 * Picks which save slot a "Continue" action should restore, based solely on on-disk metadata.
 * Files with unreadable contents or invalid slot/timestamp metadata are skipped.
 * The newest remaining timestamp wins; ties keep the first slot in enum order.
 * Full gameplay validation is deferred to {@link GameSaves}: selection alone does not prove
 * that a snapshot can be restored, and a failed restoration does not retry the other slot.
 *
 * @author Daoyan Zhu (u7782042)
 * @author Jiaqi Chen (u7851266)
 */
public final class SaveSelection {

    /** Prevents construction of the selection utility. */
    private SaveSelection() { }

    /** Reads both slots and returns the one with the newest creation timestamp.
     * @param directory directory holding the two slot files
     * @return the newest readable slot with valid metadata, or empty when neither qualifies
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
