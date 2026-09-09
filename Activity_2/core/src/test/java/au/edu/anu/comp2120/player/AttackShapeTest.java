package au.edu.anu.comp2120.player;

import static org.junit.jupiter.api.Assertions.*;

import au.edu.anu.comp2120.world.CollisionBox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Verifies pure class hitbox strategies without sprite sizes or graphics context.
 * @author Daoyan Zhu (u7782042)
 */
final class AttackShapeTest {
    /** Deliberately non-square source hurtbox. */
    private final CollisionBox source = new CollisionBox(100, 100, 20, 10);

    /** Creates a geometry strategy fixture. */
    AttackShapeTest() {
    }

    /** Area attacks ignore orientation but preserve exact independent hurtbox dimensions. */
    @Test
    void areaExpandsAllDirections() {
        assertEquals(new CollisionBox(70, 70, 80, 70), AttackShape.AREA.create(source, 30, 1, 0));
        assertEquals(AttackShape.AREA.create(source, 30, 1, 0), AttackShape.AREA.create(source, 30, 0, -1));
    }

    /**
     * All facing directions use normalized reach and do not extend behind the source along either axis.
     * @param dx horizontal facing
     * @param dy vertical facing
     */
    @ParameterizedTest
    @CsvSource({"1,0", "-1,0", "0,1", "0,-1", "1,1", "1,-1", "-1,1", "-1,-1", "3,4"})
    void forwardUsesNormalizedDirection(double dx, double dy) {
        CollisionBox box = AttackShape.FORWARD.create(source, 30, dx, dy);
        double length = Math.hypot(dx, dy);
        assertEquals(100 + Math.min(0, dx / length * 30), box.x(), 1e-9);
        assertEquals(100 + Math.min(0, dy / length * 30), box.y(), 1e-9);
        assertEquals(20 + Math.abs(dx / length * 30), box.width(), 1e-9);
        assertEquals(10 + Math.abs(dy / length * 30), box.height(), 1e-9);
    }

    /**
     * Invalid reach is rejected by both built-in implementations.
     * @param range rejected reach
     */
    @ParameterizedTest
    @ValueSource(doubles = {0, -1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.MAX_VALUE})
    void rejectsInvalidRange(double range) {
        for (AttackShape shape : new AttackShape[] {AttackShape.AREA, AttackShape.FORWARD}) {
            assertThrows(IllegalArgumentException.class, () -> shape.create(source, range, 1, 0));
        }
    }

    /** Invalid facing, null source and unrepresentable geometry never produce an invalid rectangle. */
    @Test
    void rejectsInvalidFacingAndOverflow() {
        for (AttackShape shape : new AttackShape[] {AttackShape.AREA, AttackShape.FORWARD}) {
            assertThrows(NullPointerException.class, () -> shape.create(null, 1, 1, 0));
            assertThrows(IllegalArgumentException.class, () -> shape.create(source, 1, 0, 0));
            assertThrows(IllegalArgumentException.class, () -> shape.create(source, 1, Double.NaN, 0));
            assertThrows(IllegalArgumentException.class, () -> shape.create(source, 1, 0, Double.POSITIVE_INFINITY));
            CollisionBox huge = new CollisionBox(1e308, 0, 1e307, 20);
            assertThrows(IllegalArgumentException.class, () -> shape.create(huge, 8e307, 1, 0));
        }
    }
}
