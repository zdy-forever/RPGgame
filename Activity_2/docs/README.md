# Project documentation

Updated 2026-09-26 for main at `ab76422` (pause menu, save management, merchant services and equipment upgrades).

## Where to start

| Document | Purpose |
| --- | --- |
| [Game README](../README.md) | Setup, launch, current controls, gameplay and module overview. |
| [Architecture](ARCHITECTURE.md) | Ownership, game-loop integration, persistence boundaries and contribution evidence. |
| [Inventory and equipment](INVENTORY_EQUIPMENT_REPORT.md) | Obtaining/equipping items, storage transfers, bonuses, potions, merchant supplies, +1/+2 upgrades and one-time rewards. |
| [Save and load](SAVE_LOAD_REPORT.md) | Esc menu, save management (load/delete), restart, volume, transient feedback, slot independence, safe-state rules and regression checks. |
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

**Current verification (2026-09-26, `ab76422`):** `./gradlew clean build javadocAudit` with JDK 25.0.4.1
passed **1,211 core tests and 4 desktop tests** (0 failures, 0 skipped) plus the JavaDoc audit. This is an
automated result only; it does not replace the outstanding manual/visual checks listed above.

## Keeping documentation current

When changing player behavior, update the game README and the relevant feature report.
When changing ownership, frame ordering or snapshot contracts, also update Architecture.
Record actual test commands/results separately from future regression instructions, and
add the real commit or merge reference once it exists. An Issue tracks scope and completion;
it complements these versioned project documents rather than replacing them.
