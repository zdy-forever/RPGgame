package au.edu.anu.comp2120.screen;

import static org.junit.jupiter.api.Assertions.*;

import au.edu.anu.comp2120.EncounterFlow;
import au.edu.anu.comp2120.GameSession;
import au.edu.anu.comp2120.TravelSystem;
import au.edu.anu.comp2120.npc.ClassTrainer;
import au.edu.anu.comp2120.player.AbilitySlot;
import au.edu.anu.comp2120.player.PlayerClass;
import au.edu.anu.comp2120.player.PlayerCommand;
import au.edu.anu.comp2120.player.UltimateMode;
import au.edu.anu.comp2120.world.MapCatalog;
import au.edu.anu.comp2120.world.TravelCatalog;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Checks the trainer against the real production controller without requiring a graphics context.
 * @author Daoyan Zhu (u7782042)
 */
final class ClassTrainerTest {
    /** Persistent Hub fixture. */
    private final GameSession session = new GameSession(MapCatalog.createMaps(), MapCatalog.HUB, 28, 28, 180);
    /** Actual travel boundary, independent of NPC configuration. */
    private final TravelSystem travel = new TravelSystem(session, TravelCatalog.createQuestPortals(),
            TravelCatalog.createDestinations());
    /** Production encounter admission rules. */
    private final EncounterFlow flow = new EncounterFlow(session, travel, List.of(TravelCatalog.QUEST_PORTAL_ID));
    /** Shared existing ability/combat coordinator. */
    private final CombatDemo demo = new CombatDemo(session, flow);
    /** Same NPC selection adapter used by GameScreen. */
    private final ClassTrainer trainer = new ClassTrainer(session, demo.getAbilities());

    /** Creates a fresh live-Hub fixture for each test. */
    ClassTrainerTest() { }

    /**
     * Every profession can be selected without replacing, healing or moving the player.
     * @param type requested profession
     */
    @ParameterizedTest
    @EnumSource(PlayerClass.class)
    void selectsClassAndBothModes(PlayerClass type) {
        var player = session.getPlayer();
        var health = player.getHealth();
        var bounds = player.getBounds();
        player.takeDamage(17);
        trainer.select(ClassTrainer.Choice.valueOf(type.name()));
        assertEquals(type, player.getPlayerClass());
        assertTrue(trainer.select(ClassTrainer.Choice.MODE_B));
        assertEquals(UltimateMode.B, player.getUltimateMode());
        assertFalse(trainer.select(ClassTrainer.Choice.MODE_B));
        assertTrue(trainer.select(ClassTrainer.Choice.MODE_A));
        assertEquals(UltimateMode.A, player.getUltimateMode());
        assertSame(player, session.getPlayer());
        assertSame(health, player.getHealth());
        assertEquals(83, health.getCurrentHp());
        assertEquals(bounds, player.getBounds());
        assertTrue(GameConfig.HUB_NPCS.contains(GameConfig.CLASS_MASTER));
    }

    /** Profession changes remember each profession's separately selected ultimate. */
    @Test
    void remembersEachClassModeAndLabels() {
        trainer.select(ClassTrainer.Choice.ARCHER);
        trainer.select(ClassTrainer.Choice.MODE_B);
        assertTrue(trainer.label(ClassTrainer.Choice.MODE_A).contains("Arrow Rain"));
        assertTrue(trainer.label(ClassTrainer.Choice.MODE_B).contains("Untargetable"));
        trainer.select(ClassTrainer.Choice.MAGE);
        assertEquals(UltimateMode.A, session.getPlayer().getUltimateMode());
        assertTrue(trainer.label(ClassTrainer.Choice.MODE_A).contains("Magic Circle"));
        assertTrue(trainer.label(ClassTrainer.Choice.MODE_B).contains("Barrage"));
        trainer.select(ClassTrainer.Choice.KATANA);
        trainer.select(ClassTrainer.Choice.MODE_B);
        assertTrue(trainer.label(ClassTrainer.Choice.MODE_A).contains("Watermoon"));
        assertTrue(trainer.label(ClassTrainer.Choice.MODE_B).contains("Helmbreaker"));
        trainer.select(ClassTrainer.Choice.ARCHER);
        assertEquals(UltimateMode.B, session.getPlayer().getUltimateMode());
        trainer.select(ClassTrainer.Choice.MAGE);
        assertEquals(UltimateMode.A, session.getPlayer().getUltimateMode());
        trainer.select(ClassTrainer.Choice.KATANA);
        assertEquals(UltimateMode.B, session.getPlayer().getUltimateMode());
    }

    /**
     * Both non-Hub map types reject all choices and preserve remembered configuration after travel.
     * @param mapId Training or Battle map
     */
    @ParameterizedTest
    @ValueSource(strings = {MapCatalog.TRAINING, MapCatalog.CROSSROADS})
    void rejectsOutsideHub(String mapId) {
        trainer.select(ClassTrainer.Choice.KATANA);
        trainer.select(ClassTrainer.Choice.MODE_B);
        session.switchMap(mapId);
        demo.update(0, PlayerCommand.NONE, false);
        for (var choice : ClassTrainer.Choice.values()) {
            assertFalse(trainer.select(choice));
        }
        assertEquals(PlayerClass.KATANA, session.getPlayer().getPlayerClass());
        assertEquals(UltimateMode.B, session.getPlayer().getUltimateMode());
        session.switchMap(MapCatalog.HUB);
        demo.update(0, PlayerCommand.NONE, false);
        assertTrue(trainer.select(ClassTrainer.Choice.MODE_A));
    }

    /**
     * A dead player cannot use any trainer choice to restore or change gameplay state.
     * @param choice requested option
     */
    @ParameterizedTest
    @EnumSource(ClassTrainer.Choice.class)
    void rejectsDeadPlayer(ClassTrainer.Choice choice) {
        var type = session.getPlayer().getPlayerClass();
        session.getPlayer().takeDamage(100);
        assertFalse(trainer.select(choice));
        assertFalse(session.getPlayer().isAlive());
        assertEquals(type, session.getPlayer().getPlayerClass());
        assertEquals(UltimateMode.A, session.getPlayer().getUltimateMode());
    }

    /** Active attacks prevent selection, and successful later selection does not reset cooldowns. */
    @Test
    void cannotCancelAttacksOrRefundCooldowns() {
        trainer.select(ClassTrainer.Choice.KATANA);
        demo.update(0, new PlayerCommand(0, 0, true, false, false, false), false);
        assertFalse(trainer.select(ClassTrainer.Choice.MAGE));
        assertFalse(trainer.select(ClassTrainer.Choice.MODE_B));
        demo.update(0.2, PlayerCommand.NONE, false);
        double remaining = demo.getAbilities().getCooldownRemaining(AbilitySlot.LIGHT);
        assertTrue(remaining > 0);
        assertTrue(trainer.select(ClassTrainer.Choice.MAGE));
        assertEquals(remaining, demo.getAbilities().getCooldownRemaining(AbilitySlot.LIGHT));
        assertTrue(trainer.select(ClassTrainer.Choice.MODE_B));
        assertEquals(remaining, demo.getAbilities().getCooldownRemaining(AbilitySlot.LIGHT));
    }

    /** Missing choices and dependencies fail explicitly without mutation. */
    @Test
    void rejectsNullInputs() {
        assertThrows(NullPointerException.class, () -> trainer.select(null));
        assertThrows(NullPointerException.class, () -> trainer.label(null));
        assertThrows(NullPointerException.class, () -> new ClassTrainer(null, demo.getAbilities()));
        assertThrows(NullPointerException.class, () -> new ClassTrainer(session, null));
    }
}
