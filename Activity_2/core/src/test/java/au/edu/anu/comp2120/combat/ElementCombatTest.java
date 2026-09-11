package au.edu.anu.comp2120.combat;

import static org.junit.jupiter.api.Assertions.*;

import au.edu.anu.comp2120.enemy.Boss;
import au.edu.anu.comp2120.enemy.BossState;
import au.edu.anu.comp2120.enemy.Enemy;
import au.edu.anu.comp2120.screen.GameConfig;
import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.GameMap;
import au.edu.anu.comp2120.world.TestMaps;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests elements inside the real shared pipeline, including custom-rule queued child attacks and cleanup boundaries.
 * This explicit bonus-rule fixture preserves generic engine regression contracts; the formal table
 * and status behaviours are covered independently by ReactionCompletionTest.
 * @author Daoyan Zhu (u7782042)
 */
final class ElementCombatTest {
    /** Static projectile collision environment without rendering. */
    private final GameMap map = TestMaps.rectangle(600, 500, List.of());
    /** One shared arena with explicit custom bonus/child rules, not production defaults. */
    private final CombatSystem combat = new CombatSystem(new ElementRules(3, List.of(
            new ElementRules.Reaction("vaporize", ElementType.WATER, ElementType.FIRE, 10, null),
            new ElementRules.Reaction("vaporize", ElementType.FIRE, ElementType.WATER, 10, null),
            new ElementRules.Reaction("discharge", ElementType.WATER, ElementType.LIGHTNING, 0,
                    new ElementRules.DerivedEffect(5, 40, 0.15)),
            new ElementRules.Reaction("discharge", ElementType.LIGHTNING, ElementType.WATER, 0,
                    new ElementRules.DerivedEffect(5, 40, 0.15)))));
    /** Friendly-side source using the same Damageable contract as Player. */
    private final Combatant source = new Combatant(new Enemy(1000), Faction.PLAYER, new CollisionBox(10, 100, 20, 20));
    /** Hostile target whose HP and attachment are observed independently. */
    private final Combatant target = new Combatant(new Enemy(1000), Faction.ENEMY, new CollisionBox(100, 100, 20, 20));

    /** Registers canonical source and target models. */
    ElementCombatTest() {
        combat.register(source);
        combat.register(target);
    }

    /**
     * All five elements attach on valid hits, while NONE retains ordinary damage without status.
     * @param element incoming tag
     */
    @ParameterizedTest
    @EnumSource(ElementType.class)
    void validHitsApplyElementWithoutChangingBaseDamage(ElementType element) {
        Attack attack = hit(element, 10, target.getHurtbox());
        assertEquals(990, hp(target));
        assertEquals(element, attack.getSpecification().element());
        assertEquals(element, combat.getElementStatus(target).element());
        assertEquals(element == ElementType.NONE ? 0 : 3, combat.getElementStatus(target).remainingSeconds());
        assertEquals(ElementStatus.EMPTY, combat.getElementStatus(source));
        assertFalse(attack.isDerived());
        assertEquals(0, attack.getParentAttackId());
    }

    /** NONE does not remove/refresh attachment; same-element fresh hits refresh without a reaction. */
    @Test
    void refreshReplacementAndNoneHaveExplicitSemantics() {
        hit(ElementType.FIRE, 0, target.getHurtbox());
        combat.update(1);
        hit(ElementType.NONE, 10, target.getHurtbox());
        assertEquals(new ElementStatus(ElementType.FIRE, 2), combat.getElementStatus(target));
        hit(ElementType.FIRE, 10, target.getHurtbox());
        assertEquals(new ElementStatus(ElementType.FIRE, 3), combat.getElementStatus(target));
        hit(ElementType.ICE, 10, target.getHurtbox());
        assertEquals(new ElementStatus(ElementType.ICE, 3), combat.getElementStatus(target));
        assertEquals(970, hp(target));
        assertTrue(combat.getActiveAttacks().stream().noneMatch(Attack::isDerived));
    }

    /** Attachment expires at its exact duration and cannot react after the expiration boundary. */
    @Test
    void expiryPreventsLaterReactionAndRepeatedHitsDoNotRefresh() {
        hit(ElementType.WATER, 0, target.getHurtbox());
        combat.update(0.1);
        for (int i = 0; i < 30; i++) {
            combat.update(0);
        }
        assertEquals(2.9, combat.getElementStatus(target).remainingSeconds());
        combat.update(2.9);
        assertEquals(ElementStatus.EMPTY, combat.getElementStatus(target));
        hit(ElementType.FIRE, 10, target.getHurtbox());
        assertEquals(990, hp(target));
        assertEquals(ElementType.FIRE, combat.getElementStatus(target).element());
    }

    /**
     * Both supported pairs are symmetric and consume the triggering target's attachment.
     * @param first initial element
     * @param second incoming element
     * @param immediateDamage damage before deferred child settlement
     * @param children expected derived count
     */
    @ParameterizedTest
    @CsvSource({"WATER,FIRE,20,0", "FIRE,WATER,20,0", "WATER,LIGHTNING,10,1", "LIGHTNING,WATER,10,1"})
    void reactionsUseCentralDamageAndConsumeStatus(ElementType first, ElementType second, int immediateDamage, int children) {
        hit(first, 0, target.getHurtbox());
        hit(second, 10, target.getHurtbox());
        assertEquals(1000 - immediateDamage, hp(target));
        assertEquals(ElementStatus.EMPTY, combat.getElementStatus(target));
        assertEquals(children, combat.getActiveAttacks().stream().filter(Attack::isDerived).count());
    }

    /** Bonus arithmetic saturates instead of overflowing into negative healing or invalid Health damage. */
    @Test
    void bonusDamageSaturatesAtIntegerMaximum() {
        Combatant huge = new Combatant(new Enemy(Integer.MAX_VALUE), Faction.ENEMY, new CollisionBox(200, 100, 20, 20));
        combat.register(huge);
        hit(ElementType.WATER, 0, huge.getHurtbox());
        hit(ElementType.FIRE, Integer.MAX_VALUE, huge.getHurtbox());
        assertEquals(0, hp(huge));
        assertEquals(ElementStatus.EMPTY, combat.getElementStatus(huge));
    }

    /** A real projectile carries its element through swept impact and shares the same bonus calculation. */
    @Test
    void elementalProjectileReactsOnlyOnImpact() {
        hit(ElementType.WATER, 0, target.getHurtbox());
        Attack bolt = combat.tryProjectiles(source, new AttackSpec("bolt", 10, 0.1, 0, ElementType.FIRE),
                new ProjectileSpec(400, 4, 1, 1, 0), 200, 1, 0, map).get(0);
        combat.update(0.1);
        assertEquals(1000, hp(target));
        assertEquals(ElementType.WATER, combat.getElementStatus(target).element());
        combat.update(0.2);
        assertEquals(980, hp(target));
        assertFalse(bolt.isActive());
        assertEquals(ElementStatus.EMPTY, combat.getElementStatus(target));
    }

    /** Child attacks are deferred, attributable and non-recursive; neighbors keep their own attachment. */
    @Test
    void derivedAttackCannotReapplyOrRecursivelyReact() {
        Combatant neighbor = new Combatant(new Enemy(1000), Faction.ENEMY, new CollisionBox(130, 100, 10, 10));
        Combatant friendly = new Combatant(new Enemy(1000), Faction.PLAYER, new CollisionBox(110, 125, 10, 10));
        combat.register(neighbor);
        combat.register(friendly);
        hit(ElementType.WATER, 0, target.getHurtbox());
        hit(ElementType.WATER, 0, neighbor.getHurtbox());
        Attack parent = hit(ElementType.LIGHTNING, 10, target.getHurtbox());
        Attack child = combat.getActiveAttacks().stream().filter(Attack::isDerived).findFirst().orElseThrow();
        assertEquals(parent.getId(), child.getParentAttackId());
        assertSame(source, child.getSource());
        assertEquals("reaction-discharge", child.getSpecification().type());
        assertEquals(ElementType.NONE, child.getSpecification().element());
        assertEquals(new CollisionBox(70, 70, 80, 80), child.getHitbox());
        assertEquals(990, hp(target));
        assertEquals(1000, hp(neighbor));
        for (int i = 0; i < 100; i++) {
            combat.update(0);
            assertEquals(1, combat.getActiveAttacks().stream().filter(Attack::isDerived).count());
        }
        assertEquals(985, hp(target));
        assertEquals(995, hp(neighbor));
        assertEquals(1000, hp(friendly));
        assertEquals(ElementType.WATER, combat.getElementStatus(neighbor).element());
        assertEquals(ElementStatus.EMPTY, combat.getElementStatus(target));
        combat.update(0.2);
        assertFalse(child.isActive());
        assertTrue(combat.getActiveAttacks().isEmpty());
    }

    /** Two reacting targets produce two distinct events, never repeated children from one attack-target hit. */
    @Test
    void multiTargetReactionEventsHaveIndependentBoundedChildren() {
        Combatant other = new Combatant(new Enemy(1000), Faction.ENEMY, new CollisionBox(130, 100, 10, 10));
        combat.register(other);
        CollisionBox area = new CollisionBox(95, 95, 60, 40);
        hit(ElementType.WATER, 0, area);
        Attack parent = hit(ElementType.LIGHTNING, 10, area);
        List<Attack> children = combat.getActiveAttacks().stream().filter(Attack::isDerived).toList();
        assertEquals(2, children.size());
        assertNotEquals(children.get(0).getId(), children.get(1).getId());
        assertTrue(children.stream().allMatch(child -> child.getParentAttackId() == parent.getId()));
        combat.update(0);
        assertEquals(980, hp(target));
        assertEquals(980, hp(other));
        combat.update(0);
        assertEquals(980, hp(target));
        assertEquals(2, combat.getActiveAttacks().stream().filter(Attack::isDerived).count());
    }

    /** Derived fan-out has a hard live bound; overflow skips optional children without retrying consumed reactions. */
    @Test
    void reactionFanOutCannotAccumulateUnboundedChildren() {
        for (int i = 0; i < CombatSystem.MAX_DERIVED_ATTACKS + 5; i++) {
            combat.register(new Combatant(new Enemy(10000), Faction.ENEMY, target.getHurtbox()));
        }
        hit(ElementType.WATER, 0, target.getHurtbox());
        hit(ElementType.LIGHTNING, 0, target.getHurtbox());
        assertEquals(CombatSystem.MAX_DERIVED_ATTACKS,
                combat.getActiveAttacks().stream().filter(Attack::isDerived).count());
        combat.update(0.2);
        assertTrue(combat.getActiveAttacks().isEmpty());
        combat.update(0);
        assertTrue(combat.getActiveAttacks().isEmpty());
        assertEquals(ElementStatus.EMPTY, combat.getElementStatus(target));
    }

    /** Bonus-only reactions settle once even during many active frames of the same source invocation. */
    @Test
    void sameAttackCannotRepeatReactionAfterTargetReattachment() {
        hit(ElementType.WATER, 0, target.getHurtbox());
        hit(ElementType.FIRE, 10, target.getHurtbox());
        assertEquals(980, hp(target));
        hit(ElementType.WATER, 0, target.getHurtbox());
        for (int i = 0; i < 20; i++) {
            combat.update(0);
        }
        assertEquals(980, hp(target));
        assertEquals(ElementType.WATER, combat.getElementStatus(target).element());
    }

    /** Misses, edge contact, self, friends, dead and unregistered targets never acquire elements. */
    @Test
    void invalidTargetsAndMissesCannotApplyElement() {
        Combatant friend = new Combatant(new Enemy(), Faction.PLAYER, target.getHurtbox());
        Combatant dead = new Combatant(new Enemy(), Faction.ENEMY, target.getHurtbox());
        dead.getEntity().takeDamage(100);
        combat.register(friend);
        combat.register(dead);
        hit(ElementType.FIRE, 10, new CollisionBox(80, 100, 20, 20));
        assertEquals(ElementStatus.EMPTY, combat.getElementStatus(target));
        hit(ElementType.FIRE, 10, new CollisionBox(0, 0, 400, 400));
        assertEquals(ElementStatus.EMPTY, combat.getElementStatus(friend));
        assertEquals(ElementStatus.EMPTY, combat.getElementStatus(dead));
        assertEquals(ElementStatus.EMPTY, combat.getElementStatus(source));
        combat.unregister(target);
        assertThrows(IllegalArgumentException.class, () -> combat.getElementStatus(target));
        assertThrows(NullPointerException.class, () -> combat.getElementStatus(null));
    }

    /** Death clears attachments; unregister/re-register clears status but does not invalidate attack hit ledgers. */
    @Test
    void deathAndRegistrationBoundariesClearStatus() {
        hit(ElementType.ICE, 0, target.getHurtbox());
        combat.unregister(target);
        combat.register(target);
        combat.update(0);
        assertEquals(ElementStatus.EMPTY, combat.getElementStatus(target));
        hit(ElementType.WIND, 0, target.getHurtbox());
        target.getEntity().takeDamage(1000);
        assertEquals(ElementStatus.EMPTY, combat.getElementStatus(target));
        combat.update(0);
        ((Enemy) target.getEntity()).getHealth().restoreFull();
        assertEquals(ElementStatus.EMPTY, combat.getElementStatus(target));
        hit(ElementType.WATER, 0, target.getHurtbox());
        combat.clear();
        combat.register(target);
        assertEquals(ElementStatus.EMPTY, combat.getElementStatus(target));
    }

    /**
     * Deferred children cannot escape source cancellation, unregistration, death or map clearing.
     * @param mode lifecycle boundary to exercise
     */
    @ParameterizedTest
    @ValueSource(strings = {"cancel", "unregister", "death", "clear"})
    void lifecycleBoundariesCancelQueuedChildren(String mode) {
        hit(ElementType.WATER, 0, target.getHurtbox());
        hit(ElementType.LIGHTNING, 10, target.getHurtbox());
        Attack child = combat.getActiveAttacks().stream().filter(Attack::isDerived).findFirst().orElseThrow();
        switch (mode) {
            case "cancel" -> combat.cancelAttacks(source);
            case "unregister" -> combat.unregister(source);
            case "death" -> source.getEntity().takeDamage(1000);
            case "clear" -> combat.clear();
            default -> throw new AssertionError(mode);
        }
        combat.update(0);
        assertFalse(child.isActive());
        assertEquals(990, hp(target));
        assertTrue(combat.getActiveAttacks().stream().noneMatch(Attack::isDerived));
    }

    /** Boss phase/death and one-time loot observe elemental damage from the same Health model. */
    @Test
    void elementalDamageIntegratesWithBossPhaseAndDrop() {
        Boss boss = new Boss("element-boss", "Boss", new CollisionBox(250, 100, 64, 64), map, GameConfig.SENTINEL, combat);
        boss.getHealth().takeDamage(190);
        hit(ElementType.WATER, 0, boss.getBounds());
        hit(ElementType.FIRE, 10, boss.getBounds());
        boss.synchronizeOutcome();
        assertEquals(190, boss.getHealth().getCurrentHp());
        assertEquals(BossState.PHASE_TRANSITION, boss.getState());
        assertEquals(2, boss.getPhase());
        boss.getHealth().takeDamage(185);
        hit(ElementType.WATER, 0, boss.getBounds());
        hit(ElementType.LIGHTNING, 0, boss.getBounds());
        assertEquals(5, boss.getHealth().getCurrentHp());
        combat.update(0);
        boss.synchronizeOutcome();
        assertEquals(BossState.DEAD, boss.getState());
        assertTrue(boss.claimDrop().isPresent());
        combat.update(0);
        boss.synchronizeOutcome();
        assertTrue(boss.claimDrop().isEmpty());
    }

    /** Source cooldown is not charged again by internally derived invocations. */
    @Test
    void derivedAttackDoesNotAlterSourceCooldown() {
        hit(ElementType.WATER, 0, target.getHurtbox());
        combat.tryAttack(source, new AttackSpec("charged", 10, 0.2, 2, ElementType.LIGHTNING), target.getHurtbox());
        combat.update(0);
        assertEquals(2, combat.getCooldownRemaining(source));
        combat.update(0);
        assertEquals(2, combat.getCooldownRemaining(source));
        assertEquals(985, hp(target));
    }

    /** A lethal triggering hit can still emit its child; the dead primary target cannot be hit again. */
    @Test
    void lethalReactionCanAffectNearbyLivingTarget() {
        Combatant neighbor = new Combatant(new Enemy(5), Faction.ENEMY, new CollisionBox(130, 100, 10, 10));
        combat.register(neighbor);
        target.getEntity().takeDamage(990);
        hit(ElementType.WATER, 0, target.getHurtbox());
        hit(ElementType.LIGHTNING, 10, target.getHurtbox());
        assertEquals(0, hp(target));
        assertEquals(5, hp(neighbor));
        assertEquals(ElementType.NONE, combat.getElementStatus(target).element());
        combat.update(0);
        assertEquals(0, hp(neighbor));
    }

    /** Children born before their source dies later in the same update are cancelled before the next step. */
    @Test
    void sourceKilledLaterInSameStepCannotLeaveQueuedDamage() {
        hit(ElementType.WATER, 0, target.getHurtbox());
        combat.tryAttack(source, new AttackSpec("trigger", 10, 0.2, 0, ElementType.LIGHTNING), target.getHurtbox());
        combat.tryAttack(target, new AttackSpec("counter", 1000, 0.2, 0), source.getHurtbox());
        combat.update(0);
        assertEquals(0, hp(source));
        assertTrue(combat.getActiveAttacks().stream().noneMatch(Attack::isDerived));
        combat.update(0);
        assertEquals(990, hp(target));
    }

    /** Unrepresentable optional child geometry cannot break settled damage or leave consumed status behind. */
    @Test
    void invalidDerivedExtentSkipsOnlyOptionalEffect() {
        CombatSystem arena = new CombatSystem(new ElementRules(3, List.of(new ElementRules.Reaction(
                "huge", ElementType.WATER, ElementType.LIGHTNING, 2, new ElementRules.DerivedEffect(5, 8e307, 1)))));
        Combatant far = new Combatant(new Enemy(), Faction.ENEMY, new CollisionBox(1e308, 100, 1e307, 20));
        arena.register(source);
        arena.register(far);
        arena.tryAttack(source, new AttackSpec("prime", 0, 0.1, 0, ElementType.WATER), far.getHurtbox());
        arena.update(0);
        arena.tryAttack(source, new AttackSpec("trigger", 10, 0.1, 0, ElementType.LIGHTNING), far.getHurtbox());
        assertDoesNotThrow(() -> arena.update(0));
        assertEquals(88, hp(far));
        assertEquals(ElementType.NONE, arena.getElementStatus(far).element());
        assertTrue(arena.getActiveAttacks().stream().noneMatch(Attack::isDerived));
    }

    /** New statuses age once at the end of the update, matching the existing start-step combat sample. */
    @Test
    void newlyAppliedStatusAgesOncePerCombatUpdate() {
        combat.tryAttack(source, new AttackSpec("water", 0, 0.1, 0, ElementType.WATER), target.getHurtbox());
        combat.update(1);
        assertEquals(2, combat.getElementStatus(target).remainingSeconds());
        combat.update(2);
        assertEquals(ElementStatus.EMPTY, combat.getElementStatus(target));
    }

    /**
     * Invalid elapsed time cannot age attachment or settle a queued derived attack.
     * @param delta invalid time
     */
    @ParameterizedTest
    @ValueSource(doubles = {-1, Double.NaN, Double.POSITIVE_INFINITY})
    void invalidDeltaDoesNotMutateElementState(double delta) {
        hit(ElementType.FIRE, 0, target.getHurtbox());
        assertThrows(IllegalArgumentException.class, () -> combat.update(delta));
        assertEquals(new ElementStatus(ElementType.FIRE, 3), combat.getElementStatus(target));
        assertEquals(1000, hp(target));
    }

    /**
     * Submits a fresh direct invocation and settles only the current update's snapshot.
     * @param element incoming tag
     * @param damage base damage
     * @param hitbox target geometry
     * @return accepted direct attack
     */
    private Attack hit(ElementType element, int damage, CollisionBox hitbox) {
        Attack attack = combat.tryAttack(source, new AttackSpec("element-test", damage, 0.2, 0, element), hitbox).orElseThrow();
        combat.update(0);
        return attack;
    }

    /**
     * Reads real shared Health from this test's hostile model.
     * @param actor canonical Enemy-backed adapter
     * @return remaining HP
     */
    private int hp(Combatant actor) {
        return ((Enemy) actor.getEntity()).getHealth().getCurrentHp();
    }
}
