package au.edu.anu.comp2120.player;

import static org.junit.jupiter.api.Assertions.*;

import au.edu.anu.comp2120.GameSession;
import au.edu.anu.comp2120.combat.Attack;
import au.edu.anu.comp2120.combat.AttackSpec;
import au.edu.anu.comp2120.combat.CombatSystem;
import au.edu.anu.comp2120.combat.Combatant;
import au.edu.anu.comp2120.combat.Faction;
import au.edu.anu.comp2120.combat.Projectile;
import au.edu.anu.comp2120.enemy.Enemy;
import au.edu.anu.comp2120.screen.CombatDemo;
import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.GameMap;
import au.edu.anu.comp2120.world.MapCatalog;
import au.edu.anu.comp2120.world.TestMaps;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Exercises actual Archer slot execution through the existing class, ability and combat systems.
 * @author Daoyan Zhu (u7782042)
 */
final class ArcherAbilitiesTest {
    /** Static arena with a thin wall for Archer dash and arrow checks. */
    private final GameMap map = TestMaps.rectangle(2000, 1500,
            List.of(new CollisionBox(1000, 0, 1, 1000)));
    /** Same concrete Player used by all classes. */
    private final Player player = new Player(new CollisionBox(200, 200, 20, 20), 100, map, 100,
            PlayerClasses.get(PlayerClass.ARCHER));
    /** Canonical shared receiver. */
    private final Combatant source = new Combatant(player, Faction.PLAYER, player.getBounds());
    /** Existing shared pipeline. */
    private final CombatSystem combat = new CombatSystem();
    /** Common ability controller, not a copied Archer player implementation. */
    private final PlayerAbilities abilities;

    /** Registers the actual shared models before testing Archer commands. */
    ArcherAbilitiesTest() {
        combat.register(source);
        abilities = new PlayerAbilities(player, source, combat);
    }

    /** J creates a travelling arrow, not immediate damage over the full range. */
    @Test
    void lightArrowTravelsBeforeDealingDamage() {
        Enemy enemy = target(new CollisionBox(300, 200, 20, 20));
        assertEquals(Optional.of(AbilitySlot.LIGHT), tick(0, command(AbilitySlot.LIGHT)));
        Attack arrow = combat.getActiveAttacks().get(0);
        assertEquals(1000, enemy.getHealth().getCurrentHp());
        assertEquals(new CollisionBox(207, 207, 6, 6), arrow.getHitbox());
        tick(0.1, PlayerCommand.NONE);
        assertEquals(267, arrow.getHitbox().x());
        assertEquals(1000, enemy.getHealth().getCurrentHp());
        tick(0.1, PlayerCommand.NONE);
        assertEquals(980, enemy.getHealth().getCurrentHp());
        assertFalse(arrow.isActive());
        assertTrue(combat.getActiveAttacks().isEmpty());
        assertEquals(100, player.getHealth().getCurrentHp());
    }

    /** Last WASD intent determines launch direction; turning later cannot steer a launched arrow. */
    @Test
    void directionIsCapturedAndDoesNotTrackLaterInput() {
        tick(0, new PlayerCommand(0, 1, false, false, false, false));
        tick(0, command(AbilitySlot.LIGHT));
        Projectile arrow = combat.getActiveAttacks().get(0).getProjectile();
        tick(0.1, new PlayerCommand(-1, 0, false, false, false, false));
        assertEquals(0, arrow.getDirectionX());
        assertEquals(1, arrow.getDirectionY());
        assertEquals(207, arrow.getBounds().x());
        assertEquals(267, arrow.getBounds().y());
        assertEquals(190, player.getX());
    }

    /** Launch active time does not incorrectly lock J until the first arrow expires. */
    @Test
    void lightCooldownAllowsMultipleIndependentArrowsInFlight() {
        tick(0, command(AbilitySlot.LIGHT));
        Attack first = combat.getActiveAttacks().get(0);
        assertEquals(AbilityState.ACTIVE, abilities.getState(AbilitySlot.LIGHT));
        for (int i = 0; i < 20; i++) {
            assertTrue(tick(0, command(AbilitySlot.LIGHT)).isEmpty());
        }
        tick(0.15, PlayerCommand.NONE);
        assertEquals(AbilityState.COOLDOWN, abilities.getState(AbilitySlot.LIGHT));
        tick(0.31, PlayerCommand.NONE);
        assertTrue(first.isActive());
        assertEquals(AbilityState.READY, abilities.getState(AbilitySlot.LIGHT));
        assertEquals(Optional.of(AbilitySlot.LIGHT), tick(0, command(AbilitySlot.LIGHT)));
        assertEquals(2, combat.getActiveAttacks().size());
        assertNotEquals(first.getId(), combat.getActiveAttacks().get(1).getId());
    }

    /** K is a simultaneous fan with a longer interval than the single J arrow. */
    @Test
    void specialCreatesAtomicFanWithLongerCooldown() {
        Enemy enemy = target(new CollisionBox(300, 200, 20, 20));
        tick(0, command(AbilitySlot.SPECIAL));
        Attack arrow = combat.getActiveAttacks().get(2);
        assertEquals(5, combat.getActiveAttacks().size());
        assertEquals(8, arrow.getHitbox().width());
        assertEquals(640, arrow.getProjectile().getRemainingRange());
        assertEquals(2.5, abilities.getCooldownRemaining(AbilitySlot.SPECIAL));
        tick(0.05, PlayerCommand.NONE);
        assertEquals(246, arrow.getHitbox().x());
        tick(0.1, PlayerCommand.NONE);
        assertEquals(980, enemy.getHealth().getCurrentHp());
        assertFalse(arrow.isActive());
        assertTrue(tick(0, command(AbilitySlot.SPECIAL)).isEmpty());
        assertEquals(Optional.of(AbilitySlot.LIGHT), tick(0, command(AbilitySlot.LIGHT)));
    }

    /** K emits a symmetric five-arrow fan, and every arrow can hit a different enemy once. */
    @Test
    void heavyFansFiveArrowsThroughTheSamePipeline() {
        List<Enemy> targets = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            double angle = Math.toRadians(-20 + i * 10);
            targets.add(target(new CollisionBox(210 + Math.cos(angle) * 200 - 5,
                    210 + Math.sin(angle) * 200 - 5, 10, 10)));
        }
        tick(0, command(AbilitySlot.SPECIAL));
        List<Attack> volley = combat.getActiveAttacks();
        assertEquals(5, volley.size());
        for (int i = 0; i < volley.size(); i++) {
            double angle = Math.toRadians(-20 + i * 10);
            assertEquals(Math.cos(angle), volley.get(i).getProjectile().getDirectionX(), 1e-9);
            assertEquals(Math.sin(angle), volley.get(i).getProjectile().getDirectionY(), 1e-9);
        }
        assertEquals(2.5, abilities.getCooldownRemaining(AbilitySlot.SPECIAL));
        assertTrue(tick(0, command(AbilitySlot.SPECIAL)).isEmpty());
        tick(0.5, PlayerCommand.NONE);
        for (Enemy enemy : targets) {
            assertEquals(980, enemy.getHealth().getCurrentHp());
        }
        assertTrue(volley.stream().noneMatch(Attack::isActive));
        assertTrue(combat.getActiveAttacks().isEmpty());
    }

    /** Separate fan arrows may each hit one broad target, but no arrow repeats its own damage. */
    @Test
    void heavyHasExplicitPerArrowDamage() {
        Enemy broad = target(new CollisionBox(300, 100, 20, 220));
        tick(0, command(AbilitySlot.SPECIAL));
        tick(0.5, PlayerCommand.NONE);
        assertEquals(900, broad.getHealth().getCurrentHp());
        tick(0.5, PlayerCommand.NONE);
        assertEquals(900, broad.getHealth().getCurrentHp());
    }

    /** L still uses the existing Archer dash values and shared swept movement. */
    @Test
    void archerDashRemainsSharedAndCannotCrossWall() {
        player.enterArea(map, 950, 200);
        tick(0.2, new PlayerCommand(1, 0, true, true, true, true));
        assertEquals(980, player.getX());
        assertTrue(map.isWalkable(player.getBounds()));
        assertTrue(combat.getActiveAttacks().isEmpty());
        assertEquals(1, abilities.getCooldownRemaining(AbilitySlot.DASH), 1e-9);
    }

    /**
     * Dead players cannot use any Archer slot; reset keeps the class and restores slot readiness.
     * @param slot tested common slot
     */
    @ParameterizedTest
    @EnumSource(AbilitySlot.class)
    void deathAndRecoveryUseExistingRules(AbilitySlot slot) {
        target(new CollisionBox(300, 200, 20, 20));
        player.takeDamage(100);
        assertTrue(tick(0, command(slot)).isEmpty());
        assertTrue(combat.getActiveAttacks().isEmpty());
        abilities.reset();
        assertFalse(player.isAlive());
        player.getHealth().restoreFull();
        assertEquals(PlayerClass.ARCHER, player.getPlayerClass());
        assertEquals(Optional.of(slot), tick(0, command(slot)));
    }

    /** Active volleys prevent class switching even if their short launch window has already ended. */
    @Test
    void classChangeCannotCarryFlyingArrowsToAnotherClass() {
        tick(0, command(AbilitySlot.SPECIAL));
        tick(0.41, PlayerCommand.NONE);
        assertEquals(AbilityState.COOLDOWN, abilities.getState(AbilitySlot.SPECIAL));
        assertFalse(abilities.selectClass(PlayerClasses.get(PlayerClass.MAGE)));
        tick(2, PlayerCommand.NONE);
        double cooldown = abilities.getCooldownRemaining(AbilitySlot.SPECIAL);
        assertTrue(abilities.selectClass(PlayerClasses.get(PlayerClass.MAGE)));
        assertEquals(cooldown, abilities.getCooldownRemaining(AbilitySlot.SPECIAL));
    }

    /** Actual session transitions and R reset cancel all arrows, including non-leading volley members. */
    @Test
    void travelAndResetClearEntireVolley() {
        GameSession session = new GameSession(MapCatalog.createMaps(), MapCatalog.HUB, 28, 28, 180);
        CombatDemo demo = new CombatDemo(session);
        demo.update(0, PlayerCommand.NONE, false, PlayerClass.ARCHER);
        demo.update(0, command(AbilitySlot.SPECIAL), false);
        List<Attack> old = demo.getActiveAttacks();
        assertEquals(5, old.size());
        session.switchMap(MapCatalog.TRAINING);
        demo.update(0, PlayerCommand.NONE, false);
        assertTrue(old.stream().noneMatch(Attack::isActive));
        assertTrue(demo.getActiveAttacks().isEmpty());
        assertEquals(2.5, demo.getAbilities().getCooldownRemaining(AbilitySlot.SPECIAL));
        demo.update(0, PlayerCommand.NONE, true);
        demo.update(0, command(AbilitySlot.SPECIAL), false);
        List<Attack> next = demo.getActiveAttacks();
        assertEquals(5, next.size());
        demo.update(0, PlayerCommand.NONE, true);
        assertTrue(next.stream().noneMatch(Attack::isActive));
        assertEquals(0, demo.getAbilities().getCooldownRemaining(AbilitySlot.SPECIAL));
    }

    /** Rejected launches at capacity do not start a slot's cooldown or launch window. */
    @Test
    void capacityRejectionDoesNotSpendAbilityCooldown() {
        AbilityLoadout.AreaAttack light = player.getClassDefinition().abilities().light();
        for (int i = 0; i < CombatSystem.MAX_PROJECTILES; i++) {
            combat.tryProjectiles(source, new AttackSpec("fill", 0, 0.1, 0), light.projectile(),
                    light.range(), 1, 0, map);
        }
        assertTrue(tick(0, command(AbilitySlot.SPECIAL)).isEmpty());
        assertEquals(0, abilities.getCooldownRemaining(AbilitySlot.SPECIAL));
        assertEquals(AbilityState.READY, abilities.getState(AbilitySlot.SPECIAL));
        assertEquals(CombatSystem.MAX_PROJECTILES, combat.getActiveAttacks().size());
    }

    /**
     * Advances the production ability/combat order.
     * @param delta elapsed seconds
     * @param command shared command
     * @return triggered slot, if any
     */
    private Optional<AbilitySlot> tick(double delta, PlayerCommand command) {
        Optional<AbilitySlot> result = abilities.update(delta, command);
        combat.update(delta);
        abilities.afterCombat();
        return result;
    }

    /**
     * Requests a single common slot while stationary.
     * @param slot requested slot
     * @return common command
     */
    private PlayerCommand command(AbilitySlot slot) {
        return new PlayerCommand(0, 0, slot == AbilitySlot.LIGHT, slot == AbilitySlot.SPECIAL,
                slot == AbilitySlot.DASH, slot == AbilitySlot.ULTIMATE);
    }

    /**
     * Registers a durable enemy for damage assertions.
     * @param bounds independently supplied hurtbox
     * @return enemy with shared Health
     */
    private Enemy target(CollisionBox bounds) {
        Enemy enemy = new Enemy(1000);
        combat.register(new Combatant(enemy, Faction.ENEMY, bounds));
        return enemy;
    }
}
