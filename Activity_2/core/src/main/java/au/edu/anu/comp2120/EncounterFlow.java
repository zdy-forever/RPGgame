package au.edu.anu.comp2120;

import au.edu.anu.comp2120.inventory.MaterialDrop;
import au.edu.anu.comp2120.world.MapCatalog;
import au.edu.anu.comp2120.world.MapType;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Game-loop-thread boundary between external challenge acceptance and gameplay outcomes.
 * Owns admission and an in-memory reward outbox, not Quest progress or Inventory storage.
 * Combat coordination calls complete only after normal settlement, never from damage callbacks.
 * @author Hao Liu (u7998420)
 * @author Daoyan Zhu (u7782042)
 */
public final class EncounterFlow {
    /** Existing transition executor and persistent player. */
    private final GameSession session;
    /** Existing portal routing and arrival protection. */
    private final TravelSystem travel;
    /** Explicitly owned task portals; unrelated travel is untouched. */
    private final Set<String> portalIds;
    /** Completed acceptance IDs reject replay; another acceptance must use a fresh ID. */
    private final Set<String> completedIds = new HashSet<>();
    /** Unclaimed immutable rewards survive map changes and subsequent acceptances. */
    private final Map<BattleResult, MaterialDrop> pendingRewards = new LinkedHashMap<>();
    /** Current permission, retained after failure and cleared after success. */
    private ActiveEncounter active;
    /** Latest settled result, observed without consuming rewards. */
    private BattleResult lastResult;
    /** Session-local successful encounter counts by map; independent of distinct quest/stone completion. */
    private final Map<String, Integer> successCounts = new LinkedHashMap<>();

    /**
     * Claims ownership of task portals and closes them until externally configured in Hub.
     * All definitions are checked before changing any portal.
     * @param session existing session, initially in Hub
     * @param travel travel coordinator for the same session
     * @param portalIds task portal IDs whose enabled states this boundary exclusively owns
     * @throws NullPointerException if required data is null
     * @throws IllegalArgumentException if sessions differ or a portal is missing/not Hub-owned
     * @throws IllegalStateException if initialization occurs outside a live Hub
     */
    public EncounterFlow(GameSession session, TravelSystem travel, Collection<String> portalIds) {
        this.session = Objects.requireNonNull(session, "session");
        this.travel = Objects.requireNonNull(travel, "travel");
        this.portalIds = Set.copyOf(portalIds);
        if (!travel.belongsTo(session)) {
            throw new IllegalArgumentException("Encounter and travel must share a session");
        }
        requireHub();
        for (String id : this.portalIds) {
            if (session.getMap(travel.getPortal(id).sourceMapId()).getType() != MapType.HUB) {
                throw new IllegalArgumentException("Task portal must belong to Hub: " + id);
            }
        }
        closePortals();
    }

    /**
     * Configures/replaces permission and opens its portal, only in a living Hub.
     * Portal Guide calls this after confirmation; quest acceptance alone must use prepare instead.
     * Invalid requests leave the previous permission and routes untouched. A completed ID cannot
     * reopen; accepting the same quest again requires a new acceptance ID, not a new map ID.
     * @param encounter externally accepted challenge
     * @throws NullPointerException if encounter is null
     * @throws IllegalArgumentException if references, map type, portal ownership or spawn fit are invalid
     * @throws IllegalStateException if outside Hub/dead or the acceptance already succeeded
     */
    public void activate(ActiveEncounter encounter) {
        prepare(encounter);
        travel.setPortalEnabled(encounter.portalId(), true);
    }

    /**
     * Validates and records an accepted encounter while keeping all owned task portals closed.
     * Replaces any previous route, including an open practice route, without opening the new one.
     * Invalid requests leave the old permission and portal states untouched. Re-preparing the same
     * acceptance preserves its identity but requires another explicit Portal Guide confirmation.
     * @param encounter externally accepted challenge awaiting portal confirmation
     * @throws NullPointerException if encounter is null
     * @throws IllegalArgumentException if references, map type, ownership or spawn fit are invalid
     * @throws IllegalStateException if outside a living Hub or the acceptance already succeeded
     */
    public void prepare(ActiveEncounter encounter) {
        Objects.requireNonNull(encounter, "encounter");
        requireHub();
        if (completedIds.contains(encounter.id())) {
            throw new IllegalStateException("Encounter already completed: " + encounter.id());
        }
        if (!portalIds.contains(encounter.portalId())
                || session.getMap(encounter.mapId()).getType() != MapType.BATTLE) {
            throw new IllegalArgumentException("Encounter requires an owned portal and Battle map");
        }
        travel.configurePortalDestination(encounter.portalId(), encounter.mapId(), encounter.spawnId());
        closePortals();
        active = encounter;
    }

    /**
     * Withdraws entry permission in Hub without changing external Quest state or pending rewards.
     * @throws IllegalStateException if outside a live Hub
     */
    public void clearActiveEncounter() {
        requireHub();
        closePortals();
        active = null;
    }

    /**
     * Exposes accepted permission independently from mutable Quest state and portal enabled state.
     * A prepared encounter is visible here even before the Portal Guide opens its route.
     * @return current accepted permission, or empty when a new acceptance is required
     */
    public Optional<ActiveEncounter> getActiveEncounter() {
        return Optional.ofNullable(active);
    }

    /**
     * Checks exact task portal provenance, not merely a matching Battle map ID.
     * Fast travel and low-level session transitions therefore cannot start a task Boss.
     * @return true in the current admitted battle scope
     */
    public boolean isBattleActive() {
        return active != null && active.mapId().equals(session.getCurrentMap().getId())
                && travel.arrivedThrough(active.portalId());
    }

    /**
     * Checks integration wiring without exposing a mutable session reference.
     * @param candidate session used by combat coordination
     * @return true for this boundary's session
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted") // Keep the positive public ownership query; guards reject mismatches.
    public boolean belongsTo(GameSession candidate) {
        return session == candidate;
    }

    /**
     * Stages a success payload before GameSession switches maps, then publishes the completed result.
     * The combat coordinator supplies already-settled data, then clears actors and restores HP.
     * Failure retains admission; success closes it and stores one reward until claimed.
     * Invalid Hub transitions remove the staged payload, leaving admission, result and Boss reward
     * ownership unchanged. Calls are single-threaded, with no adapter callbacks during transition.
     * @param outcome settled outcome, with failure taking priority on simultaneous player/Boss death
     * @param reward success payload observed from Boss, null for failure
     * @return immutable result, also retained for polling
     * @throws NullPointerException if outcome is null
     * @throws IllegalStateException if no admitted battle is running
     * @throws IllegalArgumentException if outcome/payload disagree or Hub/spawn is invalid
     */
    public BattleResult complete(BattleResult.Outcome outcome, MaterialDrop reward) {
        Objects.requireNonNull(outcome, "outcome");
        if (!isBattleActive()) {
            throw new IllegalStateException("No admitted battle to complete");
        }
        boolean validReward = outcome == BattleResult.Outcome.SUCCESS
                ? reward != null && reward.mapId().equals(active.mapId())
                : reward == null;
        if (!validReward) {
            throw new IllegalArgumentException("Success requires its Boss material; failure has no reward");
        }
        if (session.getMap(MapCatalog.HUB).getType() != MapType.HUB) {
            throw new IllegalArgumentException("Battle return requires a Hub map");
        }
        BattleResult result = new BattleResult(active, session.getTransitionVersion(), outcome);
        if (outcome == BattleResult.Outcome.SUCCESS) {
            pendingRewards.put(result, reward);
        }
        try {
            session.switchMap(MapCatalog.HUB);
        } catch (IllegalArgumentException invalidReturn) {
            pendingRewards.remove(result);
            throw invalidReturn;
        }
        if (outcome == BattleResult.Outcome.SUCCESS) {
            completedIds.add(active.id());
            successCounts.merge(active.mapId(), 1, (count, increment) -> count == Integer.MAX_VALUE ? count : count + increment);
            closePortals();
            active = null;
        }
        lastResult = result;
        return result;
    }

    /**
     * Observes the most recent result repeatedly without executing return or reward transfer again.
     * @return latest result, or empty before a task battle ends
     */
    public Optional<BattleResult> getLastResult() {
        return Optional.ofNullable(lastResult);
    }

    /**
     * Reads successful returns for a Boss arena, without counting failed attempts or result polls.
     * Counts advance only after a successful Hub transition, once per fresh acceptance.
     * One guardian owns each campaign arena; GameSaves captures and restores these counts for rematch scaling.
     * @param mapId stable Boss arena ID
     * @return non-negative success count, saturated at Integer.MAX_VALUE
     * @throws NullPointerException if mapId is null
     */
    public int getSuccessCount(String mapId) {
        return successCounts.getOrDefault(Objects.requireNonNull(mapId, "map ID"), 0);
    }

    /**
     * Exposes unclaimed success payloads across map changes; snapshots do not grant another claim.
     * @return immutable result-to-material snapshot
     */
    public Map<BattleResult, MaterialDrop> getPendingRewards() {
        return Map.copyOf(pendingRewards);
    }

    /**
     * Transfers one retained reward to an external adapter on the game-loop thread, at most once.
     * The adapter must be ready to own the payload before calling; no Inventory or persistence runs here.
     * @param result exact success snapshot identifying the reward
     * @return transferred material, or empty for unknown/failure/already-claimed results
     * @throws NullPointerException if result is null
     */
    public Optional<MaterialDrop> claimReward(BattleResult result) {
        return Optional.ofNullable(pendingRewards.remove(Objects.requireNonNull(result, "result")));
    }

    /** Closes only owned task routes, preserving ordinary fast-travel unlocks. */
    private void closePortals() {
        portalIds.forEach(id -> travel.setPortalEnabled(id, false));
    }

    /**
     * Prevents mid-battle permission replacement and dead-player acceptance.
     * @throws IllegalStateException if not in a live Hub
     */
    private void requireHub() {
        if (session.getCurrentMap().getType() != MapType.HUB || !session.getPlayer().isAlive()) {
            throw new IllegalStateException("Encounter selection requires a living player in Hub");
        }
    }

    /** Replaces persistent encounter data after complete save validation.
     * Last-result polling is cleared so loading cannot replay a victory or trigger autosave.
     * @param permission saved active permission, or null
     * @param counts validated win counts by arena
     * @param completed validated completed acceptance IDs
     * @param rewards validated unclaimed rewards
     */
    public void restoreProgress(ActiveEncounter permission, Map<String, Integer> counts,
            Set<String> completed, Map<BattleResult, MaterialDrop> rewards) {
        active = permission;
        successCounts.clear(); successCounts.putAll(counts);
        pendingRewards.clear(); pendingRewards.putAll(rewards);
        completedIds.clear(); completedIds.addAll(completed);
        lastResult = null;
    }

    /** Reads completed acceptance identities to preserve replay protection across loading.
     * @return immutable completed acceptance IDs
     */
    public Set<String> getCompletedEncounterIds() { return Set.copyOf(completedIds); }

}
