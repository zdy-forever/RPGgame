package au.edu.anu.comp2120.save;

import au.edu.anu.comp2120.*;
import au.edu.anu.comp2120.combat.ElementType;
import au.edu.anu.comp2120.enemy.BossChallenge;
import au.edu.anu.comp2120.inventory.*;
import au.edu.anu.comp2120.npc.NPC;
import au.edu.anu.comp2120.player.*;
import au.edu.anu.comp2120.quest.BossQuests;
import au.edu.anu.comp2120.screen.CombatDemo;
import au.edu.anu.comp2120.world.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Exercises complete save timelines through real combat settlement and restart fixtures.
 * @author Hao Liu (u7998420)
 */
final class GameSavesTest {
    /** Isolated save directory. */
    @TempDir Path directory;
    /** Creates a test fixture. */
    GameSavesTest() { }

    /** Independent game instance without any LibGDX window.
     * @author Hao Liu (u7998420)
     */
    private static final class Game {
        /** Persistent world. */
        final GameSession session = new GameSession(MapCatalog.createMaps(), MapCatalog.HUB, 28, 28, 180);
        /** Travel authority. */
        final TravelSystem travel = new TravelSystem(session, TravelCatalog.createGameplayPortals(), TravelCatalog.createDestinations());
        /** Encounter owner. */
        final EncounterFlow flow = new EncounterFlow(session, travel, List.of(TravelCatalog.QUEST_PORTAL_ID));
        /** Quest owner. */
        final BossQuests quests = new BossQuests(session, flow);
        /** Real combat coordination. */
        final CombatDemo combat = new CombatDemo(session, flow);
        /** Separate storage. */
        final Inventory chest = new Inventory();
        /** Independent reward-giving NPC. */
        final NPC npc = new NPC("merchant", "Merchant", 0, 0, 20, 20, "Reward", "weapon_lightning_staff");
        /** Disk owner. */
        final SaveStore store;
        /** Integrated save coordinator. */
        final GameSaves saves;
        /** Creates a clean runtime over existing or missing files.
         * @param directory independent disk directory
         */
        Game(Path directory) {
            store = new SaveStore(directory);
            saves = new GameSaves(session, travel, flow, quests, combat, chest, List.of(npc), store);
        }
        /** Enters a fresh admitted Boss battle through the real portal.
         * @param boss selected challenge
         */
        void enter(BossChallenge boss) {
            quests.accept(boss);
            flow.activate(flow.getActiveEncounter().orElseThrow());
            session.getPlayer().enterArea(session.getCurrentMap(), 400, 200);
            travel.updatePortals();
            var trigger = travel.getPortal(TravelCatalog.QUEST_PORTAL_ID).trigger();
            session.getPlayer().enterArea(session.getCurrentMap(), trigger.x(), trigger.y());
            assertTrue(travel.updatePortals());
            combat.update(0, PlayerCommand.NONE, false);
            assertTrue(combat.getBoss().isPresent());
        }
        /** Settles a real victory and runs the screen's post-combat pipeline.
         * @param boss chosen guardian
         */
        void win(BossChallenge boss) {
            enter(boss);
            combat.getBoss().orElseThrow().getHealth().takeDamage(Integer.MAX_VALUE);
            combat.update(0, PlayerCommand.NONE, false);
            new InventoryRewards(flow, session.getPlayer().getInventory()).collect();
            quests.synchronize();
            saves.afterSettlement();
            assertTrue(saves.status().contains("automatic save complete"), saves.status());
        }
    }

    /** Fresh runtime restores choices, cooldowns, storage, NPC flags and accepted quest.
     * @throws IOException if snapshot inspection fails
     */
    @Test
    void restartRestoresCompleteManualSnapshot() throws IOException {
        var game = new Game(directory);
        var player = game.session.getPlayer();
        var modes = new EnumMap<PlayerClass, UltimateMode>(PlayerClass.class);
        for (var c : PlayerClass.values()) { modes.put(c, UltimateMode.B); }
        player.restoreSelections(PlayerClass.KATANA, ElementType.ICE, modes);
        var timers = new EnumMap<AbilitySlot, Double>(AbilitySlot.class);
        for (var a : AbilitySlot.values()) { timers.put(a, 2.75); }
        game.combat.getAbilities().restoreCooldowns(timers);
        player.getInventory().addItem("guardian_coat", 2); player.getEquipment().toggle("guardian_coat");
        game.chest.addItem("health_potion", 7); game.npc.setInteracted();
        game.quests.accept(BossChallenge.WATER);
        game.flow.activate(game.flow.getActiveEncounter().orElseThrow());
        player.moveBy(10.25, 15.5); player.takeDamage(31);
        assertTrue(game.saves.saveManual(), game.saves.status());
        byte[] file = Files.readAllBytes(game.store.path(SaveSlot.MANUAL));
        var restarted = new Game(directory);
        assertTrue(restarted.saves.load(SaveSlot.MANUAL), restarted.saves.status());
        var restored = restarted.session.getPlayer();
        assertEquals(LocationSnapshot.capture(game.session), LocationSnapshot.capture(restarted.session));
        assertEquals(InventorySnapshot.capture(player, game.chest), InventorySnapshot.capture(restored, restarted.chest));
        assertEquals(PlayerClass.KATANA, restored.getPlayerClass());
        assertEquals(ElementType.ICE, restored.getMageElement());
        assertEquals(modes, restored.savedModes());
        for (var a : AbilitySlot.values()) { assertEquals(2.75, restarted.combat.getAbilities().getCooldownRemaining(a)); }
        assertTrue(restarted.npc.hasInteracted());
        assertEquals(game.flow.getActiveEncounter(), restarted.flow.getActiveEncounter());
        assertTrue(restarted.travel.getPortal(TravelCatalog.QUEST_PORTAL_ID).enabled());
        assertEquals(Optional.of(BossChallenge.WATER), restarted.quests.getActive());
        restarted.saves.afterSettlement();
        assertArrayEquals(file, Files.readAllBytes(game.store.path(SaveSlot.MANUAL)));
        assertFalse(Files.exists(game.store.path(SaveSlot.AUTO)));
    }

    /** Every victory checkpoints settled rewards immediately; an old manual timeline can replace auto later.
     * @throws IOException if save inspection fails
     */
    @Test
    void victoriesAndTimelineRollbackKeepSlotsIndependent() throws IOException {
        var game = new Game(directory);
        assertTrue(game.saves.saveManual());
        byte[] manual = Files.readAllBytes(game.store.path(SaveSlot.MANUAL));
        game.win(BossChallenge.FIRE);
        var autoOne = game.store.load(SaveSlot.AUTO);
        assertEquals("1", autoOne.get("quest.wins.FIRE"));
        assertEquals("hub", autoOne.get("location.map"));
        assertEquals("100", autoOne.get("location.hp"));
        assertFalse(game.session.getPlayer().getInventory().isEmpty());
        byte[] autoBytes = Files.readAllBytes(game.store.path(SaveSlot.AUTO));
        game.saves.afterSettlement();
        assertArrayEquals(autoBytes, Files.readAllBytes(game.store.path(SaveSlot.AUTO)));
        game.win(BossChallenge.FIRE);
        assertEquals("2", game.store.load(SaveSlot.AUTO).get("quest.wins.FIRE"));
        assertArrayEquals(manual, Files.readAllBytes(game.store.path(SaveSlot.MANUAL)));
        assertTrue(game.saves.load(SaveSlot.MANUAL));
        assertEquals(0, game.flow.getSuccessCount(MapCatalog.CROSSROADS));
        assertTrue(game.session.getPlayer().getInventory().isEmpty());
        game.win(BossChallenge.WATER);
        assertEquals("0", game.store.load(SaveSlot.AUTO).get("quest.wins.FIRE"));
        assertEquals("1", game.store.load(SaveSlot.AUTO).get("quest.wins.WATER"));
        assertArrayEquals(manual, Files.readAllBytes(game.store.path(SaveSlot.MANUAL)));
    }

    /** Battle/training/death and active skills prohibit manual operations; cooldown alone does not.
     * @throws IOException if file inspection fails
     */
    @Test
    void safetyGuardsProtectBothSlots() throws IOException {
        var game = new Game(directory);
        assertTrue(game.saves.saveManual());
        byte[] before = Files.readAllBytes(game.store.path(SaveSlot.MANUAL));
        game.combat.update(0, new PlayerCommand(0, 0, true, false, false, false), false);
        assertFalse(game.saves.isSafe());
        assertFalse(game.saves.saveManual()); assertFalse(game.saves.load(SaveSlot.MANUAL));
        game.session.switchMap(MapCatalog.TRAINING); game.combat.update(0, PlayerCommand.NONE, false);
        assertFalse(game.saves.saveManual()); assertFalse(game.saves.load(SaveSlot.MANUAL));
        game.session.switchMap(MapCatalog.HUB); game.combat.update(0, PlayerCommand.NONE, false);
        game.enter(BossChallenge.ICE);
        assertFalse(game.saves.saveManual()); assertFalse(game.saves.load(SaveSlot.MANUAL));
        game.combat.getBoss().orElseThrow().getHealth().takeDamage(Integer.MAX_VALUE);
        game.session.getPlayer().takeDamage(Integer.MAX_VALUE);
        assertFalse(game.saves.isSafe());
        game.combat.update(0, PlayerCommand.NONE, false);
        game.quests.synchronize(); game.saves.afterSettlement();
        assertFalse(Files.exists(game.store.path(SaveSlot.AUTO)));
        assertTrue(game.quests.getCompleted().isEmpty());
        assertArrayEquals(before, Files.readAllBytes(game.store.path(SaveSlot.MANUAL)));
    }

    /** Corruption in a late component never partially rolls back live player state.
     * @throws IOException if fixture save IO fails
     */
    @Test
    void invalidLateFieldsLeaveAllLiveStateUntouched() throws IOException {
        var game = new Game(directory);
        assertTrue(game.saves.saveManual());
        var good = game.store.load(SaveSlot.MANUAL);
        game.session.getPlayer().takeDamage(12);
        game.session.getPlayer().getInventory().addItem("ore", 9);
        game.npc.setInteracted();
        var before = LocationSnapshot.capture(game.session);
        long version = game.session.getTransitionVersion();
        for (var invalid : Map.of("npc.merchant", "not-boolean", "quest.wins.FIRE", "1",
                "player.cooldown.LIGHT", "NaN", "quest.ending", "true", "unsupported", "x").entrySet()) {
            var fields = new HashMap<>(good); fields.put(invalid.getKey(), invalid.getValue());
            game.store.save(SaveSlot.MANUAL, fields);
            assertFalse(game.saves.load(SaveSlot.MANUAL));
            assertEquals(before, LocationSnapshot.capture(game.session));
            assertEquals(version, game.session.getTransitionVersion());
            assertEquals(9, game.session.getPlayer().getInventory().quantityOf("ore"));
            assertTrue(game.npc.hasInteracted());
        }
    }

    /** Overflow rewards survive restart and are claimable exactly once when space becomes available.
     * @throws IOException if save fixture IO fails
     */
    @Test
    void pendingOverflowRewardSurvivesReload() throws IOException {
        var game = new Game(directory);
        game.enter(BossChallenge.FIRE);
        game.combat.getBoss().orElseThrow().getHealth().takeDamage(Integer.MAX_VALUE);
        game.combat.update(0, PlayerCommand.NONE, false);
        var drop = game.flow.getPendingRewards().values().iterator().next();
        game.session.getPlayer().getInventory().addItem(drop.materialId(), Integer.MAX_VALUE);
        assertEquals(0, new InventoryRewards(game.flow, game.session.getPlayer().getInventory()).collect());
        game.quests.synchronize(); game.saves.afterSettlement();
        assertTrue(Files.exists(game.store.path(SaveSlot.AUTO)), game.saves.status());
        var restarted = new Game(directory);
        assertTrue(restarted.saves.load(SaveSlot.AUTO), restarted.saves.status());
        assertEquals(1, restarted.flow.getPendingRewards().size());
        restarted.session.getPlayer().getInventory().removeItem(drop.materialId(), drop.quantity());
        var collector = new InventoryRewards(restarted.flow, restarted.session.getPlayer().getInventory());
        assertEquals(1, collector.collect()); assertEquals(0, collector.collect());
        assertEquals(Integer.MAX_VALUE, restarted.session.getPlayer().getInventory().quantityOf(drop.materialId()));
    }

    /** Final victory preserves pending ending and manually saving after playback preserves consumption. */
    @Test
    void endingProgressSurvivesBothSaveSlots() {
        var game = new Game(directory);
        for (var boss : BossChallenge.values()) { game.win(boss); }
        var restarted = new Game(directory);
        assertTrue(restarted.saves.load(SaveSlot.AUTO), restarted.saves.status());
        restarted.session.switchMap(MapCatalog.MEMORIAL_HALL);
        restarted.combat.update(0, PlayerCommand.NONE, false);
        assertTrue(restarted.quests.consumeEnding());
        assertTrue(restarted.saves.saveManual(), restarted.saves.status());
        var again = new Game(directory);
        assertTrue(again.saves.load(SaveSlot.MANUAL), again.saves.status());
        assertTrue(again.quests.hasEndingStarted());
        assertFalse(again.quests.consumeEnding());
    }

    /** A file write failure retains prior checkpoints and the already-earned victory.
     * @throws IOException if fixture IO fails
     */
    @Test
    void autosaveFailurePreservesVictoryAndManualFile() throws IOException {
        var game = new Game(directory);
        assertTrue(game.saves.saveManual());
        byte[] manual = Files.readAllBytes(game.store.path(SaveSlot.MANUAL));
        Files.createDirectory(game.store.path(SaveSlot.AUTO));
        Files.writeString(game.store.path(SaveSlot.AUTO).resolve("block"), "keep");
        game.enter(BossChallenge.FIRE);
        game.combat.getBoss().orElseThrow().getHealth().takeDamage(Integer.MAX_VALUE);
        game.combat.update(0, PlayerCommand.NONE, false);
        new InventoryRewards(game.flow, game.session.getPlayer().getInventory()).collect();
        game.quests.synchronize(); game.saves.afterSettlement();
        assertTrue(game.saves.status().startsWith("Save failed"));
        assertEquals(Set.of(BossChallenge.FIRE), game.quests.getCompleted());
        assertArrayEquals(manual, Files.readAllBytes(game.store.path(SaveSlot.MANUAL)));
        assertEquals("keep", Files.readString(game.store.path(SaveSlot.AUTO).resolve("block")));
    }
}
