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
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Exercises shared ability execution, combat integration, timing and swept dash collision without graphics.
 * @author Daoyan Zhu (u7782042)
 */
final class PlayerAbilitiesTest {
    /** Tolerance for normalized movement and timer subtraction. */
    private static final double EPSILON = 1e-8;
    /** Explicit area-only fixture keeps executor regressions independent of default class delivery. */
    private static final AbilityLoadout AREA_LOADOUT = new AbilityLoadout(
            new AbilityLoadout.AreaAttack(new AttackSpec("test-light", 25, 0.25, 0.5), 96),
            new AbilityLoadout.AreaAttack(new AttackSpec("test-special", 40, 0.3, 2), 120),
            new AbilityLoadout.Dash(600, 0.2, 1),
            new AbilityLoadout.AreaAttack(new AttackSpec("test-ultimate", 75, 0.5, 8), 180));
    /** Existing large map with a thin wall for tunnelling regression checks. */
    private final GameMap map = TestMaps.rectangle(800, 600,
            List.of(new CollisionBox(400, 0, 1, 400)));
    /** Position/health owner used by every isolated fixture. */
    private final Player player = new Player(new CollisionBox(100, 100, 20, 20), 100, map);
    /** Shared pipeline, advanced only once after each ability update. */
    private final CombatSystem combat = new CombatSystem();
    /** Canonical player adapter with a deliberately inset hurtbox. */
    private final Combatant source = new Combatant(player, Faction.PLAYER, new CollisionBox(104, 104, 12, 12));
    /** High-capacity target for independent-slot damage checks. */
    private final Enemy target = new Enemy(1000);
    /** Common controller under test. */
    private final PlayerAbilities abilities;

    /** Creates registered real health/combat models; no graphics context is required. */
    PlayerAbilitiesTest() {
        combat.register(source);
        combat.register(new Combatant(target, Faction.ENEMY, new CollisionBox(160, 100, 20, 20)));
        abilities = new PlayerAbilities(player, source, combat, AREA_LOADOUT);
    }

    /**
     * All attack slots enter the pipeline, deal configured damage once and preserve source health.
     * @param slot damage-dealing ability slot
     */
    @ParameterizedTest
    @EnumSource(value = AbilitySlot.class, names = {"LIGHT", "SPECIAL", "ULTIMATE"})
    void attacksUseSharedPipelineAndHitOnlyOnce(AbilitySlot slot) {
        assertEquals(AbilityState.READY, abilities.getState(slot));
        assertEquals(Optional.of(slot), abilities.update(0, command(slot)));
        assertEquals(1000, target.getHealth().getCurrentHp());
        Attack attack = combat.getActiveAttacks().get(0);
        assertSame(source, attack.getSource());
        assertEquals(tuning(slot).attack().type(), attack.getSpecification().type());
        assertEquals(0, combat.getCooldownRemaining(source));
        combat.update(0);
        assertEquals(1000 - tuning(slot).attack().damage(), target.getHealth().getCurrentHp());
        assertEquals(100, player.getHealth().getCurrentHp());
        for (int i = 0; i < 30; i++) {
            assertTrue(tick(0, command(slot)).isEmpty());
        }
        assertEquals(1000 - tuning(slot).attack().damage(), target.getHealth().getCurrentHp());
        assertEquals(AbilityState.ACTIVE, abilities.getState(slot));
    }

    /**
     * Ready, active, cooldown and ready again use elapsed simulation time, not number of inputs.
     * @param slot slot whose full lifecycle is checked
     */
    @ParameterizedTest
    @EnumSource(AbilitySlot.class)
    void cooldownAndLifecycleRecover(AbilitySlot slot) {
        tick(0, command(slot));
        assertEquals(AbilityState.ACTIVE, abilities.getState(slot));
        double duration = slot == AbilitySlot.DASH ? AREA_LOADOUT.dash().duration()
                : tuning(slot).attack().activeSeconds();
        double cooldown = abilities.getCooldownRemaining(slot);
        tick(duration, PlayerCommand.NONE);
        assertEquals(AbilityState.COOLDOWN, abilities.getState(slot));
        assertTrue(tick(0, command(slot)).isEmpty());
        assertEquals(cooldown - duration, abilities.getCooldownRemaining(slot), EPSILON);
        tick(cooldown, PlayerCommand.NONE);
        assertEquals(AbilityState.READY, abilities.getState(slot));
        assertEquals(Optional.of(slot), tick(0, command(slot)));
    }

    /** Light, special and ultimate never borrow one another's slot cooldown. */
    @Test
    void attackCooldownsAreIndependent() {
        assertEquals(Optional.of(AbilitySlot.LIGHT), tick(0, command(AbilitySlot.LIGHT)));
        assertEquals(Optional.of(AbilitySlot.SPECIAL), tick(0, command(AbilitySlot.SPECIAL)));
        assertEquals(Optional.of(AbilitySlot.ULTIMATE), tick(0, command(AbilitySlot.ULTIMATE)));
        assertEquals(860, target.getHealth().getCurrentHp());
        assertEquals(3, combat.getActiveAttacks().size());
        assertEquals(0.5, abilities.getCooldownRemaining(AbilitySlot.LIGHT));
        assertEquals(2, abilities.getCooldownRemaining(AbilitySlot.SPECIAL));
        assertEquals(8, abilities.getCooldownRemaining(AbilitySlot.ULTIMATE));
    }

    /** Injected tuning changes damage, area and timing without adding keyboard or class-specific rules. */
    @Test
    void usesInjectedLoadoutAndRejectsOutOfRangeTargets() {
        AbilityLoadout.AreaAttack small = new AbilityLoadout.AreaAttack(new AttackSpec("custom", 7, 0.1, 3), 1);
        AbilityLoadout custom = new AbilityLoadout(small, small, new AbilityLoadout.Dash(50, 0.5, 2), small);
        PlayerAbilities configured = new PlayerAbilities(player, source, combat, custom);
        assertEquals(Optional.of(AbilitySlot.LIGHT), configured.update(0, command(AbilitySlot.LIGHT)));
        combat.update(0);
        assertEquals(1000, target.getHealth().getCurrentHp());
        assertEquals(7, combat.getActiveAttacks().get(0).getSpecification().damage());
        assertEquals(3, configured.getCooldownRemaining(AbilitySlot.LIGHT));
        configured.update(0.5, command(AbilitySlot.DASH));
        combat.update(0.5);
        assertEquals(125, player.getX());
    }

    /** A pre-existing source-level combat delay rejects a slot request without consuming its own cooldown. */
    @Test
    void rejectedPipelineRequestDoesNotConsumeSlot() {
        combat.tryAttack(source, new AttackSpec("external", 0, 0.1, 1), source.getHurtbox());
        assertTrue(tick(0, command(AbilitySlot.LIGHT)).isEmpty());
        assertEquals(0, abilities.getCooldownRemaining(AbilitySlot.LIGHT));
        tick(1, PlayerCommand.NONE);
        assertEquals(Optional.of(AbilitySlot.LIGHT), tick(0, command(AbilitySlot.LIGHT)));
    }

    /** A queued attack from a dead source is cancelled by combat-core, not settled by ability code. */
    @Test
    void deathCancelsPendingDamageThroughCombatCore() {
        abilities.update(0, command(AbilitySlot.ULTIMATE));
        Attack pending = combat.getActiveAttacks().get(0);
        player.takeDamage(100);
        combat.update(0);
        assertFalse(pending.isActive());
        assertEquals(1000, target.getHealth().getCurrentHp());
        assertEquals(AbilityState.UNAVAILABLE, abilities.getState(AbilitySlot.ULTIMATE));
    }

    /** A zero-time dash arms its active window but cannot move or age its cooldown. */
    @Test
    void zeroTimeDoesNotMoveOrAgeDash() {
        CollisionBox before = player.getBounds();
        tick(0, command(AbilitySlot.DASH));
        for (int i = 0; i < 20; i++) {
            tick(0, command(AbilitySlot.DASH));
        }
        assertEquals(before, player.getBounds());
        assertEquals(1, abilities.getCooldownRemaining(AbilitySlot.DASH));
        assertEquals(AbilityState.ACTIVE, abilities.getState(AbilitySlot.DASH));
    }

    /** Dash wins simultaneous input; attacks otherwise prefer O then K then J among ready slots. */
    @Test
    void simultaneousInputHasStablePriorityAndNoBuffer() {
        PlayerCommand all = new PlayerCommand(0, 0, true, true, true, true);
        assertEquals(Optional.of(AbilitySlot.DASH), tick(0, all));
        assertTrue(combat.getActiveAttacks().isEmpty());
        assertTrue(tick(0.2, all).isEmpty());
        assertTrue(tick(0, PlayerCommand.NONE).isEmpty());
        assertEquals(Optional.of(AbilitySlot.ULTIMATE), tick(0, all));
        assertEquals(Optional.of(AbilitySlot.SPECIAL), tick(0, all));
        assertEquals(Optional.of(AbilitySlot.LIGHT), tick(0, all));
        assertTrue(tick(0, all).isEmpty());
        assertEquals(3, combat.getActiveAttacks().size());
    }

    /**
     * Normalized eight-direction dash covers the same distance without additional walking that frame.
     * @param horizontal horizontal intent
     * @param vertical vertical intent
     */
    @ParameterizedTest
    @CsvSource({"1,0", "-1,0", "0,1", "0,-1", "1,1", "1,-1", "-1,1", "-1,-1"})
    void dashHasFixedNormalizedDistance(int horizontal, int vertical) {
        player.enterArea(map, 200, 200);
        tick(0.2, new PlayerCommand(horizontal, vertical, false, false, true, false));
        assertEquals(120, Math.hypot(player.getX() - 200, player.getY() - 200), EPSILON);
        double norm = Math.hypot(horizontal, vertical);
        assertEquals(200 + horizontal / norm * 120, player.getX(), EPSILON);
        assertEquals(200 + vertical / norm * 120, player.getY(), EPSILON);
        assertEquals(player.getX() + 4, source.getHurtbox().x(), EPSILON);
        assertEquals(12, source.getHurtbox().width());
    }

    /**
     * Swept movement stops against all world edges even with an oversized simulation step.
     * @param horizontal horizontal dash direction
     * @param vertical vertical dash direction
     * @param x initial horizontal position
     * @param y initial vertical position
     * @param expectedX expected clamped horizontal position
     * @param expectedY expected clamped vertical position
     */
    @ParameterizedTest
    @CsvSource({"-1,0,10,100,0,100", "1,0,760,500,780,500",
            "0,-1,100,10,100,0", "0,1,100,570,100,580"})
    void dashClampsAllMapEdges(int horizontal, int vertical, double x, double y,
            double expectedX, double expectedY) {
        player.enterArea(map, x, y);
        tick(10, new PlayerCommand(horizontal, vertical, false, false, true, false));
        assertEquals(expectedX, player.getX());
        assertEquals(expectedY, player.getY());
        assertTrue(map.isWalkable(player.getBounds()));
    }

    /** Thin walls stop high-speed bursts and cooldown is consumed even if travel is blocked. */
    @Test
    void dashCannotTunnelThroughThinWall() {
        player.enterArea(map, 350, 100);
        tick(0.2, new PlayerCommand(1, 0, false, false, true, false));
        assertEquals(380, player.getX());
        assertEquals(100, player.getY());
        assertEquals(0.8, abilities.getCooldownRemaining(AbilitySlot.DASH), EPSILON);
        assertTrue(tick(0, command(AbilitySlot.DASH)).isEmpty());
        tick(1, PlayerCommand.NONE);
        assertEquals(Optional.of(AbilitySlot.DASH), tick(0.2, command(AbilitySlot.DASH)));
        assertEquals(380, player.getX());
        assertTrue(map.isWalkable(player.getBounds()));
    }

    /** Diagonal dashes use existing axis sweeps and may slide, but never overlap obstacles. */
    @Test
    void diagonalDashSlidesAlongWall() {
        player.enterArea(map, 350, 100);
        tick(0.2, new PlayerCommand(1, 1, false, false, true, false));
        assertEquals(380, player.getX());
        assertEquals(100 + 120 / Math.sqrt(2), player.getY(), EPSILON);
        assertTrue(map.isWalkable(player.getBounds()));
    }

    /** A stationary dash remembers facing; steering during its active window cannot redirect it. */
    @Test
    void dashRemembersFacingAndLocksDirection() {
        tick(0, new PlayerCommand(0, 1, false, false, false, false));
        tick(0.1, command(AbilitySlot.DASH));
        tick(0.1, new PlayerCommand(-1, 0, false, false, true, false));
        assertEquals(100, player.getX());
        assertEquals(220, player.getY(), EPSILON);
    }

    /** Large frames spend only the dash duration at dash speed, then walk for the leftover time. */
    @Test
    void dashUsesOnlyItsDurationAndDoesNotDoubleMove() {
        tick(0.3, new PlayerCommand(1, 0, false, false, true, false));
        assertEquals(230, player.getX(), EPSILON);
        assertEquals(0.7, abilities.getCooldownRemaining(AbilitySlot.DASH), EPSILON);
    }

    /** Partitioning a dash frame does not change unobstructed displacement or total cooldown. */
    @Test
    void dashIsFrameRateIndependent() {
        tick(0, new PlayerCommand(0, 1, false, false, true, false));
        for (int i = 0; i < 20; i++) {
            tick(0.01, PlayerCommand.NONE);
        }
        assertEquals(220, player.getY(), EPSILON);
        assertEquals(0.8, abilities.getCooldownRemaining(AbilitySlot.DASH), EPSILON);
        assertEquals(AbilityState.COOLDOWN, abilities.getState(AbilitySlot.DASH));
    }

    /** Death interrupts dash, blocks every slot and movement, and explicit restoration/reset recovers. */
    @Test
    void deathStopsActionsAndResetRestoresReadinessWithoutHealing() {
        tick(0.05, command(AbilitySlot.DASH));
        CollisionBox before = player.getBounds();
        player.takeDamage(100);
        for (AbilitySlot slot : AbilitySlot.values()) {
            assertTrue(tick(0, command(slot)).isEmpty());
            assertEquals(AbilityState.UNAVAILABLE, abilities.getState(slot));
        }
        tick(1, new PlayerCommand(1, 1, true, true, true, true));
        assertEquals(before, player.getBounds());
        abilities.reset();
        assertFalse(player.isAlive());
        player.getHealth().restoreFull();
        for (AbilitySlot slot : AbilitySlot.values()) {
            assertEquals(AbilityState.READY, abilities.getState(slot));
        }
        assertEquals(Optional.of(AbilitySlot.LIGHT), tick(0, command(AbilitySlot.LIGHT)));
    }

    /** Reset cancels retained player invocations without clearing targets or their health. */
    @Test
    void resetCancelsAttacksAndPreservesTargetHealth() {
        tick(0, command(AbilitySlot.ULTIMATE));
        Attack old = combat.getActiveAttacks().get(0);
        abilities.reset();
        assertFalse(old.isActive());
        assertTrue(combat.getActiveAttacks().isEmpty());
        assertEquals(925, target.getHealth().getCurrentHp());
        assertEquals(2, combat.getCombatants().size());
        assertEquals(AbilityState.READY, abilities.getState(AbilitySlot.ULTIMATE));
    }

    /** Unregistered controllers cannot continue moving or enqueue attacks in an old arena. */
    @Test
    void rejectsStaleSourceAndInvalidBindings() {
        assertThrows(IllegalArgumentException.class, () -> abilities.bindAfterTransition(
                new Combatant(player, Faction.PLAYER, player.getBounds())));
        assertThrows(NullPointerException.class, () -> abilities.bindAfterTransition(null));
        combat.unregister(source);
        CollisionBox before = player.getBounds();
        assertTrue(tick(1, new PlayerCommand(1, 0, true, true, true, true)).isEmpty());
        assertEquals(before, player.getBounds());
        assertEquals(AbilityState.UNAVAILABLE, abilities.getState(AbilitySlot.LIGHT));
        assertThrows(IllegalArgumentException.class, abilities::reset);
    }

    /**
     * Invalid time is rejected before position, cooldown, or damage can change.
     * @param delta rejected simulation time
     */
    @ParameterizedTest
    @ValueSource(doubles = {-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.MAX_VALUE})
    void rejectsInvalidDeltaAtomically(double delta) {
        CollisionBox before = player.getBounds();
        assertThrows(IllegalArgumentException.class, () -> abilities.update(delta,
                new PlayerCommand(1, 0, true, true, true, true)));
        assertEquals(before, player.getBounds());
        assertEquals(0, abilities.getCooldownRemaining(AbilitySlot.DASH));
        assertTrue(combat.getActiveAttacks().isEmpty());
        assertEquals(1000, target.getHealth().getCurrentHp());
    }

    /** Required command/slot/constructor dependencies and axis domains are validated explicitly. */
    @Test
    void rejectsNullsAndInvalidCommands() {
        assertThrows(NullPointerException.class, () -> abilities.update(0, null));
        assertThrows(NullPointerException.class, () -> abilities.getState(null));
        assertThrows(NullPointerException.class, () -> abilities.getCooldownRemaining(null));
        assertThrows(NullPointerException.class, () -> new PlayerAbilities(null, source, combat,
                AREA_LOADOUT));
        assertThrows(IllegalArgumentException.class, () -> new PlayerCommand(2, 0, false, false, false, false));
        assertThrows(IllegalArgumentException.class, () -> new PlayerCommand(0, -2, false, false, false, false));
    }

    /**
     * Advances abilities and shared combat in the production order.
     * @param delta simulation seconds
     * @param command one frame of input
     * @return triggered slot, if any
     */
    private Optional<AbilitySlot> tick(double delta, PlayerCommand command) {
        Optional<AbilitySlot> result = abilities.update(delta, command);
        combat.update(delta);
        return result;
    }

    /**
     * Requests exactly one ability without movement intent.
     * @param slot requested slot
     * @return corresponding frame command
     */
    private PlayerCommand command(AbilitySlot slot) {
        return new PlayerCommand(0, 0, slot == AbilitySlot.LIGHT, slot == AbilitySlot.SPECIAL,
                slot == AbilitySlot.DASH, slot == AbilitySlot.ULTIMATE);
    }

    /**
     * Finds the expected explicit area-fixture attack configuration.
     * @param slot damage-dealing slot
     * @return configured area attack
     * @throws IllegalArgumentException if slot is dash
     */
    private AbilityLoadout.AreaAttack tuning(AbilitySlot slot) {
        return switch (slot) {
            case LIGHT -> AREA_LOADOUT.light();
            case SPECIAL -> AREA_LOADOUT.special();
            case ULTIMATE -> AREA_LOADOUT.ultimate();
            case DASH -> throw new IllegalArgumentException("Dash does not deal damage");
        };
    }
}
