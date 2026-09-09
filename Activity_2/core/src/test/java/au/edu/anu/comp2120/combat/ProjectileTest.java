package au.edu.anu.comp2120.combat;

import static org.junit.jupiter.api.Assertions.*;

import au.edu.anu.comp2120.enemy.Enemy;
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
 * Tests projectile motion and continuous collision through the actual common damage pipeline.
 * @author Daoyan Zhu (u7782042)
 */
final class ProjectileTest {
    /** Empty arena, large enough for independent range and lifetime checks. */
    private final GameMap map = TestMaps.rectangle(2000, 2000, List.of());
    /** Existing health/position model, not a projectile-specific attacker. */
    private final Player player = new Player(new CollisionBox(100, 100, 20, 20), 100, map);
    /** Canonical source with a centre at (110, 110). */
    private final Combatant source = new Combatant(player, Faction.PLAYER, player.getBounds());
    /** Shared system under test. */
    private final CombatSystem combat = new CombatSystem();
    /** Damage specification; flight time is separate from the short launch window. */
    private final AttackSpec damage = new AttackSpec("test-arrow", 25, 0.1, 0);
    /** Test shot travels 100 units each second with a four-unit body. */
    private final ProjectileSpec flight = new ProjectileSpec(100, 4, 2, 1, 0);

    /** Registers the canonical source for each isolated test. */
    ProjectileTest() {
        combat.register(source);
    }

    /**
     * Flight direction is normalized and independent of the original vector magnitude.
     * @param dx direction x
     * @param dy direction y
     */
    @ParameterizedTest
    @CsvSource({"1,0", "-1,0", "0,1", "0,-1", "1,1", "-1,1", "1,-1", "-1,-1", "3,4"})
    void normalizesAndMovesInEveryDirection(double dx, double dy) {
        Attack attack = launch(dx, dy, 500);
        Projectile arrow = attack.getProjectile();
        combat.update(0.5);
        double length = Math.hypot(dx, dy);
        assertEquals(dx / length, arrow.getDirectionX(), 1e-9);
        assertEquals(dy / length, arrow.getDirectionY(), 1e-9);
        assertEquals(108 + dx / length * 50, attack.getHitbox().x(), 1e-9);
        assertEquals(108 + dy / length * 50, attack.getHitbox().y(), 1e-9);
        assertEquals(450, arrow.getRemainingRange());
        assertEquals(1.5, arrow.getRemainingSeconds());
        assertEquals(new CollisionBox(100, 100, 20, 20), player.getBounds());
    }

    /** Range caps a huge frame and prevents targets beyond the endpoint from receiving damage. */
    @Test
    void rangeExpiresAndCapsLargeDelta() {
        Enemy target = enemyAt(200, 108, 2, 4);
        Attack attack = launch(1, 0, 50);
        combat.update(Double.MAX_VALUE);
        assertEquals(158, attack.getHitbox().x());
        assertEquals(0, attack.getProjectile().getRemainingRange());
        assertFalse(attack.isActive());
        assertEquals(0, attack.getRemainingSeconds());
        assertTrue(combat.getActiveAttacks().isEmpty());
        assertEquals(100, target.getHealth().getCurrentHp());
    }

    /** Lifetime, independently of max range, limits distance before removal. */
    @Test
    void lifetimeCapsDistanceAndRemovesExpiredShots() {
        Attack attack = launch(1, 0, 1000);
        combat.update(10);
        assertEquals(308, attack.getHitbox().x());
        assertEquals(800, attack.getProjectile().getRemainingRange());
        assertFalse(attack.isActive());
        assertTrue(combat.getActiveAttacks().isEmpty());
    }

    /**
     * Very thin targets cannot be skipped, regardless of update partitioning.
     * @param delta frame seconds
     */
    @ParameterizedTest
    @ValueSource(doubles = {0.01, 0.1, 1, 10})
    void hitsThinTargetOnceWithoutTunnelling(double delta) {
        Enemy target = enemyAt(160, 108, 0.1, 4);
        Attack attack = launch(1, 0, 500);
        for (int i = 0; i < 100 && attack.isActive(); i++) {
            combat.update(delta);
        }
        assertEquals(75, target.getHealth().getCurrentHp());
        assertEquals(156, attack.getHitbox().x(), 1e-8);
        assertFalse(attack.isActive());
        assertTrue(combat.getActiveAttacks().isEmpty());
        target.getHealth().restoreFull();
        combat.update(1);
        assertEquals(100, target.getHealth().getCurrentHp());
    }

    /** The first physical target wins even when the farther target was registered earlier. */
    @Test
    void choosesNearestTargetAndDoesNotPierce() {
        Enemy far = enemyAt(190, 100, 20, 20);
        Enemy near = enemyAt(150, 100, 20, 20);
        launch(1, 0, 500);
        combat.update(1);
        assertEquals(75, near.getHealth().getCurrentHp());
        assertEquals(100, far.getHealth().getCurrentHp());
    }

    /** Equal-distance targets use stable registration order and one arrow still damages only one. */
    @Test
    void targetTiesAreDeterministic() {
        Enemy first = enemyAt(150, 100, 20, 20);
        Enemy second = enemyAt(150, 100, 20, 20);
        launch(1, 0, 500);
        combat.update(1);
        assertEquals(75, first.getHealth().getCurrentHp());
        assertEquals(100, second.getHealth().getCurrentHp());
    }

    /** Diagonal detection uses the swept rectangle, not a broad envelope that hits empty corners. */
    @Test
    void diagonalPathDoesNotHitOffPathEnemy() {
        Enemy offPath = enemyAt(110, 155, 5, 5);
        Enemy onPath = enemyAt(150, 150, 5, 5);
        launch(1, 1, 500);
        combat.update(1);
        assertEquals(100, offPath.getHealth().getCurrentHp());
        assertEquals(75, onPath.getHealth().getCurrentHp());
    }

    /** Walls win against targets behind them, including a target tied at the wall's leading edge. */
    @Test
    void wallsStopShotsAndWinTies() {
        GameMap blocked = TestMaps.rectangle(2000, 2000, List.of(new CollisionBox(150, 0, 0.1, 500)));
        Enemy behind = enemyAt(150, 100, 20, 20);
        Attack attack = combat.tryProjectiles(source, damage, flight, 500, 1, 0, blocked).get(0);
        combat.update(1);
        assertEquals(146, attack.getHitbox().x(), 1e-8);
        assertFalse(attack.isActive());
        assertEquals(100, behind.getHealth().getCurrentHp());
        assertTrue(blocked.isWalkable(attack.getHitbox()));
    }

    /** An enemy before a wall remains hittable; an off-path diagonal wall does not block the arrow. */
    @Test
    void targetBeforeWallAndClearDiagonalAreNotFalselyBlocked() {
        GameMap blocked = TestMaps.rectangle(2000, 2000, List.of(new CollisionBox(180, 0, 1, 500)));
        Enemy near = enemyAt(150, 100, 10, 20);
        combat.tryProjectiles(source, damage, flight, 500, 1, 0, blocked);
        combat.update(1);
        assertEquals(75, near.getHealth().getCurrentHp());
        GameMap offPathWall = TestMaps.rectangle(2000, 2000, List.of(new CollisionBox(110, 150, 5, 5)));
        Attack diagonal = combat.tryProjectiles(source, damage, flight, 500, 1, 1, offPathWall).get(0);
        combat.update(1);
        assertTrue(diagonal.isActive());
        assertEquals(108 + 100 / Math.sqrt(2), diagonal.getHitbox().x(), 1e-9);
    }

    /**
     * Full arrow bodies stop at each world boundary and are removed.
     * @param dx horizontal direction
     * @param dy vertical direction
     * @param expectedX contact x
     * @param expectedY contact y
     */
    @ParameterizedTest
    @CsvSource({"-1,0,0,108", "0,-1,108,0", "1,0,196,108", "0,1,108,196"})
    void respectsAllMapBoundaries(double dx, double dy, double expectedX, double expectedY) {
        GameMap small = TestMaps.rectangle(200, 200, List.of());
        Attack attack = combat.tryProjectiles(source, damage, flight, 1000, dx, dy, small).get(0);
        combat.update(2);
        assertEquals(expectedX, attack.getHitbox().x());
        assertEquals(expectedY, attack.getHitbox().y());
        assertFalse(attack.isActive());
        assertTrue(small.isWalkable(attack.getHitbox()));
    }

    /** Self, allies, dead and unregistered targets do not consume a shot before a valid enemy. */
    @Test
    void filtersInvalidDeadAndFriendlyTargets() {
        Health ally = new Health(100);
        combat.register(new Combatant(ally, Faction.PLAYER, new CollisionBox(120, 100, 10, 20)));
        Enemy dead = enemyAt(130, 100, 10, 20);
        dead.takeDamage(100);
        Enemy removed = new Enemy();
        Combatant stale = new Combatant(removed, Faction.ENEMY, new CollisionBox(140, 100, 10, 20));
        combat.register(stale);
        combat.unregister(stale);
        Enemy valid = enemyAt(160, 100, 10, 20);
        launch(1, 0, 500);
        combat.update(1);
        assertEquals(100, player.getHealth().getCurrentHp());
        assertEquals(100, ally.getCurrentHp());
        assertEquals(0, dead.getHealth().getCurrentHp());
        assertEquals(100, removed.getHealth().getCurrentHp());
        assertEquals(75, valid.getHealth().getCurrentHp());
    }

    /** A lethal arrow updates the existing Health dead state without any separate damage formula. */
    @Test
    void lethalArrowUsesSharedHealth() {
        Enemy target = enemyAt(150, 100, 20, 20);
        target.takeDamage(90);
        launch(1, 0, 500);
        combat.update(1);
        assertFalse(target.isAlive());
        assertEquals(0, target.getHealth().getCurrentHp());
    }

    /** Exact endpoint contact is not a hit, while entering its interior on a later step is. */
    @Test
    void preservesPositiveInteriorContactRule() {
        Enemy target = enemyAt(162, 108, 4, 4);
        Attack attack = launch(1, 0, 500);
        combat.update(0.5);
        assertTrue(attack.isActive());
        assertEquals(100, target.getHealth().getCurrentHp());
        combat.update(0);
        assertEquals(100, target.getHealth().getCurrentHp());
        combat.update(0.01);
        assertEquals(75, target.getHealth().getCurrentHp());
        assertFalse(attack.isActive());
    }

    /** Tangential edge contact never damages a target beside the actual path. */
    @Test
    void grazingDoesNotCountAsHit() {
        Enemy target = enemyAt(150, 112, 20, 20);
        Attack attack = launch(1, 0, 500);
        combat.update(1);
        assertTrue(attack.isActive());
        assertEquals(100, target.getHealth().getCurrentHp());
    }

    /** Zero-time sampling can hit an already overlapping hostile body, but cannot move or repeat damage. */
    @Test
    void overlappingTargetAtLaunchIsSettledOnce() {
        Enemy target = enemyAt(108, 108, 4, 4);
        Attack attack = launch(1, 0, 500);
        combat.update(0);
        combat.update(0);
        assertEquals(75, target.getHealth().getCurrentHp());
        assertEquals(new CollisionBox(108, 108, 4, 4), attack.getHitbox());
        assertFalse(attack.isActive());
    }

    /** Clear and unregister cancel retained arrow references, not just their list membership. */
    @Test
    void scopeCleanupCancelsProjectiles() {
        Attack first = launch(1, 0, 500);
        combat.unregister(source);
        assertFalse(first.getProjectile().isActive());
        combat.register(source);
        Attack second = launch(1, 0, 500);
        combat.clear();
        assertFalse(second.getProjectile().isActive());
        assertTrue(combat.getActiveAttacks().isEmpty());
    }

    /** Dead sources cannot launch or keep moving previously launched arrows. */
    @Test
    void deathStopsExistingAndNewShots() {
        Attack attack = launch(1, 0, 500);
        player.takeDamage(100);
        combat.update(1);
        assertEquals(new CollisionBox(108, 108, 4, 4), attack.getHitbox());
        assertFalse(attack.isActive());
        assertTrue(combat.tryProjectiles(source, damage, flight, 500, 1, 0, map).isEmpty());
    }

    /** Projectile damage and queued area attacks preserve shared invocation-order death filtering. */
    @Test
    void lethalArrowCancelsLaterEnemyRetaliation() {
        Enemy enemy = new Enemy(25);
        Combatant hostile = new Combatant(enemy, Faction.ENEMY, new CollisionBox(150, 100, 20, 20));
        combat.register(hostile);
        launch(1, 0, 500);
        Attack retaliation = combat.tryAttack(hostile, new AttackSpec("retaliation", 100, 1, 0),
                player.getBounds()).orElseThrow();
        combat.update(1);
        assertFalse(enemy.isAlive());
        assertFalse(retaliation.isActive());
        assertEquals(100, player.getHealth().getCurrentHp());
    }

    /** An enemy source can use the same projectile pipeline without any Player-specific damage branch. */
    @Test
    void enemyProjectileUsesSameDamageAndFactionRules() {
        Combatant hostile = new Combatant(new Enemy(), Faction.ENEMY, new CollisionBox(160, 100, 20, 20));
        combat.register(hostile);
        Attack arrow = combat.tryProjectiles(hostile, damage, flight, 500, -1, 0, map).get(0);
        combat.update(1);
        assertEquals(75, player.getHealth().getCurrentHp());
        assertFalse(arrow.isActive());
    }

    /** Repeated shooting/expiry cycles release all active membership instead of accumulating arrows. */
    @Test
    void repeatedExpiryDoesNotAccumulate() {
        for (int i = 0; i < 100; i++) {
            Attack shot = launch(1, 0, 50);
            combat.update(3);
            assertFalse(shot.isActive());
            assertTrue(combat.getActiveAttacks().isEmpty());
        }
    }

    /** Source cooldown is applied once for a whole volley, not between individual arrows. */
    @Test
    void volleyAndSourceCooldownAreAtomic() {
        AttackSpec timed = new AttackSpec("volley", 10, 0.1, 1);
        ProjectileSpec fan = new ProjectileSpec(100, 4, 2, 5, 40);
        List<Attack> shots = combat.tryProjectiles(source, timed, fan, 500, 1, 0, map);
        assertEquals(5, shots.size());
        assertEquals(5, shots.stream().map(Attack::getId).distinct().count());
        assertEquals(1, combat.getCooldownRemaining(source));
        assertThrows(UnsupportedOperationException.class, shots::clear);
        assertTrue(combat.tryProjectiles(source, timed, fan, 500, 1, 0, map).isEmpty());
        combat.update(1);
        assertEquals(5, combat.tryProjectiles(source, timed, fan, 500, 1, 0, map).size());
    }

    /** Capacity refuses a whole volley without consuming cooldown or accumulating unbounded objects. */
    @Test
    void capacityIsBoundedAndRecoveredAfterExpiry() {
        for (int i = 0; i < CombatSystem.MAX_PROJECTILES - 2; i++) {
            launch(1, 0, 500);
        }
        ProjectileSpec fan = new ProjectileSpec(100, 4, 2, 5, 40);
        assertTrue(combat.tryProjectiles(source, new AttackSpec("fan", 1, 0.1, 10), fan,
                500, 1, 0, map).isEmpty());
        assertEquals(0, combat.getCooldownRemaining(source));
        assertEquals(CombatSystem.MAX_PROJECTILES - 2, combat.getActiveAttacks().size());
        launch(1, 0, 500);
        launch(1, 0, 500);
        assertTrue(combat.tryProjectiles(source, damage, flight, 500, 1, 0, map).isEmpty());
        combat.update(10);
        assertTrue(combat.getActiveAttacks().isEmpty());
        assertEquals(5, combat.tryProjectiles(source, damage, fan, 500, 1, 0, map).size());
    }

    /**
     * Invalid time cannot move an arrow, apply damage or age timers.
     * @param delta rejected elapsed time
     */
    @ParameterizedTest
    @ValueSource(doubles = {-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void invalidDeltaDoesNotMutate(double delta) {
        Attack attack = launch(1, 0, 500);
        assertThrows(IllegalArgumentException.class, () -> combat.update(delta));
        assertEquals(new CollisionBox(108, 108, 4, 4), attack.getHitbox());
        assertEquals(2, attack.getRemainingSeconds());
    }

    /** Invalid direction, range, source and spawn cannot create partial volleys or spend cooldown. */
    @Test
    void invalidLaunchIsRejectedBeforeMutation() {
        assertThrows(IllegalArgumentException.class, () -> combat.tryProjectiles(source, damage, flight, 500, 0, 0, map));
        assertThrows(IllegalArgumentException.class, () -> combat.tryProjectiles(source, damage, flight, 500, Double.NaN, 0, map));
        assertThrows(IllegalArgumentException.class, () -> combat.tryProjectiles(source, damage, flight, -1, 1, 0, map));
        assertThrows(IllegalArgumentException.class, () -> combat.tryProjectiles(source, damage, flight,
                Double.POSITIVE_INFINITY, 1, 0, map));
        GameMap blocked = TestMaps.rectangle(2000, 2000, List.of(new CollisionBox(108, 108, 4, 4)));
        assertThrows(IllegalArgumentException.class, () -> combat.tryProjectiles(source, damage, flight, 500, 1, 0, blocked));
        assertThrows(IllegalArgumentException.class, () -> combat.tryProjectiles(
                new Combatant(player, Faction.PLAYER, source.getHurtbox()), damage, flight, 500, 1, 0, map));
        assertThrows(NullPointerException.class, () -> combat.tryProjectiles(source, damage, null, 500, 1, 0, map));
        assertThrows(NullPointerException.class, () -> combat.tryProjectiles(source, damage, flight, 500, 1, 0, null));
        assertTrue(combat.getActiveAttacks().isEmpty());
        assertEquals(0, combat.getCooldownRemaining(source));
        assertEquals(1, launch(1, 0, 500).getId());
    }

    /**
     * Launches the normal fixture shot.
     * @param dx direction x
     * @param dy direction y
     * @param range maximum travel distance
     * @return accepted invocation
     */
    private Attack launch(double dx, double dy, double range) {
        return combat.tryProjectiles(source, damage, flight, range, dx, dy, map).get(0);
    }

    /**
     * Registers an actual enemy health model at a requested hurtbox.
     * @param x target x
     * @param y target y
     * @param width target width
     * @param height target height
     * @return independently owned enemy
     */
    private Enemy enemyAt(double x, double y, double width, double height) {
        Enemy target = new Enemy();
        combat.register(new Combatant(target, Faction.ENEMY, new CollisionBox(x, y, width, height)));
        return target;
    }
}
