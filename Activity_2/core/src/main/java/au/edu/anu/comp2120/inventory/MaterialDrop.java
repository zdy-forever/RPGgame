package au.edu.anu.comp2120.inventory;

import au.edu.anu.comp2120.world.CollisionBox;
import java.util.Objects;

/**
 * Immutable encounter reward data, independent of artwork and inventory mutation.
 * The encounter result retains the reward until {@link InventoryRewards} transfers it to storage;
 * this record does not implement physical pickup or collection distance.
 * @param id unique reward instance identifier, not the reusable material type
 * @param materialId stable material type
 * @param quantity positive stack quantity
 * @param mapId owning map identity
 * @param bounds world geometry recorded at reward creation
 * @author Daoyan Zhu (u7782042)
 */
public record MaterialDrop(String id, String materialId, int quantity, String mapId, CollisionBox bounds) {
    /**
     * Validates a reward without mutating inventory or loading an icon.
     * The encounter creating it must separately verify placement against its map.
     * @param id unique non-blank instance ID
     * @param materialId non-blank material type
     * @param quantity positive quantity
     * @param mapId non-blank owning map ID
     * @param bounds immutable world rectangle
     * @throws NullPointerException if required data is null
     * @throws IllegalArgumentException if an ID is blank or quantity is not positive
     */
    public MaterialDrop {
        Objects.requireNonNull(id, "drop ID");
        Objects.requireNonNull(materialId, "material ID");
        Objects.requireNonNull(mapId, "map ID");
        Objects.requireNonNull(bounds, "bounds");
        if (id.isBlank() || materialId.isBlank() || mapId.isBlank() || quantity <= 0) {
            throw new IllegalArgumentException("Drop requires non-blank identities and positive quantity");
        }
    }
}
