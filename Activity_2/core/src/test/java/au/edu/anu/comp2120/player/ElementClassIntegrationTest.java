package au.edu.anu.comp2120.player;

import static org.junit.jupiter.api.Assertions.*;

import au.edu.anu.comp2120.GameSession;
import au.edu.anu.comp2120.combat.Attack;
import au.edu.anu.comp2120.combat.AttackSpec;
import au.edu.anu.comp2120.combat.Combatant;
import au.edu.anu.comp2120.combat.CombatSystem;
import au.edu.anu.comp2120.combat.ElementRules;
import au.edu.anu.comp2120.combat.ElementType;
import au.edu.anu.comp2120.combat.Faction;
import au.edu.anu.comp2120.enemy.Enemy;
import au.edu.anu.comp2120.enemy.EnemyAI;
import au.edu.anu.comp2120.enemy.EnemyConfig;
import au.edu.anu.comp2120.screen.CombatDemo;
import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.GameMap;
import au.edu.anu.comp2120.world.MapCatalog;
import au.edu.anu.comp2120.world.TestMaps;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Verifies opt-in elemental class configuration, normal enemy attacks and actual demo status cleanup.
 * @author Daoyan Zhu (u7782042)
 */
final class ElementClassIntegrationTest {
    /** Collision-only arena shared as immutable fixture data. */
    private final GameMap map = TestMaps.rectangle(800, 600, List.of());

    /** Creates a fresh class integration fixture. */
    ElementClassIntegrationTest() {
    }

    /**
     * All three classes preserve elemental metadata across slot dispatch and share reaction damage.
     * @param type real class whose J delivery mode is tested
     */
    @ParameterizedTest
    @EnumSource(PlayerClass.class)
    void eachClassCanOptIntoElementsWithoutDuplicatingPlayer(PlayerClass type) {
        Player player = new Player(new CollisionBox(100, 100, 20, 20), 100, map, 100,
                elementalLight(type, ElementType.FIRE));
        CombatSystem combat = new CombatSystem();
        Combatant source = new Combatant(player, Faction.PLAYER, new CollisionBox(104, 104, 12, 12));
        Enemy enemy = new Enemy(1000);
        Combatant target = new Combatant(enemy, Faction.ENEMY, new CollisionBox(150, 100, 20, 20));
        combat.register(source);
        combat.register(target);
        PlayerAbilities abilities = new PlayerAbilities(player, source, combat);
        combat.tryAttack(source, new AttackSpec("prime", 0, 0.1, 0, ElementType.WATER), target.getHurtbox());
        combat.update(0);
        abilities.update(0, new PlayerCommand(0, 0, true, false, false, false));
        Attack light = combat.getActiveAttacks().get(1);
        assertEquals(ElementType.FIRE, light.getSpecification().element());
        combat.update(0.2);
        int baseDamage = PlayerClasses.get(type).abilities().light().attack().damage();
        assertEquals(1000 - (int) (baseDamage * 1.5), enemy.getHealth().getCurrentHp());
        assertEquals(ElementType.NONE, combat.getElementStatus(target).element());
        assertEquals(Player.class, player.getClass());
        assertEquals(100, player.getHealth().getCurrentHp());
        assertEquals(PlayerClasses.get(type).abilities().light().attack().cooldownSeconds(),
                abilities.getCooldownRemaining(AbilitySlot.LIGHT));
        assertEquals(ElementType.NONE, PlayerClasses.get(type).abilities().light().attack().element());
    }

    /** An ordinary EnemyAI attack can attach to the actual Player without a special elemental AI path. */
    @Test
    void enemyCanApplyElementsToPlayer() {
        CombatSystem combat = new CombatSystem();
        Player player = new Player(new CollisionBox(160, 100, 20, 20), 100, map);
        Combatant target = new Combatant(player, Faction.PLAYER, player.getBounds());
        combat.register(target);
        EnemyAI ai = new EnemyAI(new Enemy(), new CollisionBox(100, 100, 20, 20), map,
                new EnemyConfig(100, 200, 250, 80, new AttackSpec("element-enemy", 15, 0.2, 1, ElementType.ICE)), combat);
        ai.update(target, 0);
        combat.update(0);
        assertEquals(85, player.getHealth().getCurrentHp());
        assertEquals(ElementType.ICE, combat.getElementStatus(target).element());
    }

    /** An injected table changes reactions without altering class executors or the central damage path. */
    @Test
    void customRuleTableUsesSamePipeline() {
        CombatSystem combat = new CombatSystem(new ElementRules(5, List.of(
                new ElementRules.Reaction("custom", ElementType.ICE, ElementType.WIND, 7, null))));
        Combatant source = new Combatant(new Enemy(), Faction.PLAYER, new CollisionBox(0, 0, 20, 20));
        Enemy enemy = new Enemy();
        Combatant target = new Combatant(enemy, Faction.ENEMY, new CollisionBox(50, 0, 20, 20));
        combat.register(source);
        combat.register(target);
        combat.tryAttack(source, new AttackSpec("ice", 0, 0.1, 0, ElementType.ICE), target.getHurtbox());
        combat.update(0);
        assertEquals(5, combat.getElementStatus(target).remainingSeconds());
        combat.tryAttack(source, new AttackSpec("wind", 10, 0.1, 0, ElementType.WIND), target.getHurtbox());
        combat.update(0);
        assertEquals(83, enemy.getHealth().getCurrentHp());
    }

    /** Native-title labels reflect real attachments and reset/travel clear them without modifying class tuning. */
    @Test
    void demoShowsLabelsAndClearsStatusAtScopeBoundaries() {
        GameSession session = new GameSession(MapCatalog.createMaps(), MapCatalog.CROSSROADS, 28, 28, 180);
        CombatDemo demo = new CombatDemo(session);
        session.getPlayer().enterArea(session.getCurrentMap(), 320, 100);
        assertTrue(demo.getAbilities().selectClass(elementalLight(PlayerClass.MAGE, ElementType.WATER)));
        demo.update(0.2, new PlayerCommand(0, 0, true, false, false, false), false);
        assertTrue(demo.getStatus().contains("[WATER]"));
        demo.update(0, false, true);
        assertFalse(demo.getStatus().contains("[WATER]"));
        demo.update(0.2, new PlayerCommand(0, 0, true, false, false, false), false);
        assertTrue(demo.getStatus().contains("[WATER]"));
        session.switchMap(MapCatalog.HUB);
        demo.update(0, false, false);
        assertFalse(demo.getStatus().contains("[WATER]"));
    }

    /**
     * Copies only J's metadata while preserving class-specific geometry, projectile and other slots.
     * @param type existing class identity
     * @param element opt-in element
     * @return immutable configured class definition
     */
    private ClassDefinition elementalLight(PlayerClass type, ElementType element) {
        AbilityLoadout original = PlayerClasses.get(type).abilities();
        AbilityLoadout.AreaAttack light = original.light();
        AttackSpec spec = light.attack();
        AbilityLoadout.AreaAttack changed = new AbilityLoadout.AreaAttack(
                new AttackSpec(spec.type(), spec.damage(), spec.activeSeconds(), spec.cooldownSeconds(), element),
                light.range(), light.shape(), light.projectile());
        return new ClassDefinition(type, new AbilityLoadout(changed, original.special(), original.dash(), original.ultimate()));
    }
}
