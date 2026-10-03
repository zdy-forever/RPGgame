package au.edu.anu.comp2120.player;

import static org.junit.jupiter.api.Assertions.*;

import au.edu.anu.comp2120.combat.*;
import au.edu.anu.comp2120.enemy.Boss;
import au.edu.anu.comp2120.screen.GameConfig;
import au.edu.anu.comp2120.world.*;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Graphics-free acceptance tests for the specified first-version skills on the one shared pipeline.
 * @author Daoyan Zhu (u7782042)
 */
final class FinalClassSkillsTest {
    /** Creates an isolated final-skill test suite. */
    FinalClassSkillsTest() {
    }

    /** Archer J is one fast straight non-piercing arrow; K atomically submits five spread arrows. */
    @Test
    void archerLightAndHeavyVolley() {
        Fixture f = fixture(PlayerClass.ARCHER, UltimateMode.A, List.of(), 1);
        f.step(0, command(AbilitySlot.LIGHT));
        assertEquals(1, f.combat().getActiveAttacks().size());
        Attack arrow = f.combat().getActiveAttacks().get(0);
        assertEquals(1, arrow.getProjectile().getDirectionX());
        assertEquals(0, arrow.getProjectile().getDirectionY());
        double lightCooldown = f.abilities().getCooldownRemaining(AbilitySlot.LIGHT);
        f.step(0, command(AbilitySlot.LIGHT));
        assertEquals(1, f.combat().getActiveAttacks().size());
        f.step(0.3, PlayerCommand.NONE);
        assertEquals(9980, f.health().getCurrentHp());
        assertTrue(f.combat().getActiveAttacks().isEmpty());
        f.step(0, command(AbilitySlot.SPECIAL));
        List<Attack> volley = f.combat().getActiveAttacks();
        assertEquals(5, volley.size());
        assertTrue(f.abilities().getCooldownRemaining(AbilitySlot.SPECIAL) > lightCooldown);
        assertEquals(5, volley.stream().map(a -> a.getProjectile().getDirectionY()).distinct().count());
        assertTrue(volley.get(0).getProjectile().getDirectionY() < 0);
        assertTrue(volley.get(4).getProjectile().getDirectionY() > 0);
        f.step(2, PlayerCommand.NONE);
        assertTrue(f.health().getCurrentHp() < 9980);
        assertTrue(f.combat().getActiveAttacks().isEmpty());
    }

    /** Shared capacity rejection must not emit a partial K volley or charge its slot cooldown. */
    @Test
    void volleyCapacityIsAtomic() {
        Fixture f = fixture(PlayerClass.ARCHER, UltimateMode.A, List.of(), 1);
        AttackSpec spec = new AttackSpec("capacity", 1, 1, 0);
        ProjectileSpec shot = new ProjectileSpec(1, 1, 100, 1, 0);
        for (int i = 0; i < 254; i++) {
            assertEquals(1, f.combat().tryProjectiles(f.source(), spec, shot, 100, -1, 0,
                    f.player().getCollisionArea()).size());
        }
        assertTrue(f.abilities().update(0, command(AbilitySlot.SPECIAL)).isEmpty());
        f.combat().update(0);
        f.abilities().afterCombat();
        assertEquals(254, f.combat().getActiveAttacks().size());
        assertEquals(0, f.abilities().getCooldownRemaining(AbilitySlot.SPECIAL));
    }

    /** Arrow Rain captures target geometry and never follows a target moving behind a wall/outside the field. */
    @Test
    void rainIsFixedFiniteAndOccluded() {
        Fixture f = fixture(PlayerClass.ARCHER, UltimateMode.A,
                List.of(new CollisionBox(290, 0, 10, 1000)), 1);
        f.step(0, command(AbilitySlot.ULTIMATE));
        CollisionBox fixed = f.combat().getActiveAttacks().get(0).getHitbox();
        assertEquals(9982, f.health().getCurrentHp());
        f.target().setHurtbox(new CollisionBox(310, 100, 20, 20));
        for (int i = 0; i < 30; i++) {
            f.step(0.1, PlayerCommand.NONE);
            assertTrue(f.combat().getActiveAttacks().stream().allMatch(a -> a.getHitbox().equals(fixed)));
        }
        assertEquals(9982, f.health().getCurrentHp());
        assertEquals("NONE", f.abilities().getSkillStatus());
        Fixture clear = fixture(PlayerClass.ARCHER, UltimateMode.A, List.of(), 1);
        clear.step(0, command(AbilitySlot.ULTIMATE));
        for (int i = 0; i < 100; i++) {
            clear.step(0, command(AbilitySlot.ULTIMATE));
        }
        assertEquals(9982, clear.health().getCurrentHp());
        advance(clear, 3);
        assertEquals(10000 - SkillTuning.RAIN_TICKS * 18, clear.health().getCurrentHp());
        advance(clear, 5);
        assertEquals(10000 - SkillTuning.RAIN_TICKS * 18, clear.health().getCurrentHp());
        Fixture away = fixture(PlayerClass.ARCHER, UltimateMode.A, List.of(), 1);
        away.step(0, command(AbilitySlot.ULTIMATE));
        away.target().setHurtbox(new CollisionBox(800, 500, 64, 64));
        advance(away, 3);
        assertEquals(9982, away.health().getCurrentHp());
    }

    /** Targetability stops new Boss targeting but never filters already-active AoE damage. */
    @Test
    void untargetableIsNotInvulnerability() {
        Fixture f = fixture(PlayerClass.ARCHER, UltimateMode.B, List.of(), 1);
        Boss boss = new Boss("boss", "Boss", new CollisionBox(140, 100, 64, 64),
                (GameMap) f.player().getCollisionArea(), GameConfig.SENTINEL, f.combat());
        f.step(0, command(AbilitySlot.ULTIMATE));
        assertFalse(f.source().isTargetable());
        boss.update(f.source(), 0);
        assertTrue(f.combat().getActiveAttacks().isEmpty());
        hit(f, false, ElementType.NONE);
        assertEquals(90, f.player().getHealth().getCurrentHp());
        assertFalse(f.source().isTargetable());
        advance(f, 3.1);
        assertTrue(f.source().isTargetable());
        boss.update(f.source(), 0);
        boss.update(f.source(), au.edu.anu.comp2120.enemy.BossConfig.MELEE_WINDUP);
        assertFalse(f.combat().getActiveAttacks().isEmpty());
    }

    /** Mage K cycles precisely, creates no attack and J captures the element selected at launch. */
    @Test
    void mageElementCycleAndNormalProjectile() {
        Fixture f = fixture(PlayerClass.MAGE, UltimateMode.A, List.of(), 1);
        assertEquals(ElementType.FIRE, f.player().getMageElement());
        for (int repeat = 0; repeat < 2; repeat++) {
            for (ElementType expected : List.of(ElementType.WATER, ElementType.LIGHTNING, ElementType.ICE,
                    ElementType.WIND, ElementType.FIRE)) {
                f.step(0.1, command(AbilitySlot.SPECIAL));
                assertEquals(expected, f.player().getMageElement());
                assertTrue(f.combat().getActiveAttacks().isEmpty());
            }
        }
        f.step(0.1, command(AbilitySlot.SPECIAL));
        f.step(0, command(AbilitySlot.LIGHT));
        assertEquals(1, f.combat().getActiveAttacks().size());
        assertEquals(ElementType.WATER, f.combat().getActiveAttacks().get(0).getSpecification().element());
        f.step(0, command(AbilitySlot.SPECIAL));
        assertEquals(ElementType.WATER, f.player().getMageElement());
        advance(f, 2);
        f.player().takeDamage(100);
        f.step(1, command(AbilitySlot.SPECIAL));
        assertEquals(ElementType.WATER, f.player().getMageElement());
    }

    /** Casting captures position/element and locks all movement and J/K/L/O until completion. */
    @Test
    void circleLocksInputAndCapturesPosition() {
        Fixture f = fixture(PlayerClass.MAGE, UltimateMode.A, List.of(), 1);
        CollisionBox body = f.player().getBounds();
        f.step(0.1, new PlayerCommand(1, 1, true, true, false, true));
        assertEquals(body, f.player().getBounds());
        assertEquals("CAST", f.abilities().getSkillStatus());
        assertTrue(f.combat().getActiveAttacks().isEmpty());
        f.target().setHurtbox(new CollisionBox(800, 500, 64, 64));
        for (int i = 0; i < 13; i++) {
            f.step(0.1, new PlayerCommand(1, 1, true, true, true, true));
        }
        assertEquals(body, f.player().getBounds());
        assertEquals(ElementType.FIRE, f.player().getMageElement());
        assertTrue(f.combat().getActiveAttacks().isEmpty());
        f.step(0.2, PlayerCommand.NONE);
        assertEquals("FIELD", f.abilities().getSkillStatus());
        f.step(0, PlayerCommand.NONE);
        Attack wave = f.combat().getActiveAttacks().get(0);
        assertEquals(new CollisionBox(40, -60, 384, 384), wave.getHitbox());
        assertEquals(ElementType.FIRE, wave.getSpecification().element());
        assertEquals(10000, f.health().getCurrentHp());
        advance(f, 3);
        assertEquals("NONE", f.abilities().getSkillStatus());
    }

    /** Effective damage interrupts even if healed in the same settlement window; zero damage does not. */
    @Test
    void circleInterruptsOnDamageWithoutRefund() {
        Fixture f = fixture(PlayerClass.MAGE, UltimateMode.A, List.of(), 1);
        f.step(0, command(AbilitySlot.ULTIMATE));
        double cooldown = f.abilities().getCooldownRemaining(AbilitySlot.ULTIMATE);
        f.abilities().update(SkillTuning.CAST_SECONDS, PlayerCommand.NONE);
        hit(f, false, ElementType.NONE);
        f.player().getHealth().heal(10);
        f.abilities().afterCombat();
        assertEquals("NONE", f.abilities().getSkillStatus());
        assertTrue(f.abilities().getCooldownRemaining(AbilitySlot.ULTIMATE) > 0);
        assertTrue(f.abilities().getCooldownRemaining(AbilitySlot.ULTIMATE) < cooldown);
        advance(f, 3);
        assertEquals(10000, f.health().getCurrentHp());
        Fixture zero = fixture(PlayerClass.MAGE, UltimateMode.A, List.of(), 1);
        zero.step(0, command(AbilitySlot.ULTIMATE));
        zero.player().takeDamage(0);
        advance(zero, 4.5);
        assertEquals(10000 - SkillTuning.FIELD_TICKS * 25, zero.health().getCurrentHp());
    }

    /** Damage between frames cancels casting before a large update can emit a field wave. */
    @Test void circleInterruptedBeforeUpdateNeverEmitsDelayedDamage() {
        Fixture f = fixture(PlayerClass.MAGE, UltimateMode.A, List.of(), 1);
        f.step(0, command(AbilitySlot.ULTIMATE));
        f.player().takeDamage(1);
        f.step(5, PlayerCommand.NONE);
        assertEquals("NONE", f.abilities().getSkillStatus());
        assertEquals(10000, f.health().getCurrentHp());
        assertTrue(f.combat().getActiveAttacks().isEmpty());
    }

    /**
     * Rejected cooldown restoration must leave every existing timer unchanged.
     * @param invalid invalid finite/NaN/infinite saved time
     */
    @ParameterizedTest @ValueSource(doubles = {-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void invalidCooldownRestoreIsAtomic(double invalid) {
        Fixture f = fixture(PlayerClass.ARCHER, UltimateMode.A, List.of(), 1);
        f.step(0, command(AbilitySlot.LIGHT));
        var before = new java.util.EnumMap<AbilitySlot, Double>(AbilitySlot.class);
        for (var slot : AbilitySlot.values()) { before.put(slot, f.abilities().getCooldownRemaining(slot)); }
        var corrupt = new java.util.EnumMap<>(before); corrupt.put(AbilitySlot.ULTIMATE, invalid);
        assertThrows(IllegalArgumentException.class, () -> f.abilities().restoreCooldowns(corrupt));
        assertThrows(IllegalArgumentException.class, () -> f.abilities().restoreCooldowns(Map.of()));
        for (var slot : AbilitySlot.values()) { assertEquals(before.get(slot), f.abilities().getCooldownRemaining(slot)); }
    }

    /** Incomplete profession choices and NONE elements cannot partly replace existing player selections. */
    @Test void invalidSelectionRestoreIsAtomic() {
        Fixture f = fixture(PlayerClass.MAGE, UltimateMode.B, List.of(), 1);
        var before = f.player().savedModes();
        assertThrows(IllegalArgumentException.class,
                () -> f.player().restoreSelections(PlayerClass.KATANA, ElementType.NONE, before));
        assertThrows(IllegalArgumentException.class,
                () -> f.player().restoreSelections(PlayerClass.KATANA, ElementType.ICE, Map.of()));
        assertEquals(PlayerClass.MAGE, f.player().getPlayerClass());
        assertEquals(ElementType.FIRE, f.player().getMageElement());
        assertEquals(before, f.player().savedModes());
    }

    /** Fixed Magic Circle centres use the same blocker query for every finite wave. */
    @Test
    void circleCannotTickAcrossWall() {
        Fixture f = fixture(PlayerClass.MAGE, UltimateMode.A,
                List.of(new CollisionBox(290, 0, 10, 1000)), 1);
        f.step(0, command(AbilitySlot.ULTIMATE));
        f.target().setHurtbox(new CollisionBox(310, 100, 20, 20));
        advance(f, 5);
        assertEquals(10000, f.health().getCurrentHp());
        assertEquals(ElementType.NONE, f.combat().getElementStatus(f.target()).element());
    }

    /** Seeded random draws are independent per bullet, can match, and disappear when the buff expires. */
    @Test
    void rapidBarrageIsSeededIndependentAndTemporary() {
        Fixture a = fixture(PlayerClass.MAGE, UltimateMode.B, List.of(), 7);
        Fixture b = fixture(PlayerClass.MAGE, UltimateMode.B, List.of(), 7);
        Random expected = new Random(7);
        List<ElementType> pool = List.of(ElementType.FIRE, ElementType.WATER, ElementType.LIGHTNING,
                ElementType.ICE, ElementType.WIND);
        a.step(0, command(AbilitySlot.ULTIMATE));
        b.step(0, command(AbilitySlot.ULTIMATE));
        for (int volley = 0; volley < 4; volley++) {
            a.combat().cancelAttacks(a.source());
            b.combat().cancelAttacks(b.source());
            a.step(0, command(AbilitySlot.LIGHT));
            b.step(0, command(AbilitySlot.LIGHT));
            List<ElementType> first = a.combat().getActiveAttacks().stream().map(attack -> attack.getSpecification().element()).toList();
            assertEquals(List.of(pool.get(expected.nextInt(5)), pool.get(expected.nextInt(5))), first);
            assertEquals(first, b.combat().getActiveAttacks().stream().map(attack -> attack.getSpecification().element()).toList());
            assertEquals(2, first.size());
            assertEquals(SkillTuning.RAPID_COOLDOWN, a.abilities().getCooldownRemaining(AbilitySlot.LIGHT));
            a.step(0.2, PlayerCommand.NONE);
            b.step(0.2, PlayerCommand.NONE);
        }
        advance(a, 4);
        a.combat().cancelAttacks(a.source());
        a.step(0, command(AbilitySlot.LIGHT));
        assertEquals(1, a.combat().getActiveAttacks().size());
        assertEquals(ElementType.FIRE, a.combat().getActiveAttacks().get(0).getSpecification().element());
        assertEquals(0.5, a.abilities().getCooldownRemaining(AbilitySlot.LIGHT));
        Fixture same = fixture(PlayerClass.MAGE, UltimateMode.B, List.of(), 4);
        same.step(0, command(AbilitySlot.ULTIMATE));
        same.step(0, command(AbilitySlot.LIGHT));
        // Seed 4 gives equal draws, proving the implementation does not force distinct elements.
        Random seed = new Random(4);
        assertEquals(List.of(pool.get(seed.nextInt(5)), pool.get(seed.nextInt(5))),
                same.combat().getActiveAttacks().stream().map(attack -> attack.getSpecification().element()).toList());
        assertEquals(1, same.combat().getActiveAttacks().stream()
                .map(attack -> attack.getSpecification().element()).distinct().count());
    }

    /** K blocks one hit, then moves through shared collision and emits no more than four segments. */
    @Test
    void katanaParryAndCounterDash() {
        Fixture f = fixture(PlayerClass.KATANA, UltimateMode.A, List.of(), 1);
        f.step(0, command(AbilitySlot.SPECIAL));
        f.abilities().update(0.1, PlayerCommand.NONE);
        hit(f, true, ElementType.FIRE);
        assertEquals(100, f.player().getHealth().getCurrentHp());
        assertEquals(ElementType.NONE, f.combat().getElementStatus(f.source()).element());
        assertEquals("PARRY", f.abilities().getSkillStatus());
        f.abilities().afterCombat();
        assertEquals("COUNTER_DASH", f.abilities().getSkillStatus());
        advance(f, 0.5);
        assertTrue(f.player().getX() > 100);
        assertTrue(f.health().getCurrentHp() < 10000);
        assertTrue(f.health().getCurrentHp() >= 10000 - SkillTuning.COUNTER_TICKS * 12);
        int hp = f.health().getCurrentHp();
        advance(f, 3);
        assertEquals(hp, f.health().getCurrentHp());
    }

    /** A missed K has a longer locked recovery and cannot be bypassed with any action spam. */
    @Test
    void missedParryHasLockedRecovery() {
        Fixture f = fixture(PlayerClass.KATANA, UltimateMode.A, List.of(), 1);
        f.step(SkillTuning.PARRY_SECONDS, command(AbilitySlot.SPECIAL));
        assertEquals("RECOVERY", f.abilities().getSkillStatus());
        CollisionBox before = f.player().getBounds();
        f.step(0.5, new PlayerCommand(1, 1, true, true, true, true));
        assertEquals(before, f.player().getBounds());
        assertTrue(f.combat().getActiveAttacks().isEmpty());
        f.step(0.4, PlayerCommand.NONE);
        assertEquals("NONE", f.abilities().getSkillStatus());
    }

    /** Stance shares interception, triggers once and permits later or unblockable hits to damage normally. */
    @Test
    void stanceCountersOnlyOnce() {
        Fixture f = fixture(PlayerClass.KATANA, UltimateMode.A, List.of(), 1);
        f.step(0, command(AbilitySlot.ULTIMATE));
        f.abilities().update(0.1, PlayerCommand.NONE);
        hit(f, true, ElementType.NONE);
        hit(f, true, ElementType.NONE);
        assertEquals(90, f.player().getHealth().getCurrentHp());
        f.abilities().afterCombat();
        f.combat().update(0);
        assertEquals(9900, f.health().getCurrentHp());
        f.abilities().afterCombat();
        f.combat().update(0);
        assertEquals(9900, f.health().getCurrentHp());
        assertEquals("NONE", f.abilities().getSkillStatus());
        Fixture unblockable = fixture(PlayerClass.KATANA, UltimateMode.A, List.of(), 1);
        unblockable.step(0, command(AbilitySlot.SPECIAL));
        hit(unblockable, false, ElementType.NONE);
        assertEquals(90, unblockable.player().getHealth().getCurrentHp());
        advance(unblockable, 0.3);
        assertEquals("RECOVERY", unblockable.abilities().getSkillStatus());
    }

    /** Dragon Thrust requires an actual accepted hit before its bounded six-wave follow-up. */
    @Test
    void dragonHitAndMiss() {
        Fixture hit = fixture(PlayerClass.KATANA, UltimateMode.B, List.of(), 1);
        hit.step(0.2, command(AbilitySlot.ULTIMATE));
        assertEquals("FINISHER", hit.abilities().getSkillStatus());
        advance(hit, 1);
        assertEquals(10000 - SkillTuning.THRUST_DAMAGE - SkillTuning.DRAGON_TICKS * 18, hit.health().getCurrentHp());
        int hp = hit.health().getCurrentHp();
        advance(hit, 3);
        assertEquals(hp, hit.health().getCurrentHp());
        Fixture miss = fixture(PlayerClass.KATANA, UltimateMode.B, List.of(), 1);
        miss.target().setHurtbox(new CollisionBox(900, 500, 64, 64));
        miss.step(0.5, command(AbilitySlot.ULTIMATE));
        advance(miss, 2);
        assertEquals(10000, miss.health().getCurrentHp());
        assertEquals("NONE", miss.abilities().getSkillStatus());
    }

    /**
     * Counter and offensive movement cannot cross a solid wall, nor damage its far side.
     * @param dragon whether to exercise O thrust instead of K counter dash
     */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void offensiveMovementRespectsWalls(boolean dragon) {
        Fixture f = fixture(PlayerClass.KATANA, dragon ? UltimateMode.B : UltimateMode.A,
                List.of(new CollisionBox(150, 0, 20, 1000)), 1);
        f.step(0, command(dragon ? AbilitySlot.ULTIMATE : AbilitySlot.SPECIAL));
        if (!dragon) {
            f.abilities().update(0, PlayerCommand.NONE);
            hit(f, true, ElementType.NONE);
            f.abilities().afterCombat();
        }
        advance(f, 2);
        assertTrue(f.player().getBounds().right() <= 150);
        assertEquals(10000, f.health().getCurrentHp());
    }

    /**
     * O selection is always Hub-only and persists per class through rebind/death/reset.
     * @param type class whose mode is selected
     */
    @ParameterizedTest
    @EnumSource(PlayerClass.class)
    void modeSelectionPersistsAndRejectsBattle(PlayerClass type) {
        Fixture f = fixture(type, UltimateMode.B, List.of(), 1);
        assertEquals(UltimateMode.B, f.player().getUltimateMode());
        f.abilities().selectClass(PlayerClasses.get(type == PlayerClass.MAGE ? PlayerClass.ARCHER : PlayerClass.MAGE));
        assertEquals(UltimateMode.A, f.player().getUltimateMode());
        f.abilities().selectClass(PlayerClasses.get(type));
        assertEquals(UltimateMode.B, f.player().getUltimateMode());
        GameMap battle = map(MapType.BATTLE, List.of());
        f.player().enterArea(battle, 100, 100);
        f.combat().clear();
        f.combat().register(f.source());
        f.abilities().bindAfterTransition(f.source());
        assertFalse(f.abilities().selectUltimateMode(UltimateMode.A));
        f.player().takeDamage(100);
        f.step(0, command(AbilitySlot.ULTIMATE));
        f.player().enterArea(map(MapType.HUB, List.of()), 100, 100);
        f.player().getHealth().restoreFull();
        f.combat().clear();
        f.combat().register(f.source());
        f.abilities().bindAfterTransition(f.source());
        f.abilities().reset();
        assertEquals(UltimateMode.B, f.player().getUltimateMode());
        assertTrue(f.abilities().selectUltimateMode(UltimateMode.A));
    }

    /**
     * Builds a real controller and canonical hostile health receiver on placeholder geometry.
     * @param type initial class
     * @param mode initial Hub-selected mode
     * @param walls static blockers
     * @param seed random seed
     * @return independent shared-pipeline fixture
     */
    private Fixture fixture(PlayerClass type, UltimateMode mode, List<CollisionBox> walls, long seed) {
        Player player = new Player(new CollisionBox(100, 100, 20, 20), 100, map(MapType.HUB, walls),
                100, PlayerClasses.get(type));
        CombatSystem combat = new CombatSystem();
        Combatant source = new Combatant(player, Faction.PLAYER, player.getBounds());
        Health health = new Health(10000);
        Combatant target = new Combatant(health, Faction.ENEMY, new CollisionBox(200, 100, 64, 64));
        combat.register(source);
        combat.register(target);
        PlayerAbilities abilities = new PlayerAbilities(player, source, combat, player.getClassDefinition().abilities(),
                true, new Random(seed));
        if (mode == UltimateMode.B) {
            assertTrue(abilities.selectUltimateMode(mode));
        }
        return new Fixture(player, combat, source, target, health, abilities);
    }

    /**
     * Creates asset-free geometry large enough for all skill travel/field tests.
     * @param type Hub or Battle classification
     * @param walls static rectangles
     * @return immutable world
     */
    private GameMap map(MapType type, List<CollisionBox> walls) {
        return new GameMap(type.name(), type.name(), type, 2000, 1000, walls,
                Map.of("entry", new SpawnPoint(100, 100)), "entry");
    }

    /**
     * Sends one stationary slot request.
     * @param slot requested slot
     * @return shared command
     */
    private PlayerCommand command(AbilitySlot slot) {
        return new PlayerCommand(0, 0, slot == AbilitySlot.LIGHT, slot == AbilitySlot.SPECIAL,
                slot == AbilitySlot.DASH, slot == AbilitySlot.ULTIMATE);
    }

    /**
     * Advances frames at the normal small simulation granularity.
     * @param f fixture
     * @param seconds elapsed time
     */
    private void advance(Fixture f, double seconds) {
        for (int i = 0; i < Math.ceil(seconds / 0.05); i++) {
            f.step(0.05, PlayerCommand.NONE);
        }
    }

    /**
     * Submits a confirmed incoming effect without duplicating damage calculation.
     * @param f fixture
     * @param blockable whether interception is allowed
     * @param element incoming tag
     */
    private void hit(Fixture f, boolean blockable, ElementType element) {
        CollisionBox box = f.source().getHurtbox();
        f.combat().tryAttack(f.target(), new AttackSpec("incoming", 10, 1, 0, element, blockable), box, box).orElseThrow();
        f.combat().update(0);
    }

    /**
     * Groups the real shared pipeline models and enforces production settlement order.
     * @param player persistent player
     * @param combat shared pipeline
     * @param source canonical player adapter
     * @param target generic hostile receiver
     * @param health hostile health
     * @param abilities controller under test
     * @author Daoyan Zhu (u7782042)
     */
    private record Fixture(Player player, CombatSystem combat, Combatant source, Combatant target,
            Health health, PlayerAbilities abilities) {
        /**
         * Executes input, one combat settlement and deferred response/timing processing.
         * @param delta elapsed seconds
         * @param command input request
         */
        void step(double delta, PlayerCommand command) {
            abilities.update(delta, command);
            combat.update(delta);
            abilities.afterCombat();
        }
    }
}
