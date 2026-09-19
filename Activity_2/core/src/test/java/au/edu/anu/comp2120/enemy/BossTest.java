package au.edu.anu.comp2120.enemy;

import static org.junit.jupiter.api.Assertions.*;

import au.edu.anu.comp2120.combat.Attack;
import au.edu.anu.comp2120.combat.AttackSpec;
import au.edu.anu.comp2120.combat.CombatSystem;
import au.edu.anu.comp2120.combat.Combatant;
import au.edu.anu.comp2120.combat.Faction;
import au.edu.anu.comp2120.inventory.MaterialDrop;
import au.edu.anu.comp2120.player.Player;
import au.edu.anu.comp2120.screen.GameConfig;
import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.GameMap;
import au.edu.anu.comp2120.world.TestMaps;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Verifies real Boss composition, shared damage, navigation, phases, cooldowns and reward ownership.
 * @author Daoyan Zhu (u7782042)
 */
final class BossTest {
    /** Unobstructed map for deterministic encounter timing. */
    private final GameMap map = TestMaps.rectangle(1000, 800, List.of());
    /** One arena clock for all actors. */
    private final CombatSystem combat = new CombatSystem();
    /** Real player target with sufficient HP for pattern-cycle checks. */
    private final Player player = new Player(new CollisionBox(290, 220, 20, 20), 100, map, 1000);
    /** Canonical registered player hurtbox. */
    private final Combatant target = new Combatant(player, Faction.PLAYER, player.getBounds());
    /** Real encounter using production placeholder tuning. */
    private final Boss boss = new Boss("test-boss", "Test Boss", new CollisionBox(200, 200, 64, 64),
            map, GameConfig.SENTINEL, combat);

    /** Registers the player after the independently registered Boss. */
    BossTest() {
        combat.register(target);
    }

    /** Identity, position and HP expose the same composed Enemy used by the combat adapter. */
    @Test
    void entityUsesOneCanonicalEnemyHealthModel() {
        assertEquals("test-boss", boss.getId());
        assertEquals("Test Boss", boss.getName());
        assertEquals(400, boss.getHealth().getMaxHp());
        assertEquals(400, boss.getHealth().getCurrentHp());
        assertSame(boss.getHealth(), ((Enemy) boss.getCombatant().getEntity()).getHealth());
        assertEquals(boss.getBounds(), boss.getCombatant().getHurtbox());
        assertEquals(2, combat.getCombatants().size());
        assertTrue(boss.isAlive());
        assertEquals(BossState.IDLE, boss.getState());
        assertTrue(boss.claimDrop().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> boss.getCombatant().getEntity().takeDamage(-1));
        assertEquals(400, boss.getHealth().getCurrentHp());
    }

    /** A player attack damages Boss HP through the existing pipeline exactly once. */
    @Test
    void playerDamageUsesCombatAndHitLedger() {
        combat.tryAttack(target, new AttackSpec("player-test", 40, 0.2, 0), boss.getBounds());
        for (int i = 0; i < 20; i++) {
            combat.update(0);
            boss.synchronizeOutcome();
        }
        assertEquals(360, boss.getHealth().getCurrentHp());
        assertEquals(1, boss.getPhase());
        assertTrue(boss.getDrop().isEmpty());
    }

    /** Boss alternates distinct patterns only when shared source cooldown accepts a new invocation. */
    @Test
    void patternsAlternateWithoutBasicEnemyAttackOrSpam() {
        boss.update(target, 0);
        assertEquals(BossState.WINDUP, boss.getState());
        assertTrue(combat.getActiveAttacks().isEmpty());
        boss.update(target, BossConfig.MELEE_WINDUP);
        assertEquals(BossState.ATTACK, boss.getState());
        assertEquals(1000, player.getHealth().getCurrentHp());
        assertEquals(1, combat.getActiveAttacks().size());
        Attack first = combat.getActiveAttacks().get(0);
        assertEquals("boss-sweep", first.getSpecification().type());
        assertSame(boss.getCombatant(), first.getSource());
        combat.update(0);
        assertEquals(985, player.getHealth().getCurrentHp());
        for (int i = 0; i < 50; i++) {
            tick(0);
        }
        assertEquals(1, combat.getActiveAttacks().size());
        assertEquals(985, player.getHealth().getCurrentHp());
        tick(1.2);
        tick(0);
        boss.update(target, BossConfig.MELEE_WINDUP);
        combat.update(0);
        Attack second = combat.getActiveAttacks().get(0);
        assertEquals("boss-slam", second.getSpecification().type());
        assertNotEquals(first.getId(), second.getId());
        assertEquals(965, player.getHealth().getCurrentHp());
        tick(1.6);
        tick(0);
        boss.update(target, BossConfig.MELEE_WINDUP);
        assertEquals("boss-sweep", combat.getActiveAttacks().get(0).getSpecification().type());
    }

    /**
     * Threshold is inclusive; a lethal hit skips phase change and produces defeat instead.
     * @param damage damage applied to the shared health model
     * @param expectedPhase expected one-way phase
     * @param expectedState expected state after outcome synchronization
     */
    @ParameterizedTest
    @CsvSource({"199,1,IDLE", "200,2,PHASE_TRANSITION", "201,2,PHASE_TRANSITION", "400,1,DEAD"})
    void thresholdAndDeathPrecedence(int damage, int expectedPhase, BossState expectedState) {
        boss.getCombatant().getEntity().takeDamage(damage);
        boss.synchronizeOutcome();
        assertEquals(expectedPhase, boss.getPhase());
        assertEquals(expectedState, boss.getState());
    }

    /** Phase change cancels pending attacks but never resets cooldown or restarts on repeated checks/healing. */
    @Test
    void phaseTransitionIsOneWayAndPreservesCooldown() {
        boss.update(target, 0);
        boss.update(target, BossConfig.MELEE_WINDUP);
        Attack old = combat.getActiveAttacks().get(0);
        boss.getHealth().takeDamage(200);
        boss.synchronizeOutcome();
        assertFalse(old.isActive());
        assertTrue(combat.getActiveAttacks().isEmpty());
        assertEquals(1.2, combat.getCooldownRemaining(boss.getCombatant()));
        CollisionBox before = boss.getBounds();
        for (int i = 0; i < 30; i++) {
            tick(0);
        }
        assertEquals(0.75, boss.getTransitionRemaining());
        tick(0.25);
        assertEquals(0.5, boss.getTransitionRemaining());
        assertEquals(before, boss.getBounds());
        boss.getHealth().heal(200);
        boss.synchronizeOutcome();
        assertEquals(2, boss.getPhase());
        assertEquals(0.5, boss.getTransitionRemaining());
        tick(0.5);
        tick(0);
        assertTrue(combat.getActiveAttacks().isEmpty());
        tick(0.5);
        tick(0);
        boss.update(target, BossConfig.MELEE_WINDUP);
        assertEquals("boss-heavy-sweep", combat.getActiveAttacks().get(0).getSpecification().type());
        boss.getHealth().takeDamage(220);
        boss.synchronizeOutcome();
        assertEquals(0, boss.getTransitionRemaining());
        tick(0.8);
        tick(0);
        boss.update(target, BossConfig.MELEE_WINDUP);
        assertEquals("boss-heavy-slam", combat.getActiveAttacks().get(0).getSpecification().type());
    }

    /** Shared lethal damage cancels queued Boss attacks and creates exactly one claimable material stack. */
    @Test
    void lethalCombatCreatesOneRewardAndStopsAllBehavior() {
        combat.tryAttack(target, new AttackSpec("lethal", 1000, 0.2, 0), boss.getBounds());
        boss.update(target, 0);
        boss.update(target, BossConfig.MELEE_WINDUP);
        Attack queued = combat.getActiveAttacks().get(1);
        combat.update(0);
        assertEquals(BossState.DEAD, boss.getState());
        boss.synchronizeOutcome();
        assertFalse(queued.isActive());
        assertEquals(1000, player.getHealth().getCurrentHp());
        assertEquals(0, boss.getHealth().getCurrentHp());
        CollisionBox position = boss.getBounds();
        MaterialDrop drop = boss.getDrop().orElseThrow();
        assertEquals("sentinel-core", drop.materialId());
        assertEquals(1, drop.quantity());
        assertEquals(map.getId(), drop.mapId());
        assertTrue(map.isWalkable(drop.bounds()));
        assertTrue(position.overlaps(drop.bounds()));
        for (int i = 0; i < 100; i++) {
            tick(0.1);
            assertSame(drop, boss.getDrop().orElseThrow());
        }
        assertEquals(position, boss.getBounds());
        assertEquals(drop, boss.claimDrop().orElseThrow());
        assertTrue(boss.claimDrop().isEmpty());
        boss.getHealth().restoreFull();
        tick(1);
        assertFalse(boss.isAlive());
        assertEquals(BossState.DEAD, boss.getState());
        assertTrue(boss.getDrop().isEmpty());
        assertTrue(combat.getActiveAttacks().isEmpty());
    }

    /** Death during the transition cannot trigger its first phase-two pattern. */
    @Test
    void deathDuringTransitionOnlyDropsOnce() {
        boss.getHealth().takeDamage(200);
        tick(0);
        boss.getHealth().takeDamage(200);
        tick(0);
        assertEquals(BossState.DEAD, boss.getState());
        assertEquals(0, boss.getTransitionRemaining());
        assertTrue(combat.getActiveAttacks().isEmpty());
        assertTrue(boss.claimDrop().isPresent());
        tick(10);
        assertTrue(boss.claimDrop().isEmpty());
    }

    /** Invalid targets and lost detection reuse normal EnemyAI behavior rather than a second targeting system. */
    @Test
    void lostAndInvalidTargetsStopPursuit() {
        target.setHurtbox(new CollisionBox(550, 220, 20, 20));
        tick(0.1);
        assertEquals(BossState.CHASE, boss.getState());
        assertTrue(boss.getBounds().x() > 200);
        target.setHurtbox(new CollisionBox(950, 700, 20, 20));
        CollisionBox before = boss.getBounds();
        tick(1);
        assertEquals(BossState.IDLE, boss.getState());
        boss.update(null, 1);
        boss.update(boss.getCombatant(), 1);
        boss.update(new Combatant(new Enemy(), Faction.PLAYER, before), 1);
        assertEquals(before, boss.getBounds());
        assertTrue(combat.getActiveAttacks().isEmpty());
    }

    /** Boss reuses full-body A* around an obstacle and never walks through it or outside the map. */
    @Test
    void largeBodyNavigatesAroundWallWithExistingAI() {
        GameMap obstacleMap = TestMaps.rectangle(800, 600, List.of(new CollisionBox(350, 0, 20, 300)));
        CombatSystem arena = new CombatSystem();
        Boss navigating = new Boss("navigator", "Navigator", new CollisionBox(200, 100, 64, 64),
                obstacleMap, GameConfig.SENTINEL, arena);
        Combatant destination = new Combatant(new Enemy(10000), Faction.PLAYER, new CollisionBox(480, 110, 20, 20));
        arena.register(destination);
        boolean wentAbove = false;
        for (int i = 0; i < 250; i++) {
            navigating.update(destination, 0.1);
            arena.update(0.1);
            navigating.synchronizeOutcome();
            assertTrue(obstacleMap.isWalkable(navigating.getBounds()));
            wentAbove |= navigating.getBounds().y() >= 300;
        }
        assertTrue(wentAbove);
        assertTrue(navigating.getBounds().x() >= 370);
        assertTrue(navigating.getState() == BossState.ATTACK || navigating.getState() == BossState.WINDUP);
        assertTrue(((Enemy) destination.getEntity()).getHealth().getCurrentHp() < 10000);
    }

    /** Detached old-map encounters cannot move, attack, generate or claim stale rewards. */
    @Test
    void detachmentStopsEncountersAndLootAccess() {
        boss.getHealth().takeDamage(400);
        boss.synchronizeOutcome();
        assertTrue(boss.getDrop().isPresent());
        combat.clear();
        tick(1);
        assertTrue(boss.getDrop().isEmpty());
        assertTrue(boss.claimDrop().isEmpty());
        Boss abandoned = new Boss("abandoned", "Abandoned", new CollisionBox(100, 100, 64, 64),
                map, GameConfig.SENTINEL, combat);
        combat.clear();
        abandoned.getHealth().takeDamage(400);
        abandoned.update(null, 1);
        assertTrue(abandoned.getDrop().isEmpty());
    }

    /** Multiple encounter instances share only immutable tuning, never HP, cooldowns or reward IDs. */
    @Test
    void encountersOwnIndependentStateAndRewards() {
        Boss other = new Boss("other", "Other", new CollisionBox(100, 100, 64, 64), map,
                GameConfig.SENTINEL, combat);
        boss.update(target, 0);
        assertEquals(0, combat.getCooldownRemaining(other.getCombatant()));
        boss.getHealth().takeDamage(400);
        boss.synchronizeOutcome();
        assertEquals(400, other.getHealth().getCurrentHp());
        assertEquals(1, other.getPhase());
        assertTrue(other.getDrop().isEmpty());
        other.getHealth().takeDamage(400);
        other.synchronizeOutcome();
        assertNotEquals(boss.getDrop().orElseThrow().id(), other.getDrop().orElseThrow().id());
        boss.claimDrop();
        assertTrue(other.getDrop().isPresent());
    }

    /**
     * Invalid simulation time cannot mutate phase, reward, position or cooldowns.
     * @param delta rejected elapsed seconds
     */
    @ParameterizedTest
    @ValueSource(doubles = {-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.MAX_VALUE})
    void invalidTimeIsRejectedBeforeMutation(double delta) {
        boss.getHealth().takeDamage(200);
        CollisionBox before = boss.getBounds();
        assertThrows(IllegalArgumentException.class, () -> boss.update(target, delta));
        assertEquals(1, boss.getPhase());
        assertEquals(before, boss.getBounds());
        assertTrue(boss.getDrop().isEmpty());
        assertTrue(combat.getActiveAttacks().isEmpty());
    }

    /** Invalid construction never leaves a partially registered Boss in the arena. */
    @Test
    void invalidIdentityOrPlacementDoesNotRegister() {
        int count = combat.getCombatants().size();
        assertThrows(IllegalArgumentException.class, () -> new Boss(" ", "Boss", boss.getBounds(), map,
                GameConfig.SENTINEL, combat));
        assertThrows(IllegalArgumentException.class, () -> new Boss("bad", "Boss", new CollisionBox(-1, 0, 64, 64),
                map, GameConfig.SENTINEL, combat));
        assertThrows(NullPointerException.class, () -> new Boss("bad", "Boss", null, map, GameConfig.SENTINEL, combat));
        assertEquals(count, combat.getCombatants().size());
    }

    /**
     * Runs encounter decisions and one shared combat step, followed by settled outcome processing.
     * @param delta elapsed simulation seconds
     */
    private void tick(double delta) {
        boss.update(target, delta);
        combat.update(delta);
        boss.synchronizeOutcome();
    }
}
