# Quests, portals, rewards and ending

Reviewed against `7fea86a` on 2026-10-02. Sources:
[EncounterFlow](../core/src/main/java/au/edu/anu/comp2120/EncounterFlow.java),
[BossQuests](../core/src/main/java/au/edu/anu/comp2120/quest/BossQuests.java),
[PortalGuide](../core/src/main/java/au/edu/anu/comp2120/npc/PortalGuide.java),
[TravelCatalog](../core/src/main/java/au/edu/anu/comp2120/world/TravelCatalog.java).

## Player route

1. In Hub, speak to Quest Keeper and choose one of five guardians. An unfinished task may be
   replaced; an already completed guardian may be accepted again as a rematch.
2. Acceptance prepares the exact battle permission and closes the Hub portal. Speak to Portal
   Guide and select Battle to activate the accepted route. The menu names the target or explains
   that Battle is locked until acceptance.
3. Close dialogue and walk into the portal at (890, 245). Selection alone does not move/heal you.
4. Victory returns you to Hub with full HP and rewards/progress; failure returns/heals with the
   task retained for retry. Accept a fresh quest after success before another battle.

Portal Guide also selects Training Ground or Memorial Hall without a battle task. Either visit
retains an accepted quest/retry identity; selecting Battle later restores that task's route.
Training returns through its exit NPC; Memorial Hall uses the return portal. Arrival protection
prevents immediate re-entry until the player leaves the trigger.

There is one configurable Hub portal, plus the Hall return portal. Internal fast-travel APIs
serve adapters and tests; there are no U/1/2/3 gameplay travel shortcuts. In Manage saves, 1/2
select save slots and have no travel meaning.

## Admission and one-time settlement

GameSession owns the current map/Player. TravelSystem owns route/enabled/unlock state and
transition provenance. EncounterFlow owns exact ActiveEncounter acceptance IDs, admitted Battle
lifetime, last result, completed IDs, victory counts and pending rewards. BossQuests owns the
active target and distinct completion state, observing matching successful results once.

`prepare` validates a living Hub context and stores permission while leaving admission closed.
`activate` enables the exact accepted route. An unauthorized direct map switch must not create
a production Boss fight. CombatDemo consumes admitted travel provenance and rebuilds one combat
scope. Training creates a TrainingDummy without a Boss reward or campaign result.

After combat settlement, Player death is checked before Boss death. Success copies the one-time
material reward into EncounterFlow before clearing the old Boss. It closes admission, records
completion/win count and returns/heals. Failure grants no material, keeps the task permission and
returns/heals for a fresh retry. A completed acceptance cannot be replayed to create extra loot.

InventoryRewards collects an outbox reward only when it fits. Overflow retains it for a later
attempt and for persistence. GameScreen then reconciles equipment, synchronizes quests and
requests victory autosave. Neither talking to Quest Keeper nor picking up a rendered object is
needed to collect an ordinary successful reward.

## Hall, objectives and credits

Each distinct guardian success lights its matching stone in Memorial Hall. Repeated wins
increase rematch scaling but do not add another stone. Defeat/Training does not light a stone.
The Hall is available before completion and is a safe save location.

Hub guidance progresses through Old Sage, Class Master, Quest Keeper and Portal Guide, with
current objective text below HP. After all five wins, Hub Old Sage supplies the Hall direction
and records that hint; this guides the player rather than unlocking the Hall route itself.
In Memorial Hall, minimap and objective panel are hidden, and the local Sage receives the
relevant marker until the ending has started.

The ending starts only on an explicit living-player interaction with the Hall Sage after all
five distinct completions. It is a short implemented dialogue; it is not a missing future script.
The consumption flag is set when it starts, so cancelling it does not create another first-time
trigger. Completing the dialogue offers credits or closing the conversation. Later completed
Hall conversations can offer credits again; credits do not exit the game or change rewards.
The player may continue exploring/practising/rematching.

Save/load restores active permission, completed IDs, pending rewards, distinct wins, rematch
counts, ending consumption, Hall hint and NPC flags. This is saved quest/ending progress, not a
separate achievement subsystem. See [save/load](SAVE_LOAD_REPORT.md).

## Verification

From Activity_2:

```sh
./gradlew core:test --tests '*QuestPortalFlowTest' --tests '*BossQuestFlowTest' \
  --tests '*PortalGuideTrainingTest' --tests '*CoreGameplayIntegrationTest' \
  --tests '*MemorialHallTest' --tests '*CreditsDialogueTest' \
  --tests '*GameScreenObjectiveTest' --tests '*GameSavesTest'
```

Scenarios cover locked/accepted admission, retries/replacements, practice preservation,
movement-triggered arrival, outcome/reward deduplication, distinct stones, ending consumption,
credits and persisted state. [Verified CI](TEST_COVERAGE_REVIEW.md#recorded-result) is recorded
separately from native acceptance. [Native checks on 2026-10-03](PLAYTEST_REPORT.md) exercised
Quest Keeper acceptance, Guide selection, actual portal entry, five distinct wins/rewards,
Wind defeat and retry, relaunch/Continue, Training, Hub Sage endgame guidance, all five Hall
stones, three ending lines and all five Credits pages. Wind was completed by the user without
debug features; NPC/portal approaches used disclosed position-only fixtures. Ending state was
saved and reloaded in Hall. Normal walking routes, rematches and exhaustive objective/marker
and ending cancellation/replay cases remain outside this session.
