package au.edu.anu.comp2120;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import au.edu.anu.comp2120.enemy.Enemy;
import au.edu.anu.comp2120.inventory.Inventory;
import au.edu.anu.comp2120.npc.NPC;
import au.edu.anu.comp2120.player.Player;
import au.edu.anu.comp2120.world.GameMap;
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
