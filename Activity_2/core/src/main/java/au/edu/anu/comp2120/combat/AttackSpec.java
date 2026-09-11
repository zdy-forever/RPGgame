package au.edu.anu.comp2120.combat;

import java.util.Objects;

/**
 * Reusable attack tuning without input, artwork, or class-specific damage logic.
 * Each invocation supplies its own independent world-space hitbox to the combat system.
 *
 * @param type non-blank attack label, not an entity class or elemental damage rule
 * @param damage non-negative raw integer damage per target
 * @param activeSeconds positive finite area active duration or projectile launch window in seconds
 * @param cooldownSeconds finite non-negative attacker cooldown from attack creation
 * @param element element applied by valid direct hits; NONE preserves ordinary attack behavior
 * @param blockable whether generic defensive interception may consume this hit
 * @author Daoyan Zhu (u7782042)
 */
public record AttackSpec(String type, int damage, double activeSeconds, double cooldownSeconds,
        ElementType element, boolean blockable) {
    /**
     * Preserves ordinary blockable elemental attacks.
     * @param type attack label
     * @param damage non-negative damage
     * @param activeSeconds positive active window
     * @param cooldownSeconds non-negative cooldown
     * @param element elemental metadata
     * @throws NullPointerException if type or element is null
     * @throws IllegalArgumentException if tuning is invalid
     */
    public AttackSpec(String type, int damage, double activeSeconds, double cooldownSeconds, ElementType element) {
        this(type, damage, activeSeconds, cooldownSeconds, element, true);
    }
    /**
     * Preserves existing non-elemental attack configurations.
     * @param type non-blank attack label
     * @param damage non-negative damage
     * @param activeSeconds positive finite active duration
     * @param cooldownSeconds finite non-negative source cooldown
     * @throws NullPointerException if type is null
     * @throws IllegalArgumentException if numeric tuning or label is invalid
     */
    public AttackSpec(String type, int damage, double activeSeconds, double cooldownSeconds) {
        this(type, damage, activeSeconds, cooldownSeconds, ElementType.NONE);
    }
    /**
     * Validates immutable tuning before an attack can consume cooldown or mutate health.
     * @param type non-blank attack label
     * @param damage non-negative damage
     * @param activeSeconds positive finite area duration or projectile launch window
     * @param cooldownSeconds finite non-negative cooldown
     * @param element required element identity, including NONE
     * @param blockable whether defensive interception is allowed
     * @throws NullPointerException if type or element is null
     * @throws IllegalArgumentException if the label, damage, or timing is invalid
     */
    public AttackSpec {
        Objects.requireNonNull(type, "attack type");
        Objects.requireNonNull(element, "element");
        if (type.isBlank() || damage < 0 || !Double.isFinite(activeSeconds) || activeSeconds <= 0
                || !Double.isFinite(cooldownSeconds) || cooldownSeconds < 0) {
            throw new IllegalArgumentException("Attack requires a label, non-negative damage/cooldown and positive duration");
        }
    }
}
