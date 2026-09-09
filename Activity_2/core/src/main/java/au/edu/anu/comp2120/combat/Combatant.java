package au.edu.anu.comp2120.combat;

import au.edu.anu.comp2120.world.CollisionBox;
import java.util.Objects;

/**
 * Binds an existing damage receiver to a faction and an independently supplied world-space hurtbox.
 * The owning gameplay adapter updates geometry; visual size and movement collision need not match it.
 * Does not own a second health model. Use one stable adapter per entity in a combat scope.
 *
 * @author Daoyan Zhu (u7782042)
 */
public final class Combatant {
    /** Stable receiver identity used for self-hit and per-attack deduplication. */
    private final Damageable entity;
    /** Fixed side for this adapter. */
    private final Faction faction;
    /** Current immutable hurt geometry, independent of artwork. */
    private CollisionBox hurtbox;

    /**
     * Creates an adapter without changing the receiver's health or position.
     *
     * @param entity existing damage receiver
     * @param faction combat side
     * @param hurtbox initial world-space vulnerable rectangle
     * @throws NullPointerException if any argument is null
     */
    public Combatant(Damageable entity, Faction faction, CollisionBox hurtbox) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.faction = Objects.requireNonNull(faction, "faction");
        this.hurtbox = Objects.requireNonNull(hurtbox, "hurtbox");
    }

    /**
     * Returns the receiver, not a copy of its health.
     * @return underlying damage receiver
     */
    public Damageable getEntity() {
        return entity;
    }

    /**
     * Returns the side used by the friendly-fire rule.
     * @return immutable faction
     */
    public Faction getFaction() {
        return faction;
    }

    /**
     * Returns current vulnerable geometry for detection or debug rendering.
     * @return immutable hurtbox
     */
    public CollisionBox getHurtbox() {
        return hurtbox;
    }

    /**
     * Updates only combat geometry; call after movement and before combat simulation.
     * @param hurtbox new world-space vulnerable rectangle
     * @throws NullPointerException if hurtbox is null; previous geometry is retained
     */
    public void setHurtbox(CollisionBox hurtbox) {
        this.hurtbox = Objects.requireNonNull(hurtbox, "hurtbox");
    }
}
