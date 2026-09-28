# Project documentation

Updated 2026-09-28 for current debug, Training, map, input and save-display refinements.

## Current verification — 2026-09-28

`./Activity_2/gradlew -p Activity_2 test javadocAudit lwjgl3:classes` passed:
**1,235 core + 4 desktop = 1,239 tests**, zero failures, errors or skipped tests.
JavaDoc audits and desktop compilation passed. This audit adds nine test cases for console input
ownership/editing/colour state, one-shot boundaries, all four no-CD slots, non-revival/isolation,
and free-upgrade safety. `git diff --check` is clean.

The [coverage matrix and manual regression cases](DEBUG_TRAINING_UI_REPORT.md) distinguish model
verification from actual window/key/mouse rendering checks. These results do not claim a fresh
manual playthrough, and no commit/MR identity is assigned to uncommitted work.

Follow-up interaction audit on the same date fixed NPC dialogue simulation and no-CD save
restoration. Full verification now passes **1,237 core + 4 desktop = 1,241 tests**; JavaDoc
and desktop compilation also pass. See the follow-up section in the coverage report.

## Where to start

| Document | Purpose |
| --- | --- |
| [Game README](../README.md) | Setup, launch, current controls, gameplay and module overview. |
| [Architecture](ARCHITECTURE.md) | Ownership, game-loop integration, persistence boundaries and contribution evidence. |
| [Inventory and equipment](INVENTORY_EQUIPMENT_REPORT.md) | Obtaining/equipping items, storage transfers, bonuses, potions and one-time rewards. |
| [Save and load](SAVE_LOAD_REPORT.md) | Esc menu, restart, volume, transient feedback, slot independence, safe-state rules and regression checks. |
| [Debug, Training and UI](DEBUG_TRAINING_UI_REPORT.md) | Current commands, training rules, feature-to-test coverage and manual regression cases. |
| [Art integration](ART_INTEGRATION.md) | Runtime artwork, HUD presentation and asset coverage. |
| [Guardian traits](BOSS_TRAIT_REPORT.md) | Boss mechanics, status display and balance measurements. |
| [Asset attribution](../assets/README.md) | Visual/audio provenance and licensing references. |

## Integration and historical reports

[Quest portal flow](QUEST_PORTAL_FLOW_REPORT.md), [player defeat](PLAYER_DEFEAT_REPORT.md),
[class skills](FINAL_CLASS_SKILLS_REPORT.md), [wall occlusion](COMBAT_OCCLUSION_REPORT.md),
[element reactions](ELEMENT_REACTION_COMPLETION_REPORT.md) and the original
[placeholder integration](INTEGRATION_REPORT.md) retain the design and test evidence from
those development stages. Their old debug keys, missing-feature lists, test counts and
suggested handoff text are not current release instructions. Use the game README for
current controls and the feature reports above for inventory/persistence behavior.

Activity 0's [marking note](../../Activity_0/Activity_0.md) and Activity 1's
[skeleton reference](../../Activity_1/Game_skeleton.md) serve earlier assignment activities;
they are not the save/load user guide.

## Delivered work and evidence

| Work | Traceability / validation boundary |
| --- | --- |
| Bag/equipment, item bonuses, potions, reward transfer | `21de2898`, merged through !38 / `b3ac158b`; original dated checks are retained in the inventory report. |
| Save foundations and complete gameplay integration | `82f6a450`, `c8d14bc9`, merged as `d47a1f6b`; original integration run recorded 1,165 core and 4 desktop tests passing. |
| User save/load acceptance | User reported restoration of position, bag/equipment and chest contents, including after restart. This does not establish every failure-path or Boss playthrough check. |
| Notification lifetime, position and resizing | Follow-up working-branch refinement; 62 focused save/menu/HUD tests plus core JavaDoc audit and desktop compilation passed on 2026-09-24. Visual resize/dialogue regression remains to be checked. |
| Storage-to-bag guidance and empty weapon wording | Follow-up working-branch refinement; core compilation and JavaDoc audit passed on 2026-09-25. Visual interaction check remains to be performed. |
| Boss victory autosave | Covered by automated save integration tests; a complete manual victory/checkpoint/reload playthrough remains outstanding. |

The subsequent Esc menu redesign passes **1,191 core and 4 desktop tests**, JavaDoc audits
and desktop compilation. Its complete native visual/restart checklist is in the save/load
report; it is not claimed as manually accepted. The earlier top-left notification design
is superseded by this menu and transient-toast implementation.

These results belong to their recorded code stages. The subsequent pre-merge verification on 2026-09-25 passed **1,184 core tests and 4 desktop
tests**, plus JavaDoc/build checks using the local JDK 26 override. It does not establish
that every checklist item has been manually accepted.
The team subsequently changed Wind Boss behavior and added the P potion shortcut; those
are distinct from Hao Liu's inventory/save implementation.

## Keeping documentation current

When changing player behavior, update the game README and the relevant feature report.
When changing ownership, frame ordering or snapshot contracts, also update Architecture.
Record actual test commands/results separately from future regression instructions, and
add the real commit or merge reference once it exists. An Issue tracks scope and completion;
it complements these versioned project documents rather than replacing them.
