package au.edu.anu.comp2120.combat;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Validates finite projectile tuning and bounded volley construction before runtime state changes.
 * @author Daoyan Zhu (u7782042)
 */
final class ProjectileSpecTest {
    /** Creates a configuration validation fixture. */
    ProjectileSpecTest() {
    }

    /**
     * Non-positive/non-finite physical values are rejected.
     * @param value invalid physical value
     */
    @ParameterizedTest
    @ValueSource(doubles = {0, -1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsInvalidFlight(double value) {
        assertThrows(IllegalArgumentException.class, () -> new ProjectileSpec(value, 4, 2, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new ProjectileSpec(100, value, 2, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new ProjectileSpec(100, 4, value, 1, 0));
    }

    /** Overflow/underflow and invalid fan/count combinations cannot create a volley. */
    @Test
    void validatesDistanceAndVolleyLimits() {
        assertThrows(IllegalArgumentException.class, () -> new ProjectileSpec(Double.MAX_VALUE, 4, 2, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new ProjectileSpec(Double.MIN_VALUE, 4, Double.MIN_VALUE, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new ProjectileSpec(100, 4, 2, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new ProjectileSpec(100, 4, 2, ProjectileSpec.MAX_VOLLEY_SIZE + 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new ProjectileSpec(100, 4, 2, 1, 1));
        for (double spread : new double[] {-1, 181, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> new ProjectileSpec(100, 4, 2, 5, spread));
        }
        assertEquals(9, new ProjectileSpec(100, 4, 2, 9, 180).count());
        assertEquals(0, new ProjectileSpec(100, 4, 2, 3, 0).spreadDegrees());
    }
}
