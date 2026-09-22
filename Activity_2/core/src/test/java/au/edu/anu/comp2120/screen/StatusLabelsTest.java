package au.edu.anu.comp2120.screen;

import au.edu.anu.comp2120.combat.CombatStatus;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies which negative statuses become visible labels, and in which order they stack.
 * @author Daoyan Zhu (u7782042)
 */
final class StatusLabelsTest {
    /** Creates an isolated fixture. */
    StatusLabelsTest() { }

    /** An unaffected actor shows nothing, so ordinary combat stays uncluttered. */
    @Test
    void healthyActorShowsNoLabel() {
        assertTrue(StatusLabels.forActor(new CombatStatus(), "").isEmpty());
    }

    /** Freeze and stagger both surface as readable English control labels. */
    @Test
    void controlEffectsBecomeReadableLabels() {
        CombatStatus frozen = new CombatStatus();
        frozen.applyFreeze(2);
        assertEquals(List.of("FROZEN"), texts(StatusLabels.forActor(frozen, "")));
        CombatStatus staggered = new CombatStatus();
        staggered.applyStagger(0.6);
        assertEquals(List.of("STAGGERED"), texts(StatusLabels.forActor(staggered, "")));
    }

    /** An attacker-owned affliction appears with its own stack count and colour. */
    @Test
    void attackerOwnedAfflictionIsShownAndBlankIsIgnored() {
        StatusLabels.Tag burning = StatusLabels.forActor(new CombatStatus(), "BURNING x3").get(0);
        assertEquals("BURNING x3", burning.text());
        assertTrue(burning.red() > burning.blue(), "Burning must read as a warm label");
        assertTrue(StatusLabels.forActor(new CombatStatus(), "   ").isEmpty());
    }

    /** Several simultaneous statuses stack with control effects first. */
    @Test
    void simultaneousStatusesStackWithControlFirst() {
        CombatStatus status = new CombatStatus();
        status.applyFreeze(2);
        status.applyStagger(0.5);
        List<StatusLabels.Tag> tags = StatusLabels.forActor(status, "BURNING x1");
        assertEquals(List.of("FROZEN", "STAGGERED", "BURNING x1"), texts(tags));
        assertThrows(UnsupportedOperationException.class, tags::clear);
    }

    /** An immunity window is labelled too, so negated hits read as a mechanic, not a bug. */
    @Test
    void immunityWindowIsLabelled() {
        CombatStatus status = new CombatStatus();
        status.applyInvulnerability(2);
        assertEquals(List.of("INVULNERABLE"), texts(StatusLabels.forActor(status, "")));
    }

    /** Null inputs are rejected rather than silently producing an empty overlay. */
    @Test
    void nullInputsAreRejected() {
        assertThrows(NullPointerException.class, () -> StatusLabels.forActor(null, ""));
        assertThrows(NullPointerException.class, () -> StatusLabels.forActor(new CombatStatus(), null));
        assertThrows(IllegalArgumentException.class, () -> new StatusLabels.Tag(" ", 1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new StatusLabels.Tag("X", 2, 0, 0));
    }

    /**
     * Extracts label text for order assertions.
     * @param tags labels under test
     * @return label strings in their given order
     */
    private static List<String> texts(List<StatusLabels.Tag> tags) {
        return tags.stream().map(StatusLabels.Tag::text).toList();
    }
}
