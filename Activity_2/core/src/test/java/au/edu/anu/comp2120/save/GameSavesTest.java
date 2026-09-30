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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Exercises complete save timelines through real combat settlement and restart fixtures.
 * @author Hao Liu (u7998420)
 * @author Daoyan Zhu (u7782042)
 */
final class GameSavesTest {
    /** Isolated save directory. */
    @TempDir Path directory;
    /** Creates a test fixture. */
    GameSavesTest() { }

    /**
     * Each guardian's restored repeat-win count changes a newly admitted Boss, not just a UI field.
     * @param guardian campaign guardian being challenged repeatedly
     */
    @ParameterizedTest @EnumSource(BossChallenge.class)
    void restoredWinsScaleOnlyTheMatchingGuardian(BossChallenge guardian) {
        var game = new Game(directory);
        game.win(guardian);
        game.win(guardian);
        assertTrue(game.saves.saveManual(), game.saves.status());
        for (SaveSlot slot : SaveSlot.values()) {
            var restarted = new Game(directory);
            assertTrue(restarted.saves.load(slot), restarted.saves.status());
            for (BossChallenge other : BossChallenge.values()) {
                assertEquals(other == guardian ? 2 : 0, restarted.flow.getSuccessCount(other.mapId()));
            }
            restarted.enter(guardian);
            var boss = restarted.combat.getBoss().orElseThrow();
            int expected = (int) Math.round(au.edu.anu.comp2120.screen.GameConfig.SENTINEL.maxHp()
                    * au.edu.anu.comp2120.enemy.BossScaling.healthMultiplier(2));
            assertEquals(expected, boss.getHealth().getMaxHp());
            assertEquals(expected, boss.getHealth().getCurrentHp());
            assertEquals(1, boss.getPhase());
        }
    }

    /** Victory autosaves retain consumed stock while the Hub return restores only health. */
    @Test void victoryAutosaveRestoresRemainingPotions() {
        var game = new Game(directory);
        var player = game.session.getPlayer();
        player.getInventory().addItem(ItemCatalog.POTION, 3);
        game.chest.addItem(ItemCatalog.POTION, 7);
        player.takeDamage(50);
        assertTrue(player.useHealthPotion());
        game.win(BossChallenge.FIRE);
        var restarted = new Game(directory);
        assertTrue(restarted.saves.load(SaveSlot.AUTO), restarted.saves.status());
        assertEquals(2, restarted.session.getPlayer().getInventory().quantityOf(ItemCatalog.POTION));
        assertEquals(7, restarted.chest.quantityOf(ItemCatalog.POTION));
        assertEquals(restarted.session.getPlayer().getHealth().getMaxHp(),
                restarted.session.getPlayer().getHealth().getCurrentHp());
        assertFalse(restarted.session.getPlayer().useHealthPotion());
        assertEquals(2, restarted.session.getPlayer().getInventory().quantityOf(ItemCatalog.POTION));
    }

    /** Saves after consumption restore bag and chest counts separately, without replacing the player. */
    @Test void consumedPotionCountsSurviveSaveLoadAndRestart() {
        var game = new Game(directory);
        var player = game.session.getPlayer();
        var bag = player.getInventory();
        bag.addItem(ItemCatalog.POTION, 3);
        game.chest.addItem(ItemCatalog.POTION, 7);
        player.takeDamage(70);
        assertTrue(player.useHealthPotion());
        assertEquals(60, player.getHealth().getCurrentHp());
        assertTrue(game.saves.saveManual(), game.saves.status());
        assertTrue(player.useHealthPotion());
        game.chest.removeItem(ItemCatalog.POTION, 2);
        assertTrue(game.saves.load(SaveSlot.MANUAL), game.saves.status());
        assertSame(player, game.session.getPlayer());
        assertSame(bag, player.getInventory());
        assertEquals(2, bag.quantityOf(ItemCatalog.POTION));
        assertEquals(7, game.chest.quantityOf(ItemCatalog.POTION));
        assertEquals(60, player.getHealth().getCurrentHp());
        var restarted = new Game(directory);
        assertTrue(restarted.saves.load(SaveSlot.MANUAL), restarted.saves.status());
        assertEquals(2, restarted.session.getPlayer().getInventory().quantityOf(ItemCatalog.POTION));
        assertEquals(7, restarted.chest.quantityOf(ItemCatalog.POTION));
        assertTrue(restarted.session.getPlayer().useHealthPotion());
        assertEquals(90, restarted.session.getPlayer().getHealth().getCurrentHp());
        assertEquals(1, restarted.session.getPlayer().getInventory().quantityOf(ItemCatalog.POTION));
    }

    /** A defeated encounter restores HP but does not refund a consumed potion or create supplies. */
    @Test void defeatReturnAndRetryPreserveConsumedPotionCount() {
        var game = new Game(directory);
        var player = game.session.getPlayer();
        player.getInventory().addItem(ItemCatalog.POTION, 2);
        game.enter(BossChallenge.FIRE);
        player.takeDamage(60);
        assertTrue(player.useHealthPotion());
        assertEquals(70, player.getHealth().getCurrentHp());
        player.takeDamage(Integer.MAX_VALUE);
        assertFalse(player.useHealthPotion());
        game.combat.update(0, PlayerCommand.NONE, false);
        assertEquals(BattleResult.Outcome.FAILURE, game.flow.getLastResult().orElseThrow().outcome());
        assertEquals(MapCatalog.HUB, game.session.getCurrentMap().getId());
        assertSame(player, game.session.getPlayer());
        assertEquals(player.getHealth().getMaxHp(), player.getHealth().getCurrentHp());
        assertEquals(1, player.getInventory().quantityOf(ItemCatalog.POTION));
        assertFalse(player.useHealthPotion());
        game.travel.updatePortals();
        var portal = game.travel.getPortal(TravelCatalog.QUEST_PORTAL_ID).trigger();
        player.enterArea(game.session.getCurrentMap(), portal.x(), portal.y());
        assertTrue(game.travel.updatePortals());
        game.combat.update(0, PlayerCommand.NONE, false);
        assertTrue(game.combat.getBoss().isPresent());
        assertEquals(1, player.getInventory().quantityOf(ItemCatalog.POTION));
    }

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

    /** Debug victories settle rewards normally but cannot replace either normal save slot.
     * @throws IOException if save files cannot be read
     */
    @Test void debugVictoryPreservesBothSaveFiles() throws IOException {
        var game = new Game(directory);
        game.win(BossChallenge.FIRE);
        assertTrue(game.saves.saveManual());
        byte[] automatic = Files.readAllBytes(game.store.path(SaveSlot.AUTO));
        byte[] manual = Files.readAllBytes(game.store.path(SaveSlot.MANUAL));
        game.session.getPlayer().getDebugOptions().execute("oneshot on");
        game.enter(BossChallenge.FIRE);
        game.combat.getBoss().orElseThrow().getHealth().takeDamage(Integer.MAX_VALUE);
        game.combat.update(0, PlayerCommand.NONE, false);
        new InventoryRewards(game.flow, game.session.getPlayer().getInventory()).collect();
        game.quests.synchronize(); game.saves.afterSettlement();
        assertEquals(MapCatalog.HUB, game.session.getCurrentMap().getId());
        assertEquals(BattleResult.Outcome.SUCCESS, game.flow.getLastResult().orElseThrow().outcome());
        assertTrue(game.saves.status().contains("debug"));
        assertArrayEquals(automatic, Files.readAllBytes(game.store.path(SaveSlot.AUTO)));
        assertArrayEquals(manual, Files.readAllBytes(game.store.path(SaveSlot.MANUAL)));
        game.session.getPlayer().getDebugOptions().execute("debug off");
        assertFalse(game.saves.saveManual());
        assertTrue(game.saves.load(SaveSlot.MANUAL));
        assertTrue(game.session.getPlayer().getDebugOptions().blocksSaving());
    }

    /** Summaries use Sydney standard/daylight time without rewriting UTC timestamps.
     * @throws IOException if isolated save files cannot be inspected
     */
    @Test void summaryDisplaysSydneyTimeWithoutChangingFiles() throws IOException {
        var game = new Game(directory);
        assertTrue(game.saves.saveManual());
        String[][] cases = {
            {"2026-09-27T14:45:50.583510Z", "2026-09-28 00:45:50 AEST"},
            {"2026-12-01T14:45:50Z", "2026-12-02 01:45:50 AEDT"}
        };
        for (var example : cases) {
            var fields = new HashMap<>(game.store.load(SaveSlot.MANUAL));
            fields.put("metadata.savedAt", example[0]);
            game.store.save(SaveSlot.MANUAL, fields);
            byte[] before = Files.readAllBytes(game.store.path(SaveSlot.MANUAL));
            assertTrue(game.saves.summary(SaveSlot.MANUAL).contains(example[1]), game.saves.summary(SaveSlot.MANUAL));
            assertArrayEquals(before, Files.readAllBytes(game.store.path(SaveSlot.MANUAL)));
        }
    }

    /** Loading with no-CD enabled cannot resurrect hidden saved cooldowns when the toggle is disabled. */
    @Test void debugNoCooldownAppliesToRestoredTimers() {
        var game = new Game(directory);
        var timers = new EnumMap<AbilitySlot, Double>(AbilitySlot.class);
        for (var slot : AbilitySlot.values()) { timers.put(slot, 5.0); }
        game.combat.getAbilities().restoreCooldowns(timers);
        assertTrue(game.saves.saveManual());
        var options = game.session.getPlayer().getDebugOptions();
        options.execute("nocd");
        assertTrue(game.saves.load(SaveSlot.MANUAL));
        options.execute("nocd");
        for (var slot : AbilitySlot.values()) { assertEquals(0, game.combat.getAbilities().getCooldownRemaining(slot)); }
        assertTrue(options.blocksSaving());
        // Without no-CD, the original file retains its normal cooldown data.
        var fresh = new Game(directory); assertTrue(fresh.saves.load(SaveSlot.MANUAL));
        for (var slot : AbilitySlot.values()) { assertEquals(5, fresh.combat.getAbilities().getCooldownRemaining(slot)); }
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

    /** Overflow rewards survive restart and are claimable exactly once when space becomes available. */
    @Test
    void pendingOverflowRewardSurvivesReload() {
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
    /** Hood capacity validates against saved gear, retains exact HP and accepts old base-HP saves.
     * @throws IOException if isolated files cannot be inspected
     */
    @Test void hoodHealthRestoresAcrossEquipmentChangesAndOldSaves() throws IOException {
        var game = new Game(directory); var player = game.session.getPlayer();
        player.getInventory().addItem("guardian_hood+2"); player.getEquipment().toggle("guardian_hood+2");
        player.getHealth().takeDamage(5);
        assertTrue(game.saves.saveManual());
        player.getEquipment().toggle("guardian_hood+2");
        assertEquals(100, player.getHealth().getMaxHp());
        assertTrue(game.saves.load(SaveSlot.MANUAL), game.saves.status());
        assertEquals(140, player.getHealth().getMaxHp()); assertEquals(135, player.getHealth().getCurrentHp());
        var fields = new HashMap<>(game.store.load(SaveSlot.MANUAL));
        fields.put("location.maxHp", "100"); fields.put("location.hp", "90");
        game.store.save(SaveSlot.MANUAL, fields);
        assertTrue(game.saves.load(SaveSlot.MANUAL), game.saves.status());
        assertEquals(140, player.getHealth().getMaxHp()); assertEquals(90, player.getHealth().getCurrentHp());
        fields.put("location.maxHp", "150"); fields.put("location.hp", "145");
        game.store.save(SaveSlot.MANUAL, fields);
        assertFalse(game.saves.load(SaveSlot.MANUAL));
        assertEquals(90, player.getHealth().getCurrentHp());
        assertEquals("guardian_hood+2", player.getEquipment().itemAt(EquipmentSlot.HEAD));
    }

}
