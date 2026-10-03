package au.edu.anu.comp2120.combat;

/**
 * Central provisional balance for the agreed reaction behaviours; no rendering dependencies.
 * @author Daoyan Zhu (u7782042)
 */
public final class ReactionTuning {
    /** Ordinary attachment lifetime in seconds. */ public static final double ATTACHMENT = 3;
    /** Overload explosion base damage. */ public static final int OVERLOAD_DAMAGE = 20;
    /** Overload square half-extent. */ public static final double OVERLOAD_RADIUS = 80;
    /** Overload poise loss per reaction. */ public static final double OVERLOAD_POISE = 100;
    /** Collision-resolved displacement length. */ public static final double KNOCKBACK = 60;
    /** Default capacity, refill occurs after each stagger. */ public static final double MAX_POISE = 100;
    /** Brief action lock following a poise break. */ public static final double STAGGER_SECONDS = 0.5;
    /** Electro-charged damage per tick. */ public static final int CHARGED_DAMAGE = 8;
    /** Electro-charged lifetime; four ticks at 0.5 second intervals. */ public static final double CHARGED_SECONDS = 2;
    /** Electro-charged tick spacing. */ public static final double CHARGED_INTERVAL = 0.5;
    /** Shared freeze duration for any recipient, including Boss encounters. */ public static final double FREEZE_SECONDS = 0.8;
    /** Superconduct area base damage. */ public static final int SUPERCONDUCT_DAMAGE = 12;
    /** Superconduct square half-extent. */ public static final double SUPERCONDUCT_RADIUS = 70;
    /** Physical vulnerability duration. */ public static final double RESISTANCE_SECONDS = 4;
    /** Effective physical multiplier is one plus this reduction from baseline zero resistance. */
    public static final double PHYSICAL_REDUCTION = 0.25;
    /** Each swirl field tick's elemental base damage, including WATER. */ public static final int FIELD_DAMAGE = 6;
    /** Fixed swirl square half-extent. */ public static final double FIELD_RADIUS = 80;
    /** Swirl field lifetime. */ public static final double FIELD_SECONDS = 3;
    /** Swirl tick spacing; six ticks total. */ public static final double FIELD_INTERVAL = 0.5;
    /** Short lifetime of one derived hitbox. */ public static final double PULSE_ACTIVE = 0.05;
    /** Bound on ticks per configurable periodic instance. */ public static final int MAX_TICKS = 64;
    /** Bound on simultaneously retained fields. */ public static final int MAX_FIELDS = 64;
    /** Prevents constructing a constants catalogue. */ private ReactionTuning() { }
}
