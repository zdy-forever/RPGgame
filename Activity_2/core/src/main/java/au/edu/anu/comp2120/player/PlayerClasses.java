package au.edu.anu.comp2120.player;

import au.edu.anu.comp2120.combat.AttackSpec;
import au.edu.anu.comp2120.combat.DamageType;
import au.edu.anu.comp2120.combat.ElementType;
import au.edu.anu.comp2120.combat.ProjectileSpec;
import java.util.Map;
import java.util.Objects;

/**
 * Built-in skill definitions and adjustable balance values for Archer, Mage and Katana.
 * Execution composes shared attacks, projectiles, health and collision-safe movement;
 * class differences are data and skill kinds rather than Player subclasses or asset lookups.
 * @author Daoyan Zhu (u7782042)
 */
public final class PlayerClasses {
    /** Default class; elemental bolts and stateful spells share the common ability executor. */
    public static final PlayerClass DEFAULT = PlayerClass.MAGE;
    /** Complete immutable catalog; all mutable ability state stays in each player's controller. */
    private static final Map<PlayerClass, ClassDefinition> DEFINITIONS = Map.of(
            PlayerClass.ARCHER, new ClassDefinition(PlayerClass.ARCHER, new AbilityLoadout(
                    new AbilityLoadout.AreaAttack(new AttackSpec("archer-light", 20, 0.15, 0.45),
                            480, AttackShape.FORWARD, new ProjectileSpec(600, 6, 1.2, 1, 0)),
                    new AbilityLoadout.AreaAttack(new AttackSpec("archer-special", 20, 0.25, 2.5),
                            640, AttackShape.FORWARD, new ProjectileSpec(800, 8, 1.2, 5, 40)),
                    new AbilityLoadout.Dash(650, 0.2, 1.2),
                    skill("archer-rain", 18, 8, 100, SkillKind.ARROW_RAIN)),
                    skill("archer-untargetable", 0, 8, 1, SkillKind.UNTARGETABLE)),
            PlayerClass.MAGE, new ClassDefinition(PlayerClass.MAGE, new AbilityLoadout(
                    new AbilityLoadout.AreaAttack(new AttackSpec("mage-light", 25, 0.25, 0.5,
                            ElementType.NONE, true, DamageType.ELEMENTAL),
                            360, AttackShape.FORWARD, new ProjectileSpec(400, 10, 1.5, 1, 0), SkillKind.ELEMENT_PROJECTILE),
                    skill("mage-element-cycle", 0, 0.05, 1, SkillKind.ELEMENT_CYCLE),
                    new AbilityLoadout.Dash(600, 0.2, 1),
                    skill("mage-circle", 25, 10, 160, SkillKind.MAGIC_CIRCLE)),
                    skill("mage-rapid", 0, 10, 1, SkillKind.RAPID_BARRAGE)),
            PlayerClass.KATANA, new ClassDefinition(PlayerClass.KATANA, new AbilityLoadout(
                    new AbilityLoadout.AreaAttack(new AttackSpec("katana-light", 30, 0.15, 0.35),
                            48, AttackShape.FORWARD),
                    skill("katana-parry", 12, 2, 12, SkillKind.PARRY),
                    new AbilityLoadout.Dash(750, 0.16, 0.9),
                    skill("katana-stance", 100, 7, 140, SkillKind.COUNTER_STANCE)),
                    skill("katana-dragon", 18, 7, 80, SkillKind.DRAGON_THRUST)));

    /**
     * Builds a stateful slot without implying that its activation immediately creates a hitbox.
     * @param id skill identifier
     * @param damage wave/counter damage
     * @param cooldown slot cooldown
     * @param range effect expansion
     * @param kind execution behaviour
     * @return immutable skill definition
     * @throws NullPointerException if id or kind is null
     * @throws IllegalArgumentException if numeric tuning is invalid
     */
    private static AbilityLoadout.AreaAttack skill(String id, int damage, double cooldown, double range, SkillKind kind) {
        return new AbilityLoadout.AreaAttack(new AttackSpec(id, damage, SkillTuning.TICK_ACTIVE, cooldown,
                ElementType.NONE, true, kind == SkillKind.MAGIC_CIRCLE ? DamageType.ELEMENTAL : DamageType.PHYSICAL),
                range, AttackShape.AREA, null, kind);
    }

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

}
