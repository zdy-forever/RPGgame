package au.edu.anu.comp2120.world;

/**
 * Immutable axis-aligned collision data independent of graphics libraries.
 * Edge contact is permitted; overlapping interiors are blocked.
 *
 * @param x bottom-left horizontal coordinate
 * @param y bottom-left vertical coordinate
 * @param width positive collision width
 * @param height positive collision height
 * @author Daoyan Zhu (u7782042)
 */
public record CollisionBox(double x, double y, double width, double height) {
    /**
     * Validates finite geometry with representable, positive extents.
     *
     * @param x bottom-left horizontal coordinate
     * @param y bottom-left vertical coordinate
     * @param width positive collision width
     * @param height positive collision height
     * @throws IllegalArgumentException if geometry is non-finite, non-positive, or loses its extent
     */
    public CollisionBox {
        if (!Double.isFinite(x) || !Double.isFinite(y)
                || !Double.isFinite(width) || !Double.isFinite(height)
                || width <= 0 || height <= 0
                || !Double.isFinite(x + width) || !Double.isFinite(y + height)
                || x + width <= x || y + height <= y) {
            throw new IllegalArgumentException("Collision geometry must have finite, positive extents");
        }
    }

    /**
     * Returns the right boundary.
     *
     * @return x plus width
     */
    public double right() {
        return x + width;
    }

    /**
     * Returns the top boundary.
     *
     * @return y plus height
     */
    public double top() {
        return y + height;
    }

    /**
     * Tests whether two rectangle interiors overlap.
     *
     * @param other rectangle to test
     * @return true for interior overlap, false for separation or edge contact
     * @throws NullPointerException if other is null
     */
    public boolean overlaps(CollisionBox other) {
        return x < other.right() && right() > other.x
                && y < other.top() && top() > other.y;
    }
}
