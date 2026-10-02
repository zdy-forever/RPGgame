package au.edu.anu.comp2120.screen;

import au.edu.anu.comp2120.GameSession;
import au.edu.anu.comp2120.inventory.ItemCatalog;
import au.edu.anu.comp2120.player.Player;
import au.edu.anu.comp2120.world.MapCatalog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises the feedback action shared by the potion hotkey and bag without a graphics context.
 * @author Daoyan Zhu (u7782042)
 */
final class PotionFeedbackTest {
    /** Creates isolated message tests. */
    PotionFeedbackTest() { }

    /**
     * Creates a full-health player with production health and inventory rules.
     * @return fresh Hub player
     */
    private Player player() {
        return new GameSession(MapCatalog.createMaps(), MapCatalog.HUB, 28, 28, 180).getPlayer();
    }

    /**
     * Successful feedback reports effective healing and the exact remaining stock.
     * @param damage damage before the request
     * @param healed effective expected healing
     */
    @ParameterizedTest @CsvSource({"1,1", "29,29", "30,30", "80,30"})
    void reportsActualHealing(int damage, int healed) {
        Player player = player();
        player.getInventory().addItem(ItemCatalog.POTION, 2);
        player.takeDamage(damage);
        assertEquals("Recovered " + healed + " HP | Potions: 1", GameScreen.consumePotionWithFeedback(player));
        assertEquals(100 - damage + healed, player.getHealth().getCurrentHp());
        assertEquals(1, player.getInventory().quantityOf(ItemCatalog.POTION));
    }

    /** Refusals distinguish full HP, absent stock and death without consuming items. */
    @Test void refusalsAreSpecificAndDoNotConsume() {
        Player player = player();
        player.getInventory().addItem(ItemCatalog.POTION);
        assertEquals("HP is already full - potion not consumed", GameScreen.consumePotionWithFeedback(player));
        assertEquals(1, player.getInventory().quantityOf(ItemCatalog.POTION));
        player.getInventory().clear();
        player.takeDamage(1);
        assertEquals("No health potions in your bag", GameScreen.consumePotionWithFeedback(player));
        assertEquals(99, player.getHealth().getCurrentHp());
        player.getInventory().addItem(ItemCatalog.POTION);
        player.takeDamage(Integer.MAX_VALUE);
        assertEquals("Cannot use a potion while defeated", GameScreen.consumePotionWithFeedback(player));
        assertEquals(0, player.getHealth().getCurrentHp());
        assertEquals(1, player.getInventory().quantityOf(ItemCatalog.POTION));
    }

    /** The last potion reports zero; a second request cannot create healing or negative stock. */
    @Test void lastPotionAndRepeatedRequestRemainConsistent() {
        Player player = player();
        player.getInventory().addItem(ItemCatalog.POTION);
        player.takeDamage(80);
        assertEquals("Recovered 30 HP | Potions: 0", GameScreen.consumePotionWithFeedback(player));
        assertEquals("No health potions in your bag", GameScreen.consumePotionWithFeedback(player));
        assertEquals(50, player.getHealth().getCurrentHp());
        assertEquals(0, player.getInventory().quantityOf(ItemCatalog.POTION));
    }

    /** Deliberately violates the non-null contract to verify its documented failure behavior. */
    @SuppressWarnings("DataFlowIssue")
    @Test void nullPlayerIsRejected() {
        assertThrows(NullPointerException.class, () -> GameScreen.consumePotionWithFeedback(null));
    }
}
