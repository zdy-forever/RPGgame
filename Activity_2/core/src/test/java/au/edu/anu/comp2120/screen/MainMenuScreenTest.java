package au.edu.anu.comp2120.screen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies main-menu focus navigation, especially skipping the disabled Continue row.
 *
 * @author Jiaqi Chen (u7851266)
 */
final class MainMenuScreenTest {

    /** Row index of the Continue menu item, which is disabled when no save exists. */
    private static final int CONTINUE_ROW = 1;
    /** Total number of selectable rows in the main menu. */
    private static final int ROW_COUNT = 4;

    /** Creates the test instance. */
    MainMenuScreenTest() { }

    /** With a save, every row is reachable including Continue. */
    @Test
    void navigatesAllRowsWhenSaveExists() {
        assertTrue(MainMenuScreen.nextFocus(0, 1, true, CONTINUE_ROW, ROW_COUNT) == 1);
        assertTrue(MainMenuScreen.nextFocus(1, 1, true, CONTINUE_ROW, ROW_COUNT) == 2);
        assertTrue(MainMenuScreen.nextFocus(3, 1, true, CONTINUE_ROW, ROW_COUNT) == 0);
        assertTrue(MainMenuScreen.nextFocus(0, -1, true, CONTINUE_ROW, ROW_COUNT) == 3);
    }

    /** Without a save, the disabled Continue row is skipped both directions. */
    @Test
    void skipsContinueWhenNoSave() {
        assertTrue(MainMenuScreen.nextFocus(0, 1, false, CONTINUE_ROW, ROW_COUNT) == 2);
        assertTrue(MainMenuScreen.nextFocus(3, -1, false, CONTINUE_ROW, ROW_COUNT) == 2);
    }

    /** Moving down from Quit (no save) wraps to New Game, skipping Continue. */
    @Test
    void wrapsPastContinueWithoutSave() {
        assertTrue(MainMenuScreen.nextFocus(3, 1, false, CONTINUE_ROW, ROW_COUNT) == 0);
        assertTrue(MainMenuScreen.nextFocus(2, -1, false, CONTINUE_ROW, ROW_COUNT) == 0);
    }

    /** Settings pointer hit testing maps all clickable rows and rejects gaps/outside points. */
    @Test
    void settingsPointerRowsMatchRenderedButtons() {
        assertEquals(0, SettingsScreen.pointerRow(100, 375));
        assertEquals(1, SettingsScreen.pointerRow(100, 320));
        assertEquals(2, SettingsScreen.pointerRow(100, 266));
        assertEquals(-1, SettingsScreen.pointerRow(10, 375));
        assertEquals(-1, SettingsScreen.pointerRow(100, 349));
    }
}
