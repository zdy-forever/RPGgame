package au.edu.anu.comp2120.inventory;

import au.edu.anu.comp2120.GameSession;
import au.edu.anu.comp2120.save.*;
import au.edu.anu.comp2120.world.MapCatalog;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Validates capped upgrades, material accounting and persistent item identity.
 * @author Hao Liu (u7998420)
 */
final class GearUpgradesTest {
    /** Isolated save files. */
    @TempDir Path directory;
    /** Creates a fixture. */
    GearUpgradesTest() { }

    /** All five pieces upgrade twice, remain worn, retain set bonuses and reject further spending. */
    @Test void allSlotsUseSharedCoresAndStopAtTwo() {
        var bag = new Inventory(); var chest = new Inventory(); var worn = new Equipment(bag);
        bag.addItem("sentinel-core", 1); chest.addItem("sentinel-core", 12);
        for (var gear : ItemCatalog.equipment()) {
            bag.addItem(gear.id()); worn.toggle(gear.id());
            for (int level = 0; level < 2; level++) {
                String id = gear.id() + (level == 0 ? "" : "+1");
                assertTrue(GearUpgrades.upgrade(id, bag, chest, worn).startsWith("Upgraded"));
                assertEquals(0, bag.quantityOf(id));
                assertEquals(gear.id() + "+" + (level + 1), worn.itemAt(gear.slot()));
            }
            long before = GearUpgrades.count("sentinel-core", bag, chest);
            assertTrue(GearUpgrades.upgrade(gear.id() + "+2", bag, chest, worn).contains("maximum"));
            assertEquals(before, GearUpgrades.count("sentinel-core", bag, chest));
            assertTrue(ItemCatalog.gear(gear.id() + "+3").isEmpty());
        }
        assertEquals(0, bag.quantityOf("sentinel-core"));
        assertEquals(3, chest.quantityOf("sentinel-core"));
        assertEquals(40, worn.attackPercent());
        assertEquals(17, worn.defencePercent());
        assertEquals(4, worn.setPieces());
    }

    /** Rejections leave both stores and equipment unchanged, including stack overflow. */
    @Test void rejectedUpgradesNeverSpendMaterials() {
        var bag = new Inventory(); var chest = new Inventory(); var worn = new Equipment(bag);
        String id = "guardian_hood";
        bag.addItem(id); worn.toggle(id);
        assertTrue(GearUpgrades.upgrade(id, bag, chest, worn).contains("missing material"));
        chest.addItem("sentinel-core", 2);
        assertTrue(GearUpgrades.upgrade("guardian_coat", bag, chest, worn).contains("blocked"));
        bag.addItem(id + "+1", Integer.MAX_VALUE);
        assertTrue(GearUpgrades.upgrade(id, bag, chest, worn).contains("full"));
        assertEquals(2, chest.quantityOf("sentinel-core"));
        assertEquals(1, bag.quantityOf(id));
        assertEquals(id, worn.itemAt(EquipmentSlot.HEAD));
    }

    /** One owned unit upgrades, worn candidates take priority, and chest transfers retain the level. */
    @Test void duplicateItemsAndTransfersRetainIdentity() {
        var bag = new Inventory(); var chest = new Inventory(); var worn = new Equipment(bag);
        String id = "guardian_boots";
        bag.addItem(id, 2); bag.addItem(id + "+2"); bag.addItem("sentinel-core");
        worn.toggle(id);
        assertEquals(id, GearUpgrades.candidate(EquipmentSlot.FEET, bag, worn));
        GearUpgrades.upgrade(id, bag, chest, worn);
        assertEquals(1, bag.quantityOf(id));
        assertEquals(id + "+1", worn.itemAt(EquipmentSlot.FEET));
        chest.addItem(id + "+1", bag.removeItem(id + "+1", 1));
        assertEquals("", worn.itemAt(EquipmentSlot.FEET));
        bag.addItem(id + "+1", chest.removeItem(id + "+1", 1));
        worn.toggle(id + "+1");
        assertEquals(20, worn.speedPercent());
    }

    /** Both save slots round-trip upgraded worn and stored items; base-level saves still parse.
     * @throws Exception if temporary file IO fails
     */
    @Test void upgradedItemsSurviveBothSaveSlots() throws Exception {
        var player = new GameSession(MapCatalog.createMaps(), MapCatalog.HUB, 28, 28, 180).getPlayer();
        var bag = player.getInventory(); var chest = new Inventory();
        bag.addItem("weapon_lightning_staff"); player.getEquipment().toggle("weapon_lightning_staff");
        var old = InventorySnapshot.capture(player, chest);
        chest.addItem("sentinel-core", 2);
        GearUpgrades.upgrade("weapon_lightning_staff", bag, chest, player.getEquipment());
        GearUpgrades.upgrade("weapon_lightning_staff+1", bag, chest, player.getEquipment());
        chest.addItem("guardian_coat+2");
        var snapshot = InventorySnapshot.capture(player, chest);
        var store = new SaveStore(directory);
        for (var slot : SaveSlot.values()) {
            store.save(slot, snapshot.toFields());
            old.restore(player, chest);
            assertEquals(20, player.getEquipment().attackPercent());
            InventorySnapshot.fromFields(store.load(slot)).restore(player, chest);
            assertEquals(snapshot, InventorySnapshot.capture(player, chest));
            assertEquals(30, player.getEquipment().attackPercent());
        }
    }
}
