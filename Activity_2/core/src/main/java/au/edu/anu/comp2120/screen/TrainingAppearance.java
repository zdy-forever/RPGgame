package au.edu.anu.comp2120.screen;

import au.edu.anu.comp2120.enemy.BossChallenge;
import java.util.Objects;
import java.util.Random;

/**
 * Independent arena and guardian presentation choices, never a hostile training AI.
 * @param arena elemental background identity
 * @param guardian stationary practice target's appearance
 * @author Daoyan Zhu (u7782042)
 */
public record TrainingAppearance(BossChallenge arena, BossChallenge guardian) {
    /**
     * Validates both presentation choices.
     * @param arena background identity
     * @param guardian target appearance
     * @throws NullPointerException if either choice is null
     */
    public TrainingAppearance {
        Objects.requireNonNull(arena, "arena");
        Objects.requireNonNull(guardian, "guardian");
    }

    /**
     * Draws two independent choices; a seeded source makes selection reproducible in tests.
     * @param random non-null random source
     * @return one immutable pair, retained for the whole visit
     * @throws NullPointerException if random is null
     */
    public static TrainingAppearance choose(Random random) {
        Objects.requireNonNull(random, "random");
        BossChallenge[] choices = BossChallenge.values();
        return new TrainingAppearance(choices[random.nextInt(choices.length)], choices[random.nextInt(choices.length)]);
    }
}
