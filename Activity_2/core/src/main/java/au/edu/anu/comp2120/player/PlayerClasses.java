package au.edu.anu.comp2120.player;

import au.edu.anu.comp2120.combat.AttackSpec;
import au.edu.anu.comp2120.combat.ProjectileSpec;
import java.util.Map;
import java.util.Objects;

/**
 * Central built-in prototype definitions for Archer, Mage and Katana.
 * No Player subclasses or asset lookups are needed.
 * Values demonstrate class differences; they are not final skill designs or balance decisions.
 * Mage uses a straight magic bolt for J, fixed surrounding pulses for K/O, and the shared
 * collision-safe dash for L. Stable attack IDs identify spells without implementing elements.
 * Katana uses forward light/heavy slashes and a single-hit surrounding ultimate, with the
 * existing faster dash. Slashes capture facing/geometry at invocation, not from a sprite.
 * @author Daoyan Zhu (u7782042)
 */
public final class PlayerClasses {
    /** Default class; its magic bolt and area pulses share the common ability executor. */
    public static final PlayerClass DEFAULT = PlayerClass.MAGE;
    /** Complete immutable catalog; all mutable ability state stays in each player's controller. */
    private static final Map<PlayerClass, ClassDefinition> DEFINITIONS = Map.of(
            PlayerClass.ARCHER, new ClassDefinition(PlayerClass.ARCHER, new AbilityLoadout(
                    new AbilityLoadout.AreaAttack(new AttackSpec("archer-light", 20, 0.15, 0.45),
                            480, AttackShape.FORWARD, new ProjectileSpec(600, 6, 1.2, 1, 0)),
                    new AbilityLoadout.AreaAttack(new AttackSpec("archer-special", 45, 0.25, 2.5),
                            640, AttackShape.FORWARD, new ProjectileSpec(800, 8, 1.2, 1, 0)),
                    new AbilityLoadout.Dash(650, 0.2, 1.2),
                    new AbilityLoadout.AreaAttack(new AttackSpec("archer-ultimate", 30, 0.4, 8),
                            500, AttackShape.FORWARD, new ProjectileSpec(550, 6, 1.2, 5, 40)))),
            PlayerClass.MAGE, new ClassDefinition(PlayerClass.MAGE, new AbilityLoadout(
                    new AbilityLoadout.AreaAttack(new AttackSpec("mage-light", 25, 0.25, 0.5),
                            360, AttackShape.FORWARD, new ProjectileSpec(400, 10, 1.5, 1, 0)),
                    attack("mage-special", 40, 0.3, 2, 120, AttackShape.AREA),
                    new AbilityLoadout.Dash(600, 0.2, 1),
                    attack("mage-ultimate", 75, 0.5, 8, 180, AttackShape.AREA))),
            PlayerClass.KATANA, new ClassDefinition(PlayerClass.KATANA, new AbilityLoadout(
                    attack("katana-light", 30, 0.15, 0.35, 48, AttackShape.FORWARD),
                    attack("katana-special", 45, 0.25, 2, 80, AttackShape.FORWARD),
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
