package au.edu.anu.comp2120.save;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Confirms the two independent save slots expose fixed, non-arbitrary filenames.
 * @author COMP2120 Assignment 3 group
 */
final class SaveSlotTest {
    /** Each slot maps to its fixed disk filename. */
    @Test void hasDistinctFixedFilenames() {
        assertEquals("automatic.properties", SaveSlot.AUTO.fileName());
        assertEquals("manual.properties", SaveSlot.MANUAL.fileName());
    }

    /** Exactly two independent slots are defined. */
    @Test void definesTwoIndependentSlots() {
        assertEquals(2, SaveSlot.values().length);
    }
}
