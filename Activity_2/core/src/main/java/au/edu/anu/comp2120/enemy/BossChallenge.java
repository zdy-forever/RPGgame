package au.edu.anu.comp2120.enemy;

import au.edu.anu.comp2120.world.MapCatalog;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Five distinct campaign targets; their first version shares the existing Sentinel combat tuning.
 * Identity and map routing do not depend on graphics or quest progress.
 * @author Daoyan Zhu (u7782042)
 */
public enum BossChallenge {
    /** Fire guardian in the existing Crossroads arena. */
    FIRE("fire-sentinel", "Fire Sentinel", MapCatalog.CROSSROADS),
    /** Water guardian. */
    WATER("water-sentinel", "Water Sentinel", MapCatalog.WATER_ARENA),
    /** Lightning guardian. */
    LIGHTNING("lightning-sentinel", "Lightning Sentinel", MapCatalog.LIGHTNING_ARENA),
    /** Ice guardian. */
    ICE("ice-sentinel", "Ice Sentinel", MapCatalog.ICE_ARENA),
    /** Wind guardian. */
    WIND("wind-sentinel", "Wind Sentinel", MapCatalog.WIND_ARENA);

    /** Stable Boss identity, independent of any particular attempt. */
    private final String id;
    /** Provisional readable guardian name. */
    private final String displayName;
    /** Corresponding registered Battle map. */
    private final String mapId;

    /**
     * Defines one campaign target.
     * @param id stable Boss ID
     * @param displayName readable name
     * @param mapId owning Battle map
     */
    BossChallenge(String id, String displayName, String mapId) {
        this.id = id;
        this.displayName = displayName;
        this.mapId = mapId;
    }

    /**
     * Reads the reusable target identity.
     * @return stable Boss ID
     */
    public String id() { return id; }
    /**
     * Reads the name shown in task selection and combat diagnostics.
     * @return readable guardian name
     */
    public String displayName() { return displayName; }
    /**
     * Reads this target's distinct combat arena.
     * @return registered arena ID
     */
    public String mapId() { return mapId; }

    /**
     * Resolves a campaign target without treating custom test maps as built-in guardians.
     * @param mapId registered or custom map ID
     * @return corresponding target, or empty for non-campaign maps
     * @throws NullPointerException if mapId is null
     */
    public static Optional<BossChallenge> forMap(String mapId) {
        Objects.requireNonNull(mapId, "map ID");
        return Arrays.stream(values()).filter(target -> target.mapId.equals(mapId)).findFirst();
    }
}
