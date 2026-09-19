package au.edu.anu.comp2120.screen;

import au.edu.anu.comp2120.EncounterFlow;
import au.edu.anu.comp2120.GameSession;
import au.edu.anu.comp2120.TravelSystem;
import au.edu.anu.comp2120.enemy.BossChallenge;
import au.edu.anu.comp2120.npc.PortalGuide;
import au.edu.anu.comp2120.player.PlayerClass;
import au.edu.anu.comp2120.player.PlayerCommand;
import au.edu.anu.comp2120.player.UltimateMode;
import au.edu.anu.comp2120.quest.BossQuests;
import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.MapCatalog;
import au.edu.anu.comp2120.world.MapType;
import au.edu.anu.comp2120.world.TravelCatalog;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises the production Hall route, guardian-backed stones and explicit ending conversation.
 * No graphics, synthetic rewards or duplicate combat/quest models are involved.
 * @author Daoyan Zhu (u7782042)
 */
final class MemorialHallTest {
    /** Persistent player and all production map definitions. */
    private final GameSession session = new GameSession(MapCatalog.createMaps(), MapCatalog.HUB, 28, 28, 180);
    /** Actual production route set, including the independent Hall return. */
    private final TravelSystem travel = new TravelSystem(session, TravelCatalog.createGameplayPortals(),
            TravelCatalog.createDestinations());
    /** Owns only the Hub task gate, never the Memorial Hall exit. */
    private final EncounterFlow flow = new EncounterFlow(session, travel, List.of(TravelCatalog.QUEST_PORTAL_ID));
    /** Production combat lifecycle coordinator. */
    private final CombatDemo demo = new CombatDemo(session, flow);
    /** Single source of guardian completion and ending progress. */
    private final BossQuests quests = new BossQuests(session, flow);
    /** Same route-selection adapter used by the NPC dialogue. */
    private final PortalGuide guide = new PortalGuide(session, travel, flow);

    /** Creates fresh session-local progress for every test. */
    MemorialHallTest() { }

    /** Hub and Hall share the Sage's display identity, but not scripts, placement or ending routing ID. */
    @Test void sageHasSeparateBeginnerAndMemorialRoles() {
        var hubSage = GameConfig.HUB_GUIDE;
        var hallSage = GameConfig.GUIDE;
        assertEquals("Old Sage", hubSage.getName());
        assertEquals(hubSage.getName(), hallSage.getName());
        assertNotSame(hubSage, hallSage);
        assertNotEquals(hubSage.getId(), hallSage.getId());
        assertTrue(GameConfig.HUB_NPCS.contains(hubSage));
        assertEquals(184, hubSage.getX());
        assertEquals(221, hubSage.getY());
        assertFalse(GameConfig.HUB_NPCS.contains(hallSage));
        assertEquals(1, GameConfig.HUB_NPCS.stream().filter(npc -> npc.getName().equals("Old Sage")).count());
        assertTrue(session.getCurrentMap().isWalkable(new CollisionBox(hubSage.getX(), hubSage.getY(),
                hubSage.getWidth(), hubSage.getHeight())));
        for (var other : GameConfig.HUB_NPCS) {
            if (other == hubSage) { continue; }
            assertFalse(new CollisionBox(hubSage.getX(), hubSage.getY(), hubSage.getWidth(), hubSage.getHeight())
                    .overlaps(new CollisionBox(other.getX(), other.getY(), other.getWidth(), other.getHeight())),
                    "Sage must not overlap another NPC: " + other.getName());
        }
        assertNull(hubSage.getRewardItemId());
        assertNotEquals(hubSage.getLines(), hallSage.getLines());
        String tutorial = String.join("\n", hubSage.getLines());
        assertTrue(tutorial.contains("WASD"));
        assertTrue(tutorial.contains("Class Master"));
        assertTrue(tutorial.contains("Quest Keeper FIRST"));
        assertTrue(tutorial.contains("Accepting a task alone does not open it"));
        assertTrue(tutorial.contains("Training"));
        assertTrue(tutorial.contains("IN THE HALL"));
        assertFalse(quests.consumeEnding());
        assertTrue(quests.getActive().isEmpty());
        assertTrue(quests.getCompleted().isEmpty());
    }

    /** Hall fits one view, has a left-side Sage/right-side circle and a valid lower-left return. */
    @Test void layoutIsOneScreenAndHasNoCombatEncounter() {
        guide.select(PortalGuide.Destination.MEMORIAL);
        cross(TravelCatalog.QUEST_PORTAL_ID);
        var hall = session.getCurrentMap();
        assertEquals(MapType.MEMORIAL, hall.getType());
        assertEquals(new CollisionBox(0, 0, GameConfig.CAMERA_VIEW_WIDTH, GameConfig.CAMERA_VIEW_HEIGHT), hall.getBounds());
        assertTrue(hall.isWalkable(GameConfig.MEMORIAL_CIRCLE));
        assertEquals(Set.of(BossChallenge.values()), GameConfig.MEMORIAL_STONES.keySet());
        GameConfig.MEMORIAL_STONES.values().forEach(box -> assertTrue(hall.isWalkable(box)));
        assertTrue(GameConfig.GUIDE.getX() < hall.getBounds().width() / 2);
        assertEquals(hall.getBounds().height() / 2, GameConfig.GUIDE.getY() + GameConfig.GUIDE.getHeight() / 2, 4);
        assertFalse(GameConfig.HUB_NPCS.contains(GameConfig.GUIDE));
        assertTrue(GameConfig.MEMORIAL_CIRCLE.x() > GameConfig.GUIDE.getX());
        var portal = travel.getPortal(TravelCatalog.MEMORIAL_RETURN);
        assertEquals(new CollisionBox(32, 32, 48, 64), portal.trigger());
        assertTrue(hall.isWalkable(portal.trigger()));
        assertFalse(session.getPlayer().getBounds().overlaps(portal.trigger()));
        assertEquals(1, demo.getCombatants().size());
        assertTrue(demo.getBoss().isEmpty());
        assertTrue(demo.getTrainingDummy().isEmpty());
        assertTrue(demo.getEnemies().isEmpty());
    }

    /** Hall is quest-free and both journeys preserve the player, selected class/mode and anti-loop rules. */
    @Test void roundTripWithoutQuestDoesNotLoopOrCreateResults() {
        demo.update(0, PlayerCommand.NONE, false, PlayerClass.ARCHER);
        assertTrue(demo.getAbilities().selectUltimateMode(UltimateMode.B));
        var player = session.getPlayer();
        guide.select(PortalGuide.Destination.MEMORIAL);
        cross(TravelCatalog.QUEST_PORTAL_ID);
        for (int i = 0; i < 20; i++) { assertFalse(travel.updatePortals()); }
        assertEquals(MapCatalog.MEMORIAL_HALL, session.getCurrentMap().getId());
        assertFalse(quests.consumeEnding());
        assertFalse(demo.getAbilities().selectUltimateMode(UltimateMode.A));
        cross(TravelCatalog.MEMORIAL_RETURN);
        for (int i = 0; i < 20; i++) { assertFalse(travel.updatePortals()); }
        assertEquals(MapCatalog.HUB, session.getCurrentMap().getId());
        assertSame(player, session.getPlayer());
        assertEquals(PlayerClass.ARCHER, player.getPlayerClass());
        assertEquals(UltimateMode.B, player.getUltimateMode());
        assertTrue(flow.getLastResult().isEmpty());
        assertTrue(flow.getPendingRewards().isEmpty());
        assertTrue(quests.getCompleted().isEmpty());
    }

    /** A Hall detour does not replace an accepted quest or disable the independent Hall exit. */
    @Test void hallPreservesAcceptedQuestAndBattleRouteCanBeRestored() {
        quests.accept(BossChallenge.WATER);
        var accepted = flow.getActiveEncounter();
        assertTrue(travel.getPortal(TravelCatalog.MEMORIAL_RETURN).enabled());
        guide.select(PortalGuide.Destination.MEMORIAL);
        cross(TravelCatalog.QUEST_PORTAL_ID);
        assertFalse(flow.isBattleActive());
        assertEquals(accepted, flow.getActiveEncounter());
        cross(TravelCatalog.MEMORIAL_RETURN);
        guide.select(PortalGuide.Destination.BATTLE);
        cross(TravelCatalog.QUEST_PORTAL_ID);
        assertEquals(MapCatalog.WATER_ARENA, session.getCurrentMap().getId());
        assertEquals(BossChallenge.WATER.id(), demo.getBoss().orElseThrow().getId());
    }

    /**
     * Each victory lights only its corresponding stone and neither consumes nor duplicates loot.
     * @param target guardian whose stone should light
     */
    @ParameterizedTest @EnumSource(BossChallenge.class)
    void victoryPersistsAsExactlyOneStoneAcrossHallVisits(BossChallenge target) {
        quests.accept(target);
        guide.select(PortalGuide.Destination.BATTLE);
        cross(TravelCatalog.QUEST_PORTAL_ID);
        demo.getBoss().orElseThrow().getHealth().takeDamage(400);
        demo.update(0, PlayerCommand.NONE, false);
        quests.synchronize();
        var rewards = flow.getPendingRewards();
        assertEquals(1, rewards.size());
        for (int visit = 0; visit < 2; visit++) {
            guide.select(PortalGuide.Destination.MEMORIAL);
            cross(TravelCatalog.QUEST_PORTAL_ID);
            for (int poll = 0; poll < 10; poll++) { quests.synchronize(); }
            assertEquals(Set.of(target), quests.getCompleted());
            assertFalse(quests.consumeEnding());
            assertEquals(rewards, flow.getPendingRewards());
            cross(TravelCatalog.MEMORIAL_RETURN);
        }
    }

    /** Defeat cannot grant a stone or an ending, and its retained quest survives a Hall detour. */
    @Test void defeatDoesNotLightAnyStone() {
        quests.accept(BossChallenge.FIRE);
        guide.select(PortalGuide.Destination.BATTLE);
        cross(TravelCatalog.QUEST_PORTAL_ID);
        session.getPlayer().takeDamage(100);
        demo.update(0, PlayerCommand.NONE, false);
        quests.synchronize();
        guide.select(PortalGuide.Destination.MEMORIAL);
        cross(TravelCatalog.QUEST_PORTAL_ID);
        assertTrue(quests.getCompleted().isEmpty());
        assertFalse(quests.consumeEnding());
        assertEquals(BossChallenge.FIRE, quests.getActive().orElseThrow());
        assertTrue(flow.getPendingRewards().isEmpty());
    }

    /**
     * Crosses a real gate after arming the existing arrival protection outside all triggers.
     * @param id registered source portal ID
     */
    private void cross(String id) {
        travel.updatePortals();
        var portal = travel.getPortal(id);
        session.getPlayer().enterArea(session.getCurrentMap(), portal.trigger().x(), portal.trigger().y());
        assertTrue(travel.updatePortals());
        demo.update(0, PlayerCommand.NONE, false);
    }
}
