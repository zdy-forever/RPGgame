package au.edu.anu.comp2120.npc;

import au.edu.anu.comp2120.inventory.Inventory;
import au.edu.anu.comp2120.inventory.ItemCatalog;
import au.edu.anu.comp2120.inventory.PotionSupply;

/** Independent merchant choices backed by the existing saved reward flag and inventories.
 * @author Hao Liu (u7998420)
 */
public final class MerchantServices {
    /** Prevents construction of stateless services. */
    private MerchantServices() { }

    /** Available services; append future services without coupling their rewards.
     * @author Hao Liu (u7998420)
     */
    public enum Choice {
        /** Once-only weapon reward. */ WEAPON,
        /** Repeatable potion top-up. */ POTIONS
    }

    /** Builds the live status beside a menu choice.
     * @param choice service
     * @param merchant reward owner
     * @param bag carried items
     * @param chest stored items
     * @return player-facing label and current status
     */
    public static String label(Choice choice, NPC merchant, Inventory bag, Inventory chest) {
        if (choice == Choice.WEAPON) {
            return "Claim Lightning Staff  [" + (merchant.hasInteracted() ? "Claimed" : "Available") + "]";
        }
        int carried = bag.quantityOf(ItemCatalog.POTION), stored = chest.quantityOf(ItemCatalog.POTION);
        long total = (long) carried + stored;
        return "Replenish potions  [Bag: " + carried + " | Chest: " + stored + " | Total: " + total
                + "/3" + (total >= 3 ? " - Full" : " - Need " + (3 - total)) + "]";
    }

    /** Performs only the selected service; repeated weapon requests cannot duplicate rewards.
     * @param choice service
     * @param merchant reward owner
     * @param bag carried items
     * @param chest stored items
     * @return operation feedback
     */
    public static String select(Choice choice, NPC merchant, Inventory bag, Inventory chest) {
        if (choice == Choice.WEAPON) {
            if (merchant.hasInteracted()) { return "Weapon already claimed."; }
            bag.addItem(merchant.getRewardItemId());
            merchant.setInteracted();
            return "Lightning Staff added to your bag. Press B, then right-click it to equip.";
        }
        int added = PotionSupply.replenish(bag, chest);
        return added > 0 ? "Added " + added + " health potion(s) to your bag."
                : "You already have at least 3 potions across bag and chest.";
    }
}
