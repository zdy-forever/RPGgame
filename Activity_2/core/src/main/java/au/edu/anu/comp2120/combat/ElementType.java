package au.edu.anu.comp2120.combat;

/**
 * Stable gameplay element identities, independent of colors, textures and character classes.
 * @author Daoyan Zhu (u7782042)
 */
public enum ElementType {
    /** Ordinary damage without elemental attachment or reaction. */
    NONE,
    /** Water attachment. */
    WATER,
    /** Fire attachment. */
    FIRE,
    /** Lightning attachment. */
    LIGHTNING,
    /** Ice attachment; no default reaction recipe is assumed. */
    ICE,
    /** Wind attachment; no default reaction recipe is assumed. */
    WIND
}
