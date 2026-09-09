package au.edu.anu.comp2120.player;

/**
 * Stable class-independent ability slots; keyboard bindings live in PlayerInput.
 * @author Daoyan Zhu (u7782042)
 */
public enum AbilitySlot {
    /** Basic repeatable attack. */
    LIGHT,
    /** Secondary class ability entry point. */
    SPECIAL,
    /** Collision-constrained movement burst. */
    DASH,
    /** Long-cooldown ability entry point. */
    ULTIMATE
}
