package au.edu.anu.comp2120.combat;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Central immutable reaction table, not a second damage engine. Matching distinguishes attached and triggering elements.
 * Reactions consume attachment; unknown pairs replace it with the incoming element.
 * @param attachmentSeconds positive finite duration refreshed by an ordinary elemental hit
 * @param reactions distinct ordered pair rules, defensively copied
 * @author Daoyan Zhu (u7782042)
 */
public record ElementRules(double attachmentSeconds, List<Reaction> reactions) {
    /** Built-in ordered reaction table shared by all elemental combat sources. */
    public static final ElementRules DEFAULT = standard();

    /**
     * Builds the small ordered table without string-based pair dispatch.
     * @return immutable built-in rules
     */
    private static ElementRules standard() {
        java.util.ArrayList<Reaction> rules = new java.util.ArrayList<>();
        rules.add(new Reaction("vaporize", ElementType.FIRE, ElementType.WATER, 0, null, 2, Kind.DAMAGE));
        rules.add(new Reaction("vaporize", ElementType.WATER, ElementType.FIRE, 0, null, 1.5, Kind.DAMAGE));
        rules.add(new Reaction("melt", ElementType.ICE, ElementType.FIRE, 0, null, 2, Kind.DAMAGE));
        rules.add(new Reaction("melt", ElementType.FIRE, ElementType.ICE, 0, null, 1.5, Kind.DAMAGE));
        pair(rules, "overload", ElementType.FIRE, ElementType.LIGHTNING, Kind.OVERLOAD,
                new DerivedEffect(ReactionTuning.OVERLOAD_DAMAGE, ReactionTuning.OVERLOAD_RADIUS,
                        ReactionTuning.PULSE_ACTIVE, ElementType.FIRE));
        pair(rules, "electro-charged", ElementType.WATER, ElementType.LIGHTNING, Kind.ELECTRO_CHARGED, null);
        pair(rules, "freeze", ElementType.WATER, ElementType.ICE, Kind.FREEZE, null);
        pair(rules, "superconduct", ElementType.ICE, ElementType.LIGHTNING, Kind.SUPERCONDUCT,
                new DerivedEffect(ReactionTuning.SUPERCONDUCT_DAMAGE, ReactionTuning.SUPERCONDUCT_RADIUS,
                        ReactionTuning.PULSE_ACTIVE, ElementType.ICE));
        for (ElementType attached : List.of(ElementType.FIRE, ElementType.WATER, ElementType.ICE, ElementType.LIGHTNING)) {
            rules.add(new Reaction("swirl-" + attached.name().toLowerCase(java.util.Locale.ROOT),
                    attached, ElementType.WIND, 0, null, 1, Kind.SWIRL));
        }
        return new ElementRules(ReactionTuning.ATTACHMENT, rules);
    }

    /**
     * Adds both explicit orders for a symmetric behaviour.
     * @param rules destination table
     * @param id shared reaction label
     * @param a first element
     * @param b second element
     * @param kind effect behaviour
     * @param effect optional area
     */
    private static void pair(List<Reaction> rules, String id, ElementType a, ElementType b, Kind kind, DerivedEffect effect) {
        rules.add(new Reaction(id, a, b, 0, effect, 1, kind));
        rules.add(new Reaction(id, b, a, 0, effect, 1, kind));
    }

    /**
     * Small reaction outcome vocabulary, not actor-specific behaviour.
     * @author Daoyan Zhu (u7782042)
     */
    public enum Kind {
        /** Triggering damage only. */ DAMAGE,
        /** Fire area, poise loss and queued knockback. */ OVERLOAD,
        /** Target-local finite lightning ticks. */ ELECTRO_CHARGED,
        /** Temporary action suppression. */ FREEZE,
        /** Ice area and physical vulnerability. */ SUPERCONDUCT,
        /** Fixed field carrying the consumed attachment element. */ SWIRL
    }

    /**
     * Validates timing and rejects duplicate ordered definitions instead of ambiguous precedence.
     * @param attachmentSeconds positive finite attachment duration
     * @param reactions required rule list, possibly empty
     * @throws NullPointerException if rules or any entry is null
     * @throws IllegalArgumentException if timing or ordered pair uniqueness is invalid
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
                if (earlier.matches(current.attachedElement(), current.triggeringElement())) {
                    throw new IllegalArgumentException("Ordered reaction pairs must be unique");
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
     * Declarative result of a compatible ordered pair; combat-core performs actual settlement.
     * @param id stable non-blank reaction label
     * @param attachedElement existing non-NONE attachment
     * @param triggeringElement incoming different non-NONE element
     * @param bonusDamage non-negative addition to the triggering hit's base damage
     * @param derived optional one-generation area effect, null for bonus-only reactions
     * @param multiplier positive finite triggering-damage multiplier
     * @param kind outcome behaviour
     * @author Daoyan Zhu (u7782042)
     */
    public record Reaction(String id, ElementType attachedElement, ElementType triggeringElement, int bonusDamage,
            DerivedEffect derived, double multiplier, Kind kind) {
        /**
         * Creates a custom ordered bonus/child rule with an identity multiplier.
         * @param id label
         * @param attachedElement existing element
         * @param triggeringElement incoming element
         * @param bonusDamage non-negative bonus
         * @param derived optional child
         * @throws NullPointerException if identity/elements are null
         * @throws IllegalArgumentException if rule data is invalid
         */
        public Reaction(String id, ElementType attachedElement, ElementType triggeringElement,
                int bonusDamage, DerivedEffect derived) {
            this(id, attachedElement, triggeringElement, bonusDamage, derived, 1, Kind.DAMAGE);
        }
        /**
         * Validates one rule without hard-coding class-specific behavior.
         * @param id non-blank rule ID
         * @param attachedElement non-NONE existing element
         * @param triggeringElement distinct incoming element
         * @param bonusDamage non-negative bonus
         * @param derived optional area-effect configuration
         * @param multiplier positive finite damage multiplier
         * @param kind non-null effect behaviour
         * @throws NullPointerException if identity or elements are null
         * @throws IllegalArgumentException if pair, label or damage is invalid
         */
        public Reaction {
            Objects.requireNonNull(id, "reaction ID");
            Objects.requireNonNull(attachedElement, "attached element");
            Objects.requireNonNull(triggeringElement, "triggering element");
            Objects.requireNonNull(kind, "kind");
            if (id.isBlank() || attachedElement == ElementType.NONE || triggeringElement == ElementType.NONE
                    || attachedElement == triggeringElement || bonusDamage < 0 || !Double.isFinite(multiplier) || multiplier <= 0) {
                throw new IllegalArgumentException("Reaction requires distinct non-NONE elements and non-negative damage");
            }
        }

        /**
         * Tests the exact attachment/trigger ordering.
         * @param attached current attachment
         * @param incoming incoming element
         * @return true only for this ordered pair
         */
        private boolean matches(ElementType attached, ElementType incoming) {
            return attachedElement == attached && triggeringElement == incoming;
        }
    }

    /**
     * One explicitly tagged rectangular child attack centred on the reacting target's hurtbox.
     * @param damage non-negative raw child damage
     * @param radius positive finite half-width/half-height of the resulting square
     * @param element child damage element; derived provenance prevents reattachment/reaction
     * @param activeSeconds positive finite active duration, starting at the next combat update
     * @author Daoyan Zhu (u7782042)
     */
    public record DerivedEffect(int damage, double radius, double activeSeconds, ElementType element) {
        /**
         * Preserves custom untagged child definitions; all children still use ELEMENTAL damage category.
         * @param damage base damage
         * @param radius square half-extent
         * @param activeSeconds active duration
         * @throws IllegalArgumentException if tuning is invalid
         */
        public DerivedEffect(int damage, double radius, double activeSeconds) {
            this(damage, radius, activeSeconds, ElementType.NONE);
        }
        /**
         * Validates bounded geometry and lifecycle tuning.
         * @param damage non-negative damage
         * @param radius finite positive square half-extent
         * @param activeSeconds finite positive duration
         * @param element required child tag
         * @throws NullPointerException if element is null
         * @throws IllegalArgumentException if damage, geometry or duration is invalid
         */
        public DerivedEffect {
            Objects.requireNonNull(element, "child element");
            if (damage < 0 || !Double.isFinite(radius) || radius <= 0 || !Double.isFinite(2 * radius)
                    || !Double.isFinite(activeSeconds) || activeSeconds <= 0) {
                throw new IllegalArgumentException("Derived effect requires legal damage, range and duration");
            }
        }
    }
}
