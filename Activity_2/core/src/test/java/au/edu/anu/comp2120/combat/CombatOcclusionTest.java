package au.edu.anu.comp2120.combat;

import static org.junit.jupiter.api.Assertions.*;

import au.edu.anu.comp2120.enemy.Boss;
import au.edu.anu.comp2120.enemy.BossConfig;
import au.edu.anu.comp2120.enemy.Enemy;
import au.edu.anu.comp2120.player.AbilityLoadout;
import au.edu.anu.comp2120.player.AbilitySlot;
import au.edu.anu.comp2120.player.AttackShape;
import au.edu.anu.comp2120.player.ClassDefinition;
import au.edu.anu.comp2120.player.Player;
import au.edu.anu.comp2120.player.PlayerAbilities;
import au.edu.anu.comp2120.player.PlayerClass;
import au.edu.anu.comp2120.player.PlayerClasses;
import au.edu.anu.comp2120.player.PlayerCommand;
import au.edu.anu.comp2120.screen.GameConfig;
import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.GameMap;
import au.edu.anu.comp2120.world.TestMaps;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Verifies world obstruction at the shared damage boundary using real classes and generic receivers.
 * Arrow Rain/ElementField are contract fixtures: those concrete skills do not exist in this checkout.
 * @author Daoyan Zhu (u7782042)
 */
final class CombatOcclusionTest {
    /** Creates independent collision/combat fixtures per test. */
    CombatOcclusionTest() {
    }

    /**
     * Real Archer/Mage projectiles and all fixed Mage/Katana slots obey the same static world.
     * @param type configured class
     * @param slot offensive slot
     * @param blocked whether a thin full-height wall separates player and Boss
     */
    @ParameterizedTest
    @CsvSource({"ARCHER,LIGHT,true", "ARCHER,LIGHT,false", "ARCHER,SPECIAL,true", "ARCHER,SPECIAL,false",
            "ARCHER,ULTIMATE,true", "ARCHER,ULTIMATE,false", "MAGE,LIGHT,true", "MAGE,LIGHT,false",
            "MAGE,SPECIAL,true", "MAGE,SPECIAL,false", "MAGE,ULTIMATE,true", "MAGE,ULTIMATE,false",
            "KATANA,LIGHT,true", "KATANA,LIGHT,false", "KATANA,SPECIAL,true", "KATANA,SPECIAL,false",
            "KATANA,ULTIMATE,true", "KATANA,ULTIMATE,false"})
    void classAttacksCannotHitBossThroughWall(PlayerClass type, AbilitySlot slot, boolean blocked) {
        GameMap map = arena(blocked);
        CombatSystem combat = new CombatSystem();
        Player player = new Player(new CollisionBox(100, 100, 20, 20), 100, map, 100, PlayerClasses.get(type));
        Combatant source = new Combatant(player, Faction.PLAYER, player.getBounds());
        combat.register(source);
        PlayerAbilities abilities = new PlayerAbilities(player, source, combat);
        Boss boss = new Boss("boss", "Boss", new CollisionBox(150, 100, 64, 64), map, GameConfig.SENTINEL, combat);
        assertTrue(abilities.update(0, command(slot)).isPresent());
        List<Attack> attacks = combat.getActiveAttacks();
        assertFalse(attacks.isEmpty());
        combat.update(2);
        assertEquals(blocked, boss.getHealth().getCurrentHp() == 400);
        assertTrue(attacks.stream().noneMatch(Attack::isActive));
        assertTrue(combat.getActiveAttacks().isEmpty());
        if (blocked) {
            assertEquals(ElementType.NONE, combat.getElementStatus(boss.getCombatant()).element());
        }
    }

    /**
     * A replaceable Archer O fixed-area contract stays at its cast centre and obeys walls.
     * This does not replace the catalog's existing five-arrow fan or invent a complete Arrow Rain skill.
     * @param blocked whether effect centre and target are separated
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void arrowRainFixedAreaContractUsesCapturedEffectCentre(boolean blocked) {
        GameMap map = arena(blocked);
        CombatSystem combat = new CombatSystem();
        Player player = new Player(new CollisionBox(100, 100, 20, 20), 100, map);
        Combatant source = new Combatant(player, Faction.PLAYER, player.getBounds());
        combat.register(source);
        PlayerAbilities abilities = new PlayerAbilities(player, source, combat);
        CollisionBox fixed = new CollisionBox(20, 60, 180, 100);
        AbilityLoadout old = PlayerClasses.get(PlayerClass.ARCHER).abilities();
        AbilityLoadout.AreaAttack rain = new AbilityLoadout.AreaAttack(new AttackSpec("arrow-rain-contract", 20, 1, 2),
                180, (body, range, dx, dy) -> fixed);
        abilities.selectClass(new ClassDefinition(PlayerClass.ARCHER,
                new AbilityLoadout(old.light(), old.special(), old.dash(), rain)));
        Combatant target = target(combat, 150);
        abilities.update(0, command(AbilitySlot.ULTIMATE));
        Attack attack = combat.getActiveAttacks().get(0);
        player.enterArea(map, 300, 200);
        abilities.update(0, PlayerCommand.NONE);
        assertEquals(fixed, attack.getHitbox());
        assertEquals(fixed, attack.getOrigin());
        combat.update(0);
        assertEquals(blocked ? 100 : 80, hp(target));
        combat.update(2);
        assertFalse(attack.isActive());
    }

    /**
     * The future ElementField adapter can submit its own centre and must use ordinary settlement.
     * @param blocked whether the fixed field centre is separated from the target
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void elementFieldContractCannotAttachOrDamageAcrossWall(boolean blocked) {
        CombatSystem combat = new CombatSystem();
        combat.bindWorld(arena(blocked));
        Combatant source = source(combat);
        Combatant target = target(combat, 150);
        CollisionBox field = new CollisionBox(20, 60, 180, 100);
        Attack attack = combat.tryAttack(source, new AttackSpec("element-field-contract", 10, 2, 0, ElementType.FIRE),
                field, field).orElseThrow();
        source.setHurtbox(new CollisionBox(300, 200, 20, 20));
        combat.update(0);
        assertEquals(blocked ? 100 : 90, hp(target));
        assertEquals(blocked ? ElementType.NONE : ElementType.FIRE, combat.getElementStatus(target).element());
        assertEquals(field, attack.getOrigin());
        combat.update(0);
        assertEquals(blocked ? 100 : 90, hp(target));
    }

    /** Occluded candidates do not consume hit ledgers or reactions and can be hit once after moving into view. */
    @Test
    void blockedTargetMayEnterSightDuringActiveWindowWithoutDuplicateSettlement() {
        CombatSystem combat = new CombatSystem();
        combat.bindWorld(arena(true));
        Combatant source = source(combat);
        Combatant target = target(combat, 150);
        CollisionBox area = new CollisionBox(20, 50, 200, 120);
        Attack attack = combat.tryAttack(source, new AttackSpec("fire", 10, 1, 0, ElementType.FIRE), area).orElseThrow();
        combat.update(0);
        assertEquals(100, hp(target));
        assertFalse(attack.hasHit(target.getEntity()));
        target.setHurtbox(new CollisionBox(120, 100, 8, 20));
        combat.update(0);
        assertEquals(90, hp(target));
        assertTrue(attack.hasHit(target.getEntity()));
        target.setHurtbox(new CollisionBox(150, 100, 20, 20));
        combat.update(0);
        target.setHurtbox(new CollisionBox(120, 100, 8, 20));
        combat.update(0);
        assertEquals(90, hp(target));
    }

    /**
     * A real WATER/LIGHTNING reaction produces one child which cannot reach the next receiver through a wall.
     * @param blocked whether a wall divides the reacting target from the collateral target
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void reactionAreaUsesItsOwnCentreAndCannotPropagateThroughWall(boolean blocked) {
        CombatSystem combat = new CombatSystem();
        combat.bindWorld(arena(blocked));
        Combatant source = source(combat);
        Combatant reacting = target(combat, 120);
        reacting.setHurtbox(new CollisionBox(120, 100, 8, 20));
        Combatant behind = target(combat, 150);
        combat.tryAttack(source, new AttackSpec("water", 0, 0.1, 0, ElementType.WATER), reacting.getHurtbox());
        combat.update(0.1);
        combat.tryAttack(source, new AttackSpec("lightning", 10, 0.1, 0, ElementType.LIGHTNING), reacting.getHurtbox());
        combat.update(0);
        Attack child = combat.getActiveAttacks().stream().filter(Attack::isDerived).findFirst().orElseThrow();
        assertEquals(child.getHitbox(), child.getOrigin());
        assertEquals(100, hp(behind), "Children settle on the next update");
        combat.update(0);
        assertEquals(85, hp(reacting));
        assertEquals(blocked ? 100 : 95, hp(behind));
        combat.update(0);
        assertEquals(blocked ? 100 : 95, hp(behind));
        combat.update(1);
        assertTrue(combat.getActiveAttacks().isEmpty());
    }

    /** A pre-existing attachment is neither consumed nor refreshed by a blocked triggering hit. */
    @Test
    void blockedReactionDoesNotConsumeStatusOrCreateChildren() {
        CombatSystem combat = new CombatSystem();
        combat.bindWorld(arena(true));
        Combatant source = source(combat);
        Combatant target = target(combat, 120);
        target.setHurtbox(new CollisionBox(120, 100, 8, 20));
        combat.tryAttack(source, new AttackSpec("water", 0, 0.1, 0, ElementType.WATER), target.getHurtbox());
        combat.update(0.1);
        target.setHurtbox(new CollisionBox(150, 100, 20, 20));
        ElementStatus before = combat.getElementStatus(target);
        combat.tryAttack(source, new AttackSpec("lightning", 10, 1, 0, ElementType.LIGHTNING), target.getHurtbox());
        combat.update(0);
        assertEquals(before, combat.getElementStatus(target));
        assertEquals(100, hp(target));
        assertTrue(combat.getActiveAttacks().stream().noneMatch(Attack::isDerived));
    }

    /** Effect-local origin is independent of caster sight, while melee retains the cast-time attacker origin. */
    @Test
    void originsCannotFollowCasterOrUseExtendedMeleeCentre() {
        CombatSystem combat = new CombatSystem();
        combat.bindWorld(arena(true));
        Combatant source = source(combat);
        Combatant target = target(combat, 150);
        CollisionBox slash = AttackShape.FORWARD.create(source.getHurtbox(), 180, 1, 0);
        Attack melee = combat.tryAttack(source, new AttackSpec("melee", 10, 1, 0), slash,
                AttackShape.FORWARD.origin(source.getHurtbox(), slash)).orElseThrow();
        source.setHurtbox(new CollisionBox(300, 100, 20, 20));
        combat.update(0);
        assertEquals(100, hp(target), "Long melee centre on the far side must not bypass the wall");
        assertEquals(new CollisionBox(100, 100, 20, 20), melee.getOrigin());
        CollisionBox area = new CollisionBox(140, 80, 100, 100);
        source.setHurtbox(new CollisionBox(100, 100, 20, 20));
        combat.tryAttack(source, new AttackSpec("local-area", 10, 1, 0), area, area);
        combat.update(0);
        assertEquals(90, hp(target), "Local AoE uses its own centre, not the distant caster");
    }

    /** Scope binding cannot silently change the blockers under a live attack; clear permits a fresh map. */
    @Test
    void mapBindingAndClearPreventStaleWorldUse() {
        CombatSystem combat = new CombatSystem();
        GameMap first = arena(true);
        combat.bindWorld(first);
        combat.bindWorld(first);
        Combatant source = source(combat);
        Combatant target = target(combat, 150);
        Attack old = combat.tryAttack(source, new AttackSpec("attack", 10, 1, 0), target.getHurtbox()).orElseThrow();
        GameMap next = arena(false);
        assertThrows(IllegalArgumentException.class, () -> combat.bindWorld(next));
        assertThrows(NullPointerException.class, () -> combat.bindWorld(null));
        combat.update(0);
        assertEquals(100, hp(target));
        combat.clear();
        assertFalse(old.isActive());
        combat.bindWorld(next);
        combat.register(source);
        combat.register(target);
        combat.tryAttack(source, new AttackSpec("new-map", 10, 1, 0), target.getHurtbox());
        combat.update(0);
        assertEquals(90, hp(target));
    }

    /**
     * Already-cast Boss patterns must recheck target occlusion, independently of the AI's initial corridor gate.
     * @param patternIndex one of the four configured phase patterns
     * @param blocked whether the target moves behind a wall before settlement
     */
    @ParameterizedTest
    @CsvSource({"0,true", "0,false", "1,true", "1,false", "2,true", "2,false", "3,true", "3,false"})
    void bossPatternsCannotHitPlayerAcrossWallAfterCasting(int patternIndex, boolean blocked) {
        GameMap map = TestMaps.rectangle(600, 400,
                blocked ? List.of(new CollisionBox(180, 0, 2, 400)) : List.of());
        CombatSystem combat = new CombatSystem();
        Combatant player = new Combatant(new Player(new CollisionBox(160, 110, 20, 20), 100, map),
                Faction.PLAYER, new CollisionBox(160, 110, 20, 20));
        combat.register(player);
        List<BossConfig.Pattern> patterns = List.of(GameConfig.SENTINEL.phaseOne().get(0),
                GameConfig.SENTINEL.phaseOne().get(1), GameConfig.SENTINEL.phaseTwo().get(0),
                GameConfig.SENTINEL.phaseTwo().get(1));
        BossConfig.Pattern pattern = patterns.get(patternIndex);
        BossConfig config = new BossConfig(400, GameConfig.SENTINEL.navigation(), 0.5, 0.75,
                List.of(pattern), List.of(pattern), "test-material", 1);
        Boss boss = new Boss("boss", "Boss", new CollisionBox(100, 100, 64, 64), map, config, combat);
        boss.update(player, 0);
        assertEquals(1, combat.getActiveAttacks().size(), "AI must actually cast before target moves");
        player.setHurtbox(new CollisionBox(200, 110, 20, 20));
        assertTrue(combat.getActiveAttacks().get(0).getHitbox().overlaps(player.getHurtbox()));
        combat.update(0);
        assertEquals(blocked ? 100 : 100 - pattern.attack().damage(),
                ((Player) player.getEntity()).getHealth().getCurrentHp());
    }

    /** Fixed-area tangency is clear, wall interiors block, and invalid anchors cannot consume cooldown. */
    @Test
    void fixedOriginBoundaryAndValidationRules() {
        CombatSystem combat = new CombatSystem();
        combat.bindWorld(TestMaps.rectangle(600, 400, List.of(new CollisionBox(130, 110, 2, 100))));
        Combatant source = source(combat);
        Combatant target = target(combat, 150);
        AttackSpec spec = new AttackSpec("edge", 10, 1, 0);
        assertThrows(NullPointerException.class, () -> combat.tryAttack(source, spec, target.getHurtbox(), null));
        assertEquals(0, combat.getCooldownRemaining(source));
        assertTrue(combat.getActiveAttacks().isEmpty());
        combat.tryAttack(source, spec, target.getHurtbox());
        combat.update(0);
        assertEquals(90, hp(target), "Line along y=110 only touches the wall boundary");
        combat.tryAttack(source, spec, target.getHurtbox(), new CollisionBox(130, 120, 2, 2));
        combat.update(0);
        assertEquals(90, hp(target), "An origin inside solid wall cannot deal damage");
        combat.tryAttack(source, spec, target.getHurtbox(), new CollisionBox(-20, 10, 2, 2));
        combat.update(0);
        assertEquals(90, hp(target), "An origin outside the world cannot deal damage");
    }

    /**
     * Creates immutable collision data with an optional separating wall.
     * @param blocked whether to include the wall
     * @return bounded arena
     */
    private GameMap arena(boolean blocked) {
        return TestMaps.rectangle(600, 400, blocked ? List.of(new CollisionBox(130, 0, 2, 400)) : List.of());
    }

    /**
     * Registers a generic source to ensure damage logic does not branch on Player/Boss types.
     * @param combat current scope
     * @return canonical source
     */
    private Combatant source(CombatSystem combat) {
        Combatant source = new Combatant(new Enemy(), Faction.PLAYER, new CollisionBox(100, 100, 20, 20));
        combat.register(source);
        return source;
    }

    /**
     * Registers a canonical damageable receiver at the fixture's fixed y coordinate of 100.
     * @param combat current scope
     * @param x receiver x
     * @return generic hostile adapter
     */
    private Combatant target(CombatSystem combat, double x) {
        Combatant target = new Combatant(new Enemy(), Faction.ENEMY, new CollisionBox(x, 100, 20, 20));
        combat.register(target);
        return target;
    }

    /**
     * Reads the fixture's shared health model.
     * @param actor fixture receiver
     * @return remaining HP
     */
    private int hp(Combatant actor) {
        return ((Enemy) actor.getEntity()).getHealth().getCurrentHp();
    }

    /**
     * Builds a stationary offensive input, preserving default right-facing direction.
     * @param slot requested slot
     * @return shared ability command
     */
    private PlayerCommand command(AbilitySlot slot) {
        return new PlayerCommand(0, 0, slot == AbilitySlot.LIGHT, slot == AbilitySlot.SPECIAL,
                false, slot == AbilitySlot.ULTIMATE);
    }
}
