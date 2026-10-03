package au.edu.anu.comp2120.inventory;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Owns worn items backed by one inventory; equipping reserves an existing stack unit.
 * Moving away the last unit invalidates its equipped slot. No item is copied or consumed.
 * @author Hao Liu (u7998420)
 */
public final class Equipment {
    /** Inventory owning all equipped units. */
    private final Inventory inventory;
    /** Called after a worn-slot change, allowing player HP to clamp immediately. */
    private final Runnable changed;
    /** At most one item identity per position. */
    private final Map<EquipmentSlot, String> worn = new EnumMap<>(EquipmentSlot.class);

    /** Binds equipment to existing storage.
     * @param inventory owning player bag
     * @throws NullPointerException if inventory is null
     */
    public Equipment(Inventory inventory) {
        this(inventory, () -> { });
    }

    /** Binds equipment and a capacity-change observer.
     * @param inventory owning bag
     * @param changed observer invoked after changes
     * @throws NullPointerException if inventory or changed is null
     */
    public Equipment(Inventory inventory, Runnable changed) {
        this.inventory = Objects.requireNonNull(inventory, "inventory");
        this.changed = Objects.requireNonNull(changed, "observer");
    }

    /** Toggles one owned wearable, replacing the previous occupant of its slot.
     * @param id catalogue item identity
     * @return true if a wearable was toggled, false for absent or unknown items
     */
    public boolean toggle(String id) {
        var gear = ItemCatalog.gear(id);
        if (gear.isEmpty() || inventory.quantityOf(id) <= 0) { return false; }
        reconcile();
        EquipmentSlot slot = gear.get().slot();
        if (id.equals(worn.get(slot))) { worn.remove(slot); }
        else { worn.put(slot, id); }
        changed.run();
        return true;
    }

    /** Drops stale slot references after transfers or other inventory operations. */
    public void reconcile() {
        if (worn.values().removeIf(id -> inventory.quantityOf(id) <= 0)) { changed.run(); }
    }

    /** Checks whether an owned item currently occupies a slot.
     * @param id item identity
     * @return whether the item is worn
     */
    public boolean isEquipped(String id) {
        reconcile();
        return worn.containsValue(id);
    }

    /** Reads one position.
     * @param slot wearable position
     * @return item ID, or empty string if vacant
     */
    public String itemAt(EquipmentSlot slot) {
        reconcile();
        return worn.getOrDefault(slot, "");
    }

    /** Reconciles missing items and supplies immutable worn identities for save snapshots.
     * @return worn item identities by position
     */
    public Map<EquipmentSlot, String> snapshot() {
        reconcile();
        return Map.copyOf(worn);
    }

    /** Counts the currently supported guardian set without counting weapons.
     * @return number of worn guardian armour pieces
     */
    public int setPieces() {
        reconcile();
        return (int) worn.values().stream().map(id -> ItemCatalog.gear(id).orElseThrow())
                .filter(item -> item.setId().equals("guardian")).count();
    }

    /** Adds weapon bonuses and the four-piece set's ten percent attack bonus.
     * @return non-negative percentage added to damage
     */
    public int attackPercent() {
        reconcile();
        return worn.values().stream().map(id -> ItemCatalog.gear(id).orElseThrow())
                .mapToInt(ItemCatalog.Gear::attackPercent).sum() + (setPieces() >= 4 ? 10 : 0);
    }

    /** Adds armour and the two-piece five percent bonus, capped below immunity.
     * @return damage reduction percentage, at most 60
     */
    public int defencePercent() {
        reconcile();
        return Math.min(60, worn.values().stream().map(id -> ItemCatalog.gear(id).orElseThrow())
                .mapToInt(ItemCatalog.Gear::defencePercent).sum() + (setPieces() >= 2 ? 5 : 0));
    }
    /** Sums equipped maximum-health bonuses.
     * @return added HP
     */
    public int healthBonus() {
        reconcile();
        return worn.values().stream().map(id -> ItemCatalog.gear(id).orElseThrow()).mapToInt(ItemCatalog.Gear::healthBonus).sum();
    }
    /** Sums equipped K/L/O cooldown reduction.
     * @return percentage reduction
     */
    public int cooldownPercent() {
        reconcile();
        return worn.values().stream().map(id -> ItemCatalog.gear(id).orElseThrow()).mapToInt(ItemCatalog.Gear::cooldownPercent).sum();
    }
    /** Sums equipped ordinary movement bonuses.
     * @return percentage amplification
     */
    public int speedPercent() {
        reconcile();
        return worn.values().stream().map(id -> ItemCatalog.gear(id).orElseThrow()).mapToInt(ItemCatalog.Gear::speedPercent).sum();
    }

}
