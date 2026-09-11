package au.edu.anu.comp2120.combat;

/**
 * Minimal scope-local reaction control/vulnerability state shared by any combatant.
 * CombatSystem owns timers; movement owners consume queued displacement through world collision.
 * @author Daoyan Zhu (u7782042)
 */
public final class CombatStatus {
    /** Remaining frozen seconds. */ private double frozen;
    /** Remaining stagger action-lock seconds. */ private double stagger;
    /** Remaining physical vulnerability seconds. */ private double resistance;
    /** Reduction from baseline zero physical resistance. */ private double reduction;
    /** Configurable poise capacity. */ private double maxPoise = ReactionTuning.MAX_POISE;
    /** Current poise, replenished on a break. */ private double poise = maxPoise;
    /** Queued collision-resolved displacement x. */ private double knockbackX;
    /** Queued collision-resolved displacement y. */ private double knockbackY;
    /** Optional charged schedule, refreshed rather than stacked. */ private ReactionTimer charged;
    /** Originator of the latest charged application. */ private Combatant chargedSource;
    /** Direct triggering ID for every charged pulse. */ private long chargedParent;
    /** Creates empty temporary status with default poise capacity. */ public CombatStatus() { }
    /**
     * Reports frozen movement/attack suppression.
     * @return true while frozen
     */
    public boolean isFrozen() { return frozen > 0; }
    /**
     * Reports generic action suppression, not immunity or attack cancellation.
     * @return true while frozen or staggered
     */
    public boolean blocksActions() { return frozen > 0 || stagger > 0; }
    /**
     * Observes freeze expiry.
     * @return non-negative seconds
     */
    public double getFrozenRemaining() { return frozen; }
    /**
     * Observes stagger expiry.
     * @return non-negative seconds
     */
    public double getStaggerRemaining() { return stagger; }
    /**
     * Observes vulnerability expiry.
     * @return non-negative seconds
     */
    public double getResistanceRemaining() { return resistance; }
    /**
     * Returns the multiplier applied only to physical damage at the shared boundary.
     * @return one normally, one plus reduction while vulnerable
     */
    public double physicalMultiplier() { return resistance > 0 ? 1 + reduction : 1; }
    /**
     * Observes charged status lifetime.
     * @return non-negative seconds
     */
    public double getChargedRemaining() { return charged == null ? 0 : charged.remaining(); }
    /**
     * Observes current poise.
     * @return non-negative capacity remaining
     */
    public double getPoise() { return poise; }
    /**
     * Configures a fresh poise capacity without changing HP.
     * @param capacity positive finite poise
     * @throws IllegalArgumentException if capacity is invalid
     */
    public void configurePoise(double capacity) {
        if (!Double.isFinite(capacity) || capacity <= 0) { throw new IllegalArgumentException("Invalid poise"); }
        maxPoise = capacity;
        poise = capacity;
    }
    /**
     * Reads pending displacement, consumed by a collision-aware owner.
     * @return horizontal movement budget
     */
    public double getKnockbackX() { return knockbackX; }
    /**
     * Reads pending displacement, consumed by a collision-aware owner.
     * @return vertical movement budget
     */
    public double getKnockbackY() { return knockbackY; }
    /** Clears displacement after the movement owner resolves it against its world. */
    public void clearKnockback() { knockbackX = 0; knockbackY = 0; }
    /** Refreshes, rather than stacks, the short configured freeze. */
    void freeze() { frozen = ReactionTuning.FREEZE_SECONDS; }
    /** Refreshes finite physical vulnerability, without changing elemental damage. */
    void superconduct() { resistance = ReactionTuning.RESISTANCE_SECONDS; reduction = ReactionTuning.PHYSICAL_REDUCTION; }
    /**
     * Applies configured poise loss and latches one bounded displacement for the movement owner.
     * Latest displacement replaces pending displacement; repeated hits cannot overflow a movement budget.
     * @param dx normalized direction x
     * @param dy normalized direction y
     */
    void overload(double dx, double dy) {
        poise = Math.max(0, poise - ReactionTuning.OVERLOAD_POISE);
        if (poise == 0) { stagger = ReactionTuning.STAGGER_SECONDS; poise = maxPoise; }
        knockbackX = dx * ReactionTuning.KNOCKBACK;
        knockbackY = dy * ReactionTuning.KNOCKBACK;
    }
    /**
     * Refreshes one charged status, retaining the direct source/provenance.
     * @param source originating attacker
     * @param parent positive triggering ID
     */
    void charge(Combatant source, long parent) {
        charged = new ReactionTimer(ReactionTuning.CHARGED_SECONDS, ReactionTuning.CHARGED_INTERVAL);
        chargedSource = source;
        chargedParent = parent;
    }
    /**
     * Ages only pre-existing state once per combat step.
     * @param delta validated finite non-negative seconds
     * @return newly due charged ticks
     */
    int advance(double delta) {
        frozen = Math.max(0, frozen - delta);
        stagger = Math.max(0, stagger - delta);
        resistance = Math.max(0, resistance - delta);
        return charged == null ? 0 : charged.advance(delta);
    }
    /**
     * Reads charged originator for the shared pipeline.
     * @return source or null without a charge
     */
    Combatant chargedSource() { return chargedSource; }
    /**
     * Reads charged direct provenance.
     * @return original positive ID or zero
     */
    long chargedParent() { return chargedParent; }
    /** Clears charged ownership on expiry, cancellation, source death or detachment. */
    void clearCharge() {
        if (charged != null) { charged.cancel(); }
        charged = null; chargedSource = null; chargedParent = 0;
    }
    /** Clears temporary state on death/detachment while retaining configured poise capacity. */
    public void clear() {
        frozen = 0; stagger = 0; resistance = 0; reduction = 0; poise = maxPoise;
        clearKnockback(); clearCharge();
    }
}
