package au.edu.anu.comp2120.world;

/**
 * Immutable bottom-left placement anchor in world coordinates. Map construction validates
 * the point; entry separately validates the entering body's complete dimensions.
 *
 * @param x finite, non-negative horizontal coordinate
 * @param y finite, non-negative vertical coordinate
 * @author Daoyan Zhu (u7782042)
 */
public record SpawnPoint(double x, double y) {
    /**
     * Creates a finite placement anchor.
     *
     * @param x horizontal world coordinate
     * @param y vertical world coordinate
     * @throws IllegalArgumentException if either coordinate is negative or non-finite
     */
    public SpawnPoint {
        if (!Double.isFinite(x) || !Double.isFinite(y) || x < 0 || y < 0) {
            throw new IllegalArgumentException("Spawn coordinates must be finite and non-negative");
        }
    }
}
