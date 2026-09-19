package au.edu.anu.comp2120.screen;

import static org.junit.jupiter.api.Assertions.*;

import au.edu.anu.comp2120.GameSession;
import au.edu.anu.comp2120.combat.Attack;
import au.edu.anu.comp2120.enemy.Boss;
import au.edu.anu.comp2120.enemy.BossState;
import au.edu.anu.comp2120.inventory.MaterialDrop;
import au.edu.anu.comp2120.player.PlayerClass;
import au.edu.anu.comp2120.player.PlayerCommand;
import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.MapCatalog;
import au.edu.anu.comp2120.world.GameMap;
import au.edu.anu.comp2120.world.MapType;
import au.edu.anu.comp2120.world.SpawnPoint;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Exercises actual map/demo integration, shared player attacks, resets and encounter-scoped loot.
 * @author Daoyan Zhu (u7782042)
 */
final class BossDemoTest {
    /** Existing map coordinator with unchanged player defaults. */
    private final GameSession session = new GameSession(MapCatalog.createMaps(), MapCatalog.CROSSROADS, 28, 28, 180);
    /** Production coordinator, not a separate Boss-only game loop. */
    private final CombatDemo demo = new CombatDemo(session);

    /** Creates an isolated encounter demonstration. */
    BossDemoTest() {
    }

    /** All five arenas and the stationary training guardian initially share the exact map centre. */
    @Test
    void builtInBossAndPracticeTargetsSpawnAtMapCentre() {
        for (GameMap map : MapCatalog.createMaps()) {
            if (map.getType() != MapType.BATTLE && map.getType() != MapType.TRAINING) { continue; }
            GameSession current = new GameSession(List.of(map),map.getId(),28,28,180);
            CombatDemo encounter = new CombatDemo(current);
            CollisionBox body = map.getType() == MapType.TRAINING
                    ? encounter.getTrainingDummy().orElseThrow().getCombatant().getHurtbox()
                    : encounter.getBoss().orElseThrow().getBounds();
            assertEquals(800,body.x()+body.width()/2,map.getId());
            assertEquals(600,body.y()+body.height()/2,map.getId());
            assertTrue(map.isWalkable(body));
        }
    }

    /**
     * Centre placement adapts to each map's dimensions instead of hard-coding the built-in centre.
     * @param width map width
     * @param height map height
     */
    @ParameterizedTest
    @CsvSource({"600,400", "900,600", "1600,1200"})
    void centreTracksMapDimensions(double width, double height) {
        GameMap map = new GameMap("custom","Custom",MapType.BATTLE,width,height,List.of(),
                Map.of("entry",new SpawnPoint(0,0)),"entry");
        CombatDemo encounter = new CombatDemo(new GameSession(List.of(map),map.getId(),28,28,180));
        CollisionBox body = encounter.getBoss().orElseThrow().getBounds();
        assertEquals(width/2,body.x()+body.width()/2);
        assertEquals(height/2,body.y()+body.height()/2);
    }

    /** A custom centre wall still cannot receive an invalid Boss; nearby safety fallback is retained. */
    @Test
    void blockedCentreUsesSafeNearbyPlacement() {
        CollisionBox wall = new CollisionBox(468,268,64,64);
        GameMap map = new GameMap("blocked","Blocked centre",MapType.BATTLE,1000,600,List.of(wall),
                Map.of("entry",new SpawnPoint(0,0)),"entry");
        CombatDemo encounter = new CombatDemo(new GameSession(List.of(map),map.getId(),28,28,180));
        CollisionBox body = encounter.getBoss().orElseThrow().getBounds();
        assertFalse(body.overlaps(wall));
        assertTrue(map.isWalkable(body));
    }

    /** Each battle map gets one Boss with safe large geometry; Hub has no encounter. */
    @Test
    void encounterUsesExistingMapAndCombatRegistration() {
        Boss boss = demo.getBoss().orElseThrow();
        assertEquals(64, boss.getBounds().width());
        assertTrue(session.getCurrentMap().isWalkable(boss.getBounds()));
        assertFalse(boss.getBounds().overlaps(session.getPlayer().getBounds()));
        assertTrue(demo.getCombatants().contains(boss.getCombatant()));
        assertTrue(demo.getStatus().contains("Sentinel 400/400 P1"));
        for (String map : List.of(MapCatalog.HUB, MapCatalog.TRAINING)) {
            session.switchMap(map);
            demo.update(0, false, false);
            assertTrue(demo.getBoss().isEmpty());
            assertEquals(map.equals(MapCatalog.TRAINING), demo.getTrainingDummy().isPresent());
            assertTrue(demo.getEnemies().isEmpty());
            assertEquals(map.equals(MapCatalog.HUB) ? 1 : 2, demo.getCombatants().size());
            assertTrue(demo.getDrops().isEmpty());
        }
    }

    /** An actual player ultimate reaches Boss health; a later melee cast triggers defeat and loot in the same frame. */
    @Test
    void playerAbilitiesAndPostCombatOutcomeAreIntegrated() {
        Boss boss = demo.getBoss().orElseThrow();
        demo.update(0, PlayerCommand.NONE, false, PlayerClass.KATANA);
        session.getPlayer().enterArea(session.getCurrentMap(),
                demo.getBoss().orElseThrow().getBounds().x() - 40, demo.getBoss().orElseThrow().getBounds().y());
        boss.getHealth().takeDamage(180);
        demo.update(0, new PlayerCommand(0, 0, false, false, false, true), false);
        demo.update(0, PlayerCommand.NONE, false);
        assertEquals(120, boss.getHealth().getCurrentHp());
        assertEquals(2, boss.getPhase());
        assertEquals(BossState.PHASE_TRANSITION, boss.getState());
        boss.getHealth().takeDamage(100);
        demo.update(0, new PlayerCommand(1, 0, true, false, false, false), false);
        assertEquals(BossState.DEAD, boss.getState());
        assertEquals(1, demo.getDrops().size());
        assertTrue(demo.getStatus().contains("sentinel-core x1"));
        for (int i = 0; i < 50; i++) {
            demo.update(0, false, false);
        }
        assertEquals(1, demo.getDrops().size());
    }

    /** Debug reset creates a new encounter and reward identity, rather than reviving an already-rewarded Boss. */
    @Test
    void resetDiscardsOldLootAndCreatesFreshEncounter() {
        Boss old = demo.getBoss().orElseThrow();
        old.getHealth().takeDamage(400);
        demo.update(0, false, false);
        MaterialDrop first = demo.getDrops().get(0);
        demo.update(0, true, true);
        Boss fresh = demo.getBoss().orElseThrow();
        assertNotSame(old, fresh);
        assertEquals(400, fresh.getHealth().getCurrentHp());
        assertEquals(1, fresh.getPhase());
        assertTrue(demo.getDrops().isEmpty());
        assertTrue(old.claimDrop().isEmpty());
        assertFalse(demo.getCombatants().contains(old.getCombatant()));
        fresh.getHealth().takeDamage(400);
        demo.update(0, false, false);
        assertNotEquals(first.id(), demo.getDrops().get(0).id());
    }

    /** Travel cancels old attacks and loot, and returning creates a new encounter without a second map manager. */
    @Test
    void transitionInvalidatesOldBossAndPendingAttacks() {
        Boss old = demo.getBoss().orElseThrow();
        session.getPlayer().enterArea(session.getCurrentMap(),
                demo.getBoss().orElseThrow().getBounds().x() - 40, demo.getBoss().orElseThrow().getBounds().y());
        demo.update(0, false, false);
        List<Attack> attacks = demo.getActiveAttacks();
        assertTrue(attacks.stream().anyMatch(attack -> attack.getSource() == old.getCombatant()));
        CollisionBox before = old.getBounds();
        session.switchMap(MapCatalog.HUB);
        demo.update(0, true, false);
        assertTrue(attacks.stream().noneMatch(Attack::isActive));
        assertTrue(demo.getBoss().isEmpty());
        old.update(demo.getCombatants().get(0), 1);
        assertEquals(before, old.getBounds());
        old.getHealth().takeDamage(400);
        old.synchronizeOutcome();
        assertTrue(old.getDrop().isEmpty());
        session.switchMap(MapCatalog.CROSSROADS);
        demo.update(0, false, false);
        assertNotSame(old, demo.getBoss().orElseThrow());
    }

    /**
     * Reset placement avoids the player from either named spawn of the current Battle map.
     * @param spawnId built-in Battle spawn
     */
    @ParameterizedTest
    @ValueSource(strings = {"entry", "east"})
    void resetUsesSafeFallbackPlacement(String spawnId) {
        session.switchMap(MapCatalog.CROSSROADS, spawnId);
        demo.update(0, PlayerCommand.NONE, false);
        CollisionBox previous = demo.getBoss().orElseThrow().getBounds();
        session.getPlayer().enterArea(session.getCurrentMap(), previous.x(), previous.y());
        demo.update(0, false, true);
        Boss fresh = demo.getBoss().orElseThrow();
        assertNotEquals(previous, fresh.getBounds());
        assertFalse(fresh.getBounds().overlaps(session.getPlayer().getBounds()));
        assertTrue(session.getCurrentMap().isWalkable(fresh.getBounds()));
    }
}
