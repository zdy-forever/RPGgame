package au.edu.anu.comp2120;

import java.util.Objects;

/**
 * Immutable gameplay outcome; never completes, fails or replaces an external Quest.
 * Polling this snapshot does not claim its separately retained material reward.
 * @param encounter accepted challenge that produced the result
 * @param encounterVersion session transition stamp distinguishing retries
 * @param outcome settled success or failure
 * @author Daoyan Zhu (u7782042)
 */
public record BattleResult(ActiveEncounter encounter, long encounterVersion, Outcome outcome) {
    /**
     * Creates a validated result with no entity references.
     * @param encounter challenge permission snapshot
     * @param encounterVersion battle scope stamp
     * @param outcome settled outcome
     * @throws NullPointerException if encounter or outcome is null
     */
    public BattleResult {
        Objects.requireNonNull(encounter, "encounter");
        Objects.requireNonNull(outcome, "outcome");
    }

    /**
     * Gameplay outcomes independent of Quest state.
     * @author Daoyan Zhu (u7782042)
     */
    public enum Outcome {
        /** The player survived and defeated the encounter Boss. */
        SUCCESS,
        /** The player's health reached zero; the same permission remains retryable. */
        FAILURE
    }
}
