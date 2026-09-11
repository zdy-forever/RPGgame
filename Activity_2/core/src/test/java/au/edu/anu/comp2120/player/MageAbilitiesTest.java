package au.edu.anu.comp2120.player;

import static org.junit.jupiter.api.Assertions.*;

import au.edu.anu.comp2120.combat.Attack;
import au.edu.anu.comp2120.combat.CombatSystem;
import au.edu.anu.comp2120.combat.Combatant;
import au.edu.anu.comp2120.combat.Faction;
import au.edu.anu.comp2120.combat.Projectile;
import au.edu.anu.comp2120.combat.ProjectileSpec;
import au.edu.anu.comp2120.enemy.Enemy;
import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.GameMap;
import au.edu.anu.comp2120.world.TestMaps;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Verifies actual Mage definitions through shared player, ability, projectile and health systems.
 * Tests require no graphics context or assets and do not substitute a separate spell executor.
 * @author Daoyan Zhu (u7782042)
 */
final class MageAbilitiesTest {
    /** Tolerance for normalized flight and fractional timers. */
    private static final double EPSILON = 1e-8;
    /** Large arena with one thin wall; default bolt expires before reaching the wall. */
    private final GameMap map = TestMaps.rectangle(2000, 1500,
            List.of(new CollisionBox(1000, 0, 1, 1000)));
    /** Existing concrete player, explicitly configured as Mage. */
    private final Player player = new Player(new CollisionBox(200, 200, 20, 20), 100, map, 100,
            PlayerClasses.get(PlayerClass.MAGE));
    /** Existing shared damage pipeline. */
    private final CombatSystem combat = new CombatSystem();
    /** Inset hurtbox proves spells do not depend on the visual body dimensions. */
    private final Combatant source = new Combatant(player, Faction.PLAYER, new CollisionBox(204, 204, 12, 12));
    /** Immutable built-in tuning under test. */
    private final AbilityLoadout loadout = player.getClassDefinition().abilities();
    /** Shared controller used without any Mage-specific player subclass. */
    private final PlayerAbilities abilities;

    /** Registers the real player adapter and initializes its shared controller. */
    MageAbilitiesTest() {
        combat.register(source);
        abilities = new PlayerAbilities(player, source, combat);
    }

    /** J creates a small travelling bolt; only the central pipeline damages the first enemy. */
    @Test
    void lightTravelsThenDealsDamageOnceAndIsRemoved() {
        Enemy enemy = target(new CollisionBox(300, 200, 20, 20));
        Enemy behind = target(new CollisionBox(350, 200, 20, 20));
        assertEquals(Optional.of(AbilitySlot.LIGHT), tick(0, command(AbilitySlot.LIGHT)));
        Attack bolt = combat.getActiveAttacks().get(0);
        assertEquals("mage-light", bolt.getSpecification().type());
        assertSame(source, bolt.getSource());
        assertEquals(Projectile.class, bolt.getProjectile().getClass());
        assertEquals(new CollisionBox(205, 205, 10, 10), bolt.getHitbox());
        assertEquals(1000, enemy.getHealth().getCurrentHp());
        tick(0.1, PlayerCommand.NONE);
        assertEquals(245, bolt.getHitbox().x());
        assertEquals(1000, enemy.getHealth().getCurrentHp());
        tick(0.2, PlayerCommand.NONE);
        assertEquals(975, enemy.getHealth().getCurrentHp());
        assertEquals(1000, behind.getHealth().getCurrentHp());
        assertEquals(100, player.getHealth().getCurrentHp());
        assertFalse(bolt.isActive());
        assertTrue(combat.getActiveAttacks().isEmpty());
        tick(2, PlayerCommand.NONE);
        assertEquals(975, enemy.getHealth().getCurrentHp());
    }

    /**
     * All eight launch directions use normalized velocity and ignore subsequent turning.
     * @param horizontal horizontal facing intent
     * @param vertical vertical facing intent
     */
    @ParameterizedTest
    @CsvSource({"1,0", "-1,0", "0,1", "0,-1", "1,1", "1,-1", "-1,1", "-1,-1"})
    void boltCapturesEightDirectionFacing(int horizontal, int vertical) {
        tick(0, new PlayerCommand(horizontal, vertical, true, false, false, false));
        Projectile bolt = combat.getActiveAttacks().get(0).getProjectile();
        tick(0.1, new PlayerCommand(-horizontal, -vertical, false, false, false, false));
        double length = Math.hypot(horizontal, vertical);
        assertEquals(horizontal / length, bolt.getDirectionX(), EPSILON);
        assertEquals(vertical / length, bolt.getDirectionY(), EPSILON);
        assertEquals(205 + horizontal / length * 40, bolt.getBounds().x(), EPSILON);
        assertEquals(205 + vertical / length * 40, bolt.getBounds().y(), EPSILON);
        assertEquals(40, Math.hypot(bolt.getBounds().x() - 205, bolt.getBounds().y() - 205), EPSILON);
    }

    /** Large frame deltas stop flight at range and cannot damage a target just beyond it. */
    @Test
    void rangeCapsTravelAndExpiresBolt() {
        Enemy beyond = target(new CollisionBox(576, 200, 20, 20));
        tick(0, command(AbilitySlot.LIGHT));
        Attack bolt = combat.getActiveAttacks().get(0);
        assertEquals(360, bolt.getProjectile().getRemainingRange());
        assertEquals(1.5, bolt.getRemainingSeconds());
        tick(10, PlayerCommand.NONE);
        assertEquals(565, bolt.getHitbox().x());
        assertEquals(0, bolt.getProjectile().getRemainingRange());
        assertFalse(bolt.isActive());
        assertEquals(1000, beyond.getHealth().getCurrentHp());
        assertTrue(combat.getActiveAttacks().isEmpty());
    }

    /** A shorter configured lifetime overrides range without changing any spell execution code. */
    @Test
    void configurableLifetimeCanExpireBeforeRange() {
        AbilityLoadout.AreaAttack light = loadout.light();
        ProjectileSpec flight = light.projectile();
        AbilityLoadout.AreaAttack shortLived = new AbilityLoadout.AreaAttack(light.attack(), light.range(),
                light.shape(), new ProjectileSpec(flight.speed(), flight.size(), 0.1, 1, 0));
        assertTrue(abilities.selectClass(new ClassDefinition(PlayerClass.MAGE,
                new AbilityLoadout(shortLived, loadout.special(), loadout.dash(), loadout.ultimate()))));
        Enemy beyond = target(new CollisionBox(260, 200, 20, 20));
        tick(0, command(AbilitySlot.LIGHT));
        Attack bolt = combat.getActiveAttacks().get(0);
        tick(1, PlayerCommand.NONE);
        assertEquals(245, bolt.getHitbox().x());
        assertEquals(320, bolt.getProjectile().getRemainingRange());
        assertEquals(0, bolt.getRemainingSeconds());
        assertEquals(1000, beyond.getHealth().getCurrentHp());
        assertTrue(combat.getActiveAttacks().isEmpty());
    }

    /** Swept wall contact stops a fast spell before a hostile target, even in one large step. */
    @Test
    void boltCannotPassThroughThinWall() {
        player.enterArea(map, 900, 200);
        Enemy behindWall = target(new CollisionBox(1040, 200, 20, 20));
        tick(0, command(AbilitySlot.LIGHT));
        Attack bolt = combat.getActiveAttacks().get(0);
        tick(1, PlayerCommand.NONE);
        assertEquals(990, bolt.getHitbox().x());
        assertTrue(map.isWalkable(bolt.getHitbox()));
        assertEquals(1000, behindWall.getHealth().getCurrentHp());
        assertFalse(bolt.isActive());
        assertTrue(combat.getActiveAttacks().isEmpty());
    }

    /** Self, friendly, dead and unregistered targets are ignored; a live hostile can be killed. */
    @Test
    void invalidTargetsDoNotAbsorbBoltAndLethalDamageUsesHealth() {
        Enemy friendly = new Enemy();
        Enemy dead = new Enemy();
        dead.takeDamage(100);
        Enemy removed = new Enemy();
        Enemy live = new Enemy(20);
        combat.register(new Combatant(friendly, Faction.PLAYER, new CollisionBox(230, 200, 10, 20)));
        combat.register(new Combatant(dead, Faction.ENEMY, new CollisionBox(250, 200, 10, 20)));
        Combatant stale = new Combatant(removed, Faction.ENEMY, new CollisionBox(270, 200, 10, 20));
        combat.register(stale);
        combat.unregister(stale);
        combat.register(new Combatant(live, Faction.ENEMY, new CollisionBox(300, 200, 20, 20)));
        tick(0.3, command(AbilitySlot.LIGHT));
        assertEquals(100, player.getHealth().getCurrentHp());
        assertEquals(100, friendly.getHealth().getCurrentHp());
        assertEquals(0, dead.getHealth().getCurrentHp());
        assertEquals(100, removed.getHealth().getCurrentHp());
        assertEquals(0, live.getHealth().getCurrentHp());
        assertFalse(live.isAlive());
        assertTrue(combat.getActiveAttacks().isEmpty());
    }

    /**
     * Custom direct area slots retain overlap/late-entry semantics; final Mage K/O use selection/casting instead.
     * @param slot area spell slot
     */
    @ParameterizedTest
    @EnumSource(value = AbilitySlot.class, names = {"SPECIAL", "ULTIMATE"})
    void areaSpellsHitEachTargetOnceInsideConfiguredRange(AbilitySlot slot) {
        AbilityLoadout.AreaAttack tuning = new AbilityLoadout.AreaAttack(
                new au.edu.anu.comp2120.combat.AttackSpec("custom-area", 40, 0.3, 2), 120);
        abilities.selectClass(new ClassDefinition(PlayerClass.MAGE,
                new AbilityLoadout(loadout.light(), tuning, loadout.dash(), tuning)));
        Enemy front = target(new CollisionBox(300, 200, 20, 20));
        Enemy behind = target(new CollisionBox(150, 200, 20, 20));
        Enemy touching = target(new CollisionBox(216 + tuning.range(), 200, 20, 20));
        Enemy late = new Enemy(1000);
        Combatant lateActor = new Combatant(late, Faction.ENEMY, new CollisionBox(700, 200, 20, 20));
        combat.register(lateActor);
        abilities.update(0, command(slot));
        assertEquals(1000, front.getHealth().getCurrentHp());
        Attack pulse = combat.getActiveAttacks().get(0);
        CollisionBox original = pulse.getHitbox();
        assertNull(pulse.getProjectile());
        assertEquals("custom-area",
                pulse.getSpecification().type());
        assertEquals(AttackShape.AREA.create(source.getHurtbox(), tuning.range(), 1, 0), original);
        combat.update(0);
        assertEquals(1000 - tuning.attack().damage(), front.getHealth().getCurrentHp());
        assertEquals(1000 - tuning.attack().damage(), behind.getHealth().getCurrentHp());
        assertEquals(1000, touching.getHealth().getCurrentHp());
        lateActor.setHurtbox(new CollisionBox(250, 200, 20, 20));
        tick(0.1, new PlayerCommand(0, 1, false, false, false, false));
        assertEquals(original, pulse.getHitbox());
        assertEquals(1000 - tuning.attack().damage(), late.getHealth().getCurrentHp());
        assertEquals(1000 - tuning.attack().damage(), front.getHealth().getCurrentHp());
        tick(tuning.attack().activeSeconds(), PlayerCommand.NONE);
        assertFalse(pulse.isActive());
        assertTrue(combat.getActiveAttacks().isEmpty());
        assertEquals(AbilityState.COOLDOWN, abilities.getState(slot));
    }

    /**
     * Spam cannot bypass slot timing; every Mage slot recovers without resetting the controller.
     * @param slot tested ability slot
     */
    @ParameterizedTest
    @EnumSource(AbilitySlot.class)
    void cooldownRejectsSpamAndRecovers(AbilitySlot slot) {
        target(new CollisionBox(500, 200, 20, 20));
        assertEquals(Optional.of(slot), tick(0, command(slot)));
        double cooldown = abilities.getCooldownRemaining(slot);
        assertEquals(slot == AbilitySlot.SPECIAL ? AbilityState.COOLDOWN : AbilityState.ACTIVE, abilities.getState(slot));
        for (int i = 0; i < 30; i++) {
            assertTrue(tick(0, command(slot)).isEmpty());
        }
        assertEquals(cooldown, abilities.getCooldownRemaining(slot));
        tick(cooldown, PlayerCommand.NONE);
        if (slot == AbilitySlot.ULTIMATE) {
            tick(3, PlayerCommand.NONE);
        }
        assertEquals(AbilityState.READY, abilities.getState(slot));
        assertEquals(Optional.of(slot), tick(0, command(slot)));
    }

    /** Projectile flight outlives its launch window but does not lock J past its independent cooldown. */
    @Test
    void flightAndCooldownAreIndependentWithoutAllowingMidFlightClassSwitch() {
        tick(0, command(AbilitySlot.LIGHT));
        Attack first = combat.getActiveAttacks().get(0);
        tick(0.25, PlayerCommand.NONE);
        assertEquals(AbilityState.COOLDOWN, abilities.getState(AbilitySlot.LIGHT));
        tick(0.25, PlayerCommand.NONE);
        assertTrue(first.isActive());
        assertEquals(AbilityState.READY, abilities.getState(AbilitySlot.LIGHT));
        assertFalse(abilities.selectClass(PlayerClasses.get(PlayerClass.ARCHER)));
        assertEquals(Optional.of(AbilitySlot.LIGHT), tick(0, command(AbilitySlot.LIGHT)));
        assertEquals(2, combat.getActiveAttacks().size());
        assertNotEquals(first.getId(), combat.getActiveAttacks().get(1).getId());
        tick(1, PlayerCommand.NONE);
        assertTrue(combat.getActiveAttacks().isEmpty());
        assertTrue(abilities.selectClass(PlayerClasses.get(PlayerClass.ARCHER)));
    }

    /** Spells can overlap across slots without sharing cooldowns or accumulating expired objects. */
    @Test
    void spellsShareOneLifecycleOwnerAndResetCancelsEverything() {
        Enemy receiver = target(new CollisionBox(600, 200, 20, 20));
        for (int i = 0; i < 100; i++) {
            receiver.getHealth().restoreFull();
            assertEquals(Optional.of(AbilitySlot.SPECIAL), tick(0.1, command(AbilitySlot.SPECIAL)));
            assertTrue(combat.getActiveAttacks().isEmpty());
            assertEquals(Optional.of(AbilitySlot.LIGHT), tick(0.6, command(AbilitySlot.LIGHT)));
            assertEquals(Optional.of(AbilitySlot.ULTIMATE), tick(1.5, command(AbilitySlot.ULTIMATE)));
            tick(10, PlayerCommand.NONE);
            assertTrue(combat.getActiveAttacks().isEmpty());
        }
        tick(0, command(AbilitySlot.LIGHT));
        List<Attack> retained = combat.getActiveAttacks();
        player.takeDamage(30);
        abilities.reset();
        assertTrue(retained.stream().noneMatch(Attack::isActive));
        assertTrue(combat.getActiveAttacks().isEmpty());
        assertEquals(70, player.getHealth().getCurrentHp());
        assertEquals(PlayerClass.MAGE, player.getPlayerClass());
        for (AbilitySlot slot : AbilitySlot.values()) {
            assertEquals(AbilityState.READY, abilities.getState(slot));
        }
    }

    /** L uses the shared 120-unit dash and stops at walls, never becoming a teleport. */
    @Test
    void dashKeepsSharedMovementAndWallRules() {
        tick(0.2, command(AbilitySlot.DASH));
        assertEquals(320, player.getX());
        assertEquals(0.8, abilities.getCooldownRemaining(AbilitySlot.DASH), EPSILON);
        abilities.reset();
        player.enterArea(map, 950, 200);
        tick(0.2, command(AbilitySlot.DASH));
        assertEquals(980, player.getX());
        assertTrue(map.isWalkable(player.getBounds()));
        assertEquals(984, source.getHurtbox().x());
    }

    /**
     * The actual Mage dash clamps each world edge through normal movement collision.
     * @param horizontal horizontal intent
     * @param vertical vertical intent
     * @param x initial horizontal coordinate
     * @param y initial vertical coordinate
     * @param expectedX clamped horizontal coordinate
     * @param expectedY clamped vertical coordinate
     */
    @ParameterizedTest
    @CsvSource({"-1,0,10,200,0,200", "1,0,1970,200,1980,200",
            "0,-1,200,10,200,0", "0,1,200,1470,200,1480"})
    void dashClampsMapBounds(int horizontal, int vertical, double x, double y,
            double expectedX, double expectedY) {
        player.enterArea(map, x, y);
        tick(0.2, new PlayerCommand(horizontal, vertical, false, false, true, false));
        assertEquals(expectedX, player.getX());
        assertEquals(expectedY, player.getY());
        assertTrue(map.isWalkable(player.getBounds()));
    }

    /**
     * Death cancels pending spells before damage and prevents all new Mage slot requests.
     * @param slot pending ability slot
     */
    @ParameterizedTest
    @EnumSource(AbilitySlot.class)
    void deathStopsSpellsAndSharedDash(AbilitySlot slot) {
        Enemy enemy = target(new CollisionBox(240, 200, 20, 20));
        abilities.update(0, command(slot));
        List<Attack> pending = combat.getActiveAttacks();
        CollisionBox before = player.getBounds();
        player.takeDamage(100);
        assertTrue(tick(1, command(slot)).isEmpty());
        assertEquals(AbilityState.UNAVAILABLE, abilities.getState(slot));
        assertEquals(before, player.getBounds());
        assertEquals(1000, enemy.getHealth().getCurrentHp());
        assertTrue(pending.stream().noneMatch(Attack::isActive));
        assertTrue(combat.getActiveAttacks().isEmpty());
    }

    /**
     * Registers an isolated stationary enemy through the existing combat adapter.
     * @param bounds enemy hurtbox
     * @return real enemy health owner
     */
    private Enemy target(CollisionBox bounds) {
        Enemy enemy = new Enemy(1000);
        combat.register(new Combatant(enemy, Faction.ENEMY, bounds));
        return enemy;
    }

    /**
     * Advances the existing ability controller then combat once, as the demo does.
     * @param delta elapsed simulation seconds
     * @param command one frame's intent
     * @return accepted slot or empty
     */
    private Optional<AbilitySlot> tick(double delta, PlayerCommand command) {
        Optional<AbilitySlot> result = abilities.update(delta, command);
        combat.update(delta);
        abilities.afterCombat();
        return result;
    }

    /**
     * Requests one slot without changing the last facing direction.
     * @param slot requested ability
     * @return shared input command
     */
    private PlayerCommand command(AbilitySlot slot) {
        return new PlayerCommand(0, 0, slot == AbilitySlot.LIGHT, slot == AbilitySlot.SPECIAL,
                slot == AbilitySlot.DASH, slot == AbilitySlot.ULTIMATE);
    }
}
