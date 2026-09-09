package au.edu.anu.comp2120.enemy;

import au.edu.anu.comp2120.combat.Combatant;
import au.edu.anu.comp2120.combat.CombatSystem;
import au.edu.anu.comp2120.combat.Faction;
import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.AStarPathfinder;
import au.edu.anu.comp2120.world.WalkableArea;
import java.util.List;
import java.util.Objects;

/**
 * One enemy's graphics-free state, movement geometry, and basic pursuit decisions.
 * Reuses WalkableArea for movement and CombatSystem for attacks, cooldowns, damage, and death.
 * The arena owner updates every AI, then advances combat once per simulation step, not per enemy.
 * Clear approaches use direct movement; obstacles use cached full-body A* paths.
 * There is no dynamic entity collision or crowd avoidance.
 *
 * @author Daoyan Zhu (u7782042)
 */
public final class EnemyAI {
    /** Small coordinate-precision allowance preventing normalized movement from stalling at attack range. */
    private static final double ATTACK_RANGE_ULPS = 4;
    /** Existing entity; remains the only owner of this enemy's health. */
    private final Enemy enemy;
    /** Current collision body, not a sprite or animation size. */
    private CollisionBox bounds;
    /** Current arena's collision environment, fixed for this controller's lifetime. */
    private final WalkableArea area;
    /** Static-map navigation data for this enemy's body size. */
    private final AStarPathfinder pathfinder;
    /** Cached full-body route, never exposed as mutable data. */
    private List<CollisionBox> path = List.of();
    /** Next unconsumed waypoint in the route. */
    private int nextWaypoint;
    /** Target geometry used by the last search, or null before planning. */
    private CollisionBox plannedTarget;
    /** Minimum simulation time remaining before another blocked-path search. */
    private double repathRemaining;
    /** Validated reusable decision and movement tuning. */
    private final EnemyConfig config;
    /** Arena pipeline; this controller never advances its shared clock. */
    private final CombatSystem combat;
    /** Stable registered damage/hurtbox adapter for this enemy. */
    private final Combatant combatant;
    /** Last decision, initially idle; death is also checked directly when state is queried. */
    private EnemyState state = EnemyState.IDLE;

    /**
     * Creates and registers one enemy controller at a fully walkable position.
     * Validation finishes before registration. No player state, health, or map data is changed.
     * @param enemy existing health-owning enemy
     * @param bounds initial positive collision body
     * @param area arena movement/collision environment
     * @param config reusable enemy tuning
     * @param combat shared arena pipeline
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if geometry/grid size is invalid or the enemy is already registered
     */
    public EnemyAI(Enemy enemy, CollisionBox bounds, WalkableArea area, EnemyConfig config, CombatSystem combat) {
        this.enemy = Objects.requireNonNull(enemy, "enemy");
        this.bounds = Objects.requireNonNull(bounds, "bounds");
        this.area = Objects.requireNonNull(area, "area");
        this.config = Objects.requireNonNull(config, "config");
        this.combat = Objects.requireNonNull(combat, "combat");
        if (!area.isWalkable(bounds)) {
            throw new IllegalArgumentException("Enemy spawn must be fully walkable");
        }
        attackBox(bounds);
        pathfinder = new AStarPathfinder(area, bounds.width(), bounds.height(), config.pathCellSize());
        combatant = new Combatant(enemy, Faction.ENEMY, bounds);
        combat.register(combatant);
    }

    /**
     * Chooses idle, chase, or attack for an eligible target; dead enemies do nothing.
     * Null, dead, friendly, self, and unregistered targets cause idle without movement.
     * A controller removed from its arena also becomes idle. Detection includes its boundary;
     * tracking is lost only beyond loseRange. Attack-distance comparisons allow four coordinate ULPs
     * for arithmetic rounding, not additional gameplay reach. Clear pursuit stops at attack distance;
     * obstructed pursuit follows cached A* waypoints with the same movement budget. Enemy attacks
     * require a clear conservative corridor to the target. Attacks are requested, not settled here.
     * @param target canonical registered player/hostile-to-this-enemy adapter, or null if absent
     * @param delta finite non-negative elapsed seconds with representable movement distance
     * @throws IllegalArgumentException if time or resulting movement/attack geometry is invalid
     */
    public void update(Combatant target, double delta) {
        if (!Double.isFinite(delta) || delta < 0 || !Double.isFinite(config.speed() * delta)) {
            throw new IllegalArgumentException("Enemy time and movement distance must be finite and non-negative");
        }
        if (!enemy.isAlive()) {
            state = EnemyState.DEAD;
            clearPath();
            return;
        }
        List<Combatant> registered = combat.getCombatants();
        if (!registered.contains(combatant) || target == null || !registered.contains(target)
                || target.getEntity() == enemy || target.getFaction() == Faction.ENEMY
                || !target.getEntity().isAlive()) {
            state = EnemyState.IDLE;
            clearPath();
            return;
        }
        double distance = distanceTo(target);
        double limit = getState() == EnemyState.IDLE ? config.detectionRange() : config.loseRange();
        if (distance > limit) {
            state = EnemyState.IDLE;
            clearPath();
            return;
        }
        repathRemaining = Math.max(0, repathRemaining - delta);
        if (!canAttack(target, distance)) {
            state = EnemyState.CHASE;
            chase(target, config.speed() * delta);
            distance = distanceTo(target);
        }
        if (canAttack(target, distance)) {
            state = EnemyState.ATTACK;
            clearPath();
            combat.tryAttack(combatant, config.attack(), attackBox(bounds));
        }
    }

    /**
     * Returns health-aware state, reflecting lethal combat settlement without a one-frame delay.
     * After explicit health restoration, a previously dead controller starts idle until updated.
     * @return current decision or DEAD when HP is zero
     */
    public EnemyState getState() {
        if (!enemy.isAlive()) {
            return EnemyState.DEAD;
        }
        return state == EnemyState.DEAD ? EnemyState.IDLE : state;
    }

    /**
     * Returns independent collision geometry for read-only gameplay/debug consumers.
     * @return current immutable body
     */
    public CollisionBox getBounds() {
        return bounds;
    }

    /**
     * Returns the existing enemy so reset/debug code can use its validated Health API.
     * @return health-owning entity
     */
    public Enemy getEnemy() {
        return enemy;
    }

    /**
     * Returns the canonical adapter; controller movement synchronizes its hurtbox with the body.
     * @return stable combat adapter registered by this controller
     */
    public Combatant getCombatant() {
        return combatant;
    }

    /**
     * Returns tuning for diagnostics without exposing writable configuration.
     * @return immutable enemy configuration
     */
    public EnemyConfig getConfig() {
        return config;
    }

    /**
     * Forgets pursuit without moving, healing, or changing shared cooldowns.
     * Used by the arena's explicit debug reset after it clears attacks and restores HP.
     */
    public void resetAwareness() {
        state = EnemyState.IDLE;
        clearPath();
    }

    /**
     * Uses direct pursuit when clear, otherwise reuses/replans a bounded A* route.
     * @param target eligible player target
     * @param budget non-negative finite travel distance available this step
     */
    private void chase(Combatant target, double budget) {
        var direct = pathfinder.findDirectDestination(bounds, target.getHurtbox(), config.attackRange());
        if (direct.isPresent()) {
            clearPath();
            path = List.of(direct.get());
        } else if (plannedTarget == null || (repathRemaining == 0
                && (!plannedTarget.equals(target.getHurtbox()) || nextWaypoint >= path.size()))) {
            path = pathfinder.findPath(bounds, target.getHurtbox(), config.attackRange());
            nextWaypoint = 0;
            plannedTarget = target.getHurtbox();
            repathRemaining = config.repathSeconds();
        }
        while (nextWaypoint < path.size() && budget > 0) {
            CollisionBox waypoint = path.get(nextWaypoint);
            double dx = waypoint.x() - bounds.x();
            double dy = waypoint.y() - bounds.y();
            double distance = Math.hypot(dx, dy);
            if (distance == 0) {
                nextWaypoint++;
                continue;
            }
            double step = Math.min(budget, distance);
            CollisionBox desired = step == distance ? waypoint : new CollisionBox(bounds.x() + dx / distance * step,
                    bounds.y() + dy / distance * step, bounds.width(), bounds.height());
            if (!pathfinder.hasClearCorridor(bounds, desired)) {
                clearPath();
                return;
            }
            CollisionBox moved = area.move(bounds, desired.x() - bounds.x(), desired.y() - bounds.y());
            attackBox(moved);
            bounds = moved;
            combatant.setHurtbox(bounds);
            budget -= step;
            if (step == distance) {
                nextWaypoint++;
            } else {
                break;
            }
            if (canAttack(target, distanceTo(target))) {
                break;
            }
        }
    }

    /** Discards route/retry state after death, target loss, direct pursuit, or explicit reset. */
    private void clearPath() {
        path = List.of();
        nextWaypoint = 0;
        plannedTarget = null;
        repathRemaining = 0;
    }

    /**
     * Combines range with conservative wall clearance so a nearby enemy does not attack through a wall.
     * @param target valid target
     * @param distance current centre separation
     * @return true if in range and no obstacle intersects the body-to-target corridor
     */
    private boolean canAttack(Combatant target, double distance) {
        return withinAttackRange(target, distance) && pathfinder.hasClearCorridor(bounds, target.getHurtbox());
    }

    /**
     * Measures separation between physical enemy centre and target hurtbox centre.
     * @param target valid target adapter
     * @return non-negative Euclidean distance, possibly infinite for extremely separated geometry
     */
    private double distanceTo(Combatant target) {
        return Math.hypot(centreX(target.getHurtbox()) - centreX(bounds),
                centreY(target.getHurtbox()) - centreY(bounds));
    }

    /**
     * Allows only coordinate-scale rounding error at the stopping/attack threshold.
     * @param target valid target whose centre determines arithmetic precision
     * @param distance already measured centre separation
     * @return true within attack range including a four-ULP rounding allowance
     */
    private boolean withinAttackRange(Combatant target, double distance) {
        double scale = Math.max(config.attackRange(), Math.max(
                Math.max(Math.abs(centreX(bounds)), Math.abs(centreY(bounds))),
                Math.max(Math.abs(centreX(target.getHurtbox())), Math.abs(centreY(target.getHurtbox())))));
        return distance <= config.attackRange() + ATTACK_RANGE_ULPS * Math.ulp(scale);
    }

    /**
     * Creates a fixed square attack centred on the enemy, independently of rendering.
     * @param body current or candidate collision body
     * @return positive attack geometry with configured half-width/half-height
     * @throws IllegalArgumentException if the resulting geometry is not representable
     */
    private CollisionBox attackBox(CollisionBox body) {
        double range = config.attackRange();
        return new CollisionBox(centreX(body) - range, centreY(body) - range, range * 2, range * 2);
    }

    /**
     * Computes a rectangle's horizontal centre without graphics APIs.
     * @param box immutable geometry
     * @return horizontal centre
     */
    private static double centreX(CollisionBox box) {
        return box.x() + box.width() / 2;
    }

    /**
     * Computes a rectangle's vertical centre without graphics APIs.
     * @param box immutable geometry
     * @return vertical centre
     */
    private static double centreY(CollisionBox box) {
        return box.y() + box.height() / 2;
    }
}
