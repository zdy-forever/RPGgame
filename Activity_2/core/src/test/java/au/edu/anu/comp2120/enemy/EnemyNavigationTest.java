package au.edu.anu.comp2120.enemy;

import static org.junit.jupiter.api.Assertions.*;

import au.edu.anu.comp2120.combat.AttackSpec;
import au.edu.anu.comp2120.combat.Combatant;
import au.edu.anu.comp2120.combat.CombatSystem;
import au.edu.anu.comp2120.combat.Faction;
import au.edu.anu.comp2120.player.Player;
import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.GameMap;
import au.edu.anu.comp2120.world.MapCatalog;
import au.edu.anu.comp2120.world.TestMaps;
import au.edu.anu.comp2120.world.WalkableArea;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Exercises A* pursuit through the existing movement, combat, and health pipeline over many updates.
 *
 * @author Daoyan Zhu (u7782042)
 */
final class EnemyNavigationTest {
    /** Wide tracking radius isolates route behaviour from deliberate target-loss rules. */
    private final EnemyConfig config = new EnemyConfig(100, 1500, 2000, 30,
            new AttackSpec("navigation-test", 10, 0.2, 1), 16, 0.5);

    /** Creates the navigation integration fixture. */
    EnemyNavigationTest() {
    }

    /**
     * Actual AI can detour around a partial wall and attack, independent of update size.
     * @param delta simulation step, including a step spanning many waypoints
     */
    @ParameterizedTest
    @ValueSource(doubles = {0.016, 0.1, 1, 100})
    void followsDetourWithoutOverspeedOrWallPenetration(double delta) {
        GameMap map = TestMaps.rectangle(320, 240, List.of(new CollisionBox(120, 0, 8, 160)));
        CombatSystem combat = new CombatSystem();
        Player player = new Player(new CollisionBox(200, 40, 20, 20), 100, map);
        Combatant target = new Combatant(player, Faction.PLAYER, player.getBounds());
        combat.register(target);
        EnemyAI ai = new EnemyAI(new Enemy(), new CollisionBox(40, 40, 20, 20), map, config, combat);
        for (int i = 0; i < 2000 && ai.getState() != EnemyState.ATTACK; i++) {
            CollisionBox before = ai.getBounds();
            ai.update(target, delta);
            assertTrue(map.isWalkable(ai.getBounds()));
            assertTrue(Math.hypot(ai.getBounds().x() - before.x(), ai.getBounds().y() - before.y())
                    <= config.speed() * delta + 1e-8);
            combat.update(delta);
        }
        assertEquals(EnemyState.ATTACK, ai.getState());
        assertEquals(90, player.getHealth().getCurrentHp());
        assertTrue(ai.getBounds().x() >= 128);
    }

    /** Standing near a target on the other side of a thin wall does not trigger an attack through it. */
    @Test
    void nearbyTargetAcrossWallRequiresDetour() {
        GameMap map = TestMaps.rectangle(300, 240, List.of(new CollisionBox(124, 0, 4, 160)));
        CombatSystem combat = new CombatSystem();
        Player player = new Player(new CollisionBox(130, 100, 20, 20), 100, map);
        Combatant target = new Combatant(player, Faction.PLAYER, player.getBounds());
        combat.register(target);
        EnemyAI ai = new EnemyAI(new Enemy(), new CollisionBox(100, 100, 20, 20), map, config, combat);
        ai.update(target, 0);
        combat.update(0);
        assertEquals(EnemyState.CHASE, ai.getState());
        assertTrue(combat.getActiveAttacks().isEmpty());
        assertEquals(100, player.getHealth().getCurrentHp());
        for (int i = 0; i < 100 && ai.getState() != EnemyState.ATTACK; i++) {
            ai.update(target, 0.1);
            combat.update(0.1);
            assertTrue(map.isWalkable(ai.getBounds()));
        }
        assertEquals(EnemyState.ATTACK, ai.getState());
        assertEquals(90, player.getHealth().getCurrentHp());
    }

    /** A moved player invalidates the cached goal and can be reached without following stale waypoints forever. */
    @Test
    void replansForMovingTargetAndDropsPathOnDeath() {
        GameMap map = TestMaps.rectangle(320, 240, List.of(new CollisionBox(120, 0, 8, 160)));
        CombatSystem combat = new CombatSystem();
        Player player = new Player(new CollisionBox(200, 40, 20, 20), 100, map);
        Combatant target = new Combatant(player, Faction.PLAYER, player.getBounds());
        combat.register(target);
        Enemy enemy = new Enemy();
        EnemyAI ai = new EnemyAI(enemy, new CollisionBox(40, 40, 20, 20), map, config, combat);
        ai.update(target, 0.1);
        player.enterArea(map, 260, 100);
        target.setHurtbox(player.getBounds());
        for (int i = 0; i < 100 && ai.getState() != EnemyState.ATTACK; i++) {
            ai.update(target, 0.1);
            combat.update(0.1);
        }
        assertEquals(EnemyState.ATTACK, ai.getState());
        assertEquals(90, player.getHealth().getCurrentHp());
        enemy.takeDamage(100);
        CollisionBox stopped = ai.getBounds();
        ai.update(target, 10);
        assertEquals(EnemyState.DEAD, ai.getState());
        assertEquals(stopped, ai.getBounds());
    }

    /** Two independent controllers can navigate the actual alternating Training walls without sharing path state. */
    @Test
    void multipleEnemiesNavigateActualBattleMap() {
        GameMap map = MapCatalog.createMaps().stream().filter(level -> level.getId().equals(MapCatalog.TRAINING))
                .findFirst().orElseThrow();
        CombatSystem combat = new CombatSystem();
        Player player = new Player(new CollisionBox(680, 480, 28, 28), 100, map, 1000);
        Combatant target = new Combatant(player, Faction.PLAYER, player.getBounds());
        combat.register(target);
        EnemyAI first = new EnemyAI(new Enemy(), new CollisionBox(80, 80, 28, 28), map, config, combat);
        EnemyAI second = new EnemyAI(new Enemy(), new CollisionBox(160, 80, 28, 28), map, config, combat);
        for (int i = 0; i < 600; i++) {
            first.update(target, 0.1);
            second.update(target, 0.1);
            combat.update(0.1);
            assertTrue(map.isWalkable(first.getBounds()));
            assertTrue(map.isWalkable(second.getBounds()));
            if (first.getState() == EnemyState.ATTACK && second.getState() == EnemyState.ATTACK) {
                break;
            }
        }
        assertEquals(EnemyState.ATTACK, first.getState());
        assertEquals(EnemyState.ATTACK, second.getState());
        assertTrue(player.getHealth().getCurrentHp() < 1000);
    }

    /** No-route results are retried on the configured interval, not with a full A* search every frame. */
    @Test
    void throttlesUnreachableTargetSearches() {
        GameMap map = TestMaps.rectangle(320, 240, List.of(new CollisionBox(120, 0, 8, 240)));
        CountingArea area = new CountingArea(map);
        CombatSystem combat = new CombatSystem();
        Player player = new Player(new CollisionBox(200, 40, 20, 20), 100, map);
        Combatant target = new Combatant(player, Faction.PLAYER, player.getBounds());
        combat.register(target);
        EnemyAI ai = new EnemyAI(new Enemy(), new CollisionBox(40, 40, 20, 20), area, config, combat);
        int before = area.queries;
        ai.update(target, 0);
        int searchQueries = area.queries - before;
        assertTrue(searchQueries > 100);
        before = area.queries;
        for (int i = 0; i < 4; i++) {
            ai.update(target, 0.1);
        }
        assertTrue(area.queries - before < 30);
        before = area.queries;
        ai.update(target, 0.11);
        assertTrue(area.queries - before > 100);
        assertEquals(new CollisionBox(40, 40, 20, 20), ai.getBounds());
    }

    /**
     * Counts collision queries to verify search throttling without exposing production counters.
     * @author Daoyan Zhu (u7782042)
     */
    private static final class CountingArea implements WalkableArea {
        /** Underlying unchanged collision model. */
        private final GameMap map;
        /** Full-body occupancy calls observed by this test. */
        private int queries;

        /**
         * Wraps the map only for diagnostic counting.
         * @param map existing collision world
         */
        private CountingArea(GameMap map) {
            this.map = map;
        }

        /** {@inheritDoc} */
        @Override
        public boolean hasLineOfSight(double fromX, double fromY, double toX, double toY) {
            return map.hasLineOfSight(fromX, fromY, toX, toY);
        }

        /** {@inheritDoc} */
        @Override
        public CollisionBox getBounds() {
            return map.getBounds();
        }

        /** {@inheritDoc} */
        @Override
        public boolean isWalkable(float x, float y) {
            return map.isWalkable(x, y);
        }

        /** {@inheritDoc} */
        @Override
        public boolean isWalkable(CollisionBox bounds) {
            queries++;
            return map.isWalkable(bounds);
        }

        /** {@inheritDoc} */
        @Override
        public CollisionBox move(CollisionBox bounds, double deltaX, double deltaY) {
            return map.move(bounds, deltaX, deltaY);
        }

        /** {@inheritDoc} */
        @Override
        public double firstBlock(CollisionBox bounds, double deltaX, double deltaY) {
            return map.firstBlock(bounds, deltaX, deltaY);
        }
    }
}
