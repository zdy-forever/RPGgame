# Game Tester and Verification

[Home](home) · [Installation and Setup](installation-and-setup)

## What the automatic tester is

The automatic game tester is the Gradle-run JUnit suite, including scripted integration scenarios that exercise the production gameplay models and coordinators. It sends `PlayerCommand` inputs, advances bounded simulation steps and asserts the resulting state or event. This follows the assignment examples of command-to-state testing, action-to-event testing and rejection of illegal operations. The assignment description supplied for this project does not require a separate interactive executable.

The suite runs without opening the game window. It does not automate a native desktop window; screen tests inject keyboard/pointer input through recording device boundaries. Training and the debug console are additional manual testing aids; the tutor can separately play the graphical game using the User Manual.

## Activity 3.3 testing and coverage

[Activity 3.3 — automated testing and code coverage](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/64310bfa5869a213c6e9025bc6eff2d45effcc3e/Activity_3/3.3/REPORT.md)
explains the test scenarios, execution commands and reasons for remaining coverage gaps.
The current measured results for main `64310bf` are recorded below.

The suite includes real screen/input integration with recording GPU/audio boundaries:
keyboard and mouse menus, save/load, inventory/storage, NPC services, all six class/mode
combinations in Training, five-victory settlement, Hall ending and Credits. Graphics
and sound calls are recorded; this does not establish native pixels or audible output.
All core production classes remain included in the coverage report.

Run the complete core coverage measurement from `Activity_2`:

```sh
./gradlew :core:test :core:jacocoTestReport --rerun-tasks --no-daemon
```

Open `core/build/reports/jacoco/test/html/index.html`. The report includes all core
production classes, including rendering code, with no custom exclusions. Line/branch
coverage differs from the percentage of tests that passed. Selected `--tests` runs do
not represent full-suite coverage; regenerate the full report after focused debugging.

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
./gradlew clean build javadocAudit :core:jacocoTestReport --no-daemon
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
| `ScreenFlowTest` | Real screen input, save/load, NPC menus, bag/chest, class combat and ending/Credits; recording GPU/audio boundaries |
| World collision and combat occlusion tests | Movement respects geometry; blocked attacks do not damage through walls |
| `BossTraitTest` and Boss tests | Guardian-specific mechanics, phases and encounter rules |
| `EquipmentIntegrationTest`, `GearUpgradesTest` | Ownership, stat effects, material consumption and +2 cap |
| `GameSavesTest`, `SaveStoreTest` | Snapshot restoration, invalid files, slot separation, debug save protection and Sydney display |
| `PortalGuideTrainingTest`, `CombatDemoTest` | Practice shots, mitigation, recovery, minimum health, paused simulation and stable reset positions |
| `DebugOptionsTest`, `DebugConsoleTest` | Toggle commands, input editing, state colours and input-processor restoration |

A passing test means its assertions hold for the scenario tested. For example, the debug-victory save-file test settles the Boss programmatically; it does not establish a full keyboard-driven one-shot playthrough.


## Assignment examples mapped to real tests

Implementation references for these scenarios are pinned to reviewed main `64310bf`. The CI result below verifies that revision.

| Requirement | Input/actions performed | Expected state/event | Test evidence |
| --- | --- | --- | --- |
| Send commands and reach a state | Configure the Hub route and repeatedly issue rightward `PlayerCommand` steps into the portal | Arrive at the selected map/spawn with the correct collision area and exactly the appropriate Boss or Training target | `CoreGameplayIntegrationTest.admissionByMovementUsesCurrentMapCollisionAndOneTarget` |
| Actions produce an expected event | Run a bounded fight using movement/basic/special/ultimate commands for the three classes and A/B selections | SUCCESS, return to Hub with full health, exactly one claimable reward, closed completed encounter | `CoreGameplayIntegrationTest.actualCombatCompletesOneRewardAndClosesPermission` |
| Defeat and retry | Advance simulation under real Boss attacks, then re-enter the encounter | FAILURE without victory rewards; full-health Hub return; fresh phase-one Boss on retry | `CoreGameplayIntegrationTest.actualBossCombatFailureReturnsAndRetries` |
| Cannot walk through walls or leave the map | Apply diagonal movement beside a wall and movement toward map boundaries | Slide along the wall or stop at the boundary | `PlayerTest.diagonalMovementSlidesAlongWall`, `stopsAtMapBoundary`; `CollisionSweepTest.mapCombinesWallsAndWorldBounds` |
| Cannot attack through walls | Execute class attacks with blocked/unblocked lines of sight | The wall blocks damage; the unblocked control can hit | `CombatOcclusionTest.classAttacksCannotHitBossThroughWall` |
| Illegal actions do not change valid state | Request invalid movement/time or disallowed equipment upgrades | Reject invalid input without state corruption; rejected upgrades spend no materials; upgrades stop at +2 | `PlayerTest.rejectsInvalidTimeWithoutChangingState`, `rejectsInvalidDirection`; `GearUpgradesTest.rejectedUpgradesNeverSpendMaterials`, `allSlotsUseSharedCoresAndStopAtTwo` |

An actual portal scenario advances the production model with the following statement inside the test method. This is an excerpt, not a standalone Java file:

```text
frame(0.1, new PlayerCommand(1, 0, false, false, false, false));
```

The test then checks the selected map, named spawn and encounter state. These commands use the real model API; they are not simulated keyboard events sent to the desktop window. Combat victory scenarios use real ability damage, while some save/failure fixtures deliberately arrange state directly. Each test must be interpreted within its own setup.

## How to tell whether it behaved as expected

- A fresh successful run ends with `BUILD SUCCESSFUL` and a zero exit status. Open both HTML reports and check failures, errors and skipped tests. A skipped test is not a demonstrated pass.
- A failing assertion makes the Gradle test task fail. The report identifies the test, expected/actual result and stack trace; inspect that failure before claiming the behavior works.
- `UP-TO-DATE` or `FROM-CACHE` is not a fresh execution. Use the command above when collecting submission evidence.
- Core report: `core/build/reports/tests/test/index.html`; desktop report: `lwjgl3/build/reports/tests/test/index.html`; machine-readable results: each module's `build/test-results/test/`.
- Record the source commit, date, OS/JDK, exact command and result together. Attach a report/pipeline screenshot for that run; a screenshot of Training alone does not prove automated tests passed.

The CI result below establishes that the suite passed for the identified commit. Native input, audio, rendering and a complete human playthrough remain separate checks.

## Latest results and coverage — 3 October 2026

| Item | Observed result |
| --- | --- |
| Source commit | `64310bfa5869a213c6e9025bc6eff2d45effcc3e` |
| Pipeline | [#1052939](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/pipelines/1052939), passed |
| Build job | [gradle-build #7718629](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/jobs/7718629), passed |
| Environment | GitLab Docker executor, `eclipse-temurin:25-jdk`, Gradle 9.7.1 |
| Executed command | `./gradlew clean build javadocAudit :core:jacocoTestReport --no-daemon` from `Activity_2` |
| Raw Gradle test reports | 1,449 core + 4 desktop tests; 0 failures and 0 skipped |
| Build/JavaDoc | Both modules tested and both strict audits completed; `BUILD SUCCESSFUL` |

All **1,453 tests passed**, with zero failures, errors or skipped tests. The totals come
from both generated Gradle HTML reports in the [job archive](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/jobs/7718629/artifacts/download?file_type=archive).
[Browse the artifacts](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/jobs/7718629/artifacts/browse).

The GitLab summary deduplicates test cases with the same class and display name.
Parameterized invocations in this suite can share names such as `[1] ARCHER` across
different methods in one class. The underlying Gradle reports retain all executions;
use those reports for the complete test total. This is a reporting-name collision,
not an omitted test execution. See [GitLab's JUnit reporting rules](https://docs.gitlab.com/ci/testing/unit_test_reports/).

The same archive's `Activity_2/core/build/reports/jacoco/test/jacocoTestReport.xml`
records the following core production-code coverage, with no custom class exclusions:

| Metric | Covered / total | Coverage |
| --- | --- | --- |
| Lines | 5,682 / 5,821 | 97.61% |
| Branches | 3,844 / 4,338 | 88.61% |
| Instructions | 36,158 / 37,084 | 97.50% |
| Methods | 1,020 / 1,024 | 99.61% |
| Classes | 135 / 135 | 100% |

These measurements cover core production code at `64310bf`. Passing assertions and
high coverage do not establish native rendering, audible music, balance or every
possible player interaction.

## CI and JavaDoc

The repository's [CI configuration](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/64310bfa5869a213c6e9025bc6eff2d45effcc3e/.gitlab-ci.yml) defines `gradle-build`
using `eclipse-temurin:25-jdk` and runner tag `comp2120`. It runs
`clean build javadocAudit :core:jacocoTestReport --no-daemon`, executing both modules'
tests and retaining JUnit XML, test HTML and core JaCoCo HTML/XML. The job has no branch
filter excluding main pushes. Pipeline #1052939 verifies this configuration at `64310bf`; check the pipeline again
if the final submitted commit changes. The results above are verified remote CI evidence.

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

The 3 October native session now records title/settings, save restoration and relaunch, equipment, five distinct victories, Hall ending and all Credits pages. Wind was won normally by the user; other travel scenarios used position-only save fixtures. Training overlap was subsequently fixed and captured. See [Playtest and Screenshots](Playtest-and-Screenshots) and [the detailed native report](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/64310bfa5869a213c6e9025bc6eff2d45effcc3e/Activity_2/docs/PLAYTEST_REPORT.md). The checklist above still includes cases not completed; do not treat it as an all-platform pass.

![Training is a manual testing aid showing damage/hits and practice instructions. This image verifies layout; automated correctness is evidenced by the JUnit report above.](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/raw/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/screenshots/41-training-layout-fixed-resized.jpg)

Training is a manual testing aid showing damage/hits and practice instructions. This image verifies layout; automated correctness is evidenced by the JUnit report above.


## Debug console

F10 or backtick opens the console. Enter runs a command; Esc closes. Bare `god`, `oneshot`, `freeupgrade` and `nocd` toggle their state; entering one again disables it. Green ON and white OFF rows show state. Explicit `on`/`off` also works. `status` and `help` report information; `debug off` disables all switches.

God prevents HP damage, not every status effect. One-shot still respects valid-hit gates and cannot kill the immortal Training target. Free upgrades preserve ownership checks and +2 cap. No-CD preserves active-action restrictions.

Enabling any cheat permanently blocks save writes for the current live game, even after disabling it or loading a slot. Existing files can still be read. Restart game creates a clean session. Debug flags are not stored in saves.

## Implementation references

- [Production gameplay integration scenarios](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/64310bfa5869a213c6e9025bc6eff2d45effcc3e/Activity_2/core/src/test/java/au/edu/anu/comp2120/screen/CoreGameplayIntegrationTest.java).
- [Player movement and invalid-input tests](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/64310bfa5869a213c6e9025bc6eff2d45effcc3e/Activity_2/core/src/test/java/au/edu/anu/comp2120/player/PlayerTest.java), [collision sweeps](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/64310bfa5869a213c6e9025bc6eff2d45effcc3e/Activity_2/core/src/test/java/au/edu/anu/comp2120/world/CollisionSweepTest.java) and [attack wall occlusion](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/64310bfa5869a213c6e9025bc6eff2d45effcc3e/Activity_2/core/src/test/java/au/edu/anu/comp2120/combat/CombatOcclusionTest.java).
- [Equipment upgrade validation](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/64310bfa5869a213c6e9025bc6eff2d45effcc3e/Activity_2/core/src/test/java/au/edu/anu/comp2120/inventory/GearUpgradesTest.java).
- [Test coverage review](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/64310bfa5869a213c6e9025bc6eff2d45effcc3e/Activity_2/docs/TEST_COVERAGE_REVIEW.md): coverage map, dated execution blockers and manual QA gaps; not a measured coverage percentage.
- [Debug, Training and UI report](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/64310bfa5869a213c6e9025bc6eff2d45effcc3e/Activity_2/docs/DEBUG_TRAINING_UI_REPORT.md): commands, training behavior and feature-to-test mapping.
- [Save/load report](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/64310bfa5869a213c6e9025bc6eff2d45effcc3e/Activity_2/docs/SAVE_LOAD_REPORT.md): slot safety, persistence and dated checks.

[Complete document/reference map](Documentation-Index).
