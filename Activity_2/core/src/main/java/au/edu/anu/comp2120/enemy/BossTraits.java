package au.edu.anu.comp2120.enemy;

import au.edu.anu.comp2120.combat.Attack;
import au.edu.anu.comp2120.combat.AttackSpec;
import au.edu.anu.comp2120.combat.Combatant;
import au.edu.anu.comp2120.combat.ElementType;
import au.edu.anu.comp2120.world.CollisionBox;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

/**
 * The five campaign guardians' signature behaviours and their movement-feel tuning.
 * Every guardian keeps the shared health, reward, phase and attack template; only the trait
 * below and the arena's bolt element differ, so one balance change still reaches all five.
 * Arenas outside the campaign receive {@link BossTrait#NONE} and behave exactly as before.
 * @author Daoyan Zhu (u7782042)
 */
public final class BossTraits {
    /** Water displacement applied to a struck target, resolved by its own world collision. */
    public static final double PUSH_DISTANCE = 120;
    /** Fire damage per burning stack on every tick. */
    public static final int BURN_DAMAGE_PER_STACK = 2;
    /**
     * Share of the shared template's direct melee damage the fire guardian keeps.
     * Its burning stacks return that damage over time instead, so total output stays comparable
     * while the fight changes character: breaking contact now matters more than surviving a hit.
     */
    public static final double BURN_DIRECT_DAMAGE_SHARE = 0.6;
    /** Largest number of simultaneously burning stacks. */
    public static final int MAX_BURN_STACKS = 5;
    /** Ticks a refreshed burn still owes, covering ten seconds at the configured interval. */
    public static final int BURN_TICKS = 5;
    /** Seconds between two burning ticks. */
    public static final double BURN_INTERVAL = 2;
    /** Seconds of taking no damage before the lightning guardian punishes a passive challenger. */
    public static final double RETRIBUTION_DELAY = 10;
    /** Retribution strike damage, delivered through the shared pipeline. */
    public static final int RETRIBUTION_DAMAGE = 10;
    /** Brief action lock accompanying a retribution strike. */
    public static final double RETRIBUTION_STAGGER = 0.6;
    /** Landed ice hits required for one frostbite. */
    public static final int FROSTBITE_HITS = 3;
    /** Frostbite freeze duration applied to the struck target. */
    public static final double FROSTBITE_FREEZE = 2;
    /** Damage inside one window that makes the wind guardian disengage. */
    public static final int BLINK_DAMAGE = 45;
    /** Sliding window over which incoming damage is accumulated. */
    public static final double BLINK_WINDOW = 1.5;
    /** Minimum seconds between two blinks. */
    public static final double BLINK_COOLDOWN = 4;
    /** Immunity the wind guardian gains immediately after a blink. */
    public static final double BLINK_INVULNERABILITY = 2;
    /** Shortest random blink, far enough to break contact. */
    public static final double BLINK_MIN_DISTANCE = 120;
    /** Longest random blink, near enough to stay in the same fight. */
    public static final double BLINK_MAX_DISTANCE = 320;
    /** Bounded random placement attempts, so a crowded arena cannot stall a decision step. */
    private static final int BLINK_ATTEMPTS = 24;

    /** Prevents construction of this guardian catalogue. */
    private BossTraits() {
    }

    /**
     * Selects a fresh trait for an arena, without treating custom or test maps as campaign guardians.
     * @param mapId arena identifier
     * @return new stateful trait for a campaign arena, otherwise the shared no-op behaviour
     * @throws NullPointerException if mapId is null
     */
    public static BossTrait forMap(String mapId) {
        return BossChallenge.forMap(mapId).map(BossTraits::forChallenge).orElse(BossTrait.NONE);
    }

    /**
     * Creates one guardian's signature behaviour; each call returns independent encounter state.
     * @param guardian campaign target
     * @return new trait instance
     * @throws NullPointerException if guardian is null
     */
    public static BossTrait forChallenge(BossChallenge guardian) {
        return switch (Objects.requireNonNull(guardian, "guardian")) {
            case FIRE -> new Burning();
            case WATER -> new TidalPush();
            case LIGHTNING -> new Retribution();
            case ICE -> new Frostbite();
            case WIND -> new Evasion();
        };
    }

    /**
     * Copies the shared template with different movement feel only.
     * Health, phase timing, attack patterns, damage and reward remain the template's values.
     * @param base shared definition
     * @param speed positive pursuit speed in world units per second
     * @param repathSeconds positive minimum interval between obstructed-path searches
     * @return independent definition differing only in navigation movement
     */
    private static BossConfig withMovement(BossConfig base, double speed, double repathSeconds) {
        return withMovement(base, speed, repathSeconds, 1);
    }

    /**
     * Copies the shared template with different movement feel and direct melee damage.
     * Health, phase timing, attack geometry, pacing, elements and reward remain the template's
     * values, so one balance change still reaches every guardian. A damage share below one funds
     * a trait that returns the difference through its own effect rather than adding output.
     * @param base shared definition
     * @param speed positive pursuit speed in world units per second
     * @param repathSeconds positive minimum interval between obstructed-path searches
     * @param damageShare positive share of each pattern's direct damage to keep
     * @return independent definition differing only in navigation movement and pattern damage
     */
    private static BossConfig withMovement(BossConfig base, double speed, double repathSeconds, double damageShare) {
        EnemyConfig navigation = base.navigation();
        return new BossConfig(base.maxHp(),
                new EnemyConfig(speed, navigation.detectionRange(), navigation.loseRange(),
                        navigation.attackRange(), navigation.attack(), navigation.pathCellSize(), repathSeconds),
                base.phaseTwoFraction(), base.transitionSeconds(),
                scaleDamage(base.phaseOne(), damageShare), scaleDamage(base.phaseTwo(), damageShare),
                base.materialId(), base.materialQuantity());
    }

    /**
     * Scales only each pattern's direct damage, preserving geometry, pacing, element and category.
     * @param cycle immutable template patterns
     * @param share positive damage share; one returns the same damage values
     * @return new immutable cycle whose damage is never reduced below one
     */
    private static List<BossConfig.Pattern> scaleDamage(List<BossConfig.Pattern> cycle, double share) {
        if (share == 1) {
            return cycle;
        }
        return cycle.stream().map(pattern -> {
            AttackSpec attack = pattern.attack();
            AttackSpec scaled = new AttackSpec(attack.type(), Math.max(1, (int) Math.round(attack.damage() * share)),
                    attack.activeSeconds(), attack.cooldownSeconds(), attack.element(),
                    attack.blockable(), attack.damageType());
            return new BossConfig.Pattern(scaled, pattern.range(), pattern.shape());
        }).toList();
    }

    /**
     * Fire guardian: every connected attack adds one burning stack that keeps dealing damage.
     * A refreshed burn owes five ticks two seconds apart, so stacks fade ten seconds after the
     * last hit. Stacks multiply each tick's damage, rewarding a challenger who breaks contact.
     * @author Daoyan Zhu (u7782042)
     */
    private static final class Burning implements BossTrait {
        /** Creates one encounter's burning state. */
        Burning() { }

        /** Current stacks in [0, MAX_BURN_STACKS]. */
        private int stacks;
        /** Ticks still owed by the current burn. */
        private int remainingTicks;
        /** Seconds until the next tick. */
        private double untilTick;

        /** {@inheritDoc} */
        @Override
        public BossConfig tune(BossConfig base) {
            return withMovement(base, 78, 0.5, BURN_DIRECT_DAMAGE_SHARE);
        }

        /** {@inheritDoc} */
        @Override
        public void onLandedHit(Boss boss, Combatant target, Attack source) {
            if (stacks == 0) {
                untilTick = BURN_INTERVAL;
            }
            stacks = Math.min(MAX_BURN_STACKS, stacks + 1);
            remainingTicks = BURN_TICKS;
        }

        /** {@inheritDoc} */
        @Override
        public void update(Boss boss, Combatant target, double delta) {
            if (stacks == 0) {
                return;
            }
            if (target == null || !target.getEntity().isAlive()) {
                clear();
                return;
            }
            untilTick -= delta;
            while (remainingTicks > 0 && untilTick <= 0) {
                boss.afflict(target, "boss-burn", BURN_DAMAGE_PER_STACK * stacks, ElementType.FIRE);
                remainingTicks--;
                untilTick += BURN_INTERVAL;
            }
            if (remainingTicks == 0) {
                clear();
            }
        }

        /** {@inheritDoc} */
        @Override
        public String status() {
            return stacks == 0 ? "" : "Burn x" + stacks;
        }

        /** {@inheritDoc} */
        @Override
        public String afflictionLabel() {
            return stacks == 0 ? "" : "BURNING x" + stacks;
        }

        /** Ends the affliction without touching health or the shared pipeline. */
        private void clear() {
            stacks = 0;
            remainingTicks = 0;
            untilTick = 0;
        }
    }

    /**
     * Water guardian: every connected attack shoves the target outward instead of adding damage.
     * The displacement is queued for the target's own collision resolver, so walls still stop it
     * and the challenger keeps control; it breaks melee chains rather than locking the target out.
     * @author Daoyan Zhu (u7782042)
     */
    private static final class TidalPush implements BossTrait {
        /** Creates one encounter's shove counter. */
        TidalPush() { }

        /** Completed shoves, reported for diagnostics only. */
        private int pushes;

        /** {@inheritDoc} */
        @Override
        public BossConfig tune(BossConfig base) {
            return withMovement(base, 62, 0.55);
        }

        /** {@inheritDoc} */
        @Override
        public void onLandedHit(Boss boss, Combatant target, Attack source) {
            CollisionBox from = boss.getBounds();
            CollisionBox to = target.getHurtbox();
            double dx = centreX(to) - centreX(from);
            double dy = centreY(to) - centreY(from);
            if (!Double.isFinite(Math.hypot(dx, dy)) || Math.hypot(dx, dy) == 0) {
                dx = 1;
                dy = 0;
            }
            target.getStatus().applyPush(dx, dy, PUSH_DISTANCE);
            pushes++;
        }

        /** {@inheritDoc} */
        @Override
        public String status() {
            return "Push x" + pushes;
        }
    }

    /**
     * Lightning guardian: standing off and refusing to trade punishes the challenger.
     * The timer restarts on any damage this Boss takes, so pressure is the only way to avoid a
     * strike; the accompanying action lock is short enough to survive but long enough to feel.
     * @author Daoyan Zhu (u7782042)
     */
    private static final class Retribution implements BossTrait {
        /** Creates one encounter's idle countdown. */
        Retribution() { }

        /** Seconds since this Boss last lost health. */
        private double idleSeconds;
        /** Delivered strikes, reported for diagnostics only. */
        private int strikes;

        /** {@inheritDoc} */
        @Override
        public BossConfig tune(BossConfig base) {
            return withMovement(base, 92, 0.4);
        }

        /** {@inheritDoc} */
        @Override
        public void onDamageTaken(Boss boss, Combatant target, int damage) {
            idleSeconds = 0;
        }

        /** {@inheritDoc} */
        @Override
        public void update(Boss boss, Combatant target, double delta) {
            if (target == null || !target.getEntity().isAlive()) {
                idleSeconds = 0;
                return;
            }
            idleSeconds += delta;
            if (idleSeconds < RETRIBUTION_DELAY || !boss.canAct()) {
                return;
            }
            idleSeconds = 0;
            if (boss.afflict(target, "boss-retribution", RETRIBUTION_DAMAGE, ElementType.LIGHTNING)) {
                target.getStatus().applyStagger(RETRIBUTION_STAGGER);
                strikes++;
            }
        }

        /** {@inheritDoc} */
        @Override
        public String status() {
            return String.format(Locale.ROOT, "Storm %.1fs (%d)", RETRIBUTION_DELAY - idleSeconds, strikes);
        }
    }

    /**
     * Ice guardian: the third connected attack freezes the target outright.
     * The counter is visible in diagnostics and resets after each freeze, so the fight is about
     * avoiding the third hit rather than out-healing damage. It moves slowest of the five.
     * @author Daoyan Zhu (u7782042)
     */
    private static final class Frostbite implements BossTrait {
        /** Creates one encounter's frostbite counter. */
        Frostbite() { }

        /** Landed hits since the last freeze, in [0, FROSTBITE_HITS). */
        private int hits;
        /** Applied freezes, reported for diagnostics only. */
        private int freezes;

        /** {@inheritDoc} */
        @Override
        public BossConfig tune(BossConfig base) {
            return withMovement(base, 54, 0.6);
        }

        /** {@inheritDoc} */
        @Override
        public void onLandedHit(Boss boss, Combatant target, Attack source) {
            hits++;
            if (hits < FROSTBITE_HITS) {
                return;
            }
            hits = 0;
            target.getStatus().applyFreeze(FROSTBITE_FREEZE);
            freezes++;
        }

        /** {@inheritDoc} */
        @Override
        public String status() {
            return "Frost " + hits + "/" + FROSTBITE_HITS + " (" + freezes + ")";
        }
    }

    /**
     * Wind guardian: heavy burst damage makes it vanish to a random spot instead of trading hits.
     * Damage is accumulated over a short sliding window, so a single skill or a fast combo triggers
     * the escape while ordinary chip damage does not. The destination is drawn at random from the
     * random bearing and distance rather than pushed away from the challenger, so a blink cannot
     * be predicted or herded. A blink drops its prepared strike and route, and the arrival is
     * briefly untouchable, which ends the burst that caused it instead of merely relocating it.
     * @author Daoyan Zhu (u7782042)
     */
    private static final class Evasion implements BossTrait {
        /** Creates one encounter's evasion window. */
        Evasion() { }

        /** Damage accumulated inside the current window. */
        private int windowDamage;
        /** Seconds left in the current window. */
        private double windowRemaining;
        /** Seconds until another blink is permitted. */
        private double cooldownRemaining;
        /** Completed blinks, reported for diagnostics only. */
        private int blinks;
        /** Gameplay randomness for destination choice, independent of any presentation random. */
        private final Random random = new Random();

        /** {@inheritDoc} */
        @Override
        public BossConfig tune(BossConfig base) {
            return withMovement(base, 104, 0.3);
        }

        /** {@inheritDoc} */
        @Override
        public void onDamageTaken(Boss boss, Combatant target, int damage) {
            if (damage <= 0) {
                return;
            }
            windowDamage = (int) Math.min(Integer.MAX_VALUE, (long) windowDamage + damage);
            windowRemaining = BLINK_WINDOW;
            if (windowDamage < BLINK_DAMAGE || cooldownRemaining > 0 || target == null || !boss.canAct()) {
                return;
            }
            if (blink(boss, target)) {
                boss.getCombatant().getStatus().applyInvulnerability(BLINK_INVULNERABILITY);
                windowDamage = 0;
                windowRemaining = 0;
                cooldownRemaining = BLINK_COOLDOWN;
                blinks++;
            }
        }

        /** {@inheritDoc} */
        @Override
        public void update(Boss boss, Combatant target, double delta) {
            cooldownRemaining = Math.max(0, cooldownRemaining - delta);
            windowRemaining = Math.max(0, windowRemaining - delta);
            if (windowRemaining == 0) {
                windowDamage = 0;
            }
        }

        /** {@inheritDoc} */
        @Override
        public String status() {
            return String.format(Locale.ROOT, "Dodge %s (%d)", cooldownRemaining == 0 ? "ready"
                    : String.format(Locale.ROOT, "%.1fs", cooldownRemaining), blinks);
        }

        /**
         * Vanishes to a random spot around its current position, in a random direction.
         * Both the bearing and the distance are drawn at random, so an escape cannot be predicted
         * or herded the way a fixed outward shove could. The distance is bounded at both ends: far
         * enough to break contact, near enough that the guardian stays in the same fight instead of
         * crossing the arena and walking back. The shared world resolver rejects blocked or
         * out-of-bounds candidates and a candidate landing on the target is skipped, so a rejected
         * roll simply becomes another roll. Attempts are bounded, and exhausting them leaves the
         * guardian in place without consuming its cooldown.
         * @param boss owning encounter
         * @param target current target a destination may not land on
         * @return true when the encounter actually relocated
         */
        private boolean blink(Boss boss, Combatant target) {
            CollisionBox body = boss.getBounds();
            for (int attempt = 0; attempt < BLINK_ATTEMPTS; attempt++) {
                double bearing = random.nextDouble() * Math.PI * 2;
                double distance = BLINK_MIN_DISTANCE
                        + random.nextDouble() * (BLINK_MAX_DISTANCE - BLINK_MIN_DISTANCE);
                double x = body.x() + Math.cos(bearing) * distance;
                double y = body.y() + Math.sin(bearing) * distance;
                CollisionBox candidate = new CollisionBox(x, y, body.width(), body.height());
                if (!candidate.overlaps(target.getHurtbox()) && boss.blinkTo(x, y)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Computes a rectangle's horizontal centre.
     * @param box immutable geometry
     * @return horizontal centre
     */
    private static double centreX(CollisionBox box) {
        return box.x() + box.width() / 2;
    }

    /**
     * Computes a rectangle's vertical centre.
     * @param box immutable geometry
     * @return vertical centre
     */
    private static double centreY(CollisionBox box) {
        return box.y() + box.height() / 2;
    }
}
