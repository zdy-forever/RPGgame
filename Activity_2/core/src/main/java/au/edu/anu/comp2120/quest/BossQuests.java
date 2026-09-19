package au.edu.anu.comp2120.quest;

import au.edu.anu.comp2120.ActiveEncounter;
import au.edu.anu.comp2120.BattleResult;
import au.edu.anu.comp2120.EncounterFlow;
import au.edu.anu.comp2120.GameSession;
import au.edu.anu.comp2120.enemy.BossChallenge;
import au.edu.anu.comp2120.world.MapType;
import au.edu.anu.comp2120.world.MapCatalog;
import au.edu.anu.comp2120.world.TravelCatalog;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Session-local five-Boss quest progress, adapting the existing encounter/result boundary.
 * One quest is active at a time; failure retains it, success marks its distinct target complete.
 * Rewards remain in EncounterFlow's outbox for the inventory owner. No save/load is performed.
 * Call synchronize on the game-loop thread after combat settlement and before accepting a quest.
 * @author Daoyan Zhu (u7782042)
 */
public final class BossQuests {
    /** Replaceable short first-version ending; no artwork or cinematic engine is required. */
    public static final List<String> ENDING_LINES = List.of(
            "The five guardians have fallen. The paths to the Hub are safe once more.",
            "Old Sage: The five stones shine together. Their power has restored the ancient seal.",
            "Your journey is complete. Thank you, adventurer. You may continue exploring and practising.");
    /** Current player/map authority. */
    private final GameSession session;
    /** Existing admission/outcome authority. */
    private final EncounterFlow encounters;
    /** Distinct targets completed in this session. */
    private final EnumSet<BossChallenge> completed = EnumSet.noneOf(BossChallenge.class);
    /** Accepted target, or null when awaiting another quest. */
    private BossChallenge active;
    /** Exact acceptance identity used to reject stale or unrelated results. */
    private ActiveEncounter permission;
    /** Latest result already observed, including failures. */
    private BattleResult observed;
    /** One pending ending trigger after all distinct guardians are defeated. */
    private boolean endingPending;
    /** Prevents repeated ending playback after reopening the NPC or polling results. */
    private boolean endingStarted;

    /**
     * Binds progress to the existing gameplay boundary.
     * @param session persistent player/map session
     * @param encounters boundary belonging to the same session
     * @throws NullPointerException if a dependency is null
     * @throws IllegalArgumentException if the boundary belongs to another session
     */
    public BossQuests(GameSession session, EncounterFlow encounters) {
        this.session = Objects.requireNonNull(session, "session");
        this.encounters = Objects.requireNonNull(encounters, "encounters");
        if (!encounters.belongsTo(session)) {
            throw new IllegalArgumentException("Quests and encounters must share a session");
        }
    }

    /**
     * Accepts/replaces a target in a living Hub, retaining retry identity for the same active quest.
     * Completed targets may be challenged again with a fresh acceptance; first-clear stones remain lit.
     * Acceptance only prepares permission and closes the portal; Portal Guide must explicitly open it.
     * @param target one of the five guardians
     * @throws NullPointerException if target is null
     * @throws IllegalStateException if outside a living Hub
     * @throws IllegalArgumentException if the target portal/map/spawn is invalid
     */
    public void accept(BossChallenge target) {
        Objects.requireNonNull(target, "target");
        requireHub();
        synchronize();
        ActiveEncounter next = active == target && permission != null ? permission
                : new ActiveEncounter("boss-quest-" + UUID.randomUUID(), target.mapId(), "entry",
                        TravelCatalog.QUEST_PORTAL_ID);
        encounters.prepare(next);
        active = target;
        permission = next;
    }

    /** Observes settled matching results once; never claims rewards or switches maps inside a callback. */
    public void synchronize() {
        var latest = encounters.getLastResult();
        if (latest.isEmpty() || latest.get().equals(observed)) {
            return;
        }
        observed = latest.get();
        if (!observed.encounter().equals(permission)
                || observed.outcome() != BattleResult.Outcome.SUCCESS) {
            return;
        }
        completed.add(active);
        active = null;
        permission = null;
        if (completed.size() == BossChallenge.values().length && !endingStarted) {
            endingPending = true;
        }
    }

    /**
     * Reads the task retained across failure and practice visits.
     * @return currently accepted target, empty before acceptance or after success
     */
    public Optional<BossChallenge> getActive() { return Optional.ofNullable(active); }
    /**
     * Reads defeated targets without permitting external mutation.
     * @return immutable distinct completion snapshot
     */
    public Set<BossChallenge> getCompleted() { return Set.copyOf(completed); }

    /**
     * Reads authoritative encounter victories for a guardian, including repeat wins.
     * @param target guardian whose next challenge should be previewed
     * @return successful challenge count; failure and practice do not increase it
     * @throws NullPointerException if target is null
     */
    public int getSuccessCount(BossChallenge target) {
        return encounters.getSuccessCount(Objects.requireNonNull(target, "target").mapId());
    }

    /**
     * Starts the one-time ending when called by the Memorial Hall Sage conversation.
     * Merely completing the fifth quest or entering the hall does not call this method.
     * @return true once when the screen should open the ending dialogue
     */
    public boolean consumeEnding() {
        synchronize();
        if (!endingPending || endingStarted || !session.getCurrentMap().getId().equals(MapCatalog.MEMORIAL_HALL)
                || !session.getPlayer().isAlive()) {
            return false;
        }
        endingPending = false;
        endingStarted = true;
        return true;
    }

    /**
     * Guards NPC quest changes without reimplementing map transition or combat rules.
     * @throws IllegalStateException if not in a living Hub
     */
    private void requireHub() {
        if (session.getCurrentMap().getType() != MapType.HUB || !session.getPlayer().isAlive()) {
            throw new IllegalStateException("Accept quests from the Quest Keeper in Hub");
        }
    }
}
