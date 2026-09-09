package au.edu.anu.comp2120.player;

import java.util.Objects;

/**
 * Composes class identity with immutable common-slot tuning and attack geometry strategies.
 * Does not own HP, movement, cooldown clocks, input or mutable invocation state.
 * @param type stable class identity
 * @param abilities complete common ability configuration
 * @author Daoyan Zhu (u7782042)
 */
public record ClassDefinition(PlayerClass type, AbilityLoadout abilities) {
    /**
     * Validates a complete definition before initialization or selection.
     * @param type class identity
     * @param abilities complete validated ability loadout
     * @throws NullPointerException if either argument is null
     */
    public ClassDefinition {
        Objects.requireNonNull(type, "class type");
        Objects.requireNonNull(abilities, "abilities");
    }
}
