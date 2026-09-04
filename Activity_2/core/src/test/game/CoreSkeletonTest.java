package game;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import game.entity.Enemy;
import game.entity.NPC;
import game.entity.Player;
import game.inventory.Inventory;
import game.world.GameMap;
import org.junit.jupiter.api.Test;

/**
 * Verifies that the initial domain placeholders can be constructed without a
 * running graphics context.
 *
 * @author Daoyan Zhu (u7782042)
 */
final class CoreSkeletonTest {

    /**
     * Confirms that the core model skeleton remains independent of LibGDX
     * desktop initialization.
     */
    @Test
    void constructsCorePlaceholdersWithoutGraphics() {
        assertDoesNotThrow(() -> {
            new Player();
            new Enemy();
            new NPC();
            new GameMap();
            new Inventory();
        });
    }
}
