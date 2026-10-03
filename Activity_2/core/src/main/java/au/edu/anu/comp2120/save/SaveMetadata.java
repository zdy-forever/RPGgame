package au.edu.anu.comp2120.save;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Slot identity and UTC creation time stored alongside gameplay fields by {@link GameSaves}.
 * {@link SaveSelection} compares timestamps for Continue; this component validates slot identity
 * and timestamp syntax without ranking gameplay progress or restoring state.
 * @param slot slot that created this snapshot
 * @param savedAt UTC timestamp of snapshot creation, not a gameplay timer
 * @author Hao Liu (u7998420)
 */
public record SaveMetadata(SaveSlot slot, Instant savedAt) {
    /** Keys owned by this component. */
    private static final Set<String> KEYS = Set.of("metadata.slot", "metadata.savedAt");

    /** Requires explicit identity and timestamp without consulting the clock.
     * @param slot originating slot
     * @param savedAt creation time
     * @throws NullPointerException if either argument is null
     */
    public SaveMetadata {
        Objects.requireNonNull(slot, "slot");
        Objects.requireNonNull(savedAt, "savedAt");
    }

    /** Encodes an unambiguous timestamp independent of the computer's timezone.
     * @return immutable fields for inclusion in a full save
     */
    public Map<String, String> toFields() {
        return Map.of("metadata.slot", slot.name(), "metadata.savedAt", savedAt.toString());
    }

    /** Validates file identity against the explicitly selected slot without fallback or writes.
     * Extra gameplay namespaces are allowed; unknown metadata keys are rejected.
     * Future timestamps remain valid because a user's system clock can change.
     * @param fields decoded snapshot fields
     * @param expectedSlot slot the player or coordinator chose to read
     * @return validated metadata
     * @throws NullPointerException if fields or expectedSlot is null
     * @throws IllegalArgumentException if metadata is missing, malformed or belongs to another slot
     */
    public static SaveMetadata fromFields(Map<String, String> fields, SaveSlot expectedSlot) {
        Objects.requireNonNull(fields, "fields");
        Objects.requireNonNull(expectedSlot, "expectedSlot");
        for (String key : KEYS) {
            if (fields.get(key) == null) { throw new IllegalArgumentException("Missing " + key); }
        }
        for (String key : fields.keySet()) {
            if (key == null || (key.startsWith("metadata.") && !KEYS.contains(key))) {
                throw new IllegalArgumentException("Unknown metadata field: " + key);
            }
        }
        SaveSlot slot = SaveSlot.valueOf(fields.get("metadata.slot"));
        if (slot != expectedSlot) { throw new IllegalArgumentException("Save belongs to another slot"); }
        try {
            return new SaveMetadata(slot, Instant.parse(fields.get("metadata.savedAt")));
        } catch (DateTimeParseException error) {
            throw new IllegalArgumentException("Invalid save timestamp", error);
        }
    }
}
