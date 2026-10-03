# Project documentation

All 18 documentation files under `Activity_2` were reviewed against the local source at
`7fea86a` on 2026-10-02. This includes this directory, the game README and the three asset
README files. Feature reports now describe the current implementation; superseded prototype
controls, completed-feature TODOs, temporary local verification commands and old handoff
suggestions have been removed. Development history remains available in Git.

Native evidence was added on **2026-10-03** at `1ae3c78`. After the campaign captures,
`GameScreen.java` was changed to fix the Training/Objectives overlap; new captures distinguish
that working-tree build from the original review baseline. See the [playtest report](PLAYTEST_REPORT.md) and
[screenshot index](screenshots/README.md) for the executed checks and limitations.

## Reading guide and audit scope

| Document | Current content checked against code |
| --- | --- |
| [Game README](../README.md) | Dependencies, build/run/package steps, title menu, settings, controls and playthrough |
| [Architecture](ARCHITECTURE.md) | Modules, state ownership, frame order, persistence and real scope decisions |
| [Integration](INTEGRATION_REPORT.md) | Production gameplay path, startup/settings, configuration and lifecycle |
| [Automated game tester](TEST_COVERAGE_REVIEW.md) | How to run, command/action-to-state scenarios, illegal-operation checks, CI evidence and manual gaps |
| [Class skills](FINAL_CLASS_SKILLS_REPORT.md) | Actual J/K/L/O skills, per-class selections, timing and shared combat integration |
| [Element reactions](ELEMENT_REACTION_COMPLETION_REPORT.md) | Ordered reactions, attachment, statuses, fields and damage boundaries |
| [Wall collision and occlusion](COMBAT_OCCLUSION_REPORT.md) | Movement/projectile wall checks, fixed-effect visibility and current open-map limits |
| [Guardian traits](BOSS_TRAIT_REPORT.md) | Five mechanics, shared pattern framework, warnings and rematch scaling |
| [Quest and portal flow](QUEST_PORTAL_FLOW_REPORT.md) | Quest acceptance, Guide routing, results, stones, Sage ending and credits |
| [Player defeat](PLAYER_DEFEAT_REPORT.md) | Outcome priority, cleanup, preserved progress and retry behavior |
| [Inventory/equipment](INVENTORY_EQUIPMENT_REPORT.md) | Items, bonuses, potions, storage, upgrades and reward collection |
| [Save/load](SAVE_LOAD_REPORT.md) | Both slots, Continue, management, safe states, snapshots, validation and debug restrictions |
| [Debug/Training/UI](DEBUG_TRAINING_UI_REPORT.md) | Console commands, Training projectile/recovery/reset, HUD and input ownership |
| [Native playtest](PLAYTEST_REPORT.md) | Dated native steps/results, position fixtures, screenshots, issues and remaining acceptance |
| [Art integration](ART_INTEGRATION.md) | Current asset use, rendering ownership, HUD, status labels and native verification limits |
| [Asset attribution](../assets/README.md) | Recorded visual/audio provenance and license references |
| [Audio README](../assets/audio/README.md) | Three shipped tracks, switching, looping and persistent versus session volume |
| [Art pack README](../assets/art/README.txt) | Current 232-image manifest and procedural UI boundary |

The stable report filenames are retained so existing Wiki references remain usable.
The [Wiki Game Tester page](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/wikis/game-tester)
contains the previously recorded CI screenshot. This local documentation audit does not claim
to have republished or rechecked all Wiki pages.

## Verification reference

[The tester guide](TEST_COVERAGE_REVIEW.md#recorded-result) is the single detailed result record:
**1,302 passed, zero failed/error/skipped**, pipeline **#1052082**, source commit **1a2c60c**.
Its build job ran `./gradlew clean build javadocAudit --no-daemon` with JDK 25.
A local Git comparison confirms that `1a2c60c..7fea86a` changes only Markdown within Activity_2
and does not change its source/assets/build configuration or root CI configuration.
This supports the source comparison; it is not a new pipeline execution at `7fea86a`.

This update refreshes documentation, adds native screenshots and fixes the Training HUD overlap. The 2026-10-03 native
scenario results are recorded separately from CI; no new full Gradle-suite success or coverage
percentage is claimed. All five distinct wins, Hall ending, all Credits pages, save restoration and relaunch were
verified across the combined session. Wind was completed by the user without debug features;
agent-controlled travel used position-only fixtures. Remaining manual gaps are explicit in the report.
No launch approval is pending; the user launched the temporary app successfully.

Local documentation checks on 2026-10-02 passed: 114 relative file/directory links and heading
anchors resolve; all 87 documented test selectors match existing test classes; all 232 PNG
manifest paths and recorded dimensions match the files. Referenced implementation commits
exist in local Git history, Markdown fences are balanced, and `git diff --check` is clean.
These checks validate documentation structure and source references, not test execution.

Final local checks on 2026-10-03 passed: 231 relative links/anchors resolve, all 87 documented
test selectors match existing classes, all 41 JPEG captures have valid image headers/dimensions
and recorded SHA-256 checksums, and `git diff --check` is clean. The images total approximately
3.95 MB. The only production source change is the Training panel stacking in `GameScreen.java`;
build configuration is unchanged. Fresh direct-JDK compilation, 83 selected JUnit tests and
strict JavaDoc for that file passed; native captures verify the wrapped-objective layout.

## Maintaining these documents

Update the game README and the relevant feature report when player behavior changes.
Update Architecture when state ownership, frame order or persistence contracts change.
Keep run instructions separate from dated execution results. Record actual commit/pipeline
identities for results and distinguish model assertions from native window/audio checks.
Do not copy stale test totals into each report or describe test-fixture shortcuts as game controls.
