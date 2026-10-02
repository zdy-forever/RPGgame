package au.edu.anu.comp2120.inventory;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * Validates item definitions, enhancement identities and UI formatting helpers.
 * @author COMP2120 Assignment 3 group
 * @author Jiaqi Chen (u7851266)
 */
final class ItemCatalogTest {
    /** Creates a new {@code ItemCatalogTest} instance. */
    ItemCatalogTest() {
        // Test instances are created by JUnit.
    }

    /** Shared catalogue constants are exposed. */
    @Test void definesConstants() {
        assertEquals("health_potion", ItemCatalog.POTION);
        assertEquals(30, ItemCatalog.POTION_HEAL);
    }

    /** Known gear resolves with its slot and base attack. */
    @Test void findsKnownGear() {
        Optional<ItemCatalog.Gear> staff = ItemCatalog.gear("weapon_lightning_staff");
        assertTrue(staff.isPresent());
        assertEquals(EquipmentSlot.WEAPON, staff.get().slot());
        assertEquals(20, staff.get().attackPercent());
    }

    /** Unknown gear is absent rather than an error. */
    @Test void rejectsUnknownGear() {
        assertTrue(ItemCatalog.gear("mystery_item").isEmpty());
    }

    /** Upgraded identities carry scaled bonuses. */
    @Test void enhancesUpgradedIdentities() {
        var plus1 = ItemCatalog.gear("weapon_lightning_staff+1");
        assertTrue(plus1.isPresent());
        assertEquals(25, plus1.get().attackPercent());
    }

    /** The catalogue exposes five wearable definitions. */
    @Test void listsFiveEquipmentDefinitions() {
        assertEquals(5, ItemCatalog.equipment().size());
    }

    /** Names and effects format for the text UI. */
    @Test void formatsNamesAndEffects() {
        assertEquals("Heal 30", ItemCatalog.effect("health_potion"));
        assertEquals("ATK +20%", ItemCatalog.effect("weapon_lightning_staff"));
        assertEquals("Max HP +20", ItemCatalog.effect("guardian_hood"));
        assertEquals("unknown item", ItemCatalog.name("unknown_item"));
    }

    /** Piece-specific bonuses read from the gear record. */
    @Test void gearBonusAccessors() {
        assertEquals(20, ItemCatalog.gear("guardian_hood").get().healthBonus());
        assertEquals(10, ItemCatalog.gear("guardian_gloves").get().cooldownPercent());
        assertEquals(10, ItemCatalog.gear("guardian_boots").get().speedPercent());
    }
}
