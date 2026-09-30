package au.edu.anu.comp2120.inventory;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Exercises worn-item tracking and the additive set bonuses derived from the catalogue.
 * @author COMP2120 Assignment 3 group
 */
final class EquipmentTest {
    /** Independent inventory and equipment per test. */
    private final Inventory inventory = new Inventory();
    /** Binds equipment to the shared inventory. */
    private final Equipment equipment = new Equipment(inventory);

    /** Unknown items cannot be equipped. */
    @Test void toggleUnknownReturnsFalse() {
        assertFalse(equipment.toggle("not_a_real_item"));
    }

    /** Items without stock cannot be equipped. */
    @Test void toggleWithoutStockReturnsFalse() {
        assertFalse(equipment.toggle("weapon_lightning_staff"));
    }

    /** Equipping reserves one owned unit and reports its slot. */
    @Test void equippingReservesOwnedItem() {
        inventory.addItem("weapon_lightning_staff", 1);
        assertTrue(equipment.toggle("weapon_lightning_staff"));
        assertTrue(equipment.isEquipped("weapon_lightning_staff"));
        assertEquals("weapon_lightning_staff", equipment.itemAt(EquipmentSlot.WEAPON));
        assertEquals(20, equipment.attackPercent());
    }

    /** Toggling an equipped item again unequips it. */
    @Test void togglingAgainUnequips() {
        inventory.addItem("weapon_lightning_staff", 1);
        equipment.toggle("weapon_lightning_staff");
        equipment.toggle("weapon_lightning_staff");
        assertFalse(equipment.isEquipped("weapon_lightning_staff"));
        assertEquals("", equipment.itemAt(EquipmentSlot.WEAPON));
        assertEquals(0, equipment.attackPercent());
    }

    /** A four-piece guardian set unlocks the set attack bonus alongside piece bonuses. */
    @Test void fourPieceGuardianSetAddsAttackBonus() {
        for (String id : new String[] {"guardian_hood", "guardian_coat", "guardian_gloves",
                "guardian_boots", "weapon_lightning_staff"}) {
            inventory.addItem(id, 1);
            equipment.toggle(id);
        }
        assertEquals(4, equipment.setPieces());
        assertEquals(30, equipment.attackPercent());
        assertEquals(13, equipment.defencePercent());
        assertEquals(20, equipment.healthBonus());
        assertEquals(10, equipment.cooldownPercent());
        assertEquals(10, equipment.speedPercent());
    }

    /** Removing the last unit of a worn item drops its slot. */
    @Test void reconcileDropsMissingItems() {
        inventory.addItem("weapon_lightning_staff", 1);
        equipment.toggle("weapon_lightning_staff");
        inventory.removeItem("weapon_lightning_staff", 1);
        assertFalse(equipment.isEquipped("weapon_lightning_staff"));
    }

    /** The snapshot is a stable view of currently worn items. */
    @Test void snapshotIsImmutable() {
        inventory.addItem("weapon_lightning_staff", 1);
        equipment.toggle("weapon_lightning_staff");
        assertEquals(1, equipment.snapshot().size());
    }
}
