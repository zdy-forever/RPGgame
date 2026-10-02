# Game Tester and Verification

[Home](home) · [Installation and Setup](installation-and-setup)

## What the automatic tester is

The automatic game tester is the Gradle-run JUnit suite, including scripted integration scenarios that exercise the production gameplay models and coordinators. It sends `PlayerCommand` inputs, advances bounded simulation steps and asserts the resulting state or event. This follows the assignment examples of command-to-state testing, action-to-event testing and rejection of illegal operations. The assignment description supplied for this project does not require a separate interactive executable.

The suite runs without opening the game window. It does not automate desktop pixels or mouse clicks. Training and the debug console are additional manual testing aids; the tutor can separately play the graphical game using the User Manual.

## Run the automatic tester

Install the JDK and clone the project as described in [Installation and Setup](installation-and-setup), then run from `Activity_2`:

```sh
# macOS / Linux: all core and desktop JUnit tests
./gradlew test --rerun-tasks --no-daemon
```

```powershell
# Windows PowerShell
./gradlew.bat test --rerun-tasks --no-daemon
```

`--rerun-tasks` requests a fresh execution instead of accepting an up-to-date test task. The first run needs network access to download Gradle and dependencies. No graphical window is required for these tests.

## Run all checks

From `Activity_2`, with JDK 25:

```bash
./gradlew clean build javadocAudit --no-daemon
```

Use `./gradlew.bat` on Windows. `test` alone runs JUnit; the full command also compiles/builds the modules and performs the strict JavaDoc audit. Reports are in each module's `build/reports/tests/test/index.html` and `build/test-results/test/`.

## Run selected gameplay scenarios

```bash
./gradlew core:test --tests '*CoreGameplayIntegrationTest' --rerun-tasks
./gradlew core:test --tests '*PlayerTest' --tests '*CollisionSweepTest' --tests '*CombatOcclusionTest' --rerun-tasks
./gradlew core:test --tests '*GearUpgradesTest' --rerun-tasks
./gradlew core:test --tests '*GameSavesTest'
./gradlew core:test --tests '*EquipmentIntegrationTest'
./gradlew core:test --tests '*PortalGuideTrainingTest'
./gradlew core:test --tests '*DebugConsoleTest'
```

| Test group | Expected behavior checked |
| --- | --- |
| `CoreGameplayIntegrationTest` | Portal admission through movement; actual combat victory/reward settlement; defeat, return and retry; map-local ability cleanup |
| World collision and combat occlusion tests | Movement respects geometry; blocked attacks do not damage through walls |
| `BossTraitTest` and Boss tests | Guardian-specific mechanics, phases and encounter rules |
| `EquipmentIntegrationTest`, `GearUpgradesTest` | Ownership, stat effects, material consumption and +2 cap |
| `GameSavesTest`, `SaveStoreTest` | Snapshot restoration, invalid files, slot separation, debug save protection and Sydney display |
| `PortalGuideTrainingTest`, `CombatDemoTest` | Practice shots, mitigation, recovery, minimum health, paused simulation and stable reset positions |
| `DebugOptionsTest`, `DebugConsoleTest` | Toggle commands, input editing, state colours and input-processor restoration |

A passing test means its assertions hold for the scenario tested. For example, the debug-victory save-file test settles the Boss programmatically; it does not establish a full keyboard-driven one-shot playthrough.


## Assignment examples mapped to real tests

Source references below are fixed at `1a2c60c`. The current CI result for that commit is recorded below.

| Requirement | Input/actions performed | Expected state/event | Test evidence |
| --- | --- | --- | --- |
| Send commands and reach a state | Configure the Hub route and repeatedly issue rightward `PlayerCommand` steps into the portal | Arrive at the selected map/spawn with the correct collision area and exactly the appropriate Boss or Training target | `CoreGameplayIntegrationTest.admissionByMovementUsesCurrentMapCollisionAndOneTarget` |
| Actions produce an expected event | Run a bounded fight using movement/basic/special/ultimate commands for the three classes and A/B selections | SUCCESS, return to Hub with full health, exactly one claimable reward, closed completed encounter | `CoreGameplayIntegrationTest.actualCombatCompletesOneRewardAndClosesPermission` |
| Defeat and retry | Advance simulation under real Boss attacks, then re-enter the encounter | FAILURE without victory rewards; full-health Hub return; fresh phase-one Boss on retry | `CoreGameplayIntegrationTest.actualBossCombatFailureReturnsAndRetries` |
| Cannot walk through walls or leave the map | Apply diagonal movement beside a wall and movement toward map boundaries | Slide along the wall or stop at the boundary | `PlayerTest.diagonalMovementSlidesAlongWall`, `stopsAtMapBoundary`; `CollisionSweepTest.mapCombinesWallsAndWorldBounds` |
| Cannot attack through walls | Execute class attacks with blocked/unblocked lines of sight | The wall blocks damage; the unblocked control can hit | `CombatOcclusionTest.classAttacksCannotHitBossThroughWall` |
| Illegal actions do not change valid state | Request invalid movement/time or disallowed equipment upgrades | Reject invalid input without state corruption; rejected upgrades spend no materials; upgrades stop at +2 | `PlayerTest.rejectsInvalidTimeWithoutChangingState`, `rejectsInvalidDirection`; `GearUpgradesTest.rejectedUpgradesNeverSpendMaterials`, `allSlotsUseSharedCoresAndStopAtTwo` |

An actual portal scenario advances the production model with:

```java
frame(0.1, new PlayerCommand(1, 0, false, false, false, false));
```

The test then checks the selected map, named spawn and encounter state. These commands use the real model API; they are not simulated keyboard events sent to the desktop window. Combat victory scenarios use real ability damage, while some save/failure fixtures deliberately arrange state directly. Each test must be interpreted within its own setup.

## How to tell whether it behaved as expected

- A fresh successful run ends with `BUILD SUCCESSFUL` and a zero exit status. Open both HTML reports and check failures, errors and skipped tests. A skipped test is not a demonstrated pass.
- A failing assertion makes the Gradle test task fail. The report identifies the test, expected/actual result and stack trace; inspect that failure before claiming the behavior works.
- `UP-TO-DATE` or `FROM-CACHE` is not a fresh execution. Use the command above when collecting submission evidence.
- Core report: `core/build/reports/tests/test/index.html`; desktop report: `lwjgl3/build/reports/tests/test/index.html`; machine-readable results: each module's `build/test-results/test/`.
- Record the source commit, date, OS/JDK, exact command and result together. Attach a report/pipeline screenshot for that run; a screenshot of Training alone does not prove automated tests passed.

The current CI result below establishes that the suite passed for the identified commit. Native input, audio, rendering and a complete human playthrough remain separate checks.

## Recorded results and coverage

### Current CI evidence — 3 October 2026

| Item | Observed result |
| --- | --- |
| Source commit | `7fea86a83d8eae720650bed84511ad3c989f7c1b` |
| Pipeline | [#1052111](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/pipelines/1052111), passed |
| Build job | [gradle-build #7716533](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/jobs/7716533), passed |
| Environment | GitLab Docker executor, `eclipse-temurin:25-jdk`, Gradle 9.7.1 |
| Executed command | `./gradlew clean build javadocAudit --no-daemon` from `Activity_2` |
| JUnit report | **1,302 passed; 0 failures; 0 errors; 0 skipped** |
| Build/JavaDoc | Both modules compiled and tested; `core:javadocAudit` and `lwjgl3:javadocAudit` completed; log ends `BUILD SUCCESSFUL` |

**Observed outcome:** the automatic tests met their assertions for this commit. GitLab reports the same 1,302-test result for the latest pipeline. The screenshot below was captured from pipeline #1052082 on 2 October 2026 and shows that report format and count; the links above are the authoritative evidence for the later `7fea86a` run. The displayed 100% is the test success rate, not source-code line/branch coverage. This CI result does not establish manual desktop, audio or complete campaign acceptance.

![GitLab pipeline 1052082: 1302 tests passed, zero failures, errors or skipped tests](uploads/55b3cca95fa485eda6abad6b996ff2a2/clipboard.jpeg){width=900}

A post-merge local run of `./gradlew clean build javadocAudit --no-daemon` also completed successfully on 3 October 2026. The linked main pipeline remains the reproducible submission evidence; later source commits must be checked against their own pipeline.

### Historical result

The documented full validation on 28 September 2026 passed 1,237 core tests and four desktop tests, with zero failures/errors, plus JavaDoc audits and desktop compilation. This is dated evidence, not a claim that every later revision has been rerun. Consult the pipeline for the exact submitted commit.

No measured line/branch coverage percentage is available in the inspected build configuration. Test counts and feature-to-test mappings must not be presented as a coverage percentage. A measured coverage report remains a verification improvement.

## CI and JavaDoc

The repository's [CI configuration](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/main/.gitlab-ci.yml) defines `gradle-build` using `eclipse-temurin:25-jdk` and the teaching runner tag `comp2120`. It runs `clean build javadocAudit --no-daemon` and uploads JUnit XML even when the job fails. The job has no branch filter excluding main; verify the resulting main pipeline at submission.

[View pipelines](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/pipelines). Open the relevant commit's job for its log and test report. Security jobs are separate from gameplay tests.

`javadocAudit` includes main/test sources and private members, enables full doclint and treats warnings as errors. Generated HTML is in each module's `build/docs/javadoc-audit/`. Documentation review also checks descriptions, attribution and appropriate parameter/return tags; successful generation alone is not evidence that every explanation is accurate.

## Manual acceptance

| Check | Procedure and expected result |
| --- | --- |
| Input and pause | Open bag/dialogue during Training shots; simulation stops. Navigate with keyboard and mouse; closing does not trigger movement/attacks. |
| Save round trip | Save in Hub, change position/items, load and confirm restoration. Restart the app and load again. |
| Slot safety | Cancel overwrite/delete; files remain. Confirm deleting one slot; the other and live state remain. Use disposable saves. |
| UI scaling | Resize small/default/large; inspect centred fading notifications, map labels, console text and clickable controls. |
| Training | Compare boots/coat effects; take damage, step away and recover; repeatedly press R beside/overlapping the target without relocation. |
| Campaign | Accept quest, choose Battle, win/lose, verify return and progress; five distinct wins unlock the Hall ending. |

These manual cases are a checklist, not a record that all have passed on every platform.

## Debug console

F10 or backtick opens the console. Enter runs a command; Esc closes. Bare `god`, `oneshot`, `freeupgrade` and `nocd` toggle their state; entering one again disables it. Green ON and white OFF rows show state. Explicit `on`/`off` also works. `status` and `help` report information; `debug off` disables all switches.

God prevents HP damage, not every status effect. One-shot still respects valid-hit gates and cannot kill the immortal Training target. Free upgrades preserve ownership checks and +2 cap. No-CD preserves active-action restrictions.

Enabling any cheat permanently blocks save writes for the current live game, even after disabling it or loading a slot. Existing files can still be read. Restart game creates a clean session. Debug flags are not stored in saves.

## Implementation references

- [Production gameplay integration scenarios](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/1a2c60c2b7602e2501d86ac380c364b1d704b175/Activity_2/core/src/test/java/au/edu/anu/comp2120/screen/CoreGameplayIntegrationTest.java).
- [Player movement and invalid-input tests](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/1a2c60c2b7602e2501d86ac380c364b1d704b175/Activity_2/core/src/test/java/au/edu/anu/comp2120/player/PlayerTest.java), [collision sweeps](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/1a2c60c2b7602e2501d86ac380c364b1d704b175/Activity_2/core/src/test/java/au/edu/anu/comp2120/world/CollisionSweepTest.java) and [attack wall occlusion](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/1a2c60c2b7602e2501d86ac380c364b1d704b175/Activity_2/core/src/test/java/au/edu/anu/comp2120/combat/CombatOcclusionTest.java).
- [Equipment upgrade validation](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/1a2c60c2b7602e2501d86ac380c364b1d704b175/Activity_2/core/src/test/java/au/edu/anu/comp2120/inventory/GearUpgradesTest.java).
- [Test coverage review](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/1a2c60c2b7602e2501d86ac380c364b1d704b175/Activity_2/docs/TEST_COVERAGE_REVIEW.md): coverage map, dated execution blockers and manual QA gaps; not a measured coverage percentage.
- [Debug, Training and UI report](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/1a2c60c2b7602e2501d86ac380c364b1d704b175/Activity_2/docs/DEBUG_TRAINING_UI_REPORT.md): commands, training behavior and feature-to-test mapping.
- [Save/load report](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/1a2c60c2b7602e2501d86ac380c364b1d704b175/Activity_2/docs/SAVE_LOAD_REPORT.md): slot safety, persistence and dated checks.
