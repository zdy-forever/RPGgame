package au.edu.anu.comp2120.screen;

import static org.junit.jupiter.api.Assertions.*;

import au.edu.anu.comp2120.ActiveEncounter;
import au.edu.anu.comp2120.BattleResult;
import au.edu.anu.comp2120.EncounterFlow;
import au.edu.anu.comp2120.GameSession;
import au.edu.anu.comp2120.TravelSystem;
import au.edu.anu.comp2120.enemy.BossChallenge;
import au.edu.anu.comp2120.npc.PortalGuide;
import au.edu.anu.comp2120.player.PlayerCommand;
import au.edu.anu.comp2120.quest.BossQuests;
import au.edu.anu.comp2120.world.MapCatalog;
import au.edu.anu.comp2120.world.MapType;
import au.edu.anu.comp2120.world.TravelCatalog;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Exercises quest acceptance, exact portal admission, distinct Boss completion and one-time ending.
 * Uses the production encounter/combat coordinator with no graphics or simulated Quest callbacks.
 * @author Daoyan Zhu (u7782042)
 */
final class BossQuestFlowTest {
    /** Fresh Hub and persistent player. */
    private final GameSession session = new GameSession(MapCatalog.createMaps(), MapCatalog.HUB, 28, 28, 180);
    /** Real sole-Hub-portal routing. */
    private final TravelSystem travel = new TravelSystem(session, TravelCatalog.createQuestPortals(), TravelCatalog.createDestinations());
    /** Existing gameplay outcome/reward boundary. */
    private final EncounterFlow flow = new EncounterFlow(session, travel, List.of(TravelCatalog.QUEST_PORTAL_ID));
    /** Actual combat scope coordinator with production admission rules. */
    private final CombatDemo demo = new CombatDemo(session, flow);
    /** The NPC-facing task model. */
    private final BossQuests quests = new BossQuests(session, flow);
    /** Destination guide that cannot create its own Battle permission. */
    private final PortalGuide guide = new PortalGuide(session, travel, flow);

    /** Creates a fresh isolated session per test. */
    BossQuestFlowTest() { }

    /** Battle stays unavailable without acceptance, including after selecting or returning from practice. */
    @Test
    void noQuestCannotEnterBattleButTrainingIsAvailable() {
        assertThrows(IllegalStateException.class, () -> guide.select(PortalGuide.Destination.BATTLE));
        assertFalse(travel.getPortal(TravelCatalog.QUEST_PORTAL_ID).enabled());
        assertTrue(guide.destinationLabel(PortalGuide.Destination.BATTLE).contains("LOCKED"));
        guide.select(PortalGuide.Destination.TRAINING);
        var practiceRoute = travel.getPortal(TravelCatalog.QUEST_PORTAL_ID);
        assertThrows(IllegalStateException.class, () -> guide.select(PortalGuide.Destination.BATTLE));
        assertEquals(practiceRoute, travel.getPortal(TravelCatalog.QUEST_PORTAL_ID));
        enter();
        assertTrue(demo.getTrainingDummy().isPresent());
        guide.returnFromTraining();
        demo.update(0, PlayerCommand.NONE, false);
        assertThrows(IllegalStateException.class, () -> guide.select(PortalGuide.Destination.BATTLE));
        assertTrue(quests.getCompleted().isEmpty());
    }

    /**
     * Every task selects its own arena and exactly one correctly identified Boss.
     * @param target chosen guardian
     */
    @ParameterizedTest
    @EnumSource(BossChallenge.class)
    void acceptedQuestControlsMapBossAndSuccess(BossChallenge target) {
        var player = session.getPlayer();
        quests.accept(target);
        var permission = flow.getActiveEncounter().orElseThrow();
        assertFalse(travel.getPortal(TravelCatalog.QUEST_PORTAL_ID).enabled());
        quests.accept(target);
        assertEquals(permission, flow.getActiveEncounter().orElseThrow());
        assertFalse(travel.getPortal(TravelCatalog.QUEST_PORTAL_ID).enabled());
        assertEquals(MapCatalog.HUB, session.getCurrentMap().getId());
        // Reading/opening the dialogue is not confirmation and must not enable travel.
        assertTrue(guide.destinationLabel(PortalGuide.Destination.BATTLE).contains(target.displayName()));
        var trigger = travel.getPortal(TravelCatalog.QUEST_PORTAL_ID).trigger();
        travel.updatePortals();
        player.enterArea(session.getCurrentMap(), trigger.x(), trigger.y());
        assertFalse(travel.updatePortals());
        demo.update(0, PlayerCommand.NONE, false);
        assertEquals(MapCatalog.HUB, session.getCurrentMap().getId());
        assertTrue(demo.getBoss().isEmpty());
        guide.select(PortalGuide.Destination.BATTLE);
        assertTrue(travel.getPortal(TravelCatalog.QUEST_PORTAL_ID).enabled());
        assertTrue(guide.destinationLabel(PortalGuide.Destination.BATTLE).contains(target.displayName()));
        enter();
        assertEquals(target.mapId(), session.getCurrentMap().getId());
        assertEquals(MapType.BATTLE, session.getCurrentMap().getType());
        assertEquals(target.id(), demo.getBoss().orElseThrow().getId());
        assertEquals(target.displayName(), demo.getBoss().orElseThrow().getName());
        assertEquals(2, demo.getCombatants().size());
        assertTrue(demo.getEnemies().isEmpty());
        win();
        assertSame(player, session.getPlayer());
        assertEquals(MapCatalog.HUB, session.getCurrentMap().getId());
        assertEquals(100, player.getHealth().getCurrentHp());
        assertEquals(java.util.Set.of(target), quests.getCompleted());
        assertTrue(quests.getActive().isEmpty());
        assertFalse(travel.getPortal(TravelCatalog.QUEST_PORTAL_ID).enabled());
        assertThrows(IllegalStateException.class, () -> guide.select(PortalGuide.Destination.BATTLE));
        assertThrows(IllegalStateException.class, () -> flow.activate(permission));
        assertEquals(1, flow.getPendingRewards().size());
        for (int i = 0; i < 10; i++) { quests.synchronize(); }
        assertEquals(1, quests.getCompleted().size());
        assertEquals(1, quests.getSuccessCount(target));
        assertFalse(quests.consumeEnding());
        quests.accept(target);
        assertNotEquals(permission, flow.getActiveEncounter().orElseThrow());
        assertFalse(travel.getPortal(TravelCatalog.QUEST_PORTAL_ID).enabled());
        guide.select(PortalGuide.Destination.BATTLE);
        enter();
        assertEquals(520, demo.getBoss().orElseThrow().getHealth().getMaxHp());
        assertEquals(520, demo.getBoss().orElseThrow().getHealth().getCurrentHp());
    }

    /** Repeat wins increase only that guardian and grant a fresh one-time reward, not another stone. */
    @Test void repeatedVictoriesGrowOneGuardianWithoutUnlockingOtherStones() {
        for (int wins = 0; wins < 5; wins++) {
            quests.accept(BossChallenge.FIRE);
            guide.select(PortalGuide.Destination.BATTLE);
            enter();
            int hp = au.edu.anu.comp2120.enemy.BossScaling.apply(GameConfig.SENTINEL, wins).maxHp();
            assertEquals(hp, demo.getBoss().orElseThrow().getHealth().getMaxHp());
            assertEquals(hp, demo.getBoss().orElseThrow().getHealth().getCurrentHp());
            win();
            var result = flow.getLastResult().orElseThrow();
            assertEquals(wins + 1, quests.getSuccessCount(BossChallenge.FIRE));
            for (int poll = 0; poll < 20; poll++) { quests.synchronize(); demo.returnToHubIfBattleEnded(); }
            assertEquals(wins + 1, quests.getSuccessCount(BossChallenge.FIRE));
            assertTrue(flow.claimReward(result).isPresent());
            assertTrue(flow.claimReward(result).isEmpty());
            assertEquals(java.util.Set.of(BossChallenge.FIRE), quests.getCompleted());
            assertEquals(0, quests.getSuccessCount(BossChallenge.WATER));
        }
        guide.select(PortalGuide.Destination.MEMORIAL);
        enter();
        assertFalse(quests.consumeEnding(), "Five wins of the same Boss must not unlock all five stones");
        session.switchMap(MapCatalog.HUB);
        demo.update(0, PlayerCommand.NONE, false);
        quests.accept(BossChallenge.WATER);
        guide.select(PortalGuide.Destination.BATTLE);
        enter();
        assertEquals(400, demo.getBoss().orElseThrow().getHealth().getMaxHp());
    }

    /** Failure/reacceptance/replacement never increases difficulty or compounds an existing scaled definition. */
    @Test void failedRematchesKeepSameDifficultyAndFreshEncounter() {
        quests.accept(BossChallenge.FIRE);
        guide.select(PortalGuide.Destination.BATTLE);
        enter();
        win();
        quests.accept(BossChallenge.FIRE);
        guide.select(PortalGuide.Destination.BATTLE);
        enter();
        for (int retry = 0; retry < 3; retry++) {
            var old = demo.getBoss().orElseThrow();
            var permission = flow.getActiveEncounter().orElseThrow();
            assertEquals(520, old.getHealth().getMaxHp());
            session.getPlayer().takeDamage(100);
            demo.update(0, PlayerCommand.NONE, false);
            quests.synchronize();
            assertEquals(1, quests.getSuccessCount(BossChallenge.FIRE));
            assertEquals(permission, flow.getActiveEncounter().orElseThrow());
            assertTrue(travel.getPortal(TravelCatalog.QUEST_PORTAL_ID).enabled());
            enter();
            var fresh = demo.getBoss().orElseThrow();
            assertNotSame(old, fresh);
            assertEquals(520, fresh.getHealth().getCurrentHp());
            assertEquals(1, fresh.getPhase());
        }
        session.getPlayer().takeDamage(100);
        demo.update(0, PlayerCommand.NONE, false);
        quests.accept(BossChallenge.WATER);
        quests.accept(BossChallenge.FIRE);
        assertEquals(1, quests.getSuccessCount(BossChallenge.FIRE));
        guide.select(PortalGuide.Destination.BATTLE);
        enter();
        assertEquals(520, demo.getBoss().orElseThrow().getHealth().getMaxHp());
    }

    /** Null count queries fail explicitly instead of inventing a default guardian. */
    @Test void nullSuccessQueriesAreRejected() {
        assertThrows(NullPointerException.class, () -> quests.getSuccessCount(null));
        assertThrows(NullPointerException.class, () -> flow.getSuccessCount(null));
    }

    /** Failure keeps acceptance retryable; replacing it in Hub routes only to the newly chosen Boss. */
    @Test
    void failureRetriesFreshBossAndCanReplaceQuest() {
        quests.accept(BossChallenge.FIRE);
        guide.select(PortalGuide.Destination.BATTLE);
        enter();
        var firstBoss = demo.getBoss().orElseThrow();
        var permission = flow.getActiveEncounter().orElseThrow();
        session.getPlayer().takeDamage(100);
        demo.update(0, PlayerCommand.NONE, false);
        quests.synchronize();
        assertEquals(BattleResult.Outcome.FAILURE, flow.getLastResult().orElseThrow().outcome());
        assertEquals(BossChallenge.FIRE, quests.getActive().orElseThrow());
        assertTrue(quests.getCompleted().isEmpty());
        guide.select(PortalGuide.Destination.BATTLE);
        assertEquals(permission, flow.getActiveEncounter().orElseThrow());
        enter();
        assertNotSame(firstBoss, demo.getBoss().orElseThrow());
        assertEquals(400, demo.getBoss().orElseThrow().getHealth().getCurrentHp());
        assertEquals(1, demo.getBoss().orElseThrow().getPhase());
        session.getPlayer().takeDamage(100);
        demo.update(0, PlayerCommand.NONE, false);
        quests.accept(BossChallenge.ICE);
        quests.synchronize();
        assertTrue(quests.getCompleted().isEmpty());
        assertNotEquals(permission, flow.getActiveEncounter().orElseThrow());
        assertFalse(travel.getPortal(TravelCatalog.QUEST_PORTAL_ID).enabled());
        guide.select(PortalGuide.Destination.BATTLE);
        enter();
        assertEquals(BossChallenge.ICE.id(), demo.getBoss().orElseThrow().getId());
    }

    /** Practice reroutes the shared portal temporarily but never deletes an accepted quest. */
    @Test
    void trainingPreservesAcceptedBattleTarget() {
        quests.accept(BossChallenge.LIGHTNING);
        var permission = flow.getActiveEncounter().orElseThrow();
        guide.select(PortalGuide.Destination.TRAINING);
        enter();
        assertFalse(flow.isBattleActive());
        assertTrue(demo.getBoss().isEmpty());
        assertEquals(BossChallenge.LIGHTNING, quests.getActive().orElseThrow());
        guide.returnFromTraining();
        demo.update(0, PlayerCommand.NONE, false);
        guide.select(PortalGuide.Destination.BATTLE);
        assertEquals(permission, flow.getActiveEncounter().orElseThrow());
        enter();
        assertEquals(MapCatalog.LIGHTNING_ARENA, session.getCurrentMap().getId());
    }

    /** Five successes unlock the ending, but only an explicit Hall Sage conversation consumes it. */
    @Test
    void completingFiveDifferentBossesTriggersEndingExactlyOnce() {
        int count = 0;
        for (var target : BossChallenge.values()) {
            quests.accept(target);
            assertFalse(travel.getPortal(TravelCatalog.QUEST_PORTAL_ID).enabled());
            guide.select(PortalGuide.Destination.BATTLE);
            enter();
            win();
            count++;
            assertEquals(count, quests.getCompleted().size());
            assertEquals(count, flow.getPendingRewards().size());
            if (count < BossChallenge.values().length) {
                assertFalse(quests.consumeEnding());
            }
        }
        assertFalse(quests.consumeEnding(), "No automatic ending on the fifth return to Hub");
        guide.select(PortalGuide.Destination.MEMORIAL);
        enter();
        assertEquals(MapCatalog.MEMORIAL_HALL, session.getCurrentMap().getId());
        session.getPlayer().takeDamage(100);
        assertFalse(quests.consumeEnding());
        session.getPlayer().getHealth().restoreFull();
        assertTrue(quests.consumeEnding());
        for (int i = 0; i < 20; i++) {
            quests.synchronize();
            assertFalse(quests.consumeEnding());
        }
        assertFalse(BossQuests.ENDING_LINES.isEmpty());
        for (var result : List.copyOf(flow.getPendingRewards().keySet())) {
            assertTrue(flow.claimReward(result).isPresent());
            assertTrue(flow.claimReward(result).isEmpty());
        }
        assertEquals(5, quests.getCompleted().size());
        session.switchMap(MapCatalog.HUB);
        demo.update(0, PlayerCommand.NONE, false);
        quests.accept(BossChallenge.FIRE);
        guide.select(PortalGuide.Destination.BATTLE);
        enter();
        win();
        assertEquals(2, quests.getSuccessCount(BossChallenge.FIRE));
        assertEquals(5, quests.getCompleted().size());
        guide.select(PortalGuide.Destination.MEMORIAL);
        enter();
        assertFalse(quests.consumeEnding(), "A rematch after the ending must not replay it");
    }

    /** Unrelated external acceptance results cannot mark the currently tracked task complete. */
    @Test
    void ignoresResultsFromAnotherAcceptance() {
        quests.accept(BossChallenge.FIRE);
        flow.activate(new ActiveEncounter("external", MapCatalog.CROSSROADS, "entry", TravelCatalog.QUEST_PORTAL_ID));
        enter();
        win();
        assertTrue(quests.getCompleted().isEmpty());
        assertEquals(BossChallenge.FIRE, quests.getActive().orElseThrow());
        assertFalse(quests.consumeEnding());
    }

    /** Invalid contexts do not alter progress or replace the running challenge. */
    @Test
    void rejectsDeadBattleAndNullAcceptance() {
        assertThrows(NullPointerException.class, () -> quests.accept(null));
        session.getPlayer().takeDamage(100);
        assertThrows(IllegalStateException.class, () -> quests.accept(BossChallenge.FIRE));
        session.getPlayer().getHealth().restoreFull();
        quests.accept(BossChallenge.WIND);
        guide.select(PortalGuide.Destination.BATTLE);
        enter();
        assertThrows(IllegalStateException.class, () -> quests.accept(BossChallenge.WATER));
        assertEquals(BossChallenge.WIND, quests.getActive().orElseThrow());
        assertThrows(NullPointerException.class, () -> new BossQuests(null, flow));
        assertThrows(NullPointerException.class, () -> new BossQuests(session, null));
    }

    /** Missing map configuration fails before an existing valid task or portal can be replaced. */
    @Test
    void invalidArenaDoesNotReplaceCurrentQuest() {
        GameSession partial = new GameSession(MapCatalog.createMaps().stream()
                .filter(map -> !map.getId().equals(MapCatalog.WATER_ARENA)).toList(), MapCatalog.HUB, 28, 28, 180);
        TravelSystem partialTravel = new TravelSystem(partial, TravelCatalog.createQuestPortals(), TravelCatalog.createDestinations());
        EncounterFlow partialFlow = new EncounterFlow(partial, partialTravel, List.of(TravelCatalog.QUEST_PORTAL_ID));
        BossQuests partialQuests = new BossQuests(partial, partialFlow);
        partialQuests.accept(BossChallenge.FIRE);
        var permission = partialFlow.getActiveEncounter().orElseThrow();
        partialFlow.activate(permission);
        assertThrows(IllegalArgumentException.class, () -> partialQuests.accept(BossChallenge.WATER));
        assertEquals(permission, partialFlow.getActiveEncounter().orElseThrow());
        assertTrue(partialTravel.getPortal(TravelCatalog.QUEST_PORTAL_ID).enabled());
        assertEquals(BossChallenge.FIRE, partialQuests.getActive().orElseThrow());
        assertThrows(IllegalArgumentException.class, () -> new BossQuests(session, partialFlow));
    }

    /**
     * Accepting another task closes any previously opened shared route until explicit confirmation.
     * @param oldDestination an opened practice or battle route
     */
    @ParameterizedTest
    @EnumSource(PortalGuide.Destination.class)
    void replacementClosesOldPortalUntilGuideConfirmation(PortalGuide.Destination oldDestination) {
        quests.accept(BossChallenge.FIRE);
        guide.select(oldDestination);
        assertTrue(travel.getPortal(TravelCatalog.QUEST_PORTAL_ID).enabled());
        quests.accept(BossChallenge.WATER);
        assertFalse(travel.getPortal(TravelCatalog.QUEST_PORTAL_ID).enabled());
        assertEquals(BossChallenge.WATER, quests.getActive().orElseThrow());
        assertEquals(MapCatalog.WATER_ARENA, flow.getActiveEncounter().orElseThrow().mapId());
        assertEquals(MapCatalog.HUB, session.getCurrentMap().getId());
        guide.select(PortalGuide.Destination.BATTLE);
        enter();
        assertEquals(BossChallenge.WATER.id(), demo.getBoss().orElseThrow().getId());
    }

    /** Re-accepting the current task keeps progress identity but does not silently reopen its portal. */
    @Test
    void reacceptingSameTaskAlsoRequiresGuideConfirmation() {
        quests.accept(BossChallenge.ICE);
        guide.select(PortalGuide.Destination.BATTLE);
        var permission = flow.getActiveEncounter().orElseThrow();
        quests.accept(BossChallenge.ICE);
        assertEquals(permission, flow.getActiveEncounter().orElseThrow());
        assertFalse(travel.getPortal(TravelCatalog.QUEST_PORTAL_ID).enabled());
        guide.select(PortalGuide.Destination.BATTLE);
        assertTrue(travel.getPortal(TravelCatalog.QUEST_PORTAL_ID).enabled());
    }

    /** Crosses the real configured portal and synchronizes the new combat scope. */
    private void enter() {
        session.getPlayer().enterArea(session.getCurrentMap(), 64, 64);
        travel.updatePortals();
        var trigger = travel.getPortal(TravelCatalog.QUEST_PORTAL_ID).trigger();
        session.getPlayer().enterArea(session.getCurrentMap(), trigger.x(), trigger.y());
        assertTrue(travel.updatePortals());
        demo.update(0, PlayerCommand.NONE, false);
    }

    /** Supplies lethal HP for outcome orchestration, then settles and polls through production APIs. */
    private void win() {
        var boss = demo.getBoss().orElseThrow();
        boss.getHealth().takeDamage(boss.getHealth().getMaxHp());
        demo.update(0, PlayerCommand.NONE, false);
        quests.synchronize();
    }
}
