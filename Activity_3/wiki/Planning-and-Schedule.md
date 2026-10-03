# Planning and Schedule

[Home](home) · [User Stories](User-Stories) · [Reflection](Reflection)

## Planning evidence

The repository contains a [work plan](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/64310bfa5869a213c6e9025bc6eff2d45effcc3e/Activity_1/1.2.md), [story catalogue](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/64310bfa5869a213c6e9025bc6eff2d45effcc3e/Activity-1-User-Stories.md) and dated architecture/implementation reports. GitLab provides the execution record:

- [Issues](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/issues)
- [Milestones](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/milestones)
- [Merge requests](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/merge_requests)
- [Main commit history](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/commits/main)

The plan describes lightweight iterative development, with integration and feedback driving later tasks. Planned dates below are reproduced from that document; they are not actual completion dates.

| Milestone | Planned dates in 2026 | Purpose |
| --- | --- | --- |
| M2 — Core Prototype Implementation | 7–24 September | Movement, maps, health, combat, classes, Bosses and elements |
| M3 — Integration & Feature Refinement | 19–27 September | NPCs, quests, inventory, saves and refinement |
| M4 — Verification & Quality Assurance | 26–29 September | Tests, CI and automatic testing |
| M5 — Documentation & Deployment Readiness | 28 September–1 October | Wiki, README, JavaDoc and licensing |
| M6 — Release Candidate & Final Submission | 30 September–3 October | Packaging and final verification |

The plan explains that M1 was initial discussion/document preparation and was not created as a GitLab milestone. Dependencies progress from collision/maps to combat, classes/Bosses, quest outcomes, inventory rewards and finally save snapshots.

## Traceable examples

| Work | Evidence recorded in the repository |
| --- | --- |
| Inventory/equipment integration | [Issue #47](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/issues/47), commit `21de289`, MR !38 |
| Save/load | [Issue #48](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/issues/48), commits `82f6a45` and `c8d14bc` |
| Wind guardian correction | [Issue #49](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/issues/49), commit `36ce30d` |
| Pause/save management and equipment progression | Commits `a4c7e9f` and `bbe814b` |
| UI, controls and Training tools | Commit `5aa5c49` |
| Documentation refresh, native evidence and Training HUD correction | [Commit 626e88e](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/commit/626e88eba47cace51d30c471e20c1976cbcea2b9), [merge 59de4e6](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/commit/59de4e66120654605150c8baad4ca49dea39224f), [dated playtest](Playtest-and-Screenshots) and [passing main pipeline](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/pipelines/1052436) |
| Debug console and interaction fixes | Commit `3d5638e` and the debug/Training implementation report |

Use the issue/MR discussion, changed files and tests together when reviewing a feature. A closed issue or merge alone does not prove all acceptance checks passed. Some tasks were documented after implementation; the record should preserve their real dates.

## Review and integration

Feature work was integrated with the team repository through branches and merge requests, alongside earlier direct-main work recorded in the architecture history. Later changes required synchronizing with upstream and reconciling shared documentation. The intended review sequence is focused changes, relevant tests, full CI, review, merge and documentation updates.

No meeting minutes are reproduced here because verified minutes were not available in the inspected materials. The role summary below follows the repository's recorded delivered work, rather than treating the original allocation as proof of actual authorship.

## Team roles and delivered responsibilities

| Team member | Recorded delivered responsibilities |
| --- | --- |
| Daoyan Zhu (`u7782042`) | Build/CI, player/world/combat/enemy systems, quest and encounter integration, audio, shared screen integration, tests and documentation |
| Jiaqi Chen (`u7851266`) | NPC/dialogue and inventory prototypes, storage/minimap/HUD, title/settings screens, objective and endgame UI, associated tests |
| Hao Liu (`u7998420`) | Inventory/equipment integration, save/load, pause/save management UI, merchant potion supplies and equipment upgrades |

This summarizes the dated contribution evidence in [Architecture](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/64310bfa5869a213c6e9025bc6eff2d45effcc3e/Activity_2/docs/ARCHITECTURE.md). It is not an exhaustive allocation of every later change or a ranking of effort. The original plan differs from the delivered allocation: for example, inventory began as a prototype and was later integrated by another contributor. `u8350466` remains part of the repository name but is not in the active development allocation recorded there.

## Scope decisions and dependencies

As confirmed by the team on 2 October 2026, achievements, following NPCs, a general crafting system and a scripted tutorial were outside the core playable loop. Achievements additionally depended on save/load, which was completed relatively late; the team did not have enough time to integrate and verify achievements afterward. Merchant upgrades and NPC/Notice Board guidance provide the reduced crafting/onboarding scope. See [Known Issues and Future Work](Known-Issues-and-Future-Work) for each decision.

The original plan's smoke-tester row records a deferral to M4. The delivered automatic verification is the runnable JUnit suite documented in [Game Tester](game-tester), including command/action-to-state and invalid-operation scenarios. This documentation does not invent a separate tester executable or retrospectively change milestone dates.

## Implementation references

- [Architecture and contribution evidence](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/64310bfa5869a213c6e9025bc6eff2d45effcc3e/Activity_2/docs/ARCHITECTURE.md): original boundaries, feature priorities and actual integration records.
- [Documentation index](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/64310bfa5869a213c6e9025bc6eff2d45effcc3e/Activity_2/docs/README.md): report navigation and dated delivery evidence.
- [Inventory integration](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/64310bfa5869a213c6e9025bc6eff2d45effcc3e/Activity_2/docs/INVENTORY_EQUIPMENT_REPORT.md) and [save/load integration](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/64310bfa5869a213c6e9025bc6eff2d45effcc3e/Activity_2/docs/SAVE_LOAD_REPORT.md): feature handoffs, changes and validation boundaries.
- [Debug/Training follow-up](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/64310bfa5869a213c6e9025bc6eff2d45effcc3e/Activity_2/docs/DEBUG_TRAINING_UI_REPORT.md): regression findings and verification checklist.
