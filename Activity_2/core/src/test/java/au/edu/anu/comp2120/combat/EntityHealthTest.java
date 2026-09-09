package au.edu.anu.comp2120.combat;

import static org.junit.jupiter.api.Assertions.*;

import au.edu.anu.comp2120.GameSession;
import au.edu.anu.comp2120.enemy.Enemy;
import au.edu.anu.comp2120.player.Player;
import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.GameMap;
import au.edu.anu.comp2120.world.MapCatalog;
import au.edu.anu.comp2120.world.TestMaps;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Verifies shared player/enemy health and preservation across existing gameplay operations.
 * No entity AI, renderer, input, or graphics context is needed.
 *
 * @author Daoyan Zhu (u7782042)
 */
final class EntityHealthTest {
    /** Unobstructed collision area for player fixtures. */
    private final GameMap map = TestMaps.rectangle(1000, 1000, List.of());

    /** Creates the integration test fixture. */
    EntityHealthTest() {
    }

    /** Existing constructors stay compatible and allocate full default health. */
    @Test
    void existingConstructorsProvideDefaultHealth() {
        Player player = new Player(new CollisionBox(20, 20, 10, 10), 100, map);
        Enemy enemy = new Enemy();
        assertEquals(Player.DEFAULT_MAX_HP, player.getHealth().getMaxHp());
        assertEquals(Player.DEFAULT_MAX_HP, player.getHealth().getCurrentHp());
        assertEquals(Enemy.DEFAULT_MAX_HP, enemy.getHealth().getMaxHp());
        assertEquals(Enemy.DEFAULT_MAX_HP, enemy.getHealth().getCurrentHp());
        assertTrue(player.isAlive());
        assertTrue(enemy.isAlive());
    }

    /**
     * Both entities accept independently configured positive capacities.
     *
     * @param maximum capacity to configure
     */
    @ParameterizedTest
    @ValueSource(ints = {1, 250, Integer.MAX_VALUE})
    void supportsCustomMaximum(int maximum) {
        Player player = player(maximum);
        Enemy enemy = new Enemy(maximum);
        assertEquals(maximum, player.getHealth().getMaxHp());
        assertEquals(maximum, player.getHealth().getCurrentHp());
        assertEquals(maximum, enemy.getHealth().getMaxHp());
        assertEquals(maximum, enemy.getHealth().getCurrentHp());
    }

    /**
     * Both constructors reject capacities that violate the shared health contract.
     *
     * @param maximum invalid capacity
     */
    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void rejectsInvalidEntityCapacity(int maximum) {
        assertThrows(IllegalArgumentException.class, () -> player(maximum));
        assertThrows(IllegalArgumentException.class, () -> new Enemy(maximum));
    }

    /**
     * Combat sees identical damage/death semantics while healing uses each entity's shared model.
     *
     * @param usePlayer true to exercise Player, false to exercise Enemy
     */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void delegatesToTheSameHealthRules(boolean usePlayer) {
        Player player = player(100);
        Enemy enemy = new Enemy(100);
        Damageable target = usePlayer ? player : enemy;
        Health health = usePlayer ? player.getHealth() : enemy.getHealth();
        target.takeDamage(40);
        assertEquals(60, health.getCurrentHp());
        assertTrue(target.isAlive());
        assertThrows(IllegalArgumentException.class, () -> target.takeDamage(-1));
        assertEquals(60, health.getCurrentHp());
        assertEquals(20, health.heal(20));
        target.takeDamage(Integer.MAX_VALUE);
        assertEquals(0, health.getCurrentHp());
        assertFalse(target.isAlive());
        assertEquals(0, health.heal(100));
        health.restoreFull();
        assertTrue(target.isAlive());
        assertEquals(100, health.getCurrentHp());
        assertSame(health, usePlayer ? player.getHealth() : enemy.getHealth());
    }

    /** Different entities never share mutable hit points, including two of the same type. */
    @Test
    void healthIsOwnedPerEntity() {
        Player firstPlayer = player(100);
        Player secondPlayer = player(100);
        Enemy firstEnemy = new Enemy();
        Enemy secondEnemy = new Enemy();
        firstPlayer.takeDamage(100);
        firstEnemy.takeDamage(100);
        assertFalse(firstPlayer.isAlive());
        assertFalse(firstEnemy.isAlive());
        assertTrue(secondPlayer.isAlive());
        assertTrue(secondEnemy.isAlive());
        firstPlayer.getHealth().restoreFull();
        assertFalse(firstEnemy.isAlive());
    }

    /**
     * Successful and failed map transitions cannot heal, revive, or replace the player's health.
     *
     * @param lethal true to preserve death, false to preserve an injury
     */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void mapTransitionsPreserveHealth(boolean lethal) {
        List<GameMap> maps = List.copyOf(MapCatalog.createMaps());
        GameSession session = new GameSession(maps, maps.get(0).getId(), 28, 28, 180);
        Player player = session.getPlayer();
        Health health = player.getHealth();
        player.takeDamage(lethal ? health.getMaxHp() : 1);
        int before = health.getCurrentHp();
        for (GameMap destination : maps) {
            session.switchMap(destination.getId());
            assertSame(player, session.getPlayer());
            assertSame(health, session.getPlayer().getHealth());
            assertEquals(before, health.getCurrentHp());
            assertEquals(!lethal, player.isAlive());
        }
        assertThrows(IllegalArgumentException.class, () -> session.switchMap("missing-health-test-map"));
        assertEquals(before, health.getCurrentHp());
        assertSame(health, player.getHealth());
    }

    /** Health operations leave geometry unchanged; movement does not heal or perform a respawn. */
    @Test
    void healthAndMovementRemainIndependent() {
        Player player = player(100);
        CollisionBox original = player.getBounds();
        player.takeDamage(50);
        player.getHealth().heal(10);
        assertEquals(original, player.getBounds());
        player.updateMovement(1, 0, 1);
        assertEquals(60, player.getHealth().getCurrentHp());
        CollisionBox moved = player.getBounds();
        player.takeDamage(100);
        assertEquals(moved, player.getBounds());
        player.getHealth().restoreFull();
        assertEquals(moved, player.getBounds());
        assertEquals(100, player.getSpeed());
        assertEquals(100, player.getHealth().getCurrentHp());
    }

    /**
     * Creates an unobstructed player with a chosen capacity.
     *
     * @param maximum initial and maximum HP
     * @return new player with independent health
     * @throws IllegalArgumentException if maximum is not positive
     */
    private Player player(int maximum) {
        return new Player(new CollisionBox(20, 20, 10, 10), 100, map, maximum);
    }
}
