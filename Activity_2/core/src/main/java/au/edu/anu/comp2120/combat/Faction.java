package au.edu.anu.comp2120.combat;

/**
 * Minimal opposing combat sides; members of the same side cannot damage one another.
 *
 * @author Daoyan Zhu (u7782042)
 */
public enum Faction {
    /** Player-controlled actors and their allies. */
    PLAYER,
    /** Hostile actors, including Boss encounters and the training target. */
    ENEMY
}
