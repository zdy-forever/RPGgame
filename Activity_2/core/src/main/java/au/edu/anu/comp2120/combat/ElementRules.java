package au.edu.anu.comp2120.combat;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Central immutable reaction table, not a second damage engine. Matching is symmetric and pure.
 * Reactions consume attachment; unknown pairs replace it with the incoming element.
 * @param attachmentSeconds positive finite duration refreshed by an ordinary elemental hit
 * @param reactions distinct unordered pair rules, defensively copied
 * @author Daoyan Zhu (u7782042)
 */
public record ElementRules(double attachmentSeconds, List<Reaction> reactions) {
    /** Explicit provisional rules, pending an agreed final game-design reaction table. */
    public static final ElementRules PROTOTYPE = new ElementRules(3, List.of(
            new Reaction("vaporize", ElementType.WATER, ElementType.FIRE, 10, null),
            new Reaction("discharge", ElementType.WATER, ElementType.LIGHTNING, 0, new DerivedEffect(5, 40, 0.15))));

    /**
     * Validates timing and rejects duplicate/reversed definitions instead of ambiguous precedence.
     * @param attachmentSeconds positive finite attachment duration
     * @param reactions required rule list, possibly empty
     * @throws NullPointerException if rules or any entry is null
     * @throws IllegalArgumentException if timing or unordered pair uniqueness is invalid
     */
    public ElementRules {
        reactions = List.copyOf(reactions);
        if (!Double.isFinite(attachmentSeconds) || attachmentSeconds <= 0) {
            throw new IllegalArgumentException("Attachment duration must be positive and finite");
        }
        for (int i = 0; i < reactions.size(); i++) {
            Reaction current = reactions.get(i);
            for (int j = 0; j < i; j++) {
                Reaction earlier = reactions.get(j);
                if (earlier.matches(current.first(), current.second()) || earlier.id().equals(current.id())) {
                    throw new IllegalArgumentException("Reaction pairs and IDs must be unique");
                }
            }
        }
    }

    /**
     * Looks up a compatible pair without mutating attachment, attacks or health.
     * @param attached existing attachment
     * @param incoming element carried by the confirmed hit
     * @return matching rule or empty, including for NONE and same-element pairs
     * @throws NullPointerException if either element is null
     */
    public Optional<Reaction> find(ElementType attached, ElementType incoming) {
        Objects.requireNonNull(attached, "attached element");
        Objects.requireNonNull(incoming, "incoming element");
        return reactions.stream().filter(rule -> rule.matches(attached, incoming)).findFirst();
    }

    /**
     * Declarative result of a compatible unordered pair; combat-core performs actual settlement.
     * @param id stable non-blank reaction label
     * @param first first non-NONE element
     * @param second different non-NONE element
     * @param bonusDamage non-negative addition to the triggering hit's base damage
     * @param derived optional one-generation area effect, null for bonus-only reactions
     * @author Daoyan Zhu (u7782042)
     */
    public record Reaction(String id, ElementType first, ElementType second, int bonusDamage, DerivedEffect derived) {
        /**
         * Validates one rule without hard-coding class-specific behavior.
         * @param id non-blank rule ID
         * @param first non-NONE element
         * @param second distinct non-NONE element
         * @param bonusDamage non-negative bonus
         * @param derived optional area-effect configuration
         * @throws NullPointerException if identity or elements are null
         * @throws IllegalArgumentException if pair, label or damage is invalid
         */
        public Reaction {
            Objects.requireNonNull(id, "reaction ID");
            Objects.requireNonNull(first, "first element");
            Objects.requireNonNull(second, "second element");
            if (id.isBlank() || first == ElementType.NONE || second == ElementType.NONE || first == second || bonusDamage < 0) {
                throw new IllegalArgumentException("Reaction requires distinct non-NONE elements and non-negative damage");
            }
        }

        /**
         * Tests a pair in either arrival order.
         * @param attached current attachment
         * @param incoming incoming element
         * @return true for either ordering of this rule's pair
         */
        private boolean matches(ElementType attached, ElementType incoming) {
            return (first == attached && second == incoming) || (second == attached && first == incoming);
        }
    }

    /**
     * One non-elemental rectangular child attack centred on the reacting target's hurtbox.
     * @param damage non-negative raw child damage
     * @param radius positive finite half-width/half-height of the resulting square
     * @param activeSeconds positive finite active duration, starting at the next combat update
     * @author Daoyan Zhu (u7782042)
     */
    public record DerivedEffect(int damage, double radius, double activeSeconds) {
        /**
         * Validates bounded geometry and lifecycle tuning.
         * @param damage non-negative damage
         * @param radius finite positive square half-extent
         * @param activeSeconds finite positive duration
         * @throws IllegalArgumentException if damage, geometry or duration is invalid
         */
        public DerivedEffect {
            if (damage < 0 || !Double.isFinite(radius) || radius <= 0 || !Double.isFinite(2 * radius)
                    || !Double.isFinite(activeSeconds) || activeSeconds <= 0) {
                throw new IllegalArgumentException("Derived effect requires legal damage, range and duration");
            }
        }
    }
}
