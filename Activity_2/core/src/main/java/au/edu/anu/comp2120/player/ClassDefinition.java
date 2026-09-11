package au.edu.anu.comp2120.player;

import au.edu.anu.comp2120.combat.AttackSpec;
import au.edu.anu.comp2120.combat.ElementType;
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

    /**
     * Copies this definition with one debug element on J/K/O, preserving all geometry, timing and dash data.
     * The running controller must still approve selection; this method does not reset gameplay state.
     * @param element requested element, including NONE
     * @return immutable definition sharing unchanged tuning
     * @throws NullPointerException if element is null
     */
    public ClassDefinition withElement(ElementType element) {
        Objects.requireNonNull(element, "element");
        return new ClassDefinition(type, new AbilityLoadout(tag(abilities.light(), element),
                tag(abilities.special(), element), abilities.dash(), tag(abilities.ultimate(), element)));
    }

    /**
     * Copies only elemental attack metadata without dropping projectile or shape configuration.
     * @param slot existing attack slot
     * @param element validated element
     * @return tagged slot retaining its previous delivery mode
     */
    private static AbilityLoadout.AreaAttack tag(AbilityLoadout.AreaAttack slot, ElementType element) {
        AttackSpec spec = slot.attack();
        return new AbilityLoadout.AreaAttack(new AttackSpec(spec.type(), spec.damage(), spec.activeSeconds(),
                spec.cooldownSeconds(), element), slot.range(), slot.shape(), slot.projectile());
    }
}
