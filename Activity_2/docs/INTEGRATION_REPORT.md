# Placeholder gameplay integration report

## Current reading guide — 2026-09-30

This report preserves the placeholder milestone, not today's production setup. Production has
imported art/music, quest/NPC portal admission, one Boss per battle, inventory/equipment and saves.
Training is a stationary damage target with an upward test bolt and five-second recovery, not a
normal Boss encounter. Recent work adds a shared potion quickbar, immediate feedback and save/load
onboarding. Use [README](../README.md) and [current test coverage](TEST_COVERAGE_REVIEW.md) for
present behavior and verification status; do not treat the old numbers below as a fresh test run.

> Current integration note (2026-09-25): Inventory reward transfer, equipment and save/load are now implemented; the original limitations and test counts below describe the September 10 placeholder baseline.
> See [current controls](../README.md), [inventory/equipment](INVENTORY_EQUIPMENT_REPORT.md)
> and [save/load](SAVE_LOAD_REPORT.md) for the current production behavior.

Validated on 10 September 2026, macOS, JDK 25 (Java 17 compilation target), Gradle 9.7.1.
Scope: existing gameplay integration only. No Git/GitLab operations, new art, sound,
profession mechanics, damage engine, inventory rewrite or UI redesign were performed.

## Outcome and evidence boundary

The graphics-independent baseline passes **634 tests in 36 suites**, with **0 failures,
0 errors and 0 skipped tests**. A clean full build and strict all-module JavaDoc audit pass.
This branch adds 14 test cases: 10 gameplay integration cases, one input case and three
parameterized class-configuration cases. Existing tests were retained.

The desktop application also starts from an **empty working directory**, using the actual
LWJGL3/OpenGL backend. Runtime inspection found a GameScreen, CombatDemo, PlayerAbilities,
Lwjgl3Window and ShapeRenderer; the inspected live heap contained no Texture, Sprite,
BitmapFont or ParticleEffect objects. Both production modules have no art/file/audio-loader
references matching the audit, and the core JAR contains no image/atlas/font/audio assets.

This is startup and model-level acceptance, **not a claim of completed visual/keyboard QA**.
The available UI automation surface did not expose the Java game window. Native key delivery,
visual camera scrolling and live window resizing remain on the manual checklist below.

## Verified gameplay paths

| Path | Evidence / result |
| --- | --- |
| Hub movement → portal → Training spawn | New integration test moves the real Player into the built-in portal, retains the same Player/Health, verifies arrival guard and new-map collision. |
| Training wall → camera clamp → fast travel | Player stops at x=212 against the x=240 wall with a 28-unit body. Locked travel is rejected; unlocking and travelling to Crossroads uses its (80,80) spawn and collision map. Camera math is checked after each simulated frame. |
| Archer / Mage / Katana → enemy combat → death | Three parameterized scenarios use normal movement/slot commands to defeat the real Training enemies while the player survives. No direct HP writes are used to win these scenarios. Attacks expire and dead enemy positions remain stable. |
| Crossroads → Boss fight → phase 2 → death → one drop | Actual Katana slot commands defeat the enemies and Boss. The Boss reaches phase 2, dies and creates one stable material drop. Continued updates do not duplicate it. |
| Departure after Boss death | Returning to Hub removes the encounter and drop from the active scope. The old Boss can no longer claim its drop. |
| Element selection → attachment → reaction → derived attack | F4 intent tags real Katana attacks. WATER/FIRE bonus damage and WATER/LIGHTNING derived damage affect the Boss through CombatSystem; derived attacks expire. This focused test positions the player near the Boss to isolate reaction timing from navigation. |
| Repeated travel during active projectiles | Thirty transitions cancel old attacks and registrations, preserve Player/Health/class/element/cooldowns, and keep actor membership bounded. Simultaneous class/element/ability requests cannot leak into arrival. |
| Selection, spam, reset and death | New tests cover selection priority, active-attack/dash rejection, cooldown preservation, competing commands, invalid delta/null commands, dead-player rejection and explicit reset recovery. Existing ability/combat tests cover slot spam, collision-safe dash and single-hit ledgers. |

Existing dedicated tests additionally cover eight-direction normalization, movement extremes,
map/spawn validation, portal errors and disabled portals, A* wall navigation, camera small-map
math, health bounds, all class slots, projectile sweep/range/lifetime, enemy cooldown/death,
Boss patterns, the element pair matrix and bounded/deduplicated derived attacks.

## Actual changes and findings

### 1. Element gameplay entry was missing

The element pipeline already worked in tests, but default class attacks carried NONE and
the running placeholder scene had no input to opt into elemental attacks. The requested
playable reaction chain therefore required code editing.

- Added one F4 key-edge request to the existing PlayerInput adapter, not a second input system.
- Added immutable `ClassDefinition.withElement` configuration copying for J/K/O, preserving
  damage, range, geometry, projectile settings, timing and dash tuning.
- Routed it through the existing guarded `PlayerAbilities.selectClass` path. It cannot
  refresh cooldowns, heal, reposition, change a live projectile, bypass death or interrupt a dash.
- Defined priority: travel/reset → class selection → element selection → ordinary commands.
  A rejected selection consumes new commands but does not pause existing simulation.
- Exposed outgoing element in the native title. Existing target attachment labels and
  generic yellow attack rectangles remain the only elemental presentation.
- Class selection preserves the selected light-slot tag; F4 intentionally gives J/K/O
  the same tag. Catalog definitions stay non-elemental and immutable.

### 2. Desktop startup mitigation

The launcher now sets an explicit initial window position (80,80). Inspection of the installed
LibGDX backend showed that this bypasses its auto-centering path through an unchecked primary
monitor handle, which had failed in the previous validation session. The initial launch in
this integration session already succeeded before the change: this is a targeted mitigation,
not a claim that the earlier exception was reproduced or every GLFW failure is fixed.
An available graphical session/OpenGL context is still required.

### 3. Documentation audit gap

The desktop launcher's window constants lacked field JavaDoc. Those descriptions and the new
position field documentation are present. A repository-owned `javadocAudit` task now checks
both modules' production **and test** Java, including private members, with `-Xdoclint:all`
and `-Werror`. It no longer requires an external audit script.

All 90 Java source files contain an author tag. New/changed Java uses the previously supplied
author, **Daoyan Zhu (u7782042)**. New classes/methods/fields/constructors have descriptions
and applicable parameter, return and exception documentation. Doclint cannot prove authorship
or documentation semantics; changed code was also reviewed directly.

### 4. Existing lifecycle behavior confirmed, not rewritten

No failing pre-existing JUnit case or duplicate damage engine was found in this audit.
Do not describe the following as newly fixed bugs: they were already implemented and are
now covered by additional cross-module acceptance tests.

- Player and Enemy delegate to Health; Boss composes the same Enemy/Health foundation.
  CombatSystem is the only attack damage calculation/delivery pipeline.
- GameSession owns map/spawn switching; TravelSystem delegates to it. CombatDemo clears
  and rebinds the map-scoped combat state, retaining the persistent player and cooldowns.
- Expired/hit projectiles and inactive attack hitboxes are removed. Dead actors cannot
  continue moving or attacking. Their bounded debug bodies can remain visible until reset/travel.
- Derived attacks run through the same pipeline, resolve no earlier than the next update,
  cannot generate further reactions, and obey existing capacity and per-target hit guards.
- CameraFollow recalculates from the current player/map every render. Small maps are centered
  with stable background margins rather than being forced through invalid clamp intervals.

## Changed files

Paths are relative to the Activity_2 project root.

| File | Change |
| --- | --- |
| `build.gradle` | Reproducible strict all-module JavaDoc audit task. |
| `core/src/main/java/au/edu/anu/comp2120/player/ClassDefinition.java` | Immutable element-tagged loadout copy. |
| `core/src/main/java/au/edu/anu/comp2120/player/PlayerInput.java` | Separate F4 edge sampling. |
| `core/src/main/java/au/edu/anu/comp2120/screen/CombatDemo.java` | Guarded element selection, priority, class-tag preservation and native-title feedback. |
| `core/src/main/java/au/edu/anu/comp2120/screen/GameScreen.java` | Connects the input request to existing frame coordination. |
| `lwjgl3/src/main/java/au/edu/anu/comp2120/lwjgl3/Lwjgl3Launcher.java` | Explicit placement and complete field/startup JavaDoc. |
| `core/src/test/java/au/edu/anu/comp2120/screen/GameplayIntegrationTest.java` | New cross-module acceptance and regression scenarios. |
| `core/src/test/java/au/edu/anu/comp2120/player/PlayerInputTest.java` | F4 edge/held-key and input independence test. |
| `core/src/test/java/au/edu/anu/comp2120/player/PlayerClassTest.java` | All-class element-copy preservation and null validation. |
| `README.md` | Controls, reaction walkthrough and audit command. |
| `docs/ARCHITECTURE.md` | Input arbitration and integration-test boundaries. |
| `docs/INTEGRATION_REPORT.md` | This report. |

No existing assets were modified, removed or loaded. Temporary validation files were created
only under `/tmp/comp6120-placeholder.CixTIV`, outside the repository.

## Commands and results

Executed from Activity_2:

```bash
./gradlew test build core:javadoc
./gradlew test javadocAudit
./gradlew core:test --tests '*GameplayIntegrationTest'
./gradlew clean test build javadocAudit
./gradlew core:javadoc
./gradlew lwjgl3:run
./gradlew -I /tmp/comp6120-placeholder.CixTIV/empty-runtime.gradle lwjgl3:run
```

- Initial baseline build/tests/doc task: success, mostly up-to-date; not used as evidence
  of a fresh final test execution.
- Targeted integration run: success, followed by further regression additions.
- Final clean build: **BUILD SUCCESSFUL**, 14 tasks, 13 executed. Core tests: **634/634**,
  36 suites, no failure/error/skip. Desktop module currently has no separate JUnit tests.
- `core:javadocAudit` and `lwjgl3:javadocAudit`: passed including private and test declarations.
- The normal `core:javadoc` task was rerun after the clean build and passed as well.
- Original desktop launch and updated empty-directory launch: initialized the actual scene.
  The earlier validation process was stopped before starting the updated one.
  A subsequent thread snapshot showed the updated process in `Lwjgl3Application.loop`,
  rather than blocked during initialization; that validation window was left running.
- The first empty-directory attempt failed in the temporary init script because it addressed
  the Gradle project from the wrong scope. The script was corrected and rerun successfully;
  this was a validation-script error, not a game/build defect.

The temporary init script sets only the `lwjgl3:run` working directory to its empty `runtime/`
subdirectory. No repository asset directory is hidden, renamed or deleted. Inspection commands
included `jcmd -l`, `jcmd 60027 GC.class_histogram`, `jcmd 60027 Thread.print`,
`lsof -a -p 60027 -d cwd`, `jar tf`,
`rg` for forbidden loader/types and Ruby aggregation of JUnit XML and author-tag coverage.
Process IDs and temporary paths are session-specific. Generated test reports are under
`core/build/reports/tests/test/`; strict JavaDoc output is under each module's
`build/docs/javadoc-audit/`.

## Manual acceptance checklist

1. Start with `./gradlew lwjgl3:run`. Use WASD, including diagonal movement, against Hub walls.
2. Walk into the nearby purple portal. Confirm Training arrival is not immediately reversed,
   then walk clear of the return portal and re-enter to go back. Check camera position.
3. Try 2/3 before unlock; then U followed by 2/3. Verify fast travel, spawn and new-map collision.
4. F1/F2/F3 select Archer/Mage/Katana. Aim with current/last WASD direction; tap J/K/L/O.
   Confirm projectile cleanup, melee single-hit behavior, cooldowns and wall-safe dash.
5. In Crossroads, approach and fight the large Boss rectangle. Watch HP/phase in the native
   title, then the single gold drop rectangle. R recreates the debug encounter; it is not
   a production respawn or loot-preserving action.
6. Use the README F4 walkthrough for WATER/FIRE and WATER/LIGHTNING reactions. Wait for live
   player attacks to finish before cycling. Some keyboards require Fn with the function keys.
7. Travel during an arrow/attack/dash, repeat map changes, resize/minimize/restore the window,
   and pause/resume. Verify no old attacks or large resume movement jumps are visible.

## Limitations and separate issue candidates

These are explicit follow-up topics, not reasons to add another large system to this branch:

- **Inventory and persistent drops:** Boss drop data and one-time claim exist, but no automatic
  pickup/inventory transfer/save integration. Unclaimed drops and defeated map populations reset
  on travel; R also discards the old encounter/drop. Agree persistence policy with the inventory owner.
- **Final element design:** only WATER/FIRE (+10) and WATER/LIGHTNING (one 5-damage area child)
  are prototype recipes. ICE/WIND reactions, class assignments, resistance and status effects
  remain undefined. F4 is debug configuration, not a permanent class or equipment selection UI.
- **Melee/area occlusion policy:** fixed attack rectangles, Boss secondary areas and reaction
  areas are not wall-clipped. Projectile walls and movement/dash collision work. Decide the
  intended through-wall/line-of-sight rules before extending those shapes.
- **Navigation and targeting:** A* is body-aware but conservative; no crowd avoidance/body
  blocking or complex Boss navigation is implemented. Projectile sweeps use targets' current
  hurtboxes, not continuous relative-motion trajectories.
- **Game flow and balance:** no production death/respawn/victory loop, checkpoint persistence
  or final difficulty tuning. Test commands establish that encounters are beatable; they do
  not prove balance or enjoyable manual pacing.
- **Presentation acceptance:** native titles can become long; no formal HUD is introduced.
  Complete the OS-keyboard, camera/resize and pause/resume checklist on the user's display.
  Dedicated desktop smoke automation or headless-CI graphics setup belongs in a tooling issue.

## Suggested handoff text

Commit message:

```text
fix(gameplay): integrate and validate the placeholder gameplay baseline
```

MR title:

```text
Integrate and validate the asset-free gameplay baseline
```
