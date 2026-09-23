package au.edu.anu.comp2120.save;

import au.edu.anu.comp2120.inventory.EquipmentSlot;
import au.edu.anu.comp2120.inventory.Inventory;
import au.edu.anu.comp2120.inventory.Inventory.ItemStack;
import au.edu.anu.comp2120.inventory.ItemCatalog;
import au.edu.anu.comp2120.player.Player;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Validated, immutable bag/chest/equipment state from one point in gameplay.
 * Preserves acquisition order and permits unknown material IDs, but worn items must exist
 * in the equipment catalogue and belong to the bag. This is one part of a future full save:
 * the coordinator must validate all other parts before applying any live-state changes.
 * @param bag ordered player stacks
 * @param chest ordered chest stacks
 * @param worn occupied equipment slots; items remain counted in the bag
 * @author Hao Liu (u7998420)
 */
public record InventorySnapshot(List<ItemStack> bag, List<ItemStack> chest,
                                Map<EquipmentSlot, String> worn) {
    /** File parsing bound, not an in-game inventory capacity. */
    private static final int MAX_STACKS = 4096;
    /** Namespace prevents collision with future player/quest snapshot fields. */
    private static final String PREFIX = "inventory.";

    /** Copies and validates every field without changing any game object.
     * @param bag ordered player stacks
     * @param chest ordered chest stacks
     * @param worn occupied equipment positions
     * @throws NullPointerException if a collection, entry or required value is null
     * @throws IllegalArgumentException if a stack or equipped item is inconsistent
     */
    public InventorySnapshot {
        bag = List.copyOf(bag);
        chest = List.copyOf(chest);
        worn = Map.copyOf(worn);
        Set<String> owned = validateStacks(bag);
        validateStacks(chest);
        for (var entry : worn.entrySet()) {
            var gear = ItemCatalog.gear(entry.getValue()).orElseThrow(
                    () -> new IllegalArgumentException("Unknown equipped item: " + entry.getValue()));
            if (gear.slot() != entry.getKey() || !owned.contains(entry.getValue())) {
                throw new IllegalArgumentException("Equipped item has the wrong slot or is absent from the bag");
            }
        }
    }

    /** Captures one player's bag, worn items and separate chest on the game-loop thread.
     * @param player player owning the inventory and equipment
     * @param chestStorage separate chest
     * @return independent validated snapshot
     * @throws NullPointerException if either argument is null
     * @throws IllegalArgumentException if chest aliases the bag or live data cannot be saved
     */
    public static InventorySnapshot capture(Player player, Inventory chestStorage) {
        requireSeparateStorage(player, chestStorage);
        return new InventorySnapshot(player.getInventory().entries(), chestStorage.entries(),
                player.getEquipment().snapshot());
    }

    /** Restores this validated component through existing public APIs on the game-loop thread.
     * Replaces quantities rather than adding them. Repeated restoration does not duplicate loot,
     * toggle gear off or regrant starter items. Bag/chest object identities remain unchanged.
     * The full-game coordinator must validate the entire save before invoking this method.
     * @param player player whose bag and equipment are replaced
     * @param chestStorage separate chest whose contents are replaced
     * @throws NullPointerException if either argument is null
     * @throws IllegalArgumentException if chest aliases the player's bag
     */
    public void restore(Player player, Inventory chestStorage) {
        requireSeparateStorage(player, chestStorage);
        Inventory inventory = player.getInventory();
        inventory.clear();
        // Reconcile while empty: old worn slots must not survive just because an item is restored.
        player.getEquipment().reconcile();
        chestStorage.clear();
        bag.forEach(stack -> inventory.addItem(stack.materialId(), stack.quantity()));
        chest.forEach(stack -> chestStorage.addItem(stack.materialId(), stack.quantity()));
        worn.values().forEach(id -> player.getEquipment().toggle(id));
    }

    /** Encodes ordered stacks and every equipment position for the versioned SaveStore.
     * @return immutable namespaced string fields
     */
    public Map<String, String> toFields() {
        Map<String, String> fields = new LinkedHashMap<>();
        encodeStacks(fields, "bag", bag);
        encodeStacks(fields, "chest", chest);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            fields.put(PREFIX + "worn." + slot.name(), worn.getOrDefault(slot, ""));
        }
        return Map.copyOf(fields);
    }

    /** Parses and validates this component without mutating live inventories.
     * Other modules' namespaces are left for their validators; unknown inventory fields fail.
     * @param fields decoded save fields
     * @return complete validated inventory component
     * @throws NullPointerException if fields is null
     * @throws IllegalArgumentException if fields are missing, malformed or inconsistent
     */
    public static InventorySnapshot fromFields(Map<String, String> fields) {
        Objects.requireNonNull(fields, "save fields");
        List<ItemStack> bag = decodeStacks(fields, "bag");
        List<ItemStack> chest = decodeStacks(fields, "chest");
        Map<EquipmentSlot, String> worn = new EnumMap<>(EquipmentSlot.class);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            String id = required(fields, PREFIX + "worn." + slot.name());
            if (!id.isEmpty()) { worn.put(slot, id); }
        }
        InventorySnapshot snapshot = new InventorySnapshot(bag, chest, worn);
        Set<String> expected = snapshot.toFields().keySet();
        for (String key : fields.keySet()) {
            if (key == null || (key.startsWith(PREFIX) && !expected.contains(key))) {
                throw new IllegalArgumentException("Unknown inventory field: " + key);
            }
        }
        return snapshot;
    }

    /** Checks stack identity and quantity before any restoration.
     * @param stacks ordered stacks
     * @return distinct item IDs
     * @throws NullPointerException if an item ID is null
     * @throws IllegalArgumentException if identities repeat or quantities/IDs are invalid
     */
    private static Set<String> validateStacks(List<ItemStack> stacks) {
        if (stacks.size() > MAX_STACKS) { throw new IllegalArgumentException("Too many saved item types"); }
        Set<String> ids = new HashSet<>();
        for (ItemStack stack : stacks) {
            String id = Objects.requireNonNull(stack.materialId(), "item ID");
            if (id.isBlank() || id.length() > 256 || stack.quantity() <= 0 || !ids.add(id)) {
                throw new IllegalArgumentException("Invalid or duplicate saved item: " + id);
            }
        }
        return ids;
    }

    /** Prevents clearing the same live storage twice during restoration.
     * @param player inventory owner
     * @param chest separate chest
     * @throws NullPointerException if either argument is null
     * @throws IllegalArgumentException if both refer to the player's bag
     */
    private static void requireSeparateStorage(Player player, Inventory chest) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(chest, "chest");
        if (player.getInventory() == chest) { throw new IllegalArgumentException("Chest must be separate from bag"); }
    }

    /** Writes indexed stack fields to retain insertion order after a disk round trip.
     * @param fields destination field map
     * @param name bag or chest namespace
     * @param stacks validated ordered stacks
     */
    private static void encodeStacks(Map<String, String> fields, String name, List<ItemStack> stacks) {
        String prefix = PREFIX + name + ".";
        fields.put(prefix + "count", Integer.toString(stacks.size()));
        for (int i = 0; i < stacks.size(); i++) {
            fields.put(prefix + i + ".id", stacks.get(i).materialId());
            fields.put(prefix + i + ".quantity", Integer.toString(stacks.get(i).quantity()));
        }
    }

    /** Parses bounded stack counts before allocating item records.
     * @param fields source field map
     * @param name bag or chest namespace
     * @return ordered stack records for constructor validation
     * @throws IllegalArgumentException if required values are missing or invalid
     */
    private static List<ItemStack> decodeStacks(Map<String, String> fields, String name) {
        String prefix = PREFIX + name + ".";
        int count = Integer.parseInt(required(fields, prefix + "count"));
        if (count < 0 || count > MAX_STACKS) { throw new IllegalArgumentException("Invalid saved stack count"); }
        List<ItemStack> stacks = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            stacks.add(new ItemStack(required(fields, prefix + i + ".id"),
                    Integer.parseInt(required(fields, prefix + i + ".quantity"))));
        }
        return stacks;
    }

    /** Rejects truncated snapshots instead of silently inventing defaults.
     * @param fields source map
     * @param key mandatory field name
     * @return present non-null value
     * @throws IllegalArgumentException if the field is missing or null
     */
    private static String required(Map<String, String> fields, String key) {
        String value = fields.get(key);
        if (value == null) { throw new IllegalArgumentException("Missing save field: " + key); }
        return value;
    }
}
