package au.edu.anu.comp2120.combat;

import static org.junit.jupiter.api.Assertions.*;

import au.edu.anu.comp2120.enemy.Boss;
import au.edu.anu.comp2120.enemy.Enemy;
import au.edu.anu.comp2120.player.*;
import au.edu.anu.comp2120.screen.GameConfig;
import au.edu.anu.comp2120.world.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Built-in reaction matrix, real Boss controls, bounded fields and shared class damage.
 * All tests run through confirmed-hit settlement without graphics or a second damage engine.
 * @author Daoyan Zhu (u7782042)
 */
final class ReactionCompletionTest {
    /** Fresh pure-logic fixture for every case. */ ReactionCompletionTest() { }

    /**
     * Arrival order controls the multiplier; integer damage floors once at the shared boundary.
     * @param attached initial attachment
     * @param trigger incoming element
     * @param expected expected amplified damage
     */
    @ParameterizedTest
    @CsvSource({"FIRE,WATER,22", "WATER,FIRE,16", "ICE,FIRE,22", "FIRE,ICE,16"})
    void orderedAmplification(ElementType attached, ElementType trigger, int expected) {
        Fixture f = fixture(List.of());
        hit(f, attached, 0);
        Attack parent = hit(f, trigger, 11);
        assertEquals(1000 - expected, hp(f));
        assertEquals(ElementStatus.EMPTY, f.combat.getElementStatus(f.target));
        for (int i = 0; i < 100; i++) { f.combat.update(0); }
        assertEquals(1000 - expected, hp(f));
        assertTrue(parent.hasLandedHit(f.target.getEntity()));
        assertTrue(f.combat.getElementFields().isEmpty());
    }

    /** Saturation and explicit damage category validation prevent overflow or implicit null paths. */
    @Test
    void categoryAndMultiplierValidation() {
        assertEquals(DamageType.PHYSICAL, new AttackSpec("weapon", 1, 1, 0).damageType());
        assertEquals(DamageType.ELEMENTAL, new AttackSpec("magic", 1, 1, 0, ElementType.FIRE).damageType());
        assertThrows(NullPointerException.class, () -> new AttackSpec("bad", 1, 1, 0, ElementType.NONE, true, null));
        assertThrows(NullPointerException.class, () -> ElementRules.DEFAULT.find(ElementType.FIRE, null));
        assertThrows(NullPointerException.class, () -> new ElementRules.Reaction("bad", null, ElementType.WIND, 0, null));
        for (double value : new double[] {0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> new ElementRules.Reaction("bad",
                    ElementType.FIRE, ElementType.WATER, 0, null, value, ElementRules.Kind.DAMAGE));
        }
        Fixture f = fixture(List.of());
        hit(f, ElementType.FIRE, 0);
        hit(f, ElementType.WATER, Integer.MAX_VALUE);
        assertEquals(0, hp(f));
        assertEquals(ElementStatus.EMPTY, f.combat.getElementStatus(f.target));
    }

    /** Same elements refresh, NONE preserves, and reverse-WIND non-reactions replace attachment. */
    @Test
    void attachmentPolicies() {
        Fixture f = fixture(List.of());
        hit(f, ElementType.FIRE, 0);
        f.combat.update(1);
        hit(f, ElementType.NONE, 0);
        assertEquals(2, f.combat.getElementStatus(f.target).remainingSeconds());
        hit(f, ElementType.FIRE, 0);
        assertEquals(3, f.combat.getElementStatus(f.target).remainingSeconds());
        f.combat.update(3);
        hit(f, ElementType.WIND, 0);
        hit(f, ElementType.FIRE, 0);
        assertEquals(ElementType.FIRE, f.combat.getElementStatus(f.target).element());
        assertTrue(f.combat.getElementFields().isEmpty());
        hit(f, ElementType.WATER, 1);
        assertEquals(ElementStatus.EMPTY, f.combat.getElementStatus(f.target));
    }

    /**
     * Overload works in both arrival orders and shares child damage/poise/queued movement.
     * @param reverse whether lightning is attached first
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void overloadDamageAndPoise(boolean reverse) {
        Fixture f = fixture(List.of());
        f.target.getStatus().configurePoise(200);
        hit(f, reverse ? ElementType.LIGHTNING : ElementType.FIRE, 0);
        Attack parent = hit(f, reverse ? ElementType.FIRE : ElementType.LIGHTNING, 10);
        assertEquals(990, hp(f));
        assertEquals(100, f.target.getStatus().getPoise());
        assertEquals(0, f.target.getStatus().getStaggerRemaining());
        Attack child = f.combat.getActiveAttacks().stream().filter(Attack::isDerived).findFirst().orElseThrow();
        assertEquals(parent.getId(), child.getParentAttackId());
        assertEquals(ElementType.FIRE, child.getSpecification().element());
        assertEquals(DamageType.ELEMENTAL, child.getSpecification().damageType());
        assertTrue(f.target.getStatus().getKnockbackX() > 0);
        f.combat.update(0.1);
        assertEquals(970, hp(f));
        hit(f, ElementType.FIRE, 0);
        hit(f, ElementType.LIGHTNING, 0);
        assertEquals(ReactionTuning.STAGGER_SECONDS, f.target.getStatus().getStaggerRemaining());
        assertEquals(200, f.target.getStatus().getPoise(), "Poise refills on break");
    }

    /**
     * The real Boss movement owner resolves displacement against walls and map boundaries.
     * @param wall whether an obstacle, rather than the map edge, stops the Boss
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void bossKnockbackIsCollisionSafe(boolean wall) {
        GameMap map = TestMaps.rectangle(360, 300, wall ? List.of(new CollisionBox(320, 0, 20, 300)) : List.of());
        CombatSystem combat = new CombatSystem();
        Combatant source = new Combatant(new Enemy(1000), Faction.PLAYER, new CollisionBox(180, 100, 20, 20));
        combat.register(source);
        Boss boss = new Boss("boss", "Boss", new CollisionBox(250, 100, 64, 64), map, GameConfig.SENTINEL, combat);
        direct(combat, source, boss.getCombatant(), ElementType.FIRE, 0);
        direct(combat, source, boss.getCombatant(), ElementType.LIGHTNING, 0);
        assertTrue(boss.getCombatant().getStatus().blocksActions());
        assertEquals(250, boss.getBounds().x(), "No geometry mutation inside hit settlement");
        boss.update(source, 0);
        assertEquals(wall ? 256 : 296, boss.getBounds().x(), 1e-9);
        assertTrue(map.isWalkable(boss.getBounds()));
        assertEquals(boss.getBounds(), boss.getCombatant().getHurtbox());
        assertEquals(0, boss.getCombatant().getStatus().getKnockbackX());
        assertTrue(combat.getActiveAttacks().stream().noneMatch(a -> a.getSource() == boss.getCombatant()));
        combat.update(0.5);
        assertFalse(boss.getCombatant().getStatus().blocksActions());
    }

    /**
     * Charged status has exactly four ticks, no chains or zero-time damage, then expires.
     * @param reverse whether lightning is attached before water
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void chargedIsFiniteAndTargetLocal(boolean reverse) {
        Fixture f = fixture(List.of());
        Combatant neighbour = new Combatant(new Enemy(1000), Faction.ENEMY, f.target.getHurtbox());
        f.combat.register(neighbour);
        // Prime only the intended receiver by temporarily separating the neighbour.
        neighbour.setHurtbox(new CollisionBox(500, 100, 20, 20));
        hit(f, reverse ? ElementType.LIGHTNING : ElementType.WATER, 0);
        hit(f, reverse ? ElementType.WATER : ElementType.LIGHTNING, 10);
        assertEquals(2, f.target.getStatus().getChargedRemaining());
        f.combat.update(0.05); // End priming hitboxes before the neighbour enters, isolating periodic damage.
        neighbour.setHurtbox(f.target.getHurtbox());
        for (int i = 0; i < 100; i++) { f.combat.update(0); }
        assertEquals(990, hp(f));
        for (int i = 1; i <= 4; i++) {
            f.combat.update(0.5);
            assertEquals(990 - i * ReactionTuning.CHARGED_DAMAGE, hp(f));
        }
        assertEquals(0, f.target.getStatus().getChargedRemaining());
        assertEquals(1000, ((Enemy) neighbour.getEntity()).getHealth().getCurrentHp());
        f.combat.update(1000);
        assertEquals(958, hp(f));
        assertTrue(f.combat.getActiveAttacks().isEmpty());
        assertEquals(ElementStatus.EMPTY, f.combat.getElementStatus(f.target));
    }

    /** A refresh replaces a charged schedule, never stacks an unbounded collection of timers. */
    @Test
    void chargedRefreshAndLargeStep() {
        Fixture f = fixture(List.of());
        hit(f, ElementType.WATER, 0); hit(f, ElementType.LIGHTNING, 0);
        f.combat.update(0.5);
        hit(f, ElementType.WATER, 0); hit(f, ElementType.LIGHTNING, 0);
        assertEquals(2, f.target.getStatus().getChargedRemaining());
        f.combat.update(Double.MAX_VALUE);
        assertEquals(1000 - 5 * ReactionTuning.CHARGED_DAMAGE, hp(f));
        f.combat.update(0);
        assertEquals(0, f.target.getStatus().getChargedRemaining());
    }

    /** Freeze stops new Boss actions, preserves existing attacks and exposes correctly aging remaining time. */
    @Test
    void frozenBossRecoversWithoutCancellingExistingAttacks() {
        GameMap map = TestMaps.rectangle(700, 400, List.of());
        CombatSystem combat = new CombatSystem();
        Combatant source = new Combatant(new Enemy(1000), Faction.PLAYER, new CollisionBox(180, 100, 20, 20));
        combat.register(source);
        Boss boss = new Boss("boss", "Boss", new CollisionBox(250, 100, 64, 64), map, GameConfig.SENTINEL, combat);
        Attack existing = combat.tryAttack(boss.getCombatant(), new AttackSpec("existing", 7, 2, 0),
                source.getHurtbox()).orElseThrow();
        direct(combat, source, boss.getCombatant(), ElementType.WATER, 0);
        direct(combat, source, boss.getCombatant(), ElementType.ICE, 0);
        assertEquals(ReactionTuning.FREEZE_SECONDS, boss.getCombatant().getStatus().getFrozenRemaining());
        CollisionBox before = boss.getBounds();
        int attacks = combat.getActiveAttacks().size();
        boss.update(source, 0.4);
        assertEquals(before, boss.getBounds());
        assertEquals(attacks, combat.getActiveAttacks().size());
        assertTrue(existing.isActive());
        assertTrue(boss.getCombatant().getStatus().isFrozen());
        assertEquals(ReactionTuning.FREEZE_SECONDS, boss.getCombatant().getStatus().getFrozenRemaining(),
                "AI must not advance the shared status clock");
        combat.update(ReactionTuning.FREEZE_SECONDS / 2);
        assertEquals(ReactionTuning.FREEZE_SECONDS / 2, boss.getCombatant().getStatus().getFrozenRemaining(), 1e-9);
        combat.update(ReactionTuning.FREEZE_SECONDS / 2);
        assertEquals(0, boss.getCombatant().getStatus().getFrozenRemaining());
        assertFalse(boss.getCombatant().getStatus().blocksActions());
        boss.update(source, 0);
        boss.update(source, au.edu.anu.comp2120.enemy.BossConfig.MELEE_WINDUP);
        assertTrue(combat.getActiveAttacks().stream().anyMatch(a -> a.getSource() == boss.getCombatant() && a != existing));
        source.setHurtbox(new CollisionBox(100, 100, 20, 20));
        boss.update(source, 0.1);
        assertTrue(boss.getBounds().x() < before.x());
    }

    /** Physical vulnerability affects only weapon categories and reports its countdown through expiry. */
    @Test
    void superconductCalculationAndExpiry() {
        Fixture f = fixture(List.of());
        hit(f, ElementType.ICE, 0); hit(f, ElementType.LIGHTNING, 0);
        Attack child = f.combat.getActiveAttacks().stream().filter(Attack::isDerived).findFirst().orElseThrow();
        assertEquals(ElementType.ICE, child.getSpecification().element());
        assertEquals(ReactionTuning.RESISTANCE_SECONDS, f.target.getStatus().getResistanceRemaining());
        f.combat.update(0.1);
        assertEquals(ReactionTuning.RESISTANCE_SECONDS - 0.1, f.target.getStatus().getResistanceRemaining(), 1e-9);
        assertEquals(988, hp(f));
        assertEquals(1.25, f.target.getStatus().physicalMultiplier());
        f.combat.tryAttack(f.source, new AttackSpec("physical", 20, 0.05, 0, ElementType.NONE, true,
                DamageType.PHYSICAL), f.target.getHurtbox());
        f.combat.update(0);
        assertEquals(963, hp(f));
        f.combat.tryAttack(f.source, new AttackSpec("magic", 20, 0.05, 0, ElementType.FIRE, true,
                DamageType.ELEMENTAL), f.target.getHurtbox());
        f.combat.update(0);
        assertEquals(943, hp(f));
        f.combat.update(4);
        assertEquals(0, f.target.getStatus().getResistanceRemaining());
        assertEquals(1, f.target.getStatus().physicalMultiplier());
        hit(f, ElementType.NONE, 20);
        assertEquals(923, hp(f));
    }

    /**
     * Real class J submissions carry correct categories through the same vulnerability calculation.
     * @param type actual profession
     */
    @ParameterizedTest
    @EnumSource(PlayerClass.class)
    void classesUsePhysicalOrElementalDamage(PlayerClass type) {
        GameMap map = TestMaps.rectangle(600, 400, List.of());
        Player player = new Player(new CollisionBox(100, 100, 20, 20), 100, map, 100, PlayerClasses.get(type));
        CombatSystem combat = new CombatSystem();
        Combatant source = new Combatant(player, Faction.PLAYER, player.getBounds());
        Combatant target = new Combatant(new Enemy(1000), Faction.ENEMY, new CollisionBox(145, 100, 30, 30));
        combat.register(source); combat.register(target);
        PlayerAbilities abilities = new PlayerAbilities(player, source, combat);
        direct(combat, source, target, ElementType.ICE, 0);
        direct(combat, source, target, ElementType.LIGHTNING, 0);
        combat.update(0.1);
        int before = ((Enemy) target.getEntity()).getHealth().getCurrentHp();
        abilities.update(0, new PlayerCommand(0, 0, true, false, false, false));
        Attack attack = combat.getActiveAttacks().stream().filter(a -> !a.isDerived()).findFirst().orElseThrow();
        assertEquals(type == PlayerClass.MAGE ? DamageType.ELEMENTAL : DamageType.PHYSICAL,
                attack.getSpecification().damageType());
        combat.update(0.2); abilities.afterCombat();
        int damage = (int) (PlayerClasses.get(type).abilities().light().attack().damage()
                * (type == PlayerClass.MAGE ? 1 : 1.25));
        assertEquals(before - damage, ((Enemy) target.getEntity()).getHealth().getCurrentHp());
    }

    /**
     * Every elemental swirl, including WATER, produces damaging fixed geometry and finite ticks.
     * @param attached consumed element carried by the new field
     */
    @ParameterizedTest
    @EnumSource(value = ElementType.class, names = {"FIRE", "WATER", "ICE", "LIGHTNING"})
    void swirlFieldsAreFixedFiniteAndNonRecursive(ElementType attached) {
        Fixture f = fixture(List.of());
        hit(f, attached, 0);
        Attack parent = hit(f, ElementType.WIND, 0);
        ElementField field = f.combat.getElementFields().get(0);
        CollisionBox fixed = field.getBounds();
        assertEquals(attached, field.getDefinition().element());
        assertEquals(parent.getId(), field.getParentAttackId());
        hit(f, ElementType.FIRE, 0);
        f.combat.update(0.5);
        assertEquals(994, hp(f));
        assertEquals(ElementType.FIRE, f.combat.getElementStatus(f.target).element());
        assertEquals(1, f.combat.getElementFields().size(), "A child cannot create another field");
        for (int i = 0; i < 100; i++) { f.combat.update(0); }
        assertEquals(994, hp(f));
        f.target.setHurtbox(new CollisionBox(500, 100, 20, 20));
        f.combat.update(0.5);
        assertEquals(fixed, field.getBounds());
        assertEquals(994, hp(f), "Leaving the field avoids the next tick");
        f.target.setHurtbox(new CollisionBox(200, 100, 20, 20));
        f.combat.update(2);
        assertEquals(970, hp(f), "Exactly four remaining ticks, no missed-wave catch-up on reentry");
        assertFalse(field.isActive());
        assertTrue(f.combat.getElementFields().isEmpty());
        f.combat.update(100);
        assertEquals(970, hp(f));
    }

    /**
     * Reaction fields obey the effect-centre wall contract, including tangent contact.
     * @param geometry wall layout: none, interior blocker, or edge tangent
     */
    @ParameterizedTest
    @ValueSource(strings = {"none", "blocked", "tangent"})
    void fieldOcclusion(String geometry) {
        List<CollisionBox> walls = switch (geometry) {
            case "blocked" -> List.of(new CollisionBox(240, 90, 10, 80));
            case "tangent" -> List.of(new CollisionBox(240, 110, 10, 80));
            default -> List.of();
        };
        Fixture f = fixture(walls);
        hit(f, ElementType.WATER, 0); hit(f, ElementType.WIND, 0);
        f.target.setHurtbox(new CollisionBox(260, 100, 20, 20));
        f.combat.update(0.5);
        assertEquals(geometry.equals("blocked") ? 1000 : 994, hp(f));
    }

    /**
     * Death, detach, cancellation and scope clear stop statuses/fields with no old target retention.
     * @param boundary lifecycle boundary
     */
    @ParameterizedTest
    @ValueSource(strings = {"target-death", "target-unregister", "source-death", "source-unregister", "cancel", "clear"})
    void cleanupBoundaries(String boundary) {
        Fixture f = fixture(List.of());
        hit(f, ElementType.FIRE, 0); hit(f, ElementType.WIND, 0);
        ElementField field = f.combat.getElementFields().get(0);
        hit(f, ElementType.WATER, 0); hit(f, ElementType.LIGHTNING, 0);
        hit(f, ElementType.WATER, 0); hit(f, ElementType.ICE, 0);
        hit(f, ElementType.ICE, 0); hit(f, ElementType.LIGHTNING, 0);
        assertTrue(f.target.getStatus().blocksActions());
        switch (boundary) {
            case "target-death" -> f.target.getEntity().takeDamage(1000);
            case "target-unregister" -> f.combat.unregister(f.target);
            case "source-death" -> f.source.getEntity().takeDamage(1000);
            case "source-unregister" -> f.combat.unregister(f.source);
            case "cancel" -> f.combat.cancelAttacks(f.source);
            case "clear" -> f.combat.clear();
            default -> throw new AssertionError(boundary);
        }
        f.combat.update(0);
        assertEquals(0, f.target.getStatus().getChargedRemaining());
        if (boundary.startsWith("target") || boundary.equals("clear")) {
            assertFalse(f.target.getStatus().blocksActions());
            assertEquals(1, f.target.getStatus().physicalMultiplier());
        } else {
            assertFalse(field.isActive());
        }
        if (!boundary.startsWith("target")) { assertTrue(f.combat.getElementFields().isEmpty()); }
        f.combat.clear();
        assertFalse(field.isActive());
        assertEquals(0, f.target.getStatus().getChargedRemaining());
    }

    /** Limits and invalid data reject unbounded periodic schedules; clear cancels retained references. */
    @Test
    void fieldDefinitionAndCapacitySafety() {
        assertThrows(NullPointerException.class, () -> new ElementField.Definition(null, 1, 20, 2, 0.5));
        assertThrows(IllegalArgumentException.class, () -> new ElementField.Definition(ElementType.NONE, 1, 20, 2, 0.5));
        assertThrows(IllegalArgumentException.class, () -> new ElementField.Definition(ElementType.FIRE, -1, 20, 2, 0.5));
        for (double bad : new double[] {0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> new ElementField.Definition(ElementType.FIRE, 1, bad, 2, 0.5));
            assertThrows(IllegalArgumentException.class, () -> new ElementField.Definition(ElementType.FIRE, 1, 20, bad, 0.5));
            assertThrows(IllegalArgumentException.class, () -> new ElementField.Definition(ElementType.FIRE, 1, 20, 2, bad));
        }
        assertThrows(IllegalArgumentException.class, () -> new ElementField.Definition(ElementType.FIRE, 1, 20, 100, 0.001));
        Fixture f = fixture(List.of());
        for (int i = 0; i < ReactionTuning.MAX_FIELDS + 5; i++) {
            hit(f, ElementType.FIRE, 0); hit(f, ElementType.WIND, 0);
        }
        assertEquals(ReactionTuning.MAX_FIELDS, f.combat.getElementFields().size());
        f.combat.update(100);
        assertTrue(f.combat.getElementFields().isEmpty());
        assertTrue(f.combat.getActiveAttacks().isEmpty());
        assertEquals(1000 - CombatSystem.MAX_DERIVED_ATTACKS * ReactionTuning.FIELD_DAMAGE, hp(f));
    }

    /**
     * Stateful weapon/magic abilities preserve their explicit category, not just ordinary J.
     * @param skill counter/field execution to exercise
     * @param expected accepted base damage after physical vulnerability
     */
    @ParameterizedTest
    @CsvSource({"parry,15", "stance,125", "dragon,34", "rain,22", "circle,25"})
    void statefulSkillsPreserveDamageCategory(String skill, int expected) {
        PlayerClass type = skill.equals("circle") ? PlayerClass.MAGE
                : skill.equals("rain") ? PlayerClass.ARCHER : PlayerClass.KATANA;
        GameMap map = new GameMap("hub", "Test Hub", MapType.HUB, 600, 400, List.of(),
                java.util.Map.of("entry", new SpawnPoint(100, 100)), "entry");
        Player player = new Player(new CollisionBox(100, 100, 20, 20), 100, map, 100, PlayerClasses.get(type));
        CombatSystem combat = new CombatSystem();
        Combatant source = new Combatant(player, Faction.PLAYER, player.getBounds());
        Combatant target = new Combatant(new Enemy(1000), Faction.ENEMY, new CollisionBox(145, 100, 30, 30));
        combat.register(source); combat.register(target);
        PlayerAbilities abilities = new PlayerAbilities(player, source, combat);
        if (skill.equals("dragon")) { assertTrue(abilities.selectUltimateMode(UltimateMode.B)); }
        direct(combat, source, target, ElementType.ICE, 0);
        direct(combat, source, target, ElementType.LIGHTNING, 0);
        combat.update(0.1);
        int before = ((Enemy) target.getEntity()).getHealth().getCurrentHp();
        boolean parry = skill.equals("parry");
        abilities.update(0, new PlayerCommand(0, 0, false, parry, false, !parry));
        if (parry || skill.equals("stance")) {
            combat.tryAttack(target, new AttackSpec("incoming", 10, 0.05, 0), source.getHurtbox());
        }
        combat.update(0); abilities.afterCombat();
        switch (skill) {
            case "parry" -> step(abilities, combat, 0.09);
            case "stance" -> step(abilities, combat, 0);
            case "dragon" -> { step(abilities, combat, 0.05); step(abilities, combat, 0.01); }
            case "circle" -> { step(abilities, combat, 1.5); step(abilities, combat, 0); }
            case "rain" -> { }
            default -> throw new AssertionError(skill);
        }
        assertEquals(before - expected, ((Enemy) target.getEntity()).getHealth().getCurrentHp());
    }

    /** A tiny final frame cannot leave a field hitbox damaging a late entrant after expiry. */
    @Test
    void fieldTickHasNoPostExpiryHitbox() {
        Fixture f = fixture(List.of());
        hit(f, ElementType.FIRE, 0); hit(f, ElementType.WIND, 0);
        ElementField field = f.combat.getElementFields().get(0);
        f.target.setHurtbox(new CollisionBox(500, 100, 20, 20));
        f.combat.update(2.999);
        f.combat.update(0.001);
        assertFalse(field.isActive());
        assertTrue(f.combat.getActiveAttacks().isEmpty());
        f.target.setHurtbox(new CollisionBox(200, 100, 20, 20));
        f.combat.update(0);
        assertEquals(1000, hp(f));
    }

    /** Split-frame accumulation cannot multiply periodic events or prevent exact expiry. */
    @Test
    void splitStepsAndInvalidTimeAreSafe() {
        Fixture f = fixture(List.of());
        hit(f, ElementType.WATER, 0); hit(f, ElementType.LIGHTNING, 0);
        hit(f, ElementType.WATER, 0); hit(f, ElementType.WIND, 0);
        ElementField field = f.combat.getElementFields().get(0);
        assertThrows(IllegalArgumentException.class, () -> f.combat.update(Double.NaN));
        assertEquals(3, field.getRemainingSeconds());
        assertEquals(2, f.target.getStatus().getChargedRemaining());
        for (int i = 0; i < 30; i++) { f.combat.update(0.1); }
        assertEquals(1000 - 4 * ReactionTuning.CHARGED_DAMAGE - 6 * ReactionTuning.FIELD_DAMAGE, hp(f));
        assertFalse(field.isActive());
        assertEquals(0, f.target.getStatus().getChargedRemaining());
    }

    /**
     * Advances an existing controller in production settlement order without new input.
     * @param abilities common skill executor
     * @param combat shared arena
     * @param delta simulation seconds
     */
    private void step(PlayerAbilities abilities, CombatSystem combat, double delta) {
        abilities.update(delta, PlayerCommand.NONE);
        combat.update(delta);
        abilities.afterCombat();
    }

    /**
     * Builds an isolated shared arena with source left of the target.
     * @param walls static blockers
     * @return registered fixture
     */
    private Fixture fixture(List<CollisionBox> walls) {
        CombatSystem combat = new CombatSystem();
        combat.bindWorld(TestMaps.rectangle(700, 400, walls));
        Combatant source = new Combatant(new Enemy(1000), Faction.PLAYER, new CollisionBox(100, 100, 20, 20));
        Combatant target = new Combatant(new Enemy(1000), Faction.ENEMY, new CollisionBox(200, 100, 20, 20));
        combat.register(source); combat.register(target);
        return new Fixture(combat, source, target);
    }
    /**
     * Sends one direct hit and settles its current snapshot.
     * @param f fixture
     * @param element incoming tag
     * @param damage base damage
     * @return direct invocation
     */
    private Attack hit(Fixture f, ElementType element, int damage) {
        return direct(f.combat, f.source, f.target, element, damage);
    }
    /**
     * Uses the common pipeline, including world occlusion and once-only hit recording.
     * @param combat arena
     * @param source direct source
     * @param target receiver
     * @param element incoming tag
     * @param damage base damage
     * @return submitted invocation
     */
    private Attack direct(CombatSystem combat, Combatant source, Combatant target, ElementType element, int damage) {
        Attack attack = combat.tryAttack(source, new AttackSpec("test", damage, 0.05, 0, element),
                target.getHurtbox()).orElseThrow();
        combat.update(0);
        return attack;
    }
    /**
     * Reads the existing shared Health model.
     * @param f registered fixture
     * @return current target HP
     */
    private int hp(Fixture f) { return ((Enemy) f.target.getEntity()).getHealth().getCurrentHp(); }
    /**
     * Immutable references to real combat actors, not mock damage engines.
     * @param combat shared arena
     * @param source direct source
     * @param target hostile receiver
     * @author Daoyan Zhu (u7782042)
     */
    private record Fixture(CombatSystem combat, Combatant source, Combatant target) { }
}
