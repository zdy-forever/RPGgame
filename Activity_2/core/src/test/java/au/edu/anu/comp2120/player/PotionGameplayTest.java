package au.edu.anu.comp2120.player;

import au.edu.anu.comp2120.GameSession;
import au.edu.anu.comp2120.inventory.Inventory;
import au.edu.anu.comp2120.inventory.ItemCatalog;
import au.edu.anu.comp2120.screen.CombatDemo;
import au.edu.anu.comp2120.world.MapCatalog;
import com.badlogic.gdx.Input;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Checks shared potion rules across classes, keyboard edges, bag ownership and map transitions.
 * Keyboard tests exercise PlayerInput and Player without pretending to render or dispatch GameScreen menus.
 * @author Daoyan Zhu (u7782042)
 */
final class PotionGameplayTest {
    /** Creates an isolated test instance. */
    PotionGameplayTest() { }

    /**
     * Builds real maps and a persistent player without a graphics context.
     * @return a new Hub session
     */
    private GameSession session() {
        return new GameSession(MapCatalog.createMaps(), MapCatalog.HUB, 28, 28, 180);
    }

    /**
     * Healing is class-independent and does not reset ability cooldowns, movement or equipment.
     * @param type class under test
     */
    @ParameterizedTest @EnumSource(PlayerClass.class)
    void everyClassHealsWithoutResettingGameplayState(PlayerClass type) {
        GameSession session = session();
        Player player = session.getPlayer();
        CombatDemo demo = new CombatDemo(session);
        demo.getAbilities().selectClass(PlayerClasses.get(type));
        assertEquals(type, player.getPlayerClass());
        player.getInventory().addItem(ItemCatalog.POTION, 2);
        player.getInventory().addItem("weapon_lightning_staff");
        assertTrue(player.getEquipment().toggle("weapon_lightning_staff"));
        demo.update(0, new PlayerCommand(0, 0, true, false, false, false), false);
        double cooldown = demo.getAbilities().getCooldownRemaining(AbilitySlot.LIGHT);
        assertTrue(cooldown > 0);
        var equipment = player.getEquipment().snapshot();
        var bounds = player.getBounds();
        var mode = player.getUltimateMode();
        player.takeDamage(50);
        long damageVersion = player.getHealth().getDamageVersion();
        assertTrue(player.useHealthPotion());
        assertEquals(80, player.getHealth().getCurrentHp());
        assertEquals(1, player.getInventory().quantityOf(ItemCatalog.POTION));
        assertEquals(cooldown, demo.getAbilities().getCooldownRemaining(AbilitySlot.LIGHT));
        assertEquals(damageVersion, player.getHealth().getDamageVersion());
        assertEquals(equipment, player.getEquipment().snapshot());
        assertEquals(bounds, player.getBounds());
        assertEquals(type, player.getPlayerClass());
        assertEquals(mode, player.getUltimateMode());
    }

    /** P consumes once on a press, not on held frames, and permits a later separate press. */
    @Test void heldPotionKeyDoesNotDrainTheBag() {
        Set<Integer> pressed = new HashSet<>();
        Input keyboard = (Input) Proxy.newProxyInstance(Input.class.getClassLoader(),
                new Class<?>[] {Input.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "isKeyPressed" -> (Integer) args[0] == Input.Keys.P;
                    case "isKeyJustPressed" -> pressed.contains((Integer) args[0]);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        PlayerInput input = new PlayerInput(keyboard);
        Player player = session().getPlayer();
        player.getInventory().addItem(ItemCatalog.POTION, 3);
        player.takeDamage(90);
        pressed.add(Input.Keys.P);
        assertTrue(input.samplePotionUse());
        assertTrue(player.useHealthPotion());
        pressed.clear();
        for (int frame = 0; frame < 120; frame++) {
            if (input.samplePotionUse()) { player.useHealthPotion(); }
        }
        assertEquals(40, player.getHealth().getCurrentHp());
        assertEquals(2, player.getInventory().quantityOf(ItemCatalog.POTION));
        pressed.add(Input.Keys.P);
        assertTrue(input.samplePotionUse());
        assertTrue(player.useHealthPotion());
        assertEquals(70, player.getHealth().getCurrentHp());
        assertEquals(1, player.getInventory().quantityOf(ItemCatalog.POTION));
    }

    /**
     * Full, nearly full, injured and dead states preserve the shared health and inventory contract.
     * @param damage initial damage taken from full health
     */
    @ParameterizedTest @ValueSource(ints = {0, 1, 29, 30, 31, 99, 100})
    void healthBoundariesConsumeOnlyOnEffectiveHealing(int damage) {
        Player player = session().getPlayer();
        player.getInventory().addItem(ItemCatalog.POTION, Integer.MAX_VALUE);
        player.takeDamage(damage);
        boolean usable = damage > 0 && damage < 100;
        assertEquals(usable, player.useHealthPotion());
        assertEquals(usable ? Math.min(100, 100 - damage + ItemCatalog.POTION_HEAL) : 100 - damage,
                player.getHealth().getCurrentHp());
        assertEquals(Integer.MAX_VALUE - (usable ? 1 : 0), player.getInventory().quantityOf(ItemCatalog.POTION));
    }

    /** Chest-only supplies cannot be consumed remotely; travel retains the actual bag and used count. */
    @Test void stockComesOnlyFromThePersistentBag() {
        GameSession session = session();
        Player player = session.getPlayer();
        Inventory bag = player.getInventory(), chest = new Inventory();
        chest.addItem(ItemCatalog.POTION, 7);
        player.takeDamage(50);
        assertFalse(player.useHealthPotion());
        assertEquals(0, bag.quantityOf(ItemCatalog.POTION));
        assertEquals(7, chest.quantityOf(ItemCatalog.POTION));
        bag.addItem(ItemCatalog.POTION, chest.removeItem(ItemCatalog.POTION, 2));
        assertTrue(player.useHealthPotion());
        assertEquals(1, bag.quantityOf(ItemCatalog.POTION));
        for (String map : new String[] {MapCatalog.TRAINING, MapCatalog.MEMORIAL_HALL, MapCatalog.HUB}) {
            session.switchMap(map);
            assertSame(player, session.getPlayer());
            assertSame(bag, session.getPlayer().getInventory());
            assertEquals(1, bag.quantityOf(ItemCatalog.POTION));
            assertEquals(5, chest.quantityOf(ItemCatalog.POTION));
        }
        bag.removeItem(ItemCatalog.POTION, 1);
        assertEquals(0, bag.quantityOf(ItemCatalog.POTION));
        assertFalse(player.useHealthPotion());
    }

    /** Headgear-expanded HP is the healing cap, not the default unequipped value of 100. */
    @Test void potionRespectsEquippedMaximumHealth() {
        Player player = session().getPlayer();
        player.getInventory().addItem("guardian_hood");
        assertTrue(player.getEquipment().toggle("guardian_hood"));
        int maximum = player.getHealth().getMaxHp();
        assertTrue(maximum > Player.DEFAULT_MAX_HP);
        player.getInventory().addItem(ItemCatalog.POTION, 2);
        player.takeDamage(1);
        assertTrue(player.useHealthPotion());
        assertEquals(maximum, player.getHealth().getCurrentHp());
        assertFalse(player.useHealthPotion());
        assertEquals(1, player.getInventory().quantityOf(ItemCatalog.POTION));
    }
}
