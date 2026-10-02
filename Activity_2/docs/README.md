# Project documentation

Updated 2026-10-02 for Wiki references, current startup/settings behavior and verified CI evidence.

## Current source CI evidence — 2026-10-02

The GitLab report for commit `1a2c60c2b7602e2501d86ac380c364b1d704b175` records
**1,302 passed, 0 failures, 0 errors and 0 skipped tests** in
[pipeline #1052082](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/pipelines/1052082).
[Build job #7716451](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/jobs/7716451)
ran `./gradlew clean build javadocAudit --no-daemon` with the Docker executor and
`eclipse-temurin:25-jdk`; both module test and JavaDoc audit tasks completed, ending in
`BUILD SUCCESSFUL`. This was read from CI, not newly executed locally. The attempted local
rerun was blocked by the Gradle cache lock. The current local follow-up changes Markdown only.

The [Wiki Game Tester page](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/wikis/game-tester)
contains run instructions, command/action-to-state examples, illegal-operation checks,
the actual CI screenshot and manual acceptance boundaries. The success rate is not line/branch
coverage. Native input, rendering, audio and full campaign acceptance remain separate checks.
The older execution blockers/results below are retained as dated historical records.

## Current working-tree verification — 2026-09-30

See [Test coverage review](TEST_COVERAGE_REVIEW.md). The full `test javadocAudit` run was attempted,
but sandbox access to the Gradle cache lock was denied; the elevated request is awaiting authorization.
There is no new passing total, JavaDoc result or native visual acceptance to report for this checkout.
New tests and the coverage map are not substitutes for execution. Historical evidence below is retained.

## Recorded verification — 2026-09-28

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
| [Test coverage review](TEST_COVERAGE_REVIEW.md) | Current test inventory, new cross-module regressions, execution blockers and manual QA gaps. |
| [Architecture](ARCHITECTURE.md) | Ownership, game-loop integration, persistence boundaries and contribution evidence. |
| [Inventory and equipment](INVENTORY_EQUIPMENT_REPORT.md) | Obtaining/equipping items, storage transfers, bonuses, potions, merchant supplies, +1/+2 upgrades and one-time rewards. |
| [Save and load](SAVE_LOAD_REPORT.md) | Esc menu, save management (load/delete), restart, volume, transient feedback, slot independence, safe-state rules and regression checks. |
| [Debug, Training and UI](DEBUG_TRAINING_UI_REPORT.md) | Current commands, training rules, feature-to-test coverage and manual regression cases. |
| [Art integration](ART_INTEGRATION.md) | Runtime artwork, HUD presentation and asset coverage. |
| [Guardian traits](BOSS_TRAIT_REPORT.md) | Per-guardian signature mechanics, status labels, minimap Boss marker and balance measurements. |
| [Asset attribution](../assets/README.md) | Visual/audio provenance and licensing references. |

## Integration and historical reports

| Report | Stage it records |
| --- | --- |
| [Placeholder integration](INTEGRATION_REPORT.md) | 2026-09-10 placeholder gameplay baseline (#15). |
| [Player defeat](PLAYER_DEFEAT_REPORT.md) | Failure return to Hub and encounter reset (#16). |
| [Wall occlusion](COMBAT_OCCLUSION_REPORT.md) | Shared static-wall line-of-sight gate for attacks (#17). |
| [Quest portal flow](QUEST_PORTAL_FLOW_REPORT.md) | Quest-gated portal admission and outcome handoff (#18). |
| [Class skills](FINAL_CLASS_SKILLS_REPORT.md) | First-version Archer/Mage/Katana skills and A/B ultimates (#19). |
| [Element reactions](ELEMENT_REACTION_COMPLETION_REPORT.md) | Ordered reaction table, statuses and ground fields (#20). |

These retain the design and test evidence from those development stages. Their old debug keys, missing-feature lists, test counts and
suggested handoff text are not current release instructions. Use the game README for
current controls and the feature reports above for inventory/persistence behavior.


## Delivered work and evidence

| Work | Traceability / validation boundary |
| --- | --- |
| Bag/equipment, item bonuses, potions, reward transfer (#47) | `21de2898`, merged through !38 / `b3ac158b`; original dated checks are retained in the inventory report. |
| Save foundations and complete gameplay integration (#48) | `82f6a450`, `c8d14bc9`, merged as `d47a1f6b`; original integration run recorded 1,165 core and 4 desktop tests passing. |
| Save UI refinement and persistence documentation (#48) | `534cb93`, merged through !41 as `12d6da8`. |
| Pause menu, save management/deletion, restart, volume, merchant potion supplies | `ec0a8fb` (2026-09-25). |
| Pause-menu improvements, hood/gloves/boots effects, Health capacity changes, +1/+2 equipment upgrades | `a4c7e9f`, `bbe814b` (2026-09-26); with `ec0a8fb`, merged through !42 as `ab76422`. Visual/manual acceptance of these screens is not recorded here. |
| Guardian signature mechanics (#46) and minimap Boss marker (#45) | Merged through !37 / `3ffda97` and !36 / `0dcb746`; measurements in the guardian traits report. |
| Wind Boss blink fix (#49) and P potion hotkey (#50) | !40 / `cabdce7`; `721f9e6` committed directly to main. |
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

**Recorded verification (2026-09-26, `ab76422`):** `./gradlew clean build javadocAudit` with JDK 25.0.4.1
passed **1,211 core tests and 4 desktop tests** (0 failures, 0 skipped) plus the JavaDoc audit. This is an
automated result only; it does not replace the outstanding manual/visual checks listed above.

## Keeping documentation current

When changing player behavior, update the game README and the relevant feature report.
When changing ownership, frame ordering or snapshot contracts, also update Architecture.
Record actual test commands/results separately from future regression instructions, and
add the real commit or merge reference once it exists. An Issue tracks scope and completion;
it complements these versioned project documents rather than replacing them.
