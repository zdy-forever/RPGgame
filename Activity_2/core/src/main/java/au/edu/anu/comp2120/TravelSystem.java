package au.edu.anu.comp2120;

import au.edu.anu.comp2120.world.FastTravelDestination;
import au.edu.anu.comp2120.world.Portal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Coordinates automatic portals and unlocked fast travel through the existing game session.
 * Owns only travel state; map lookup, full-body spawn validation, and switching remain in
 * GameSession. Calls are made on the game-loop thread, independently of graphics/input APIs.
 *
 * @author Daoyan Zhu (u7782042)
 */
public final class TravelSystem {
    /** Existing map/player session; the only executor of map transitions. */
    private final GameSession session;
    /** Portal snapshots in registration order, which breaks overlapping-trigger ties. */
    private final Map<String, Portal> portals = new LinkedHashMap<>();
    /** Destination snapshots, including locked entries. */
    private final Map<String, FastTravelDestination> destinations = new LinkedHashMap<>();
    /** Last observed session transition, used to detect external and same-map re-entry. */
    private long observedTransition;
    /** Suppresses automatic travel until an update observes the player outside all portal regions. */
    private boolean waitingForExit;

    /**
     * Registers definitions and validates references, duplicate IDs, and source trigger geometry.
     * A player initially inside a portal must leave it before automatic activation is armed.
     * Full-body target fit is deliberately validated by GameSession when travel is executed.
     *
     * @param session existing game session
     * @param portals portal definitions in deterministic priority order
     * @param destinations fast-travel definitions with initial unlock states
     * @throws NullPointerException if a required value or collection entry is null
     * @throws IllegalArgumentException if IDs repeat, references are missing, or a trigger is blocked/outside
     */
    public TravelSystem(GameSession session, Collection<Portal> portals,
            Collection<FastTravelDestination> destinations) {
        this.session = Objects.requireNonNull(session, "session");
        Objects.requireNonNull(portals, "portals");
        Objects.requireNonNull(destinations, "destinations");
        for (Portal portal : portals) {
            Objects.requireNonNull(portal, "portal");
            if (!session.getMap(portal.sourceMapId()).isWalkable(portal.trigger())) {
                throw new IllegalArgumentException("Blocked or outside portal trigger: " + portal.id());
            }
            session.getMap(portal.destinationMapId()).getSpawnPoint(portal.destinationSpawnId());
            if (this.portals.putIfAbsent(portal.id(), portal) != null) {
                throw new IllegalArgumentException("Duplicate portal ID: " + portal.id());
            }
        }
        for (FastTravelDestination destination : destinations) {
            Objects.requireNonNull(destination, "destination");
            session.getMap(destination.mapId()).getSpawnPoint(destination.spawnId());
            if (this.destinations.putIfAbsent(destination.id(), destination) != null) {
                throw new IllegalArgumentException("Duplicate destination ID: " + destination.id());
            }
        }
        observedTransition = session.getTransitionVersion();
        waitingForExit = overlapsAnyPortal();
    }

    /**
     * Checks the player's current full-body overlap after movement. At most one enabled portal
     * triggers per call. Edge contact alone does not trigger. After arrival, including external
     * same-map transitions, one update must observe the body outside every portal region
     * (including disabled regions) before another entry may trigger.
     * A failed portal attempt is also held until exit to prevent repeated error attempts.
     *
     * @return true only when a portal successfully changes the session's map/spawn
     * @throws IllegalArgumentException if the selected target cannot accommodate the player
     */
    public boolean updatePortals() {
        if (observedTransition != session.getTransitionVersion()) {
            observedTransition = session.getTransitionVersion();
            waitingForExit = true;
        }
        if (waitingForExit) {
            if (!overlapsAnyPortal()) {
                waitingForExit = false;
            }
            return false;
        }
        for (Portal portal : portals.values()) {
            if (portal.enabled() && overlapsCurrentPlayer(portal)) {
                waitingForExit = true;
                session.switchMap(portal.destinationMapId(), portal.destinationSpawnId());
                observedTransition = session.getTransitionVersion();
                return true;
            }
        }
        return false;
    }

    /**
     * Executes unlocked fast travel and arms the same arrival protection used by portals.
     * Failure leaves map/player state and arrival protection unchanged.
     *
     * @param destinationId registered fast-travel destination ID
     * @throws NullPointerException if destinationId is null
     * @throws IllegalArgumentException if the ID is unknown or the target body placement is invalid
     * @throws IllegalStateException if the destination is locked
     */
    public void fastTravel(String destinationId) {
        FastTravelDestination destination = getDestination(destinationId);
        if (!destination.unlocked()) {
            throw new IllegalStateException("Destination is locked: " + destination.name());
        }
        session.switchMap(destination.mapId(), destination.spawnId());
        observedTransition = session.getTransitionVersion();
        waitingForExit = true;
    }

    /**
     * Unlocks a registered destination in memory. Repeated unlock calls are harmless.
     *
     * @param destinationId destination to unlock
     * @throws NullPointerException if destinationId is null
     * @throws IllegalArgumentException if the destination does not exist
     */
    public void unlockDestination(String destinationId) {
        FastTravelDestination old = getDestination(destinationId);
        destinations.put(destinationId,
                new FastTravelDestination(old.id(), old.name(), old.mapId(), old.spawnId(), true));
    }

    /**
     * Updates a portal's enabled state without changing its validated references or geometry.
     *
     * @param portalId registered portal ID
     * @param enabled desired activation state
     * @throws NullPointerException if portalId is null
     * @throws IllegalArgumentException if the portal does not exist
     */
    public void setPortalEnabled(String portalId, boolean enabled) {
        Portal old = portals.get(Objects.requireNonNull(portalId, "portal ID"));
        if (old == null) {
            throw new IllegalArgumentException("Unknown portal: " + portalId);
        }
        portals.put(portalId, new Portal(old.id(), old.sourceMapId(), old.trigger(),
                old.destinationMapId(), old.destinationSpawnId(), enabled));
    }

    /**
     * Returns portal snapshots for a known map, including disabled portals for presentation.
     *
     * @param mapId registered map ID
     * @return immutable ordered list of portals belonging to the map
     * @throws NullPointerException if mapId is null
     * @throws IllegalArgumentException if the map is unknown
     */
    public List<Portal> getPortals(String mapId) {
        session.getMap(mapId);
        return portals.values().stream().filter(portal -> portal.sourceMapId().equals(mapId)).toList();
    }

    /**
     * Returns current destination snapshots, including locked entries.
     *
     * @return immutable destination list in registration order
     */
    public List<FastTravelDestination> getDestinations() {
        return List.copyOf(destinations.values());
    }

    /**
     * Reads the current unlock state and target of a registered destination.
     *
     * @param destinationId destination ID
     * @return immutable destination snapshot
     * @throws NullPointerException if destinationId is null
     * @throws IllegalArgumentException if the destination is unknown
     */
    public FastTravelDestination getDestination(String destinationId) {
        FastTravelDestination destination = destinations.get(
                Objects.requireNonNull(destinationId, "destination ID"));
        if (destination == null) {
            throw new IllegalArgumentException("Unknown destination: " + destinationId);
        }
        return destination;
    }

    /**
     * Tests all current-map portal regions, irrespective of enabled state.
     *
     * @return true while the player overlaps any arrival-sensitive region
     */
    private boolean overlapsAnyPortal() {
        return portals.values().stream().anyMatch(this::overlapsCurrentPlayer);
    }

    /**
     * Tests interior overlap using the existing collision geometry and current map identity.
     *
     * @param portal portal to test
     * @return true if the current-map player overlaps the portal's trigger
     */
    private boolean overlapsCurrentPlayer(Portal portal) {
        return portal.sourceMapId().equals(session.getCurrentMap().getId())
                && portal.trigger().overlaps(session.getPlayer().getBounds());
    }
}
