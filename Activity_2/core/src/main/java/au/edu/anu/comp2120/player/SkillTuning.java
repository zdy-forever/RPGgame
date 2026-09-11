package au.edu.anu.comp2120.player;

/**
 * Central adjustable timing/geometry for first-version class behaviours, not final balance.
 * Damage, slot cooldowns and ordinary projectiles remain in PlayerClasses.
 * @author Daoyan Zhu (u7782042)
 */
public final class SkillTuning {
    /** Arrow Rain wave limit. */
    public static final int RAIN_TICKS = 6;
    /** Seconds between Arrow Rain waves. */
    public static final double RAIN_INTERVAL = 0.35;
    /** Magic Circle uninterrupted preparation time. */
    public static final double CAST_SECONDS = 1.5;
    /** Magic Circle wave limit. */
    public static final int FIELD_TICKS = 6;
    /** Seconds between Magic Circle waves. */
    public static final double FIELD_INTERVAL = 0.4;
    /** Small geometry lifetime for one scheduled damage wave. */
    public static final double TICK_ACTIVE = 0.05;
    /** Archer selection suppression duration; incoming geometry remains effective. */
    public static final double UNTARGETABLE_SECONDS = 3;
    /** Rapid double-projectile duration. */
    public static final double RAPID_SECONDS = 4;
    /** Rapid light-attack interval. */
    public static final double RAPID_COOLDOWN = 0.16;
    /** Rapid light launch window, shorter than its interval. */
    public static final double RAPID_LAUNCH = 0.08;
    /** Number of independently randomized projectiles per rapid J. */
    public static final int RAPID_COUNT = 2;
    /** Small angular separation between rapid projectiles. */
    public static final double RAPID_SPREAD = 6;
    /** Successful interception window for Katana K. */
    public static final double PARRY_SECONDS = 0.25;
    /** Locked recovery after a missed Katana K. */
    public static final double RECOVERY_SECONDS = 0.8;
    /** Counter Dash speed, resolved by ordinary world collision. */
    public static final double COUNTER_SPEED = 700;
    /** Counter Dash duration. */
    public static final double COUNTER_SECONDS = 0.36;
    /** Maximum damage segments during Counter Dash. */
    public static final int COUNTER_TICKS = 4;
    /** Counter Stance duration. */
    public static final double STANCE_SECONDS = 1.2;
    /** Dragon Thrust speed. */
    public static final double THRUST_SPEED = 600;
    /** Maximum Dragon Thrust travel duration before a miss. */
    public static final double THRUST_SECONDS = 0.4;
    /** Initial thrust damage, separate from the finite follow-up waves. */
    public static final int THRUST_DAMAGE = 10;
    /** Maximum follow-up waves after a confirmed Dragon Thrust. */
    public static final int DRAGON_TICKS = 6;
    /** Seconds between Dragon follow-up waves. */
    public static final double DRAGON_INTERVAL = 0.12;

    /** Prevents construction of the tuning catalogue. */
    private SkillTuning() {
    }
}
