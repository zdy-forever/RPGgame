package au.edu.anu.comp2120.combat;

import static org.junit.jupiter.api.Assertions.*;

import au.edu.anu.comp2120.enemy.Enemy;
import au.edu.anu.comp2120.player.Player;
import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.TestMaps;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Exercises the real damage pipeline, identity/lifecycle rules, and timing without graphics.
 *
 * @author Daoyan Zhu (u7782042)
 */
final class CombatSystemTest {
    /** Shared test tuning with binary-exact duration and cooldown boundaries. */
    private final AttackSpec strike = new AttackSpec("strike", 25, 1, 0.5);
    /** Fixed attack region covering the enemy fixture. */
    private final CollisionBox hitbox = new CollisionBox(30, 10, 40, 20);
    /** Actual player, retaining its existing movement and health implementation. */
    private final Player player = new Player(new CollisionBox(10, 10, 20, 20), 100,
            TestMaps.rectangle(500, 500, List.of()));
    /** Actual enemy using the same health implementation. */
    private final Enemy enemy = new Enemy();
    /** Player hurtbox is smaller than the movement/visual body. */
    private final Combatant source = new Combatant(player, Faction.PLAYER, new CollisionBox(12, 12, 16, 16));
    /** Opposing target adapter, independent of any enemy visual model. */
    private final Combatant target = new Combatant(enemy, Faction.ENEMY, new CollisionBox(40, 10, 20, 20));
    /** Arena owning both canonical adapters. */
    private final CombatSystem system = new CombatSystem();

    /** Registers the two actors for each isolated test. */
    CombatSystemTest() {
        system.register(source);
        system.register(target);
    }

    /** A detected hit updates the actual enemy Health without modifying attacker HP. */
    @Test
    void playerDamagesEnemyThroughPipeline() {
        Attack attack = system.tryAttack(source, strike, hitbox).orElseThrow();
        assertEquals(100, enemy.getHealth().getCurrentHp());
        system.update(0);
        assertEquals(75, enemy.getHealth().getCurrentHp());
        assertEquals(100, player.getHealth().getCurrentHp());
        assertTrue(enemy.isAlive());
        assertSame(source, attack.getSource());
        assertSame(strike, attack.getSpecification());
        assertEquals(hitbox, attack.getHitbox());
        assertTrue(attack.getId() > 0);
    }

    /** The exact same path handles enemy attacks on the player. */
    @Test
    void enemyDamagesPlayerThroughPipeline() {
        system.tryAttack(target, strike, source.getHurtbox());
        system.update(0);
        assertEquals(75, player.getHealth().getCurrentHp());
        assertEquals(100, enemy.getHealth().getCurrentHp());
    }

    /**
     * Separated rectangles, all touching edges/corners, and a tiny true overlap obey one rule.
     * @param x attack origin x
     * @param y attack origin y
     * @param width attack width
     * @param height attack height
     * @param expectedHp target HP after detection
     */
    @ParameterizedTest
    @CsvSource({"20,10,20,20,100", "60,10,20,20,100", "40,-10,20,20,100",
            "40,30,20,20,100", "20,-10,20,20,100", "60,30,20,20,100",
            "100,100,10,10,100", "39.999,10,0.002,20,75", "40,10,20,20,75"})
    void testsPositiveAreaOverlap(double x, double y, double width, double height, int expectedHp) {
        system.tryAttack(source, strike, new CollisionBox(x, y, width, height));
        system.update(0);
        assertEquals(expectedHp, enemy.getHealth().getCurrentHp());
    }

    /** Self and friendly actors are immune even when fully inside a valid hostile attack region. */
    @Test
    void filtersSelfAndFriendlyTargets() {
        Health allyHealth = new Health(100);
        system.register(new Combatant(allyHealth, Faction.PLAYER, target.getHurtbox()));
        system.tryAttack(source, strike, new CollisionBox(0, 0, 100, 100));
        system.update(0);
        assertEquals(100, player.getHealth().getCurrentHp());
        assertEquals(100, allyHealth.getCurrentHp());
        assertEquals(75, enemy.getHealth().getCurrentHp());
    }

    /** Enemy-to-enemy friendly fire is also excluded, not merely player self-hits. */
    @Test
    void filtersEnemyAllies() {
        Health allyHealth = new Health(100);
        system.register(new Combatant(allyHealth, Faction.ENEMY, source.getHurtbox()));
        system.tryAttack(target, strike, new CollisionBox(0, 0, 100, 100));
        system.update(0);
        assertEquals(100, allyHealth.getCurrentHp());
        assertEquals(100, enemy.getHealth().getCurrentHp());
        assertEquals(75, player.getHealth().getCurrentHp());
    }

    /** Maximum raw damage reaches shared Health, clamps at zero, and produces death. */
    @Test
    void lethalDamageUsesHealthSaturation() {
        system.tryAttack(source, new AttackSpec("lethal", Integer.MAX_VALUE, 1, 0), hitbox);
        system.update(0);
        assertEquals(0, enemy.getHealth().getCurrentHp());
        assertFalse(enemy.isAlive());
        assertTrue(enemy.getHealth().isDead());
        assertEquals(0, enemy.getHealth().heal(100));
    }

    /** No number of repeated samples, exits, re-entries, or healing can repeat one invocation's hit. */
    @Test
    void settlesEachReceiverOnceForWholeInvocation() {
        Attack attack = system.tryAttack(source, strike, hitbox).orElseThrow();
        system.update(0);
        for (int i = 0; i < 100; i++) {
            system.update(0);
        }
        target.setHurtbox(new CollisionBox(200, 200, 20, 20));
        system.update(0.25);
        target.setHurtbox(hitbox);
        enemy.getHealth().heal(25);
        system.update(0.25);
        assertEquals(100, enemy.getHealth().getCurrentHp());
        assertTrue(attack.hasHit(enemy));
    }

    /** A miss does not consume the target's first hit if it later enters during the active interval. */
    @Test
    void movingTargetCanEnterLater() {
        target.setHurtbox(new CollisionBox(200, 200, 20, 20));
        system.tryAttack(source, strike, hitbox);
        system.update(0.25);
        assertEquals(100, enemy.getHealth().getCurrentHp());
        target.setHurtbox(hitbox);
        system.update(0.25);
        assertEquals(75, enemy.getHealth().getCurrentHp());
    }

    /** Distinct targets each receive one hit; targets are not tracked merely by type/faction. */
    @Test
    void hitsMultipleHostileTargetsOnceEach() {
        Enemy other = new Enemy();
        system.register(new Combatant(other, Faction.ENEMY, hitbox));
        system.tryAttack(source, strike, hitbox);
        system.update(0);
        system.update(0);
        assertEquals(75, other.getHealth().getCurrentHp());
        assertEquals(75, enemy.getHealth().getCurrentHp());
    }

    /**
     * Exact expiry and oversized finite steps sample once, expire, and release cooldown safely.
     * @param delta elapsed step at least as large as the attack duration
     */
    @ParameterizedTest
    @ValueSource(doubles = {1, 10, Double.MAX_VALUE})
    void expiresAfterStartOfStepSample(double delta) {
        Attack attack = system.tryAttack(source, strike, hitbox).orElseThrow();
        system.update(delta);
        assertEquals(75, enemy.getHealth().getCurrentHp());
        assertFalse(attack.isActive());
        assertEquals(0, attack.getRemainingSeconds());
        assertTrue(system.getActiveAttacks().isEmpty());
        assertEquals(0, system.getCooldownRemaining(source));
        enemy.getHealth().restoreFull();
        system.update(0);
        assertEquals(100, enemy.getHealth().getCurrentHp());
    }

    /** Targets arriving exactly after expiry are not hit by the removed invocation. */
    @Test
    void cannotHitAfterExpiry() {
        target.setHurtbox(new CollisionBox(200, 200, 20, 20));
        system.tryAttack(source, strike, hitbox);
        system.update(1);
        target.setHurtbox(hitbox);
        system.update(0);
        assertEquals(100, enemy.getHealth().getCurrentHp());
    }

    /** Cooldown is per attacker, across attack types, and becomes ready at the exact boundary. */
    @Test
    void enforcesIndependentSourceCooldownsAndNewInvocationIds() {
        Attack first = system.tryAttack(source, strike, hitbox).orElseThrow();
        assertTrue(system.tryAttack(source, new AttackSpec("other", 10, 1, 0), hitbox).isEmpty());
        assertTrue(system.tryAttack(target, strike, source.getHurtbox()).isPresent());
        system.update(0.25);
        assertEquals(0.25, system.getCooldownRemaining(source));
        assertTrue(system.tryAttack(source, strike, hitbox).isEmpty());
        system.update(0.25);
        Attack second = system.tryAttack(source, strike, hitbox).orElseThrow();
        assertTrue(second.getId() > first.getId());
        assertTrue(first.isActive());
        system.update(0);
        assertEquals(50, enemy.getHealth().getCurrentHp());
        assertEquals(75, player.getHealth().getCurrentHp());
    }

    /** Explicit zero cooldown permits distinct invocations, but never repeats either one's hit. */
    @Test
    void zeroCooldownAllowsDistinctAttacks() {
        AttackSpec rapid = new AttackSpec("rapid", 10, 1, 0);
        system.tryAttack(source, rapid, hitbox);
        system.tryAttack(source, rapid, hitbox);
        system.update(0);
        system.update(0);
        assertEquals(80, enemy.getHealth().getCurrentHp());
    }

    /**
     * Invalid time is rejected before detection, aging, cooldown changes, or damage delivery.
     * @param delta invalid elapsed time
     */
    @ParameterizedTest
    @ValueSource(doubles = {-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsInvalidTimeAtomically(double delta) {
        Attack attack = system.tryAttack(source, strike, hitbox).orElseThrow();
        assertThrows(IllegalArgumentException.class, () -> system.update(delta));
        assertEquals(100, enemy.getHealth().getCurrentHp());
        assertEquals(1, attack.getRemainingSeconds());
        assertEquals(0.5, system.getCooldownRemaining(source));
        assertFalse(attack.hasHit(enemy));
    }

    /** Null/unknown sources and duplicate receiver aliases cannot enter or bypass this arena. */
    @Test
    void validatesMembershipAndRequestsBeforeMutation() {
        Combatant alias = new Combatant(player, Faction.ENEMY, hitbox);
        assertThrows(IllegalArgumentException.class, () -> system.register(source));
        assertThrows(IllegalArgumentException.class, () -> system.register(alias));
        assertThrows(NullPointerException.class, () -> system.register(null));
        assertThrows(IllegalArgumentException.class, () -> system.tryAttack(alias, strike, hitbox));
        assertThrows(IllegalArgumentException.class, () -> system.getCooldownRemaining(alias));
        assertThrows(NullPointerException.class, () -> system.tryAttack(null, strike, hitbox));
        assertThrows(NullPointerException.class, () -> system.tryAttack(source, null, hitbox));
        assertThrows(NullPointerException.class, () -> system.tryAttack(source, strike, null));
        assertEquals(0, system.getCooldownRemaining(source));
        assertEquals(2, system.getCombatants().size());
        assertTrue(system.getActiveAttacks().isEmpty());
    }

    /** Unregistered and dead targets are never damaged, and dead sources cannot create attacks. */
    @Test
    void ignoresInvalidOrDeadParticipants() {
        system.unregister(target);
        Attack first = system.tryAttack(source, strike, hitbox).orElseThrow();
        system.update(0);
        assertEquals(100, enemy.getHealth().getCurrentHp());
        system.register(target);
        enemy.takeDamage(100);
        system.update(0);
        assertFalse(first.hasHit(enemy));
        assertTrue(system.tryAttack(target, strike, hitbox).isEmpty());
        assertEquals(0, system.getCooldownRemaining(target));
    }

    /** Death before settlement cancels a queued strike; revival cannot resurrect that strike. */
    @Test
    void sourceDeathCancelsPendingAttack() {
        Attack attack = system.tryAttack(source, strike, hitbox).orElseThrow();
        player.takeDamage(100);
        system.update(0);
        assertFalse(attack.isActive());
        player.getHealth().restoreFull();
        system.update(0);
        assertEquals(100, enemy.getHealth().getCurrentHp());
    }

    /** Simultaneous strikes follow creation order and a killed source cannot retaliate afterward. */
    @Test
    void resolvesQueuedAttacksInDeterministicOrder() {
        system.tryAttack(source, new AttackSpec("lethal", 100, 1, 0), hitbox);
        Attack retaliation = system.tryAttack(target, strike, source.getHurtbox()).orElseThrow();
        system.update(0);
        assertFalse(enemy.isAlive());
        assertFalse(retaliation.isActive());
        assertEquals(100, player.getHealth().getCurrentHp());
    }

    /** Re-registering the same receiver via another adapter cannot repeat a previous hit. */
    @Test
    void ledgerUsesUnderlyingReceiverIdentity() {
        system.tryAttack(source, strike, hitbox);
        system.update(0);
        assertTrue(system.unregister(target));
        Combatant replacement = new Combatant(enemy, Faction.ENEMY, hitbox);
        system.register(replacement);
        system.update(0);
        assertEquals(75, enemy.getHealth().getCurrentHp());
    }

    /** Removing a source cancels its attacks; unknown removal is harmless. */
    @Test
    void unregisterCancelsOnlyRemovedSourceAttacks() {
        Attack first = system.tryAttack(source, strike, hitbox).orElseThrow();
        Attack second = system.tryAttack(target, strike, source.getHurtbox()).orElseThrow();
        assertTrue(system.unregister(source));
        assertFalse(first.isActive());
        assertTrue(second.isActive());
        assertFalse(system.unregister(source));
        assertThrows(NullPointerException.class, () -> system.unregister(null));
        system.update(0);
        assertEquals(100, player.getHealth().getCurrentHp());
    }

    /** Clearing a scene cancels retained references, clears cooldowns, and does not heal anyone. */
    @Test
    void clearPreservesHealthAndNeverReusesIds() {
        Attack first = system.tryAttack(source, strike, hitbox).orElseThrow();
        system.update(0);
        system.clear();
        assertFalse(first.isActive());
        assertTrue(system.getActiveAttacks().isEmpty());
        assertTrue(system.getCombatants().isEmpty());
        assertEquals(75, enemy.getHealth().getCurrentHp());
        system.register(source);
        system.register(target);
        Attack next = system.tryAttack(source, strike, hitbox).orElseThrow();
        assertTrue(next.getId() > first.getId());
    }

    /** Membership snapshots cannot inject targets or attacks into the owning system. */
    @Test
    void exposesImmutableMembershipSnapshots() {
        List<Combatant> actors = system.getCombatants();
        assertThrows(UnsupportedOperationException.class, actors::clear);
        Attack attack = system.tryAttack(source, strike, hitbox).orElseThrow();
        List<Attack> attacks = system.getActiveAttacks();
        assertThrows(UnsupportedOperationException.class, () -> attacks.add(attack));
        system.clear();
        assertEquals(2, actors.size());
        assertEquals(1, attacks.size());
        assertFalse(attacks.get(0).isActive());
    }

    /** Hitboxes do not track moving source geometry and hurtboxes need not equal visual bounds. */
    @Test
    void separatesHitAndHurtGeometryFromPlayerMovement() {
        Attack attack = system.tryAttack(source, strike, hitbox).orElseThrow();
        player.updateMovement(1, 0, 1);
        source.setHurtbox(new CollisionBox(112, 12, 16, 16));
        assertEquals(hitbox, attack.getHitbox());
        system.update(0);
        assertEquals(75, enemy.getHealth().getCurrentHp());
        system.tryAttack(target, strike, new CollisionBox(110, 10, 1, 20));
        system.update(0);
        assertEquals(100, player.getHealth().getCurrentHp());
    }

    /** Zero-damage hits still settle once and cannot alter life state. */
    @Test
    void zeroDamageStillRecordsContact() {
        Attack attack = system.tryAttack(source, new AttackSpec("zero", 0, 1, 0), hitbox).orElseThrow();
        system.update(0);
        assertTrue(attack.hasHit(enemy));
        assertEquals(100, enemy.getHealth().getCurrentHp());
        assertTrue(enemy.isAlive());
    }
}
