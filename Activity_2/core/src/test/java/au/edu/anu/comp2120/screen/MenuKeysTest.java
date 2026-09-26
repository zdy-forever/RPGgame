package au.edu.anu.comp2120.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import java.lang.reflect.Proxy;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Verifies edge-triggered aliases without sampling held gameplay movement.
 * @author Hao Liu (u7998420)
 */
final class MenuKeysTest {
    /** Creates isolated tests. */
    MenuKeysTest() { }

    /** Both key families navigate and simultaneous opposite paging cancels out. */
    @Test void aliasesAndPaging() {
        frame(Set.of(Input.Keys.W, Input.Keys.D, Input.Keys.E), () -> {
            assertTrue(MenuKeys.up()); assertTrue(MenuKeys.right()); assertTrue(MenuKeys.confirm());
            assertFalse(MenuKeys.down()); assertFalse(MenuKeys.left()); assertEquals(0, MenuKeys.page());
        });
        frame(Set.of(Input.Keys.DOWN, Input.Keys.LEFT, Input.Keys.ENTER, Input.Keys.PAGE_DOWN), () -> {
            assertTrue(MenuKeys.down()); assertTrue(MenuKeys.left()); assertTrue(MenuKeys.confirm());
            assertEquals(1, MenuKeys.page());
        });
        frame(Set.of(Input.Keys.Q, Input.Keys.R, Input.Keys.E, Input.Keys.ENTER), () -> {
            assertEquals(0, MenuKeys.page()); assertTrue(MenuKeys.confirm());
        });
        frame(Set.of(), () -> {
            assertFalse(MenuKeys.up()); assertFalse(MenuKeys.confirm()); assertEquals(0, MenuKeys.page());
        });
    }

    /** Installs only edge input; querying held movement fails this test.
     * @param edges keys newly pressed in this frame
     * @param assertions checks under the fake input
     */
    private void frame(Set<Integer> edges, Runnable assertions) {
        Input old = Gdx.input;
        Gdx.input = (Input) Proxy.newProxyInstance(Input.class.getClassLoader(), new Class<?>[] {Input.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("isKeyJustPressed")) { return edges.contains((Integer) args[0]); }
                    throw new AssertionError("Unexpected input read: " + method.getName());
                });
        try { assertions.run(); } finally { Gdx.input = old; }
    }
}
