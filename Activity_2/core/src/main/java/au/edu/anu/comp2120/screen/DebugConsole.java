package au.edu.anu.comp2120.screen;

import au.edu.anu.comp2120.player.DebugOptions;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;

/** Graphics-free console input and text shared by the screen and regression tests.
 * @author Hao Liu (u7998420)
 */
final class DebugConsole extends InputAdapter {
    /** Bounded editable command. */
    private final StringBuilder command = new StringBuilder();
    /** Last result, displayed separately from the input. */
    private String response = "";
    /** Captured input owner; null when closed. */
    private Input input;
    /** Previous processor restored on close. */
    private InputProcessor previous;
    /** Creates a closed console. */
    DebugConsole() { }
    /** Reads modal ownership.
     * @return whether open
     */
    boolean isOpen() { return input != null; }
    /** Acquires text input once and refreshes status.
     * @param source game input
     * @param options runtime switches
     */
    void open(Input source, DebugOptions options) {
        if (isOpen()) { return; }
        input = source; previous = source.getInputProcessor();
        command.setLength(0); response = options.status();
        input.setInputProcessor(this);
    }
    /** Restores input ownership; safe to repeat during disposal. */
    void close() {
        if (!isOpen()) { return; }
        input.setInputProcessor(previous); input = null; previous = null;
    }
    /** Edits printable ASCII commands without accepting control or toggle characters.
     * @param character typed character
     * @return true only while the console owns input
     */
    @Override public boolean keyTyped(char character) {
        if (!isOpen()) { return false; }
        if (character == '\b' && !command.isEmpty()) { command.deleteCharAt(command.length() - 1); }
        else if (character >= 32 && character < 127 && character != '`' && command.length() < 80) { command.append(character); }
        return true;
    }
    /** Executes the complete current command once and clears the input.
     * @param options runtime switches
     */
    void submit(DebugOptions options) {
        if (!isOpen()) { return; }
        response = options.execute(command.toString()); command.setLength(0);
    }
    /** Builds markup for the screen while escaping player-entered markup.
     * @param options live switch state
     * @return complete console text
     */
    String text(DebugOptions options) {
        return "DEBUG CONSOLE - Enter: toggle/run | Esc / F10 / `: close\n\n"
                + row("god", "ignore damage", options.god())
                + row("oneshot", "lethal enemy hits", options.oneShot())
                + row("freeupgrade", "no upgrade material cost", options.freeUpgrade())
                + row("nocd", "zero ability cooldown", options.noCooldown())
                + "\nType a command again to disable it.\nstatus | help | debug off\n\n"
                + response.replace("[", "[[") + "\n\n> " + command.toString().replace("[", "[[") + "_";
    }
    /** Formats one state row and restores colour afterwards.
     * @param name command
     * @param description purpose
     * @param enabled state
     * @return coloured row
     */
    private String row(String name, String description, boolean enabled) {
        return (enabled ? "[#55EE88]" : "[WHITE]") + name + (enabled ? " (ON)" : " (OFF)")
                + " - " + description + "[]\n";
    }
}
