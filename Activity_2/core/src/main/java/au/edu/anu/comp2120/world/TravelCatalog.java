package au.edu.anu.comp2120.world;

import java.util.List;

/**
 * Built-in gameplay portal routes and initial fast-travel state, independent of artwork.
 * Separate unrestricted routes support isolated travel tests.
 *
 * @author Daoyan Zhu (u7782042)
 */
public final class TravelCatalog {
    /** Single production Hub portal; its historical ID stays stable while its destination can change. */
    public static final String QUEST_PORTAL_ID = "hub-training";
    /** Production portal bounds at the configured Hub destination entrance. */
    public static final CollisionBox HUB_PORTAL_TRIGGER = new CollisionBox(890, 245, 48, 64);
    /** Always-open return route in the lower-left corner of Memorial Hall. */
    public static final String MEMORIAL_RETURN = "memorial-hub";

    /**
     * Supplies production routes, separating the ordinary hall exit from quest-owned admission.
     * @return one configurable Hub entrance and an always-enabled Memorial Hall return
     */
    public static List<Portal> createGameplayPortals() {
        return List.of(createQuestPortals().get(0),
                new Portal(MEMORIAL_RETURN, MapCatalog.MEMORIAL_HALL, new CollisionBox(32, 32, 48, 64),
                        MapCatalog.HUB, "entry", true));
    }
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
        return List.of(new Portal(QUEST_PORTAL_ID, MapCatalog.HUB, HUB_PORTAL_TRIGGER,
                MapCatalog.TRAINING, "entry", false));
    }

    /**
     * Defines the two outbound Hub routes and standalone demo return routes.
     * Demo returns overlap Battle entry spawns to exercise the leave-before-retrigger rule;
     * production uses {@link #createGameplayPortals()} and does not register these test return routes.
     *
     * @return immutable portal definitions in deterministic trigger priority order
     */
    public static List<Portal> createPortals() {
        return List.of(
                new Portal("hub-training", MapCatalog.HUB, new CollisionBox(160, 64, 48, 64),
                        MapCatalog.TRAINING, "entry", true),
                new Portal("hub-crossroads", MapCatalog.HUB, new CollisionBox(160, MapCatalog.HUB_HEIGHT - 140, 48, 64),
                        MapCatalog.CROSSROADS, "entry", true),
                new Portal("training-hub", MapCatalog.TRAINING, new CollisionBox(64, 64, 80, 80),
                        MapCatalog.HUB, "entry", true),
                new Portal("crossroads-hub", MapCatalog.CROSSROADS, new CollisionBox(64, 64, 80, 80),
                        MapCatalog.HUB, "north", true));
    }

    /**
     * Defines initial destinations; TravelSystem owns runtime unlocks and GameSaves persists their state.
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
