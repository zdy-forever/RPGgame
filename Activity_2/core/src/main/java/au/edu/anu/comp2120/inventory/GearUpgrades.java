package au.edu.anu.comp2120.inventory;


/** Bounded item upgrades retaining inventory identities through saves and chest transfers.
 * @author Hao Liu (u7998420)
 */
public final class GearUpgrades {
    /** Maximum enhancement level per individual item. */
    public static final int MAX_LEVEL = 2;
    /** Prevents construction of recipes. */
    private GearUpgrades() { }

    /** Reads a validated catalogue item's enhancement level.
     * @param id equipment ID
     * @return level zero through two
     * @throws IllegalArgumentException if id is null or not a supported equipment identity
     */
    public static int level(String id) {
        ItemCatalog.gear(id).orElseThrow(() -> new IllegalArgumentException("Unknown equipment"));
        int marker = id.lastIndexOf('+');
        return marker < 0 ? 0 : Integer.parseInt(id.substring(marker + 1));
    }

    /** Chooses worn equipment first, otherwise the highest-level owned item in this slot.
     * @param slot position to upgrade
     * @param bag carried items
     * @param equipment worn slots
     * @return candidate ID or an empty string when no matching item is carried
     */
    public static String candidate(EquipmentSlot slot, Inventory bag, Equipment equipment) {
        String worn = equipment.itemAt(slot);
        if (!worn.isEmpty()) { return worn; }
        return bag.entries().stream().map(Inventory.ItemStack::materialId)
                .filter(id -> ItemCatalog.gear(id).map(g -> g.slot() == slot).orElse(false))
                .max(java.util.Comparator.comparingInt(GearUpgrades::level)).orElse("");
    }

    /** Counts a material across both separate stores without integer overflow.
     * @param id material identity
     * @param bag carried items
     * @param chest stored items
     * @return combined quantity
     */
    public static long count(String id, Inventory bag, Inventory chest) {
        return (long) bag.quantityOf(id) + chest.quantityOf(id);
    }

    /** Performs one fully prevalidated upgrade; equipped items remain equipped.
     * Consumes one shared sentinel core, from the bag before the chest.
     * @param id explicitly confirmed carried item
     * @param bag carried items
     * @param chest separate stored items
     * @param equipment player equipment backed by bag
     * @return feedback, including rejection without consuming anything
     */
    public static String upgrade(String id, Inventory bag, Inventory chest, Equipment equipment) {
        return upgrade(id, bag, chest, equipment, false);
    }

    /** Performs a debug-aware upgrade while retaining ownership, capacity and level checks.
     * @param id carried item ID
     * @param bag player bag
     * @param chest stored items
     * @param equipment worn equipment
     * @param free whether to skip material requirements and consumption
     * @return operation feedback
     */
    public static String upgrade(String id, Inventory bag, Inventory chest, Equipment equipment, boolean free) {
        var gear = ItemCatalog.gear(id);
        if (bag == chest || gear.isEmpty() || bag.quantityOf(id) == 0) { return "Upgrade blocked: carry the equipment in your bag."; }
        int current = level(id);
        if (current >= MAX_LEVEL) { return "Already at maximum level +2."; }
        String material = "sentinel-core";
        if (!free && count(material, bag, chest) == 0) { return "Upgrade blocked: missing material."; }
        String next = (current == 0 ? id : id.substring(0, id.lastIndexOf('+'))) + "+" + (current + 1);
        if (bag.quantityOf(next) == Integer.MAX_VALUE) { return "Upgrade blocked: destination stack is full."; }
        boolean worn = equipment.isEquipped(id);
        if (!free) {
            if (bag.quantityOf(material) > 0) { bag.removeItem(material, 1); }
            else { chest.removeItem(material, 1); }
        }
        bag.addItem(next);
        if (worn) { equipment.toggle(next); }
        bag.removeItem(id, 1);
        return "Upgraded to " + ItemCatalog.name(next) + ": " + ItemCatalog.effect(next);
    }
}
