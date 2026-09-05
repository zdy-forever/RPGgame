package au.edu.anu.comp2120.world;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Checks geometry validation against non-finite and unrepresentable coordinates.
 *
 * @author Daoyan Zhu (u7782042)
 */
final class CollisionBoxTest {
    /** Creates a geometry test fixture. */
    CollisionBoxTest() {
    }

    /**
     * Non-finite values are rejected in every coordinate or dimension.
     *
     * @param value non-finite geometry value
     */
    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsNonFiniteGeometry(double value) {
        assertThrows(IllegalArgumentException.class, () -> new CollisionBox(value, 0, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new CollisionBox(0, value, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new CollisionBox(0, 0, value, 1));
        assertThrows(IllegalArgumentException.class, () -> new CollisionBox(0, 0, 1, value));
    }

    /** Zero sizes, negative sizes, overflow, and loss of extent fail explicitly. */
    @Test
    void rejectsInvalidExtents() {
        assertThrows(IllegalArgumentException.class, () -> new CollisionBox(0, 0, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new CollisionBox(0, 0, 1, -1));
        assertThrows(IllegalArgumentException.class,
                () -> new CollisionBox(Double.MAX_VALUE, 0, Double.MAX_VALUE, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new CollisionBox(0, Double.MAX_VALUE, 1, 1));
    }
}
