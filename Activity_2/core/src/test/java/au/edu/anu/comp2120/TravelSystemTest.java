package au.edu.anu.comp2120;

import static org.junit.jupiter.api.Assertions.*;

import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.FastTravelDestination;
import au.edu.anu.comp2120.world.GameMap;
import au.edu.anu.comp2120.world.MapCatalog;
import au.edu.anu.comp2120.world.MapType;
import au.edu.anu.comp2120.world.Portal;
import au.edu.anu.comp2120.world.SpawnPoint;
import au.edu.anu.comp2120.world.TravelCatalog;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tests portal/fast-travel execution, arrival protection, and failure without graphics.
 *
 * @author Daoyan Zhu (u7782042)
 */
final class TravelSystemTest {
    /** Session containing distinct Hub and Battle collision geometry. */
    private final GameSession session = createSession();

    /** Creates a travel fixture. */
    TravelSystemTest() {
    }

    /** Interior entry triggers the correct map/spawn and keeps the same player and target collisions. */
    @Test
    void enteringPortalUsesExistingMapTransition() {
        TravelSystem travel = travel();
        var player = session.getPlayer();
        assertFalse(travel.updatePortals());
        player.updateMovement(1, 0, 0.35);
        assertTrue(travel.updatePortals());
        assertEquals("battle", session.getCurrentMap().getId());
        assertSame(player, session.getPlayer());
        assertEquals(100, player.getX());
        assertEquals(100, player.getY());
        assertEquals(1, session.getTransitionVersion());
        player.updateMovement(1, 0, 10);
        assertEquals(210, player.getX());
        assertTrue(session.getCurrentMap().isWalkable(player.getBounds()));
    }

    /** Touching a portal edge is insufficient; positive body overlap triggers. */
    @Test
    void edgeContactDoesNotTrigger() {
        TravelSystem travel = travel();
        session.getPlayer().updateMovement(1, 0, 0.3);
        assertEquals(40, session.getPlayer().getX());
        assertFalse(travel.updatePortals());
        session.getPlayer().updateMovement(1, 0, 0.01);
        assertTrue(travel.updatePortals());
    }

    /** Arrival inside a return portal never loops; leaving fully then re-entering rearms it. */
    @Test
    void requiresExitBeforeReturnAndBeforeNextOutboundTrip() {
        TravelSystem travel = travel();
        session.getPlayer().updateMovement(1, 0, 0.35);
        assertTrue(travel.updatePortals());
        for (int i = 0; i < 100; i++) {
            assertFalse(travel.updatePortals());
        }
        assertEquals(1, session.getTransitionVersion());
        session.getPlayer().updateMovement(1, 0, 0.2);
        assertFalse(travel.updatePortals()); // Still overlaps the return region.
        session.getPlayer().updateMovement(1, 0, 0.2);
        assertFalse(travel.updatePortals()); // Fully outside; arms the next entry.
        session.getPlayer().updateMovement(-1, 0, 0.4);
        assertTrue(travel.updatePortals());
        assertEquals("hub", session.getCurrentMap().getId());
        assertEquals(60, session.getPlayer().getX());
        assertEquals(10, session.getPlayer().getY());
        for (int i = 0; i < 100; i++) {
            assertFalse(travel.updatePortals());
        }
        session.getPlayer().updateMovement(1, 0, 0.3);
        assertFalse(travel.updatePortals());
        session.getPlayer().updateMovement(-1, 0, 0.3);
        assertTrue(travel.updatePortals());
        assertEquals(3, session.getTransitionVersion());
    }

    /** Disabled portals cannot trigger; runtime enabling preserves their route and geometry. */
    @Test
    void disabledPortalDoesNotTrigger() {
        TravelSystem travel = travel();
        travel.setPortalEnabled("out", false);
        session.getPlayer().updateMovement(1, 0, 0.35);
        assertFalse(travel.updatePortals());
        assertEquals("hub", session.getCurrentMap().getId());
        assertFalse(travel.getPortals("hub").get(0).enabled());
        travel.setPortalEnabled("out", true);
        assertTrue(travel.updatePortals());
    }

    /** A geometrically overlapping portal on another map has no effect. */
    @Test
    void onlyCurrentMapPortalsCanTrigger() {
        Portal elsewhere = new Portal("elsewhere", "battle", new CollisionBox(40, 0, 40, 40),
                "hub", "entry", true);
        TravelSystem travel = new TravelSystem(session, List.of(elsewhere), List.of());
        session.getPlayer().updateMovement(1, 0, 0.35);
        assertFalse(travel.updatePortals());
        assertEquals(0, session.getTransitionVersion());
    }

    /** Initial spawn inside a portal is treated as arrival, not a fresh walking entry. */
    @Test
    void startupInsidePortalRequiresExit() {
        session.switchMap("hub", "return");
        TravelSystem travel = travel();
        assertFalse(travel.updatePortals());
        session.getPlayer().updateMovement(1, 0, 0.3);
        assertFalse(travel.updatePortals());
        session.getPlayer().updateMovement(-1, 0, 0.3);
        assertTrue(travel.updatePortals());
    }

    /** External transitions, including re-entry to the same map, cannot bypass arrival protection. */
    @Test
    void externalAndSameMapTransitionsAreDetected() {
        TravelSystem travel = travel();
        session.switchMap("hub", "return");
        assertFalse(travel.updatePortals());
        assertEquals("hub", session.getCurrentMap().getId());
        session.switchMap("battle", "arrival");
        assertFalse(travel.updatePortals());
        session.switchMap("battle", "arrival");
        assertFalse(travel.updatePortals());
        assertEquals("battle", session.getCurrentMap().getId());
    }

    /** Locked travel fails atomically; unlocking permits exactly the configured target. */
    @Test
    void fastTravelRequiresUnlockAndUsesArrivalGuard() {
        TravelSystem travel = travel();
        CollisionBox before = session.getPlayer().getBounds();
        assertThrows(IllegalStateException.class, () -> travel.fastTravel("battle-stop"));
        assertEquals(before, session.getPlayer().getBounds());
        assertEquals("hub", session.getCurrentMap().getId());
        assertEquals(0, session.getTransitionVersion());
        travel.unlockDestination("battle-stop");
        travel.unlockDestination("battle-stop");
        assertTrue(travel.getDestination("battle-stop").unlocked());
        travel.fastTravel("battle-stop");
        assertEquals("battle", session.getCurrentMap().getId());
        assertEquals(100, session.getPlayer().getX());
        assertEquals(100, session.getPlayer().getY());
        for (int i = 0; i < 100; i++) {
            assertFalse(travel.updatePortals());
        }
    }

    /** Same-map fast travel retains arrival protection and works repeatedly by explicit command. */
    @Test
    void explicitFastTravelCanReenterSameMapSafely() {
        TravelSystem travel = new TravelSystem(session, portals(), List.of(
                new FastTravelDestination("return", "Return", "hub", "return", true)));
        travel.fastTravel("return");
        assertFalse(travel.updatePortals());
        travel.fastTravel("return");
        assertFalse(travel.updatePortals());
        assertEquals(2, session.getTransitionVersion());
    }

    /** Unknown IDs fail explicitly and do not suppress a subsequent valid portal entry. */
    @Test
    void unknownTravelIdsAreRejectedWithoutStateChanges() {
        TravelSystem travel = travel();
        assertThrows(IllegalArgumentException.class, () -> travel.fastTravel("missing"));
        assertThrows(IllegalArgumentException.class, () -> travel.unlockDestination("missing"));
        assertThrows(IllegalArgumentException.class, () -> travel.setPortalEnabled("missing", true));
        assertThrows(IllegalArgumentException.class, () -> travel.getPortals("missing"));
        assertThrows(NullPointerException.class, () -> travel.fastTravel(null));
        assertEquals(0, session.getTransitionVersion());
        session.getPlayer().updateMovement(1, 0, 0.35);
        assertTrue(travel.updatePortals());
    }

    /** Registered anchors that cannot fit a full player still fail through GameSession at execution. */
    @Test
    void invalidFastTravelPlacementPreservesOldPositionAndCollision() {
        for (String spawn : List.of("outside", "blocked")) {
            TravelSystem travel = new TravelSystem(session, List.of(), List.of(
                    new FastTravelDestination("invalid", "Invalid", "battle", spawn, true)));
            CollisionBox before = session.getPlayer().getBounds();
            assertThrows(IllegalArgumentException.class, () -> travel.fastTravel("invalid"));
            assertEquals(before, session.getPlayer().getBounds());
            assertEquals("hub", session.getCurrentMap().getId());
            assertEquals(0, session.getTransitionVersion());
        }
        session.getPlayer().updateMovement(1, 0, 10);
        assertEquals(140, session.getPlayer().getX());
    }

    /** Failed portal attempts preserve map state and are not retried every frame while overlapping. */
    @Test
    void invalidPortalPlacementFailsOnceUntilExit() {
        Portal badTarget = new Portal("bad", "hub", new CollisionBox(50, 10, 20, 20),
                "battle", "outside", true);
        TravelSystem travel = new TravelSystem(session, List.of(badTarget), List.of());
        session.getPlayer().updateMovement(1, 0, 0.35);
        CollisionBox before = session.getPlayer().getBounds();
        assertThrows(IllegalArgumentException.class, travel::updatePortals);
        assertEquals(before, session.getPlayer().getBounds());
        assertEquals("hub", session.getCurrentMap().getId());
        assertEquals(0, session.getTransitionVersion());
        assertFalse(travel.updatePortals());
        session.getPlayer().updateMovement(-1, 0, 0.35);
        assertFalse(travel.updatePortals());
        session.getPlayer().updateMovement(1, 0, 0.35);
        assertThrows(IllegalArgumentException.class, travel::updatePortals);
    }

    /** Overlapping enabled portals choose the first registered match and never chain in one update. */
    @Test
    void overlappingPortalsHaveDeterministicPriority() {
        Portal first = portals().get(0);
        Portal second = new Portal("second", "hub", first.trigger(), "hub", "return", true);
        TravelSystem travel = new TravelSystem(session, List.of(first, second), List.of());
        session.getPlayer().updateMovement(1, 0, 0.35);
        assertTrue(travel.updatePortals());
        assertEquals("battle", session.getCurrentMap().getId());
        assertEquals(1, session.getTransitionVersion());
    }

    /** Invalid references, source regions, and duplicate IDs are rejected during registration. */
    @Test
    void validatesRegistration() {
        Portal valid = portals().get(0);
        List<Portal> invalid = List.of(
                new Portal("bad", "missing", valid.trigger(), "battle", "arrival", true),
                new Portal("bad", "hub", valid.trigger(), "missing", "arrival", true),
                new Portal("bad", "hub", valid.trigger(), "battle", "missing", true),
                new Portal("bad", "hub", new CollisionBox(195, 0, 10, 10), "battle", "arrival", true),
                new Portal("bad", "hub", new CollisionBox(145, 0, 20, 20), "battle", "arrival", false));
        for (Portal portal : invalid) {
            assertThrows(IllegalArgumentException.class,
                    () -> new TravelSystem(session, List.of(portal), List.of()));
        }
        assertThrows(IllegalArgumentException.class,
                () -> new TravelSystem(session, List.of(valid, valid), List.of()));
        for (FastTravelDestination destination : List.of(
                new FastTravelDestination("bad", "Bad", "missing", "entry", false),
                new FastTravelDestination("bad", "Bad", "battle", "missing", true))) {
            assertThrows(IllegalArgumentException.class,
                    () -> new TravelSystem(session, List.of(), List.of(destination)));
        }
        FastTravelDestination validDestination = destinations().get(0);
        assertThrows(IllegalArgumentException.class,
                () -> new TravelSystem(session, List.of(), List.of(validDestination, validDestination)));
        assertThrows(NullPointerException.class, () -> new TravelSystem(null, List.of(), List.of()));
        assertThrows(NullPointerException.class, () -> new TravelSystem(session, null, List.of()));
        assertThrows(NullPointerException.class, () -> new TravelSystem(session, List.of(), null));
    }

    /** Read-only snapshots and defensive registration prevent callers from bypassing unlock state. */
    @Test
    void protectsCollectionsAndStateSnapshots() {
        List<Portal> portalSource = new ArrayList<>(portals());
        List<FastTravelDestination> destinationSource = new ArrayList<>(destinations());
        TravelSystem travel = new TravelSystem(session, portalSource, destinationSource);
        portalSource.clear();
        destinationSource.clear();
        assertEquals(1, travel.getPortals("hub").size());
        assertEquals(1, travel.getDestinations().size());
        assertThrows(UnsupportedOperationException.class, () -> travel.getPortals("hub").clear());
        assertThrows(UnsupportedOperationException.class, () -> travel.getDestinations().clear());
        FastTravelDestination old = travel.getDestination("battle-stop");
        travel.unlockDestination("battle-stop");
        assertFalse(old.unlocked());
        assertTrue(travel.getDestination("battle-stop").unlocked());
        TravelSystem another = travel();
        assertFalse(another.getDestination("battle-stop").unlocked());
    }

    /** The built-in routes and destination definitions integrate with the configured maps and player. */
    @Test
    void builtInCatalogueSupportsPortalTripAndAllUnlockedDestinations() {
        GameSession demo = new GameSession(MapCatalog.createMaps(), MapCatalog.HUB, 28, 28, 180);
        TravelSystem travel = new TravelSystem(demo, TravelCatalog.createPortals(),
                TravelCatalog.createDestinations());
        assertTrue(travel.getDestination(TravelCatalog.HUB_ENTRY).unlocked());
        assertFalse(travel.getDestination(TravelCatalog.TRAINING_ENTRY).unlocked());
        demo.getPlayer().updateMovement(1, 0, 0.5);
        assertTrue(travel.updatePortals());
        assertEquals(MapCatalog.TRAINING, demo.getCurrentMap().getId());
        assertEquals(80, demo.getPlayer().getX());
        assertFalse(travel.updatePortals());
        for (FastTravelDestination destination : travel.getDestinations()) {
            travel.unlockDestination(destination.id());
            travel.fastTravel(destination.id());
            assertEquals(destination.mapId(), demo.getCurrentMap().getId());
            SpawnPoint spawn = demo.getCurrentMap().getSpawnPoint(destination.spawnId());
            assertEquals(spawn.x(), demo.getPlayer().getX());
            assertEquals(spawn.y(), demo.getPlayer().getY());
            assertFalse(travel.updatePortals());
        }
    }

    /**
     * Creates a fresh travel system using the fixture routes and locked destination.
     *
     * @return independent travel state for this session
     */
    private TravelSystem travel() {
        return new TravelSystem(session, portals(), destinations());
    }

    /**
     * Defines a return route whose arrival overlaps the source portal to exercise both directions.
     *
     * @return bidirectional fixture portal definitions
     */
    private List<Portal> portals() {
        return List.of(
                new Portal("out", "hub", new CollisionBox(50, 10, 20, 20), "battle", "arrival", true),
                new Portal("back", "battle", new CollisionBox(90, 90, 40, 40), "hub", "return", true));
    }

    /**
     * Defines one locked battle stop at the return portal's location.
     *
     * @return initial fast-travel definitions
     */
    private List<FastTravelDestination> destinations() {
        return List.of(new FastTravelDestination("battle-stop", "Battle stop", "battle", "arrival", false));
    }

    /**
     * Creates two maps with walls and deliberately body-invalid test anchors.
     *
     * @return test session with a ten-unit player and a hundred-unit movement speed
     */
    private static GameSession createSession() {
        GameMap hub = new GameMap("hub", "Hub", MapType.HUB, 200, 200,
                List.of(new CollisionBox(150, 0, 10, 200)),
                Map.of("entry", new SpawnPoint(10, 10), "return", new SpawnPoint(60, 10)), "entry");
        GameMap battle = new GameMap("battle", "Battle", MapType.BATTLE, 300, 240,
                List.of(new CollisionBox(220, 0, 10, 240)),
                Map.of("arrival", new SpawnPoint(100, 100), "outside", new SpawnPoint(295, 235),
                        "blocked", new SpawnPoint(215, 40)), "arrival");
        return new GameSession(List.of(hub, battle), "hub", 10, 10, 100);
    }
}
