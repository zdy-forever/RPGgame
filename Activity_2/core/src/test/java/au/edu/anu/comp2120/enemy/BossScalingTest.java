package au.edu.anu.comp2120.enemy;

import au.edu.anu.comp2120.combat.*;
import au.edu.anu.comp2120.screen.GameConfig;
import au.edu.anu.comp2120.world.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies logarithmic rematch tuning, caps, unchanged telegraphs and both shared attack deliveries.
 * @author Daoyan Zhu (u7782042)
 */
final class BossScalingTest {
    /** Creates independent graphics-free test fixtures. */
    BossScalingTest() { }

    /**
     * Known logarithm points and very large counts remain finite and capped.
     * @param wins previously successful challenges
     * @param hp expected scaled production HP
     * @param rate expected attack frequency multiplier
     */
    @ParameterizedTest
    @CsvSource({"0,400,1", "1,520,1.075257498916", "9,800,1.25", "99,1200,1.5",
            "999,1600,1.75", "9999,1600,2", "2147483647,1600,2"})
    void logarithmicValuesAndCaps(int wins, int hp, double rate) {
        var scaled = BossScaling.apply(GameConfig.SENTINEL, wins);
        assertEquals(hp, scaled.maxHp());
        assertEquals(rate, BossScaling.attackRate(wins), 1e-10);
        assertEquals(1.2 / rate, scaled.phaseOne().get(0).attack().cooldownSeconds(), 1e-10);
        assertEquals(0.8 / rate, scaled.phaseTwo().get(0).attack().cooldownSeconds(), 1e-10);
        assertTrue(scaled.phaseOne().get(0).attack().cooldownSeconds() >= 0.6);
    }

    /** Each additional win has diminishing health/rate gains before the caps; attempts are not inputs. */
    @Test void growthIsMonotonicWithDiminishingReturns() {
        double previousHp = 1, previousRate = 1;
        double hpGain = Double.MAX_VALUE, rateGain = Double.MAX_VALUE;
        for (int wins = 1; wins <= 10000; wins++) {
            double hp = BossScaling.healthMultiplier(wins), rate = BossScaling.attackRate(wins);
            assertTrue(hp >= previousHp && hp <= BossScaling.MAX_HEALTH_MULTIPLIER);
            assertTrue(rate >= previousRate && rate <= BossScaling.MAX_ATTACK_RATE);
            assertTrue(hp - previousHp <= hpGain + 1e-12);
            assertTrue(rate - previousRate <= rateGain + 1e-12);
            hpGain = hp - previousHp; rateGain = rate - previousRate;
            previousHp = hp; previousRate = rate;
        }
    }

    /** Tuning copies preserve damage, geometry, navigation, elements, rewards and the original data. */
    @Test void onlyHpAndCooldownsChange() {
        BossConfig base = GameConfig.SENTINEL;
        assertSame(base, BossScaling.apply(base, 0));
        BossConfig scaled = BossScaling.apply(base, 99);
        assertEquals(400, base.maxHp());
        assertEquals(1.2, base.phaseOne().get(0).attack().cooldownSeconds());
        assertSame(base.navigation(), scaled.navigation());
        assertEquals(base.phaseTwoFraction(), scaled.phaseTwoFraction());
        assertEquals(base.transitionSeconds(), scaled.transitionSeconds());
        assertEquals(base.materialId(), scaled.materialId());
        assertEquals(base.materialQuantity(), scaled.materialQuantity());
        for (var pair : List.of(List.of(base.phaseOne(), scaled.phaseOne()), List.of(base.phaseTwo(), scaled.phaseTwo()))) {
            for (int index = 0; index < pair.get(0).size(); index++) {
                var original = pair.get(0).get(index);
                var actual = pair.get(1).get(index);
                assertSame(original.shape(), actual.shape());
                assertEquals(original.range(), actual.range());
                assertEquals(original.attack().damage(), actual.attack().damage());
                assertEquals(original.attack().element(), actual.attack().element());
                assertEquals(original.attack().damageType(), actual.attack().damageType());
                assertEquals(original.attack().blockable(), actual.attack().blockable());
                assertEquals(original.attack().activeSeconds(), actual.attack().activeSeconds());
                assertTrue(actual.attack().cooldownSeconds() >= actual.attack().activeSeconds());
            }
        }
    }

    /** Health multiplication saturates instead of wrapping to a negative integer. */
    @Test void extremeBaseHealthStaysValid() {
        var base = GameConfig.SENTINEL;
        var extreme = new BossConfig(Integer.MAX_VALUE, base.navigation(), base.phaseTwoFraction(),
                base.transitionSeconds(), base.phaseOne(), base.phaseTwo(), base.materialId(), base.materialQuantity());
        assertEquals(Integer.MAX_VALUE, BossScaling.apply(extreme, Integer.MAX_VALUE).maxHp());
        assertThrows(IllegalArgumentException.class, () -> BossScaling.healthMultiplier(-1));
        assertThrows(IllegalArgumentException.class, () -> BossScaling.attackRate(-1));
        assertThrows(IllegalArgumentException.class, () -> BossScaling.apply(base, -1));
        assertThrows(NullPointerException.class, () -> BossScaling.apply(null, 0));
    }

    /**
     * Real melee preserves the full warning window while using scaled HP and shared cooldown.
     * @param wins successful prior battles
     */
    @ParameterizedTest @ValueSource(ints = {0, 1, 99, Integer.MAX_VALUE})
    void realBossMeleeKeepsReadableWindup(int wins) {
        GameMap map = TestMaps.rectangle(1000, 800, List.of());
        CombatSystem combat = new CombatSystem();
        Boss boss = new Boss("test", "Boss", new CollisionBox(200, 200, 64, 64), map, GameConfig.SENTINEL, combat, wins);
        Combatant target = new Combatant(new Enemy(1000), Faction.PLAYER, new CollisionBox(290, 220, 20, 20));
        combat.register(target);
        assertEquals(BossScaling.apply(GameConfig.SENTINEL, wins).maxHp(), boss.getHealth().getCurrentHp());
        boss.update(target, 0);
        boss.update(target, BossConfig.MELEE_WINDUP / 2);
        assertEquals(BossState.WINDUP, boss.getState());
        assertEquals(0.5, boss.getWindupProgress());
        assertTrue(combat.getActiveAttacks().isEmpty());
        boss.update(target, BossConfig.MELEE_WINDUP / 2);
        assertEquals(1.2 / BossScaling.attackRate(wins), combat.getCooldownRemaining(boss.getCombatant()), 1e-10);
        assertEquals(15, combat.getActiveAttacks().get(0).getSpecification().damage());
        combat.update(0);
        assertEquals(985, ((Enemy) target.getEntity()).getHealth().getCurrentHp());
        boss.getHealth().takeDamage((boss.getHealth().getMaxHp() + 1) / 2);
        boss.synchronizeOutcome();
        assertEquals(2, boss.getPhase());
    }

    /** Ranged bolts use the same rate cap without faster flight, extra damage or extra projectiles. */
    @Test void rangedCooldownCapsAtHalfBase() {
        GameMap map = MapCatalog.createMaps().stream().filter(m -> m.getId().equals(MapCatalog.CROSSROADS)).findFirst().orElseThrow();
        CombatSystem combat = new CombatSystem();
        Boss boss = new Boss("fire", "Fire Boss", new CollisionBox(200, 200, 64, 64), map,
                GameConfig.SENTINEL, combat, Integer.MAX_VALUE);
        Combatant target = new Combatant(new Enemy(1000), Faction.PLAYER, new CollisionBox(500, 220, 20, 20));
        combat.register(target);
        boss.update(target, 0);
        assertEquals(1, combat.getActiveAttacks().size());
        Attack bolt = combat.getActiveAttacks().get(0);
        assertNotNull(bolt.getProjectile());
        assertEquals(ElementType.FIRE, bolt.getSpecification().element());
        assertEquals(BossConfig.BOLT_DAMAGE, bolt.getSpecification().damage());
        assertEquals(BossConfig.BOLT_COOLDOWN / 2, combat.getCooldownRemaining(boss.getCombatant()));
        boss.update(target, 0);
        assertEquals(1, combat.getActiveAttacks().size());
    }
}
