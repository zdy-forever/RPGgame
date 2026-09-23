package au.edu.anu.comp2120.save;

import au.edu.anu.comp2120.GameSession;
import au.edu.anu.comp2120.inventory.EquipmentSlot;
import au.edu.anu.comp2120.inventory.Inventory;
import au.edu.anu.comp2120.inventory.Inventory.ItemStack;
import au.edu.anu.comp2120.inventory.ItemCatalog;
import au.edu.anu.comp2120.player.Player;
import au.edu.anu.comp2120.world.MapCatalog;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Checks persistence against real player storage and the disk store.
 * @author Hao Liu (u7998420)
 */
final class InventorySnapshotTest {
    /** Isolated save directory. */
    @TempDir Path directory;
    /** Creates a test instance. */
    InventorySnapshotTest() { }

    /** Creates a player without starting graphics.
     * @return player in the Hub
     */
    private Player player() {
        return new GameSession(MapCatalog.createMaps(), MapCatalog.HUB, 28, 28, 180).getPlayer();
    }

    /** Disk restoration preserves ordering, maximum stacks and equipment bonuses.
     * @throws IOException if temporary file IO fails
     */
    @Test
    void diskRoundTripRetainsContentsAndBonuses() throws IOException {
        Player source = player();
        Inventory chest = new Inventory();
        source.getInventory().addItem("unknown-material", Integer.MAX_VALUE);
        for (var gear : ItemCatalog.equipment()) {
            source.getInventory().addItem(gear.id());
            assertTrue(source.getEquipment().toggle(gear.id()));
        }
        chest.addItem("health_potion", 3);
        chest.addItem("guardian_hood", 2);
        var expected = InventorySnapshot.capture(source, chest);
        SaveStore store = new SaveStore(directory);
        store.save(SaveSlot.MANUAL, expected.toFields());
        Player target = player();
        Inventory targetChest = new Inventory();
        InventorySnapshot.fromFields(store.load(SaveSlot.MANUAL)).restore(target, targetChest);
        assertEquals(expected, InventorySnapshot.capture(target, targetChest));
        assertEquals(30, target.getEquipment().attackPercent());
        assertEquals(25, target.getEquipment().defencePercent());
    }

    /** Repeated loads replace contents, retain identities and clear previously worn gear. */
    @Test
    void repeatedRestoreDoesNotDuplicateOrRetainOldEquipment() {
        Player target = player();
        Inventory bag = target.getInventory();
        var equipment = target.getEquipment();
        Inventory chest = new Inventory();
        bag.addItem("weapon_lightning_staff");
        equipment.toggle("weapon_lightning_staff");
        chest.addItem("old-loot", 9);
        var snapshot = new InventorySnapshot(
                List.of(new ItemStack("weapon_lightning_staff", 2), new ItemStack("guardian_hood", 1)),
                List.of(new ItemStack("health_potion", 3)), Map.of(EquipmentSlot.HEAD, "guardian_hood"));
        snapshot.restore(target, chest);
        snapshot.restore(target, chest);
        assertSame(bag, target.getInventory());
        assertSame(equipment, target.getEquipment());
        assertEquals(snapshot, InventorySnapshot.capture(target, chest));
        assertEquals(0, equipment.attackPercent());
        assertEquals(4, equipment.defencePercent());
    }

    /** Empty saves never regrant starter items or retain equipment effects. */
    @Test
    void emptySnapshotClearsStorageAndBonuses() {
        Player target = player();
        target.getInventory().addItem("guardian_coat");
        target.getEquipment().toggle("guardian_coat");
        Inventory chest = new Inventory();
        chest.addItem("health_potion", 3);
        new InventorySnapshot(List.of(), List.of(), Map.of()).restore(target, chest);
        assertTrue(target.getInventory().isEmpty());
        assertTrue(chest.isEmpty());
        assertTrue(target.getEquipment().snapshot().isEmpty());
        assertEquals(0, target.getEquipment().defencePercent());
    }

    /** Invalid quantities, duplicated stacks and inconsistent equipment fail validation. */
    @Test
    void invalidStacksAndGearAreRejected() {
        for (var stacks : List.of(List.of(new ItemStack("x", 0)), List.of(new ItemStack("x", -1)),
                List.of(new ItemStack(" ", 1)), List.of(new ItemStack("x", 1), new ItemStack("x", 2)))) {
            assertThrows(IllegalArgumentException.class,
                    () -> new InventorySnapshot(stacks, List.of(), Map.of()));
        }
        var hood = List.of(new ItemStack("guardian_hood", 1));
        assertThrows(IllegalArgumentException.class,
                () -> new InventorySnapshot(hood, List.of(), Map.of(EquipmentSlot.BODY, "guardian_hood")));
        assertThrows(IllegalArgumentException.class,
                () -> new InventorySnapshot(List.of(), hood, Map.of(EquipmentSlot.HEAD, "guardian_hood")));
        assertThrows(IllegalArgumentException.class,
                () -> new InventorySnapshot(List.of(new ItemStack("unknown", 1)), List.of(),
                        Map.of(EquipmentSlot.WEAPON, "unknown")));
    }

    /** Corrupt fields fail before restoration, leaving current gameplay untouched. */
    @Test
    void invalidFieldsCannotPartiallyRestore() {
        Player target = player();
        target.getInventory().addItem("health_potion", 7);
        Inventory chest = new Inventory();
        chest.addItem("ore", 4);
        var before = InventorySnapshot.capture(target, chest);
        for (var change : Map.of("inventory.bag.count", "2147483647",
                "inventory.bag.0.quantity", "2147483648", "inventory.extra", "unexpected",
                "inventory.worn.WEAPON", "weapon_lightning_staff").entrySet()) {
            Map<String, String> fields = new HashMap<>(before.toFields());
            fields.put(change.getKey(), change.getValue());
            assertThrows(IllegalArgumentException.class,
                    () -> InventorySnapshot.fromFields(fields).restore(target, chest));
            assertEquals(before, InventorySnapshot.capture(target, chest));
        }
        Map<String, String> truncated = new HashMap<>(before.toFields());
        truncated.remove("inventory.worn.HEAD");
        assertThrows(IllegalArgumentException.class, () -> InventorySnapshot.fromFields(truncated));
        assertEquals(before, InventorySnapshot.capture(target, chest));
    }

    /** Constructor inputs and accessors cannot mutate captured state. */
    @Test
    void snapshotsAreIndependentAndImmutable() {
        var stacks = new ArrayList<>(List.of(new ItemStack("guardian_hood", 1)));
        var worn = new HashMap<>(Map.of(EquipmentSlot.HEAD, "guardian_hood"));
        var snapshot = new InventorySnapshot(stacks, stacks, worn);
        stacks.clear();
        worn.clear();
        assertEquals(1, snapshot.bag().size());
        assertEquals(1, snapshot.chest().size());
        assertEquals(1, snapshot.worn().size());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.bag().clear());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.worn().clear());
        Map<String, String> combined = new HashMap<>(snapshot.toFields());
        combined.put("player.hp", "100");
        assertEquals(snapshot, InventorySnapshot.fromFields(combined));
    }

    /** Mistaking the bag for the chest fails before either is cleared. */
    @Test
    void aliasedStorageIsRejectedBeforeMutation() {
        Player target = player();
        target.getInventory().addItem("ore", 5);
        var before = target.getInventory().entries();
        var empty = new InventorySnapshot(List.of(), List.of(), Map.of());
        assertThrows(IllegalArgumentException.class, () -> empty.restore(target, target.getInventory()));
        assertThrows(IllegalArgumentException.class,
                () -> InventorySnapshot.capture(target, target.getInventory()));
        assertEquals(before, target.getInventory().entries());
    }
}
