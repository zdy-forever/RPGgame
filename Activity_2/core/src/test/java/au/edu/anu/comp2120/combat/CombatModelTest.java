package au.edu.anu.comp2120.combat;

import static org.junit.jupiter.api.Assertions.*;

import au.edu.anu.comp2120.world.CollisionBox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Checks combat specification and hurtbox-adapter validation independently of simulation.
 *
 * @author Daoyan Zhu (u7782042)
 */
final class CombatModelTest {
    /** Creates the data-contract test fixture. */
    CombatModelTest() {
    }

    /** Positive finite extremes and zero damage/cooldown have explicit supported meanings. */
    @Test
    void acceptsValidExtremes() {
        AttackSpec maximum = new AttackSpec("maximum", Integer.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, maximum.damage());
        assertEquals("maximum", maximum.type());
        AttackSpec minimum = new AttackSpec("minimum", 0, Double.MIN_VALUE, 0);
        assertEquals(0, minimum.damage());
        assertEquals(Double.MIN_VALUE, minimum.activeSeconds());
    }

    /**
     * Active duration cannot be absent, negative, or non-finite.
     * @param duration invalid active seconds
     */
    @ParameterizedTest
    @ValueSource(doubles = {0, -1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsInvalidDuration(double duration) {
        assertThrows(IllegalArgumentException.class, () -> new AttackSpec("strike", 1, duration, 0));
    }

    /**
     * Cooldown cannot be negative or non-finite.
     * @param cooldown invalid cooldown seconds
     */
    @ParameterizedTest
    @ValueSource(doubles = {-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsInvalidCooldown(double cooldown) {
        assertThrows(IllegalArgumentException.class, () -> new AttackSpec("strike", 1, 1, cooldown));
    }

    /**
     * Negative damage cannot be smuggled into the pipeline as healing.
     * @param damage invalid damage
     */
    @ParameterizedTest
    @ValueSource(ints = {-1, Integer.MIN_VALUE})
    void rejectsNegativeDamage(int damage) {
        assertThrows(IllegalArgumentException.class, () -> new AttackSpec("strike", damage, 1, 0));
    }

    /** Null and blank attack labels are rejected. */
    @Test
    void rejectsMissingType() {
        assertThrows(NullPointerException.class, () -> new AttackSpec(null, 1, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new AttackSpec("", 1, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new AttackSpec(" \t", 1, 1, 0));
    }

    /** Adapter validation is atomic and never changes the underlying receiver's health. */
    @Test
    void validatesAdapterAndUpdatesGeometryIndependently() {
        Health health = new Health(100);
        CollisionBox box = new CollisionBox(0, 0, 10, 10);
        assertThrows(NullPointerException.class, () -> new Combatant(null, Faction.PLAYER, box));
        assertThrows(NullPointerException.class, () -> new Combatant(health, null, box));
        assertThrows(NullPointerException.class, () -> new Combatant(health, Faction.PLAYER, null));
        Combatant actor = new Combatant(health, Faction.PLAYER, box);
        assertThrows(NullPointerException.class, () -> actor.setHurtbox(null));
        assertSame(box, actor.getHurtbox());
        CollisionBox replacement = new CollisionBox(100, 100, 5, 8);
        actor.setHurtbox(replacement);
        assertSame(replacement, actor.getHurtbox());
        assertSame(health, actor.getEntity());
        assertEquals(Faction.PLAYER, actor.getFaction());
        assertEquals(100, health.getCurrentHp());
    }
}
