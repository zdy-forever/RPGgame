package au.edu.anu.comp2120.screen;

import static org.junit.jupiter.api.Assertions.*;

import au.edu.anu.comp2120.GameSession;
import au.edu.anu.comp2120.TravelSystem;
import au.edu.anu.comp2120.combat.Attack;
import au.edu.anu.comp2120.combat.Combatant;
import au.edu.anu.comp2120.combat.Faction;
import au.edu.anu.comp2120.enemy.Enemy;
import au.edu.anu.comp2120.enemy.EnemyAI;
import au.edu.anu.comp2120.enemy.EnemyState;
import au.edu.anu.comp2120.player.AbilitySlot;
import au.edu.anu.comp2120.player.AbilityState;
import au.edu.anu.comp2120.player.PlayerCommand;
import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.GameMap;
import au.edu.anu.comp2120.world.MapCatalog;
import au.edu.anu.comp2120.world.MapType;
import au.edu.anu.comp2120.world.SpawnPoint;
import au.edu.anu.comp2120.world.TravelCatalog;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tests the real debug adapter, map/travel cleanup, and shared player/enemy health without graphics.
 *
 * @author Daoyan Zhu (u7782042)
 */
final class CombatDemoTest {
    /** Creates an isolated demo test fixture. */
    CombatDemoTest() {
    }

    /**
     * Actual session travel cancels active slots and discards inputs without granting fresh cooldowns.
     * @param slot slot active before transition
     */
    @ParameterizedTest
    @EnumSource(AbilitySlot.class)
    void mapTransitionPreservesAbilityCooldownsAndCancelsActivity(AbilitySlot slot) {
        GameSession session = session(MapCatalog.HUB);
        CombatDemo demo = new CombatDemo(session);
        PlayerCommand request = new PlayerCommand(0, 0, slot == AbilitySlot.LIGHT,
                slot == AbilitySlot.SPECIAL, slot == AbilitySlot.DASH, slot == AbilitySlot.ULTIMATE);
        demo.update(0, request, false);
        double cooldown = demo.getAbilities().getCooldownRemaining(slot);
        List<Attack> previous = demo.getActiveAttacks();
        session.switchMap(MapCatalog.TRAINING);
        CollisionBox spawn = session.getPlayer().getBounds();
        demo.update(0, new PlayerCommand(1, 1, true, true, true, true), false);
        assertEquals(spawn, session.getPlayer().getBounds());
        assertEquals(cooldown, demo.getAbilities().getCooldownRemaining(slot));
        assertEquals(AbilityState.COOLDOWN, demo.getAbilities().getState(slot));
        assertTrue(previous.stream().noneMatch(Attack::isActive));
        assertTrue(demo.getActiveAttacks().isEmpty());
        demo.update(0.1, PlayerCommand.NONE, false);
        assertEquals(spawn, session.getPlayer().getBounds());
        assertEquals(cooldown - 0.1, demo.getAbilities().getCooldownRemaining(slot), 1e-9);
        demo.update(0, request, true);
        assertEquals(AbilityState.READY, demo.getAbilities().getState(slot));
        assertEquals(spawn, session.getPlayer().getBounds());
    }

    /** The real demo routes all attack slots through combat and prevents enemy damage during no-time resets. */
    @Test
    void commonAbilityCommandsReachDemoCombat() {
        CombatDemo demo = new CombatDemo(session(MapCatalog.TRAINING));
        demo.update(0, new PlayerCommand(0, 0, false, true, false, false), false);
        assertEquals(60, enemy(demo).getHealth().getCurrentHp());
        demo.update(0, new PlayerCommand(0, 0, false, false, false, true), false);
        assertFalse(enemy(demo).isAlive());
        assertEquals(EnemyState.DEAD, demo.getEnemies().get(0).getState());
        demo.update(0, PlayerCommand.NONE, true);
        assertEquals(100, enemy(demo).getHealth().getCurrentHp());
        for (AbilitySlot slot : AbilitySlot.values()) {
            assertEquals(AbilityState.READY, demo.getAbilities().getState(slot));
        }
    }

    /** Player requests and autonomous enemy attacks share the pipeline; reset clears all attacks. */
    @Test
    void demonstratesBothAttackDirectionsAndExplicitReset() {
        GameSession session = session(MapCatalog.TRAINING);
        CombatDemo demo = new CombatDemo(session);
        Enemy enemy = enemy(demo);
        CollisionBox position = session.getPlayer().getBounds();
        demo.update(0, true, false);
        assertEquals(75, enemy.getHealth().getCurrentHp());
        assertEquals(100, session.getPlayer().getHealth().getCurrentHp());
        assertEquals(1, demo.getActiveAttacks().size());
        demo.update(0.65, false, false);
        assertEquals(75, enemy.getHealth().getCurrentHp());
        assertEquals(80, session.getPlayer().getHealth().getCurrentHp());
        demo.update(0, true, false);
        List<Attack> beforeReset = demo.getActiveAttacks();
        demo.update(0, true, true);
        assertTrue(demo.getActiveAttacks().isEmpty());
        assertTrue(beforeReset.stream().noneMatch(Attack::isActive));
        assertEquals(100, enemy.getHealth().getCurrentHp());
        assertEquals(100, session.getPlayer().getHealth().getCurrentHp());
        assertEquals(position, session.getPlayer().getBounds());
        for (EnemyAI ai : demo.getEnemies()) {
            assertEquals(EnemyState.IDLE, ai.getState());
            assertEquals(100, ai.getEnemy().getHealth().getCurrentHp());
        }
        demo.update(0, true, false);
        assertEquals(75, enemy.getHealth().getCurrentHp());
    }

    /** A dead player cannot attack until explicitly restored; input requests do not auto-revive. */
    @Test
    void deathPreventsNewAttacksAndResetRevives() {
        GameSession session = session(MapCatalog.TRAINING);
        CombatDemo demo = new CombatDemo(session);
        for (int i = 0; i < 100; i++) {
            demo.update(0.1, false, false);
        }
        assertFalse(session.getPlayer().isAlive());
        demo.update(0, true, false);
        assertEquals(100, enemy(demo).getHealth().getCurrentHp());
        assertTrue(demo.getStatus().contains("HP 0"));
        demo.update(0, false, true);
        assertTrue(session.getPlayer().isAlive());
    }

    /** Movement refreshes the player's inset hurtbox before AI evaluates the target. */
    @Test
    void updatesHurtboxAfterMovement() {
        GameSession session = session(MapCatalog.TRAINING);
        CombatDemo demo = new CombatDemo(session);
        session.getPlayer().updateMovement(0, 1, 1);
        demo.update(0, false, false);
        Combatant player = demo.getCombatants().get(0);
        CollisionBox body = session.getPlayer().getBounds();
        assertEquals(body.y() + GameConfig.COMBAT_HURTBOX_INSET, player.getHurtbox().y());
        assertTrue(player.getHurtbox().width() < body.width());
        assertEquals(100, session.getPlayer().getHealth().getCurrentHp());
    }

    /** Every built-in spawn supports multiple valid enemies in battle maps; Hub remains non-hostile. */
    @Test
    void placesEnemiesSafelyForEveryBuiltInSpawn() {
        GameSession session = session(MapCatalog.HUB);
        CombatDemo demo = new CombatDemo(session);
        for (GameMap map : MapCatalog.createMaps()) {
            for (String spawn : map.getSpawnPoints().keySet()) {
                session.switchMap(map.getId(), spawn);
                demo.update(0, true, false);
                assertTrue(demo.getActiveAttacks().isEmpty());
                assertEquals(map.getType() == MapType.HUB ? 1 : 1 + GameConfig.ENEMY_COUNT,
                        demo.getCombatants().size());
                for (Combatant actor : demo.getCombatants()) {
                    assertTrue(map.isWalkable(actor.getHurtbox()));
                }
                if (map.getType() == MapType.BATTLE) {
                    List<EnemyAI> enemies = demo.getEnemies();
                    for (int i = 0; i < enemies.size(); i++) {
                        assertFalse(enemies.get(i).getBounds().overlaps(session.getPlayer().getBounds()));
                        for (int j = 0; j < i; j++) {
                            assertFalse(enemies.get(i).getBounds().overlaps(enemies.get(j).getBounds()));
                        }
                    }
                }
            }
        }
    }

    /** Actual fast travel cancels retained attacks, preserves player HP, and creates a fresh arena. */
    @Test
    void fastTravelCannotCarryAnAttackIntoAnotherMap() {
        GameSession session = session(MapCatalog.TRAINING);
        CombatDemo demo = new CombatDemo(session);
        TravelSystem travel = new TravelSystem(session, TravelCatalog.createPortals(),
                TravelCatalog.createDestinations());
        demo.update(0, true, false);
        session.getPlayer().takeDamage(25);
        List<Attack> oldAttacks = demo.getActiveAttacks();
        Enemy previousEnemy = enemy(demo);
        EnemyAI previousAI = demo.getEnemies().get(0);
        CollisionBox previousPosition = previousAI.getBounds();
        travel.unlockDestination(TravelCatalog.CROSSROADS_ENTRY);
        travel.fastTravel(TravelCatalog.CROSSROADS_ENTRY);
        demo.update(0, true, false);
        assertEquals(75, session.getPlayer().getHealth().getCurrentHp());
        assertEquals(75, previousEnemy.getHealth().getCurrentHp());
        assertEquals(100, enemy(demo).getHealth().getCurrentHp());
        assertNotSame(previousEnemy, enemy(demo));
        assertTrue(oldAttacks.stream().noneMatch(Attack::isActive));
        assertTrue(demo.getActiveAttacks().isEmpty());
        previousAI.update(demo.getCombatants().get(0), 1);
        assertEquals(previousPosition, previousAI.getBounds());
    }

    /** A small battle map with no safe candidate produces no enemies rather than an invalid spawn. */
    @Test
    void skipsEnemyPlacementWhenNoCandidateFits() {
        GameMap small = new GameMap("small", "Small", MapType.BATTLE, 60, 60, List.of(),
                Map.of("entry", new SpawnPoint(0, 0)), "entry");
        GameSession session = new GameSession(List.of(small), "small", 28, 28, 180);
        CombatDemo demo = new CombatDemo(session);
        demo.update(1, false, false);
        assertTrue(demo.getEnemies().isEmpty());
        assertEquals(1, demo.getCombatants().size());
        assertEquals(100, session.getPlayer().getHealth().getCurrentHp());
        assertThrows(UnsupportedOperationException.class, () -> demo.getEnemies().clear());
    }

    /** Same-map re-entry still clears combat; a failed transition does not clear a valid arena. */
    @Test
    void usesTransitionStampRatherThanOnlyMapId() {
        GameSession session = session(MapCatalog.TRAINING);
        CombatDemo demo = new CombatDemo(session);
        demo.update(0, true, false);
        Attack attack = demo.getActiveAttacks().get(0);
        assertThrows(IllegalArgumentException.class, () -> session.switchMap("missing"));
        demo.update(0, false, false);
        assertTrue(attack.isActive());
        session.switchMap(MapCatalog.TRAINING);
        demo.update(0, false, false);
        assertFalse(attack.isActive());
        assertEquals(100, enemy(demo).getHealth().getCurrentHp());
    }

    /** A portal transition uses the same cleanup path and does not automatically heal the player. */
    @Test
    void portalArrivalClearsPendingCombat() {
        GameSession session = session(MapCatalog.HUB);
        CombatDemo demo = new CombatDemo(session);
        TravelSystem travel = new TravelSystem(session, TravelCatalog.createPortals(),
                TravelCatalog.createDestinations());
        session.getPlayer().takeDamage(20);
        demo.update(0, true, false);
        Attack attack = demo.getActiveAttacks().get(0);
        session.getPlayer().updateMovement(1, 0, 0.55);
        assertTrue(travel.updatePortals());
        demo.update(0, true, false);
        assertEquals(MapCatalog.TRAINING, session.getCurrentMap().getId());
        assertFalse(attack.isActive());
        assertEquals(80, session.getPlayer().getHealth().getCurrentHp());
        assertEquals(100, enemy(demo).getHealth().getCurrentHp());
    }

    /**
     * Invalid demo time cannot consume input, reset health, or settle damage.
     * @param delta rejected simulation time
     */
    @ParameterizedTest
    @ValueSource(doubles = {-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.MAX_VALUE})
    void rejectsInvalidTimeBeforeMutation(double delta) {
        GameSession session = session(MapCatalog.TRAINING);
        CombatDemo demo = new CombatDemo(session);
        session.getPlayer().takeDamage(25);
        assertThrows(IllegalArgumentException.class, () -> demo.update(delta, true, true));
        assertEquals(75, session.getPlayer().getHealth().getCurrentHp());
        assertTrue(demo.getActiveAttacks().isEmpty());
    }

    /** Missing session input is rejected immediately. */
    @Test
    void rejectsNullSession() {
        assertThrows(NullPointerException.class, () -> new CombatDemo(null));
    }

    /**
     * Creates the existing map/player coordinator using normal demo geometry.
     * @param mapId built-in starting map
     * @return independent session at its default spawn
     */
    private GameSession session(String mapId) {
        return new GameSession(MapCatalog.createMaps(), mapId, GameConfig.PLAYER_WIDTH,
                GameConfig.PLAYER_HEIGHT, GameConfig.PLAYER_SPEED);
    }

    /**
     * Retrieves the first battle-map enemy adapter.
     * @param demo initialized battle-map demonstration
     * @return hostile adapter
     * @throws java.util.NoSuchElementException if no enemy is present
     */
    private Combatant firstEnemyActor(CombatDemo demo) {
        return demo.getCombatants().stream().filter(actor -> actor.getFaction() == Faction.ENEMY)
                .findFirst().orElseThrow();
    }

    /**
     * Retrieves the actual health-owning enemy used by the debug adapter.
     * @param demo initialized battle-map demonstration
     * @return existing first enemy
     * @throws java.util.NoSuchElementException if no enemy is present
     */
    private Enemy enemy(CombatDemo demo) {
        return (Enemy) firstEnemyActor(demo).getEntity();
    }
}
