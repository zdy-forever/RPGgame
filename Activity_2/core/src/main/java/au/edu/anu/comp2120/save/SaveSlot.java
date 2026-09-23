package au.edu.anu.comp2120.save;

/** The two independent save destinations; neither is a fallback for the other.
 * @author Hao Liu (u7998420)
 */
public enum SaveSlot {
    /** Replaced only by a successful Boss-victory checkpoint. */
    AUTO("automatic.properties"),
    /** Replaced only by an explicit player save request. */
    MANUAL("manual.properties");

    /** Fixed filename prevents callers from selecting arbitrary relative paths. */
    private final String fileName;

    /** Defines one slot's separate disk file.
     * @param fileName fixed filename inside the save directory
     */
    SaveSlot(String fileName) { this.fileName = fileName; }

    /** Reads the filename assigned to this slot.
     * @return slot-specific filename
     */
    public String fileName() { return fileName; }
}
