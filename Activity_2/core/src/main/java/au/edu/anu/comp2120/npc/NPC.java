package au.edu.anu.comp2120.npc;

/**
 * Non-player character with dialogue and item reward.
 * @author Jiaqi Chen (u7851266)
 */
public final class NPC {
    private final String id;
    private final String name;
    private final double x, y;
    private final double width, height;
    private final String dialogueText;
    private final String rewardItemId;
    private boolean interacted;

    public NPC(String id, String name, double x, double y, double w, double h, String text, String reward) {
        this.id = id; this.name = name; this.x = x; this.y = y;
        this.width = w; this.height = h; this.dialogueText = text; this.rewardItemId = reward;
        this.interacted = false;
    }

    public NPC() { this("dummy", "Unknown", 0,0,0,0,"",null); }

    public String getId() { return id; }
    public String getName() { return name; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public String getDialogueText() { return dialogueText; }
    public String getRewardItemId() { return rewardItemId; }
    public boolean hasInteracted() { return interacted; }
    public void setInteracted() { this.interacted = true; }

    public boolean isAdjacent(double px, double py, double range) {
        return Math.abs(px - x) <= range && Math.abs(py - y) <= range;
    }
}