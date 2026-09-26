package au.edu.anu.comp2120.screen;

import static org.junit.jupiter.api.Assertions.*;

import au.edu.anu.comp2120.BattleResult;
import au.edu.anu.comp2120.EncounterFlow;
import au.edu.anu.comp2120.GameSession;
import au.edu.anu.comp2120.TravelSystem;
import au.edu.anu.comp2120.combat.Attack;
import au.edu.anu.comp2120.combat.AttackSpec;
import au.edu.anu.comp2120.combat.CombatSystem;
import au.edu.anu.comp2120.combat.ElementField;
import au.edu.anu.comp2120.combat.ElementType;
import au.edu.anu.comp2120.combat.Faction;
import au.edu.anu.comp2120.enemy.TrainingDummy;
import au.edu.anu.comp2120.enemy.BossChallenge;
import au.edu.anu.comp2120.quest.BossQuests;
import au.edu.anu.comp2120.npc.PortalGuide;
import au.edu.anu.comp2120.player.AbilitySlot;
import au.edu.anu.comp2120.player.PlayerClass;
import au.edu.anu.comp2120.player.PlayerClasses;
import au.edu.anu.comp2120.player.PlayerCommand;
import au.edu.anu.comp2120.player.UltimateMode;
import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.MapCatalog;
import au.edu.anu.comp2120.world.MapType;
import au.edu.anu.comp2120.world.TravelCatalog;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Verifies single-portal NPC selection, non-hostile training scores and unchanged Boss outcomes.
 * Exercises real session/travel/combat coordinators without graphics, Quest or Inventory substitutes.
 * @author Daoyan Zhu (u7782042)
 */
final class PortalGuideTrainingTest {
    /** Real shared player and maps, initially in Hub. */
    private final GameSession session = new GameSession(MapCatalog.createMaps(), MapCatalog.HUB, 28, 28, 180);
    /** Single production portal plus independent fast-travel destinations. */
    private final TravelSystem travel = new TravelSystem(session, TravelCatalog.createQuestPortals(),
            TravelCatalog.createDestinations());
    /** Existing admission/result/reward boundary. */
    private final EncounterFlow flow = new EncounterFlow(session, travel, List.of(TravelCatalog.QUEST_PORTAL_ID));
    /** Single damage pipeline, retained for focused reaction arrangements. */
    private final CombatSystem combat = new CombatSystem();
    /** Actual production coordinator, not the unrestricted demo. */
    private final CombatDemo demo = new CombatDemo(session, combat, flow);
    /** NPC adapter used by GameScreen. */
    private final PortalGuide guide = new PortalGuide(session, travel, flow);
    /** Real quest acceptance is now required before the guide can configure Battle. */
    private final BossQuests quests = new BossQuests(session, flow);

    /** Creates an independent live-Hub fixture for each test. */
    PortalGuideTrainingTest() { }

    /** Hub layout uses the requested coordinates and preserves the merchant/forge offset. */
    @Test
    void hubLayoutPreservesRequestedPositionsAndOffsets() {
        assertEquals(new CollisionBox(0, 0, 1080, 400), session.getCurrentMap().getBounds());
        assertEquals(590, GameConfig.STORAGE_CHEST.getX());
        assertEquals(120, GameConfig.STORAGE_CHEST.getY());
        assertEquals(347, GameConfig.QUEST_KEEPER.getX());
        assertEquals(240, GameConfig.QUEST_KEEPER.getY());
        assertEquals(650, GameConfig.WEAPON_MERCHANT.getX());
        assertEquals(250, GameConfig.WEAPON_MERCHANT.getY());
        assertEquals(40, GameConfig.FORGE_BOUNDS.x() - GameConfig.WEAPON_MERCHANT.getX());
        assertEquals(GameConfig.WEAPON_MERCHANT.getY(), GameConfig.FORGE_BOUNDS.y());
        assertEquals(765, GameConfig.PORTAL_GUIDE.getX());
        assertEquals(245, GameConfig.PORTAL_GUIDE.getY());
        var trigger = travel.getPortal(TravelCatalog.QUEST_PORTAL_ID).trigger();
        assertEquals(new CollisionBox(890, 245, 48, 64), trigger);
    }

    /** Resizing Hub leaves all NPCs, both spawn bodies and portal definitions within valid bounds. */
    @Test
    void smallerHubContainsAllInteractionPointsAndSpawns() {
        var hub = session.getCurrentMap();
        for (var npc : GameConfig.HUB_NPCS) {
            assertTrue(hub.isWalkable(new CollisionBox(npc.getX(), npc.getY(),
                    npc.getWidth(), npc.getHeight())), npc.getName());
        }
        assertTrue(hub.isWalkable(GameConfig.FORGE_BOUNDS));
        for (var spawn : hub.getSpawnPoints().values()) {
            assertTrue(hub.isWalkable(new CollisionBox(spawn.x(), spawn.y(), 28, 28)));
        }
        for (var portal : TravelCatalog.createQuestPortals()) {
            assertTrue(hub.isWalkable(portal.trigger()));
        }
        for (var portal : TravelCatalog.createPortals()) {
            if (portal.sourceMapId().equals(MapCatalog.HUB)) {
                assertTrue(hub.isWalkable(portal.trigger()));
            }
        }
    }

    /** The sole portal starts closed and the guide is registered nearby without covering its trigger. */
    @Test
    void onePortalAndAdjacentGuideAreAvailable() {
        var portals = travel.getPortals(MapCatalog.HUB);
        assertEquals(1, portals.size());
        assertFalse(portals.get(0).enabled());
        var npc = GameConfig.PORTAL_GUIDE;
        assertTrue(GameConfig.HUB_NPCS.contains(npc));
        assertFalse(portals.get(0).trigger().overlaps(
                new CollisionBox(npc.getX(), npc.getY(), npc.getWidth(), npc.getHeight())));
        assertEquals(125, portals.get(0).trigger().x() - npc.getX());
        assertEquals(npc.getY(), portals.get(0).trigger().y());
        assertTrue(flow.getActiveEncounter().isEmpty());
    }

    /**
     * Confirmation changes routing without teleporting; crossing the same portal reaches the selected map.
     * @param destination one of the two visible options
     */
    @ParameterizedTest
    @EnumSource(PortalGuide.Destination.class)
    void selectionRequiresWalkingIntoTheSinglePortal(PortalGuide.Destination destination) {
        CollisionBox before = session.getPlayer().getBounds();
        if (destination == PortalGuide.Destination.BATTLE) {
            quests.accept(BossChallenge.FIRE);
        }
        String expectedMap = switch (destination) {
            case BATTLE -> MapCatalog.CROSSROADS;
            case TRAINING -> MapCatalog.TRAINING;
            case MEMORIAL -> MapCatalog.MEMORIAL_HALL;
        };
        guide.select(destination);
        assertEquals(MapCatalog.HUB, session.getCurrentMap().getId());
        assertEquals(before, session.getPlayer().getBounds());
        assertEquals(expectedMap, travel.getPortal(TravelCatalog.QUEST_PORTAL_ID).destinationMapId());
        enterPortal();
        assertEquals(expectedMap, session.getCurrentMap().getId());
        var spawn = session.getCurrentMap().getSpawnPoint("entry");
        assertEquals(new CollisionBox(spawn.x(), spawn.y(), 28, 28), session.getPlayer().getBounds());
        assertEquals(destination == PortalGuide.Destination.BATTLE, flow.isBattleActive());
        assertEquals(destination == PortalGuide.Destination.BATTLE, demo.getBoss().isPresent());
        assertEquals(destination == PortalGuide.Destination.TRAINING, demo.getTrainingDummy().isPresent());
        assertEquals(destination == PortalGuide.Destination.MEMORIAL ? 1 : 2, demo.getCombatants().size());
        assertTrue(demo.getEnemies().isEmpty());
        for (int i = 0; i < 20; i++) { assertFalse(travel.updatePortals()); }
        assertEquals(expectedMap, session.getCurrentMap().getId());
    }

    /** Practice stays stationary, fires upward without hitting a player to its side, and grants no rewards. */
    @Test
    void targetStaysStillAndOnlyAttacksUpward() {
        train();
        var dummy = dummy();
        CollisionBox before = dummy.getCombatant().getHurtbox();
        session.getPlayer().enterArea(session.getCurrentMap(),
                dummy().getCombatant().getHurtbox().x() - 40, dummy().getCombatant().getHurtbox().y());
        for (int i = 0; i < 600; i++) { demo.update(0.1, PlayerCommand.NONE, false); }
        assertEquals(before, dummy.getCombatant().getHurtbox());
        assertEquals(100, session.getPlayer().getHealth().getCurrentHp());
        assertEquals(0, dummy.getTotalDamage());
        assertTrue(combat.getActiveAttacks().stream().allMatch(a -> a.getProjectile() != null
                && a.getProjectile().getDirectionX() == 0 && a.getProjectile().getDirectionY() == 1));
        assertTrue(flow.getLastResult().isEmpty());
        assertTrue(flow.getPendingRewards().isEmpty());
        assertTrue(demo.getDrops().isEmpty());
    }

    /**
     * All three professions score real J damage without duplicating any damage calculation.
     * @param type selected player profession
     */
    @ParameterizedTest
    @EnumSource(PlayerClass.class)
    void lightAttacksUseSharedDamageAndScoreOnce(PlayerClass type) {
        demo.update(0, PlayerCommand.NONE, false, type);
        train();
        session.getPlayer().enterArea(session.getCurrentMap(),
                dummy().getCombatant().getHurtbox().x() - 40, dummy().getCombatant().getHurtbox().y());
        demo.update(0.1, new PlayerCommand(1, 0, true, false, false, false), false);
        int expected = PlayerClasses.get(type).abilities().light().attack().damage();
        assertEquals(expected, dummy().getTotalDamage());
        assertEquals(expected, dummy().getLastDamage());
        assertEquals(1, dummy().getHitCount());
        for (int i = 0; i < 10; i++) { demo.update(0, PlayerCommand.NONE, false); }
        assertEquals(expected, dummy().getTotalDamage());
        assertEquals(1, dummy().getHitCount());
        assertTrue(dummy().isAlive());
        assertEquals(100, session.getPlayer().getHealth().getCurrentHp());
    }

    /** Reaction multipliers and periodic child damage are counted at actual delivery, not cast input. */
    @Test
    void reactionsAndFieldsContributeTheirFinalDamage() {
        train();
        hit(ElementType.FIRE, 10);
        hit(ElementType.WATER, 20);
        assertEquals(50, dummy().getTotalDamage());
        assertEquals(40, dummy().getLastDamage());
        assertEquals(2, dummy().getHitCount());
        hit(ElementType.FIRE, 10);
        hit(ElementType.WIND, 10);
        assertEquals(70, dummy().getTotalDamage());
        assertEquals(1, combat.getElementFields().size());
        demo.update(0.5, PlayerCommand.NONE, false);
        assertEquals(76, dummy().getTotalDamage());
        assertEquals(6, dummy().getLastDamage());
        assertEquals(5, dummy().getHitCount());
        assertTrue(flow.getLastResult().isEmpty());
    }

    /** Knockback is discarded for the fixed target while reaction damage still uses the shared pipeline. */
    @Test
    void overloadDoesNotMoveTheTrainingTarget() {
        train();
        CollisionBox before = dummy().getCombatant().getHurtbox();
        hit(ElementType.FIRE, 10);
        hit(ElementType.LIGHTNING, 10);
        demo.update(0.1, PlayerCommand.NONE, false);
        assertEquals(40, dummy().getTotalDamage());
        assertEquals(before, dummy().getCombatant().getHurtbox());
        assertEquals(0, dummy().getCombatant().getStatus().getKnockbackX());
        assertEquals(0, dummy().getCombatant().getStatus().getKnockbackY());
    }

    /** R creates a clean practice scope, removing pending damage and preserving player choices. */
    @Test
    void resetClearsScoreEffectsAndCooldowns() {
        demo.update(0, PlayerCommand.NONE, false, PlayerClass.ARCHER);
        assertTrue(demo.getAbilities().selectUltimateMode(UltimateMode.B));
        train();
        var old = dummy();
        var player = session.getPlayer();
        hit(ElementType.FIRE, 10);
        hit(ElementType.WIND, 10);
        demo.update(0, new PlayerCommand(0, 0, true, false, false, false), false);
        var attacks = combat.getActiveAttacks();
        assertFalse(attacks.isEmpty());
        var fields = combat.getElementFields();
        assertFalse(fields.isEmpty());
        demo.update(0.1, new PlayerCommand(1, 1, true, true, true, true), true);
        assertSame(player, session.getPlayer());
        assertEquals(PlayerClass.ARCHER, player.getPlayerClass());
        assertEquals(UltimateMode.B, player.getUltimateMode());
        assertNotSame(old, dummy());
        assertFalse(combat.getCombatants().contains(old.getCombatant()));
        assertEquals(0, dummy().getTotalDamage());
        assertEquals(0, dummy().getHitCount());
        assertEquals(0, dummy().getLastDamage());
        assertEquals(0, demo.getAbilities().getCooldownRemaining(AbilitySlot.LIGHT));
        assertTrue(combat.getActiveAttacks().isEmpty());
        assertTrue(combat.getElementFields().isEmpty());
        assertTrue(attacks.stream().noneMatch(Attack::isActive));
        assertTrue(fields.stream().noneMatch(ElementField::isActive));
    }

    /** Leaving practice clears effects and re-entry starts a fresh score, with no fabricated battle result. */
    @Test
    void practiceReturnAndBattleSelectionStaySeparate() {
        train();
        var old = dummy();
        hit(ElementType.FIRE, 10);
        hit(ElementType.WIND, 10);
        hub();
        assertTrue(demo.getTrainingDummy().isEmpty());
        assertFalse(combat.getCombatants().contains(old.getCombatant()));
        assertTrue(combat.getElementFields().isEmpty());
        assertTrue(flow.getLastResult().isEmpty());
        assertTrue(flow.getPendingRewards().isEmpty());
        enterPortal();
        assertNotSame(old, dummy());
        assertEquals(0, dummy().getTotalDamage());
        hub();
        quests.accept(BossChallenge.FIRE);
        guide.select(PortalGuide.Destination.BATTLE);
        enterPortal();
        assertEquals(MapType.BATTLE, session.getCurrentMap().getType());
        assertTrue(demo.getTrainingDummy().isEmpty());
        assertTrue(demo.getBoss().isPresent());
    }

    /**
     * Battle still restores Hub and either retains retry permission or closes it with one reward.
     * @param success whether the Boss is defeated instead of the player
     */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void battleOutcomesAndSubsequentNpcSelection(boolean success) {
        quests.accept(BossChallenge.FIRE);
        guide.select(PortalGuide.Destination.BATTLE);
        var accepted = flow.getActiveEncounter().orElseThrow();
        guide.select(PortalGuide.Destination.BATTLE);
        assertEquals(accepted, flow.getActiveEncounter().orElseThrow());
        enterPortal();
        var oldBoss = demo.getBoss().orElseThrow();
        if (success) { oldBoss.getHealth().takeDamage(400); }
        else { session.getPlayer().takeDamage(100); }
        demo.update(0, PlayerCommand.NONE, false);
        assertEquals(MapCatalog.HUB, session.getCurrentMap().getId());
        assertEquals(success ? BattleResult.Outcome.SUCCESS : BattleResult.Outcome.FAILURE,
                flow.getLastResult().orElseThrow().outcome());
        assertEquals(!success, travel.getPortal(TravelCatalog.QUEST_PORTAL_ID).enabled());
        assertEquals(success ? 1 : 0, flow.getPendingRewards().size());
        quests.synchronize();
        if (success) {
            assertThrows(IllegalStateException.class, () -> guide.select(PortalGuide.Destination.BATTLE));
            quests.accept(BossChallenge.WATER);
        }
        guide.select(PortalGuide.Destination.BATTLE);
        if (success) { assertNotEquals(accepted.id(), flow.getActiveEncounter().orElseThrow().id()); }
        else { assertEquals(accepted, flow.getActiveEncounter().orElseThrow()); }
        guide.select(PortalGuide.Destination.TRAINING);
        assertTrue(flow.getActiveEncounter().isPresent(), "Practice must not abandon an accepted quest");
        enterPortal();
        assertTrue(demo.getBoss().isEmpty());
        assertTrue(demo.getTrainingDummy().isPresent());
        assertEquals(success ? 1 : 0, flow.getPendingRewards().size());
    }

    /**
     * Neither dialogue destination can change admission outside a living Hub.
     * @param destination requested route
     */
    @ParameterizedTest
    @EnumSource(PortalGuide.Destination.class)
    void selectionGuardsAreAtomic(PortalGuide.Destination destination) {
        quests.accept(BossChallenge.FIRE);
        guide.select(PortalGuide.Destination.BATTLE);
        var accepted = flow.getActiveEncounter().orElseThrow();
        var portal = travel.getPortal(TravelCatalog.QUEST_PORTAL_ID);
        session.getPlayer().takeDamage(100);
        assertThrows(IllegalStateException.class, () -> guide.select(destination));
        assertEquals(portal, travel.getPortal(TravelCatalog.QUEST_PORTAL_ID));
        assertEquals(accepted, flow.getActiveEncounter().orElseThrow());
        session.getPlayer().getHealth().restoreFull();
        enterPortal();
        assertThrows(IllegalStateException.class, () -> guide.select(destination));
        assertEquals(portal, travel.getPortal(TravelCatalog.QUEST_PORTAL_ID));
        assertEquals(accepted, flow.getActiveEncounter().orElseThrow());
        assertThrows(NullPointerException.class, () -> guide.select(null));
    }

    /** Tests that the current route reaches a full dummy scope rather than a Boss encounter. */
    private void train() {
        guide.select(PortalGuide.Destination.TRAINING);
        enterPortal();
        assertTrue(demo.getBoss().isEmpty());
    }

    /** Uses the production Training NPC return action and immediately cleans the old scope. */
    private void hub() {
        guide.returnFromTraining();
        demo.update(0, PlayerCommand.NONE, false);
    }

    /** NPC return preserves HP, identity, class and mode without granting a result or unlocking routes. */
    @Test
    void trainingNpcReturnPreservesPlayerAndDoesNotUnlockDebugDestinations() {
        demo.update(0, PlayerCommand.NONE, false, PlayerClass.MAGE);
        demo.getAbilities().selectUltimateMode(UltimateMode.B);
        var player = session.getPlayer();
        train();
        player.takeDamage(23);
        hub();
        assertSame(player, session.getPlayer());
        assertEquals(MapCatalog.HUB, session.getCurrentMap().getId());
        assertEquals(77, player.getHealth().getCurrentHp());
        assertEquals(PlayerClass.MAGE, player.getPlayerClass());
        assertEquals(UltimateMode.B, player.getUltimateMode());
        assertTrue(demo.getTrainingDummy().isEmpty());
        assertTrue(flow.getLastResult().isEmpty());
        assertTrue(flow.getPendingRewards().isEmpty());
        assertFalse(travel.getDestination(TravelCatalog.TRAINING_ENTRY).unlocked());
        assertFalse(travel.getDestination(TravelCatalog.CROSSROADS_ENTRY).unlocked());
        assertFalse(travel.updatePortals());
    }

    /**
     * The practice exit action is not a general battle-escape or Hub teleport shortcut.
     * @param mapId non-practice map
     */
    @ParameterizedTest
    @ValueSource(strings = {MapCatalog.HUB, MapCatalog.CROSSROADS})
    void trainingReturnRejectsOtherMaps(String mapId) {
        session.switchMap(mapId);
        long version = session.getTransitionVersion();
        assertThrows(IllegalStateException.class, guide::returnFromTraining);
        assertEquals(version, session.getTransitionVersion());
        assertEquals(mapId, session.getCurrentMap().getId());
    }

    /** Dead practice players cannot use the NPC to bypass normal state guards. */
    @Test
    void trainingReturnRejectsDeadPlayer() {
        train();
        session.getPlayer().getHealth().takeDamage(100); // Explicit corrupt/dead state, bypassing practice protection.
        assertThrows(IllegalStateException.class, guide::returnFromTraining);
        assertEquals(MapCatalog.TRAINING, session.getCurrentMap().getId());
        assertFalse(session.getPlayer().isAlive());
    }

    /** Real upward projectiles use equipment mitigation, while practice never kills the player. */
    @Test void upwardShotsRespectArmour() {
        train();
        var player = session.getPlayer();
        var target = dummy().getCombatant().getHurtbox();
        player.enterArea(session.getCurrentMap(), target.x() + target.width() / 2 - 14, target.y() + 100);
        for (int i = 0; i < 80 && player.getTrainingLastDamage() == 0; i++) { demo.update(0.05, PlayerCommand.NONE, false); }
        assertEquals(25, player.getTrainingLastDamage());
        assertEquals(75, player.getHealth().getCurrentHp());
        demo.update(0, PlayerCommand.NONE, true);
        player.getInventory().addItem("guardian_coat");
        player.getEquipment().toggle("guardian_coat");
        player.enterArea(session.getCurrentMap(), target.x() + target.width() / 2 - 14, target.y() + 100);
        for (int i = 0; i < 80 && player.getTrainingLastDamage() == 0; i++) { demo.update(0.05, PlayerCommand.NONE, false); }
        assertTrue(player.getTrainingLastDamage() > 0 && player.getTrainingLastDamage() < 25);
        assertEquals(100 - player.getTrainingLastDamage(), player.getHealth().getCurrentHp());
    }

    /** Hits at one HP restart recovery, zero damage does not, and leaving practice removes protection. */
    @Test void practiceFloorAndRecoveryAreMapScoped() {
        train();
        var player = session.getPlayer();
        player.takeDamage(Integer.MAX_VALUE);
        assertEquals(1, player.getHealth().getCurrentHp());
        player.updateTrainingRecovery(4);
        player.takeDamage(25);
        player.updateTrainingRecovery(4);
        assertEquals(1, player.getHealth().getCurrentHp());
        player.takeDamage(0);
        player.updateTrainingRecovery(1);
        assertEquals(100, player.getHealth().getCurrentHp());
        assertThrows(IllegalArgumentException.class, () -> player.takeDamage(-1));
        session.switchMap(MapCatalog.HUB);
        player.takeDamage(100);
        player.updateTrainingRecovery(10);
        assertFalse(player.isAlive());
    }

    /** Enters the real route after arrival protection has observed departure from its region. */
    private void enterPortal() {
        travel.updatePortals();
        CollisionBox trigger = travel.getPortal(TravelCatalog.QUEST_PORTAL_ID).trigger();
        session.getPlayer().enterArea(session.getCurrentMap(), trigger.x(), trigger.y());
        assertTrue(travel.updatePortals());
        demo.update(0, PlayerCommand.NONE, false);
    }

    /**
     * Returns the current registered practice receiver.
     * @return current target, failing the test if absent
     */
    private TrainingDummy dummy() { return demo.getTrainingDummy().orElseThrow(); }

    /**
     * Arranges an ordinary elemental hit through the one real combat pipeline.
     * @param element direct-hit element
     * @param damage base damage before shared reaction calculation
     */
    private void hit(ElementType element, int damage) {
        var source = combat.getCombatants().stream().filter(actor -> actor.getFaction() == Faction.PLAYER)
                .findFirst().orElseThrow();
        var bounds = dummy().getCombatant().getHurtbox();
        combat.tryAttack(source, new AttackSpec("practice-test", damage, 0.1, 0, element), bounds, bounds).orElseThrow();
        demo.update(0, PlayerCommand.NONE, false);
    }
}
