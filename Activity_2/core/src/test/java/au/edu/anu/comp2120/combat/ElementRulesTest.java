package au.edu.anu.comp2120.combat;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests the complete ordered first-version element-pair matrix and immutable rule/status validation.
 * @author Daoyan Zhu (u7782042)
 */
final class ElementRulesTest {
    /** Creates an isolated pure-data fixture. */
    ElementRulesTest() {
    }

    /**
     * All distinct non-NONE pairs react except WIND attachment followed by another element.
     * @param first first element, including NONE
     */
    @ParameterizedTest
    @EnumSource(ElementType.class)
    void checksEveryPairIncludingUndefinedAndSameElement(ElementType first) {
        for (ElementType second : ElementType.values()) {
            boolean expected = first != ElementType.NONE && second != ElementType.NONE
                    && first != second && first != ElementType.WIND;
            var result = ElementRules.DEFAULT.find(first, second);
            assertEquals(expected, result.isPresent(), first + " + " + second);
            result.ifPresent(rule -> {
                assertEquals(first, rule.attachedElement());
                assertEquals(second, rule.triggeringElement());
            });
        }
    }

    /** Rule tables are copied and can be replaced or empty without altering the prototype table. */
    @Test
    void rulesAreConfigurableAndDefensivelyCopied() {
        var custom = new ElementRules.Reaction("custom", ElementType.ICE, ElementType.WIND, 7, null);
        var mutable = new ArrayList<>(List.of(custom));
        var rules = new ElementRules(5, mutable);
        mutable.clear();
        assertEquals(custom, rules.find(ElementType.ICE, ElementType.WIND).orElseThrow());
        assertTrue(rules.find(ElementType.WATER, ElementType.FIRE).isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> rules.reactions().clear());
        assertTrue(new ElementRules(1, List.of()).find(ElementType.FIRE, ElementType.WATER).isEmpty());
        assertTrue(ElementRules.DEFAULT.find(ElementType.WIND, ElementType.ICE).isEmpty());
    }

    /** Duplicate ordered pairs are rejected; reverse order and shared reaction labels are permitted. */
    @Test
    void rejectsDuplicateRulesAndMalformedPairs() {
        var first = new ElementRules.Reaction("first", ElementType.WATER, ElementType.FIRE, 1, null);
        var reverse = new ElementRules.Reaction("reverse", ElementType.FIRE, ElementType.WATER, 2, null);
        var sameId = new ElementRules.Reaction("first", ElementType.ICE, ElementType.WIND, 1, null);
        assertDoesNotThrow(() -> new ElementRules(3, List.of(first, reverse)));
        assertDoesNotThrow(() -> new ElementRules(3, List.of(first, sameId)));
        assertThrows(IllegalArgumentException.class, () -> new ElementRules(3, List.of(first, first)));
        assertThrows(IllegalArgumentException.class, () -> new ElementRules.Reaction("bad", ElementType.NONE, ElementType.FIRE, 1, null));
        assertThrows(IllegalArgumentException.class, () -> new ElementRules.Reaction("bad", ElementType.FIRE, ElementType.FIRE, 1, null));
        assertThrows(IllegalArgumentException.class, () -> new ElementRules.Reaction(" ", ElementType.FIRE, ElementType.WATER, 1, null));
        assertThrows(IllegalArgumentException.class, () -> new ElementRules.Reaction("bad", ElementType.FIRE, ElementType.WATER, -1, null));
        assertThrows(NullPointerException.class, () -> ElementRules.DEFAULT.find(null, ElementType.FIRE));
        assertThrows(NullPointerException.class, () -> new CombatSystem(null));
    }

    /**
     * Illegal attachment time cannot enter immutable state or an arena's rule table.
     * @param duration rejected positive-duration setting
     */
    @ParameterizedTest
    @ValueSource(doubles = {-1, 0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsInvalidDurations(double duration) {
        assertThrows(IllegalArgumentException.class, () -> new ElementRules(duration, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new ElementStatus(ElementType.FIRE, duration));
        assertThrows(IllegalArgumentException.class, () -> new ElementRules.DerivedEffect(1, 20, duration));
    }

    /** Status aging is immutable and exact at zero; NONE cannot carry a hidden duration. */
    @Test
    void statusIsImmutableAndExpiresAtBoundary() {
        ElementStatus status = new ElementStatus(ElementType.ICE, 3);
        assertEquals(status, status.advance(0));
        assertEquals(new ElementStatus(ElementType.ICE, 2), status.advance(1));
        assertEquals(3, status.remainingSeconds());
        assertEquals(ElementStatus.EMPTY, status.advance(3));
        assertEquals(ElementStatus.EMPTY, status.advance(Double.MAX_VALUE));
        assertEquals(ElementStatus.EMPTY, ElementStatus.EMPTY.advance(0));
        assertThrows(IllegalArgumentException.class, () -> new ElementStatus(ElementType.NONE, 1));
        assertThrows(NullPointerException.class, () -> new ElementStatus(null, 0));
        assertThrows(IllegalArgumentException.class, () -> status.advance(-1));
        assertThrows(IllegalArgumentException.class, () -> status.advance(Double.NaN));
    }

    /** Derived geometry and legacy/new AttackSpec constructors retain strict validated contracts. */
    @Test
    void validatesDerivedEffectsAndAttackElements() {
        assertEquals(ElementType.NONE, new AttackSpec("legacy", 1, 0.1, 0).element());
        assertThrows(NullPointerException.class, () -> new AttackSpec("bad", 1, 0.1, 0, null));
        assertThrows(IllegalArgumentException.class, () -> new ElementRules.DerivedEffect(-1, 20, 1));
        for (double radius : new double[] {0, -1, Double.NaN, Double.POSITIVE_INFINITY, Double.MAX_VALUE}) {
            assertThrows(IllegalArgumentException.class, () -> new ElementRules.DerivedEffect(1, radius, 1));
        }
    }
}
