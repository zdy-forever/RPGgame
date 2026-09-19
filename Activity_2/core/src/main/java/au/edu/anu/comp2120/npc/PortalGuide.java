package au.edu.anu.comp2120.npc;

import au.edu.anu.comp2120.ActiveEncounter;
import au.edu.anu.comp2120.EncounterFlow;
import au.edu.anu.comp2120.GameSession;
import au.edu.anu.comp2120.TravelSystem;
import au.edu.anu.comp2120.world.MapCatalog;
import au.edu.anu.comp2120.world.MapType;
import au.edu.anu.comp2120.world.TravelCatalog;
import java.util.Objects;
import java.util.UUID;

/**
 * Small NPC adapter selecting a route for the single Hub portal and returning from practice.
 * Selection configures admission only: movement into the portal still performs travel.
 * Does not own Quest state, rewards, map transitions or combat rules.
 * All calls run on the game-loop thread.
 * @author Daoyan Zhu (u7782042)
 */
public final class PortalGuide {
    /** Existing authority for live-Hub admission and success/failure lifecycle. */
    private final EncounterFlow encounters;
    /** Persistent player and current-map authority, used to guard both destination choices. */
    private final GameSession session;
    /** Existing routing authority, also used for practice travel without a battle permission. */
    private final TravelSystem travel;

    /**
     * The two existing playable destinations, without an achievement-room placeholder.
     * @author Daoyan Zhu (u7782042)
     */
    public enum Destination {
        /** Practice map containing only a stationary training target. */
        TRAINING(MapCatalog.TRAINING, "Training Ground"),
        /** Existing Crossroads battle map. */
        BATTLE(MapCatalog.CROSSROADS, "Battle Map (Crossroads)");

        /** Registered map identifier used by GameSession. */
        private final String mapId;
        /** Built-in-font-compatible name for the placeholder dialogue. */
        private final String label;

        /**
         * Defines one catalogue-backed option.
         * @param mapId registered destination map ID
         * @param label displayed option text
         */
        Destination(String mapId, String label) {
            this.mapId = mapId;
            this.label = label;
        }

        /**
         * Gets the registered destination.
         * @return map ID, independent of display text
         */
        public String mapId() { return mapId; }

        /**
         * Gets the placeholder dialogue label.
         * @return readable destination name
         */
        public String label() { return label; }
    }

    /**
     * Reuses the gameplay admission boundary without constructing another travel system.
     * @param session persistent player/map session
     * @param travel travel system owning the single Hub portal
     * @param encounters existing gameplay boundary owning the catalogue's single portal
     * @throws NullPointerException if any dependency is null
     * @throws IllegalArgumentException if dependencies belong to different sessions
     */
    public PortalGuide(GameSession session, TravelSystem travel, EncounterFlow encounters) {
        this.session = Objects.requireNonNull(session, "session");
        this.travel = Objects.requireNonNull(travel, "travel");
        this.encounters = Objects.requireNonNull(encounters, "encounters");
        if (!travel.belongsTo(session) || !encounters.belongsTo(session)) {
            throw new IllegalArgumentException("Portal guide dependencies must share a session");
        }
    }

    /**
     * Configures the portal in a living Hub without moving or healing the player.
     * Re-selecting an active destination keeps its acceptance identity, including after failure.
     * A replacement or selection after success creates a fresh session-local acceptance ID.
     * Practice removes local battle admission and configures ordinary travel; it has no outcome/reward.
     * No external Quest state is modified. Invalid target geometry is checked before clearing admission.
     * @param destination selected existing map
     * @throws NullPointerException if destination is null
     * @throws IllegalStateException if selection is outside Hub or the player is dead
     * @throws IllegalArgumentException if portal/map/spawn configuration is invalid
     */
    public void select(Destination destination) {
        Objects.requireNonNull(destination, "destination");
        if (session.getCurrentMap().getType() != MapType.HUB || !session.getPlayer().isAlive()) {
            throw new IllegalStateException("Portal selection requires a living player in Hub");
        }
        if (destination == Destination.TRAINING) {
            if (session.getMap(destination.mapId()).getType() != MapType.TRAINING) {
                throw new IllegalArgumentException("Practice destination must be a Training map");
            }
            travel.configurePortalDestination(TravelCatalog.QUEST_PORTAL_ID, destination.mapId(), "entry");
            encounters.clearActiveEncounter();
            travel.setPortalEnabled(TravelCatalog.QUEST_PORTAL_ID, true);
            return;
        }
        ActiveEncounter permission = encounters.getActiveEncounter()
                .filter(active -> active.portalId().equals(TravelCatalog.QUEST_PORTAL_ID)
                        && active.mapId().equals(destination.mapId()) && active.spawnId().equals("entry"))
                .orElseGet(() -> new ActiveEncounter("portal-guide-" + UUID.randomUUID(),
                        destination.mapId(), "entry", TravelCatalog.QUEST_PORTAL_ID));
        encounters.activate(permission);
    }

    /**
     * Returns a living practice player through existing travel validation and arrival protection.
     * The caller synchronizes the combat scope after this transition, as for every other journey.
     * Does not create a battle result, consume rewards, heal or change the selected portal route.
     * @throws IllegalStateException if outside Training, dead or the Hub destination is locked
     * @throws IllegalArgumentException if the registered Hub destination/spawn is missing or invalid
     */
    public void returnFromTraining() {
        if (session.getCurrentMap().getType() != MapType.TRAINING || !session.getPlayer().isAlive()) {
            throw new IllegalStateException("Practice return requires a living player in Training");
        }
        travel.fastTravel(TravelCatalog.HUB_ENTRY);
    }
}
