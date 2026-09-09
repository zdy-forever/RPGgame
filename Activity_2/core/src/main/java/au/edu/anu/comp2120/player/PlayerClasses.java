package au.edu.anu.comp2120.player;

import au.edu.anu.comp2120.combat.AttackSpec;
import java.util.Map;
import java.util.Objects;

/**
 * Central built-in placeholder class definitions, without Player subclasses or asset lookups.
 * Values demonstrate class differences; they are not final skill designs or balance decisions.
 * @author Daoyan Zhu (u7782042)
 */
public final class PlayerClasses {
    /** Default preserves the previous common area-attack behaviour and tuning. */
    public static final PlayerClass DEFAULT = PlayerClass.MAGE;
    /** Complete immutable catalog; all mutable ability state stays in each player's controller. */
    private static final Map<PlayerClass, ClassDefinition> DEFINITIONS = Map.of(
            PlayerClass.ARCHER, new ClassDefinition(PlayerClass.ARCHER, new AbilityLoadout(
                    attack("archer-light", 20, 0.15, 0.45, 160, AttackShape.FORWARD),
                    attack("archer-special", 35, 0.25, 2.5, 220, AttackShape.FORWARD),
                    new AbilityLoadout.Dash(650, 0.2, 1.2),
                    attack("archer-ultimate", 65, 0.4, 8, 260, AttackShape.FORWARD))),
            PlayerClass.MAGE, new ClassDefinition(PlayerClass.MAGE, new AbilityLoadout(
                    attack("light", 25, 0.25, 0.5, 96, AttackShape.AREA),
                    attack("special", 40, 0.3, 2, 120, AttackShape.AREA),
                    new AbilityLoadout.Dash(600, 0.2, 1),
                    attack("ultimate", 75, 0.5, 8, 180, AttackShape.AREA))),
            PlayerClass.KATANA, new ClassDefinition(PlayerClass.KATANA, new AbilityLoadout(
                    attack("katana-light", 30, 0.15, 0.35, 48, AttackShape.FORWARD),
                    attack("katana-special", 45, 0.25, 2, 80, AttackShape.AREA),
                    new AbilityLoadout.Dash(750, 0.16, 0.9),
                    attack("katana-ultimate", 85, 0.35, 7, 110, AttackShape.AREA))));

    /** Prevents construction of the immutable catalog holder. */
    private PlayerClasses() {
    }

    /**
     * Returns a built-in definition; no per-player mutable state is shared through it.
     * @param type requested stable class identity
     * @return immutable definition
     * @throws NullPointerException if type is null
     */
    public static ClassDefinition get(PlayerClass type) {
        return DEFINITIONS.get(Objects.requireNonNull(type, "class type"));
    }

    /**
     * Builds validated common attack tuning for the catalog.
     * @param type attack identifier
     * @param damage raw damage
     * @param active active seconds
     * @param cooldown slot cooldown seconds
     * @param range world-space reach
     * @param shape geometry strategy
     * @return immutable common attack definition
     * @throws NullPointerException if type or shape is null
     * @throws IllegalArgumentException if numeric tuning is invalid
     */
    private static AbilityLoadout.AreaAttack attack(String type, int damage, double active,
            double cooldown, double range, AttackShape shape) {
        return new AbilityLoadout.AreaAttack(new AttackSpec(type, damage, active, cooldown), range, shape);
    }
}
