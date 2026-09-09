package au.edu.anu.comp2120.player;

import static org.junit.jupiter.api.Assertions.*;

import au.edu.anu.comp2120.combat.Attack;
import au.edu.anu.comp2120.combat.AttackSpec;
import au.edu.anu.comp2120.combat.CombatSystem;
import au.edu.anu.comp2120.combat.Combatant;
import au.edu.anu.comp2120.combat.Faction;
import au.edu.anu.comp2120.enemy.Enemy;
import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.GameMap;
import au.edu.anu.comp2120.world.TestMaps;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tests real class composition, configured strategies, shared state and safe selection without graphics.
 * @author Daoyan Zhu (u7782042)
 */
final class PlayerClassTest {
    /** Collision map shared as immutable data, never as mutable player state. */
    private final GameMap map = TestMaps.rectangle(800, 600,
            List.of(new CollisionBox(400, 0, 1, 400)));

    /** Creates the class-system test fixture. */
    PlayerClassTest() {
    }

    /** Built-in identities have stable unique IDs and complete independent class definitions. */
    @Test
    void catalogContainsThreeStableIdentities() {
        assertEquals(List.of("archer", "mage", "katana"),
                Arrays.stream(PlayerClass.values()).map(PlayerClass::getId).toList());
        assertEquals(3, Arrays.stream(PlayerClass.values()).map(PlayerClass::getDisplayName).distinct().count());
        assertEquals(PlayerClass.MAGE, PlayerClasses.DEFAULT);
        assertThrows(NullPointerException.class, () -> PlayerClasses.get(null));
        assertThrows(NullPointerException.class, () -> new ClassDefinition(null,
                PlayerClasses.get(PlayerClass.MAGE).abilities()));
        assertThrows(NullPointerException.class, () -> new ClassDefinition(PlayerClass.MAGE, null));
    }

    /**
     * Every class uses the exact same concrete Player, movement, health and collision code.
     * @param type selected built-in class
     */
    @ParameterizedTest
    @EnumSource(PlayerClass.class)
    void initializesSharedPlayerAndPreservesOrdinaryMovement(PlayerClass type) {
        Fixture f = fixture(type);
        assertEquals(Player.class, f.player().getClass());
        assertSame(f.player(), f.source().getEntity());
        assertEquals(type, f.player().getPlayerClass());
        assertEquals(PlayerClasses.get(type), f.player().getClassDefinition());
        f.player().takeDamage(30);
        f.tick(0.1, new PlayerCommand(1, 1, false, false, false, false));
        assertEquals(100 + 10 / Math.sqrt(2), f.player().getX(), 1e-9);
        assertEquals(100 + 10 / Math.sqrt(2), f.player().getY(), 1e-9);
        assertEquals(70, f.player().getHealth().getCurrentHp());
        assertEquals(100, f.player().getHealth().getMaxHp());
        f.player().enterArea(map, 350, 100);
        f.tick(2, new PlayerCommand(1, 0, false, false, false, false));
        assertEquals(380, f.player().getX());
        assertTrue(map.isWalkable(f.player().getBounds()));
    }

    /**
     * Each class's J/K/O tuning and geometry strategy feed the same combat/health pipeline.
     * @param type selected class
     */
    @ParameterizedTest
    @EnumSource(PlayerClass.class)
    void appliesEveryAttackSlotDefinition(PlayerClass type) {
        for (AbilitySlot slot : List.of(AbilitySlot.LIGHT, AbilitySlot.SPECIAL, AbilitySlot.ULTIMATE)) {
            Fixture f = fixture(type);
            Enemy front = new Enemy(1000);
            Enemy behind = new Enemy(1000);
            f.combat().register(new Combatant(front, Faction.ENEMY, new CollisionBox(160, 100, 20, 20)));
            f.combat().register(new Combatant(behind, Faction.ENEMY, new CollisionBox(60, 100, 20, 20)));
            AbilityLoadout loadout = f.player().getClassDefinition().abilities();
            AbilityLoadout.AreaAttack tuning = switch (slot) {
                case LIGHT -> loadout.light();
                case SPECIAL -> loadout.special();
                case ULTIMATE -> loadout.ultimate();
                case DASH -> throw new AssertionError("Not an attack");
            };
            f.tick(0, command(slot));
            Attack attack = f.combat().getActiveAttacks().get(0);
            assertSame(f.source(), attack.getSource());
            assertEquals(tuning.attack().type(), attack.getSpecification().type());
            if (tuning.projectile() != null) {
                assertNotNull(attack.getProjectile());
                assertEquals(tuning.projectile().lifetime(), attack.getRemainingSeconds());
                assertEquals(tuning.projectile().count(), f.combat().getActiveAttacks().size());
                assertEquals(tuning.projectile().size(), attack.getHitbox().width());
                assertEquals(tuning.attack().cooldownSeconds(), f.abilities().getCooldownRemaining(slot));
                assertEquals(1000, front.getHealth().getCurrentHp());
                f.tick(0.2, command(slot));
                assertTrue(front.getHealth().getCurrentHp() < 1000);
                assertEquals(1000, behind.getHealth().getCurrentHp());
                continue;
            }
            assertEquals(tuning.attack().activeSeconds(), attack.getRemainingSeconds());
            assertEquals(tuning.attack().cooldownSeconds(), f.abilities().getCooldownRemaining(slot));
            assertEquals(tuning.shape().create(f.source().getHurtbox(), tuning.range(), 1, 0), attack.getHitbox());
            assertEquals(1000 - tuning.attack().damage(), front.getHealth().getCurrentHp());
            assertEquals(tuning.shape() == AttackShape.AREA ? 1000 - tuning.attack().damage() : 1000,
                    behind.getHealth().getCurrentHp());
            f.tick(0, command(slot));
            assertEquals(1, f.combat().getActiveAttacks().size());
            assertEquals(1000 - tuning.attack().damage(), front.getHealth().getCurrentHp());
        }
    }

    /**
     * Dash differences are data applied by the same collision-constrained controller.
     * @param type selected class
     */
    @ParameterizedTest
    @EnumSource(PlayerClass.class)
    void appliesClassDashTuningWithoutDuplicatingMovement(PlayerClass type) {
        Fixture f = fixture(type);
        AbilityLoadout.Dash tuning = f.player().getClassDefinition().abilities().dash();
        f.tick(tuning.duration(), new PlayerCommand(1, 1, false, false, true, false));
        assertEquals(tuning.speed() * tuning.duration(),
                Math.hypot(f.player().getX() - 100, f.player().getY() - 100), 1e-9);
        assertEquals(tuning.cooldown() - tuning.duration(),
                f.abilities().getCooldownRemaining(AbilitySlot.DASH), 1e-9);
        f.abilities().reset();
        f.player().enterArea(map, 350, 100);
        f.tick(tuning.duration(), new PlayerCommand(1, 0, false, false, true, false));
        assertEquals(380, f.player().getX());
        assertTrue(map.isWalkable(f.player().getBounds()));
        assertEquals(type, f.player().getPlayerClass());
    }

    /** Switching preserves canonical registration, health, position, speed and remaining slot cooldown. */
    @Test
    void switchingCannotHealRepositionOrRefreshCooldown() {
        Fixture f = fixture(PlayerClass.MAGE);
        var health = f.player().getHealth();
        f.player().takeDamage(40);
        f.tick(0.25, command(AbilitySlot.LIGHT));
        CollisionBox before = f.player().getBounds();
        double cooldown = f.abilities().getCooldownRemaining(AbilitySlot.LIGHT);
        List<Combatant> registered = f.combat().getCombatants();
        assertTrue(f.abilities().selectClass(PlayerClasses.get(PlayerClass.ARCHER)));
        assertEquals(PlayerClass.ARCHER, f.player().getPlayerClass());
        assertSame(health, f.player().getHealth());
        assertEquals(60, health.getCurrentHp());
        assertEquals(before, f.player().getBounds());
        assertEquals(100, f.player().getSpeed());
        assertEquals(registered, f.combat().getCombatants());
        assertSame(f.source(), f.combat().getCombatants().get(0));
        assertEquals(cooldown, f.abilities().getCooldownRemaining(AbilitySlot.LIGHT));
        assertTrue(f.abilities().update(0, command(AbilitySlot.LIGHT)).isEmpty());
        f.tick(cooldown, PlayerCommand.NONE);
        f.tick(0, command(AbilitySlot.LIGHT));
        assertEquals("archer-light", f.combat().getActiveAttacks().get(0).getSpecification().type());
    }

    /**
     * All active slot types reject switching, so an old attack/dash cannot borrow new tuning.
     * @param slot active ability slot
     */
    @ParameterizedTest
    @EnumSource(AbilitySlot.class)
    void activeAbilitiesRejectSelection(AbilitySlot slot) {
        Fixture f = fixture(PlayerClass.MAGE);
        f.tick(0, command(slot));
        ClassDefinition before = f.player().getClassDefinition();
        double cooldown = f.abilities().getCooldownRemaining(slot);
        assertFalse(f.abilities().selectClass(PlayerClasses.get(PlayerClass.KATANA)));
        assertSame(before, f.player().getClassDefinition());
        assertEquals(cooldown, f.abilities().getCooldownRemaining(slot));
        assertEquals(AbilityState.ACTIVE, f.abilities().getState(slot));
        f.tick(0.5, PlayerCommand.NONE);
        assertTrue(f.abilities().selectClass(PlayerClasses.get(PlayerClass.KATANA)));
    }

    /** Dead, stale, same-definition and null requests cannot mutate class or revive the player. */
    @Test
    void invalidAndUnavailableSelectionsLeaveStateAlone() {
        Fixture f = fixture(PlayerClass.MAGE);
        assertFalse(f.abilities().selectClass(PlayerClasses.get(PlayerClass.MAGE)));
        assertThrows(NullPointerException.class, () -> f.abilities().selectClass(null));
        f.player().takeDamage(100);
        assertFalse(f.abilities().selectClass(PlayerClasses.get(PlayerClass.ARCHER)));
        assertFalse(f.player().isAlive());
        f.abilities().reset();
        assertEquals(PlayerClass.MAGE, f.player().getPlayerClass());
        f.player().getHealth().restoreFull();
        assertTrue(f.abilities().selectClass(PlayerClasses.get(PlayerClass.ARCHER)));
        f.combat().unregister(f.source());
        assertFalse(f.abilities().selectClass(PlayerClasses.get(PlayerClass.KATANA)));
        assertEquals(PlayerClass.ARCHER, f.player().getPlayerClass());
    }

    /** Pipeline attacks submitted outside the common slots also prevent mid-invocation switching. */
    @Test
    void externalPlayerAttackAlsoGuardsSelection() {
        Fixture f = fixture(PlayerClass.MAGE);
        f.combat().tryAttack(f.source(), new AttackSpec("external", 0, 1, 2), f.source().getHurtbox());
        assertFalse(f.abilities().selectClass(PlayerClasses.get(PlayerClass.ARCHER)));
        f.tick(1, PlayerCommand.NONE);
        assertTrue(f.abilities().selectClass(PlayerClasses.get(PlayerClass.ARCHER)));
        assertEquals(1, f.combat().getCooldownRemaining(f.source()));
    }

    /** Shared immutable class definitions do not share health, facing or cooldown clocks between players. */
    @Test
    void independentPlayersDoNotShareMutableClassState() {
        Fixture first = fixture(PlayerClass.KATANA);
        Fixture second = fixture(PlayerClass.KATANA);
        first.tick(0, command(AbilitySlot.LIGHT));
        first.player().takeDamage(15);
        assertEquals(AbilityState.READY, second.abilities().getState(AbilitySlot.LIGHT));
        assertEquals(100, second.player().getHealth().getCurrentHp());
        assertNotSame(first.player().getHealth(), second.player().getHealth());
        second.abilities().selectClass(PlayerClasses.get(PlayerClass.ARCHER));
        assertEquals(PlayerClass.KATANA, first.player().getPlayerClass());
    }

    /** A custom geometry strategy is actually invoked; there is no controller switch on class identity. */
    @Test
    void customClassStrategyFeedsExistingPipeline() {
        Fixture f = fixture(PlayerClass.MAGE);
        CollisionBox customBox = new CollisionBox(500, 450, 20, 20);
        AttackShape customShape = (body, range, dx, dy) -> customBox;
        AbilityLoadout.AreaAttack customAttack = new AbilityLoadout.AreaAttack(
                new AttackSpec("custom-special", 9, 0.1, 2), 10, customShape);
        AbilityLoadout original = f.player().getClassDefinition().abilities();
        ClassDefinition custom = new ClassDefinition(PlayerClass.MAGE,
                new AbilityLoadout(original.light(), customAttack, original.dash(), original.ultimate()));
        Enemy enemy = new Enemy();
        f.combat().register(new Combatant(enemy, Faction.ENEMY, customBox));
        assertTrue(f.abilities().selectClass(custom));
        f.tick(0, command(AbilitySlot.SPECIAL));
        assertEquals(customBox, f.combat().getActiveAttacks().get(0).getHitbox());
        assertEquals(91, enemy.getHealth().getCurrentHp());
        assertEquals(2, f.abilities().getCooldownRemaining(AbilitySlot.SPECIAL));
    }

    /** A broken custom strategy cannot consume cooldown or enqueue a null attack. */
    @Test
    void rejectsNullStrategyAndNullGeometry() {
        Fixture f = fixture(PlayerClass.MAGE);
        AttackSpec spec = new AttackSpec("custom", 1, 0.1, 1);
        assertThrows(NullPointerException.class, () -> new AbilityLoadout.AreaAttack(spec, 10, null));
        AbilityLoadout.AreaAttack broken = new AbilityLoadout.AreaAttack(spec, 10, (body, range, dx, dy) -> null);
        AbilityLoadout original = f.player().getClassDefinition().abilities();
        f.abilities().selectClass(new ClassDefinition(PlayerClass.MAGE,
                new AbilityLoadout(broken, original.special(), original.dash(), original.ultimate())));
        assertThrows(NullPointerException.class, () -> f.tick(0, command(AbilitySlot.LIGHT)));
        assertEquals(0, f.abilities().getCooldownRemaining(AbilitySlot.LIGHT));
        assertTrue(f.combat().getActiveAttacks().isEmpty());
    }

    /**
     * Creates a shared Player with an explicitly selected class, not a subclass or duplicate damage model.
     * @param type initial class
     * @return independent live controller and combat fixture
     */
    private Fixture fixture(PlayerClass type) {
        Player player = new Player(new CollisionBox(100, 100, 20, 20), 100, map, 100, PlayerClasses.get(type));
        CombatSystem combat = new CombatSystem();
        Combatant source = new Combatant(player, Faction.PLAYER, player.getBounds());
        combat.register(source);
        return new Fixture(player, source, combat, new PlayerAbilities(player, source, combat));
    }

    /**
     * Builds a stationary single-slot request.
     * @param slot requested slot
     * @return common class-independent command
     */
    private PlayerCommand command(AbilitySlot slot) {
        return new PlayerCommand(0, 0, slot == AbilitySlot.LIGHT, slot == AbilitySlot.SPECIAL,
                slot == AbilitySlot.DASH, slot == AbilitySlot.ULTIMATE);
    }

    /**
     * Groups already initialized test models without owning alternate gameplay behaviour.
     * @param player shared player implementation
     * @param source canonical receiver
     * @param combat existing pipeline
     * @param abilities common controller
     * @author Daoyan Zhu (u7782042)
     */
    private record Fixture(Player player, Combatant source, CombatSystem combat, PlayerAbilities abilities) {
        /**
         * Keeps production update ordering in every test.
         * @param delta elapsed seconds
         * @param command frame request
         */
        void tick(double delta, PlayerCommand command) {
            abilities.update(delta, command);
            combat.update(delta);
        }
    }
}
