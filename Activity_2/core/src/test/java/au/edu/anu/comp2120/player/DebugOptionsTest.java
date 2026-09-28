package au.edu.anu.comp2120.player;

import au.edu.anu.comp2120.GameSession;
import au.edu.anu.comp2120.world.MapCatalog;
import au.edu.anu.comp2120.inventory.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Checks runtime cheats without graphics or persistent global state.
 * @author Hao Liu (u7998420)
 */
final class DebugOptionsTest {
    /** Creates independent tests. */
    DebugOptionsTest() { }
    /** Commands are validated and turning cheats off never clears sticky save protection. */
    @Test void commandsAndDamage() {
        var player = new GameSession(MapCatalog.createMaps(), MapCatalog.HUB, 28, 28, 180).getPlayer();
        var debug = player.getDebugOptions();
        debug.execute("god maybe"); assertFalse(debug.blocksSaving());
        debug.execute(" GOD ON "); player.takeDamage(999); assertEquals(100, player.getHealth().getCurrentHp());
        assertThrows(IllegalArgumentException.class, () -> player.takeDamage(-1));
        debug.execute("debug off"); assertFalse(debug.god()); assertTrue(debug.blocksSaving());
        player.takeDamage(25); assertEquals(75, player.getHealth().getCurrentHp());
        assertFalse(new DebugOptions().blocksSaving());
    }
    /** Every cheat is validated, independently switchable and included in the global disable command. */
    @Test void allCommandsResetTogetherAndRejectInvalidInput() {
        var debug = new DebugOptions();
        debug.execute("nocd yes"); assertFalse(debug.blocksSaving());
        debug.execute("oneshot on extra"); assertFalse(debug.oneShot());
        debug.execute("nocd on"); debug.execute("oneshot on"); debug.execute("freeupgrade on"); debug.execute("god on");
        assertTrue(debug.noCooldown()); assertTrue(debug.oneShot()); assertTrue(debug.freeUpgrade()); assertTrue(debug.god());
        debug.execute("debug off");
        assertFalse(debug.noCooldown()); assertFalse(debug.oneShot()); assertFalse(debug.freeUpgrade()); assertFalse(debug.god());
        assertTrue(debug.blocksSaving());
    }

    /** Bare commands toggle independently; explicit commands remain idempotent and save protection stays sticky. */
    @Test void bareCommandsToggleAndKeepExplicitAliases() {
        var debug = new DebugOptions();
        for (String command : new String[] {"god", "freeupgrade", "oneshot", "nocd"}) {
            debug.execute(command);
            String enabled = debug.status();
            debug.execute(command + " on"); assertEquals(enabled, debug.status());
            debug.execute(command);
            String disabled = debug.status();
            assertNotEquals(enabled, disabled);
            debug.execute(command + " off"); assertEquals(disabled, debug.status());
        }
        assertFalse(debug.god()); assertFalse(debug.freeUpgrade());
        assertFalse(debug.oneShot()); assertFalse(debug.noCooldown());
        assertTrue(debug.blocksSaving());
        debug.execute(" GOD "); assertTrue(debug.god());
        debug.execute("god invalid"); assertTrue(debug.god());
    }

    /** God mode cannot revive a dead player or leak to a new game. */
    @Test void godDoesNotReviveOrLeakAcrossGames() {
        var player = new GameSession(MapCatalog.createMaps(), MapCatalog.HUB, 28, 28, 180).getPlayer();
        player.takeDamage(Integer.MAX_VALUE);
        player.getDebugOptions().execute("god"); player.takeDamage(1);
        assertFalse(player.isAlive());
        var fresh = new GameSession(MapCatalog.createMaps(), MapCatalog.HUB, 28, 28, 180).getPlayer();
        assertFalse(fresh.getDebugOptions().god()); assertFalse(fresh.getDebugOptions().blocksSaving());
        fresh.takeDamage(10); assertEquals(90, fresh.getHealth().getCurrentHp());
    }
    /** Free recipes still validate ownership, distinct stores and destination capacity atomically. */
    @Test void freeUpgradeRetainsSafetyChecks() {
        var bag = new Inventory(); var chest = new Inventory(); var worn = new Equipment(bag);
        assertTrue(GearUpgrades.upgrade("guardian_boots", bag, chest, worn, true).contains("blocked"));
        bag.addItem("guardian_boots"); bag.addItem("sentinel-core", 3);
        assertTrue(GearUpgrades.upgrade("guardian_boots", bag, bag, worn, true).contains("blocked"));
        bag.addItem("guardian_boots+1", Integer.MAX_VALUE);
        assertTrue(GearUpgrades.upgrade("guardian_boots", bag, chest, worn, true).contains("full"));
        assertEquals(1, bag.quantityOf("guardian_boots")); assertEquals(3, bag.quantityOf("sentinel-core"));
        assertEquals(Integer.MAX_VALUE, bag.quantityOf("guardian_boots+1"));
    }

    /** Free upgrades preserve materials and the level cap, while ordinary recipes still charge. */
    @Test void freeUpgradeStillRequiresEquipmentAndRespectsCap() {
        var bag = new Inventory(); var chest = new Inventory(); var worn = new Equipment(bag);
        var debug = new DebugOptions(); debug.execute("freeupgrade on");
        bag.addItem("guardian_boots");
        GearUpgrades.upgrade("guardian_boots", bag, chest, worn, debug.freeUpgrade());
        assertEquals(1, bag.quantityOf("guardian_boots+1"));
        chest.addItem("sentinel-core");
        GearUpgrades.upgrade("guardian_boots+1", bag, chest, worn, debug.freeUpgrade());
        assertEquals(1, chest.quantityOf("sentinel-core"));
        assertTrue(GearUpgrades.upgrade("guardian_boots+2", bag, chest, worn, true).contains("maximum"));
        assertEquals(1, bag.quantityOf("guardian_boots+2"));
    }
}
