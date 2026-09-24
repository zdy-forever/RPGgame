# Player defeat and Hub recovery

> Current integration note (2026-09-25): Production now uses quest admission and success/failure Hub returns. Bag/equipment survive recovery; successful settlement autosaves, failure does not. The old debug travel instructions and missing-feature statements below describe the September 11 checkout.
> See [current controls](../README.md), [inventory/equipment](INVENTORY_EQUIPMENT_REPORT.md)
> and [save/load](SAVE_LOAD_REPORT.md) for the current production behavior.

Validated 11 September 2026. Scope: Developer A's failure handling and encounter cleanup.
No Git/GitLab operations, art/audio loading, QuestManager, NPC logic or task UI were introduced.

## Flow

1. CombatDemo validates frame input and checks any already-settled battle death before commands.
2. Otherwise PlayerAbilities and the single Boss update, followed by the existing CombatSystem.update.
3. When the settled player HP is zero in a BATTLE map, `returnToHubIfDefeated()` records the
   failed map/session stamp locally and calls the existing GameSession.switchMap(HUB).
4. The existing map synchronization path clears the combat scope and rebinds the player adapter.
5. Health.restoreFull restores the same player's HP, only after successful Hub arrival/cleanup.
6. The completed failure snapshot is published through getLastDefeat. The remainder of the frame
   is consumed, including movement, attacks, selections, debug reset and portal processing.
7. Subsequent live Hub frames accept input normally. Re-entry creates a completely fresh Boss.

GameScreen also checks settled death before travel shortcuts. All damage callbacks finish while
the old battle scope is still active; neither Health nor Damageable switches maps. Failure handling
precedes post-combat Boss reward/phase synchronization. No Quest state is read or changed: an external
owner can keep its task ACTIVE and allow retries or replacement through its own NPC workflow.

## Retained and invalidated state

| Retained | Invalidated / rebuilt |
| --- | --- |
| Same Player instance | Old player Combatant replaced by a Hub adapter |
| Same Health instance, restored to max HP | Boss registration, source cooldown and encounter membership |
| Same PlayerAbilities controller | Dash, launch windows and active-slot attack references |
| Exact ClassDefinition, including complete selected O configuration | Player/Boss hitboxes, projectiles and derived attacks |
| Existing remaining slot cooldown seconds | All elemental attachments and reaction-owned active effects |
| Travel unlock/enabled state | Old Boss reference in the coordinator, including its owned Enemy/AI/path/phase/pattern/drop state |

CombatSystem.clear performs shared pipeline cleanup; PlayerAbilities.bindAfterTransition performs
active ability cleanup. No alternative map manager or damage engine exists. The old Boss is detached,
not revived: stale external references cannot update against the new scope or generate/claim loot.
Once external observers release old objects, normal JVM garbage collection can reclaim them. This
code does not pretend to synchronously destroy Java objects retained by callers.

Each built-in battle map now contains exactly one Boss and no mobs. Ordinary Enemy/EnemyAI code and
its independent tests remain available. The legacy getEnemies diagnostic API returns an immutable
empty list. A safe fallback placement was added for Training so debug reset while standing at the
first Boss spawn does not leave an empty encounter. Fresh Boss objects start at max HP, phase 1,
IDLE, zero cooldown/transition time, first attack pattern and no pending reward.

Repeated failure polling is harmless after recovery because the player is now alive in Hub.
The latest immutable Defeat(mapId, encounterVersion) contains no actor or Quest references and is
not an unbounded event history. Each retry has a different session stamp.

## O-mode compatibility boundary

The inspected checkout has no independent O-mode selector, enum, field or mode-switch input.
This branch therefore does not claim to test a nonexistent mode-selection API or invent one.
It preserves the entire existing selected ultimate configuration and class object exactly.
Parameterized tests use non-default O configurations for all three classes, verify identity
across recovery, then execute O in Hub to confirm the selected attack type/element still apply.
If a separate O-mode branch exists elsewhere, its owner should integrate that state through the
same persistent player/configuration boundary and add a mode-specific regression test.

## Changed files

Paths relative to Activity_2:

- `core/src/main/java/au/edu/anu/comp2120/screen/CombatDemo.java`: post-settlement failure return,
  immutable diagnostic snapshot, shared-pipeline integration constructor and Boss-only scenes.
- `core/src/main/java/au/edu/anu/comp2120/screen/GameScreen.java`: failure-first input arbitration,
  consumed arrival frame, native-title feedback and existing camera refresh.
- `core/src/main/java/au/edu/anu/comp2120/screen/GameConfig.java`: remove obsolete mob spawn tuning
  and provide a safe fallback Boss placement usable in Training.
- `core/src/test/java/au/edu/anu/comp2120/screen/PlayerDefeatTest.java`: 24 new failure cases.
- `core/src/test/java/au/edu/anu/comp2120/screen/CombatDemoTest.java`: update scene assertions for
  single-Boss battle scopes and automatic recovery instead of in-place debug revival.
- `core/src/test/java/au/edu/anu/comp2120/screen/BossDemoTest.java`: Boss presence on both battle maps
  and parameterized safe reset placement.
- `core/src/test/java/au/edu/anu/comp2120/screen/GameplayIntegrationTest.java`: replace mob scenarios
  with Boss scenarios; an automatic failure recovery explicitly cannot count as a combat win.
- `core/src/test/java/au/edu/anu/comp2120/player/ElementClassIntegrationTest.java`: target the actual
  Boss, keeping the player clear of its reset spawn while checking elemental status cleanup.
- `README.md`, `docs/ARCHITECTURE.md`, `docs/PLAYER_DEFEAT_REPORT.md`: current behavior and handoff.

Existing Player, Health, GameSession, CombatSystem, Boss and TravelSystem implementations were
reused without modification. No ordinary AI unit test or combat rule was removed to obtain green tests.

## Verification

Executed from Activity_2:

```bash
./gradlew test
./gradlew core:test
./gradlew core:test --tests '*PlayerDefeatTest' --tests '*GameplayIntegrationTest' --tests '*ElementClassIntegrationTest'
./gradlew clean test build javadocAudit
./gradlew test build javadocAudit
./gradlew lwjgl3:run
```

- Final full suite: **659 tests, 37 suites, 0 failures, 0 errors, 0 skipped**.
- 24 new PlayerDefeatTest cases plus one extra parameterized reset-placement case;
  existing scene tests were updated for the newly confirmed rules.
- Clean core/desktop compilation and packaging: **BUILD SUCCESSFUL**.
- Both strict JavaDoc audits: **passed**, including main/test/private members with full doclint
  and warnings as errors. All 91 Java files have author tags. New/modified Java documentation
  uses the previously supplied author **Daoyan Zhu (u7782042)**; no author was invented.
- New tests cover lethal Boss damage, settlement callback ordering, dead WASD/J/K/L/O rejection,
  no reset/selection bypass, active dash/attack/projectile/element/derived cleanup, Hub full-health
  recovery, identity/class/O configuration preservation, fresh Boss HP/phase/AI/pattern/cooldown,
  idempotent return, travel arrival protection/retry, both battle maps and malformed Hub data.
- Three-class combat scenarios start within attack range with full-health Bosses. Their purpose
  is attack-delivery acceptance, not proof that a naive ranged character can tank the Boss or that
  final encounter balance is complete. Actual input/health settlement, not direct HP writes, wins them.
- Desktop run reaches the actual game scene and game loop using geometry placeholders. Runtime
  inspection and source search are distinct from manual gameplay acceptance; native keyboard and
  visual camera behavior were not claimed as manually tested.

## Manual check

1. Start with `./gradlew lwjgl3:run`; select F1/F2/F3 (Fn may be required).
2. Press U then 3, approach the large Boss and let it attack without pressing R.
3. At zero HP, expect Hub entry (64,64), full HP, the same profession/element configuration,
   no old yellow attacks and no lingering dash movement. The title reports FAILURE.
4. Use the corresponding portal again or debug fast travel. Verify a new 400-HP, phase-1 Boss.
5. Repeat with different classes, active arrows/spells and Dash; check that cooldown seconds
   are not refunded by failure. Try the same flow in Training, navigating around its walls.

## Limitations / separate work

- Quest/NPC state, quest-gated portal access, victory return and rewards are intentionally absent.
  Existing debug portals/fast-travel/R remain for development; no Quest is failed or deleted.
- Independent O-mode selection is absent in this checkout, as detailed above.
- Player slot cooldowns follow the existing travel policy and are preserved, not reset to READY.
- Invalid/missing Hub, wrong Hub type or a spawn too small for the player throws explicitly and
  never heals in Battle. Built-in maps are valid; callers configuring custom maps must supply
  a valid Hub and safe Boss placements. Custom maps with no valid Boss candidate are not silently
  populated with an illegal body.
- Failure drops and transient encounter state are discarded, not saved. Inventory integration,
  final balance, wall-clipped area attacks and persistence remain separate concerns.
- Hub death is not a challenge FAILURE. Low-level geometry methods remain permission-free;
  gameplay input must continue through PlayerAbilities and the scene coordinator.

Suggested commit:

```text
feat(gameplay): return defeated players to Hub and reset boss encounters
```

Suggested MR title:

```text
Add player defeat recovery and fresh Boss encounter retries
```
