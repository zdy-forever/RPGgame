# Game Tester and Verification

[Home](home) · [Installation and Setup](installation-and-setup)

## What the automatic tester is

The inspected implementation uses JUnit tests, including scripted gameplay integration scenarios, as its automatic gameplay verification. Tests construct gameplay models, submit commands, advance simulation and assert expected states. They do not automate desktop pixels or mouse clicks. There is no separate interactive command-line tester executable in this baseline; the debug console is a manual testing aid.

## Run all checks

From `Activity_2`, with JDK 25:

```bash
./gradlew clean build javadocAudit --no-daemon
```

Use `./gradlew.bat` on Windows. `test` alone runs JUnit; the full command also compiles/builds the modules and performs the strict JavaDoc audit. Reports are in each module's `build/reports/tests/test/index.html` and `build/test-results/test/`.

## Run selected gameplay scenarios

```bash
./gradlew core:test --tests '*CoreGameplayIntegrationTest'
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

## Recorded results and coverage

The full validation rerun on 1 October 2026 passed 1,241 tests across 79 JUnit report files, with zero failures, errors or skipped tests. It also passed both JavaDoc audits and desktop compilation. Consult the final pipeline for commit-specific submission evidence.

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
