package au.edu.anu.comp2120.screen;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies credits choice, finite paging and repeat viewing without a graphics context.
 * @author Daoyan Zhu (u7782042)
 */
final class CreditsDialogueTest {
    /** Creates an independent dialogue fixture for each test. */
    CreditsDialogueTest() { }

    /** E visits every supplied section and closes after the final thank-you, without skipping pages. */
    @Test void viewCreditsVisitsAllPagesAndCloses() {
        var dialogue = new CreditsDialogue();
        assertFalse(dialogue.isOpen());
        assertEquals("", dialogue.text());
        dialogue.open();
        assertTrue(dialogue.text().contains("> View credits"));
        assertTrue(dialogue.hint().contains("Up/Down"));
        dialogue.confirm();
        for (int i = 0; i < CreditsDialogue.PAGES.size(); i++) {
            assertTrue(dialogue.isOpen());
            assertEquals(CreditsDialogue.PAGES.get(i), dialogue.text());
            assertTrue(dialogue.hint().contains("[" + (i + 1) + "/5]"));
            dialogue.select(false, true);
            assertEquals(CreditsDialogue.PAGES.get(i), dialogue.text());
            dialogue.confirm();
        }
        assertFalse(dialogue.isOpen());
        dialogue.confirm();
        assertEquals("", dialogue.text());
    }

    /** Selecting Exit closes the conversation without opening any credits page. */
    @Test void exitChoiceClosesImmediately() {
        var dialogue = new CreditsDialogue();
        dialogue.open();
        dialogue.select(false, true);
        assertTrue(dialogue.text().contains("> Exit dialogue"));
        dialogue.confirm();
        assertFalse(dialogue.isOpen());
    }

    /** Both directions wrap the two choices; simultaneous opposite inputs preserve selection. */
    @Test void arrowsAreStableAndWrap() {
        var dialogue = new CreditsDialogue();
        dialogue.open();
        String initial = dialogue.text();
        dialogue.select(true, true);
        assertEquals(initial, dialogue.text());
        dialogue.select(true, false);
        assertTrue(dialogue.text().contains("> Exit dialogue"));
        dialogue.select(false, true);
        assertEquals(initial, dialogue.text());
    }

    /** Escape/close at any page permits a later visit to start at the choice page again. */
    @Test void closeAndReopenResetOnlyPresentation() {
        var dialogue = new CreditsDialogue();
        dialogue.open();
        dialogue.confirm();
        dialogue.confirm();
        dialogue.close();
        assertFalse(dialogue.isOpen());
        dialogue.select(false, true);
        dialogue.confirm();
        assertFalse(dialogue.isOpen());
        dialogue.open();
        assertTrue(dialogue.text().contains("> View credits"));
        dialogue.confirm();
        assertEquals(CreditsDialogue.PAGES.get(0), dialogue.text());
    }

    /** All user-provided credits survive paging and no Markdown markers appear as literal dialogue. */
    @Test void preservesRequestedCredits() {
        String credits = String.join("\n", CreditsDialogue.PAGES);
        for (String text : new String[] {"Special Thanks", "Larry Wang", "COMP2120 Tutor",
                "COMP2120 Teaching Team", "ANU School of Computing", "Development Team", "Daoyan Zhu",
                "Hao Liu", "Jiaqi Chen", "Art & Visual Assets",
                "Visual assets created using OpenAI GPT image generation.", "Music created using Suno.",
                "Development Tools", "Java · LibGDX · GitLab", "Thank you for playing."}) {
            assertTrue(credits.contains(text), text);
        }
        assertFalse(credits.contains("**"));
        assertEquals("Thank you for playing.", CreditsDialogue.PAGES.get(4));
    }
}
