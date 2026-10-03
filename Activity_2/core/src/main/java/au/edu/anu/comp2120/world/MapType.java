package au.edu.anu.comp2120.world;

/**
 * Describes a map's gameplay role without defining enemies, quests, or rendering.
 *
 * @author Daoyan Zhu (u7782042)
 */
public enum MapType {
    /** Safe base for class selection, NPC services, storage and challenge preparation. */
    HUB,
    /** Practice area with a damage-scoring target, periodic target shots and player health safeguards. */
    TRAINING,
    /** Peaceful hall displaying guardian stones and hosting the final Sage conversation. */
    MEMORIAL,
    /** Boss arena with combat, victory rewards and defeat return to Hub. */
    BATTLE
}
