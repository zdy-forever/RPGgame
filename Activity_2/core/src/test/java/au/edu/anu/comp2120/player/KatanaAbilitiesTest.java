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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Exercises real Katana slashes, timing and collision through the shared ability/combat pipeline.
 * Uses independent hurtboxes and no graphics context, weapon assets or separate melee executor.
 * @author Daoyan Zhu (u7782042)
 */
final class KatanaAbilitiesTest {
    /** Keeps reusable direct-geometry regressions independent of now-defensive K/O gameplay. */
    private void configureDirectGeometry() {
        AbilityLoadout slots = player.getClassDefinition().abilities();
        abilities.selectClass(new ClassDefinition(PlayerClass.KATANA, new AbilityLoadout(slots.light(),
                new AbilityLoadout.AreaAttack(new AttackSpec("custom-heavy", 45, 0.25, 2), 80, AttackShape.FORWARD),
                slots.dash(), new AbilityLoadout.AreaAttack(new AttackSpec("custom-area", 85, 0.35, 7), 110))));
    }

    /** Submits one nearby hostile hit for the existing generic defensive boundary. */
    private void incoming() {
        Combatant attacker = combat.getCombatants().stream().filter(a -> a.getFaction() == Faction.ENEMY).findFirst().orElseThrow();
        combat.tryAttack(attacker, new AttackSpec("incoming", 10, 0.1, 0), source.getHurtbox(), source.getHurtbox()).orElseThrow();
    }

    /** Floating-point tolerance for diagonal geometry and countdowns. */
    private static final double EPSILON = 1e-8;
    /** Collision arena with a thin wall to test the existing swept dash. */
    private final GameMap map = TestMaps.rectangle(800, 600,
            List.of(new CollisionBox(400, 0, 1, 400)));
    /** Same concrete Player used by Archer and Mage. */
    private final Player player = new Player(new CollisionBox(200, 200, 20, 20), 100, map, 100,
            PlayerClasses.get(PlayerClass.KATANA));
    /** Inset non-square hurtbox, deliberately different from the rendered body. */
    private final Combatant source = new Combatant(player, Faction.PLAYER, new CollisionBox(204, 206, 12, 8));
    /** One shared arena owns damage and attack lifetimes. */
    private final CombatSystem combat = new CombatSystem();
    /** Existing class-configured ability controller. */
    private final PlayerAbilities abilities;

    /** Registers the canonical source and initializes the standard controller. */
    KatanaAbilitiesTest() {
        combat.register(source);
        abilities = new PlayerAbilities(player, source, combat);
    }

    /**
     * Light slashes use all eight normalized facing directions and reject targets behind; K is now defensive.
     * @param horizontal horizontal facing intent
     * @param vertical vertical facing intent
     */
    @ParameterizedTest
    @CsvSource({"1,0", "-1,0", "0,1", "0,-1", "1,1", "1,-1", "-1,1", "-1,-1"})
    void directionalSlashesUseHurtboxAndNormalizedFacing(int horizontal, int vertical) {
        double length = Math.hypot(horizontal, vertical);
        double dx = horizontal / length;
        double dy = vertical / length;
        Enemy front = target(new CollisionBox(208 + dx * 30, 208 + dy * 30, 4, 4));
        Enemy behind = target(new CollisionBox(208 - dx * 30, 208 - dy * 30, 4, 4));
        for (AbilitySlot slot : List.of(AbilitySlot.LIGHT)) {
            front.getHealth().restoreFull();
            abilities.reset();
            tick(0, new PlayerCommand(horizontal, vertical, false, false, false, false));
            assertEquals(Optional.of(slot), tick(0, command(slot)));
            Attack slash = combat.getActiveAttacks().get(0);
            double range = 48;
            int damage = 30;
            CollisionBox box = slash.getHitbox();
            assertEquals(204 + Math.min(0, dx * range), box.x(), EPSILON);
            assertEquals(206 + Math.min(0, dy * range), box.y(), EPSILON);
            assertEquals(12 + Math.abs(dx * range), box.width(), EPSILON);
            assertEquals(8 + Math.abs(dy * range), box.height(), EPSILON);
            assertEquals(range, Math.hypot(box.width() - 12, box.height() - 8), EPSILON);
            assertNull(slash.getProjectile());
            assertSame(source, slash.getSource());
            assertEquals(1000 - damage, front.getHealth().getCurrentHp());
            assertEquals(1000, behind.getHealth().getCurrentHp());
            assertEquals(100, player.getHealth().getCurrentHp());
        }
    }

    /**
     * Custom direct melee geometry damages interior overlap but not exact edge contact or out-of-range targets.
     * @param slot damage-dealing slot
     */
    @ParameterizedTest
    @EnumSource(value = AbilitySlot.class, names = {"LIGHT", "SPECIAL", "ULTIMATE"})
    void rangeBoundaryRequiresInteriorOverlap(AbilitySlot slot) {
        configureDirectGeometry();
        double range = tuning(slot).range();
        Enemy inside = target(new CollisionBox(216 + range - 0.01, 206, 4, 4));
        Enemy edge = target(new CollisionBox(216 + range, 206, 4, 4));
        Enemy outside = target(new CollisionBox(217 + range, 206, 4, 4));
        tick(0, command(slot));
        assertEquals(1000 - tuning(slot).attack().damage(), inside.getHealth().getCurrentHp());
        assertEquals(1000, edge.getHealth().getCurrentHp());
        assertEquals(1000, outside.getHealth().getCurrentHp());
    }

    /** A swing captures its origin/direction; turning updates the next swing, not the current rectangle. */
    @Test
    void facingIsRememberedAndCapturedPerSwing() {
        tick(0, new PlayerCommand(0, 1, false, false, false, false));
        tick(0, command(AbilitySlot.LIGHT));
        Attack first = combat.getActiveAttacks().get(0);
        assertEquals(new CollisionBox(204, 206, 12, 56), first.getHitbox());
        tick(0.05, new PlayerCommand(-1, 0, false, false, false, false));
        assertEquals(195, player.getX());
        assertEquals(new CollisionBox(204, 206, 12, 56), first.getHitbox());
        tick(0.31, PlayerCommand.NONE);
        tick(0, command(AbilitySlot.LIGHT));
        Attack second = combat.getActiveAttacks().get(0);
        assertEquals(new CollisionBox(151, 206, 60, 8), second.getHitbox());
        assertNotEquals(first.getId(), second.getId());
    }

    /**
     * A custom direct melee cast settles each target once, including late entrants, and never repeats on re-entry.
     * @param slot tested melee invocation
     */
    @ParameterizedTest
    @EnumSource(value = AbilitySlot.class, names = {"LIGHT", "SPECIAL", "ULTIMATE"})
    void activeWindowHitsLateEntrantsOnlyOnceThenCleansUp(AbilitySlot slot) {
        configureDirectGeometry();
        Enemy first = target(new CollisionBox(230, 206, 4, 4));
        Enemy late = new Enemy(1000);
        Combatant lateActor = new Combatant(late, Faction.ENEMY, new CollisionBox(600, 500, 4, 4));
        combat.register(lateActor);
        abilities.update(0, command(slot));
        assertEquals(1000, first.getHealth().getCurrentHp());
        Attack swing = combat.getActiveAttacks().get(0);
        combat.update(0);
        int damage = tuning(slot).attack().damage();
        assertEquals(1000 - damage, first.getHealth().getCurrentHp());
        assertEquals(1000, late.getHealth().getCurrentHp());
        tick(tuning(slot).attack().activeSeconds() / 2, PlayerCommand.NONE);
        lateActor.setHurtbox(new CollisionBox(240, 206, 4, 4));
        tick(0, PlayerCommand.NONE);
        lateActor.setHurtbox(new CollisionBox(600, 500, 4, 4));
        tick(0, PlayerCommand.NONE);
        lateActor.setHurtbox(new CollisionBox(240, 206, 4, 4));
        for (int i = 0; i < 100; i++) {
            assertTrue(tick(0, command(slot)).isEmpty());
        }
        assertEquals(1000 - damage, first.getHealth().getCurrentHp());
        assertEquals(1000 - damage, late.getHealth().getCurrentHp());
        tick(tuning(slot).attack().activeSeconds(), PlayerCommand.NONE);
        assertFalse(swing.isActive());
        assertTrue(combat.getActiveAttacks().isEmpty());
        Enemy afterExpiry = target(new CollisionBox(240, 206, 4, 4));
        tick(0, PlayerCommand.NONE);
        assertEquals(1000, afterExpiry.getHealth().getCurrentHp());
    }

    /** K blocks one incoming hit and then counter-dashes; it no longer submits an immediate heavy slash. */
    @Test
    void specialInterceptsThenCounterDashes() {
        Enemy near = target(new CollisionBox(240, 206, 4, 4));
        Enemy farther = target(new CollisionBox(280, 206, 4, 4));
        Enemy behind = target(new CollisionBox(180, 206, 4, 4));
        tick(0, command(AbilitySlot.LIGHT));
        assertEquals(970, near.getHealth().getCurrentHp());
        abilities.update(0, command(AbilitySlot.SPECIAL));
        incoming();
        combat.update(0);
        abilities.afterCombat();
        assertEquals(100, player.getHealth().getCurrentHp());
        assertEquals("COUNTER_DASH", abilities.getSkillStatus());
        tick(SkillTuning.COUNTER_SECONDS, PlayerCommand.NONE);
        assertTrue(near.getHealth().getCurrentHp() < 970);
        assertTrue(farther.getHealth().getCurrentHp() < 1000);
        assertEquals(1000, behind.getHealth().getCurrentHp());
        assertTrue(map.isWalkable(player.getBounds()));
        assertTrue(abilities.getCooldownRemaining(AbilitySlot.SPECIAL) > 0);
    }

    /** A new swing after cooldown gets its own hit ledger, so the same enemy can be hit again. */
    @Test
    void laterSwingCanDamageSameTargetWithoutRetainingExpiredAttacks() {
        Enemy enemy = target(new CollisionBox(240, 206, 4, 4));
        tick(0, command(AbilitySlot.LIGHT));
        Attack first = combat.getActiveAttacks().get(0);
        tick(0.35, PlayerCommand.NONE);
        assertEquals(970, enemy.getHealth().getCurrentHp());
        assertFalse(first.isActive());
        assertTrue(combat.getActiveAttacks().isEmpty());
        tick(0, command(AbilitySlot.LIGHT));
        assertEquals(940, enemy.getHealth().getCurrentHp());
        assertEquals(1, combat.getActiveAttacks().size());
        assertNotEquals(first.getId(), combat.getActiveAttacks().get(0).getId());
        abilities.reset();
        assertTrue(combat.getActiveAttacks().isEmpty());
        assertEquals(940, enemy.getHealth().getCurrentHp());
        assertEquals(PlayerClass.KATANA, player.getPlayerClass());
    }

    /** O stance creates one powerful surrounding counter only after interception, not every frame. */
    @Test
    void ultimateStanceCounterHasLethalHealthIntegration() {
        Enemy left = target(new CollisionBox(150, 206, 4, 4));
        Enemy right = target(new CollisionBox(270, 206, 4, 4));
        Enemy above = target(new CollisionBox(206, 270, 4, 4));
        Enemy below = new Enemy(80);
        combat.register(new Combatant(below, Faction.ENEMY, new CollisionBox(206, 150, 4, 4)));
        abilities.update(0, command(AbilitySlot.ULTIMATE));
        assertTrue(combat.getActiveAttacks().isEmpty());
        incoming();
        combat.update(0);
        abilities.afterCombat();
        Attack counter = combat.getActiveAttacks().stream().filter(a -> a.getSource() == source).findFirst().orElseThrow();
        assertEquals("katana-stance", counter.getSpecification().type());
        assertEquals(new CollisionBox(64, 66, 292, 288), counter.getHitbox());
        assertEquals(7, abilities.getCooldownRemaining(AbilitySlot.ULTIMATE));
        tick(0.1, command(AbilitySlot.ULTIMATE));
        for (Enemy enemy : List.of(left, right, above)) {
            assertEquals(900, enemy.getHealth().getCurrentHp());
        }
        assertEquals(0, below.getHealth().getCurrentHp());
        assertFalse(below.isAlive());
        tick(0, command(AbilitySlot.ULTIMATE));
        assertEquals(900, left.getHealth().getCurrentHp());
    }

    /**
     * Every slot has active/cooldown/ready phases; repeated input cannot spend or bypass them.
     * @param slot tested slot
     */
    @ParameterizedTest
    @EnumSource(AbilitySlot.class)
    void cooldownLifecycleRecoversWithoutSpamBypass(AbilitySlot slot) {
        double active = switch (slot) {
            case DASH -> 0.16;
            case SPECIAL -> SkillTuning.PARRY_SECONDS;
            case ULTIMATE -> SkillTuning.STANCE_SECONDS;
            case LIGHT -> tuning(slot).attack().activeSeconds();
        };
        assertEquals(Optional.of(slot), tick(0, command(slot)));
        double cooldown = abilities.getCooldownRemaining(slot);
        assertEquals(AbilityState.ACTIVE, abilities.getState(slot));
        for (int i = 0; i < 30; i++) {
            assertTrue(tick(0, command(slot)).isEmpty());
        }
        assertEquals(cooldown, abilities.getCooldownRemaining(slot));
        tick(active, PlayerCommand.NONE);
        if (slot == AbilitySlot.SPECIAL) {
            tick(SkillTuning.RECOVERY_SECONDS, PlayerCommand.NONE);
        }
        assertEquals(AbilityState.COOLDOWN, abilities.getState(slot));
        assertTrue(tick(0, command(slot)).isEmpty());
        tick(cooldown, PlayerCommand.NONE);
        assertEquals(AbilityState.READY, abilities.getState(slot));
        assertEquals(Optional.of(slot), tick(0, command(slot)));
    }

    /** Self, friendly, dead and unregistered receivers are filtered by combat-core for melee too. */
    @Test
    void invalidTargetsAreIgnoredBySharedPipeline() {
        CollisionBox hit = new CollisionBox(230, 206, 4, 4);
        Enemy friendly = new Enemy();
        Enemy dead = new Enemy();
        dead.takeDamage(100);
        Enemy removed = new Enemy();
        combat.register(new Combatant(friendly, Faction.PLAYER, hit));
        combat.register(new Combatant(dead, Faction.ENEMY, hit));
        Combatant stale = new Combatant(removed, Faction.ENEMY, hit);
        combat.register(stale);
        combat.unregister(stale);
        tick(0, command(AbilitySlot.LIGHT));
        assertEquals(100, player.getHealth().getCurrentHp());
        assertEquals(100, friendly.getHealth().getCurrentHp());
        assertEquals(0, dead.getHealth().getCurrentHp());
        assertEquals(100, removed.getHealth().getCurrentHp());
    }

    /** L uses shared normalized movement, remembers facing and cannot tunnel through a thin wall. */
    @Test
    void dashUsesKatanaTuningAndExistingWallCollision() {
        tick(0.16, new PlayerCommand(1, 1, false, false, true, false));
        assertEquals(120, Math.hypot(player.getX() - 200, player.getY() - 200), EPSILON);
        assertEquals(0.74, abilities.getCooldownRemaining(AbilitySlot.DASH), EPSILON);
        abilities.reset();
        player.enterArea(map, 350, 200);
        tick(0.16, command(AbilitySlot.DASH));
        assertEquals(380, player.getX());
        assertEquals(200, player.getY());
        assertTrue(map.isWalkable(player.getBounds()));
        assertEquals(384, source.getHurtbox().x());
        assertTrue(combat.getActiveAttacks().isEmpty());
    }

    /**
     * The real Katana dash stops at every map edge without granting wall traversal.
     * @param horizontal horizontal input
     * @param vertical vertical input
     * @param x starting horizontal position
     * @param y starting vertical position
     * @param expectedX final horizontal position
     * @param expectedY final vertical position
     */
    @ParameterizedTest
    @CsvSource({"-1,0,10,200,0,200", "1,0,770,500,780,500",
            "0,-1,200,10,200,0", "0,1,200,570,200,580"})
    void dashClampsEveryBoundary(int horizontal, int vertical, double x, double y,
            double expectedX, double expectedY) {
        player.enterArea(map, x, y);
        tick(0.16, new PlayerCommand(horizontal, vertical, false, false, true, false));
        assertEquals(expectedX, player.getX());
        assertEquals(expectedY, player.getY());
        assertTrue(map.isWalkable(player.getBounds()));
    }

    /**
     * Death cancels pending damage/movement and prevents new attacks until explicit restoration.
     * @param slot active ability
     */
    @ParameterizedTest
    @EnumSource(AbilitySlot.class)
    void deathCancelsPendingAbilities(AbilitySlot slot) {
        Enemy enemy = target(new CollisionBox(240, 206, 4, 4));
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

    /** Simultaneous inputs retain common dash/O/K/J priority with independent invocation IDs. */
    @Test
    void simultaneousInputsUseSharedPriorityAndIndependentCooldowns() {
        PlayerCommand all = new PlayerCommand(0, 0, true, true, true, true);
        assertEquals(Optional.of(AbilitySlot.DASH), tick(0, all));
        assertTrue(combat.getActiveAttacks().isEmpty());
        assertTrue(tick(0.16, all).isEmpty());
        assertEquals(Optional.of(AbilitySlot.ULTIMATE), tick(0, all));
        assertTrue(tick(0, all).isEmpty());
        tick(SkillTuning.STANCE_SECONDS, PlayerCommand.NONE);
        assertEquals(Optional.of(AbilitySlot.SPECIAL), tick(0, command(AbilitySlot.SPECIAL)));
        assertTrue(tick(0, command(AbilitySlot.LIGHT)).isEmpty());
        tick(SkillTuning.PARRY_SECONDS, PlayerCommand.NONE);
        tick(SkillTuning.RECOVERY_SECONDS, PlayerCommand.NONE);
        assertEquals(Optional.of(AbilitySlot.LIGHT), tick(0, command(AbilitySlot.LIGHT)));
        assertEquals(1, combat.getActiveAttacks().stream().filter(a -> a.getSource() == source).count());
        tick(7, PlayerCommand.NONE);
        assertTrue(combat.getActiveAttacks().isEmpty());
    }

    /**
     * Registers a stationary target with real shared health.
     * @param bounds independent target hurtbox
     * @return health-owning enemy
     */
    private Enemy target(CollisionBox bounds) {
        Enemy enemy = new Enemy(1000);
        combat.register(new Combatant(enemy, Faction.ENEMY, bounds));
        return enemy;
    }

    /**
     * Samples input then advances combat once in production order.
     * @param delta elapsed simulation seconds
     * @param command one input frame
     * @return triggered slot, or empty
     */
    private Optional<AbilitySlot> tick(double delta, PlayerCommand command) {
        Optional<AbilitySlot> result = abilities.update(delta, command);
        combat.update(delta);
        abilities.afterCombat();
        return result;
    }

    /**
     * Requests a single slot without changing facing.
     * @param slot requested slot
     * @return common input command
     */
    private PlayerCommand command(AbilitySlot slot) {
        return new PlayerCommand(0, 0, slot == AbilitySlot.LIGHT, slot == AbilitySlot.SPECIAL,
                slot == AbilitySlot.DASH, slot == AbilitySlot.ULTIMATE);
    }

    /**
     * Returns the current class's actual attack tuning.
     * @param slot damage-dealing slot
     * @return immutable attack configuration
     * @throws IllegalArgumentException if slot is dash
     */
    private AbilityLoadout.AreaAttack tuning(AbilitySlot slot) {
        AbilityLoadout loadout = player.getClassDefinition().abilities();
        return switch (slot) {
            case LIGHT -> loadout.light();
            case SPECIAL -> loadout.special();
            case ULTIMATE -> loadout.ultimate();
            case DASH -> throw new IllegalArgumentException("Dash is movement, not damage");
        };
    }
}
