package au.edu.anu.comp2120.player;

/**
 * One frame of movement intent and edge-triggered ability requests, independent of keyboard APIs.
 * @param horizontal horizontal movement intent in [-1, 1]
 * @param vertical vertical movement intent in [-1, 1]
 * @param light whether light attack was newly requested
 * @param special whether special attack was newly requested
 * @param dash whether dash was newly requested
 * @param ultimate whether ultimate was newly requested
 * @author Daoyan Zhu (u7782042)
 */
public record PlayerCommand(int horizontal, int vertical, boolean light, boolean special,
        boolean dash, boolean ultimate) {
    /** Empty input, also used when travel consumes a frame. */
    public static final PlayerCommand NONE = new PlayerCommand(0, 0, false, false, false, false);

    /**
     * Validates movement before it can affect facing or ability state.
     * @param horizontal horizontal intent in [-1, 1]
     * @param vertical vertical intent in [-1, 1]
     * @param light light request
     * @param special special request
     * @param dash dash request
     * @param ultimate ultimate request
     * @throws IllegalArgumentException if either axis is outside [-1, 1]
     */
    public PlayerCommand {
        if (horizontal < -1 || horizontal > 1 || vertical < -1 || vertical > 1) {
            throw new IllegalArgumentException("Movement axes must be in [-1, 1]");
        }
    }
}
