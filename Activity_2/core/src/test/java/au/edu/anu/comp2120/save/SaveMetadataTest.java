package au.edu.anu.comp2120.save;

import java.time.Instant;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Checks portable timestamps and explicit slot identity validation.
 * @author Hao Liu (u7998420)
 */
final class SaveMetadataTest {
    /** Creates a test instance. */
    SaveMetadataTest() { }

    /** Both slots preserve precise timestamps, including dates ahead of the system clock. */
    @Test
    void roundTripPreservesTimeAndAllowsGameplayFields() {
        for (SaveSlot slot : SaveSlot.values()) {
            var metadata = new SaveMetadata(slot, Instant.parse("2099-01-02T03:04:05.123456789Z"));
            var fields = new HashMap<>(metadata.toFields());
            fields.put("location.map", "hub");
            assertEquals(metadata, SaveMetadata.fromFields(fields, slot));
        }
    }

    /** Reading the other slot's metadata cannot silently select or merge it. */
    @Test
    void mismatchedSlotIsRejectedInBothDirections() {
        for (SaveSlot slot : SaveSlot.values()) {
            var fields = new SaveMetadata(slot, Instant.EPOCH).toFields();
            SaveSlot other = slot == SaveSlot.AUTO ? SaveSlot.MANUAL : SaveSlot.AUTO;
            assertThrows(IllegalArgumentException.class, () -> SaveMetadata.fromFields(fields, other));
        }
    }

    /** Missing, malformed and unexpected metadata fail instead of inventing defaults. */
    @Test
    void invalidMetadataIsRejected() {
        var valid = new SaveMetadata(SaveSlot.MANUAL, Instant.EPOCH).toFields();
        for (String key : valid.keySet()) {
            var missing = new HashMap<>(valid);
            missing.remove(key);
            assertThrows(IllegalArgumentException.class,
                    () -> SaveMetadata.fromFields(missing, SaveSlot.MANUAL));
            var malformed = new HashMap<>(valid);
            malformed.put(key, "invalid");
            assertThrows(IllegalArgumentException.class,
                    () -> SaveMetadata.fromFields(malformed, SaveSlot.MANUAL));
        }
        var extra = new HashMap<>(valid);
        extra.put("metadata.unknown", "value");
        assertThrows(IllegalArgumentException.class,
                () -> SaveMetadata.fromFields(extra, SaveSlot.MANUAL));
    }
}
