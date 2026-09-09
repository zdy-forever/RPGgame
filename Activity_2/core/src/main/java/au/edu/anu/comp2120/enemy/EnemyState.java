package au.edu.anu.comp2120.enemy;

/**
 * Small finite-state model for basic enemy decisions, without separate state classes or a behaviour tree.
 *
 * @author Daoyan Zhu (u7782042)
 */
public enum EnemyState {
    /** No eligible target is currently being pursued. */
    IDLE,
    /** Following a direct approach or A* waypoints toward a detected target through map collision. */
    CHASE,
    /** In attack range, stationary and requesting attacks through the shared cooldown. */
    ATTACK,
    /** Zero HP; movement and new attack requests are disabled. */
    DEAD
}
