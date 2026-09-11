package au.edu.anu.comp2120.combat;

/**
 * Minimal defensive boundary called after geometry/occlusion, before damage and elements.
 * Implementations may latch a response but must not move actors, mutate arena membership or
 * submit attacks during settlement. The owner executes deferred responses after combat.
 * @author Daoyan Zhu (u7782042)
 */
@FunctionalInterface
public interface HitInterceptor {
    /**
     * Decides whether one blockable incoming invocation is consumed without damage.
     * @param incoming geometrically confirmed attack
     * @return true to block this invocation's hit on this receiver
     */
    boolean blocks(Attack incoming);
}
