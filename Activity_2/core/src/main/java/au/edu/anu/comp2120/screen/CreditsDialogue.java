package au.edu.anu.comp2120.screen;

import java.util.List;

/**
 * Small presentation-only menu appended to the completed Memorial Sage conversation.
 * It never changes quest progress, consumes rewards or exits the game.
 * @author Daoyan Zhu (u7782042)
 */
final class CreditsDialogue {
    /** User-supplied credits divided into readable pages without Markdown control characters. */
    static final List<String> PAGES = List.of(
            "CREDITS\n\nSpecial Thanks\n\nLarry Wang\nCOMP2120 Tutor\n\nCOMP2120 Teaching Team\nANU School of Computing",
            "CREDITS\n\nDevelopment Team\n\nDaoyan Zhu\nHao Liu\nJiaqi Chen",
            "CREDITS\n\nArt & Visual Assets\n\nDaoyan Zhu\n\nVisual assets created using OpenAI GPT image generation.\n\nMusic created using Suno.",
            "CREDITS\n\nDevelopment Tools\n\nJava · LibGDX · GitLab",
            "Thank you for playing.");
    /** Whether this conversation extension owns input. */
    private boolean open;
    /** Zero selects credits; one selects closing this conversation. */
    private int choice;
    /** Negative for the choice page, otherwise the current credits page. */
    private int page = -1;

    /** Creates a closed conversation with no graphics dependencies. */
    CreditsDialogue() { }

    /** Opens a fresh choice page, selecting credits by default. */
    void open() { open = true; choice = 0; page = -1; }

    /** Closes only this presentation, leaving gameplay and completion unchanged. */
    void close() { open = false; choice = 0; page = -1; }

    /**
     * Reports whether the extension is open.
     * @return true on either its menu or a credits page
     */
    boolean isOpen() { return open; }

    /**
     * Changes the two-option selection; opposite simultaneous inputs cancel each other.
     * @param up whether Up was newly pressed
     * @param down whether Down was newly pressed
     */
    void select(boolean up, boolean down) {
        if (open && page < 0 && up != down) { choice = 1 - choice; }
    }

    /** Confirms a menu choice, advances one credits page, or closes after the final page. */
    void confirm() {
        if (!open) { return; }
        if (page < 0) {
            if (choice == 1) { close(); }
            else { page = 0; }
        } else if (++page >= PAGES.size()) { close(); }
    }

    /**
     * Reads the menu or current credits content for the existing adaptive dialogue box.
     * @return visible text, or empty when closed
     */
    String text() {
        if (!open) { return ""; }
        if (page >= 0) { return PAGES.get(page); }
        return "Would you like to view the credits?\n\n"
                + (choice == 0 ? "> " : "  ") + "View credits\n"
                + (choice == 1 ? "> " : "  ") + "Exit dialogue";
    }

    /**
     * Supplies controls and page progress without claiming that Exit quits the application.
     * @return footer appropriate to the current page
     */
    String hint() {
        return page < 0 ? "[Up/Down] Choose   [E] Confirm   [Esc] Close"
                : "[" + (page + 1) + "/" + PAGES.size() + "] [E] "
                + (page == PAGES.size() - 1 ? "Close" : "Next") + "   [Esc] Close";
    }
}
