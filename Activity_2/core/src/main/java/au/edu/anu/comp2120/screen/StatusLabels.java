package au.edu.anu.comp2120.screen;

import au.edu.anu.comp2120.combat.CombatStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Chooses the readable status labels shown above one actor, independent of graphics.
 * Generic control, vulnerability and immunity states are read from the shared CombatStatus, so
 * both the player and a guardian report them. An attacker-owned affliction, such as burning stacks
 * held by a Boss trait, is supplied by its owner because no shared status models it.
 * Control effects come first because they decide what the player may do next, and an immunity
 * window is listed too: without it, negated hits read as a bug rather than as a mechanic.
 * @author Daoyan Zhu (u7782042)
 */
public final class StatusLabels {
    /** Prevents construction of this label catalogue. */
    private StatusLabels() {
    }

    /**
     * One prominent label and its colour, both independent of any drawing API.
     * @param text uppercase English label
     * @param red red component in [0, 1]
     * @param green green component in [0, 1]
     * @param blue blue component in [0, 1]
     * @author Daoyan Zhu (u7782042)
     */
    public record Tag(String text, float red, float green, float blue) {
        /**
         * Validates a non-blank label with representable colour components.
         * @param text uppercase English label
         * @param red red component in [0, 1]
         * @param green green component in [0, 1]
         * @param blue blue component in [0, 1]
         * @throws NullPointerException if text is null
         * @throws IllegalArgumentException if the label is blank or a component is out of range
         */
        public Tag {
            Objects.requireNonNull(text, "label text");
            if (text.isBlank() || red < 0 || red > 1 || green < 0 || green > 1 || blue < 0 || blue > 1) {
                throw new IllegalArgumentException("Status tag needs a label and components in [0, 1]");
            }
        }
    }

    /**
     * Lists the labels currently applying to one actor, in the order they should be stacked.
     * A blank affliction contributes nothing, so actors without an attacker-owned effect are
     * described by their shared status alone.
     * @param status scope-local control, vulnerability and immunity state of the actor
     * @param affliction attacker-owned label for this actor, or empty when none applies
     * @return immutable ordered labels, empty when the actor has no negative status
     * @throws NullPointerException if status or affliction is null
     */
    public static List<Tag> forActor(CombatStatus status, String affliction) {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(affliction, "affliction");
        List<Tag> tags = new ArrayList<>(4);
        if (status.isFrozen()) {
            tags.add(new Tag("FROZEN", 0.55f, 0.85f, 1f));
        }
        if (status.getStaggerRemaining() > 0) {
            tags.add(new Tag("STAGGERED", 1f, 0.75f, 0.30f));
        }
        if (!affliction.isBlank()) {
            tags.add(new Tag(affliction, 1f, 0.45f, 0.15f));
        }
        if (status.getChargedRemaining() > 0) {
            tags.add(new Tag("CHARGED", 0.95f, 0.90f, 0.35f));
        }
        if (status.getResistanceRemaining() > 0) {
            tags.add(new Tag("VULNERABLE", 0.80f, 0.55f, 1f));
        }
        if (status.isInvulnerable()) {
            tags.add(new Tag("INVULNERABLE", 0.75f, 0.95f, 0.95f));
        }
        return List.copyOf(tags);
    }
}
