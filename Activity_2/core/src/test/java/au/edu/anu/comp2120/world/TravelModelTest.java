package au.edu.anu.comp2120.world;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Verifies local portal/destination validation independently of sessions and rendering.
 *
 * @author Daoyan Zhu (u7782042)
 */
final class TravelModelTest {
    /** Valid trigger geometry shared by model tests. */
    private final CollisionBox trigger = new CollisionBox(10, 20, 30, 40);

    /** Creates a travel data test fixture. */
    TravelModelTest() {
    }

    /** Production keeps one configurable route, without demo-only or unused portal markers. */
    @Test
    void productionCatalogueOmitsUnusedAndDemoReturnPortals() {
        List<Portal> portals = TravelCatalog.createQuestPortals();
        assertEquals(List.of(TravelCatalog.QUEST_PORTAL_ID), portals.stream().map(Portal::id).toList());
        assertTrue(portals.stream().allMatch(portal -> portal.sourceMapId().equals(MapCatalog.HUB)));
        assertTrue(portals.stream().noneMatch(Portal::enabled));
        assertTrue(TravelCatalog.createPortals().stream().noneMatch(portal -> portal.id().equals("hub-disabled")));
    }

    /**
     * Blank identifiers and labels cannot create portal or destination data.
     *
     * @param blank invalid text
     */
    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    void rejectsBlankText(String blank) {
        assertThrows(IllegalArgumentException.class, () -> new Portal(blank, "hub", trigger, "battle", "entry", true));
        assertThrows(IllegalArgumentException.class, () -> new Portal("id", blank, trigger, "battle", "entry", true));
        assertThrows(IllegalArgumentException.class, () -> new Portal("id", "hub", trigger, blank, "entry", true));
        assertThrows(IllegalArgumentException.class, () -> new Portal("id", "hub", trigger, "battle", blank, true));
        assertThrows(IllegalArgumentException.class, () -> new FastTravelDestination(blank, "Name", "hub", "entry", false));
        assertThrows(IllegalArgumentException.class, () -> new FastTravelDestination("id", blank, "hub", "entry", false));
        assertThrows(IllegalArgumentException.class, () -> new FastTravelDestination("id", "Name", blank, "entry", false));
        assertThrows(IllegalArgumentException.class, () -> new FastTravelDestination("id", "Name", "hub", blank, false));
    }

    /** Missing required values are rejected immediately. */
    @Test
    void rejectsNullData() {
        assertThrows(NullPointerException.class, () -> new Portal(null, "hub", trigger, "battle", "entry", true));
        assertThrows(NullPointerException.class, () -> new Portal("id", null, trigger, "battle", "entry", true));
        assertThrows(NullPointerException.class, () -> new Portal("id", "hub", null, "battle", "entry", true));
        assertThrows(NullPointerException.class, () -> new Portal("id", "hub", trigger, null, "entry", true));
        assertThrows(NullPointerException.class, () -> new Portal("id", "hub", trigger, "battle", null, true));
        assertThrows(NullPointerException.class, () -> new FastTravelDestination(null, "Name", "hub", "entry", false));
        assertThrows(NullPointerException.class, () -> new FastTravelDestination("id", null, "hub", "entry", false));
        assertThrows(NullPointerException.class, () -> new FastTravelDestination("id", "Name", null, "entry", false));
        assertThrows(NullPointerException.class, () -> new FastTravelDestination("id", "Name", "hub", null, false));
    }
}
