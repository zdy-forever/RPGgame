package au.edu.anu.comp2120.combat;

import static org.junit.jupiter.api.Assertions.*;

import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.TestMaps;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Verifies generic interception, targetability, damage observation and mixed atomic volleys.
 * No player class is involved in these combat-core contracts.
 * @author Daoyan Zhu (u7782042)
 */
final class CombatCapabilitiesTest {
    /** One shared damage/element pipeline. */
    private final CombatSystem combat = new CombatSystem();
    /** Source health independent of receiver state. */
    private final Combatant source = new Combatant(new Health(100), Faction.ENEMY, new CollisionBox(0, 0, 20, 20));
    /** Receiver's existing health model. */
    private final Health health = new Health(100);
    /** Canonical receiver, with no class identity. */
    private final Combatant target = new Combatant(health, Faction.PLAYER, new CollisionBox(30, 0, 20, 20));

    /** Registers generic actors without graphics or a profession-specific receiver. */
    CombatCapabilitiesTest() {
        combat.register(source);
        combat.register(target);
    }

    /** A block consumes the invocation ledger but does not deliver HP loss, elements or landed-hit callbacks. */
    @Test
    void interceptionConsumesHitOnceWithoutDamageOrElements() {
        AtomicInteger calls = new AtomicInteger();
        target.setHitInterceptor(attack -> {
            calls.incrementAndGet();
            return true;
        });
        Attack attack = combat.tryAttack(source, new AttackSpec("blockable", 20, 1, 0, ElementType.WATER),
                target.getHurtbox()).orElseThrow();
        for (int i = 0; i < 50; i++) {
            combat.update(0);
        }
        assertEquals(1, calls.get());
        assertEquals(100, health.getCurrentHp());
        assertEquals(0, health.getDamageVersion());
        assertEquals(ElementType.NONE, combat.getElementStatus(target).element());
        assertFalse(attack.hasLandedHit(health));
        target.setHitInterceptor(null);
        combat.update(0);
        assertEquals(100, health.getCurrentHp());
    }

    /** Untargetable receivers can still receive non-blockable geometry damage through the normal pipeline. */
    @Test
    void untargetableAndUnblockableAreIndependent() {
        target.setTargetable(false);
        target.setHitInterceptor(attack -> fail("Unblockable attacks must not consult interception"));
        Attack attack = combat.tryAttack(source, new AttackSpec("ground", 20, 1, 0, ElementType.FIRE, false),
                target.getHurtbox()).orElseThrow();
        combat.update(0);
        assertFalse(target.isTargetable());
        assertEquals(80, health.getCurrentHp());
        assertEquals(1, health.getDamageVersion());
        assertTrue(attack.hasLandedHit(health));
        assertEquals(ElementType.FIRE, combat.getElementStatus(target).element());
    }

    /** Unregister and clear remove capability closures so old defensive state cannot reappear. */
    @Test
    void capabilitiesAreScopeLocal() {
        target.setTargetable(false);
        target.setHitInterceptor(attack -> true);
        combat.unregister(target);
        combat.register(target);
        assertTrue(target.isTargetable());
        combat.tryAttack(source, new AttackSpec("hit", 10, 1, 0), target.getHurtbox());
        combat.update(0);
        assertEquals(90, health.getCurrentHp());
        target.setHitInterceptor(attack -> true);
        target.setTargetable(false);
        combat.clear();
        combat.register(source);
        combat.register(target);
        assertTrue(target.isTargetable());
        combat.tryAttack(source, new AttackSpec("hit-after-clear", 10, 1, 0), target.getHurtbox());
        combat.update(0);
        assertEquals(80, health.getCurrentHp());
    }

    /** Effective damage stamps ignore zero/invalid/dead hits, survive healing and change on new actual loss. */
    @Test
    void damageStampTracksActualLossRatherThanCurrentHpDifference() {
        health.takeDamage(0);
        assertEquals(0, health.getDamageVersion());
        assertThrows(IllegalArgumentException.class, () -> health.takeDamage(-1));
        assertEquals(0, health.getDamageVersion());
        health.takeDamage(10);
        health.heal(10);
        assertEquals(100, health.getCurrentHp());
        assertEquals(1, health.getDamageVersion());
        health.takeDamage(Integer.MAX_VALUE);
        assertEquals(2, health.getDamageVersion());
        health.takeDamage(10);
        assertEquals(2, health.getDamageVersion());
        health.restoreFull();
        health.takeDamage(1);
        assertEquals(3, health.getDamageVersion());
    }

    /** Mixed-element volleys are validated atomically and retain distinct per-projectile metadata. */
    @Test
    void mixedVolleyUsesOneValidatedSharedLaunch() {
        var map = TestMaps.rectangle(300, 100, List.of());
        ProjectileSpec flight = new ProjectileSpec(100, 2, 1, 2, 0);
        AttackSpec water = new AttackSpec("water", 10, 0.1, 0, ElementType.WATER);
        AttackSpec fire = new AttackSpec("fire", 10, 0.1, 0, ElementType.FIRE);
        assertThrows(IllegalArgumentException.class,
                () -> combat.tryProjectiles(source, List.of(water), flight, 100, 1, 0, map));
        assertTrue(combat.getActiveAttacks().isEmpty());
        List<Attack> volley = combat.tryProjectiles(source, List.of(water, fire), flight, 100, 1, 0, map);
        assertEquals(2, volley.size());
        assertEquals(List.of(ElementType.WATER, ElementType.FIRE),
                volley.stream().map(attack -> attack.getSpecification().element()).toList());
        combat.update(0.5);
        assertEquals(70, health.getCurrentHp());
        assertTrue(combat.getActiveAttacks().isEmpty());
    }
}
