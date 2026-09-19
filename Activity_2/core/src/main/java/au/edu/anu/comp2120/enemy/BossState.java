package au.edu.anu.comp2120.enemy;

/**
 * Small encounter state model; no animation or cutscene states are required.
 * @author Daoyan Zhu (u7782042)
 */
public enum BossState {
    /** No eligible detected target, or controller detached from its arena. */
    IDLE,
    /** Existing enemy navigation is positioning toward the target. */
    CHASE,
    /** Stationary melee preparation with fixed visible geometry and no damage yet. */
    WINDUP,
    /** Within attack distance, including time waiting for shared source cooldown. */
    ATTACK,
    /** Reaction freeze suppresses movement and new attacks, not existing hitboxes. */
    FROZEN,
    /** Broken poise briefly suppresses new actions. */
    STAGGERED,
    /** One-time pause after entering the second HP phase. */
    PHASE_TRANSITION,
    /** Encounter defeated; no movement or attacks even if health is externally restored. */
    DEAD
}
