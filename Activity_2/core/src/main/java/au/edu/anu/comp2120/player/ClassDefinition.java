package au.edu.anu.comp2120.player;

import au.edu.anu.comp2120.combat.AttackSpec;
import au.edu.anu.comp2120.combat.ElementType;
import java.util.Objects;

/**
 * Composes class identity with immutable common-slot tuning and attack geometry strategies.
 * Does not own HP, movement, cooldown clocks, input or mutable invocation state.
 * @param type stable class identity
 * @param abilities complete common ability configuration
 * @param alternateUltimate mode B configuration; mode A uses the loadout ultimate
 * @author Daoyan Zhu (u7782042)
 */
public record ClassDefinition(PlayerClass type, AbilityLoadout abilities, AbilityLoadout.AreaAttack alternateUltimate) {
    /**
     * Preserves custom loadouts without a distinct alternate ultimate.
     * @param type class identity
     * @param abilities common loadout, also supplying the fallback mode B
     * @throws NullPointerException if either argument is null
     */
    public ClassDefinition(PlayerClass type, AbilityLoadout abilities) {
        this(type, abilities, Objects.requireNonNull(abilities, "abilities").ultimate());
    }
    /**
     * Validates a complete definition before initialization or selection.
     * @param type class identity
     * @param abilities complete validated ability loadout
     * @param alternateUltimate mode B tuning
     * @throws NullPointerException if either argument is null
     */
    public ClassDefinition {
        Objects.requireNonNull(type, "class type");
        Objects.requireNonNull(abilities, "abilities");
        Objects.requireNonNull(alternateUltimate, "alternate ultimate");
    }

    /**
     * Resolves selected O tuning without changing persistent selection.
     * @param mode selected mode
     * @return immutable ultimate definition
     * @throws NullPointerException if mode is null
     */
    public AbilityLoadout.AreaAttack ultimate(UltimateMode mode) {
        return Objects.requireNonNull(mode, "mode") == UltimateMode.A ? abilities.ultimate() : alternateUltimate;
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
                tag(abilities.special(), element), abilities.dash(), tag(abilities.ultimate(), element)),
                tag(alternateUltimate, element));
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
                spec.cooldownSeconds(), element, spec.blockable(), spec.damageType()), slot.range(), slot.shape(), slot.projectile(), slot.kind());
    }
}
