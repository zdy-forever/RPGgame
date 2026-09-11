package au.edu.anu.comp2120.combat;

/**
 * Finite simulation-time schedule shared by ground fields and charged statuses.
 * First tick occurs after one interval, never on each zero-time update.
 * @author Daoyan Zhu (u7782042)
 */
final class ReactionTimer {
    /** Immutable total duration. */ private final double duration;
    /** Immutable tick spacing. */ private final double interval;
    /** Bounded total scheduled count. */ private final int limit;
    /** Elapsed clamped simulation time. */ private double elapsed;
    /** Count consumed, including capacity-rejected ticks. */ private int emitted;
    /**
     * Validates a finite bounded periodic schedule.
     * @param duration positive lifetime
     * @param interval positive spacing no longer than lifetime
     * @throws IllegalArgumentException if timing is invalid or more than MAX_TICKS are requested
     */
    ReactionTimer(double duration, double interval) {
        if (!Double.isFinite(duration) || !Double.isFinite(interval) || interval <= 0 || duration < interval
                || duration / interval > ReactionTuning.MAX_TICKS) {
            throw new IllegalArgumentException("Periodic duration/interval must produce a bounded positive schedule");
        }
        this.duration = duration;
        this.interval = interval;
        limit = (int) Math.floor(duration / interval + Math.ulp(duration / interval) * 4);
    }
    /**
     * Consumes all newly due ticks, including the final boundary tick.
     * @param delta finite non-negative seconds
     * @return finite number of newly due ticks
     * @throws IllegalArgumentException if delta is invalid
     */
    int advance(double delta) {
        if (!Double.isFinite(delta) || delta < 0) {
            throw new IllegalArgumentException("Periodic time must be finite and non-negative");
        }
        elapsed = Math.min(duration, elapsed + Math.min(delta, duration));
        double quotient = elapsed / interval;
        int due = Math.min(limit, (int) Math.floor(quotient + Math.ulp(quotient) * 4));
        int count = Math.max(0, due - emitted);
        emitted = Math.max(emitted, due);
        return count;
    }
    /**
     * Returns time left, independent of how often observations are read.
     * @return non-negative seconds
     */
    double remaining() { return Math.max(0, duration - elapsed); }
    /** Cancels future ticks and clears remaining lifetime. */
    void cancel() { elapsed = duration; emitted = limit; }
}
