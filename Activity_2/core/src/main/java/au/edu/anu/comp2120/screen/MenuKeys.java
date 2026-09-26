package au.edu.anu.comp2120.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

/** Edge-triggered UI aliases, sampled only while a modal owns input.
 * @author Hao Liu (u7998420)
 */
final class MenuKeys {
    /** Prevents construction. */
    private MenuKeys() { }
    /** Reads upward selection.
     * @return W or Up edge
     */
    static boolean up() { return pressed(Input.Keys.W, Input.Keys.UP); }
    /** Reads downward selection.
     * @return S or Down edge
     */
    static boolean down() { return pressed(Input.Keys.S, Input.Keys.DOWN); }
    /** Reads leftward selection.
     * @return A or Left edge
     */
    static boolean left() { return pressed(Input.Keys.A, Input.Keys.LEFT); }
    /** Reads rightward selection.
     * @return D or Right edge
     */
    static boolean right() { return pressed(Input.Keys.D, Input.Keys.RIGHT); }
    /** Reads activation once, regardless of simultaneous aliases.
     * @return E or Enter edge
     */
    static boolean confirm() { return pressed(Input.Keys.E, Input.Keys.ENTER); }
    /** Reads pagination without reusing column navigation.
     * @return minus one, zero or one
     */
    static int page() {
        return (pressed(Input.Keys.R, Input.Keys.PAGE_DOWN) ? 1 : 0)
                - (pressed(Input.Keys.Q, Input.Keys.PAGE_UP) ? 1 : 0);
    }
    /** Reads either alias.
     * @param first primary key
     * @param second alias
     * @return whether either edge occurred
     */
    private static boolean pressed(int first, int second) {
        return Gdx.input.isKeyJustPressed(first) || Gdx.input.isKeyJustPressed(second);
    }
}
