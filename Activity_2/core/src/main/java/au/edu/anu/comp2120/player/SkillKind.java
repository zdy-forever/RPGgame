package au.edu.anu.comp2120.player;

/**
 * Small execution vocabulary shared by configured class slots, never interpreted by CombatSystem.
 * @author Daoyan Zhu (u7782042)
 */
public enum SkillKind {
    /** Ordinary shape or projectile attack. */
    DIRECT,
    /** Single projectile using the persistent selected element, modified by rapid buff. */
    ELEMENT_PROJECTILE,
    /** Non-attacking selection of the next Mage element. */
    ELEMENT_CYCLE,
    /** Fixed target-position field with finite waves. */
    ARROW_RAIN,
    /** Selection suppression, not damage immunity. */
    UNTARGETABLE,
    /** Interruptible cast followed by a fixed elemental field. */
    MAGIC_CIRCLE,
    /** Temporary double randomized projectiles and shorter light cooldown. */
    RAPID_BARRAGE,
    /** One defensive window followed by a counter dash or miss recovery. */
    PARRY,
    /** One defensive window followed by a powerful single counter. */
    COUNTER_STANCE,
    /** Collision-safe thrust followed by finite damage only after a landed hit. */
    DRAGON_THRUST
}
