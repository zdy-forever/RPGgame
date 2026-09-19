package au.edu.anu.comp2120.world;

import java.util.List;

/**
 * Asset-free portal routes and initial fast-travel state for the built-in map demonstration.
 *
 * @author Daoyan Zhu (u7782042)
 */
public final class TravelCatalog {
    /** Single production Hub portal; its historical ID stays stable while its destination can change. */
    public static final String QUEST_PORTAL_ID = "hub-training";
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
     * Supplies one closed, reconfigurable Hub task portal; battle returns are outcome-driven.
     * The unrestricted demonstration routes remain available to isolated travel tests.
     * @return immutable single-entry task portal list, initially disabled
     */
    public static List<Portal> createQuestPortals() {
        return List.of(new Portal(QUEST_PORTAL_ID, MapCatalog.HUB, new CollisionBox(160, 64, 48, 64),
                MapCatalog.TRAINING, "entry", false));
    }

    /**
     * Defines the two outbound Hub routes and standalone demo return routes.
     * Demo returns overlap Battle entry spawns to exercise the leave-before-retrigger rule;
     * production uses createQuestPortals and never registers these return routes.
     *
     * @return immutable portal definitions in deterministic trigger priority order
     */
    public static List<Portal> createPortals() {
        return List.of(
                new Portal("hub-training", MapCatalog.HUB, new CollisionBox(160, 64, 48, 64),
                        MapCatalog.TRAINING, "entry", true),
                new Portal("hub-crossroads", MapCatalog.HUB, new CollisionBox(160, 400, 48, 64),
                        MapCatalog.CROSSROADS, "entry", true),
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
