package au.edu.anu.comp2120.player;

/**
 * Stable class identity, independent of artwork and the concrete Player implementation.
 * Use the explicit ID rather than enum ordinals for future external configuration.
 * @author Daoyan Zhu (u7782042)
 */
public enum PlayerClass {
    /** Bow-oriented class using shared straight-projectile prototypes. */
    ARCHER("archer", "Archer"),
    /** Magic-oriented class; spells and elements are future work. */
    MAGE("mage", "Mage"),
    /** Sword-oriented class; weapon combos are future work. */
    KATANA("katana", "Katana");

    /** Stable machine-readable identifier. */
    private final String id;
    /** Human-readable debug label, not an asset name. */
    private final String displayName;

    /**
     * Defines a built-in identity.
     * @param id stable lower-case ID
     * @param displayName display label
     */
    PlayerClass(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    /**
     * Returns the stable class identifier.
     * @return machine-readable ID
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the class label used in debug feedback.
     * @return display name
     */
    public String getDisplayName() {
        return displayName;
    }
}
