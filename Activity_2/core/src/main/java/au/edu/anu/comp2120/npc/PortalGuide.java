package au.edu.anu.comp2120.npc;

import au.edu.anu.comp2120.EncounterFlow;
import au.edu.anu.comp2120.GameSession;
import au.edu.anu.comp2120.TravelSystem;
import au.edu.anu.comp2120.world.MapCatalog;
import au.edu.anu.comp2120.world.MapType;
import au.edu.anu.comp2120.world.TravelCatalog;
import au.edu.anu.comp2120.enemy.BossChallenge;
import java.util.Objects;

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
     * Available practice, quest battle and peaceful memorial destinations.
     * @author Daoyan Zhu (u7782042)
     */
    public enum Destination {
        /** Practice map containing only a stationary training target. */
        TRAINING("Training Ground"),
        /** Battle destination supplied by the active quest, never a fixed default. */
        BATTLE("Battle Map"),
        /** Peaceful room showing guardian stones and the final Sage conversation. */
        MEMORIAL("Memorial Hall");

        /** Built-in-font-compatible name for the placeholder dialogue. */
        private final String label;

        /**
         * Defines one catalogue-backed option.
         * @param label displayed option text
         */
        Destination(String label) {
            this.label = label;
        }

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
     * Battle requires a previously accepted encounter; this NPC never creates acceptance itself.
     * Practice and Memorial Hall reroute without discarding the active quest or retry identity.
     * No Quest state is modified. Battle re-selection restores the accepted target's exact route.
     * @param destination selected existing map
     * @throws NullPointerException if destination is null
     * @throws IllegalStateException if outside a living Hub or Battle has no accepted encounter
     * @throws IllegalArgumentException if portal/map/spawn configuration is invalid
     */
    public void select(Destination destination) {
        Objects.requireNonNull(destination, "destination");
        if (session.getCurrentMap().getType() != MapType.HUB || !session.getPlayer().isAlive()) {
            throw new IllegalStateException("Portal selection requires a living player in Hub");
        }
        if (destination != Destination.BATTLE) {
            String mapId = destination == Destination.TRAINING ? MapCatalog.TRAINING : MapCatalog.MEMORIAL_HALL;
            MapType type = destination == Destination.TRAINING ? MapType.TRAINING : MapType.MEMORIAL;
            if (session.getMap(mapId).getType() != type) {
                throw new IllegalArgumentException("Destination has the wrong map type");
            }
            travel.configurePortalDestination(TravelCatalog.QUEST_PORTAL_ID, mapId, "entry");
            travel.setPortalEnabled(TravelCatalog.QUEST_PORTAL_ID, true);
            return;
        }
        var permission = encounters.getActiveEncounter()
                .filter(active -> active.portalId().equals(TravelCatalog.QUEST_PORTAL_ID))
                .orElseThrow(() -> new IllegalStateException("Accept a Boss quest from the Quest Keeper first"));
        encounters.activate(permission);
    }

    /**
     * Labels Battle with its accepted Boss or a clear locked explanation.
     * @param destination selected menu category
     * @return current readable destination label
     * @throws NullPointerException if destination is null
     */
    public String destinationLabel(Destination destination) {
        if (Objects.requireNonNull(destination, "destination") != Destination.BATTLE) {
            return destination.label();
        }
        return encounters.getActiveEncounter()
                .filter(active -> active.portalId().equals(TravelCatalog.QUEST_PORTAL_ID))
                .map(active -> "Battle: " + BossChallenge.forMap(active.mapId())
                        .map(BossChallenge::displayName).orElse(session.getMap(active.mapId()).getName()))
                .orElse("Battle: LOCKED - accept a Boss quest first");
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
