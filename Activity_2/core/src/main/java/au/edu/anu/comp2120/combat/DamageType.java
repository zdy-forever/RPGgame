package au.edu.anu.comp2120.combat;

/**
 * Damage calculation category, independent of elemental attachment metadata.
 * @author Daoyan Zhu (u7782042)
 */
public enum DamageType {
    /** Weapon impact/slashes, affected by physical resistance reduction. */ PHYSICAL,
    /** Magic and reaction damage, unaffected by physical resistance reduction. */ ELEMENTAL
}
