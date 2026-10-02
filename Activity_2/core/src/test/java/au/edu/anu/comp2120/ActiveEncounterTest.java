package au.edu.anu.comp2120;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Validates the encounter permission record and its identifier rules.
 * @author COMP2120 Assignment 3 group
 * @author Jiaqi Chen (u7851266)
 */
final class ActiveEncounterTest {
    /** Creates a new {@code ActiveEncounterTest} instance. */
    ActiveEncounterTest() {
        // Test instances are created by JUnit.
    }

    /** A valid record exposes each identifier unchanged. */
    @Test void buildsWithValidIdentifiers() {
        var encounter = new ActiveEncounter("e1", "battle-training", "entry", "hub-portal");
        assertEquals("e1", encounter.id());
        assertEquals("battle-training", encounter.mapId());
        assertEquals("entry", encounter.spawnId());
        assertEquals("hub-portal", encounter.portalId());
    }

    /** Any null identifier is rejected. */
    @Test void rejectsNullIdentifier() {
        assertThrows(NullPointerException.class, () -> new ActiveEncounter(null, "m", "s", "p"));
        assertThrows(NullPointerException.class, () -> new ActiveEncounter("i", null, "s", "p"));
        assertThrows(NullPointerException.class, () -> new ActiveEncounter("i", "m", null, "p"));
        assertThrows(NullPointerException.class, () -> new ActiveEncounter("i", "m", "s", null));
    }

    /** Any blank identifier is rejected. */
    @Test void rejectsBlankIdentifier() {
        assertThrows(IllegalArgumentException.class, () -> new ActiveEncounter(" ", "m", "s", "p"));
        assertThrows(IllegalArgumentException.class, () -> new ActiveEncounter("i", "", "s", "p"));
        assertThrows(IllegalArgumentException.class, () -> new ActiveEncounter("i", "m", "  ", "p"));
        assertThrows(IllegalArgumentException.class, () -> new ActiveEncounter("i", "m", "s", "\t"));
    }

    /** Records with identical identifiers are equal. */
    @Test void equalIdentifiersProduceEqualRecords() {
        assertEquals(new ActiveEncounter("e1", "m", "s", "p"), new ActiveEncounter("e1", "m", "s", "p"));
    }
}
