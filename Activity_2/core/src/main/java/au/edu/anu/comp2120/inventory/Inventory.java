package au.edu.anu.comp2120.inventory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Asset-free item storage that groups stackable materials by type.
 * Insertion order is preserved so the display stays stable between frames.
 *
 * @author Hao Liu (u7998420)
 * @author Daoyan Zhu (u7782042)
 * @author Jiaqi Chen (u7851266)
 */
public final class Inventory {

    /** Material type to stack quantity, in first-acquired order. */
    private final Map<String, Integer> stacks = new LinkedHashMap<>();

    /** Creates an empty inventory. */
    public Inventory() {
        // Storage starts empty; items are added through addItem.
    }

    /**
     * Adds a quantity of one material type to its stack.
     *
     * @param materialId non-blank material type
     * @param quantity positive quantity to add
     * @throws NullPointerException if materialId is null
     * @throws ArithmeticException if the stack quantity would overflow
     * @throws IllegalArgumentException if materialId is blank or quantity is not positive
     */
    public void addItem(String materialId, int quantity) {
        Objects.requireNonNull(materialId, "material ID");
        if (materialId.isBlank() || quantity <= 0) {
            throw new IllegalArgumentException("Item requires non-blank ID and positive quantity");
        }
        stacks.merge(materialId, quantity, Math::addExact);
    }
    /**
     * Removes a quantity of one material type from its stack.
     * The entry is dropped entirely when the stack empties.
     *
     * @param materialId material type to remove
     * @param quantity positive quantity to remove
     * @return the quantity actually removed, possibly less than requested
     * @throws NullPointerException if materialId is null
     * @throws IllegalArgumentException if quantity is not positive
     */
    public int removeItem(String materialId, int quantity) {
        Objects.requireNonNull(materialId, "material ID");
        if (quantity <= 0) {
            throw new IllegalArgumentException("Removal quantity must be positive");
        }
        Integer current = stacks.get(materialId);
        if (current == null) {
            return 0;
        }
        int removed = Math.min(quantity, current);
        if (removed == current) {
            stacks.remove(materialId);
        } else {
            stacks.put(materialId, current - removed);
        }
        return removed;
    }
    /**
     * Adds one unit of a material type.
     *
     * @param materialId non-blank material type
     */
    public void addItem(String materialId) {
        addItem(materialId, 1);
    }

    /**
     * Returns the stored quantity of a material type.
     *
     * @param materialId material type to query
     * @return stored quantity, zero when absent
     */
    public int quantityOf(String materialId) {
        return stacks.getOrDefault(materialId, 0);
    }

    /**
     * Returns a stable snapshot of every stack.
     *
     * @return immutable list of stacks in acquisition order
     */
    public List<ItemStack> entries() {
        List<ItemStack> snapshot = new ArrayList<>(stacks.size());
        stacks.forEach((id, qty) -> snapshot.add(new ItemStack(id, qty)));
        return List.copyOf(snapshot);
    }

    /**
     * Returns the number of distinct material types stored.
     *
     * @return count of distinct types
     */
    public int distinctTypes() {
        return stacks.size();
    }

    /**
     * Reports whether nothing is stored.
     *
     * @return true when the inventory holds no items
     */
    public boolean isEmpty() {
        return stacks.isEmpty();
    }

    /** Removes every stored item. */
    public void clear() {
        stacks.clear();
    }

    /**
     * One stack line shown by the inventory panel.
     *
     * @param materialId stable material type
     * @param quantity positive stack quantity
     */
    public record ItemStack(String materialId, int quantity) {
    }
}