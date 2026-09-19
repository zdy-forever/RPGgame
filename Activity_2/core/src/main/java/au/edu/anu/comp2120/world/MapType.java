package au.edu.anu.comp2120.world;

/**
 * Describes a map's gameplay role without defining enemies, quests, or rendering.
 *
 * @author Daoyan Zhu (u7782042)
 */
public enum MapType {
    /** Base used for preparation and future NPC interactions. */
    HUB,
    /** Non-hostile practice area with a stationary damage-scoring target. */
    TRAINING,
    /** Peaceful hall displaying guardian stones and hosting the final Sage conversation. */
    MEMORIAL,
    /** Area intended for future combat encounters. */
    BATTLE
}
