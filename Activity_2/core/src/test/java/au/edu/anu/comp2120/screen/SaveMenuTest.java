package au.edu.anu.comp2120.screen;

import au.edu.anu.comp2120.*;
import au.edu.anu.comp2120.inventory.Inventory;
import au.edu.anu.comp2120.quest.BossQuests;
import au.edu.anu.comp2120.save.*;
import au.edu.anu.comp2120.world.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Checks actual menu input branches without requiring an OpenGL context.
 * @author Hao Liu (u7998420)
 */
final class SaveMenuTest {
    /** Isolated save directory. */
    @TempDir Path directory;
    /** Creates a test instance. */
    SaveMenuTest() { }

    /** Supplies exactly one edge-triggered key for a menu update and restores global input.
     * @param menu menu under test
     * @param key newly pressed key
     * @param otherPanel whether another UI is open
     * @return whether gameplay input was consumed
     */
    private boolean press(SaveMenu menu, int key, boolean otherPanel) {
        Input previous = Gdx.input;
        Gdx.input = (Input) Proxy.newProxyInstance(Input.class.getClassLoader(), new Class<?>[] {Input.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("isKeyJustPressed")) { return (int) args[0] == key; }
                    throw new UnsupportedOperationException(method.getName());
                });
        try { return menu.update(otherPanel); } finally { Gdx.input = previous; }
    }

    /** Overwrite and load require confirmation, Escape cancels, and open panels block entry.
     * @throws IOException if save inspection fails
     */
    @Test
    void menuRequiresConfirmationAndNeverWritesOnLoad() throws IOException {
        var session = new GameSession(MapCatalog.createMaps(), MapCatalog.HUB, 28, 28, 180);
        var travel = new TravelSystem(session, TravelCatalog.createGameplayPortals(), TravelCatalog.createDestinations());
        var flow = new EncounterFlow(session, travel, List.of(TravelCatalog.QUEST_PORTAL_ID));
        var combat = new CombatDemo(session, flow);
        var store = new SaveStore(directory);
        var saves = new GameSaves(session, travel, flow, new BossQuests(session, flow), combat,
                new Inventory(), List.of(), store);
        var menu = new SaveMenu(saves);
        assertTrue(press(menu, Input.Keys.F5, true));
        assertFalse(Files.exists(store.path(SaveSlot.MANUAL)));
        assertTrue(menu.notification().startsWith("Close the dialogue"));
        // Closing the blocking panel clears its hint without another save/load request.
        assertFalse(press(menu, -1, false));
        assertEquals(saves.status(), menu.notification());
        press(menu, Input.Keys.F9, true);
        menu.advanceHint(3f);
        assertTrue(menu.notification().startsWith("Close the dialogue"));
        menu.advanceHint(Float.NaN);
        menu.advanceHint(-1);
        assertTrue(menu.notification().startsWith("Close the dialogue"));
        menu.advanceHint(1f);
        assertEquals(saves.status(), menu.notification());
        assertFalse(menu.isOpen());
        assertTrue(press(menu, Input.Keys.F5, false));
        byte[] original = Files.readAllBytes(store.path(SaveSlot.MANUAL));
        session.getPlayer().takeDamage(20);
        press(menu, Input.Keys.F5, false);
        assertTrue(menu.isOpen());
        assertArrayEquals(original, Files.readAllBytes(store.path(SaveSlot.MANUAL)));
        press(menu, Input.Keys.ESCAPE, false);
        assertFalse(menu.isOpen());
        press(menu, Input.Keys.F9, false);
        press(menu, Input.Keys.NUM_2, false);
        assertEquals(80, session.getPlayer().getHealth().getCurrentHp());
        press(menu, Input.Keys.ENTER, false);
        assertEquals(100, session.getPlayer().getHealth().getCurrentHp());
        assertArrayEquals(original, Files.readAllBytes(store.path(SaveSlot.MANUAL)));
        session.getPlayer().takeDamage(30);
        press(menu, Input.Keys.F5, false); press(menu, Input.Keys.ENTER, false);
        assertEquals("70", store.load(SaveSlot.MANUAL).get("location.hp"));
        press(menu, Input.Keys.F9, false); press(menu, Input.Keys.NUM_1, false);
        press(menu, Input.Keys.ENTER, false);
        assertEquals(70, session.getPlayer().getHealth().getCurrentHp());
        assertTrue(saves.status().startsWith("Load failed"));
        assertFalse(Files.exists(store.path(SaveSlot.AUTO)));
    }

    /** Width and height independently shrink notification text and preserve top-left bounds. */
    @Test
    void notificationScalesOnBothAxesAndClearsExistingHud() {
        var normal = SaveMenu.hintLayout(960, 540);
        var shortWindow = SaveMenu.hintLayout(960, 180);
        var narrowWindow = SaveMenu.hintLayout(320, 540);
        assertEquals(1f, normal.scale(), 0.00001f);
        assertEquals(normal.scale() / 3, shortWindow.scale(), 0.00001f);
        assertEquals(normal.scale() / 3, narrowWindow.scale(), 0.00001f);
        for (int[] size : new int[][] {{960, 540}, {480, 560}, {820, 180}, {320, 180}, {640, 360}, {200, 100}}) {
            var layout = SaveMenu.hintLayout(size[0], size[1]);
            var hud = HudLayout.forWindow(size[0], size[1]);
            assertTrue(layout.x() > 0);
            assertTrue(layout.width() > 0);
            assertTrue(layout.x() + layout.width() < size[0]);
            assertTrue(layout.top() > size[1] / 2f);
            assertTrue(layout.top() < size[1]);
            if (hud.detailsVisible()) {
                assertTrue(layout.top() <= size[1] - 60 * hud.scale());
            } else if (hud.healthVisible()) {
                assertTrue(layout.top() <= size[1] - 28 * hud.scale());
            }
        }
        assertEquals(0, SaveMenu.hintLayout(0, 540).scale());
        assertEquals(0, SaveMenu.hintLayout(960, 0).scale());
        assertEquals(1, SaveMenu.hintLayout(1920, 1080).scale());
    }
}
