package au.edu.anu.comp2120.screen;

import static org.junit.jupiter.api.Assertions.*;

import au.edu.anu.comp2120.GameSession;
import au.edu.anu.comp2120.TravelSystem;
import au.edu.anu.comp2120.combat.Attack;
import au.edu.anu.comp2120.combat.Combatant;
import au.edu.anu.comp2120.combat.ElementType;
import au.edu.anu.comp2120.combat.Faction;
import au.edu.anu.comp2120.enemy.Boss;
import au.edu.anu.comp2120.enemy.BossState;
import au.edu.anu.comp2120.player.AbilitySlot;
import au.edu.anu.comp2120.player.ClassDefinition;
import au.edu.anu.comp2120.player.PlayerClass;
import au.edu.anu.comp2120.player.PlayerCommand;
import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.MapCatalog;
import au.edu.anu.comp2120.world.TravelCatalog;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Cross-module placeholder acceptance checks using real movement, travel, AI, abilities and combat.
 * No assets or graphics context are loaded; camera assertions exercise the renderer's actual math.
 * @author Daoyan Zhu (u7782042)
 */
final class GameplayIntegrationTest {
    /** Existing built-in maps and persistent shared player. */
    private final GameSession session = new GameSession(MapCatalog.createMaps(), MapCatalog.HUB, 28, 28, 180);
    /** Actual automatic portal/fast-travel coordinator. */
    private final TravelSystem travel = new TravelSystem(session, TravelCatalog.createPortals(), TravelCatalog.createDestinations());
    /** Actual scene-side gameplay coordinator. */
    private final CombatDemo demo = new CombatDemo(session);

    /** Creates the normal Hub baseline without initializing graphics or loading assets. */
    GameplayIntegrationTest() {
    }

    /** WASD-scale movement enters a real portal; arrival protection, collision and camera use the new map. */
    @Test
    void hubMovementPortalArrivalCollisionAndCameraFormOneChain() {
        var player = session.getPlayer();
        var health = player.getHealth();
        frame(0.1, new PlayerCommand(0, 1, false, false, false, false));
        assertEquals(82, player.getY());
        for (int i = 0; i < 4; i++) {
            frame(0.1, new PlayerCommand(1, 0, false, false, false, false));
        }
        assertEquals(MapCatalog.TRAINING, session.getCurrentMap().getId());
        assertSame(player, session.getPlayer());
        assertSame(health, player.getHealth());
        assertEquals(80, player.getX());
        assertEquals(80, player.getY());
        assertSame(session.getCurrentMap(), player.getCollisionArea());
        for (int i = 0; i < 20; i++) {
            frame(0, PlayerCommand.NONE);
            assertEquals(MapCatalog.TRAINING, session.getCurrentMap().getId());
        }
        for (int i = 0; i < 10; i++) {
            frame(0.1, new PlayerCommand(1, 0, false, false, false, false));
        }
        assertEquals(212, player.getX());
        assertTrue(session.getCurrentMap().isWalkable(player.getBounds()));
        assertCameraInsideMap();
        assertTrue(demo.getEnemies().isEmpty());
        assertTrue(demo.getBoss().isPresent());
        assertEquals(2, demo.getCombatants().size());
        assertThrows(IllegalStateException.class, () -> travel.fastTravel(TravelCatalog.CROSSROADS_ENTRY));
        travel.unlockDestination(TravelCatalog.CROSSROADS_ENTRY);
        travel.fastTravel(TravelCatalog.CROSSROADS_ENTRY);
        frame(0, PlayerCommand.NONE);
        assertCameraInsideMap();
        assertEquals(80, player.getX());
        assertEquals(80, player.getY());
    }

    /**
     * Every class can defeat the real Crossroads Boss with normal slot commands and no HP manipulation.
     * @param type selected class
     */
    @ParameterizedTest
    @EnumSource(PlayerClass.class)
    void eachClassCompletesBattleAndCleansUpItsAttacks(PlayerClass type) {
        demo.update(0, PlayerCommand.NONE, false, type);
        if (type == PlayerClass.MAGE) {
            assertTrue(demo.getAbilities().selectUltimateMode(au.edu.anu.comp2120.player.UltimateMode.B));
        }
        session.switchMap(MapCatalog.CROSSROADS);
        frame(0, PlayerCommand.NONE);
        demo.update(0, PlayerCommand.NONE, false, type);
        // Begin in range: this scenario validates each class's delivery, not a ranged kiting strategy.
        session.getPlayer().enterArea(session.getCurrentMap(), 320, 100);
        Boss boss = demo.getBoss().orElseThrow();
        fightUntilNoHostiles(300);
        assertTrue(session.getPlayer().isAlive());
        assertTrue(demo.getLastDefeat().isEmpty(), "Auto-recovery must not count as winning");
        assertEquals(BossState.DEAD, boss.getState());
        assertTrue(demo.getEnemies().isEmpty());
        CollisionBox position = boss.getBounds();
        for (int i = 0; i < 30; i++) {
            frame(0.1, PlayerCommand.NONE);
        }
        assertTrue(demo.getActiveAttacks().isEmpty());
        assertEquals(position, boss.getBounds());
        assertEquals(type, session.getPlayer().getPlayerClass());
    }

    /** Crossroads combat reaches both Boss phases and one drop using actual player attacks, not direct HP writes. */
    @Test
    void bossFightProducesOneMaterialAndTravelDiscardsOldScope() {
        travel.unlockDestination(TravelCatalog.CROSSROADS_ENTRY);
        travel.fastTravel(TravelCatalog.CROSSROADS_ENTRY);
        frame(0, PlayerCommand.NONE);
        demo.update(0, PlayerCommand.NONE, false, PlayerClass.KATANA);
        Boss boss = demo.getBoss().orElseThrow();
        session.getPlayer().enterArea(session.getCurrentMap(), 320, 100);
        fightUntilNoHostiles(200);
        assertTrue(session.getPlayer().isAlive());
        assertEquals(BossState.DEAD, boss.getState());
        assertEquals(2, boss.getPhase());
        assertEquals(1, demo.getDrops().size());
        String dropId = demo.getDrops().get(0).id();
        for (int i = 0; i < 30; i++) {
            frame(0.1, PlayerCommand.NONE);
        }
        assertEquals(dropId, demo.getDrops().get(0).id());
        List<Combatant> oldActors = demo.getCombatants();
        List<Attack> oldAttacks = demo.getActiveAttacks();
        travel.fastTravel(TravelCatalog.HUB_ENTRY);
        frame(0, PlayerCommand.NONE);
        assertTrue(demo.getBoss().isEmpty());
        assertTrue(demo.getDrops().isEmpty());
        assertTrue(boss.claimDrop().isEmpty());
        assertTrue(oldAttacks.stream().noneMatch(Attack::isActive));
        assertTrue(oldActors.stream().noneMatch(demo.getCombatants()::contains));
        assertCameraInsideMap();
    }

    /** F4 reaches real reactions without assigning final elements to any built-in profession. */
    @Test
    void debugElementSelectionReachesReactionAndDerivedAttack() {
        session.switchMap(MapCatalog.CROSSROADS);
        frame(0, PlayerCommand.NONE);
        demo.update(0, PlayerCommand.NONE, false, PlayerClass.KATANA);
        // Fixture placement isolates the real player/Boss spell interaction from navigation already covered above.
        session.getPlayer().enterArea(session.getCurrentMap(), 320, 100);
        Boss boss = demo.getBoss().orElseThrow();
        cycleElement();
        assertEquals(ElementType.WATER, element());
        demo.update(0, new PlayerCommand(1, 0, true, false, false, false), false);
        assertTrue(demo.getStatus().contains("[WATER]"));
        demo.update(0.36, PlayerCommand.NONE, false);
        cycleElement();
        assertEquals(ElementType.FIRE, element());
        demo.update(0, new PlayerCommand(0, 0, true, false, false, false), false);
        assertEquals(325, boss.getHealth().getCurrentHp());
        demo.update(0.36, PlayerCommand.NONE, false);
        // Re-select FIRE after cycling through the other tags; the next LIGHTNING triggers Overload.
        for (int i = 0; i < 6; i++) {
            cycleElement();
        }
        assertEquals(ElementType.FIRE, element());
        demo.update(0, new PlayerCommand(0, 0, true, false, false, false), false);
        assertEquals(295, boss.getHealth().getCurrentHp());
        demo.update(0.36, PlayerCommand.NONE, false);
        cycleElement();
        assertEquals(ElementType.LIGHTNING, element());
        demo.update(0, new PlayerCommand(0, 0, true, false, false, false), false);
        List<Attack> children = demo.getActiveAttacks().stream().filter(Attack::isDerived).toList();
        assertFalse(children.isEmpty());
        int hp = boss.getHealth().getCurrentHp();
        demo.update(0, PlayerCommand.NONE, false);
        assertTrue(boss.getHealth().getCurrentHp() < hp);
        for (int i = 0; i < 10; i++) {
            demo.update(0.1, PlayerCommand.NONE, false);
        }
        assertTrue(children.stream().noneMatch(Attack::isActive));
        assertTrue(demo.getActiveAttacks().stream().noneMatch(Attack::isDerived));
    }

    /** F4 preserves state/cooldowns and safely rejects changes during a launch, live projectile or dash. */
    @Test
    void elementAndClassSelectionCannotResetCooldownOrRedirectLiveAttacks() {
        demo.update(0, PlayerCommand.NONE, false, PlayerClass.ARCHER);
        cycleElement();
        assertEquals(ElementType.WATER, element());
        CollisionBox before = session.getPlayer().getBounds();
        demo.update(0, new PlayerCommand(0, 0, true, false, false, false), false);
        Attack bolt = demo.getActiveAttacks().get(0);
        double cooldown = demo.getAbilities().getCooldownRemaining(AbilitySlot.LIGHT);
        cycleElement();
        assertEquals(ElementType.WATER, element());
        assertEquals(cooldown, demo.getAbilities().getCooldownRemaining(AbilitySlot.LIGHT));
        assertEquals(ElementType.WATER, bolt.getSpecification().element());
        demo.update(0.3, PlayerCommand.NONE, false);
        cycleElement();
        assertEquals(ElementType.WATER, element());
        demo.update(1, PlayerCommand.NONE, false);
        assertFalse(bolt.isActive());
        cycleElement();
        assertEquals(ElementType.FIRE, element());
        demo.update(0, PlayerCommand.NONE, false, PlayerClass.ARCHER, true);
        assertEquals(PlayerClass.ARCHER, session.getPlayer().getPlayerClass());
        assertEquals(ElementType.FIRE, element());
        assertEquals(before, session.getPlayer().getBounds());
        demo.update(0, new PlayerCommand(0, 0, false, false, true, false), false);
        cycleElement();
        assertEquals(ElementType.FIRE, element());
        demo.update(0, new PlayerCommand(1, 1, true, true, true, true), true, PlayerClass.KATANA, true);
        assertEquals(PlayerClass.ARCHER, session.getPlayer().getPlayerClass());
        assertEquals(ElementType.FIRE, element());
        assertTrue(demo.getActiveAttacks().isEmpty());
        assertEquals(before, session.getPlayer().getBounds());
    }

    /** An accepted element change preserves a running cooldown; rejected changes consume competing ability input. */
    @Test
    void elementSelectionCannotBypassCooldownOrDispatchCompetingCommands() {
        demo.update(0, PlayerCommand.NONE, false, PlayerClass.KATANA);
        demo.update(0, new PlayerCommand(0, 0, true, false, false, false), false);
        demo.update(0, new PlayerCommand(1, 1, false, true, false, true), false, null, true);
        assertEquals(ElementType.NONE, element());
        assertEquals(1, demo.getActiveAttacks().size());
        assertEquals(64, session.getPlayer().getX());
        demo.update(0.16, PlayerCommand.NONE, false);
        assertTrue(demo.getActiveAttacks().isEmpty());
        double cooldown = demo.getAbilities().getCooldownRemaining(AbilitySlot.LIGHT);
        assertTrue(cooldown > 0);
        cycleElement();
        assertEquals(ElementType.WATER, element());
        assertEquals(cooldown, demo.getAbilities().getCooldownRemaining(AbilitySlot.LIGHT));
        demo.update(0, new PlayerCommand(0, 0, true, false, false, false), false);
        assertTrue(demo.getActiveAttacks().isEmpty());
    }

    /** Invalid frames and dead players cannot mutate the selected element or start offensive abilities. */
    @Test
    void invalidFrameAndDeathRejectElementMutation() {
        for (double delta : new double[] {-1, Double.NaN, Double.POSITIVE_INFINITY, Double.MAX_VALUE}) {
            assertThrows(IllegalArgumentException.class,
                    () -> demo.update(delta, PlayerCommand.NONE, false, null, true));
            assertEquals(ElementType.NONE, element());
        }
        assertThrows(NullPointerException.class, () -> demo.update(0, null, false, null, true));
        session.getPlayer().takeDamage(session.getPlayer().getHealth().getMaxHp());
        demo.update(0, new PlayerCommand(1, 1, true, true, true, true), false, null, true);
        assertEquals(ElementType.NONE, element());
        assertFalse(session.getPlayer().isAlive());
        assertTrue(demo.getActiveAttacks().isEmpty());
        demo.update(0, PlayerCommand.NONE, true);
        cycleElement();
        assertTrue(session.getPlayer().isAlive());
        assertEquals(ElementType.WATER, element());
    }

    /** Repeated scope changes cancel flights, preserve HP/tuning/cooldowns and bound active actor membership. */
    @Test
    void repeatedTravelLeavesNoOldControllersOrAttacks() {
        demo.update(0, PlayerCommand.NONE, false, PlayerClass.ARCHER);
        cycleElement();
        ClassDefinition configured = session.getPlayer().getClassDefinition();
        var health = session.getPlayer().getHealth();
        for (int i = 0; i < 30; i++) {
            demo.update(0, new PlayerCommand(0, 0, false, false, false, true), false);
            List<Attack> attacks = demo.getActiveAttacks();
            List<Combatant> actors = demo.getCombatants();
            double cooldown = demo.getAbilities().getCooldownRemaining(AbilitySlot.ULTIMATE);
            session.switchMap(i % 2 == 0 ? MapCatalog.CROSSROADS : MapCatalog.TRAINING);
            demo.update(0, new PlayerCommand(1, 1, true, true, true, true), false, PlayerClass.MAGE, true);
            assertEquals(configured, session.getPlayer().getClassDefinition());
            assertSame(health, session.getPlayer().getHealth());
            assertEquals(cooldown, demo.getAbilities().getCooldownRemaining(AbilitySlot.ULTIMATE));
            assertTrue(attacks.stream().noneMatch(Attack::isActive));
            assertTrue(actors.stream().noneMatch(demo.getCombatants()::contains));
            assertTrue(demo.getActiveAttacks().isEmpty());
            assertEquals(80, session.getPlayer().getX());
            assertEquals(80, session.getPlayer().getY());
            assertEquals(2, demo.getCombatants().size());
            assertCameraInsideMap();
            demo.update(0, PlayerCommand.NONE, true);
        }
    }

    /**
     * Advances the same model ordering as GameScreen, including post-movement automatic portals.
     * @param delta simulated seconds
     * @param command shared movement/ability command
     */
    private void frame(double delta, PlayerCommand command) {
        demo.update(delta, command, false);
        if (travel.updatePortals()) {
            demo.update(0, PlayerCommand.NONE, false);
        }
        assertCameraInsideMap();
    }

    /**
     * Drives basic attack/movement commands toward the nearest live hostile, without direct damage or teleportation.
     * @param maximumFrames finite safety bound on test simulation
     */
    private void fightUntilNoHostiles(int maximumFrames) {
        for (int i = 0; i < maximumFrames; i++) {
            var target = demo.getCombatants().stream()
                    .filter(actor -> actor.getFaction() == Faction.ENEMY && actor.getEntity().isAlive())
                    .min(Comparator.comparingDouble(actor -> distance(actor.getHurtbox())));
            if (target.isEmpty()) {
                return;
            }
            assertTrue(session.getPlayer().isAlive(), "Player died during scripted combat: " + demo.getStatus());
            CollisionBox body = session.getPlayer().getBounds();
            CollisionBox enemy = target.orElseThrow().getHurtbox();
            double dx = enemy.x() + enemy.width() / 2 - (body.x() + body.width() / 2);
            double dy = enemy.y() + enemy.height() / 2 - (body.y() + body.height() / 2);
            frame(0.1, new PlayerCommand(Math.abs(dx) < 8 ? 0 : dx > 0 ? 1 : -1,
                    Math.abs(dy) < 8 ? 0 : dy > 0 ? 1 : -1, true,
                    session.getPlayer().getPlayerClass() == PlayerClass.ARCHER && i % 25 == 0, false, true));
        }
        fail("Combat did not complete within the bounded simulation: " + demo.getStatus());
    }

    /**
     * Measures target distance for the deterministic scripted input driver.
     * @param target hostile hurtbox
     * @return centre separation
     */
    private double distance(CollisionBox target) {
        CollisionBox body = session.getPlayer().getBounds();
        return Math.hypot(target.x() + target.width() / 2 - body.x() - body.width() / 2,
                target.y() + target.height() / 2 - body.y() - body.height() / 2);
    }

    /** Ensures the renderer's camera calculation stays inside each built-in map after every model frame. */
    private void assertCameraInsideMap() {
        CollisionBox bounds = session.getCurrentMap().getBounds();
        CollisionBox view = CameraFollow.viewFor(session.getPlayer().getBounds(), bounds,
                GameConfig.CAMERA_VIEW_WIDTH, GameConfig.CAMERA_VIEW_HEIGHT);
        assertTrue(view.x() >= bounds.x());
        assertTrue(view.y() >= bounds.y());
        assertTrue(view.x() + view.width() <= bounds.x() + bounds.width());
        assertTrue(view.y() + view.height() <= bounds.y() + bounds.height());
    }

    /** Requests the same single-cycle intent supplied by the F4 input adapter. */
    private void cycleElement() {
        demo.update(0, PlayerCommand.NONE, false, null, true);
    }

    /**
     * Reads the selected outgoing debug element rather than a target attachment.
     * @return current light-slot element
     */
    private ElementType element() {
        return session.getPlayer().getClassDefinition().abilities().light().attack().element();
    }
}
