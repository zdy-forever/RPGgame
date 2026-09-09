package au.edu.anu.comp2120.combat;

/**
 * Immutable non-piercing projectile and symmetric volley tuning, independent of class and artwork.
 * Damage/cooldown come from the existing attack slot; max range is supplied with the shot.
 * @param speed positive finite world units per second
 * @param size positive finite square collision size
 * @param lifetime positive finite maximum flight seconds
 * @param count number of arrows in one accepted volley, from 1 to MAX_VOLLEY_SIZE
 * @param spreadDegrees total symmetric fan angle in [0, 180]; single shots require zero
 * @author Daoyan Zhu (u7782042)
 */
public record ProjectileSpec(double speed, double size, double lifetime, int count, double spreadDegrees) {
    /** Upper bound on one volley, limiting construction and per-cast work. */
    public static final int MAX_VOLLEY_SIZE = 9;

    /**
     * Validates finite travel and bounded volley size before any cooldown is consumed.
     * @param speed world units per second
     * @param size square collision dimension
     * @param lifetime maximum flight seconds
     * @param count arrows in the volley
     * @param spreadDegrees total fan angle
     * @throws IllegalArgumentException if geometry, flight time, distance or volley tuning is invalid
     */
    public ProjectileSpec {
        if (!Double.isFinite(speed) || speed <= 0 || !Double.isFinite(size) || size <= 0
                || !Double.isFinite(lifetime) || lifetime <= 0
                || !Double.isFinite(speed * lifetime) || speed * lifetime <= 0
                || count < 1 || count > MAX_VOLLEY_SIZE || !Double.isFinite(spreadDegrees)
                || spreadDegrees < 0 || spreadDegrees > 180 || (count == 1 && spreadDegrees != 0)) {
            throw new IllegalArgumentException("Projectile requires finite positive travel and a bounded valid volley");
        }
    }
}
