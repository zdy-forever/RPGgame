package au.edu.anu.comp2120.player;

import com.badlogic.gdx.Input;
import java.util.Objects;
import java.util.Optional;

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
     * Samples debug class selection separately from shared J/K/L/O commands.
     * F1/F2/F3 select Archer/Mage/Katana; simultaneous presses prefer F1 then F2 then F3.
     * @return newly requested class, or empty; held keys do not auto-repeat
     */
    public Optional<PlayerClass> sampleClassSelection() {
        if (input.isKeyJustPressed(Input.Keys.F1)) {
            return Optional.of(PlayerClass.ARCHER);
        }
        if (input.isKeyJustPressed(Input.Keys.F2)) {
            return Optional.of(PlayerClass.MAGE);
        }
        if (input.isKeyJustPressed(Input.Keys.F3)) {
            return Optional.of(PlayerClass.KATANA);
        }
        return Optional.empty();
    }

    /**
     * Requests the next opt-in debug element without coupling keyboard input to reaction rules.
     * @return true only on an F4 key edge, not while the key remains held
     */
    public boolean sampleElementCycle() {
        return input.isKeyJustPressed(Input.Keys.F4);
    }

    /**
     * Requests an O mode toggle; the gameplay API, not this input adapter, enforces Hub-only selection.
     * @return true on an F5 key edge
     */
    public boolean sampleUltimateModeCycle() {
        return input.isKeyJustPressed(Input.Keys.F5);
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
