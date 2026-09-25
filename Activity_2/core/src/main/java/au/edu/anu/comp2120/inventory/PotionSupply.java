package au.edu.anu.comp2120.inventory;

/** Repeatable Hub supplies counting carried and stored potions together.
 * @author Hao Liu (u7998420)
 */
public final class PotionSupply {
    /** Prevents construction of the supply rule. */
    private PotionSupply() { }

    /** Tops up carried potions to a combined minimum of three without moving stored items.
     * Existing surplus is retained; repeated requests cannot farm additional potions.
     * @param inventory player bag
     * @param chest stored items
     * @return number of potions added to the bag
     */
    public static int replenish(Inventory inventory, Inventory chest) {
        long total = (long) inventory.quantityOf(ItemCatalog.POTION) + chest.quantityOf(ItemCatalog.POTION);
        int added = (int) Math.max(0L, 3L - total);
        if (added > 0) { inventory.addItem(ItemCatalog.POTION, added); }
        return added;
    }
}
