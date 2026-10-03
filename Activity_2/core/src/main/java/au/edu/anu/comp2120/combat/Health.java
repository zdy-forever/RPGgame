package au.edu.anu.comp2120.combat;

/**
 * Shared integer hit-point state for any damageable entity, independent of graphics and effects.
 * Current HP stays between zero and a positive maximum. Death is derived from zero HP,
 * not stored separately. Ordinary healing cannot revive; restoration must be requested explicitly.
 * Mutations belong on the game-loop thread; this model is not thread-safe.
 *
 * @author Daoyan Zhu (u7782042)
 * @author Hao Liu (u7998420)
 */
public final class Health implements Damageable {
    /** Positive hit-point limit, adjustable by equipment. */
    private int maxHp;
    /** Remaining hit points, inclusive of zero and maxHp. */
    private int currentHp;
    /** HP numerator at the last actual health mutation, avoiding repeated rounding on gear swaps. */
    private int ratioHp;
    /** Capacity denominator paired with ratioHp. */
    private int ratioMax;
    /** Changes only on actual HP loss, so healing cannot conceal damage from cast interruption. */
    private long damageVersion;

    /**
     * Observes effective damage without callbacks or class-specific health rules.
     * @return version to compare for inequality across a protected/casting window
     */
    public long getDamageVersion() {
        return damageVersion;
    }

    /**
     * Creates a living entity at full health.
     *
     * @param maxHp positive maximum hit points, up to {@link Integer#MAX_VALUE}
     * @throws IllegalArgumentException if maxHp is zero or negative
     */
    public Health(int maxHp) {
        if (maxHp <= 0) {
            throw new IllegalArgumentException("Maximum HP must be positive");
        }
        this.maxHp = maxHp;
        currentHp = maxHp;
        rememberRatio();
    }

    /**
     * Returns the current capacity of this health model.
     *
     * @return positive maximum hit points
     */
    public int getMaxHp() {
        return maxHp;
    }

    /** Changes capacity while preserving the health fraction without recording a combat hit.
     * Reuses the last health-mutation ratio so repeated equipment swaps neither heal nor erode HP.
     * Living entities retain at least one HP; dead entities remain dead.
     * @param maximum positive new capacity
     * @throws IllegalArgumentException if maximum is nonpositive
     */
    public void setMaxHp(int maximum) {
        if (maximum <= 0) { throw new IllegalArgumentException("Maximum HP must be positive"); }
        maxHp = maximum;
        currentHp = ratioHp == 0 ? 0 : (int) Math.max(1L, (long) ratioHp * maximum / ratioMax);
    }

    /**
     * Returns remaining hit points without exposing writable fields.
     *
     * @return current hit points in the inclusive range zero to maximum HP
     */
    public int getCurrentHp() {
        return currentHp;
    }

    /**
     * Removes hit points, saturating at zero even for maximum integer damage.
     * Zero damage and further damage after death are harmless. Invalid input never changes state.
     * No death effect, movement rule, or automatic respawn is performed.
     *
     * @param damage non-negative hit points to remove
     * @throws IllegalArgumentException if damage is negative
     */
    @Override
    public void takeDamage(int damage) {
        requireNonNegative(damage);
        if (damage > 0 && currentHp > 0) {
            damageVersion++;
        }
        if (damage > 0 && currentHp > 0) {
            currentHp -= Math.min(damage, currentHp);
            rememberRatio();
        }
    }

    /**
     * Restores a living entity's hit points up to its maximum without integer overflow.
     * Zero healing, healing at full HP, and healing after death have no effect.
     * Negative input is rejected even when dead or full. Use {@link #restoreFull()} to revive.
     *
     * @param amount non-negative hit points to restore
     * @return actual hit points restored, which may be less than amount
     * @throws IllegalArgumentException if amount is negative
     */
    public int heal(int amount) {
        requireNonNegative(amount);
        int restored = isAlive() ? Math.min(amount, maxHp - currentHp) : 0;
        currentHp += restored;
        if (restored > 0) { rememberRatio(); }
        return restored;
    }

    /**
     * Reports life status directly from current HP.
     *
     * @return true exactly when current HP is greater than zero
     */
    @Override
    public boolean isAlive() {
        return currentHp > 0;
    }

    /**
     * Reports death without maintaining a second, potentially inconsistent state flag.
     *
     * @return true exactly when current HP is zero
     */
    public boolean isDead() {
        return !isAlive();
    }

    /**
     * Explicitly resets HP to its maximum, reviving a dead entity if necessary.
     * Repeated calls are harmless. This does not move an entity, change maps, or reset other state;
     * the gameplay coordinator performs those operations separately during a defeat return or load.
     */
    public void restoreFull() {
        currentHp = maxHp;
        rememberRatio();
    }

    /** Restores an exact saved HP value without treating loading as a combat hit.
     * May explicitly revive, like restoreFull. The coordinator must discard active combat
     * actions before loading; damageVersion remains a counter of actual damage events.
     * @param savedHp saved hit points from zero through this instance's maximum
     * @throws IllegalArgumentException if savedHp is outside the valid range
     */
    public void restoreSavedHp(int savedHp) {
        if (savedHp < 0 || savedHp > maxHp) {
            throw new IllegalArgumentException("Saved HP is outside health capacity");
        }
        currentHp = savedHp;
        rememberRatio();
    }

    /** Starts a fresh exact scaling reference after damage, healing or explicit restoration. */
    private void rememberRatio() { ratioHp = currentHp; ratioMax = maxHp; }

    /**
     * Validates an operation before any hit-point mutation.
     *
     * @param amount requested damage or healing
     * @throws IllegalArgumentException if amount is negative
     */
    private static void requireNonNegative(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Damage and healing must be non-negative");
        }
    }
}
