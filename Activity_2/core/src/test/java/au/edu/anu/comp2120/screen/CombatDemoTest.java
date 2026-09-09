package au.edu.anu.comp2120.screen;

import static org.junit.jupiter.api.Assertions.*;

import au.edu.anu.comp2120.GameSession;
import au.edu.anu.comp2120.TravelSystem;
import au.edu.anu.comp2120.combat.Attack;
import au.edu.anu.comp2120.combat.Combatant;
import au.edu.anu.comp2120.combat.Faction;
import au.edu.anu.comp2120.enemy.Enemy;
import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.GameMap;
import au.edu.anu.comp2120.world.MapCatalog;
import au.edu.anu.comp2120.world.MapType;
import au.edu.anu.comp2120.world.TravelCatalog;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests the real debug adapter, map/travel cleanup, and shared player/enemy health without graphics.
 *
 * @author Daoyan Zhu (u7782042)
 */
final class CombatDemoTest {
    /** Creates an isolated demo test fixture. */
    CombatDemoTest() {
    }

    /** Both debug attack requests traverse the common pipeline; reset also clears active attacks. */
    @Test
    void demonstratesBothAttackDirectionsAndExplicitReset() {
        GameSession session = session(MapCatalog.TRAINING);
        CombatDemo demo = new CombatDemo(session);
        Enemy enemy = enemy(demo);
        CollisionBox position = session.getPlayer().getBounds();
        demo.update(0, true, false, false);
        assertEquals(75, enemy.getHealth().getCurrentHp());
        assertEquals(100, session.getPlayer().getHealth().getCurrentHp());
        assertEquals(1, demo.getActiveAttacks().size());
        demo.update(0, true, true, false);
        assertEquals(75, enemy.getHealth().getCurrentHp());
        assertEquals(75, session.getPlayer().getHealth().getCurrentHp());
        List<Attack> beforeReset = demo.getActiveAttacks();
        demo.update(0, true, true, true);
        assertTrue(demo.getActiveAttacks().isEmpty());
        assertTrue(beforeReset.stream().noneMatch(Attack::isActive));
        assertEquals(100, enemy.getHealth().getCurrentHp());
        assertEquals(100, session.getPlayer().getHealth().getCurrentHp());
        assertEquals(position, session.getPlayer().getBounds());
        demo.update(0, true, false, false);
        assertEquals(75, enemy.getHealth().getCurrentHp());
    }

    /** A dead player cannot attack until explicitly restored; input requests do not auto-revive. */
    @Test
    void deathPreventsNewAttacksAndResetRevives() {
        GameSession session = session(MapCatalog.TRAINING);
        CombatDemo demo = new CombatDemo(session);
        for (int i = 0; i < 4; i++) {
            demo.update(0.5, false, true, false);
        }
        assertFalse(session.getPlayer().isAlive());
        demo.update(0, true, false, false);
        assertEquals(100, enemy(demo).getHealth().getCurrentHp());
        assertTrue(demo.getStatus().contains("HP 0"));
        demo.update(0, false, false, true);
        assertTrue(session.getPlayer().isAlive());
    }

    /** Movement refreshes the inset hurtbox; an old fixed attack no longer follows a moving target. */
    @Test
    void updatesHurtboxAfterMovement() {
        GameSession session = session(MapCatalog.TRAINING);
        CombatDemo demo = new CombatDemo(session);
        session.getPlayer().updateMovement(0, 1, 1);
        demo.update(0, false, true, false);
        Combatant player = demo.getCombatants().get(0);
        CollisionBox body = session.getPlayer().getBounds();
        assertEquals(body.y() + GameConfig.COMBAT_HURTBOX_INSET, player.getHurtbox().y());
        assertTrue(player.getHurtbox().width() < body.width());
        assertEquals(100, session.getPlayer().getHealth().getCurrentHp());
    }

    /** Every built-in spawn supports a valid dummy in battle maps; Hub remains non-hostile. */
    @Test
    void placesDummySafelyForEveryBuiltInSpawn() {
        GameSession session = session(MapCatalog.HUB);
        CombatDemo demo = new CombatDemo(session);
        for (GameMap map : MapCatalog.createMaps()) {
            for (String spawn : map.getSpawnPoints().keySet()) {
                session.switchMap(map.getId(), spawn);
                demo.update(0, true, true, false);
                assertTrue(demo.getActiveAttacks().isEmpty());
                assertEquals(map.getType() == MapType.HUB ? 1 : 2, demo.getCombatants().size());
                for (Combatant actor : demo.getCombatants()) {
                    assertTrue(map.isWalkable(actor.getHurtbox()));
                }
                if (map.getType() == MapType.BATTLE) {
                    assertFalse(dummy(demo).getHurtbox().overlaps(session.getPlayer().getBounds()));
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
        demo.update(0, true, true, false);
        List<Attack> oldAttacks = demo.getActiveAttacks();
        Enemy previousEnemy = enemy(demo);
        travel.unlockDestination(TravelCatalog.CROSSROADS_ENTRY);
        travel.fastTravel(TravelCatalog.CROSSROADS_ENTRY);
        demo.update(0, true, true, false);
        assertEquals(75, session.getPlayer().getHealth().getCurrentHp());
        assertEquals(75, previousEnemy.getHealth().getCurrentHp());
        assertEquals(100, enemy(demo).getHealth().getCurrentHp());
        assertNotSame(previousEnemy, enemy(demo));
        assertTrue(oldAttacks.stream().noneMatch(Attack::isActive));
        assertTrue(demo.getActiveAttacks().isEmpty());
    }

    /** Same-map re-entry still clears combat; a failed transition does not clear a valid arena. */
    @Test
    void usesTransitionStampRatherThanOnlyMapId() {
        GameSession session = session(MapCatalog.TRAINING);
        CombatDemo demo = new CombatDemo(session);
        demo.update(0, true, false, false);
        Attack attack = demo.getActiveAttacks().get(0);
        assertThrows(IllegalArgumentException.class, () -> session.switchMap("missing"));
        demo.update(0, false, false, false);
        assertTrue(attack.isActive());
        session.switchMap(MapCatalog.TRAINING);
        demo.update(0, false, false, false);
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
        demo.update(0, true, false, false);
        Attack attack = demo.getActiveAttacks().get(0);
        session.getPlayer().updateMovement(1, 0, 0.55);
        assertTrue(travel.updatePortals());
        demo.update(0, true, true, false);
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
    @ValueSource(doubles = {-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsInvalidTimeBeforeMutation(double delta) {
        GameSession session = session(MapCatalog.TRAINING);
        CombatDemo demo = new CombatDemo(session);
        session.getPlayer().takeDamage(25);
        assertThrows(IllegalArgumentException.class, () -> demo.update(delta, true, true, true));
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
     * Retrieves the battle-map dummy adapter.
     * @param demo initialized battle-map demonstration
     * @return hostile adapter
     * @throws java.util.NoSuchElementException if no dummy is present
     */
    private Combatant dummy(CombatDemo demo) {
        return demo.getCombatants().stream().filter(actor -> actor.getFaction() == Faction.ENEMY)
                .findFirst().orElseThrow();
    }

    /**
     * Retrieves the actual health-owning enemy used by the debug adapter.
     * @param demo initialized battle-map demonstration
     * @return existing dummy enemy
     * @throws java.util.NoSuchElementException if no dummy is present
     */
    private Enemy enemy(CombatDemo demo) {
        return (Enemy) dummy(demo).getEntity();
    }
}
