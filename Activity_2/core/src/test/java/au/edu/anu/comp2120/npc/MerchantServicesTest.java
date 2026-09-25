package au.edu.anu.comp2120.npc;

import au.edu.anu.comp2120.inventory.Inventory;
import au.edu.anu.comp2120.inventory.ItemCatalog;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Checks independent service selection and live status after reward restoration.
 * @author Hao Liu (u7998420)
 */
final class MerchantServicesTest {
    /** Creates a test fixture. */
    MerchantServicesTest() { }

    /** Potion service cannot consume the once-only weapon, and weapon service cannot add potions. */
    @Test void servicesAreIndependentAndStatusesRefresh() {
        var merchant = new NPC("merchant", "Merchant", 0, 0, 24, 24, "Hello", "weapon_lightning_staff");
        var bag = new Inventory(); var chest = new Inventory();
        chest.addItem(ItemCatalog.POTION, 1);
        assertTrue(MerchantServices.label(MerchantServices.Choice.WEAPON, merchant, bag, chest).contains("Available"));
        MerchantServices.select(MerchantServices.Choice.POTIONS, merchant, bag, chest);
        assertFalse(merchant.hasInteracted());
        assertEquals(2, bag.quantityOf(ItemCatalog.POTION));
        assertEquals(0, bag.quantityOf("weapon_lightning_staff"));
        String status = MerchantServices.label(MerchantServices.Choice.POTIONS, merchant, bag, chest);
        assertTrue(status.contains("Bag: 2 | Chest: 1 | Total: 3/3 - Full"));
        MerchantServices.select(MerchantServices.Choice.WEAPON, merchant, bag, chest);
        MerchantServices.select(MerchantServices.Choice.WEAPON, merchant, bag, chest);
        assertEquals(1, bag.quantityOf("weapon_lightning_staff"));
        assertTrue(MerchantServices.label(MerchantServices.Choice.WEAPON, merchant, bag, chest).contains("Claimed"));
        bag.removeItem(ItemCatalog.POTION, 1);
        MerchantServices.select(MerchantServices.Choice.WEAPON, merchant, bag, chest);
        assertEquals(1, bag.quantityOf(ItemCatalog.POTION));
        assertTrue(MerchantServices.label(MerchantServices.Choice.POTIONS, merchant, bag, chest).contains("Need 1"));
        merchant.restoreInteracted(false);
        assertTrue(MerchantServices.label(MerchantServices.Choice.WEAPON, merchant, bag, chest).contains("Available"));
    }
}
