package au.edu.anu.comp2120;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import au.edu.anu.comp2120.enemy.Enemy;
import au.edu.anu.comp2120.inventory.Inventory;
import au.edu.anu.comp2120.npc.NPC;
import au.edu.anu.comp2120.player.Player;
import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.GameMap;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Verifies that domain models and remaining placeholders can be constructed without a
 * running graphics context.
 *
 * @author Daoyan Zhu (u7782042)
 */
final class CoreSkeletonTest {

    /** Creates the domain construction smoke test. */
    CoreSkeletonTest() {
    }

    /**
     * Confirms that the core model skeleton remains independent of LibGDX
     * desktop initialization.
     */
    @Test
    void constructsCorePlaceholdersWithoutGraphics() {
        assertDoesNotThrow(() -> {
            GameMap map = new GameMap(100, 100, List.of());
            new Player(new CollisionBox(10, 10, 10, 10), 20, map);
            new Enemy();
            new NPC();
            new Inventory();
        });
    }
}
