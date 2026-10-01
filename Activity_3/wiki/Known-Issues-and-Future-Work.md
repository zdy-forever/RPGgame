# Known Issues and Future Work

[Home](home) · [Game Tester](game-tester)

## Verification and delivery limits

- Window resizing, platform key delivery, pointer hit areas and visual overlap require the manual checks documented in Game Tester. Model tests do not verify the rendered desktop.
- The recorded test result is dated. Inspect the submitted commit's pipeline for current pass/fail status.
- No line/branch coverage percentage has been measured in the inspected configuration.
- Automatic gameplay verification is exposed through JUnit scenarios; there is no separate standalone tester command language.
- A project Dockerfile and validated graphical container workflow are not present in this baseline.
- The root license's final sentence needs an editorial repair, and current-build screenshots still need to be uploaded to the Wiki.

These are explicit evidence/delivery gaps, not assertions that all gameplay is broken or that every untested behavior is correct.

## Planned scope that changed

The architecture's scope decisions record achievements and following NPCs as dropped. General crafting/forging/enchanting was reduced to material-based merchant upgrades. Guided onboarding was reduced to tutorial NPC and Notice Board guidance. The campaign uses predefined maps and one Boss per arena; procedural generation and ordinary enemy populations are not implemented campaign features.

## Current design limits

- Equipment is not profession-restricted; the staff's equipment bonus does not replace each class's skill set.
- Upgrades stop at +2 and use a shared material. Balance remains provisional.
- Music gain is not saved across application launches.
- Restart preserves slots; a later save or victory may replace the corresponding older timeline.
- Debug use disables saving for the live session, even after switches are turned off. Restart to return to a clean session.
- Legacy weapon/coordinate rendering code is retained with disabled calls.

## Future improvements

Priorities are measured test coverage, a documented standalone tester entry point if required, completed packaging evidence, and broader manual platform checks. Product improvements could include stronger onboarding, more equipment/art variety, persistent audio preferences and further guardian balance testing. These are proposals, not delivered features.
