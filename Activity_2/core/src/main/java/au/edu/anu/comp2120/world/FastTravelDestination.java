package au.edu.anu.comp2120.world;

import java.util.Objects;

/**
 * Immutable fast-travel destination and in-memory unlock-state snapshot.
 *
 * @param id unique destination ID within a travel system
 * @param name display name
 * @param mapId target map ID
 * @param spawnId named target spawn
 * @param unlocked whether fast travel is permitted
 * @author Daoyan Zhu (u7782042)
 */
public record FastTravelDestination(String id, String name, String mapId, String spawnId,
        boolean unlocked) {
    /**
     * Validates required text; registration separately checks map/spawn references.
     *
     * @param id destination ID
     * @param name display name
     * @param mapId target map ID
     * @param spawnId target spawn ID
     * @param unlocked initial unlock state
     * @throws NullPointerException if a required value is null
     * @throws IllegalArgumentException if required text is blank
     */
    public FastTravelDestination {
        Objects.requireNonNull(id, "destination ID");
        Objects.requireNonNull(name, "destination name");
        Objects.requireNonNull(mapId, "target map ID");
        Objects.requireNonNull(spawnId, "target spawn ID");
        if (id.isBlank() || name.isBlank() || mapId.isBlank() || spawnId.isBlank()) {
            throw new IllegalArgumentException("Destination text must not be blank");
        }
    }
}
