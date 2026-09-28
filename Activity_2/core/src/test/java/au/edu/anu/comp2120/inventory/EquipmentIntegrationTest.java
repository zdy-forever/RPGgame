package au.edu.anu.comp2120.inventory;

import au.edu.anu.comp2120.ActiveEncounter;
import au.edu.anu.comp2120.EncounterFlow;
import au.edu.anu.comp2120.GameSession;
import au.edu.anu.comp2120.TravelSystem;
import au.edu.anu.comp2120.combat.*;
import au.edu.anu.comp2120.player.Player;
import au.edu.anu.comp2120.player.PlayerCommand;
import au.edu.anu.comp2120.screen.CombatDemo;
import au.edu.anu.comp2120.world.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Verifies equipment against real combat, health, map transitions and settled reward delivery.
 * @author Hao Liu (u7998420)
 */
final class EquipmentIntegrationTest {
    /** Creates an independent test instance. */
    EquipmentIntegrationTest() { }

    /** Builds the production map catalogue with a persistent player.
     * @return session initially in Hub
     */
    private GameSession session() {
        return new GameSession(MapCatalog.createMaps(), MapCatalog.HUB, 28, 28, 180);
    }

    /** Adds and equips every guardian piece.
     * @param player receiving player
     */
    private void armour(Player player) {
        for (var item : ItemCatalog.equipment()) {
            if (item.slot() != EquipmentSlot.WEAPON) {
                player.getInventory().addItem(item.id());
                assertTrue(player.getEquipment().toggle(item.id()));
            }
        }
    }

    /** Sends one real hit through the shared pipeline.
     * @param player player under test
     * @param outgoing whether the player attacks or receives the hit
     * @return actual health lost by the target
     */
    private int hit(Player player, boolean outgoing) {
        Health enemy = new Health(1000);
        Combatant hero = new Combatant(player, Faction.PLAYER, new CollisionBox(0, 0, 20, 20));
        Combatant foe = new Combatant(enemy, Faction.ENEMY, new CollisionBox(40, 0, 20, 20));
        CombatSystem combat = new CombatSystem();
        combat.register(hero);
        combat.register(foe);
        int before = outgoing ? enemy.getCurrentHp() : player.getHealth().getCurrentHp();
        combat.tryAttack(outgoing ? hero : foe, new AttackSpec("equipment-check", 40, 1, 0),
                outgoing ? foe.getHurtbox() : hero.getHurtbox()).orElseThrow();
        combat.update(0);
        return before - (outgoing ? enemy.getCurrentHp() : player.getHealth().getCurrentHp());
    }

    /** One-shot follows the normal hit pipeline and disables cleanly without buffing enemies. */
    @Test void debugLethalHitIsPlayerOnly() {
        var player = session().getPlayer();
        player.getDebugOptions().execute("oneshot on");
        assertEquals(1000, hit(player, true));
        assertEquals(40, hit(player, false));
        player.getDebugOptions().execute("oneshot off");
        assertEquals(40, hit(player, true));
    }

    /** Lethal debug hits still respect target invulnerability and zero-damage attacks. */
    @Test void debugDoesNotBypassInvulnerabilityOrZeroDamage() {
        var player = session().getPlayer(); player.getDebugOptions().execute("oneshot");
        var health = new Health(100);
        var source = new Combatant(player, Faction.PLAYER, new CollisionBox(0, 0, 20, 20));
        var target = new Combatant(health, Faction.ENEMY, new CollisionBox(40, 0, 20, 20));
        var combat = new CombatSystem(); combat.register(source); combat.register(target);
        target.getStatus().applyInvulnerability(1);
        combat.tryAttack(source, new AttackSpec("blocked", 10, 0.1, 0), target.getHurtbox());
        combat.update(0); assertEquals(100, health.getCurrentHp());
        combat.update(2);
        combat.tryAttack(source, new AttackSpec("zero", 0, 0.1, 0), target.getHurtbox());
        combat.update(0); assertEquals(100, health.getCurrentHp());
        combat.tryAttack(source, new AttackSpec("valid", 1, 0.1, 0), target.getHurtbox());
        combat.update(0); assertFalse(health.isAlive());
    }

    /** Equipping modifies actual damage and unequipping restores baseline without duplicating items. */
    @Test
    void weaponChangesRealDamageAndUnequipRestoresIt() {
        Player player = session().getPlayer();
        String staff = "weapon_lightning_staff";
        assertFalse(player.getEquipment().toggle(staff));
        player.getInventory().addItem(staff);
        assertEquals(40, hit(player, true));
        assertTrue(player.getEquipment().toggle(staff));
        assertEquals(48, hit(player, true));
        assertTrue(player.getEquipment().toggle(staff));
        assertEquals(40, hit(player, true));
        assertEquals(1, player.getInventory().quantityOf(staff));
    }

    /** Set thresholds, actual mitigation and last-unit removal remain consistent. */
    @Test
    void armourSetAndTransferRemoveBonuses() {
        Player player = session().getPlayer();
        armour(player);
        assertEquals(4, player.getEquipment().setPieces());
        assertEquals(34, hit(player, false));
        assertEquals(44, hit(player, true));
        player.getInventory().removeItem("guardian_coat", 1);
        assertEquals("", player.getEquipment().itemAt(EquipmentSlot.BODY));
        assertEquals(3, player.getEquipment().setPieces());
        assertEquals(0, player.getEquipment().attackPercent());
        assertEquals(5, player.getEquipment().defencePercent());
        player.getEquipment().toggle("guardian_hood");
        assertEquals(2, player.getEquipment().setPieces());
        assertEquals(5, player.getEquipment().defencePercent());
        player.getEquipment().toggle("guardian_gloves");
        assertEquals(0, player.getEquipment().defencePercent());
    }

    /** Removing some units preserves the wearable, but clearing ownership invalidates it. */
    @Test
    void partialRemovalPreservesEquipmentAndSnapshotsAreImmutable() {
        Player player = session().getPlayer();
        player.getInventory().addItem("guardian_hood", 2);
        player.getEquipment().toggle("guardian_hood");
        var snapshot = player.getEquipment().snapshot();
        assertThrows(UnsupportedOperationException.class, snapshot::clear);
        player.getInventory().removeItem("guardian_hood", 1);
        assertTrue(player.getEquipment().isEquipped("guardian_hood"));
        player.getInventory().clear();
        assertFalse(player.getEquipment().isEquipped("guardian_hood"));
        assertEquals(1, snapshot.size());
    }

    /** Full HP and death consume nothing; injured usage consumes exactly one and caps healing. */
    @Test
    void potionsRespectHealthAndOwnership() {
        Player player = session().getPlayer();
        player.getInventory().addItem(ItemCatalog.POTION, 2);
        assertFalse(player.useHealthPotion());
        player.takeDamage(10);
        assertTrue(player.useHealthPotion());
        assertEquals(100, player.getHealth().getCurrentHp());
        assertEquals(1, player.getInventory().quantityOf(ItemCatalog.POTION));
        player.takeDamage(100);
        assertFalse(player.useHealthPotion());
        assertEquals(1, player.getInventory().quantityOf(ItemCatalog.POTION));
    }

    /** Each successful request heals 30 HP and consumes one item; empty inventory never heals. */
    @Test
    void potionRequestsConsumeExactlyOneAndRejectEmptyInventory() {
        Player player = session().getPlayer();
        player.takeDamage(80);
        assertFalse(player.useHealthPotion());
        assertEquals(20, player.getHealth().getCurrentHp());
        player.getInventory().addItem(ItemCatalog.POTION, 2);
        assertTrue(player.useHealthPotion());
        assertEquals(50, player.getHealth().getCurrentHp());
        assertEquals(1, player.getInventory().quantityOf(ItemCatalog.POTION));
        assertTrue(player.useHealthPotion());
        assertEquals(80, player.getHealth().getCurrentHp());
        assertEquals(0, player.getInventory().quantityOf(ItemCatalog.POTION));
        assertFalse(player.useHealthPotion());
        assertEquals(80, player.getHealth().getCurrentHp());
    }

    /** Existing map switching preserves inventory/equipment identity and bonuses. */
    @Test
    void travelRetainsEquipment() {
        GameSession session = session();
        Player player = session.getPlayer();
        armour(player);
        session.switchMap(MapCatalog.TRAINING);
        session.switchMap(MapCatalog.HUB);
        assertSame(player, session.getPlayer());
        assertEquals(4, player.getEquipment().snapshot().size());
        assertEquals(34, hit(player, false));
    }

    /** Overflow never replaces the previous stack quantity. */
    @Test
    void overflowPreservesStack() {
        Inventory inventory = new Inventory();
        inventory.addItem("sentinel-core", Integer.MAX_VALUE);
        assertThrows(ArithmeticException.class, () -> inventory.addItem("sentinel-core"));
        assertEquals(Integer.MAX_VALUE, inventory.quantityOf("sentinel-core"));
    }

    /** Real Boss settlement queues rewards once; full stacks retain rewards until capacity exists. */
    @Test
    void settledRewardsSurviveOverflowAndCollectExactlyOnce() {
        GameSession session = session();
        var travel = new TravelSystem(session, TravelCatalog.createGameplayPortals(), TravelCatalog.createDestinations());
        var flow = new EncounterFlow(session, travel, List.of(TravelCatalog.QUEST_PORTAL_ID));
        var demo = new CombatDemo(session, flow);
        flow.activate(new ActiveEncounter("equipment-reward-check", MapCatalog.CROSSROADS,
                "entry", TravelCatalog.QUEST_PORTAL_ID));
        travel.updatePortals();
        var trigger = travel.getPortal(TravelCatalog.QUEST_PORTAL_ID).trigger();
        session.getPlayer().enterArea(session.getCurrentMap(), trigger.x(), trigger.y());
        assertTrue(travel.updatePortals());
        demo.update(0, PlayerCommand.NONE, false);
        demo.getBoss().orElseThrow().getCombatant().getEntity().takeDamage(Integer.MAX_VALUE);
        demo.update(0, PlayerCommand.NONE, false);
        assertEquals(1, flow.getPendingRewards().size());
        MaterialDrop reward = flow.getPendingRewards().values().iterator().next();
        Inventory inventory = session.getPlayer().getInventory();
        inventory.addItem(reward.materialId(), Integer.MAX_VALUE);
        var collector = new InventoryRewards(flow, inventory);
        assertEquals(0, collector.collect());
        assertEquals(1, flow.getPendingRewards().size());
        inventory.removeItem(reward.materialId(), reward.quantity());
        assertEquals(1, collector.collect());
        assertEquals(0, collector.collect());
        assertEquals(Integer.MAX_VALUE, inventory.quantityOf(reward.materialId()));
        assertTrue(flow.getPendingRewards().isEmpty());
    }
    /** Upgrade bonuses alter real combat damage immediately on the already-worn item. */
    @Test void upgradedWeaponChangesRealDamage() {
        Player player = session().getPlayer();
        var bag = player.getInventory(); var chest = new Inventory();
        bag.addItem("weapon_lightning_staff"); bag.addItem("sentinel-core", 2);
        player.getEquipment().toggle("weapon_lightning_staff");
        assertEquals(48, hit(player, true));
        GearUpgrades.upgrade("weapon_lightning_staff", bag, chest, player.getEquipment());
        assertEquals(50, hit(player, true));
        GearUpgrades.upgrade("weapon_lightning_staff+1", bag, chest, player.getEquipment());
        assertEquals(52, hit(player, true));
    }

    /** Headgear changes capacity without free healing or revival, including upgrades and removal. */
    @Test void hoodCapacityCannotBeUsedToFarmHealth() {
        var player = session().getPlayer(); var bag = player.getInventory(); var chest = new Inventory();
        var health = player.getHealth();
        bag.addItem("guardian_hood"); bag.addItem("sentinel-core", 2);
        player.getEquipment().toggle("guardian_hood");
        assertEquals(120, health.getMaxHp()); assertEquals(120, health.getCurrentHp());
        health.heal(20);
        GearUpgrades.upgrade("guardian_hood", bag, chest, player.getEquipment());
        assertEquals(130, health.getMaxHp()); assertEquals(130, health.getCurrentHp());
        GearUpgrades.upgrade("guardian_hood+1", bag, chest, player.getEquipment());
        assertEquals(140, health.getMaxHp()); assertEquals(140, health.getCurrentHp());
        player.getEquipment().toggle("guardian_hood+2");
        assertEquals(100, health.getCurrentHp());
        player.getEquipment().toggle("guardian_hood+2");
        assertEquals(140, health.getCurrentHp());
        health.takeDamage(140);
        player.getEquipment().toggle("guardian_hood+2");
        player.getEquipment().toggle("guardian_hood+2");
        assertFalse(health.isAlive());
        bag.removeItem("guardian_hood+2", 1);
        assertEquals(100, player.getHealth().getMaxHp());
    }

    /** Boots modify normal movement, and no longer contribute defence. */
    @Test void bootsChangeActualMovement() {
        var player = session().getPlayer();
        double base = player.getSpeed();
        player.getInventory().addItem("guardian_boots+2"); player.getEquipment().toggle("guardian_boots+2");
        assertEquals(base * 1.3, player.getSpeed(), 1e-8);
        double x = player.getX(); player.updateMovement(1, 0, 0.01);
        assertEquals(base * 1.3 * 0.01, player.getX() - x, 1e-8);
        assertEquals(0, player.getEquipment().defencePercent());
        player.getEquipment().toggle("guardian_boots+2"); assertEquals(base, player.getSpeed());
    }

}
