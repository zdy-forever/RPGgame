package au.edu.anu.comp2120.screen;

import static org.junit.jupiter.api.Assertions.*;

import au.edu.anu.comp2120.ActiveEncounter;
import au.edu.anu.comp2120.BattleResult;
import au.edu.anu.comp2120.EncounterFlow;
import au.edu.anu.comp2120.GameSession;
import au.edu.anu.comp2120.TravelSystem;
import au.edu.anu.comp2120.combat.Attack;
import au.edu.anu.comp2120.combat.AttackSpec;
import au.edu.anu.comp2120.combat.ElementType;
import au.edu.anu.comp2120.combat.CombatSystem;
import au.edu.anu.comp2120.enemy.Boss;
import au.edu.anu.comp2120.enemy.BossState;
import au.edu.anu.comp2120.player.AbilityLoadout;
import au.edu.anu.comp2120.player.AbilitySlot;
import au.edu.anu.comp2120.player.AbilityState;
import au.edu.anu.comp2120.player.ClassDefinition;
import au.edu.anu.comp2120.player.PlayerClass;
import au.edu.anu.comp2120.player.PlayerClasses;
import au.edu.anu.comp2120.player.PlayerCommand;
import au.edu.anu.comp2120.player.UltimateMode;
import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.MapCatalog;
import au.edu.anu.comp2120.world.Portal;
import au.edu.anu.comp2120.world.TravelCatalog;
import au.edu.anu.comp2120.world.GameMap;
import au.edu.anu.comp2120.world.MapType;
import au.edu.anu.comp2120.world.SpawnPoint;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Exercises the production admission/outcome boundary through real travel and shared combat.
 * No Quest, NPC, Inventory implementation or graphics context is substituted into the loop.
 * @author Daoyan Zhu (u7782042)
 */
final class QuestPortalFlowTest {
    /** Persistent player/map fixture, initially at Hub entry outside task portals. */
    private final GameSession session = new GameSession(MapCatalog.createMaps(), MapCatalog.HUB, 28, 28, 180);
    /** Actual production route catalogue, without manual battle return portals. */
    private final List<Portal> portals = TravelCatalog.createQuestPortals();
    /** Existing travel execution, not a test replacement. */
    private final TravelSystem travel = new TravelSystem(session, portals, TravelCatalog.createDestinations());
    /** External acceptance and retained result boundary. */
    private final EncounterFlow flow = new EncounterFlow(session, travel, portals.stream().map(Portal::id).toList());
    /** Production pipeline retained only to arrange and inspect elemental settlement in integration tests. */
    private final CombatSystem combat = new CombatSystem();
    /** Real shared combat coordinator wired exactly as the production screen. */
    private final CombatDemo demo = new CombatDemo(session, combat, flow);

    /** Creates an independent production-flow fixture. */
    QuestPortalFlowTest() {
    }

    /**
     * Every real class/mode survives task success/failure; debug mode requests in Battle are rejected.
     * Active buffs, casts and defensive states do not survive encounter cleanup.
     * @param type selected profession
     * @param mode remembered ultimate mode
     * @param success whether the Boss or player is defeated
     */
    @ParameterizedTest
    @CsvSource({"ARCHER,A,true", "ARCHER,A,false", "ARCHER,B,true", "ARCHER,B,false",
            "MAGE,A,true", "MAGE,A,false", "MAGE,B,true", "MAGE,B,false",
            "KATANA,A,true", "KATANA,A,false", "KATANA,B,true", "KATANA,B,false"})
    void finalModesSurviveActualEncounterReturns(PlayerClass type, UltimateMode mode, boolean success) {
        demo.update(0, PlayerCommand.NONE, false, type);
        if (mode == UltimateMode.B) {
            demo.update(0, PlayerCommand.NONE, false, null, false, true);
        }
        assertEquals(mode, session.getPlayer().getUltimateMode());
        var player = session.getPlayer();
        var selected = player.getClassDefinition();
        enter(permission("final-mode", MapCatalog.CROSSROADS, "hub-crossroads"));
        Boss old = boss();
        demo.update(0, PlayerCommand.NONE, false, null, false, true);
        assertEquals(mode, player.getUltimateMode());
        demo.update(0, new PlayerCommand(0, 0, false, false, false, true), false);
        String expected = switch (type) {
            case ARCHER -> mode == UltimateMode.A ? "FIELD" : "BUFF";
            case MAGE -> mode == UltimateMode.A ? "CAST" : "BUFF";
            case KATANA -> mode == UltimateMode.A ? "STANCE" : "THRUST";
        };
        assertEquals(expected, demo.getAbilities().getSkillStatus());
        if (success) {
            old.getHealth().takeDamage(400);
        } else {
            player.takeDamage(100);
        }
        demo.update(0, false, false);
        assertHubRecovered();
        assertSame(player, session.getPlayer());
        assertSame(selected, player.getClassDefinition());
        assertEquals(mode, player.getUltimateMode());
        assertEquals("NONE", demo.getAbilities().getSkillStatus());
        assertTrue(demo.getCombatants().get(0).isTargetable());
        assertTrue(demo.getActiveAttacks().isEmpty());
        if (!success) {
            enterPortal("hub-crossroads");
            assertNotSame(old, boss());
            assertEquals(mode, player.getUltimateMode());
            assertEquals(type, player.getPlayerClass());
        }
    }

    /** Closed task portals cannot create a battle, including the formerly enabled demo routes. */
    @Test
    void noAcceptanceMeansNoEntry() {
        assertTrue(flow.getActiveEncounter().isEmpty());
        assertTrue(flow.getLastResult().isEmpty());
        assertTrue(flow.getPendingRewards().isEmpty());
        for (Portal portal : portals) {
            assertFalse(travel.getPortal(portal.id()).enabled());
            session.getPlayer().enterArea(session.getCurrentMap(), portal.trigger().x(), portal.trigger().y());
            assertFalse(travel.updatePortals());
            assertEquals(MapCatalog.HUB, session.getCurrentMap().getId());
        }
        assertTrue(demo.getBoss().isEmpty());
    }

    /** Permission enables only its route, uses the requested spawn and creates exactly one fresh Boss. */
    @Test
    void admissionRoutesToConfiguredMapAndSpawn() {
        ActiveEncounter permission = new ActiveEncounter("accept-1", MapCatalog.CROSSROADS, "east", "hub-training");
        enter(permission);
        assertEquals(MapCatalog.CROSSROADS, session.getCurrentMap().getId());
        assertEquals(new CollisionBox(1080, 80, 28, 28), session.getPlayer().getBounds());
        assertTrue(flow.isBattleActive());
        assertEquals(2, demo.getCombatants().size());
        assertTrue(demo.getEnemies().isEmpty());
        assertEquals(400, boss().getHealth().getCurrentHp());
        assertTrue(travel.getPortals(MapCatalog.CROSSROADS).isEmpty());
        assertFalse(travel.getPortal("hub-crossroads").enabled());
        assertFalse(travel.getPortal("hub-disabled").enabled());
    }

    /** Failure restores Hub health, retains permission and rebuilds Boss HP/phase/state for the retry. */
    @Test
    void failureAllowsFreshRetryWithoutAcceptance() {
        ActiveEncounter permission = permission("retry", MapCatalog.CROSSROADS, "hub-crossroads");
        enter(permission);
        Boss old = boss();
        old.getHealth().takeDamage(230);
        demo.update(0, false, false);
        assertEquals(2, old.getPhase());
        session.getPlayer().takeDamage(100);
        demo.update(0.1, new PlayerCommand(1, 1, true, true, true, true), true, PlayerClass.ARCHER, true);
        BattleResult result = flow.getLastResult().orElseThrow();
        assertEquals(BattleResult.Outcome.FAILURE, result.outcome());
        assertEquals(permission, result.encounter());
        assertHubRecovered();
        assertEquals(permission, flow.getActiveEncounter().orElseThrow());
        assertTrue(travel.getPortal(permission.portalId()).enabled());
        assertTrue(flow.claimReward(result).isEmpty());
        assertTrue(old.claimDrop().isEmpty());
        long returned = session.getTransitionVersion();
        for (int i = 0; i < 5; i++) {
            assertFalse(demo.returnToHubIfBattleEnded());
            assertFalse(travel.updatePortals());
        }
        assertEquals(returned, session.getTransitionVersion());
        enterPortal(permission.portalId());
        Boss fresh = boss();
        assertNotSame(old, fresh);
        assertEquals(400, fresh.getHealth().getCurrentHp());
        assertEquals(1, fresh.getPhase());
        assertEquals(BossState.IDLE, fresh.getState());
        assertTrue(demo.getActiveAttacks().isEmpty());
        session.getPlayer().takeDamage(100);
        demo.update(0, false, false);
        assertNotEquals(result.encounterVersion(), flow.getLastResult().orElseThrow().encounterVersion());
    }

    /** A living Hub player can replace a failed challenge without restarting or touching Quest state. */
    @Test
    void failureCanReplaceTaskAndDisableOldPortal() {
        enter(permission("first", MapCatalog.TRAINING, "hub-training"));
        session.getPlayer().takeDamage(100);
        demo.update(0, false, false);
        ActiveEncounter replacement = permission("easier", MapCatalog.CROSSROADS, "hub-crossroads");
        flow.activate(replacement);
        assertFalse(travel.getPortal("hub-training").enabled());
        assertTrue(travel.getPortal("hub-crossroads").enabled());
        enterPortal(replacement.portalId());
        assertEquals(MapCatalog.CROSSROADS, session.getCurrentMap().getId());
        assertTrue(flow.isBattleActive());
    }

    /** Success survives map cleanup, closes admission and transfers exactly one immutable material payload. */
    @Test
    void successReturnsClosesAndRetainsOneReward() {
        ActiveEncounter permission = permission("success", MapCatalog.CROSSROADS, "hub-crossroads");
        enter(permission);
        Boss old = boss();
        session.getPlayer().takeDamage(20);
        old.getHealth().takeDamage(400);
        demo.update(0, new PlayerCommand(1, 0, true, true, true, true), true);
        assertHubRecovered();
        BattleResult result = flow.getLastResult().orElseThrow();
        assertEquals(BattleResult.Outcome.SUCCESS, result.outcome());
        assertTrue(flow.getActiveEncounter().isEmpty());
        assertFalse(travel.getPortal(permission.portalId()).enabled());
        assertEquals(1, flow.getPendingRewards().size());
        var reward = flow.getPendingRewards().get(result);
        assertEquals("sentinel-core", reward.materialId());
        assertEquals(1, reward.quantity());
        assertEquals(MapCatalog.CROSSROADS, reward.mapId());
        assertTrue(old.getDrop().isEmpty());
        assertTrue(old.claimDrop().isEmpty());
        long returned = session.getTransitionVersion();
        for (int i = 0; i < 10; i++) {
            demo.update(0, false, false);
            assertFalse(travel.updatePortals());
            assertEquals(result, flow.getLastResult().orElseThrow());
        }
        assertEquals(returned, session.getTransitionVersion());
        assertEquals(reward, flow.claimReward(result).orElseThrow());
        assertTrue(flow.claimReward(result).isEmpty());
        assertTrue(flow.getPendingRewards().isEmpty());
        assertThrows(IllegalStateException.class, () -> flow.activate(permission));
        session.getPlayer().enterArea(session.getCurrentMap(), 160, 400);
        assertFalse(travel.updatePortals());
        assertEquals(MapCatalog.HUB, session.getCurrentMap().getId());
        session.getPlayer().enterArea(session.getCurrentMap(), 64, 64);
        enter(permission("new-acceptance", MapCatalog.CROSSROADS, "hub-crossroads"));
        assertNotSame(old, boss());
    }

    /** An actual lethal J cast settles through combat-core before the success return and cleanup. */
    @Test
    void sharedCombatKillCompletesInSameFrame() {
        assertTrue(demo.getAbilities().selectClass(PlayerClasses.get(PlayerClass.KATANA)));
        enter(permission("melee-kill", MapCatalog.CROSSROADS, "hub-crossroads"));
        session.getPlayer().enterArea(session.getCurrentMap(), 320, 100);
        boss().getHealth().takeDamage(399);
        demo.update(0, new PlayerCommand(1, 0, true, false, false, false), false);
        assertEquals(BattleResult.Outcome.SUCCESS, flow.getLastResult().orElseThrow().outcome());
        assertHubRecovered();
        assertTrue(demo.getActiveAttacks().isEmpty());
    }

    /** Settled simultaneous death follows the existing failure-first rule and cannot grant material. */
    @Test
    void simultaneousDeathIsFailureWithoutReward() {
        enter(permission("both-dead", MapCatalog.CROSSROADS, "hub-crossroads"));
        Boss old = boss();
        old.getHealth().takeDamage(400);
        session.getPlayer().takeDamage(100);
        demo.update(0, false, false);
        assertEquals(BattleResult.Outcome.FAILURE, flow.getLastResult().orElseThrow().outcome());
        assertTrue(flow.getPendingRewards().isEmpty());
        assertTrue(old.claimDrop().isEmpty());
        assertHubRecovered();
    }

    /** Unclaimed rewards are not overwritten by a later accepted and completed challenge. */
    @Test
    void rewardOutboxSurvivesFurtherAcceptances() {
        enter(permission("one", MapCatalog.CROSSROADS, "hub-crossroads"));
        boss().getHealth().takeDamage(400);
        demo.update(0, false, false);
        BattleResult first = flow.getLastResult().orElseThrow();
        enter(permission("two", MapCatalog.TRAINING, "hub-training"));
        boss().getHealth().takeDamage(400);
        demo.update(0, false, false);
        BattleResult second = flow.getLastResult().orElseThrow();
        assertEquals(2, flow.getPendingRewards().size());
        // Deliberately attempt mutation to verify the external reward view rejects it.
        //noinspection DataFlowIssue
        assertThrows(UnsupportedOperationException.class, () -> flow.getPendingRewards().clear());
        assertNotEquals(flow.claimReward(first).orElseThrow().id(), flow.claimReward(second).orElseThrow().id());
        assertTrue(flow.claimReward(first).isEmpty());
        assertTrue(flow.claimReward(second).isEmpty());
    }

    /** Fast travel preserves its own unlock rules but cannot admit a task Boss or reuse a stale portal stamp. */
    @Test
    void fastTravelIsIndependentAndCannotBypassAdmission() {
        assertThrows(IllegalStateException.class, () -> travel.fastTravel(TravelCatalog.TRAINING_ENTRY));
        flow.activate(permission("fast-travel", MapCatalog.TRAINING, "hub-training"));
        travel.unlockDestination(TravelCatalog.TRAINING_ENTRY);
        travel.fastTravel(TravelCatalog.TRAINING_ENTRY);
        demo.update(0, false, false);
        assertFalse(flow.isBattleActive());
        assertTrue(demo.getBoss().isEmpty());
        travel.fastTravel(TravelCatalog.HUB_ENTRY);
        demo.update(0, false, false);
        enterPortal("hub-training");
        assertTrue(demo.getBoss().isPresent());
        travel.fastTravel(TravelCatalog.TRAINING_ENTRY);
        demo.update(0, false, false);
        assertFalse(flow.isBattleActive());
        assertTrue(demo.getBoss().isEmpty());
        assertTrue(travel.getDestination(TravelCatalog.TRAINING_ENTRY).unlocked());
        session.switchMap(MapCatalog.TRAINING);
        demo.update(0, false, false);
        assertTrue(demo.getBoss().isEmpty());
    }

    /**
     * Runtime class/O selection is Hub-only and exact selected data survives either outcome.
     * @param outcome success or failure return to exercise
     */
    @ParameterizedTest
    @EnumSource(BattleResult.Outcome.class)
    void identitiesAndSelectedUltimateSurviveBothOutcomes(BattleResult.Outcome outcome) {
        ClassDefinition base = PlayerClasses.get(PlayerClass.ARCHER);
        AbilityLoadout slots = base.abilities();
        var customO = new AbilityLoadout.AreaAttack(new AttackSpec("selected-o", 12, 0.2, 3, ElementType.ICE), 60);
        ClassDefinition selected = new ClassDefinition(base.type(),
                new AbilityLoadout(slots.light(), slots.special(), slots.dash(), customO));
        assertTrue(demo.getAbilities().selectClass(selected));
        var player = session.getPlayer();
        var health = player.getHealth();
        var abilities = demo.getAbilities();
        enter(permission("identity", MapCatalog.CROSSROADS, "hub-crossroads"));
        assertFalse(abilities.selectClass(base));
        demo.update(0, PlayerCommand.NONE, false, PlayerClass.KATANA, true);
        assertSame(selected, player.getClassDefinition());
        if (outcome == BattleResult.Outcome.SUCCESS) {
            boss().getHealth().takeDamage(400);
        } else {
            health.takeDamage(100);
        }
        demo.update(0, false, false);
        assertSame(player, session.getPlayer());
        assertSame(health, player.getHealth());
        assertSame(abilities, demo.getAbilities());
        assertSame(selected, player.getClassDefinition());
        assertSame(customO, player.getClassDefinition().abilities().ultimate());
        assertEquals(PlayerClass.ARCHER, player.getPlayerClass());
        assertTrue(abilities.selectClass(base));
    }

    /**
     * Both outcomes cancel live projectile/dash references and detach the former Boss.
     * @param success whether the Boss, rather than the player, is defeated
     */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void outcomeCleansProjectilesDashAndOldBoss(boolean success) {
        enter(permission("cleanup", MapCatalog.CROSSROADS, "hub-crossroads"));
        Boss old = boss();
        demo.update(0, new PlayerCommand(0, 0, true, false, false, false), false);
        List<Attack> attacks = demo.getActiveAttacks();
        assertTrue(attacks.stream().anyMatch(attack -> attack.getProjectile() != null));
        demo.update(0, new PlayerCommand(0, 1, false, false, true, false), false);
        assertEquals(AbilityState.ACTIVE, demo.getAbilities().getState(AbilitySlot.DASH));
        if (success) {
            old.getHealth().takeDamage(400);
        } else {
            session.getPlayer().takeDamage(100);
        }
        demo.update(0, false, false);
        assertHubRecovered();
        assertTrue(attacks.stream().noneMatch(Attack::isActive));
        assertTrue(demo.getActiveAttacks().isEmpty());
        assertNotEquals(AbilityState.ACTIVE, demo.getAbilities().getState(AbilitySlot.DASH));
        CollisionBox oldPosition = old.getBounds();
        old.update(demo.getCombatants().get(0), 1);
        assertEquals(oldPosition, old.getBounds());
        assertTrue(old.claimDrop().isEmpty());
    }

    /** Rejected replacements are atomic and mid-battle debug reset cannot restart an admitted Boss. */
    @Test
    void invalidReplacementAndBattleMutationAreRejected() {
        ActiveEncounter original = permission("valid", MapCatalog.TRAINING, "hub-training");
        flow.activate(original);
        for (ActiveEncounter invalid : List.of(
                permission("bad-map", "missing", "hub-crossroads"),
                permission("hub-target", MapCatalog.HUB, "hub-crossroads"),
                new ActiveEncounter("bad-spawn", MapCatalog.CROSSROADS, "missing", "hub-crossroads"),
                permission("bad-portal", MapCatalog.TRAINING, "missing"))) {
            assertThrows(IllegalArgumentException.class, () -> flow.activate(invalid));
            assertEquals(original, flow.getActiveEncounter().orElseThrow());
            assertTrue(travel.getPortal("hub-training").enabled());
            assertFalse(travel.getPortal("hub-crossroads").enabled());
        }
        enterPortal("hub-training");
        Boss old = boss();
        assertThrows(IllegalStateException.class, () -> flow.activate(original));
        assertThrows(IllegalStateException.class, flow::clearActiveEncounter);
        demo.update(0, false, true);
        assertSame(old, boss());
        assertTrue(flow.getLastResult().isEmpty());
    }

    /** Clearing admission in Hub is idempotent and never grants travel or a reward. */
    @Test
    void clearPermissionAndValidateIdentifiers() {
        flow.activate(permission("clear", MapCatalog.TRAINING, "hub-training"));
        flow.clearActiveEncounter();
        flow.clearActiveEncounter();
        assertTrue(flow.getActiveEncounter().isEmpty());
        assertFalse(travel.getPortal("hub-training").enabled());
        assertThrows(NullPointerException.class, () -> flow.activate(null));
        assertThrows(NullPointerException.class, () -> flow.claimReward(null));
        assertThrows(IllegalStateException.class, () -> flow.complete(BattleResult.Outcome.FAILURE, null));
        assertThrows(IllegalArgumentException.class, () -> permission(" ", MapCatalog.TRAINING, "hub-training"));
        assertThrows(NullPointerException.class, () -> permission(null, MapCatalog.TRAINING, "hub-training"));
    }

    /**
     * Real elemental attachments and a queued reaction child cannot survive either return path.
     * @param outcome battle settlement to exercise
     */
    @ParameterizedTest
    @EnumSource(BattleResult.Outcome.class)
    void elementalAndDerivedScopeIsCleared(BattleResult.Outcome outcome) {
        enter(permission("element-cleanup", MapCatalog.CROSSROADS, "hub-crossroads"));
        var receiver = demo.getCombatants().get(0);
        CollisionBox effect = receiver.getHurtbox();
        for (ElementType element : List.of(ElementType.WATER, ElementType.LIGHTNING, ElementType.WATER)) {
            combat.tryAttack(boss().getCombatant(), new AttackSpec("test-element", 1, 1, 0, element),
                    effect, effect).orElseThrow();
            combat.update(0);
        }
        List<Attack> oldAttacks = combat.getActiveAttacks();
        assertTrue(oldAttacks.stream().anyMatch(Attack::isDerived));
        assertEquals(ElementType.WATER, combat.getElementStatus(receiver).element());
        if (outcome == BattleResult.Outcome.SUCCESS) {
            boss().getHealth().takeDamage(400);
        } else {
            session.getPlayer().takeDamage(100);
        }
        demo.update(0, false, false);
        assertHubRecovered();
        assertTrue(oldAttacks.stream().noneMatch(Attack::isActive));
        assertTrue(combat.getActiveAttacks().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> combat.getElementStatus(receiver));
        assertEquals(ElementType.NONE, combat.getElementStatus(demo.getCombatants().get(0)).element());
        demo.update(1, false, false);
        assertEquals(100, session.getPlayer().getHealth().getCurrentHp());
        assertTrue(combat.getActiveAttacks().isEmpty());
    }

    /** Invalid completion data and foreign-session wiring cannot mutate an admitted scope. */
    @Test
    void rejectsInvalidSettlementAndWiring() {
        GameSession other = new GameSession(MapCatalog.createMaps(), MapCatalog.HUB, 28, 28, 180);
        assertThrows(IllegalArgumentException.class,
                () -> new EncounterFlow(other, travel, List.of("hub-training")));
        assertThrows(IllegalArgumentException.class, () -> new CombatDemo(other, flow));
        assertThrows(NullPointerException.class, () -> new CombatDemo(session, (EncounterFlow) null));
        enter(permission("invalid-settlement", MapCatalog.TRAINING, "hub-training"));
        long version = session.getTransitionVersion();
        assertThrows(IllegalArgumentException.class, () -> flow.complete(BattleResult.Outcome.SUCCESS, null));
        assertThrows(NullPointerException.class, () -> flow.complete(null, null));
        assertEquals(version, session.getTransitionVersion());
        assertTrue(flow.isBattleActive());
        assertTrue(flow.getLastResult().isEmpty());
        assertTrue(flow.getPendingRewards().isEmpty());
    }

    /** A rejected body-size spawn cannot disable or retarget the previously valid portal. */
    @Test
    void blockedFullBodySpawnLeavesExistingRouteIntact() {
        GameMap hub = session.getMap(MapCatalog.HUB);
        GameMap narrow = new GameMap("narrow", "Narrow", MapType.BATTLE, 100, 100, List.of(),
                Map.of("entry", new SpawnPoint(95, 95)), "entry");
        GameSession custom = new GameSession(List.of(hub, narrow), MapCatalog.HUB, 28, 28, 180);
        Portal definition = new Portal("task", MapCatalog.HUB, new CollisionBox(160, 64, 48, 64),
                MapCatalog.HUB, "entry", false);
        TravelSystem routes = new TravelSystem(custom, List.of(definition), List.of());
        EncounterFlow boundary = new EncounterFlow(custom, routes, List.of("task"));
        assertThrows(IllegalArgumentException.class,
                () -> boundary.activate(permission("bad-fit", "narrow", "task")));
        assertEquals(definition, routes.getPortal("task"));
        assertTrue(boundary.getActiveEncounter().isEmpty());
    }

    /**
     * Failed Hub validation does not consume a dead Boss's reward or publish a premature success.
     * @param invalidType whether Hub has the wrong map type rather than a blocked full-body spawn
     */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void invalidHubReturnRetainsBossReward(boolean invalidType) {
        GameMap lobby = new GameMap("lobby", "Lobby", MapType.HUB, 300, 300, List.of(),
                Map.of("entry", new SpawnPoint(0, 0)), "entry");
        GameMap brokenHub = new GameMap(MapCatalog.HUB, "Bad Hub", invalidType ? MapType.BATTLE : MapType.HUB,
                100, 100, List.of(), Map.of("entry", new SpawnPoint(95, 95)), "entry");
        GameSession custom = new GameSession(List.of(lobby, brokenHub, session.getMap(MapCatalog.CROSSROADS)),
                "lobby", 28, 28, 180);
        Portal portal = new Portal("task", "lobby", new CollisionBox(100, 100, 40, 40),
                MapCatalog.CROSSROADS, "entry", false);
        TravelSystem routes = new TravelSystem(custom, List.of(portal), List.of());
        EncounterFlow boundary = new EncounterFlow(custom, routes, List.of("task"));
        CombatDemo coordinator = new CombatDemo(custom, boundary);
        boundary.activate(permission("invalid-hub", MapCatalog.CROSSROADS, "task"));
        custom.getPlayer().enterArea(lobby, 100, 100);
        assertTrue(routes.updatePortals());
        coordinator.update(0, false, false);
        Boss old = coordinator.getBoss().orElseThrow();
        old.getHealth().takeDamage(400);
        long scope = custom.getTransitionVersion();
        assertThrows(IllegalArgumentException.class, coordinator::returnToHubIfBattleEnded);
        var material = old.getDrop().orElseThrow();
        assertThrows(IllegalArgumentException.class, coordinator::returnToHubIfBattleEnded);
        assertEquals(material, old.getDrop().orElseThrow());
        assertEquals(scope, custom.getTransitionVersion());
        assertTrue(boundary.isBattleActive());
        assertTrue(boundary.getLastResult().isEmpty());
        assertTrue(boundary.getPendingRewards().isEmpty());
        assertTrue(routes.getPortal("task").enabled());
    }

    /**
     * Builds one external acceptance request without implementing Quest rules.
     * @param id unique acceptance ID
     * @param map destination map
     * @param portal entry portal
     * @return immutable permission using the standard entry spawn
     */
    private ActiveEncounter permission(String id, String map, String portal) {
        return new ActiveEncounter(id, map, "entry", portal);
    }

    /**
     * Accepts in Hub, then uses the actual portal trigger and normal scope synchronization.
     * @param permission externally supplied permission
     */
    private void enter(ActiveEncounter permission) {
        flow.activate(permission);
        enterPortal(permission.portalId());
    }

    /**
     * Observes departure from arrival regions before entering the chosen trigger.
     * @param id enabled Hub portal
     */
    private void enterPortal(String id) {
        travel.updatePortals();
        CollisionBox trigger = travel.getPortal(id).trigger();
        session.getPlayer().enterArea(session.getCurrentMap(), trigger.x(), trigger.y());
        assertTrue(travel.updatePortals());
        demo.update(0, PlayerCommand.NONE, false);
    }

    /**
     * Reads the one currently admitted Boss.
     * @return current encounter
     */
    private Boss boss() {
        return demo.getBoss().orElseThrow();
    }

    /** Checks the shared post-outcome invariants without consuming result or reward snapshots. */
    private void assertHubRecovered() {
        assertEquals(MapCatalog.HUB, session.getCurrentMap().getId());
        assertEquals(100, session.getPlayer().getHealth().getCurrentHp());
        assertEquals(new CollisionBox(64, 64, 28, 28), session.getPlayer().getBounds());
        assertEquals(1, demo.getCombatants().size());
        assertTrue(demo.getBoss().isEmpty());
        assertTrue(demo.getDrops().isEmpty());
        assertTrue(demo.getEnemies().isEmpty());
    }
}
