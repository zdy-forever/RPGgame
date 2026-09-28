package au.edu.anu.comp2120.player;

/** Per-game, non-persistent testing switches and sticky save protection.
 * @author Hao Liu (u7998420)
 */
public final class DebugOptions {
    /** Damage immunity. */
    private boolean god;
    /** Material-free upgrades. */
    private boolean free;
    /** Lethal valid player hits. */
    private boolean oneshot;
    /** Suppresses ability cooldowns without cancelling active actions. */
    private boolean nocd;
    /** Once cheats run, turning them off cannot make changed progress safe to save. */
    private boolean used;
    /** Creates disabled switches for a new game. */
    public DebugOptions() { }
    /** Reads damage immunity.
     * @return whether positive damage is ignored
     */
    public boolean god() { return god; }
    /** Reads free upgrade mode.
     * @return whether material requirements are bypassed
     */
    public boolean freeUpgrade() { return free; }
    /** Reads lethal-hit mode.
     * @return whether damaging player hits kill enemy receivers
     */
    public boolean oneShot() { return oneshot; }
    /** Reads zero-cooldown mode.
     * @return whether ability cooldowns are disabled
     */
    public boolean noCooldown() { return nocd; }
    /** Reads sticky save protection.
     * @return whether this game has enabled a cheat
     */
    public boolean blocksSaving() { return used; }
    /** Runs a complete command, rejecting malformed commands without changing state.
     * @param command entered text
     * @return user feedback
     */
    public String execute(String command) {
        String text = command == null ? "" : command.trim().toLowerCase(java.util.Locale.ROOT);
        if (text.equals("help")) { return "Toggle: god | freeupgrade | oneshot | nocd. Also: status | debug off. Explicit on/off supported."; }
        if (text.equals("debug off")) { god = false; free = false; oneshot = false; nocd = false; return status(); }
        if (text.equals("status")) { return status(); }
        String[] parts = text.split("\\s+");
        if (parts.length > 2 || (parts.length == 2 && !parts[1].equals("on") && !parts[1].equals("off"))
                || (!parts[0].equals("god") && !parts[0].equals("freeupgrade")
                    && !parts[0].equals("oneshot") && !parts[0].equals("nocd"))) {
            return "Unknown command. Use help.";
        }
        boolean current = switch (parts[0]) {
            case "god" -> god;
            case "freeupgrade" -> free;
            case "oneshot" -> oneshot;
            case "nocd" -> nocd;
            default -> false;
        };
        boolean enabled = parts.length == 1 ? !current : parts[1].equals("on");
        switch (parts[0]) {
            case "god" -> god = enabled;
            case "freeupgrade" -> free = enabled;
            case "oneshot" -> oneshot = enabled;
            case "nocd" -> nocd = enabled;
            default -> throw new IllegalStateException("Unvalidated debug command");
        }
        used |= enabled;
        return status();
    }
    /** Formats current switches and persistence policy.
     * @return readable status
     */
    public String status() {
        return "God: " + god + " | Free upgrades: " + free
                + " | One-shot: " + oneshot + " | No CD: " + nocd
                + (used ? " | Saving disabled until a new game/restart." : " | Saving allowed.");
    }
}
