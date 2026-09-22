package au.edu.anu.comp2120.inventory;

import static org.junit.jupiter.api.Assertions.*;

import au.edu.anu.comp2120.inventory.Inventory.ItemStack;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Confirms stacking, partial removal and validation for the shared item-storage model.
 * @author Daoyan Zhu (u7782042)
 */
final class InventoryTest {
    /** Independent storage per test. */
    private final Inventory inventory = new Inventory();

    /** Creates an isolated inventory fixture. */
    InventoryTest() { }

    /** A fresh inventory has no stacks and reports zero for any material. */
    @Test void startsEmpty() {
        assertTrue(inventory.isEmpty());
        assertEquals(0, inventory.distinctTypes());
        assertEquals(0, inventory.quantityOf("ore"));
        assertTrue(inventory.entries().isEmpty());
    }

    /** Repeated additions of the same material accumulate into one stack, in first-acquired order. */
    @Test void addItemAccumulatesIntoOneStackPerMaterial() {
        inventory.addItem("ore", 3);
        inventory.addItem("wood", 1);
        inventory.addItem("ore", 2);
        assertEquals(5, inventory.quantityOf("ore"));
        assertEquals(1, inventory.quantityOf("wood"));
        assertEquals(2, inventory.distinctTypes());
        assertEquals(List.of(new ItemStack("ore", 5), new ItemStack("wood", 1)), inventory.entries());
    }

    /** The convenience single-argument overload adds exactly one unit. */
    @Test void addItemWithoutQuantityAddsOne() {
        inventory.addItem("gem");
        inventory.addItem("gem");
        assertEquals(2, inventory.quantityOf("gem"));
    }

    /** Removing part of a stack leaves the remainder rather than dropping the entry. */
    @Test void removeItemLeavesPartialStack() {
        inventory.addItem("ore", 5);
        assertEquals(2, inventory.removeItem("ore", 2));
        assertEquals(3, inventory.quantityOf("ore"));
        assertEquals(1, inventory.distinctTypes());
    }

    /** Removing an entire stack drops the entry so it no longer appears in entries() or distinctTypes(). */
    @Test void removeItemDropsEntryWhenStackEmpties() {
        inventory.addItem("ore", 3);
        assertEquals(3, inventory.removeItem("ore", 3));
        assertTrue(inventory.isEmpty());
        assertEquals(0, inventory.distinctTypes());
        assertTrue(inventory.entries().isEmpty());
    }

    /** Removing more than is stored is capped at the current stock and reports the actual amount removed. */
    @Test void removeItemCapsAtCurrentStock() {
        inventory.addItem("ore", 2);
        assertEquals(2, inventory.removeItem("ore", 10));
        assertTrue(inventory.isEmpty());
    }

    /** Removing a material never stored returns zero without throwing. */
    @Test void removeItemOfAbsentMaterialReturnsZero() {
        assertEquals(0, inventory.removeItem("nothing", 1));
    }

    /** clear() empties every stack regardless of how many distinct materials were stored. */
    @Test void clearRemovesEveryStack() {
        inventory.addItem("ore", 1);
        inventory.addItem("wood", 4);
        inventory.clear();
        assertTrue(inventory.isEmpty());
        assertEquals(0, inventory.distinctTypes());
    }

    /** entries() returns a stable snapshot: later mutation does not retroactively change it. */
    @Test void entriesSnapshotIsNotBackedByLiveState() {
        inventory.addItem("ore", 1);
        List<ItemStack> snapshot = inventory.entries();
        inventory.addItem("ore", 9);
        assertEquals(1, snapshot.get(0).quantity());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(new ItemStack("wood", 1)));
    }

    /** addItem rejects a null id, a blank id and a non-positive quantity. */
    @Test void addItemValidatesArguments() {
        assertThrows(NullPointerException.class, () -> inventory.addItem(null, 1));
        assertThrows(IllegalArgumentException.class, () -> inventory.addItem(" ", 1));
        assertThrows(IllegalArgumentException.class, () -> inventory.addItem("ore", 0));
        assertThrows(IllegalArgumentException.class, () -> inventory.addItem("ore", -1));
    }

    /** removeItem rejects a null id and a non-positive quantity. */
    @Test void removeItemValidatesArguments() {
        assertThrows(NullPointerException.class, () -> inventory.removeItem(null, 1));
        assertThrows(IllegalArgumentException.class, () -> inventory.removeItem("ore", 0));
        assertThrows(IllegalArgumentException.class, () -> inventory.removeItem("ore", -1));
    }
}
