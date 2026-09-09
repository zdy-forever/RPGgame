package au.edu.anu.comp2120.player;

import com.badlogic.gdx.Input;
import java.util.Objects;

/**
 * Translates held WASD and newly pressed J/K/L/O into intents without owning gameplay rules.
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
     * Samples movement and edge-triggered abilities once for the current frame.
     * Holding an ability key does not request automatic repetition.
     * @return immutable class-independent command
     */
    public PlayerCommand sample() {
        return new PlayerCommand(horizontal(), vertical(),
                input.isKeyJustPressed(Input.Keys.J), input.isKeyJustPressed(Input.Keys.K),
                input.isKeyJustPressed(Input.Keys.L), input.isKeyJustPressed(Input.Keys.O));
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
        player.updateMovement(horizontal(), vertical(), delta);
    }

    /**
     * Cancels opposing horizontal keys.
     * @return horizontal intent in [-1, 1]
     */
    private int horizontal() {
        return (input.isKeyPressed(Input.Keys.D) ? 1 : 0) - (input.isKeyPressed(Input.Keys.A) ? 1 : 0);
    }

    /**
     * Cancels opposing vertical keys.
     * @return vertical intent in [-1, 1]
     */
    private int vertical() {
        return (input.isKeyPressed(Input.Keys.W) ? 1 : 0) - (input.isKeyPressed(Input.Keys.S) ? 1 : 0);
    }
}
