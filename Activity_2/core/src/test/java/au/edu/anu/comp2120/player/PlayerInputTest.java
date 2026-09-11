package au.edu.anu.comp2120.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.TestMaps;
import com.badlogic.gdx.Input;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Verifies actual WASD mapping and key cancellation with a fake keyboard source.
 *
 * @author Daoyan Zhu (u7782042)
 */
final class PlayerInputTest {
    /** Creates a keyboard adapter test fixture. */
    PlayerInputTest() {
    }

    /** F4 is an independent key-edge request; holding it cannot repeatedly cycle elements. */
    @Test
    void elementCycleUsesKeyEdgesWithoutChangingOtherMappings() {
        Set<Integer> held = new HashSet<>(Set.of(Input.Keys.F4, Input.Keys.W));
        Set<Integer> pressed = new HashSet<>();
        Input keyboard = (Input) Proxy.newProxyInstance(Input.class.getClassLoader(),
                new Class<?>[] {Input.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "isKeyPressed" -> held.contains((Integer) args[0]);
                    case "isKeyJustPressed" -> pressed.contains((Integer) args[0]);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        PlayerInput input = new PlayerInput(keyboard);
        assertFalse(input.sampleElementCycle());
        pressed.addAll(Set.of(Input.Keys.F4, Input.Keys.J, Input.Keys.F2));
        assertTrue(input.sampleElementCycle());
        assertEquals(new PlayerCommand(0, 1, true, false, false, false), input.sample());
        assertEquals(Optional.of(PlayerClass.MAGE), input.sampleClassSelection());
        pressed.clear();
        assertFalse(input.sampleElementCycle());
        assertEquals(new PlayerCommand(0, 1, false, false, false, false), input.sample());
    }

    /** F1/F2/F3 select classes on key edges with fixed priority and do not alter ability mapping. */
    @Test
    void mapsClassSelectionSeparatelyFromAbilityCommands() {
        Set<Integer> pressed = new HashSet<>();
        Input keyboard = (Input) Proxy.newProxyInstance(Input.class.getClassLoader(),
                new Class<?>[] {Input.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "isKeyJustPressed" -> pressed.contains((Integer) args[0]);
                    case "isKeyPressed" -> false;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        PlayerInput input = new PlayerInput(keyboard);
        int[] keys = {Input.Keys.F1, Input.Keys.F2, Input.Keys.F3};
        PlayerClass[] types = {PlayerClass.ARCHER, PlayerClass.MAGE, PlayerClass.KATANA};
        for (int i = 0; i < keys.length; i++) {
            pressed.clear();
            pressed.add(keys[i]);
            assertEquals(Optional.of(types[i]), input.sampleClassSelection());
            assertEquals(PlayerCommand.NONE, input.sample());
            pressed.clear();
            assertEquals(Optional.empty(), input.sampleClassSelection());
        }
        pressed.addAll(Set.of(Input.Keys.F1, Input.Keys.F2, Input.Keys.F3, Input.Keys.J));
        assertEquals(Optional.of(PlayerClass.ARCHER), input.sampleClassSelection());
        assertEquals(new PlayerCommand(0, 0, true, false, false, false), input.sample());
        pressed.remove(Input.Keys.F1);
        assertEquals(Optional.of(PlayerClass.MAGE), input.sampleClassSelection());
    }

    /** Actual J/K/L/O mappings use newly pressed keys rather than held-key repetition. */
    @Test
    void mapsAllAbilityKeysAndSamplesSimultaneousIntent() {
        Set<Integer> held = new HashSet<>();
        Set<Integer> pressed = new HashSet<>();
        Input keyboard = (Input) Proxy.newProxyInstance(Input.class.getClassLoader(),
                new Class<?>[] {Input.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "isKeyPressed" -> held.contains((Integer) args[0]);
                    case "isKeyJustPressed" -> pressed.contains((Integer) args[0]);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        PlayerInput input = new PlayerInput(keyboard);
        int[] keys = {Input.Keys.J, Input.Keys.K, Input.Keys.L, Input.Keys.O};
        for (int i = 0; i < keys.length; i++) {
            pressed.clear();
            pressed.add(keys[i]);
            assertEquals(new PlayerCommand(0, 0, i == 0, i == 1, i == 2, i == 3), input.sample());
        }
        held.addAll(Set.of(Input.Keys.W, Input.Keys.D, Input.Keys.J, Input.Keys.K, Input.Keys.L, Input.Keys.O));
        pressed.clear();
        assertEquals(new PlayerCommand(1, 1, false, false, false, false), input.sample());
        pressed.addAll(Set.of(Input.Keys.J, Input.Keys.K, Input.Keys.L, Input.Keys.O));
        assertEquals(new PlayerCommand(1, 1, true, true, true, true), input.sample());
        held.addAll(Set.of(Input.Keys.A, Input.Keys.S));
        assertEquals(new PlayerCommand(0, 0, true, true, true, true), input.sample());
    }

    /** Pressing, releasing, opposing, and diagonal keys obey normalized movement rules. */
    @Test
    void mapsHeldKeysAndCancelsOpposites() {
        Set<Integer> heldKeys = new HashSet<>();
        Input keyboard = (Input) Proxy.newProxyInstance(Input.class.getClassLoader(),
                new Class<?>[] {Input.class}, (proxy, method, args) -> {
                    if (method.getName().equals("isKeyPressed")) {
                        return heldKeys.contains((Integer) args[0]);
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        PlayerInput input = new PlayerInput(keyboard);
        Player player = new Player(new CollisionBox(100, 100, 10, 10), 10,
                TestMaps.rectangle(500, 500, List.of()));
        int[] keys = {Input.Keys.W, Input.Keys.A, Input.Keys.S, Input.Keys.D};
        double[][] positions = {{100, 110}, {90, 110}, {90, 100}, {100, 100}};
        for (int i = 0; i < keys.length; i++) {
            heldKeys.clear();
            heldKeys.add(keys[i]);
            input.update(player, 1);
            assertEquals(positions[i][0], player.getX());
            assertEquals(positions[i][1], player.getY());
        }
        heldKeys.clear();
        input.update(player, 1);
        assertEquals(100, player.getX());
        assertEquals(100, player.getY());
        heldKeys.addAll(Set.of(Input.Keys.W, Input.Keys.A, Input.Keys.S, Input.Keys.D));
        input.update(player, 1);
        assertEquals(100, player.getX());
        assertEquals(100, player.getY());
        heldKeys.remove(Input.Keys.A);
        input.update(player, 1);
        assertEquals(110, player.getX());
        assertEquals(100, player.getY());
        heldKeys.remove(Input.Keys.S);
        input.update(player, 1);
        assertEquals(110 + 10 / Math.sqrt(2), player.getX(), 1e-9);
        assertEquals(100 + 10 / Math.sqrt(2), player.getY(), 1e-9);
    }
}
