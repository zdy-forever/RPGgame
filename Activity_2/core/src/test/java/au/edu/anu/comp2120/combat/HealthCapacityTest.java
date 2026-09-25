package au.edu.anu.comp2120.combat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Checks ratio-based equipment HP scaling without rounding exploits.
 * @author Hao Liu (u7998420)
 */
final class HealthCapacityTest {
    /** Creates a fixture. */
    HealthCapacityTest() { }

    /** Full, partial and critical HP survive repeated capacity swaps without gains or erosion. */
    @Test void allHealthValuesRoundTripWithoutFarmingOrLoss() {
        for (int hp = 1; hp <= 140; hp++) {
            var health = new Health(140); health.restoreSavedHp(hp);
            long damageVersion = health.getDamageVersion();
            for (int i = 0; i < 100; i++) {
                health.setMaxHp(100);
                assertEquals(Math.max(1, hp * 100 / 140), health.getCurrentHp());
                health.setMaxHp(120); health.setMaxHp(130); health.setMaxHp(140);
                assertEquals(hp, health.getCurrentHp());
            }
            assertEquals(damageVersion, health.getDamageVersion());
        }
    }

    /** Actual damage and healing establish a new ratio, while death and large capacities are safe. */
    @Test void healthMutationsResetScalingReference() {
        var health = new Health(100);
        health.setMaxHp(140); assertEquals(140, health.getCurrentHp());
        health.takeDamage(70); health.setMaxHp(100); assertEquals(50, health.getCurrentHp());
        health.heal(10); health.setMaxHp(140); assertEquals(84, health.getCurrentHp());
        health.takeDamage(83); health.setMaxHp(100); assertEquals(1, health.getCurrentHp());
        health.setMaxHp(140); assertEquals(1, health.getCurrentHp());
        health.takeDamage(1); health.setMaxHp(100); health.setMaxHp(140);
        assertEquals(0, health.getCurrentHp());
        health.restoreFull(); assertEquals(140, health.getCurrentHp());
        health.setMaxHp(Integer.MAX_VALUE); assertEquals(Integer.MAX_VALUE, health.getCurrentHp());
        assertThrows(IllegalArgumentException.class, () -> health.setMaxHp(0));
        assertEquals(Integer.MAX_VALUE, health.getMaxHp());
    }
}
