package au.edu.anu.comp2120.save;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Serializes access to two independent save slots through one session-owned store.
 * Reading a slot does not write either slot, merge snapshots or load a fallback.
 * Gameplay guards, confirmation, field validation and live-state restoration belong to
 * the future game coordinator. Construct one shared store per game session.
 * @author Hao Liu (u7998420)
 */
public final class SaveStore {
    /** Distinct file boundaries selected only by the known slot enum. */
    private final Map<SaveSlot, SaveFile> files = new EnumMap<>(SaveSlot.class);

    /** Prepares paths without creating, clearing or reading any save file.
     * @param directory directory containing both slots
     * @throws NullPointerException if directory is null
     */
    public SaveStore(Path directory) {
        Objects.requireNonNull(directory, "save directory");
        for (SaveSlot slot : SaveSlot.values()) {
            files.put(slot, new SaveFile(directory.resolve(slot.fileName())));
        }
    }

    /** Saves a detached snapshot to exactly one slot, waiting for any other store operation.
     * @param slot destination slot
     * @param fields complete validated gameplay snapshot
     * @throws IOException if disk writing or atomic replacement fails
     * @throws IllegalArgumentException if fields violate file-format constraints
     * @throws NullPointerException if slot, fields or an entry is null
     */
    public synchronized void save(SaveSlot slot, Map<String, String> fields) throws IOException {
        file(slot).write(Map.copyOf(Objects.requireNonNull(fields, "snapshot")));
    }

    /** Reads one detached snapshot without changing disk or live gameplay state.
     * Missing or broken slots fail explicitly even when the other slot is usable.
     * @param slot requested source slot
     * @return immutable decoded gameplay fields, still requiring domain validation
     * @throws IOException if the slot is missing, malformed, unreadable or unsupported
     * @throws NullPointerException if slot is null
     */
    public synchronized Map<String, String> load(SaveSlot slot) throws IOException {
        return file(slot).read();
    }

    /** Reads a slot's path for diagnostics and future UI feedback.
     * @param slot requested slot
     * @return absolute normalized destination
     * @throws NullPointerException if slot is null
     */
    public Path path(SaveSlot slot) { return file(slot).path(); }

    /** Resolves a non-null enum to its fixed file boundary.
     * @param slot known slot
     * @return corresponding save file
     * @throws NullPointerException if slot is null
     */
    private SaveFile file(SaveSlot slot) {
        return files.get(Objects.requireNonNull(slot, "save slot"));
    }
}
