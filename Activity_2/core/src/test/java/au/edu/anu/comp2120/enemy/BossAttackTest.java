package au.edu.anu.comp2120.enemy;

import au.edu.anu.comp2120.combat.*;
import au.edu.anu.comp2120.screen.GameConfig;
import au.edu.anu.comp2120.world.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises harmless locked telegraphs and elemental Boss bolts through the real shared pipeline.
 * @author Daoyan Zhu (u7782042)
 */
final class BossAttackTest {
    /** Creates independent fixtures without graphics or audio. */
    BossAttackTest() { }

    /** Windup freezes position and aim; zero-time polling never creates damage. */
    @Test void meleeWaitsThenSettlesExactlyOnce() {
        Fixture f = fixture(BossChallenge.FIRE, 290, List.of());
        CollisionBox body = f.boss.getBounds();
        f.boss.update(f.target, 0);
        CollisionBox warning = f.boss.getMeleeWarning().orElseThrow();
        assertEquals(BossState.WINDUP, f.boss.getState());
        for (int i = 0; i < 100; i++) {
            f.boss.update(f.target, 0);
            f.combat.update(0);
        }
        assertTrue(f.combat.getActiveAttacks().isEmpty());
        assertEquals(1000, f.victim.getHealth().getCurrentHp());
        f.boss.update(f.target, BossConfig.MELEE_WINDUP / 2);
        assertEquals(0.5, f.boss.getWindupProgress(), 1e-9);
        assertEquals(body, f.boss.getBounds());
        assertEquals(warning, f.boss.getMeleeWarning().orElseThrow());
        f.boss.update(f.target, BossConfig.MELEE_WINDUP / 2);
        assertTrue(f.boss.getMeleeWarning().isEmpty());
        assertEquals(warning, f.combat.getActiveAttacks().get(0).getHitbox());
        // Read the submitted pattern's own damage: each guardian trait may tune it, and this test
        // is about settling the strike exactly once rather than about one particular damage value.
        int sweep = f.combat.getActiveAttacks().get(0).getSpecification().damage();
        for (int i = 0; i < 20; i++) { f.combat.update(0); }
        assertEquals(1000 - sweep, f.victim.getHealth().getCurrentHp());
        f.boss.update(f.target, 0);
        assertEquals(1, f.combat.getActiveAttacks().size());
    }

    /** Dodging outside the fixed rectangle during preparation avoids the strike. */
    @Test void dodgeDoesNotDragWarningOrAttackAfterPlayer() {
        Fixture f = fixture(BossChallenge.WATER, 290, List.of());
        f.boss.update(f.target, 0);
        CollisionBox warning = f.boss.getMeleeWarning().orElseThrow();
        f.target.setHurtbox(new CollisionBox(290, 400, 20, 20));
        f.boss.update(f.target, BossConfig.MELEE_WINDUP);
        assertEquals(warning, f.combat.getActiveAttacks().get(0).getHitbox());
        f.combat.update(0);
        assertEquals(1000, f.victim.getHealth().getCurrentHp());
    }

    /** Death, detachment and phase changes cannot release a stale prepared strike. */
    @Test void encounterInterruptionsCancelWindup() {
        for (int reason = 0; reason < 3; reason++) {
            Fixture f = fixture(BossChallenge.ICE, 290, List.of());
            f.boss.update(f.target, 0);
            switch (reason) {
                case 0 -> f.boss.getHealth().takeDamage(400);
                case 1 -> f.combat.clear();
                default -> f.boss.getHealth().takeDamage(200);
            }
            f.boss.update(f.target, 10);
            assertTrue(f.boss.getMeleeWarning().isEmpty());
            assertTrue(f.combat.getActiveAttacks().isEmpty());
            assertEquals(1000, f.victim.getHealth().getCurrentHp());
        }
    }

    /** A confirmed freeze interrupts preparation; thawing requires a complete new windup. */
    @Test void freezeCancelsPreparedAttack() {
        Fixture f = fixture(BossChallenge.FIRE, 290, List.of());
        f.boss.update(f.target, 0);
        for (ElementType element : List.of(ElementType.WATER, ElementType.ICE)) {
            f.combat.tryAttack(f.target, new AttackSpec("freeze-test", 0, 0.1, 0, element), f.boss.getBounds());
            f.combat.update(0);
        }
        f.boss.update(f.target, BossConfig.MELEE_WINDUP);
        assertEquals(BossState.FROZEN, f.boss.getState());
        assertTrue(f.boss.getMeleeWarning().isEmpty());
        assertTrue(f.combat.getActiveAttacks().stream().noneMatch(a -> a.getSource() == f.boss.getCombatant()));
        f.combat.update(ReactionTuning.FREEZE_SECONDS);
        f.boss.update(f.target, 0);
        assertEquals(BossState.WINDUP, f.boss.getState());
        assertEquals(0, f.boss.getWindupProgress());
    }

    /**
     * Every campaign guardian aims its matching elemental bolt; shared cooldown and collision settle it once.
     * @param challenge the guardian whose official map selects the bolt element
     */
    @ParameterizedTest @EnumSource(BossChallenge.class)
    void rangedAttackUsesMatchingElementAndSharedDamage(BossChallenge challenge) {
        Fixture f = fixture(challenge, 500, List.of());
        f.boss.update(f.target, 0);
        Attack bolt = f.combat.getActiveAttacks().get(0);
        assertNotNull(bolt.getProjectile());
        assertEquals(ElementType.valueOf(challenge.name()), bolt.getSpecification().element());
        assertEquals(DamageType.ELEMENTAL, bolt.getSpecification().damageType());
        assertEquals(20, bolt.getHitbox().width());
        assertTrue(bolt.getProjectile().getDirectionX() > 0.99);
        for (int i = 0; i < 50; i++) { f.boss.update(f.target, 0); }
        assertEquals(1, f.combat.getActiveAttacks().size());
        assertEquals(BossConfig.BOLT_COOLDOWN, f.combat.getCooldownRemaining(f.boss.getCombatant()));
        f.combat.update(1);
        assertEquals(1000 - BossConfig.BOLT_DAMAGE, f.victim.getHealth().getCurrentHp());
        assertTrue(f.combat.getActiveAttacks().isEmpty());
        f.combat.update(1);
        assertEquals(1000 - BossConfig.BOLT_DAMAGE, f.victim.getHealth().getCurrentHp());
    }

    /** Swept collision stops an oversized bolt at thin walls, including large frame steps. */
    @Test void boltCannotPassWall() {
        Fixture f = fixture(BossChallenge.LIGHTNING, 500, List.of(new CollisionBox(350, 0, 2, 800)));
        f.boss.update(f.target, 0);
        assertEquals(1, f.combat.getActiveAttacks().size());
        f.combat.update(1);
        assertEquals(1000, f.victim.getHealth().getCurrentHp());
        assertTrue(f.combat.getActiveAttacks().isEmpty());
    }

    /** Missed bolts expire and encounter clear cannot retain them or their original registration. */
    @Test void missedBoltsExpireAndClearReleasesThem() {
        Fixture f = fixture(BossChallenge.WIND, 500, List.of());
        f.boss.update(f.target, 0);
        f.target.setHurtbox(new CollisionBox(500, 400, 20, 20));
        f.combat.update(3);
        assertTrue(f.combat.getActiveAttacks().isEmpty());
        assertEquals(1000, f.victim.getHealth().getCurrentHp());
        f.boss.update(f.target, 0);
        assertEquals(1, f.combat.getActiveAttacks().size());
        f.combat.clear();
        f.boss.update(f.target, 3);
        assertTrue(f.combat.getActiveAttacks().isEmpty());
        assertTrue(f.combat.getCombatants().isEmpty());
    }

    /** A missing/dead target cannot create new ranged attacks. */
    @Test void invalidTargetsCannotFire() {
        Fixture f = fixture(BossChallenge.FIRE, 500, List.of());
        f.boss.update(null, 1);
        f.victim.getHealth().takeDamage(1000);
        f.boss.update(f.target, 1);
        assertTrue(f.combat.getActiveAttacks().isEmpty());
    }

    /** Generic targetability prevents new bolts and cancels a not-yet-active targeted melee strike. */
    @Test void untargetableStopsPreparationAndNewBolts() {
        Fixture melee = fixture(BossChallenge.FIRE, 290, List.of());
        melee.boss.update(melee.target, 0);
        assertTrue(melee.boss.getMeleeWarning().isPresent());
        melee.target.setTargetable(false);
        melee.boss.update(melee.target, BossConfig.MELEE_WINDUP);
        assertTrue(melee.boss.getMeleeWarning().isEmpty());
        assertTrue(melee.combat.getActiveAttacks().isEmpty());
        Fixture ranged = fixture(BossChallenge.WIND, 500, List.of());
        ranged.target.setTargetable(false);
        ranged.boss.update(ranged.target, 1);
        assertTrue(ranged.combat.getActiveAttacks().isEmpty());
    }

    /** Non-staggering reaction displacement also interrupts a fixed windup rather than delaying knockback. */
    @Test void knockbackDuringPreparationResolvesAgainstWorld() {
        Fixture f = fixture(BossChallenge.FIRE, 290, List.of());
        f.boss.getCombatant().getStatus().configurePoise(1000);
        f.boss.update(f.target, 0);
        for (ElementType element : List.of(ElementType.FIRE, ElementType.LIGHTNING)) {
            f.combat.tryAttack(f.target, new AttackSpec("push-test", 0, 0.1, 0, element), f.boss.getBounds());
            f.combat.update(0);
        }
        assertFalse(f.boss.getCombatant().getStatus().blocksActions());
        f.boss.update(f.target, 0);
        assertTrue(f.boss.getBounds().x() < 200);
        assertEquals(0, f.boss.getCombatant().getStatus().getKnockbackX());
        assertTrue(f.boss.getMeleeWarning().isEmpty());
        assertTrue(f.combat.getActiveAttacks().stream().noneMatch(a -> a.getSource() == f.boss.getCombatant()));
    }

    /**
     * Creates a production-element map with a nearby or ranged target and optional blockers.
     * @param challenge campaign element/map
     * @param targetX target's left edge
     * @param walls static collision obstacles
     * @return isolated real Boss/shared-combat fixture
     */
    private Fixture fixture(BossChallenge challenge, double targetX, List<CollisionBox> walls) {
        GameMap map = new GameMap(challenge.mapId(), "Boss test", MapType.BATTLE, 1000, 800,
                walls, Map.of("entry", new SpawnPoint(0, 0)), "entry");
        CombatSystem combat = new CombatSystem();
        Boss boss = new Boss(challenge.id(), challenge.displayName(), new CollisionBox(200, 200, 64, 64),
                map, GameConfig.SENTINEL, combat);
        Enemy victim = new Enemy(1000);
        Combatant target = new Combatant(victim, Faction.PLAYER, new CollisionBox(targetX, 220, 20, 20));
        combat.register(target);
        return new Fixture(combat, boss, victim, target);
    }

    /**
     * Owns one isolated combat scope.
     * @param combat shared settlement engine
     * @param boss tested controller
     * @param victim health-owning target
     * @param target registered adapter
     */
    private record Fixture(CombatSystem combat, Boss boss, Enemy victim, Combatant target) { }
}
