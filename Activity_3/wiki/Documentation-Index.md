# Documentation Index and References

[Home](home) · [Installation and Setup](installation-and-setup) · [Playtest and Screenshots](Playtest-and-Screenshots)

## How to use these references

The Wiki provides a tutor-facing overview and runnable instructions. Activity 2 contains detailed implementation contracts, scenario records and the original screenshots. Activity 2 catalogue links below are pinned to **main `59de4e6`**, reviewed 3 October 2026. The later Activity 3.3 report and CI links follow `main`. Historical execution dates/commits inside a report still describe that particular run; they are not new tests of the pinned revision.

[Activity 2 documentation index](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/README.md) · [Game README](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/README.md)

## Activity 3.3 — automated testing and coverage

- [JUnit additions, measured coverage and tester instructions](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/main/Activity_3/3.3/REPORT.md)
- [Final raw coverage and JUnit XML evidence](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/raw/main/Activity_3/3.3/coverage-evidence.zip)
- [Current CI configuration](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/main/.gitlab-ci.yml)
- [Game Tester overview](Game-Tester)

The report distinguishes local test/coverage results from historical GitLab pipeline
results and explains what still requires manual gameplay verification.

## Assignment requirements to evidence

| Requirement | Wiki entry | Supporting evidence |
| --- | --- | --- |
| Project summary and game description | [Home](home), [User Manual](User-Manual) | Game README, quest/portal report |
| Game features, examples and screenshots | [Features](features), [Playtest and Screenshots](Playtest-and-Screenshots) | Feature reports, 41 dated native captures |
| Engine features and examples | [Game Engine](game-engine) | Architecture, configuration example, collision/combat/integration reports |
| Automatic tester: running, functionality and expected results | [Game Tester](game-tester) | Activity 3.3 final coverage and XML, commands, real test-method mapping, historical pipeline #1052436 |
| Planned but unimplemented work and reasons | [Known Issues](Known-Issues-and-Future-Work) | Architecture scope decisions; achievements depended on late save/load |
| Planning, scheduling and roles | [Planning and Schedule](Planning-and-Schedule), [User Stories](User-Stories) | Activity 1 plan, milestones, real issues/commits and contribution records |
| Installation, build, run and test | [Installation and Setup](installation-and-setup) | Game README, wrapper/toolchain/module build files |
| Process notes and reflection | [Reflection](Reflection), [License](License), [Docker](Docker) | Dated implementation records and asset attribution |

## Activity 2 reference catalogue

| Document | Details |
| --- | --- |
| [Game README](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/README.md) | Dependencies, build/run/package steps, title menu, settings, controls and playthrough |
| [Architecture](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/ARCHITECTURE.md) | Modules, state ownership, frame order, persistence and real scope decisions |
| [Integration](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/INTEGRATION_REPORT.md) | Production gameplay path, startup/settings, configuration and lifecycle |
| [Automated game tester](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/TEST_COVERAGE_REVIEW.md) | How to run, command/action-to-state scenarios, illegal-operation checks, CI evidence and manual gaps |
| [Class skills](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/FINAL_CLASS_SKILLS_REPORT.md) | Actual J/K/L/O skills, per-class selections, timing and shared combat integration |
| [Element reactions](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/ELEMENT_REACTION_COMPLETION_REPORT.md) | Ordered reactions, attachment, statuses, fields and damage boundaries |
| [Wall collision and occlusion](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/COMBAT_OCCLUSION_REPORT.md) | Movement/projectile wall checks, fixed-effect visibility and current open-map limits |
| [Guardian traits](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/BOSS_TRAIT_REPORT.md) | Five mechanics, shared pattern framework, warnings and rematch scaling |
| [Quest and portal flow](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/QUEST_PORTAL_FLOW_REPORT.md) | Quest acceptance, Guide routing, results, stones, Sage ending and credits |
| [Player defeat](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/PLAYER_DEFEAT_REPORT.md) | Outcome priority, cleanup, preserved progress and retry behavior |
| [Inventory/equipment](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/INVENTORY_EQUIPMENT_REPORT.md) | Items, bonuses, potions, storage, upgrades and reward collection |
| [Save/load](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/SAVE_LOAD_REPORT.md) | Both slots, Continue, management, safe states, snapshots, validation and debug restrictions |
| [Debug/Training/UI](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/DEBUG_TRAINING_UI_REPORT.md) | Console commands, Training projectile/recovery/reset, HUD and input ownership |
| [Native playtest](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/PLAYTEST_REPORT.md) | Dated native steps/results, position fixtures, screenshots, issues and remaining acceptance |
| [Art integration](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/ART_INTEGRATION.md) | Current asset use, rendering ownership, HUD, status labels and native verification limits |
| [Asset attribution](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/assets/README.md) | Recorded visual/audio provenance and license references |
| [Audio README](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/assets/audio/README.md) | Three shipped tracks, switching, looping and persistent versus session volume |
| [Art pack README](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/assets/art/README.txt) | Current 232-image manifest and procedural UI boundary |

## Screenshot and result provenance

[Native test report](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/PLAYTEST_REPORT.md) records expected versus actual outcomes, preparation, versions and incomplete checks. [Screenshot index](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/screenshots/README.md) lists every capture and its dimensions; [SHA256SUMS](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/screenshots/SHA256SUMS.txt) identifies the original files. Wiki images use the repository's pinned raw URLs; they are not separately edited copies.

The historical full build/test result recorded here is [pipeline #1052436](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/pipelines/1052436), [job #7717341](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/jobs/7717341): 1,302 passed, zero failures/errors/skips, build and strict JavaDoc successful at `59de4e6`. Earlier CI records in Activity 2 remain dated historical evidence. A 100% test success rate is not line/branch coverage.

## Maintenance

When behavior changes, update the relevant Activity 2 report, then the Wiki overview and reference baseline. Keep capture versions and execution results separate. Preserve real planning dates and only add meeting minutes if authentic records exist. Verify the final submitted commit's pipeline and assessor access before submission.
