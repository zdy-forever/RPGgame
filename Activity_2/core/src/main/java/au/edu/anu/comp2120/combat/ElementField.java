package au.edu.anu.comp2120.combat;

import au.edu.anu.comp2120.world.CollisionBox;
import java.util.Objects;

/**
 * Fixed-world ground reaction field with bounded lifetime and direct-attack provenance.
 * Only CombatSystem advances it and submits its ticks through the ordinary hit pipeline.
 * @author Daoyan Zhu (u7782042)
 */
public final class ElementField {
    /** Validated reusable ground effect data. */ private final Definition definition;
    /** Captured square geometry; never tracks the reacting target. */ private final CollisionBox bounds;
    /** Original canonical attacker. */ private final Combatant source;
    /** Direct invocation that caused this field. */ private final long parentAttackId;
    /** Finite schedule, private to this field. */ private final ReactionTimer timer;
    /**
     * Immutable field balance/element independent of runtime position.
     * @param element non-NONE damage element
     * @param damage non-negative base damage per tick
     * @param radius positive finite square half-extent
     * @param duration finite lifetime
     * @param tickInterval finite positive spacing
     * @author Daoyan Zhu (u7782042)
     */
    public record Definition(ElementType element, int damage, double radius, double duration, double tickInterval) {
        /**
         * Validates geometry, element and bounded timing.
         * @param element non-NONE tag
         * @param damage base tick damage
         * @param radius square half-extent
         * @param duration lifetime
         * @param tickInterval tick spacing
         * @throws NullPointerException if element is null
         * @throws IllegalArgumentException if element, damage, geometry or timing is invalid
         */
        public Definition {
            Objects.requireNonNull(element, "field element");
            if (element == ElementType.NONE || damage < 0 || !Double.isFinite(radius) || radius <= 0
                    || !Double.isFinite(radius * 2)) {
                throw new IllegalArgumentException("Field requires non-NONE element and valid damage/geometry");
            }
            new ReactionTimer(duration, tickInterval);
        }
    }
    /**
     * Captures a validated direct reaction's location.
     * @param definition immutable field data
     * @param x fixed centre x
     * @param y fixed centre y
     * @param source original attacker
     * @param parentAttackId positive direct invocation ID
     * @throws NullPointerException if definition or source is null
     * @throws IllegalArgumentException if provenance or geometry is invalid
     */
    ElementField(Definition definition, double x, double y, Combatant source, long parentAttackId) {
        this.definition = Objects.requireNonNull(definition, "definition");
        this.source = Objects.requireNonNull(source, "source");
        if (parentAttackId <= 0) { throw new IllegalArgumentException("Field requires direct provenance"); }
        this.parentAttackId = parentAttackId;
        bounds = new CollisionBox(x - definition.radius(), y - definition.radius(),
                2 * definition.radius(), 2 * definition.radius());
        timer = new ReactionTimer(definition.duration(), definition.tickInterval());
    }
    /**
     * Exposes immutable tuning.
     * @return field definition
     */
    public Definition getDefinition() { return definition; }
    /**
     * Exposes fixed collision geometry.
     * @return captured square
     */
    public CollisionBox getBounds() { return bounds; }
    /**
     * Exposes originator identity.
     * @return canonical source
     */
    public Combatant getSource() { return source; }
    /**
     * Exposes direct provenance.
     * @return positive parent invocation ID
     */
    public long getParentAttackId() { return parentAttackId; }
    /**
     * Observes finite lifetime.
     * @return seconds until expiry
     */
    public double getRemainingSeconds() { return timer.remaining(); }
    /**
     * Reports whether future simulation time remains.
     * @return true before expiry/cancellation
     */
    public boolean isActive() { return timer.remaining() > 0; }
    /**
     * Consumes newly due tick slots; rejected submissions are not retried.
     * @param delta valid elapsed seconds
     * @return bounded newly due count
     * @throws IllegalArgumentException if delta is invalid
     */
    int advance(double delta) { return timer.advance(delta); }
    /** Ends the field, including retained observational references. */
    void cancel() { timer.cancel(); }
}
