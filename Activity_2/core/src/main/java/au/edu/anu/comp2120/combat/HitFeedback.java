package au.edu.anu.comp2120.combat;

import au.edu.anu.comp2120.world.CollisionBox;

/**
 * Immutable confirmed-settlement feedback, containing no renderer or live actor references.
 * @param bounds receiver geometry at settlement
 * @param element incoming damage element
 * @param reaction configured reaction ID, or empty
 * @param blocked whether generic defence intercepted the hit
 * @author Daoyan Zhu (u7782042)
 */
public record HitFeedback(CollisionBox bounds, ElementType element, String reaction, boolean blocked) { }
