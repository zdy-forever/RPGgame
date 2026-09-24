# Quest portal / gameplay integration report

> Current integration note (2026-09-25): BossQuests now supplies the NPC quest flow, InventoryRewards collects settled rewards, and GameSaves persists progress, replay IDs and pending rewards. The adapter report and its test counts below record the original integration stage.
> See [current controls](../README.md), [inventory/equipment](INVENTORY_EQUIPMENT_REPORT.md)
> and [save/load](SAVE_LOAD_REPORT.md) for the current production behavior.

## Scope

Developer A now exposes task admission, single-Boss scope lifecycle, settled battle results
and a one-time material transfer boundary. No QuestManager, Quest business rules, NPC,
dialogue, UI, Inventory operations, save/load or formal art assets were added.

## External adapter contract

All calls run on the simulation/game-loop thread, not a worker or damage callback.

```text
EncounterFlow flow = gameScreen.getEncounterFlow();

// Developer B calls after an actual acceptance in Hub.
// ID identifies this acceptance, not a reusable quest definition or map.
flow.activate(new ActiveEncounter(
        "acceptance-001", MapCatalog.CROSSROADS, "entry", "hub-crossroads"));

// Read-only observations: repeated reads cause no side effects.
Optional<ActiveEncounter> permission = flow.getActiveEncounter();
Optional<BattleResult> latest = flow.getLastResult();
Map<BattleResult, MaterialDrop> unclaimed = flow.getPendingRewards();

// The external adapter chooses a pending result and is ready to own its payload.
// Only its first claim succeeds. Do not claim speculatively before adapter readiness.
Optional<MaterialDrop> transferred = flow.claimReward(result);

// Optional withdrawal/replacement in Hub; does not erase rewards or mutate a Quest.
flow.clearActiveEncounter();
flow.activate(new ActiveEncounter(
        "acceptance-002", MapCatalog.TRAINING, "entry", "hub-training"));
```

These are API call fragments, not a standalone Java compilation unit; `result` is a key selected from `unclaimed`.
Developer B observes `(encounter.id, encounterVersion, outcome)` to distinguish retries
and owns Quest completion/replacement. FAILURE does not clear gameplay permission, so the
external Quest may remain ACTIVE. SUCCESS clears permission; the next acceptance must have
a fresh encounter ID. Replaying an already successful ID throws even after reward claim.

`EncounterFlow.complete(outcome, reward)` is a **gameplay coordinator endpoint**, not an
external Quest/Inventory command. Only CombatDemo supplies settled Boss/player outcomes.
External adapters must not enable owned portals directly or claim the live Boss drop;
use this boundary's activation and retained reward methods instead.

## Lifecycle and cleanup

1. `TravelCatalog.createQuestPortals()` supplies closed Hub task portals, without manual
   Battle-to-Hub return portals. `EncounterFlow` explicitly owns their enabled states.
2. Activation validates references, Battle type and full-body player spawn before replacing
   a route. It disables old task portals, then enables the selected route. Invalid requests
   leave the old permission and route unchanged. Replacement/withdrawal require a live Hub.
3. Actual portal entry uses TravelSystem -> GameSession. A portal ID plus the exact session
   transition stamp authorizes the encounter. CombatDemo creates one Boss, no ordinary mobs.
4. The coordinator checks settled outcomes before travel/input and after the shared combat
   update. Player death takes precedence on simultaneous player/Boss death: FAILURE, no reward.
5. FAILURE returns through GameSession and retains permission for retry.
6. SUCCESS synchronizes Boss death once, observes the immutable material, returns through
   GameSession and retains material in the outbox. It consumes Boss.claimDrop before clearing
   old registrations, closes task portals and clears active permission.
7. Both paths reuse combat.clear / ability rebind: old Boss/AI registrations, attacks,
   hitboxes, projectiles, attachments, queued derived attacks, dash and launch states are
   cancelled or detached. Only the same persistent Player is registered in Hub, then its
   existing Health.restoreFull is called. Slot cooldowns follow the existing preservation rule.
8. Re-entry after FAILURE constructs a fresh Boss with full HP, phase 1 and initial state,
   not a revived or reused object. The return frame accepts no further movement/abilities/portals.

Invalid Hub configuration throws before result publication, admission closure or Boss reward
claim. Repeated failed return attempts keep the same Boss material, without producing duplicates.

## Rewards and results

- `getLastResult` retains the latest immutable BattleResult, not a Quest state or event bus.
- A separate in-memory outbox retains every unclaimed SUCCESS material across map changes and
  later acceptances. Reading a snapshot does not duplicate a reward or consume it.
- `claimReward(result)` removes one payload. Repeated, unknown or FAILURE claims return empty.
- MaterialDrop contains only existing ID/material/quantity/map/geometry data, no old Boss reference.
- No inventory add, capacity, pickup, reward accounting or persistence is implemented.

## Class, O configuration and fast travel

The production PlayerAbilities instance enforces Hub-only runtime class/loadout changes,
including same-class O configuration changes. Existing class/ultimate objects survive both
return paths. The subsequent final-class-skills implementation adds per-class A/B modes,
Hub-only selection and persistent choices; see FINAL_CLASS_SKILLS_REPORT.md. R is ignored in production Battle scopes and remains a Hub debug reset.

Fast travel remains independent: existing unlocks, validation, map switching and arrival
guards still work. It does not constitute task portal admission, even for the selected map.
Consequently debug U/2/3 visits contain no task Boss. Travelling away clears the old scope
without a success/failure result and retains retry permission; normal task play uses outcomes.

The old `CombatDemo(session)` constructor remains an explicitly unrestricted isolated demo/test
harness. Production GameScreen uses `CombatDemo(session, encounterFlow)`; adapters must not
construct the unrestricted harness for task gameplay.

## Files

New:

- `core/src/main/java/au/edu/anu/comp2120/ActiveEncounter.java`
- `core/src/main/java/au/edu/anu/comp2120/BattleResult.java`
- `core/src/main/java/au/edu/anu/comp2120/EncounterFlow.java`
- `core/src/test/java/au/edu/anu/comp2120/screen/QuestPortalFlowTest.java`
- `docs/QUEST_PORTAL_FLOW_REPORT.md`

Modified:

- `TravelSystem.java`: validated retargeting, portal observation and exact arrival provenance.
- `screen/CombatDemo.java`: optional production boundary, outcome settlement, reward handoff/cleanup.
- `screen/GameScreen.java`: production wiring, adapter getter and pre/post-settlement arbitration.
- `player/PlayerAbilities.java`: Hub-only selection policy for the production controller.
- `world/TravelCatalog.java`: separate initially closed task-only route catalogue.
- `screen/PlayerDefeatTest.java`: explicit null cast to retain the original constructor validation
  test after adding an overload; no test removed or assertion weakened.
- `README.md` and `docs/ARCHITECTURE.md`: current contract and standalone-debug distinction.

## Validation

Final full rebuild:

```sh
./gradlew clean test build javadocAudit
```

Result: **BUILD SUCCESSFUL**, all 14 tasks executed. **740 tests in 40 suites**, zero failures,
errors or skipped tests. The new integration suite adds 21 cases over the prior 719-test baseline.
Strict JavaDoc audit covers private main/test declarations in core and lwjgl3; it passed.
New files use the explicitly supplied author Daoyan Zhu (u7782042).

Coverage includes closed/open/configured portals, correct spawn, exactly one Boss, failure
retry and phase reset, Hub replacement, shared-combat lethal success, one-time reward claim,
multiple pending rewards, no replay of successful acceptance, both-outcome identity/O retention,
Hub-only configuration, fast-travel isolation, no return loop, projectile/dash/old-Boss cleanup,
elemental/derived cleanup, invalid spawn/wiring/settlement and invalid Hub reward preservation.

Development validation also ran `./gradlew test build javadocAudit` and
`./gradlew test javadocAudit`. Initial compile errors in test calls were corrected (ambiguous
null overload and the nullable Projectile accessor); the final clean run above is authoritative.

Startup smoke check: `./gradlew lwjgl3:run` launched the desktop process without a startup
exception. `jcmd <launched-pid> Thread.print` confirmed the live Lwjgl3Application loop after
more than 40 seconds. The smoke process was then intentionally stopped with Ctrl-C.
No manual keyboard combat or visual UI verification is claimed; flow coverage is JUnit-based.

## Remaining limits / separate work

- Fresh production startup intentionally has closed task portals. There is no fake acceptance
  key, NPC or task UI; Developer B must call the adapter boundary to begin normal battles.
- Reward outbox/completed-ID replay protection are in memory only. Unclaimed rewards accumulate
  until an adapter consumes them. Save/load and crash-safe cross-module delivery are separate work.
- External adapters must coordinate ownership/readiness before claim. This is at-most-once
  gameplay transfer, not a durable transaction with Inventory or an acknowledgement/retry protocol.
- Only the latest result is retained; Developer B should observe results during ordinary game-loop
  integration. Reward payloads, unlike the latest result snapshot, are retained individually.
- Custom Battle maps still need a valid existing GameConfig Boss placement candidate. This branch
  validates player entry geometry but does not add a new Boss-spawn configuration system.
- O modes/skills are now implemented by the subsequent final-class-skills work. Final balance,
  inventory adapters, Quest state, UI and persistence remain separate work. No formal art or Git/GitLab operation was used.

## Suggested handoff text

Commit: `feat(gameplay): integrate quest portal admission and battle outcome handoff`

MR title: `Integrate quest-controlled portals, battle returns and one-time Boss rewards`
