package au.edu.anu.comp2120.inventory;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Verifies repeatable supplies without duplication or loss of stored items.
 * @author Hao Liu (u7998420)
 */
final class PotionSupplyTest {
    /** Creates a test fixture. */
    PotionSupplyTest() { }

    /** Every distribution below the target tops up only the bag and is idempotent. */
    @Test void topsUpCombinedTotalWithoutMovingChestItems() {
        for (int carried = 0; carried <= 3; carried++) {
            for (int stored = 0; stored <= 3; stored++) {
                var bag = new Inventory(); var chest = new Inventory();
                if (carried > 0) { bag.addItem(ItemCatalog.POTION, carried); }
                if (stored > 0) { chest.addItem(ItemCatalog.POTION, stored); }
                bag.addItem("weapon_lightning_staff"); chest.addItem("sentinel-core", 2);
                int expected = Math.max(0, 3 - carried - stored);
                assertEquals(expected, PotionSupply.replenish(bag, chest));
                assertEquals(carried + expected, bag.quantityOf(ItemCatalog.POTION));
                assertEquals(stored, chest.quantityOf(ItemCatalog.POTION));
                assertEquals(0, PotionSupply.replenish(bag, chest));
                assertEquals(1, bag.quantityOf("weapon_lightning_staff"));
                assertEquals(2, chest.quantityOf("sentinel-core"));
            }
        }
    }

    /** Consumed potions can be replaced, while even very large surplus stacks remain intact. */
    @Test void replenishesAfterConsumptionAndPreservesSurplus() {
        var bag = new Inventory(); var chest = new Inventory();
        assertEquals(3, PotionSupply.replenish(bag, chest));
        bag.removeItem(ItemCatalog.POTION, 2);
        assertEquals(2, PotionSupply.replenish(bag, chest));
        bag.clear(); bag.addItem(ItemCatalog.POTION, Integer.MAX_VALUE);
        chest.addItem(ItemCatalog.POTION, Integer.MAX_VALUE);
        assertEquals(0, PotionSupply.replenish(bag, chest));
        assertEquals(Integer.MAX_VALUE, bag.quantityOf(ItemCatalog.POTION));
        assertEquals(Integer.MAX_VALUE, chest.quantityOf(ItemCatalog.POTION));
    }
}
