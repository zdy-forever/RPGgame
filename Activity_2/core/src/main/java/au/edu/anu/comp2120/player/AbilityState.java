package au.edu.anu.comp2120.player;

/**
 * Observational ability lifecycle; triggering is an accepted event, not a lasting state.
 * @author Daoyan Zhu (u7782042)
 */
public enum AbilityState {
    /** Ready to accept a request, subject to simultaneous-input arbitration. */
    READY,
    /** An attack or dash is still active. */
    ACTIVE,
    /** The active window ended but the slot is still cooling down. */
    COOLDOWN,
    /** The owner is dead or no longer registered in the combat scope. */
    UNAVAILABLE
}
