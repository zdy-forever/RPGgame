package au.edu.anu.comp2120.inventory;

import au.edu.anu.comp2120.EncounterFlow;
import java.util.Objects;

/** Transfers settled rewards only after verifying bag readiness on the game-loop thread.
 * Rewards that would overflow remain pending; repeated polling cannot duplicate claimed rewards.
 * @author Hao Liu (u7998420)
 */
public final class InventoryRewards {
    /** Existing encounter reward outbox. */
    private final EncounterFlow flow;
    /** Destination player storage. */
    private final Inventory inventory;

    /** Connects the existing reward boundary to one bag.
     * @param flow encounter owner
     * @param inventory destination bag
     * @throws NullPointerException if either argument is null
     */
    public InventoryRewards(EncounterFlow flow, Inventory inventory) {
        this.flow = Objects.requireNonNull(flow, "flow");
        this.inventory = Objects.requireNonNull(inventory, "inventory");
    }

    /** Collects every reward that currently fits, retaining overflow in the outbox.
     * @return number of reward payloads transferred
     */
    public int collect() {
        int collected = 0;
        for (var entry : flow.getPendingRewards().entrySet()) {
            MaterialDrop drop = entry.getValue();
            if ((long) inventory.quantityOf(drop.materialId()) + drop.quantity() > Integer.MAX_VALUE) {
                continue;
            }
            var claimed = flow.claimReward(entry.getKey());
            if (claimed.isPresent()) {
                MaterialDrop reward = claimed.get();
                inventory.addItem(reward.materialId(), reward.quantity());
                collected++;
            }
        }
        return collected;
    }
}
