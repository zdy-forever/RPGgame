# Planning and Schedule

[Home](home) · [User Stories](User-Stories) · [Reflection](Reflection)

## Planning evidence

The repository contains a [work plan](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/main/Activity_1/1.2.md), [story catalogue](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/main/Activity-1-User-Stories.md) and dated architecture/implementation reports. GitLab provides the execution record:

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
| Debug console and interaction fixes | Commit `3d5638e` and the debug/Training implementation report |

Use the issue/MR discussion, changed files and tests together when reviewing a feature. A closed issue or merge alone does not prove all acceptance checks passed. Some tasks were documented after implementation; the record should preserve their real dates.

## Review and integration

Feature work was integrated with the team repository through branches and merge requests, alongside earlier direct-main work recorded in the architecture history. Later changes required synchronizing with upstream and reconciling shared documentation. The intended review sequence is focused changes, relevant tests, full CI, review, merge and documentation updates.

No meeting minutes are reproduced here because verified minutes were not available in the inspected materials. Team names and role allocation remain managed in the existing Home/architecture records.
