package au.edu.anu.comp2120.player;

import com.badlogic.gdx.Input;
import java.util.Objects;

/**
 * Translates held WASD keys into player movement intents without owning gameplay rules.
 *
 * @author Daoyan Zhu (u7782042)
 */
public final class PlayerInput {
    /** Input source supplied by the screen, or a test substitute. */
    private final Input input;

    /**
     * Creates a keyboard adapter using the application's existing input source.
     *
     * @param input keyboard input source
     * @throws NullPointerException if input is null
     */
    public PlayerInput(Input input) {
        this.input = Objects.requireNonNull(input, "input");
    }

    /**
     * Samples held keys; opposing keys cancel independently on each axis.
     *
     * @param player player to update
     * @param delta elapsed simulation time in seconds
     * @throws NullPointerException if player is null
     * @throws IllegalArgumentException if the player's movement rejects delta
     */
    public void update(Player player, double delta) {
        int horizontal = (input.isKeyPressed(Input.Keys.D) ? 1 : 0)
                - (input.isKeyPressed(Input.Keys.A) ? 1 : 0);
        int vertical = (input.isKeyPressed(Input.Keys.W) ? 1 : 0)
                - (input.isKeyPressed(Input.Keys.S) ? 1 : 0);
        player.updateMovement(horizontal, vertical, delta);
    }
}
