package au.edu.anu.comp2120.npc;

import java.util.List;

/**
 * Non-player character with a position, one or more dialogue lines and a one-time item reward.
 * World coordinates use a bottom-left origin; x points right and y points up.
 *
 * @author Jiaqi Chen (u7851266)
 */
public final class NPC {

    /** Unique identifier, stable across the process. */
    private final String id;
    /** Display name shown as the dialogue speaker. */
    private final String name;
    /** Lower-left x in world units. */
    private final double x;
    /** Lower-left y in world units. */
    private final double y;
    /** Body width in world units. */
    private final double width;
    /** Body height in world units. */
    private final double height;
    /** Ordered script advanced one line per interaction. */
    private final List<String> lines;
    /** First or only line, kept for backwards compatibility. */
    private final String dialogueText;
    /** Item granted once, or null when this NPC gives nothing. */
    private final String rewardItemId;
    /** Whether the one-time reward has already been taken. */
    private boolean interacted;

    /**
     * Creates an NPC with a multi-line dialogue script.
     *
     * @param id unique identifier
     * @param name display name shown in the dialogue box
     * @param x lower-left x in world units
     * @param y lower-left y in world units
     * @param width body width in world units
     * @param height body height in world units
     * @param lines ordered dialogue lines, shown one per key press
     * @param rewardItemId item granted once on completion, or null
     */
    public NPC(String id, String name, double x, double y, double width, double height,
               List<String> lines, String rewardItemId) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.lines = lines == null ? List.of() : List.copyOf(lines);
        this.dialogueText = this.lines.isEmpty() ? "" : this.lines.get(0);
        this.rewardItemId = rewardItemId;
        this.interacted = false;
    }

    /**
     * Creates an NPC with a single dialogue line.
     *
     * @param id unique identifier
     * @param name display name shown in the dialogue box
     * @param x lower-left x in world units
     * @param y lower-left y in world units
     * @param width body width in world units
     * @param height body height in world units
     * @param dialogueText the only line this NPC speaks
     * @param rewardItemId item granted once on completion, or null
     */
    public NPC(String id, String name, double x, double y, double width, double height,
               String dialogueText, String rewardItemId) {
        this(id, name, x, y, width, height,
                dialogueText == null || dialogueText.isEmpty()
                        ? List.of() : List.of(dialogueText),
                rewardItemId);
    }

    /** Creates an empty placeholder NPC with no position, script or reward. */
    public NPC() {
        this("dummy", "Unknown", 0, 0, 0, 0, "", null);
    }

    /**
     * Returns the unique identifier.
     *
     * @return this NPC's id
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the display name.
     *
     * @return name shown as the dialogue speaker
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the lower-left x coordinate.
     *
     * @return x in world units
     */
    public double getX() {
        return x;
    }

    /**
     * Returns the lower-left y coordinate.
     *
     * @return y in world units
     */
    public double getY() {
        return y;
    }

    /**
     * Returns the body width.
     *
     * @return width in world units
     */
    public double getWidth() {
        return width;
    }

    /**
     * Returns the body height.
     *
     * @return height in world units
     */
    public double getHeight() {
        return height;
    }

    /**
     * Returns the ordered dialogue script.
     *
     * @return immutable list of lines, empty when this NPC has no script
     */
    public List<String> getLines() {
        return lines;
    }

    /**
     * Returns the first or only dialogue line.
     *
     * @return the legacy single-line text
     */
    public String getDialogueText() {
        return dialogueText;
    }

    /**
     * Returns the item granted once.
     *
     * @return reward item id, or null
     */
    @SuppressWarnings("unused") // Public API reserved for Inventory integration
    public String getRewardItemId() {
        return rewardItemId;
    }

    /**
     * Reports whether the reward has already been taken.
     *
     * @return true once the dialogue has been completed
     */
    public boolean hasInteracted() {
        return interacted;
    }

    /** Marks the one-time reward as taken. */
    public void setInteracted() {
        this.interacted = true;
    }

    /**
     * Checks whether a point lies within interaction range on both axes.
     *
     * @param px point x in world units
     * @param py point y in world units
     * @param range maximum distance per axis
     * @return true when the point is close enough to interact
     */
    public boolean isAdjacent(double px, double py, double range) {
        return Math.abs(px - x) <= range && Math.abs(py - y) <= range;
    }
}