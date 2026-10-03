# Activity 3.3 — Automated testing and code coverage

## Scope and final result

The automatic game tester is the JUnit suite in Activity 2. It combines unit tests,
production-model integration scenarios and real screen/input integration tests.
It checks command-to-state behavior, expected events and rejection of illegal actions.
This report contains the **final full-suite result only**.

The verified full CI command completed with BUILD SUCCESSFUL: 1,454 core + 4 desktop =
1,458 tests passed, with 0 failures and 0 skipped tests. Both modules built and both
strict JavaDoc audits passed. These totals come from the original Gradle HTML reports
in [job #7719170](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/jobs/7719170), whose artifacts were created on
4 October 2026 at 10:21:49 Australia/Sydney (UTC+11).

The measurement includes **all compiled core production classes**, including screens,
rendering and audio code. There are no custom coverage exclusions. Desktop launcher
classes are outside the core coverage denominator; their tests are included in the
full build. A test pass rate and code coverage measure different things.

| Core metric | Covered / total | Coverage |
| --- | ---: | ---: |
| Lines | 5,683 / 5,822 | 97.61% |
| Branches | 3,845 / 4,340 | 88.59% |
| Instructions | 36,137 / 37,065 | 97.50% |
| Methods | 1,020 / 1,024 | 99.61% |
| Classes | 135 / 135 | 100.00% |

| Production package | Line coverage | Branch coverage |
| --- | ---: | ---: |
| root coordinators | 99.58% | 91.35% |
| audio | 100.00% | 96.43% |
| combat | 98.92% | 91.82% |
| enemy | 97.20% | 89.14% |
| inventory | 100.00% | 97.98% |
| npc | 97.60% | 92.86% |
| player | 99.23% | 92.34% |
| quest | 100.00% | 94.44% |
| save | 99.08% | 91.91% |
| screen | 96.20% | 84.69% |
| world | 98.65% | 91.57% |

Verified source: `4bb2a4f4ad523fb1ba481eb353146fa2e6ad54f8`. The archived manifest records
SHA-256 hashes of the production/test Java sources and build configuration, verified
against that commit. [Pipeline #1053155](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/pipelines/1053155) passed.

Environment: the `eclipse-temurin:25-jdk` Docker image on the `ci-teach1` runner,
Gradle 9.7.1 and JaCoCo 0.8.15. The exact image digest is recorded in the manifest.
The project requires its configured JDK 25 toolchain and compiles Java sources with
`--release 17`.

## What is tested

Test sources: [core tests](../../Activity_2/core/src/test/java/au/edu/anu/comp2120/).

| Area | Scenarios and expected behavior |
| --- | --- |
| Movement and world collision | `PlayerTest`, `CollisionSweepTest`, `AStarPathfinderTest`: movement stays within map boundaries, slides around obstacles and cannot cross walls; invalid time/directions preserve state. |
| Full gameplay loop | `CoreGameplayIntegrationTest`: `PlayerCommand` inputs enter admitted portals, execute real ability combat, settle victory/reward once, return after defeat and permit retry. |
| Attacks, classes and Bosses | Combat/occlusion/Boss/ability suites cover walls, damage, phases, cooldowns and class modes. Explicit freeze/stagger/death precedence, damage-interrupted casts and atomic rejection of invalid restored timers/selections are included. |
| Inventory, equipment and upgrades | Ownership, equip/unequip, potions, whole-stack transfers, overflow protection, upgrade selection/costs and the +2 cap. Rejected operations must preserve materials. |
| Save/load | Corrupt snapshot guards, quest/permission/reward consistency, independent slots, no partial live-state replacement and debug save protection. Rejection checks compare live state and existing save bytes. |
| Settings | Fresh class initialization reads isolated files; defaults, valid values, clamping, malformed numbers, absent properties, unavailable paths and persisted settings are checked. |
| Real screen input | `ScreenFlowTest`: keyboard/pointer title and pause menus, settings, F5/F9, overwrite cancellation, direct slot loading, restart, debug-console input ownership and paused movement. |
| NPCs and item panels | Keyboard and left/right clicks select/confirm dialogue options; invalid Battle travel is rejected. Tests exercise class selection, merchant upgrades, bag/chest actions, page controls, ordinary dialogue and repeat interaction. |
| Rendered combat and completion | All three classes and both ultimate-mode configurations are selected and exercised with attack commands in Training, asserting real damage and target survival. Five arranged Boss defeats exercise settlement/autosave and unlock the Hall ending; every Credits page is checked, including reopening and closing. |
| Quest completion layout | `ScreenFlowTest.questChecksAlignWithBossGlyphs`: check strokes align with the actual Boss-name glyphs at 960×540, 640×360, 1920×1080 and 1280×240. Empty, partial and complete victory sets draw only the appropriate checks. |
| Post-ending objective | `GameScreenObjectiveTest.endingReplacesTasksWithFreeExploration`: ending state takes priority over earlier objectives. The screen campaign returns from the Hall to the Hub, checks `feel free to explore this game!` and the absence of a destination marker, then saves and reloads to verify persistence. |

`ScreenFlowTest` executes real screen code, real fonts and image decoding. Recording
proxies replace GPU/audio/platform devices and input is injected at the LibGDX boundary.
Assertions check gameplay state, file preservation, packaged resources and device calls.
They **do not prove that native pixels are correct or that sound is audible**.

The five-victory screen scenario sets Boss HP directly to arrange each defeat; it is a
settlement/ending regression, not a claimed manual five-Boss playthrough. Real attack
victories are tested separately by `CoreGameplayIntegrationTest`. NPC/portal fixtures
position the player before input; movement/collision scenarios separately test travel
by commands. This distinction makes the evidence reproducible without overstating it.

## Run the tests and inspect results

Use the checked-in wrapper and JDK 25. No game window, display server, Docker container
or sound device is required for the automated suite. The first run needs dependency
access, including JaCoCo and LibGDX desktop native decoders. Core Gradle tests use
`core/build/tmp/test-home` as `user.home`; individual file fixtures use temporary
folders, keeping the normal player save/settings directory separate.

From the repository root:

```sh
cd Activity_2
./gradlew clean build javadocAudit :core:jacocoTestReport --no-daemon
```

If already in `Activity_2`, run only the `./gradlew` line. On Windows use `gradlew.bat`.
This builds both modules, executes both test suites, audits production/test JavaDoc
including private members, and generates core coverage.

For a fresh core-only run, from `Activity_2`:

```sh
./gradlew :core:test :core:jacocoTestReport --rerun-tasks --no-daemon
```

For selected automatic game-tester scenarios, from `Activity_2`:

```sh
./gradlew :core:test --rerun-tasks --no-daemon \
  --tests '*CoreGameplayIntegrationTest' --tests '*ScreenFlowTest' \
  --tests '*CollisionSweepTest' --tests '*CombatOcclusionTest' \
  --tests '*GameSavesTest' --tests '*GearUpgradesTest'
```

Run the full core command again after filtered debugging to regenerate representative
coverage. An `UP-TO-DATE` task is not evidence of a fresh execution.

| Output | Path relative to Activity_2 |
| --- | --- |
| Core test HTML | `core/build/reports/tests/test/index.html` |
| Desktop test HTML | `lwjgl3/build/reports/tests/test/index.html` |
| Core JaCoCo HTML | `core/build/reports/jacoco/test/html/index.html` |
| Raw core coverage | `core/build/reports/jacoco/test/jacocoTestReport.xml` |
| JUnit XML | Each module's `build/test-results/test/` |
| Strict JavaDoc | Each module's `build/docs/javadoc-audit/` |

A successful run exits with zero and prints `BUILD SUCCESSFUL`. Inspect test failures,
errors and skipped counts; skipped tests are not passes. A failed assertion identifies
the expected/actual state and makes the test task fail. JaCoCo's first HTML percentage
column is **instruction coverage**, not line coverage; use the labelled counters.

## Why coverage can remain below 100%

The final report leaves **139 lines** and **495 branch outcomes** uncovered.
The largest missed-line groups are `GameScreen.java` (54), `SceneArt.java` (17), `PlaceholderRenderer.java` (16), `BossTraits.java` (7), `EnemyAI.java` (6).
The evidence manifest lists every uncovered source line for review.

A line can be counted as executed while only one side of a condition is tested. Branch
coverage therefore remains lower than line coverage, particularly in screens that
combine menu ownership, map type, liveness, input and viewport guards. Testing every
possible combination is different from testing each supported user journey.

The remaining gaps are not dismissed as unimportant. Examples in the final XML include:

- Defensive arithmetic/capacity paths, such as exhausting a `long` attack identifier,
  floating-point rounding corrections during pathfinding, and impossible/invalid state
  combinations already filtered by outer coordinators. Reaching every inner guard
  would require unrealistic iteration counts or direct private-state corruption.
- Optional presentation and timing combinations: alternate visual effects, fallback
  drawing, unusual viewport sizes, wrapped notification resizing and device-state
  branches. Recording-device tests exercise drawing commands but cannot validate
  actual driver behavior, final pixels, focus/fullscreen behavior or audible output.
- Some screen routes and cancellation/error combinations remain unexecuted by the
  screen integration suite. For example, saved-slot selection and restoration have
  separate tests, but this does not claim an end-to-end native title Continue test.
  These gaps are visible in the XML rather than removed from the denominator.

High coverage is useful evidence that assertions reached much of the implementation;
it is not proof of correctness, complete branch combinations or an absence of bugs.
A green CI run can coexist with bugs found during manual play. Native layout, audio,
platform behavior and a complete human playthrough still need separate evidence in
[the playtest report](../../Activity_2/docs/PLAYTEST_REPORT.md). Existing dated playtests
must not be relabelled as a new run of this change.

## CI/CD

[The repository pipeline](../../.gitlab-ci.yml) defines `gradle-build` using
`eclipse-temurin:25-jdk` and the `comp2120` runner tag. It executes the same full-build
command above, with no job rule excluding main pushes. After this change is merged,
a main-branch commit triggers the job, subject to runner availability.

The job retains core/desktop JUnit XML and HTML, plus core JaCoCo HTML/XML. GitLab
receives both `junit` and `coverage_report` artifacts. Security jobs are separate.
No test is disabled to obtain a green build, and no coverage threshold is claimed.
The verified job executed both test tasks and both JavaDoc audits; its three up-to-date
actionable tasks were clean tasks, not reused test results.

Test totals use the Gradle reports, counting each parameterized invocation. GitLab
deduplicates imported cases with the same class and display name; its summary is not
used as the execution total.

## Archived raw evidence and references

[Download final raw evidence](coverage-evidence.zip). The bundle contains the unchanged
GitLab artifact download (`ci-artifacts.zip`), both modules' Gradle test HTML trees and
core JaCoCo HTML/XML inside it, a convenient copy of the JaCoCo XML, a build-log excerpt,
and a manifest recording the command, environment, totals and SHA-256 hashes. Extract
both archives and open the HTML index files listed in the bundled README to inspect
test cases and coverage locally. CI uploads JUnit XML separately; those XML files are
not included in the downloaded archive, and the totals here are read from Gradle HTML.
Only this verified run is included. The bundle preserves evidence across `gradlew clean`.

- [Evidence summary](RESULTS.json): machine-readable final totals and source manifest.
- [Test coverage review](../../Activity_2/docs/TEST_COVERAGE_REVIEW.md).
- [Save/load report](../../Activity_2/docs/SAVE_LOAD_REPORT.md).
- [Wiki tester page](../wiki/Game-Tester.md) and [documentation index](../wiki/Documentation-Index.md).
