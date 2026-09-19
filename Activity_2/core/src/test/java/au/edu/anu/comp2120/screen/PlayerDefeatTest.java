package au.edu.anu.comp2120.screen;

import static org.junit.jupiter.api.Assertions.*;

import au.edu.anu.comp2120.GameSession;
import au.edu.anu.comp2120.TravelSystem;
import au.edu.anu.comp2120.combat.Attack;
import au.edu.anu.comp2120.combat.AttackSpec;
import au.edu.anu.comp2120.combat.Combatant;
import au.edu.anu.comp2120.combat.CombatSystem;
import au.edu.anu.comp2120.combat.Damageable;
import au.edu.anu.comp2120.combat.ElementType;
import au.edu.anu.comp2120.combat.Faction;
import au.edu.anu.comp2120.combat.ProjectileSpec;
import au.edu.anu.comp2120.enemy.Boss;
import au.edu.anu.comp2120.enemy.BossState;
import au.edu.anu.comp2120.player.AbilityLoadout;
import au.edu.anu.comp2120.player.AbilitySlot;
import au.edu.anu.comp2120.player.AbilityState;
import au.edu.anu.comp2120.player.ClassDefinition;
import au.edu.anu.comp2120.player.PlayerClass;
import au.edu.anu.comp2120.player.PlayerClasses;
import au.edu.anu.comp2120.player.PlayerCommand;
import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.MapCatalog;
import au.edu.anu.comp2120.world.GameMap;
import au.edu.anu.comp2120.world.MapType;
import au.edu.anu.comp2120.world.SpawnPoint;
import au.edu.anu.comp2120.world.TravelCatalog;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Verifies failure settlement, same-player Hub recovery and full encounter invalidation without graphics.
 * Uses the production coordinator and its one shared pipeline, never Quest or an alternate damage engine.
 * @author Daoyan Zhu (u7782042)
 */
final class PlayerDefeatTest {
    /** Built-in map session starting in a battle encounter. */
    private final GameSession session = new GameSession(MapCatalog.createMaps(), MapCatalog.CROSSROADS, 28, 28, 180);
    /** Real shared combat pipeline retained only to arrange/inspect cross-module settlement. */
    private final CombatSystem combat = new CombatSystem();
    /** Actual gameplay coordinator owns all frame updates and scope transitions. */
    private final CombatDemo demo = new CombatDemo(session, combat);

    /** Creates an isolated Boss-only battle fixture. */
    PlayerDefeatTest() {
    }

    /**
     * Every class retains its exact selected O configuration and shared identities after real Boss damage.
     * There is no separate O-mode selector in this checkout; a non-default ultimate configuration
     * exercises the existing selection boundary rather than inventing an unimplemented mode API.
     * @param type selected profession
     */
    @ParameterizedTest
    @EnumSource(PlayerClass.class)
    void lethalBossAttackReturnsSamePlayerWithClassAndUltimateConfiguration(PlayerClass type) {
        ClassDefinition original = PlayerClasses.get(type);
        AbilityLoadout slots = original.abilities();
        AbilityLoadout.AreaAttack ultimate = slots.ultimate();
        AttackSpec spec = ultimate.attack();
        AbilityLoadout.AreaAttack selectedUltimate = new AbilityLoadout.AreaAttack(
                new AttackSpec("selected-o-configuration", spec.damage(), spec.activeSeconds(),
                        spec.cooldownSeconds(), ElementType.ICE), ultimate.range(), ultimate.shape(), ultimate.projectile());
        ClassDefinition selected = new ClassDefinition(type,
                new AbilityLoadout(slots.light(), slots.special(), slots.dash(), selectedUltimate));
        assertTrue(demo.getAbilities().selectClass(selected));
        var player = session.getPlayer();
        var health = player.getHealth();
        var abilities = demo.getAbilities();
        long encounter = session.getTransitionVersion();
        player.enterArea(session.getCurrentMap(), 320, 100);
        health.takeDamage(99);
        demo.update(0, PlayerCommand.NONE, false);
        assertEquals(MapCatalog.HUB, session.getCurrentMap().getId());
        assertSame(player, session.getPlayer());
        assertSame(health, player.getHealth());
        assertSame(abilities, demo.getAbilities());
        assertSame(selected, player.getClassDefinition());
        assertSame(selectedUltimate, player.getClassDefinition().abilities().ultimate());
        assertEquals(type, player.getPlayerClass());
        assertEquals(health.getMaxHp(), health.getCurrentHp());
        assertEquals(new CollisionBox(64, 64, 28, 28), player.getBounds());
        assertSame(session.getCurrentMap(), player.getCollisionArea());
        assertEquals(new CombatDemo.Defeat(MapCatalog.CROSSROADS, encounter), demo.getLastDefeat().orElseThrow());
        assertEquals(encounter + 1, session.getTransitionVersion());
        assertTrue(demo.getBoss().isEmpty());
        assertEquals(List.of(playerActor()), combat.getCombatants());
        demo.update(0, new PlayerCommand(0, 0, false, false, false, true), false);
        assertFalse(combat.getActiveAttacks().isEmpty());
        assertTrue(combat.getActiveAttacks().stream().allMatch(attack ->
                attack.getSpecification().type().equals("selected-o-configuration")
                        && attack.getSpecification().element() == ElementType.ICE));
    }

    /** Damage callbacks finish in the battle scope; only the later coordinator phase switches and heals. */
    @Test
    void returnOccursAfterCompleteCombatSettlementNotInsideDamageCallback() {
        AtomicBoolean observed = new AtomicBoolean();
        Combatant probe = new Combatant(new Damageable() {
            /**
             * Observes settlement ordering without mutating the arena from a callback.
             * @param damage damage delivered by the shared pipeline
             */
            @Override
            public void takeDamage(int damage) {
                assertEquals(100, damage);
                assertFalse(session.getPlayer().isAlive());
                assertEquals(MapCatalog.CROSSROADS, session.getCurrentMap().getId());
                assertTrue(demo.getBoss().isPresent());
                observed.set(true);
            }

            /**
             * Keeps the observation receiver eligible for this single settlement.
             * @return true for the duration of this test
             */
            @Override
            public boolean isAlive() {
                return true;
            }
        }, Faction.PLAYER, playerActor().getHurtbox());
        combat.register(probe);
        lethalAttack();
        demo.update(0, PlayerCommand.NONE, false);
        assertTrue(observed.get());
        assertEquals(MapCatalog.HUB, session.getCurrentMap().getId());
        assertEquals(100, session.getPlayer().getHealth().getCurrentHp());
        assertFalse(combat.getCombatants().contains(probe));
    }

    /**
     * Dead-player commands cannot move, launch or bypass return through reset/class/element selection.
     * @param slot requested J/K/L/O slot
     */
    @ParameterizedTest
    @EnumSource(AbilitySlot.class)
    void deadInputIsRejectedAndRecoveryConsumesTheFrame(AbilitySlot slot) {
        var player = session.getPlayer();
        var selected = player.getClassDefinition();
        player.takeDamage(100);
        CollisionBox deathPosition = player.getBounds();
        assertTrue(demo.getAbilities().update(0.1, command(slot)).isEmpty());
        assertEquals(deathPosition, player.getBounds());
        assertTrue(combat.getActiveAttacks().isEmpty());
        assertEquals(AbilityState.UNAVAILABLE, demo.getAbilities().getState(slot));
        demo.update(0.1, command(slot), true, PlayerClass.ARCHER, true);
        assertSame(selected, player.getClassDefinition());
        assertEquals(new CollisionBox(64, 64, 28, 28), player.getBounds());
        assertEquals(MapCatalog.HUB, session.getCurrentMap().getId());
        assertTrue(combat.getActiveAttacks().isEmpty());
        assertTrue(demo.getBoss().isEmpty());
    }

    /** Dead WASD alone is blocked; normal Hub movement resumes only in a subsequent live frame. */
    @Test
    void deadMovementStopsAndResumesOnlyAfterHubRecovery() {
        session.getPlayer().takeDamage(100);
        CollisionBox before = session.getPlayer().getBounds();
        PlayerCommand move = new PlayerCommand(1, 1, false, false, false, false);
        demo.getAbilities().update(1, move);
        assertEquals(before, session.getPlayer().getBounds());
        demo.update(0.1, move, false);
        assertEquals(64, session.getPlayer().getX());
        demo.update(0.1, move, false);
        assertEquals(64 + 18 / Math.sqrt(2), session.getPlayer().getX(), 1e-9);
    }

    /**
     * Every active slot loses its launch/hitbox/dash state while retaining its unspent cooldown.
     * @param slot active slot at lethal settlement
     */
    @ParameterizedTest
    @EnumSource(AbilitySlot.class)
    void activeAbilityIsCancelledDuringFailure(AbilitySlot slot) {
        demo.getAbilities().update(0, command(slot));
        assertEquals(slot == AbilitySlot.SPECIAL ? AbilityState.COOLDOWN : AbilityState.ACTIVE, demo.getAbilities().getState(slot));
        double cooldown = demo.getAbilities().getCooldownRemaining(slot);
        List<Attack> old = combat.getActiveAttacks();
        lethalAttack();
        demo.update(0, PlayerCommand.NONE, false);
        assertEquals(MapCatalog.HUB, session.getCurrentMap().getId());
        assertTrue(old.stream().noneMatch(Attack::isActive));
        assertTrue(combat.getActiveAttacks().isEmpty());
        assertEquals(AbilityState.COOLDOWN, demo.getAbilities().getState(slot));
        assertEquals(cooldown, demo.getAbilities().getCooldownRemaining(slot));
        CollisionBox arrival = session.getPlayer().getBounds();
        demo.update(0.1, PlayerCommand.NONE, false);
        assertEquals(arrival, session.getPlayer().getBounds(), "Old dash must not resume in Hub");
    }

    /** Both sources' projectiles, fixed hitboxes, attachments and queued derived effects leave the old scope. */
    @Test
    void failureClearsAllCombatObjectsAndElementState() {
        Combatant oldPlayer = playerActor();
        Boss oldBoss = demo.getBoss().orElseThrow();
        Combatant hostile = oldBoss.getCombatant();
        combat.tryAttack(oldPlayer, new AttackSpec("ice-prime", 0, 10, 0, ElementType.ICE), hostile.getHurtbox());
        combat.tryAttack(hostile, new AttackSpec("ice-prime", 0, 10, 0, ElementType.ICE), oldPlayer.getHurtbox());
        combat.update(0);
        combat.tryAttack(oldPlayer, new AttackSpec("reaction", 0, 10, 0, ElementType.LIGHTNING), hostile.getHurtbox());
        combat.update(0);
        List<Attack> derived = combat.getActiveAttacks().stream().filter(Attack::isDerived).toList();
        assertFalse(derived.isEmpty());
        combat.tryAttack(oldPlayer, new AttackSpec("fire-prime", 0, 10, 0, ElementType.FIRE), hostile.getHurtbox());
        combat.update(0);
        assertEquals(ElementType.FIRE, combat.getElementStatus(hostile).element());
        assertEquals(ElementType.ICE, combat.getElementStatus(oldPlayer).element());
        ProjectileSpec flight = new ProjectileSpec(50, 4, 5, 1, 0);
        assertFalse(combat.tryProjectiles(oldPlayer, new AttackSpec("player-flight", 0, 1, 0),
                flight, 250, 0, 1, session.getCurrentMap()).isEmpty());
        assertFalse(combat.tryProjectiles(hostile, new AttackSpec("boss-flight", 0, 1, 0),
                flight, 250, 0, 1, session.getCurrentMap()).isEmpty());
        List<Attack> oldAttacks = combat.getActiveAttacks();
        assertEquals(2, oldAttacks.stream().filter(attack -> attack.getProjectile() != null).count());
        CollisionBox oldPosition = oldBoss.getBounds();
        lethalAttack();
        demo.update(0, PlayerCommand.NONE, false);
        assertTrue(oldAttacks.stream().noneMatch(Attack::isActive));
        assertTrue(derived.stream().noneMatch(Attack::isActive));
        assertTrue(combat.getActiveAttacks().isEmpty());
        assertFalse(combat.getCombatants().contains(oldPlayer));
        assertFalse(combat.getCombatants().contains(hostile));
        assertEquals(ElementType.NONE, combat.getElementStatus(playerActor()).element());
        assertThrows(IllegalArgumentException.class, () -> combat.getCooldownRemaining(hostile));
        oldBoss.update(playerActor(), 1);
        oldBoss.synchronizeOutcome();
        assertEquals(oldPosition, oldBoss.getBounds());
        assertTrue(oldBoss.getDrop().isEmpty());
        assertTrue(demo.getDrops().isEmpty());
        // Re-register only for inspection: clear must remove attachment storage, not just hide its owner.
        combat.register(hostile);
        assertEquals(ElementType.NONE, combat.getElementStatus(hostile).element());
        assertEquals(0, combat.getCooldownRemaining(hostile));
        combat.unregister(hostile);
        for (int i = 0; i < 20; i++) {
            demo.update(0.1, PlayerCommand.NONE, false);
        }
        assertEquals(100, session.getPlayer().getHealth().getCurrentHp());
        assertTrue(combat.getActiveAttacks().isEmpty());
    }

    /** Retry never revives the phase-two object: full HP, phase one, idle state and first-pattern cooldown are fresh. */
    @Test
    void retryCreatesNewBossHealthAiPhaseAndCooldown() {
        Boss old = demo.getBoss().orElseThrow();
        session.getPlayer().enterArea(session.getCurrentMap(), 320, 100);
        old.getHealth().takeDamage(200);
        demo.update(0, PlayerCommand.NONE, false);
        assertEquals(2, old.getPhase());
        demo.update(0.75, PlayerCommand.NONE, false);
        demo.update(0, PlayerCommand.NONE, false);
        assertTrue(combat.getCooldownRemaining(old.getCombatant()) > 0);
        session.getPlayer().takeDamage(100);
        demo.update(0, PlayerCommand.NONE, false);
        assertEquals(MapCatalog.HUB, session.getCurrentMap().getId());
        session.switchMap(MapCatalog.CROSSROADS);
        demo.update(0, PlayerCommand.NONE, false);
        Boss fresh = demo.getBoss().orElseThrow();
        assertNotSame(old, fresh);
        assertNotSame(old.getCombatant(), fresh.getCombatant());
        assertNotSame(old.getHealth(), fresh.getHealth());
        assertEquals(fresh.getHealth().getMaxHp(), fresh.getHealth().getCurrentHp());
        assertEquals(1, fresh.getPhase());
        assertEquals(BossState.IDLE, fresh.getState());
        assertEquals(0, fresh.getTransitionRemaining());
        assertEquals(0, combat.getCooldownRemaining(fresh.getCombatant()));
        assertEquals(2, combat.getCombatants().size());
        assertTrue(demo.getEnemies().isEmpty());
        assertTrue(demo.getDrops().isEmpty());
        session.getPlayer().enterArea(session.getCurrentMap(), 320, 100);
        demo.update(0, PlayerCommand.NONE, false);
        Attack attack = combat.getActiveAttacks().stream()
                .filter(candidate -> candidate.getSource() == fresh.getCombatant()).findFirst().orElseThrow();
        assertEquals("boss-sweep", attack.getSpecification().type());
        assertEquals(1.2, combat.getCooldownRemaining(fresh.getCombatant()));
    }

    /** One death causes one return; later Hub updates do not repeat healing, resetting or transitions. */
    @Test
    void repeatedPollingCannotReturnOrRestoreTwice() {
        session.getPlayer().takeDamage(100);
        assertTrue(demo.returnToHubIfDefeated());
        var failure = demo.getLastDefeat().orElseThrow();
        long version = session.getTransitionVersion();
        session.getPlayer().takeDamage(5);
        demo.update(0, command(AbilitySlot.ULTIMATE), false);
        double cooldown = demo.getAbilities().getCooldownRemaining(AbilitySlot.ULTIMATE);
        for (int i = 0; i < 20; i++) {
            assertFalse(demo.returnToHubIfDefeated());
            demo.update(0, PlayerCommand.NONE, false);
        }
        assertSame(failure, demo.getLastDefeat().orElseThrow());
        assertEquals(version, session.getTransitionVersion());
        assertEquals(95, session.getPlayer().getHealth().getCurrentHp());
        assertEquals(cooldown, demo.getAbilities().getCooldownRemaining(AbilitySlot.ULTIMATE));
    }

    /** Existing travel arrival guards prevent a Hub-spawn portal from immediately restarting the failed encounter. */
    @Test
    void failureReturnUsesTravelGuardAndAllowsExplicitPortalRetry() {
        var portal = new au.edu.anu.comp2120.world.Portal("retry", MapCatalog.HUB,
                new CollisionBox(64, 64, 80, 80), MapCatalog.CROSSROADS, "entry", true);
        TravelSystem travel = new TravelSystem(session, List.of(portal), TravelCatalog.createDestinations());
        Boss old = demo.getBoss().orElseThrow();
        session.getPlayer().takeDamage(100);
        demo.update(0, PlayerCommand.NONE, false);
        for (int i = 0; i < 10; i++) {
            assertFalse(travel.updatePortals());
        }
        demo.update(0.5, new PlayerCommand(1, 0, false, false, false, false), false);
        assertFalse(travel.updatePortals());
        demo.update(0.1, new PlayerCommand(-1, 0, false, false, false, false), false);
        assertTrue(travel.updatePortals());
        demo.update(0, PlayerCommand.NONE, false);
        assertEquals(MapCatalog.CROSSROADS, session.getCurrentMap().getId());
        assertNotSame(old, demo.getBoss().orElseThrow());
        session.getPlayer().takeDamage(100);
        demo.update(0, PlayerCommand.NONE, false);
        assertTrue(demo.getLastDefeat().orElseThrow().encounterVersion() > 0);
    }

    /** Alive players and Hub death are not battle failures; direct Health damage never switches maps itself. */
    @Test
    void onlyBattleDeathTriggersFailure() {
        assertFalse(demo.returnToHubIfDefeated());
        assertTrue(demo.getLastDefeat().isEmpty());
        session.switchMap(MapCatalog.HUB);
        demo.update(0, PlayerCommand.NONE, false);
        session.getPlayer().takeDamage(100);
        long version = session.getTransitionVersion();
        assertFalse(demo.returnToHubIfDefeated());
        demo.update(0.1, command(AbilitySlot.LIGHT), false);
        assertFalse(session.getPlayer().isAlive());
        assertEquals(version, session.getTransitionVersion());
        assertTrue(demo.getLastDefeat().isEmpty());
        assertTrue(combat.getActiveAttacks().isEmpty());
    }

    /**
     * Both named Battle spawns support a fresh single-Boss retry after failure; practice is not a battle.
     * @param spawnId Battle spawn ID
     */
    @ParameterizedTest
    @ValueSource(strings = {"entry", "east"})
    void everyBattleSpawnReturnsAndRebuilds(String spawnId) {
        session.switchMap(MapCatalog.CROSSROADS, spawnId);
        demo.update(0, PlayerCommand.NONE, false);
        Boss old = demo.getBoss().orElseThrow();
        session.getPlayer().takeDamage(100);
        demo.update(0, PlayerCommand.NONE, false);
        assertEquals(MapCatalog.HUB, session.getCurrentMap().getId());
        assertEquals(MapCatalog.CROSSROADS, demo.getLastDefeat().orElseThrow().mapId());
        session.switchMap(MapCatalog.CROSSROADS, spawnId);
        demo.update(0, PlayerCommand.NONE, false);
        assertNotSame(old, demo.getBoss().orElseThrow());
        assertEquals(400, demo.getBoss().orElseThrow().getHealth().getCurrentHp());
        assertEquals(2, combat.getCombatants().size());
        assertTrue(demo.getEnemies().isEmpty());
    }

    /** Invalid input is rejected before recovery mutates the map or restores health. */
    @Test
    void invalidFramesCannotPartiallyRecover() {
        session.getPlayer().takeDamage(100);
        long version = session.getTransitionVersion();
        assertThrows(IllegalArgumentException.class, () -> demo.update(Double.NaN, PlayerCommand.NONE, false));
        assertThrows(NullPointerException.class, () -> demo.update(0, null, false));
        assertEquals(version, session.getTransitionVersion());
        assertFalse(session.getPlayer().isAlive());
        assertTrue(demo.getLastDefeat().isEmpty());
    }

    /** A malformed catalogue fails explicitly instead of reviving in the battle map or inventing a Hub. */
    @Test
    void missingHubDoesNotReviveInBattle() {
        GameSession isolated = new GameSession(List.of(session.getCurrentMap()), MapCatalog.CROSSROADS, 28, 28, 180);
        CombatDemo arena = new CombatDemo(isolated);
        isolated.getPlayer().takeDamage(100);
        assertThrows(IllegalArgumentException.class, arena::returnToHubIfDefeated);
        assertEquals(MapCatalog.CROSSROADS, isolated.getCurrentMap().getId());
        assertFalse(isolated.getPlayer().isAlive());
        assertTrue(arena.getLastDefeat().isEmpty());
    }

    /** Wrong Hub type or a spawn that cannot fit the whole player must not heal or partially transition. */
    @Test
    void invalidHubTypeOrSpawnLeavesPlayerDeadInOriginalScope() {
        for (MapType type : MapType.values()) {
            GameMap hub = new GameMap(MapCatalog.HUB, "Invalid return", type, 100, 100, List.of(),
                    Map.of("entry", new SpawnPoint(type == MapType.HUB ? 90 : 0, 0)), "entry");
            GameSession invalid = new GameSession(List.of(session.getCurrentMap(), hub),
                    MapCatalog.CROSSROADS, 28, 28, 180);
            CombatDemo arena = new CombatDemo(invalid);
            invalid.getPlayer().takeDamage(100);
            CollisionBox before = invalid.getPlayer().getBounds();
            long stamp = invalid.getTransitionVersion();
            assertThrows(IllegalArgumentException.class, arena::returnToHubIfDefeated);
            assertEquals(MapCatalog.CROSSROADS, invalid.getCurrentMap().getId());
            assertEquals(before, invalid.getPlayer().getBounds());
            assertEquals(stamp, invalid.getTransitionVersion());
            assertFalse(invalid.getPlayer().isAlive());
        }
    }

    /** Invalid coordination dependencies and failure snapshot IDs are rejected explicitly. */
    @Test
    void validatesFailureSnapshotAndInjectedPipeline() {
        assertThrows(NullPointerException.class, () -> new CombatDemo(session, (CombatSystem) null));
        assertThrows(NullPointerException.class, () -> new CombatDemo.Defeat(null, 0));
        assertThrows(IllegalArgumentException.class, () -> new CombatDemo.Defeat(" ", 0));
    }

    /**
     * Returns the canonical current-scope player adapter.
     * @return registered player receiver
     */
    private Combatant playerActor() {
        return combat.getCombatants().stream().filter(actor -> actor.getFaction() == Faction.PLAYER)
                .findFirst().orElseThrow();
    }

    /** Enqueues a lethal hit through the real Boss source and damage pipeline without advancing time. */
    private void lethalAttack() {
        assertTrue(combat.tryAttack(demo.getBoss().orElseThrow().getCombatant(),
                new AttackSpec("lethal-fixture", 100, 1, 0), playerActor().getHurtbox()).isPresent());
    }

    /**
     * Builds one requested slot with simultaneous diagonal movement to expose accidental input replay.
     * @param slot requested action
     * @return complete shared frame command
     */
    private PlayerCommand command(AbilitySlot slot) {
        return new PlayerCommand(1, 1, slot == AbilitySlot.LIGHT, slot == AbilitySlot.SPECIAL,
                slot == AbilitySlot.DASH, slot == AbilitySlot.ULTIMATE);
    }
}
