package au.edu.anu.comp2120.world;

import java.util.Objects;

/**
 * Immutable portal definition and enabled-state snapshot, independent of presentation.
 *
 * @param id unique portal ID within a travel system
 * @param sourceMapId map containing the trigger
 * @param trigger world-space rectangle activated by interior overlap with the player
 * @param destinationMapId target map ID
 * @param destinationSpawnId named target spawn
 * @param enabled whether this portal can trigger
 * @author Daoyan Zhu (u7782042)
 */
public record Portal(String id, String sourceMapId, CollisionBox trigger,
        String destinationMapId, String destinationSpawnId, boolean enabled) {
    /**
     * Validates local data; registration separately checks map/spawn references and geometry.
     *
     * @param id portal ID
     * @param sourceMapId source map ID
     * @param trigger activation rectangle
     * @param destinationMapId destination map ID
     * @param destinationSpawnId destination spawn ID
     * @param enabled initial activation state
     * @throws NullPointerException if a required value is null
     * @throws IllegalArgumentException if an ID is blank
     */
    public Portal {
        Objects.requireNonNull(id, "portal ID");
        Objects.requireNonNull(sourceMapId, "source map ID");
        Objects.requireNonNull(trigger, "trigger");
        Objects.requireNonNull(destinationMapId, "destination map ID");
        Objects.requireNonNull(destinationSpawnId, "destination spawn ID");
        if (id.isBlank() || sourceMapId.isBlank() || destinationMapId.isBlank()
                || destinationSpawnId.isBlank()) {
            throw new IllegalArgumentException("Portal IDs must not be blank");
        }
    }
}
