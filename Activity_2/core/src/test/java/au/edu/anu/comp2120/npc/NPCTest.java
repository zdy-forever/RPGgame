package au.edu.anu.comp2120.npc;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Confirms NPC dialogue derivation, adjacency and one-time interaction state.
 * @author Daoyan Zhu (u7782042)
 */
final class NPCTest {
    /** Creates an isolated fixture per test. */
    NPCTest() { }

    /** The multi-line constructor keeps every line and exposes the first as the legacy single line. */
    @Test void multiLineConstructorKeepsOrderAndExposesFirstLine() {
        NPC npc = new NPC("guide", "Guide", 10, 20, 32, 32, List.of("Welcome.", "Take this."), "map-fragment");
        assertEquals("guide", npc.getId());
        assertEquals("Guide", npc.getName());
        assertEquals(10, npc.getX());
        assertEquals(20, npc.getY());
        assertEquals(32, npc.getWidth());
        assertEquals(32, npc.getHeight());
        assertEquals(List.of("Welcome.", "Take this."), npc.getLines());
        assertEquals("Welcome.", npc.getDialogueText());
        assertEquals("map-fragment", npc.getRewardItemId());
    }

    /** A null line list is treated as no script, not a null-pointer failure. */
    @Test void nullLinesBecomeEmptyScript() {
        NPC npc = new NPC("silent", "Silent", 0, 0, 16, 16, (List<String>) null, null);
        assertTrue(npc.getLines().isEmpty());
        assertEquals("", npc.getDialogueText());
        assertNull(npc.getRewardItemId());
    }

    /** The single-line constructor wraps its text as one script line. */
    @Test void singleLineConstructorWrapsTextAsOneLine() {
        NPC npc = new NPC("merchant", "Merchant", 5, 5, 16, 16, "Buy something?", "potion");
        assertEquals(List.of("Buy something?"), npc.getLines());
        assertEquals("Buy something?", npc.getDialogueText());
    }

    /** A null or empty single-line text produces an empty script, not a script with one blank line. */
    @Test void nullOrEmptySingleLineTextProducesEmptyScript() {
        assertTrue(new NPC("a", "A", 0, 0, 1, 1, (String) null, null).getLines().isEmpty());
        assertTrue(new NPC("b", "B", 0, 0, 1, 1, "", null).getLines().isEmpty());
    }

    /** The no-argument constructor produces a harmless placeholder with no script or reward. */
    @Test void noArgConstructorIsAnEmptyPlaceholder() {
        NPC npc = new NPC();
        assertEquals("dummy", npc.getId());
        assertTrue(npc.getLines().isEmpty());
        assertNull(npc.getRewardItemId());
        assertFalse(npc.hasInteracted());
    }

    /** hasInteracted starts false and latches true permanently once set. */
    @Test void interactedFlagIsOneTimeAndLatches() {
        NPC npc = new NPC("guide", "Guide", 0, 0, 16, 16, "Hi.", "reward");
        assertFalse(npc.hasInteracted());
        npc.setInteracted();
        assertTrue(npc.hasInteracted());
        npc.setInteracted();
        assertTrue(npc.hasInteracted());
    }

    /** isAdjacent checks each axis independently against the NPC's own position, not distance. */
    @Test void isAdjacentChecksBothAxesIndependently() {
        NPC npc = new NPC("guide", "Guide", 100, 200, 16, 16, "Hi.", null);
        assertTrue(npc.isAdjacent(100, 200, 0));
        assertTrue(npc.isAdjacent(105, 195, 5));
        assertFalse(npc.isAdjacent(106, 200, 5));
        assertFalse(npc.isAdjacent(100, 206, 5));
        assertTrue(npc.isAdjacent(90, 210, 10));
    }
}
