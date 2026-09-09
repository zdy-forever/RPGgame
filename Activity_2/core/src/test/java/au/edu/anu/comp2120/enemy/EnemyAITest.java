package au.edu.anu.comp2120.enemy;

import static org.junit.jupiter.api.Assertions.*;

import au.edu.anu.comp2120.combat.Attack;
import au.edu.anu.comp2120.combat.AttackSpec;
import au.edu.anu.comp2120.combat.Combatant;
import au.edu.anu.comp2120.combat.CombatSystem;
import au.edu.anu.comp2120.combat.Faction;
import au.edu.anu.comp2120.player.Player;
import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.GameMap;
import au.edu.anu.comp2120.world.TestMaps;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Checks autonomous state decisions using actual map collision, combat, player, and health models.
 *
 * @author Daoyan Zhu (u7782042)
 */
final class EnemyAITest {
    /** Floating-point tolerance for normalized movement calculations. */
    private static final double EPSILON = 1e-9;
    /** Empty arena used by the ordinary pursuit tests. */
    private final GameMap map = TestMaps.rectangle(1000, 1000, List.of());
    /** Shared pipeline whose clock is advanced only by test/arena coordination. */
    private final CombatSystem combat = new CombatSystem();
    /** Existing hostile health model. */
    private final Enemy enemy = new Enemy();
    /** Actual player receiver, not a second health-only stand-in. */
    private final Player player = new Player(new CollisionBox(300, 100, 20, 20), 100, map);
    /** Target hurtbox can be relocated independently to exercise boundary cases. */
    private final Combatant target = new Combatant(player, Faction.PLAYER, player.getBounds());
    /** Configured radii and binary-exact attack timing. */
    private final EnemyConfig config = new EnemyConfig(100, 200, 260, 40,
            new AttackSpec("basic", 10, 0.25, 1));
    /** Controller registering this enemy at (100, 100). */
    private final EnemyAI ai = new EnemyAI(enemy, new CollisionBox(100, 100, 20, 20), map, config, combat);

    /** Registers the player after the independently registered enemy controller. */
    EnemyAITest() {
        combat.register(target);
    }

    /** A distant player leaves the enemy stably idle across many time steps. */
    @Test
    void distantPlayerLeavesEnemyIdle() {
        target.setHurtbox(new CollisionBox(500, 100, 20, 20));
        CollisionBox before = ai.getBounds();
        for (int i = 0; i < 100; i++) {
            ai.update(target, 0.1);
            combat.update(0.1);
            assertEquals(EnemyState.IDLE, ai.getState());
        }
        assertEquals(before, ai.getBounds());
        assertTrue(combat.getActiveAttacks().isEmpty());
        assertEquals(100, player.getHealth().getCurrentHp());
    }

    /**
     * Detection/attack boundaries are inclusive and zero time cannot move the body.
     * @param distance centre separation from the enemy
     * @param expected expected decision
     */
    @ParameterizedTest
    @CsvSource({"201,IDLE", "200,CHASE", "40.001,CHASE", "40,ATTACK", "0,ATTACK"})
    void choosesStateAtRangeBoundaries(double distance, EnemyState expected) {
        target.setHurtbox(new CollisionBox(100 + distance, 100, 20, 20));
        CollisionBox before = ai.getBounds();
        ai.update(target, 0);
        assertEquals(expected, ai.getState());
        assertEquals(before, ai.getBounds());
        assertEquals(expected == EnemyState.ATTACK ? 1 : 0, combat.getActiveAttacks().size());
    }

    /**
     * Pursuit moves in all eight directions at the same configured speed.
     * @param horizontal direction sign on x
     * @param vertical direction sign on y
     */
    @ParameterizedTest
    @CsvSource({"1,0", "-1,0", "0,1", "0,-1", "1,1", "-1,1", "1,-1", "-1,-1"})
    void chasesWithNormalizedTimeBasedMovement(int horizontal, int vertical) {
        target.setHurtbox(new CollisionBox(100 + horizontal * 80, 100 + vertical * 80, 20, 20));
        ai.update(target, 0.1);
        double norm = Math.hypot(horizontal, vertical);
        assertEquals(100 + horizontal / norm * 10, ai.getBounds().x(), EPSILON);
        assertEquals(100 + vertical / norm * 10, ai.getBounds().y(), EPSILON);
        assertEquals(10, Math.hypot(ai.getBounds().x() - 100, ai.getBounds().y() - 100), EPSILON);
        assertEquals(EnemyState.CHASE, ai.getState());
        assertEquals(ai.getBounds(), ai.getCombatant().getHurtbox());
        assertEquals(player.getBounds(), new CollisionBox(300, 100, 20, 20));
    }

    /** Static-target movement depends on elapsed time rather than update count. */
    @Test
    void matchesMovementAcrossUpdateRates() {
        CombatSystem otherCombat = new CombatSystem();
        otherCombat.register(target);
        EnemyAI other = new EnemyAI(new Enemy(), ai.getBounds(), map, config, otherCombat);
        ai.update(target, 0.5);
        for (int i = 0; i < 5; i++) {
            other.update(target, 0.1);
        }
        assertEquals(ai.getBounds().x(), other.getBounds().x(), EPSILON);
        assertEquals(ai.getBounds().y(), other.getBounds().y(), EPSILON);
    }

    /** Huge finite steps stop at attack range instead of crossing through or oscillating around the player. */
    @Test
    void capsChaseAtAttackDistanceAndStopsMoving() {
        ai.update(target, 1e100);
        assertEquals(260, ai.getBounds().x(), EPSILON);
        assertEquals(EnemyState.ATTACK, ai.getState());
        CollisionBox stopped = ai.getBounds();
        ai.update(target, 0.1);
        assertEquals(stopped, ai.getBounds());
        assertEquals(1, combat.getActiveAttacks().size());
        combat.update(0);
        assertEquals(90, player.getHealth().getCurrentHp());
    }

    /**
     * Normalized diagonal arrival must not stall outside attack range due to floating-point rounding.
     * @param dx horizontal target offset
     * @param dy vertical target offset
     */
    @ParameterizedTest
    @CsvSource({"80,80", "-80,80", "80,-80", "-80,-80", "90,35", "13,77"})
    void diagonalArrivalEntersAttackWithoutBoundaryJitter(double dx, double dy) {
        target.setHurtbox(new CollisionBox(100 + dx, 100 + dy, 20, 20));
        ai.update(target, 10);
        assertEquals(EnemyState.ATTACK, ai.getState());
        CollisionBox stopped = ai.getBounds();
        for (int i = 0; i < 20; i++) {
            ai.update(target, 0.1);
            assertEquals(EnemyState.ATTACK, ai.getState());
            assertEquals(stopped, ai.getBounds());
        }
    }

    /** A deterministic spread of approach angles must all enter attack rather than stall on roundoff. */
    @Test
    void reachesAttackAcrossVariedApproachAngles() {
        for (int dx = 45; dx <= 190; dx += 7) {
            for (int dy = 10; dy <= 190; dy += 7) {
                if (Math.hypot(dx, dy) > config.detectionRange()) {
                    continue;
                }
                CombatSystem arena = new CombatSystem();
                target.setHurtbox(new CollisionBox(100 + dx, 100 + dy, 20, 20));
                arena.register(target);
                EnemyAI approaching = new EnemyAI(new Enemy(), new CollisionBox(100, 100, 20, 20),
                        map, config, arena);
                approaching.update(target, 10);
                assertEquals(EnemyState.ATTACK, approaching.getState(), "Approach " + dx + "," + dy);
            }
        }
    }

    /** Detection/loss hysteresis prevents acquisition-edge flicker and loss does not teleport home. */
    @Test
    void losesAndReacquiresTargetWithSeparateRanges() {
        ai.update(target, 0);
        assertEquals(EnemyState.CHASE, ai.getState());
        target.setHurtbox(new CollisionBox(360, 100, 20, 20));
        ai.update(target, 0);
        assertEquals(EnemyState.CHASE, ai.getState());
        target.setHurtbox(new CollisionBox(361, 100, 20, 20));
        ai.update(target, 1);
        assertEquals(EnemyState.IDLE, ai.getState());
        assertEquals(100, ai.getBounds().x());
        target.setHurtbox(new CollisionBox(320, 100, 20, 20));
        ai.update(target, 1);
        assertEquals(EnemyState.IDLE, ai.getState());
        target.setHurtbox(new CollisionBox(300, 100, 20, 20));
        ai.update(target, 0);
        assertEquals(EnemyState.CHASE, ai.getState());
    }

    /** An attacking enemy resumes pursuit when the target leaves attack range, then idles when lost. */
    @Test
    void transitionsFromAttackToChaseToIdle() {
        target.setHurtbox(new CollisionBox(130, 100, 20, 20));
        ai.update(target, 0);
        assertEquals(EnemyState.ATTACK, ai.getState());
        target.setHurtbox(new CollisionBox(200, 100, 20, 20));
        ai.update(target, 0.1);
        assertEquals(EnemyState.CHASE, ai.getState());
        assertEquals(110, ai.getBounds().x(), EPSILON);
        target.setHurtbox(new CollisionBox(800, 100, 20, 20));
        ai.update(target, 0.1);
        assertEquals(EnemyState.IDLE, ai.getState());
        assertEquals(110, ai.getBounds().x(), EPSILON);
    }

    /** Autonomous requests respect the existing cooldown; AI itself neither ages it nor applies damage. */
    @Test
    void requestsAttacksThroughSharedCooldownAndHealth() {
        target.setHurtbox(new CollisionBox(130, 100, 20, 20));
        ai.update(target, 0);
        assertEquals(100, player.getHealth().getCurrentHp());
        assertEquals(1, combat.getCooldownRemaining(ai.getCombatant()));
        combat.update(0);
        assertEquals(90, player.getHealth().getCurrentHp());
        for (int i = 0; i < 100; i++) {
            ai.update(target, 0);
            combat.update(0);
        }
        assertEquals(90, player.getHealth().getCurrentHp());
        combat.update(0.5);
        ai.update(target, 0);
        assertTrue(combat.getActiveAttacks().isEmpty());
        combat.update(0.5);
        ai.update(target, 0);
        combat.update(0);
        assertEquals(80, player.getHealth().getCurrentHp());
    }

    /** Lethal enemy combat damage updates player death, after which AI drops the target. */
    @Test
    void stopsPursuingDeadPlayer() {
        player.takeDamage(95);
        target.setHurtbox(new CollisionBox(130, 100, 20, 20));
        ai.update(target, 0);
        combat.update(0);
        assertFalse(player.isAlive());
        CollisionBox before = ai.getBounds();
        ai.update(target, 10);
        assertEquals(EnemyState.IDLE, ai.getState());
        assertEquals(before, ai.getBounds());
    }

    /** An enemy killed through combat immediately reports DEAD and cannot move or retaliate. */
    @Test
    void lethalPlayerAttackStopsEnemyBehaviour() {
        target.setHurtbox(new CollisionBox(130, 100, 20, 20));
        combat.tryAttack(target, new AttackSpec("lethal", 100, 0.25, 1), ai.getBounds());
        ai.update(target, 0);
        Attack pending = combat.getActiveAttacks().get(1);
        CollisionBox before = ai.getBounds();
        combat.update(0);
        assertEquals(EnemyState.DEAD, ai.getState());
        assertFalse(pending.isActive());
        assertEquals(100, player.getHealth().getCurrentHp());
        for (int i = 0; i < 10; i++) {
            ai.update(target, 1);
            combat.update(1);
        }
        assertEquals(before, ai.getBounds());
        assertEquals(EnemyState.DEAD, ai.getState());
        assertEquals(0, enemy.getHealth().heal(100));
        assertEquals(EnemyState.DEAD, ai.getState());
    }

    /** Explicit restoration resumes decisions without changing position or duplicating registration. */
    @Test
    void restoredEnemyCanResumeFromIdle() {
        enemy.takeDamage(100);
        ai.update(target, 0);
        enemy.getHealth().restoreFull();
        ai.resetAwareness();
        assertEquals(EnemyState.IDLE, ai.getState());
        ai.update(target, 0.1);
        assertEquals(EnemyState.CHASE, ai.getState());
        assertEquals(2, combat.getCombatants().size());
    }

    /** Absent, friendly, self, unregistered, or detached targets/controllers do not cause pursuit. */
    @Test
    void ignoresInvalidTargetsAndDetachedController() {
        Combatant friendly = new Combatant(new Enemy(), Faction.ENEMY, target.getHurtbox());
        combat.register(friendly);
        Combatant unregistered = new Combatant(player, Faction.PLAYER, target.getHurtbox());
        CollisionBox before = ai.getBounds();
        for (Combatant invalid : new Combatant[] {null, friendly, ai.getCombatant(), unregistered}) {
            ai.update(invalid, 1);
            assertEquals(EnemyState.IDLE, ai.getState());
            assertEquals(before, ai.getBounds());
        }
        combat.clear();
        ai.update(target, 1);
        assertEquals(EnemyState.IDLE, ai.getState());
        assertEquals(before, ai.getBounds());
        assertTrue(combat.getActiveAttacks().isEmpty());
    }

    /**
     * Invalid time is rejected without moving, changing state, submitting attacks, or aging cooldowns.
     * @param delta invalid elapsed time or overflowing speed-times-time product
     */
    @ParameterizedTest
    @ValueSource(doubles = {-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.MAX_VALUE})
    void rejectsInvalidTimeAtomically(double delta) {
        CollisionBox before = ai.getBounds();
        assertThrows(IllegalArgumentException.class, () -> ai.update(target, delta));
        assertEquals(before, ai.getBounds());
        assertEquals(EnemyState.IDLE, ai.getState());
        assertTrue(combat.getActiveAttacks().isEmpty());
        assertEquals(0, combat.getCooldownRemaining(ai.getCombatant()));
    }

    /** A full-height sealing wall has no route; the enemy waits rather than walking into it. */
    @Test
    void collisionStopsChaseAtThinWall() {
        GameMap walls = TestMaps.rectangle(1000, 1000, List.of(new CollisionBox(160, 0, 2, 1000)));
        CombatSystem arena = new CombatSystem();
        arena.register(target);
        EnemyAI blocked = new EnemyAI(new Enemy(), new CollisionBox(100, 100, 20, 20), walls, config, arena);
        blocked.update(target, 100);
        assertEquals(100, blocked.getBounds().x());
        assertEquals(EnemyState.CHASE, blocked.getState());
        assertTrue(walls.isWalkable(blocked.getBounds()));
        assertTrue(arena.getActiveAttacks().isEmpty());
        blocked.update(target, 100);
        assertEquals(100, blocked.getBounds().x());
    }

    /** An unreachable diagonal target does not produce aimless wall sliding or corner cutting. */
    @Test
    void unreachableDiagonalTargetDoesNotSlideAimlessly() {
        GameMap walls = TestMaps.rectangle(1000, 1000, List.of(new CollisionBox(120, 0, 2, 1000)));
        CombatSystem arena = new CombatSystem();
        target.setHurtbox(new CollisionBox(200, 200, 20, 20));
        arena.register(target);
        EnemyAI blocked = new EnemyAI(new Enemy(), new CollisionBox(100, 100, 20, 20), walls, config, arena);
        blocked.update(target, 0.1);
        assertEquals(100, blocked.getBounds().x());
        assertEquals(100, blocked.getBounds().y());
        assertTrue(walls.isWalkable(blocked.getBounds()));
    }

    /**
     * Even malformed external target geometry beyond the arena cannot pull an enemy across its edges.
     * @param targetX target x outside a small arena
     * @param targetY target y outside a small arena
     * @param expectedX unchanged enemy x when no valid navigation goal exists
     * @param expectedY unchanged enemy y when no valid navigation goal exists
     */
    @ParameterizedTest
    @CsvSource({"-100,40,40,40", "200,40,40,40", "40,-100,40,40", "40,200,40,40"})
    void preservesAllMapBoundaries(double targetX, double targetY, double expectedX, double expectedY) {
        GameMap small = TestMaps.rectangle(100, 100, List.of());
        CombatSystem arena = new CombatSystem();
        target.setHurtbox(new CollisionBox(targetX, targetY, 20, 20));
        arena.register(target);
        EnemyAI bounded = new EnemyAI(new Enemy(), new CollisionBox(40, 40, 20, 20), small, config, arena);
        bounded.update(target, 100);
        assertEquals(expectedX, bounded.getBounds().x());
        assertEquals(expectedY, bounded.getBounds().y());
        assertTrue(small.isWalkable(bounded.getBounds()));
    }

    /** Multiple AI instances share one clock while keeping their own health, state, and cooldown. */
    @Test
    void supportsMultipleIndependentEnemiesWithoutAcceleratingCooldowns() {
        Enemy otherEnemy = new Enemy();
        EnemyAI other = new EnemyAI(otherEnemy, ai.getBounds(), map, config, combat);
        target.setHurtbox(new CollisionBox(130, 100, 20, 20));
        ai.update(target, 0);
        other.update(target, 0);
        combat.update(0.5);
        assertEquals(80, player.getHealth().getCurrentHp());
        assertEquals(0.5, combat.getCooldownRemaining(ai.getCombatant()));
        assertEquals(0.5, combat.getCooldownRemaining(other.getCombatant()));
        enemy.takeDamage(100);
        ai.update(target, 0);
        other.update(target, 0);
        assertEquals(EnemyState.DEAD, ai.getState());
        assertEquals(EnemyState.ATTACK, other.getState());
        assertEquals(100, otherEnemy.getHealth().getCurrentHp());
    }

    /** Invalid spawn/null/duplicate construction leaves existing arena membership unchanged. */
    @Test
    void validatesConstructionBeforeRegistration() {
        CollisionBox body = ai.getBounds();
        assertThrows(NullPointerException.class, () -> new EnemyAI(null, body, map, config, combat));
        assertThrows(NullPointerException.class, () -> new EnemyAI(new Enemy(), null, map, config, combat));
        assertThrows(NullPointerException.class, () -> new EnemyAI(new Enemy(), body, null, config, combat));
        assertThrows(NullPointerException.class, () -> new EnemyAI(new Enemy(), body, map, null, combat));
        assertThrows(NullPointerException.class, () -> new EnemyAI(new Enemy(), body, map, config, null));
        assertThrows(IllegalArgumentException.class, () -> new EnemyAI(enemy, body, map, config, combat));
        assertThrows(IllegalArgumentException.class, () -> new EnemyAI(new Enemy(),
                new CollisionBox(-1, 0, 20, 20), map, config, combat));
        assertThrows(IllegalArgumentException.class, () -> new EnemyAI(new Enemy(),
                new CollisionBox(990, 0, 20, 20), map, config, combat));
        GameMap wallMap = TestMaps.rectangle(1000, 1000, List.of(body));
        assertThrows(IllegalArgumentException.class, () -> new EnemyAI(new Enemy(), body, wallMap, config, combat));
        assertEquals(2, combat.getCombatants().size());
        assertSame(enemy, ai.getEnemy());
        assertSame(config, ai.getConfig());
    }
}
