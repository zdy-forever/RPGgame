package au.edu.anu.comp2120.save;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Validates the strict field reader that rejects typos and unsupported data.
 * @author COMP2120 Assignment 3 group
 */
final class SaveFieldsTest {
    /** Creates a new {@code SaveFieldsTest} instance. */
    SaveFieldsTest() {
        // Test instances are created by JUnit.
    }

    /** A present text field is returned. */
    @Test void textReturnsPresentValue() {
        var fields = new SaveFields(Map.of("name", "hero"));
        assertEquals("hero", fields.text("name"));
    }

    /** A missing field is rejected. */
    @Test void textThrowsWhenMissing() {
        var fields = new SaveFields(Map.of("name", "hero"));
        assertThrows(IllegalArgumentException.class, () -> fields.text("level"));
    }

    /** Flags parse only true/false. */
    @Test void flagParsesTrueFalse() {
        var fields = new SaveFields(Map.of("on", "true", "off", "false"));
        assertTrue(fields.flag("on"));
        assertFalse(fields.flag("off"));
    }

    /** Non-boolean flag strings are rejected. */
    @Test void flagRejectsOtherValues() {
        var fields = new SaveFields(Map.of("weird", "yes"));
        assertThrows(IllegalArgumentException.class, () -> fields.flag("weird"));
    }

    /** Counts are bounded and non-negative. */
    @Test void countEnforcesBounds() {
        var fields = new SaveFields(Map.of("hp", "10"));
        assertEquals(10, fields.count("hp", 100));
        assertThrows(IllegalArgumentException.class, () -> fields.count("hp", 5));
        var neg = new SaveFields(Map.of("hp", "-1"));
        assertThrows(IllegalArgumentException.class, () -> neg.count("hp", 100));
    }

    /** Numbers must be finite and non-negative. */
    @Test void numberRequiresFiniteNonNegative() {
        var fields = new SaveFields(Map.of("x", "3.5"));
        assertEquals(3.5, fields.number("x"), 1e-9);
        var bad = new SaveFields(Map.of("x", "NaN"));
        assertThrows(IllegalArgumentException.class, () -> bad.number("x"));
        var neg = new SaveFields(Map.of("x", "-1"));
        assertThrows(IllegalArgumentException.class, () -> neg.number("x"));
    }

    /** Unused fields are rejected at finish. */
    @Test void finishRejectsUnusedFields() {
        var fields = new SaveFields(Map.of("a", "1", "b", "2"));
        fields.text("a");
        assertThrows(IllegalArgumentException.class, fields::finish);
        fields.text("b");
        fields.finish();
    }

    /** Accepting component keys marks them used so finish passes. */
    @Test void acceptMarksFieldsUsed() {
        var fields = new SaveFields(Map.of("a", "1", "b", "2"));
        fields.accept(Set.of("a", "b"));
        fields.finish();
    }
}
