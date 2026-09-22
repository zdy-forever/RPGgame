package au.edu.anu.comp2120.enemy;

import au.edu.anu.comp2120.combat.Attack;
import au.edu.anu.comp2120.combat.Combatant;

/**
 * One guardian's signature behaviour, composed into the shared Boss instead of subclassing it.
 * Health, damage settlement, collision, navigation, phases, death and rewards stay in Boss and
 * the shared combat pipeline; a trait only observes those events and applies its own extra effect.
 * Implementations are stateful for one encounter, so create a fresh instance per Boss.
 * All methods run on the game-loop thread from Boss.update, never inside a damage callback.
 * @author Daoyan Zhu (u7782042)
 */
public interface BossTrait {
    /** Shared stateless behaviour for custom and test arenas, preserving the original encounter rules. */
    BossTrait NONE = new BossTrait() { };

    /**
     * Adjusts the shared encounter template before rematch scaling is applied.
     * Implementations must keep health, reward and attack data identical so that one balance
     * change still reaches every guardian; only movement-feel tuning belongs here.
     * @param base shared unscaled definition
     * @return this guardian's definition, or base when it uses the template unchanged
     */
    default BossConfig tune(BossConfig base) {
        return base;
    }

    /**
     * Reacts to one of this Boss's own attacks that reached damage delivery on the target.
     * Called one decision step after the shared pipeline settled the hit, never during settlement.
     * @param boss owning encounter, alive and registered
     * @param target receiver the attack actually damaged
     * @param source the Boss attack that connected
     */
    default void onLandedHit(Boss boss, Combatant target, Attack source) {
    }

    /**
     * Reacts to health this Boss lost since the previous decision step, from any source.
     * @param boss owning encounter, alive and registered
     * @param target current eligible target, or null when none is present
     * @param damage positive hit points lost
     */
    default void onDamageTaken(Boss boss, Combatant target, int damage) {
    }

    /**
     * Advances this trait's own timers once per decision step, including while the Boss cannot act.
     * Effects that represent Boss agency must additionally check {@link Boss#canAct()}.
     * @param boss owning encounter, alive and registered
     * @param target current eligible target, or null when none is present
     * @param delta finite non-negative simulation seconds
     */
    default void update(Boss boss, Combatant target, double delta) {
    }

    /**
     * Describes current trait state for the existing diagnostics line, requiring no new artwork.
     * @return short label, or empty when this trait has nothing to report
     */
    default String status() {
        return "";
    }

    /**
     * Names the effect this trait currently maintains on its target, for display above that target.
     * Only afflictions this trait owns belong here; freeze, stagger and the elemental reaction
     * statuses live on the shared CombatStatus and are already described from there.
     * @return prominent uppercase label, or empty when this trait holds no affliction
     */
    default String afflictionLabel() {
        return "";
    }
}
