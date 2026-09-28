package au.edu.anu.comp2120.screen;

import au.edu.anu.comp2120.player.DebugOptions;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Exercises the production console input model without requiring a native window.
 * @author Hao Liu (u7998420)
 */
final class DebugConsoleTest {
    /** Creates isolated fixtures. */
    DebugConsoleTest() { }
    /** Captures and restores the prior processor, submits edits once and exposes matching colours. */
    @Test void typingToggleColourAndOwnership() {
        var previous = new InputAdapter();
        var owner = new AtomicReference<InputProcessor>(previous);
        var console = new DebugConsole(); var options = new DebugOptions();
        assertFalse(console.keyTyped('g'));
        console.open(input(owner), options); assertSame(console, owner.get());
        console.open(input(owner), options);
        for (char c : "gox\bd".toCharArray()) { assertTrue(owner.get().keyTyped(c)); }
        owner.get().keyTyped('\n'); // Enter is handled by the screen, not embedded in the command.
        console.submit(options); assertTrue(options.god());
        assertTrue(console.text(options).contains("[#55EE88]god (ON)"));
        assertTrue(console.text(options).endsWith("> _"));
        console.submit(options); assertTrue(options.god()); // Cleared input cannot toggle twice.
        for (char c : "god".toCharArray()) { console.keyTyped(c); }
        console.submit(options); assertFalse(options.god());
        assertTrue(console.text(options).contains("[WHITE]god (OFF)"));
        console.close(); console.close(); assertSame(previous, owner.get());
        assertFalse(console.isOpen()); assertFalse(console.keyTyped('x'));
    }
    /** Typed markup is escaped, control keys ignored and command length bounded. */
    @Test void boundedInputAndMarkupSafety() {
        var console = new DebugConsole(); var options = new DebugOptions();
        var owner = new AtomicReference<InputProcessor>();
        console.open(input(owner), options);
        console.keyTyped('`'); console.keyTyped('\b'); console.keyTyped('\r');
        for (char c : "[GREEN]".toCharArray()) { console.keyTyped(c); }
        assertTrue(console.text(options).contains("> [[GREEN]_"));
        console.close(); assertNull(owner.get()); console.open(input(owner), options);
        for (int i = 0; i < 100; i++) { console.keyTyped('x'); }
        assertTrue(console.text(options).endsWith("> " + "x".repeat(80) + "_"));
        console.submit(options); assertFalse(options.blocksSaving());
        console.close();
    }
    /** Supplies only processor ownership; native text-dialog calls would fail the test.
     * @param owner current processor
     * @return minimal input fake
     */
    private Input input(AtomicReference<InputProcessor> owner) {
        return (Input) Proxy.newProxyInstance(Input.class.getClassLoader(), new Class<?>[] {Input.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getInputProcessor" -> owner.get();
                    case "setInputProcessor" -> { owner.set((InputProcessor) args[0]); yield null; }
                    default -> throw new AssertionError("Unexpected input API: " + method.getName());
                });
    }
}
