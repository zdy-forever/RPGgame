package au.edu.anu.comp2120.screen;

import au.edu.anu.comp2120.*;
import au.edu.anu.comp2120.inventory.Inventory;
import au.edu.anu.comp2120.quest.BossQuests;
import au.edu.anu.comp2120.save.*;
import au.edu.anu.comp2120.world.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Graphics;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Exercises real pause-menu input and persistence without OpenGL.
 * @author Hao Liu (u7998420)
 */
final class SaveMenuTest {
    /** Isolated disk state. */
    @TempDir Path directory;
    /** Live world under test. */
    private GameSession session;
    /** Actual persistence coordinator. */
    private GameSaves saves;
    /** Slot storage used for byte-level invariants. */
    private SaveStore store;
    /** Application volume fake. */
    private int volume = 50;
    /** Number of confirmed restart requests. */
    private int restarts;

    /** Creates independent test state. */
    SaveMenuTest() { }

    /** Wires the production persistence coordinator and menu callbacks.
     * @return menu using isolated files
     */
    private SaveMenu menu() {
        session = new GameSession(MapCatalog.createMaps(), MapCatalog.HUB, 28, 28, 180);
        var travel = new TravelSystem(session, TravelCatalog.createGameplayPortals(), TravelCatalog.createDestinations());
        var flow = new EncounterFlow(session, travel, List.of(TravelCatalog.QUEST_PORTAL_ID));
        store = new SaveStore(directory);
        saves = new GameSaves(session, travel, flow, new BossQuests(session, flow), new CombatDemo(session, flow),
                new Inventory(), List.of(), store);
        return new SaveMenu(saves, () -> volume, value -> volume = value, () -> restarts++);
    }

    /** Sends one keyboard edge.
     * @param menu menu under test
     * @param key edge or minus one
     * @param otherPanel existing panel ownership
     * @return consumed flag
     */
    private boolean press(SaveMenu menu, int key, boolean otherPanel) {
        return frame(menu, key, otherPanel, 960, 540, 0, 0, false, false);
    }

    /** Supplies a complete input frame and restores LibGDX globals even on failure.
     * @param menu menu under test
     * @param key pressed key
     * @param otherPanel another modal owns input
     * @param width window width
     * @param height window height
     * @param x mouse x
     * @param y mouse y from top
     * @param click left button edge
     * @param held left button held
     * @return consumed flag
     */
    private boolean frame(SaveMenu menu, int key, boolean otherPanel, int width, int height,
            int x, int y, boolean click, boolean held) {
        Input previous = Gdx.input; Graphics previousGraphics = Gdx.graphics;
        Gdx.input = (Input) Proxy.newProxyInstance(Input.class.getClassLoader(), new Class<?>[] {Input.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "isKeyJustPressed" -> (int) args[0] == key;
                    case "getX" -> x;
                    case "getY" -> y;
                    case "isButtonJustPressed" -> click;
                    case "isButtonPressed" -> held;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        Gdx.graphics = (Graphics) Proxy.newProxyInstance(Graphics.class.getClassLoader(), new Class<?>[] {Graphics.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getWidth" -> width;
                    case "getHeight" -> height;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        try { return menu.update(otherPanel); }
        finally { Gdx.input = previous; Gdx.graphics = previousGraphics; }
    }

    /** Escape owns both opening and closing frames, and respects existing gameplay panels. */
    @Test void escapeMenuAndPanelPriority() {
        var menu = menu();
        assertEquals("", menu.notification());
        assertFalse(press(menu, Input.Keys.ESCAPE, true));
        assertFalse(menu.isOpen());
        assertTrue(press(menu, Input.Keys.ESCAPE, false));
        assertTrue(menu.isOpen());
        assertTrue(press(menu, -1, false));
        assertTrue(press(menu, Input.Keys.ESCAPE, false));
        assertFalse(menu.isOpen());
        assertFalse(press(menu, -1, false));
    }

    /** WASD navigation and E confirmation retain overwrite protection and consume close frames.
     * @throws IOException if isolated save files cannot be read
     */
    @Test void keyboardAliasesSaveWithoutBypassingConfirmation() throws IOException {
        var menu = menu();
        assertTrue(press(menu, Input.Keys.ESCAPE, false));
        assertTrue(press(menu, Input.Keys.S, false));
        assertTrue(press(menu, Input.Keys.E, false));
        assertFalse(menu.isOpen());
        byte[] original = Files.readAllBytes(store.path(SaveSlot.MANUAL));
        session.getPlayer().takeDamage(20);
        press(menu, Input.Keys.ESCAPE, false);
        press(menu, Input.Keys.S, false);
        assertTrue(press(menu, Input.Keys.E, false));
        assertTrue(menu.isOpen());
        assertArrayEquals(original, Files.readAllBytes(store.path(SaveSlot.MANUAL)));
        assertTrue(press(menu, Input.Keys.E, false));
        assertFalse(menu.isOpen());
        assertEquals("80", store.load(SaveSlot.MANUAL).get("location.hp"));
        press(menu, Input.Keys.ESCAPE, false);
        press(menu, Input.Keys.W, false);
        press(menu, Input.Keys.D, false);
        assertEquals(51, volume);
        press(menu, Input.Keys.A, false);
        assertEquals(50, volume);
        assertTrue(press(menu, Input.Keys.ESCAPE, false));
    }

    /** Confirmations prevent accidental overwrites and loading never writes either slot.
     * @throws IOException if test files cannot be inspected
     */
    @Test void saveAndLoadPreserveIndependentFiles() throws IOException {
        var menu = menu();
        assertTrue(press(menu, Input.Keys.F5, true));
        assertFalse(Files.exists(store.path(SaveSlot.MANUAL)));
        press(menu, Input.Keys.ESCAPE, false);
        press(menu, Input.Keys.DOWN, false);
        press(menu, Input.Keys.ENTER, false);
        assertFalse(menu.isOpen());
        byte[] original = Files.readAllBytes(store.path(SaveSlot.MANUAL));
        session.getPlayer().takeDamage(20);
        press(menu, Input.Keys.F5, false);
        assertTrue(menu.isOpen());
        assertArrayEquals(original, Files.readAllBytes(store.path(SaveSlot.MANUAL)));
        press(menu, Input.Keys.ESCAPE, false);
        assertTrue(menu.isOpen());
        press(menu, Input.Keys.ESCAPE, false);
        press(menu, Input.Keys.F9, false); press(menu, Input.Keys.NUM_2, false);
        assertEquals(80, session.getPlayer().getHealth().getCurrentHp());
        press(menu, Input.Keys.ENTER, false);
        assertEquals(100, session.getPlayer().getHealth().getCurrentHp());
        assertArrayEquals(original, Files.readAllBytes(store.path(SaveSlot.MANUAL)));
        assertFalse(Files.exists(store.path(SaveSlot.AUTO)));
        session.getPlayer().takeDamage(30);
        press(menu, Input.Keys.F5, false); press(menu, Input.Keys.ENTER, false);
        assertEquals("70", store.load(SaveSlot.MANUAL).get("location.hp"));
        press(menu, Input.Keys.F9, false); press(menu, Input.Keys.NUM_1, false);
        press(menu, Input.Keys.ENTER, false);
        assertEquals(70, session.getPlayer().getHealth().getCurrentHp());
        assertTrue(menu.notification().startsWith("Load failed"));
    }

    /** Result messages expire, do not resurrect, and repeated identical operations notify again. */
    @Test void allResultToastsExpireAndRepeat() {
        var menu = menu();
        press(menu, Input.Keys.F5, false);
        assertEquals("Manual save complete.", menu.notification());
        menu.advanceHint(Float.NaN); menu.advanceHint(-1);
        assertFalse(menu.notification().isEmpty());
        menu.advanceHint(4); menu.observeStatus();
        assertEquals("", menu.notification());
        press(menu, Input.Keys.F5, false); press(menu, Input.Keys.ENTER, false);
        assertEquals("Manual save complete.", menu.notification());
        menu.advanceHint(4);
        press(menu, Input.Keys.F9, false); press(menu, Input.Keys.NUM_2, false);
        press(menu, Input.Keys.ENTER, false);
        assertTrue(menu.notification().startsWith("Loaded MANUAL"));
        menu.advanceHint(4); menu.observeStatus();
        assertEquals("", menu.notification());
    }

    /** Pausing either combat or training does not bypass safe-map validation. */
    @Test void unsafeMapsCannotSaveOrLoadFromPausedMenu() {
        var menu = menu();
        for (String map : List.of(MapCatalog.TRAINING, MapCatalog.CROSSROADS)) {
            session.switchMap(map);
            press(menu, Input.Keys.ESCAPE, false);
            press(menu, Input.Keys.DOWN, false); press(menu, Input.Keys.ENTER, false);
            assertTrue(menu.isOpen());
            assertFalse(Files.exists(store.path(SaveSlot.MANUAL)));
            press(menu, Input.Keys.DOWN, false); press(menu, Input.Keys.ENTER, false);
            press(menu, Input.Keys.NUM_2, false);
            assertTrue(menu.notification().startsWith("Save/load unavailable"));
            press(menu, Input.Keys.DOWN, false); press(menu, Input.Keys.ENTER, false);
            assertTrue(menu.notification().startsWith("Save/load unavailable"));
            press(menu, Input.Keys.ESCAPE, false);
            press(menu, Input.Keys.ESCAPE, false);
        }
    }

    /** Restart is explicit, cancellable and leaves slot bytes and application volume untouched.
     * @throws IOException if test files cannot be inspected
     */
    @Test void restartConfirmationDoesNotTouchFiles() throws IOException {
        var menu = menu(); saves.saveManual();
        byte[] before = Files.readAllBytes(store.path(SaveSlot.MANUAL));
        press(menu, Input.Keys.ESCAPE, false);
        for (int i = 0; i < 3; i++) { press(menu, Input.Keys.DOWN, false); }
        press(menu, Input.Keys.ENTER, false);
        assertEquals(0, restarts);
        press(menu, Input.Keys.ESCAPE, false);
        assertEquals(0, restarts);
        press(menu, Input.Keys.ENTER, false); press(menu, Input.Keys.ENTER, false);
        assertEquals(1, restarts); assertEquals(50, volume);
        assertArrayEquals(before, Files.readAllBytes(store.path(SaveSlot.MANUAL)));
    }

    /** Keyboard and pointer volume controls clamp at both endpoints and use resized geometry. */
    @Test void sliderSupportsKeyboardAndResizedMouseDrag() {
        var menu = menu(); press(menu, Input.Keys.ESCAPE, false); press(menu, Input.Keys.UP, false);
        for (int i = 0; i < 120; i++) { press(menu, Input.Keys.LEFT, false); }
        assertEquals(0, volume);
        for (int i = 0; i < 120; i++) { press(menu, Input.Keys.RIGHT, false); }
        assertEquals(100, volume);
        var box = SaveMenu.layout(480, 270);
        int x = Math.round(box.x() + 250 * box.scale());
        int y = Math.round(270 - box.y() - 75 * box.scale());
        frame(menu, -1, false, 480, 270, x, y, true, true);
        assertEquals(50, volume, 1);
        frame(menu, -1, false, 480, 270, 999, y, false, true);
        assertEquals(100, volume);
        frame(menu, -1, false, 480, 270, -100, y, false, true);
        assertEquals(0, volume);
        frame(menu, -1, false, 480, 270, x, y, false, false);
        assertEquals(0, volume);
    }

    /** Both axes constrain the centered panel, including minimized and extreme aspect ratios. */
    @Test void layoutFitsSmallWindows() {
        for (int[] size : new int[][] {{960,540}, {480,560}, {820,180}, {200,100}, {120,900}}) {
            var box = SaveMenu.layout(size[0],size[1]);
            assertTrue(box.x() >= 0); assertTrue(box.y() >= 0);
            assertTrue(box.x() + 500 * box.scale() <= size[0]);
            assertTrue(box.y() + 460 * box.scale() <= size[1]);
        }
        assertEquals(0, SaveMenu.layout(0, 500).scale());
        assertEquals(0, SaveMenu.layout(500, 0).scale());
        assertEquals(0, SaveMenu.sliderPercent(-200));
        assertEquals(100, SaveMenu.sliderPercent(1000));
    }
    /** Slot deletion is cancellable, isolated and never replaces the running player.
     * @throws IOException if temporary files cannot be inspected
     */
    @Test void deletionUsesSubmenuAndKeepsOtherSlot() throws IOException {
        var menu = menu(); saves.saveManual();
        byte[] manual = Files.readAllBytes(store.path(SaveSlot.MANUAL));
        Files.writeString(store.path(SaveSlot.AUTO), "broken save");
        session.getPlayer().takeDamage(25);
        press(menu, Input.Keys.F9, false);
        press(menu, Input.Keys.DOWN, false); press(menu, Input.Keys.ENTER, false);
        press(menu, Input.Keys.ESCAPE, false);
        assertTrue(Files.exists(store.path(SaveSlot.AUTO)));
        press(menu, Input.Keys.ENTER, false); press(menu, Input.Keys.ENTER, false);
        assertFalse(Files.exists(store.path(SaveSlot.AUTO)));
        assertArrayEquals(manual, Files.readAllBytes(store.path(SaveSlot.MANUAL)));
        assertEquals(75, session.getPlayer().getHealth().getCurrentHp());
        assertTrue(menu.isOpen());
        assertTrue(menu.notification().startsWith("Deleted AUTO"));
        for (int i = 0; i < 3; i++) { press(menu, Input.Keys.DOWN, false); }
        press(menu, Input.Keys.ENTER, false); press(menu, Input.Keys.ENTER, false);
        assertFalse(Files.exists(store.path(SaveSlot.MANUAL)));
        assertEquals(75, session.getPlayer().getHealth().getCurrentHp());
        press(menu, Input.Keys.ESCAPE, false); assertTrue(menu.isOpen());
        press(menu, Input.Keys.ESCAPE, false); assertFalse(menu.isOpen());
    }

    /** Safe-state enforcement and disk errors report failure without altering another slot.
     * @throws IOException if temporary files cannot be inspected
     */
    @Test void deletionFailuresAndEmptySlotsAreSafe() throws IOException {
        menu(); saves.saveManual();
        byte[] manual = Files.readAllBytes(store.path(SaveSlot.MANUAL));
        session.switchMap(MapCatalog.TRAINING);
        assertFalse(saves.deleteSlot(SaveSlot.MANUAL));
        assertArrayEquals(manual, Files.readAllBytes(store.path(SaveSlot.MANUAL)));
        session.switchMap(MapCatalog.HUB);
        assertTrue(saves.deleteSlot(SaveSlot.AUTO));
        assertTrue(saves.status().contains("already empty"));
        Files.createDirectory(store.path(SaveSlot.AUTO));
        assertFalse(saves.deleteSlot(SaveSlot.AUTO));
        assertTrue(saves.status().startsWith("Delete failed"));
        assertTrue(Files.isDirectory(store.path(SaveSlot.AUTO)));
        assertArrayEquals(manual, Files.readAllBytes(store.path(SaveSlot.MANUAL)));
    }

    /** Mouse navigation uses scaled submenu and confirmation bounds.
     * @throws IOException if temporary files cannot be inspected
     */
    @Test void resizedMouseCanManageAndCancelDeletion() throws IOException {
        var menu = menu(); saves.saveManual();
        var box = SaveMenu.layout(480, 270);
        press(menu, Input.Keys.ESCAPE, false);
        frame(menu, -1, false, 480, 270,
                Math.round(box.x() + 100 * box.scale()),
                Math.round(270 - box.y() - 260 * box.scale()), true, false);
        frame(menu, -1, false, 480, 270,
                Math.round(box.x() + 100 * box.scale()),
                Math.round(270 - box.y() - 210 * box.scale()), true, false);
        frame(menu, -1, false, 480, 270,
                Math.round(box.x() + 100 * box.scale()),
                Math.round(270 - box.y() - 60 * box.scale()), true, false);
        assertTrue(Files.exists(store.path(SaveSlot.MANUAL)));
        press(menu, Input.Keys.ENTER, false); press(menu, Input.Keys.ENTER, false);
        assertFalse(Files.exists(store.path(SaveSlot.MANUAL)));
        assertTrue(menu.isOpen());
    }

}
