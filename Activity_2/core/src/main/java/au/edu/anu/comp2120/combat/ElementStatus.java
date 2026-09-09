package au.edu.anu.comp2120.combat;

import java.util.Objects;

/**
 * Immutable observation of one target's temporary attachment, owned by its combat arena.
 * @param element attached element, or NONE for empty status
 * @param remainingSeconds positive finite duration for an attachment, exactly zero for NONE
 * @author Daoyan Zhu (u7782042)
 */
public record ElementStatus(ElementType element, double remainingSeconds) {
    /** Shared immutable empty observation. */
    public static final ElementStatus EMPTY = new ElementStatus(ElementType.NONE, 0);

    /**
     * Validates attachment state without changing a receiver or applying damage.
     * @param element required element identity
     * @param remainingSeconds legal remaining duration
     * @throws NullPointerException if element is null
     * @throws IllegalArgumentException if duration and element do not form a valid state
     */
    public ElementStatus {
        Objects.requireNonNull(element, "element");
        if (!Double.isFinite(remainingSeconds) || (element == ElementType.NONE
                ? remainingSeconds != 0 : remainingSeconds <= 0)) {
            throw new IllegalArgumentException("Attachment requires positive finite time; NONE requires zero");
        }
    }

    /**
     * Returns an aged immutable observation, clearing exactly at expiry.
     * @param delta finite non-negative simulation seconds
     * @return remaining attachment or EMPTY
     * @throws IllegalArgumentException if delta is negative or non-finite
     */
    public ElementStatus advance(double delta) {
        if (!Double.isFinite(delta) || delta < 0) {
            throw new IllegalArgumentException("Element time must be finite and non-negative");
        }
        return delta >= remainingSeconds ? EMPTY : new ElementStatus(element, remainingSeconds - delta);
    }
}
