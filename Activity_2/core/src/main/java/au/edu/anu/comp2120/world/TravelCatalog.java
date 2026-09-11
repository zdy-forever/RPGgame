package au.edu.anu.comp2120.world;

import java.util.List;

/**
 * Asset-free portal routes and initial fast-travel state for the built-in map demonstration.
 *
 * @author Daoyan Zhu (u7782042)
 */
public final class TravelCatalog {
    /** Destination ID for the initially unlocked Hub entry. */
    public static final String HUB_ENTRY = "hub-entry";
    /** Destination ID for the initially locked Training entry. */
    public static final String TRAINING_ENTRY = "training-entry";
    /** Destination ID for the initially locked Crossroads entry. */
    public static final String CROSSROADS_ENTRY = "crossroads-entry";

    /** Prevents instantiation of this data catalogue. */
    private TravelCatalog() {
    }

    /**
     * Supplies only closed Hub task portals for production; battle returns are outcome-driven.
     * The unrestricted demonstration routes remain available to isolated travel tests.
     * @return immutable task portal definitions, initially disabled
     */
    public static List<Portal> createQuestPortals() {
        return createPortals().stream().filter(portal -> portal.sourceMapId().equals(MapCatalog.HUB))
                .map(portal -> new Portal(portal.id(), portal.sourceMapId(), portal.trigger(),
                        portal.destinationMapId(), portal.destinationSpawnId(), false)).toList();
    }

    /**
     * Defines outbound Hub portals, return portals overlapping Battle entry spawns, and
     * a disabled example. Return portals deliberately exercise the leave-before-retrigger rule.
     *
     * @return immutable portal definitions in deterministic trigger priority order
     */
    public static List<Portal> createPortals() {
        return List.of(
                new Portal("hub-training", MapCatalog.HUB, new CollisionBox(160, 64, 48, 64),
                        MapCatalog.TRAINING, "entry", true),
                new Portal("hub-crossroads", MapCatalog.HUB, new CollisionBox(160, 400, 48, 64),
                        MapCatalog.CROSSROADS, "entry", true),
                new Portal("hub-disabled", MapCatalog.HUB, new CollisionBox(220, 64, 48, 64),
                        MapCatalog.CROSSROADS, "east", false),
                new Portal("training-hub", MapCatalog.TRAINING, new CollisionBox(64, 64, 80, 80),
                        MapCatalog.HUB, "entry", true),
                new Portal("crossroads-hub", MapCatalog.CROSSROADS, new CollisionBox(64, 64, 80, 80),
                        MapCatalog.HUB, "north", true));
    }

    /**
     * Defines destinations; unlocking remains an explicit in-memory operation, not a save rule.
     *
     * @return immutable initial fast-travel definitions
     */
    public static List<FastTravelDestination> createDestinations() {
        return List.of(
                new FastTravelDestination(HUB_ENTRY, "Hub", MapCatalog.HUB, "entry", true),
                new FastTravelDestination(TRAINING_ENTRY, "Training", MapCatalog.TRAINING, "entry", false),
                new FastTravelDestination(CROSSROADS_ENTRY, "Crossroads", MapCatalog.CROSSROADS, "entry", false));
    }
}
