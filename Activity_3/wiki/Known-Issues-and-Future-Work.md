# Known Issues and Future Work

[Home](home) · [Game Tester](game-tester)

## Verification and delivery limits

- Window resizing, platform key delivery, pointer hit areas and visual overlap require the manual checks documented in Game Tester. Model tests do not verify the rendered desktop.
- Current-source automated evidence is available: [pipeline #1052436](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/pipelines/1052436) for `59de4e6` passed 1,302 tests with zero failures, errors or skips, plus build/JavaDoc checks. See [Game Tester](game-tester) for the report screenshot and job log. Any later submitted commit needs its own pipeline check.
- No line/branch coverage percentage has been measured in the inspected configuration.
- The automatic tester is the JUnit command/action-to-state suite described in [Game Tester](game-tester). It exercises the assignment's examples without requiring a separate interactive command language; rendered desktop behavior still requires manual checks.
- A project Dockerfile and validated graphical container workflow are not present in this baseline.
- [Playtest and Screenshots](Playtest-and-Screenshots) now includes title/settings, combat, five-win saves, Hall ending, Credits and fixed Training layout. All 41 native captures are indexed in Activity 2; a debug-console screenshot and wider platform/input/audio acceptance remain outstanding.
- The restricted coursework license and its corrected final sentence are present in the latest team `main`. The final submission should retain that `LICENSE.md` unchanged unless the team deliberately revises its terms.

These are explicit evidence/delivery gaps, not assertions that all gameplay is broken or that every untested behavior is correct.

## Native findings from 3 October

- **Fixed:** Objectives overlapped Training information. The merged layout now positions Training below the measured wrapped objective; screenshots 40–41 show the correction at the tested window sizes.
- **Open:** the native title bar still advertises obsolete `[F5 Hub]` help although F5 saves.
- **Needs balance review:** Mage O damaged an off-screen idle Fire Sentinel from the entry area; that victory does not establish encounter difficulty or telegraph/dodge quality.
- **Open presentation issue:** unavailable-slot diagnostics containing long paths are very small.
- **Not established:** physical held-key movement and mouse dragging were inconclusive through the desktop tool. Broader classes/traits, audio, exact 640 × 360/HiDPI layout and packaged distribution acceptance remain to be checked.

See [the native playtest report](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/PLAYTEST_REPORT.md) for observed versus untested behavior. No uninterrupted walking campaign or exhaustive manual regression pass is claimed.

## Planned scope that changed

The following reasons were confirmed by the team on 2 October 2026. These features were outside the core playable loop, so we prioritized completing and integrating movement, combat, quests, equipment and saving.

| Original plan | Delivered scope | Why it was cancelled or reduced |
| --- | --- | --- |
| Achievement definitions, progress and unlock events | Not implemented | Achievements were not a core gameplay feature and depended on save/load to retain progress. Save/load was completed relatively late, leaving insufficient time to integrate and verify the dependent achievement system. |
| NPC movement and player following | Stationary NPC interactions and services | Following behavior was not required for the core quest and combat loop, so it was dropped from the delivery scope. |
| General crafting, forging and enchanting | Material-based merchant upgrades, capped at +2 | A general crafting system was outside the core loop. Merchant upgrades provided the required equipment progression within the available scope. |
| Guided tutorial sequence | Tutorial NPC and Notice Board instructions | A scripted tutorial was not a core feature. We retained practical guidance through existing NPC/dialogue interfaces. |

The campaign uses predefined maps and one Boss per arena. Procedural generation and ordinary enemy populations are not implemented campaign features. This describes current limits, rather than claiming those items were all promised or cancelled for the same reason.

## Current design limits

- Equipment is not profession-restricted; the staff's equipment bonus does not replace each class's skill set.
- Upgrades stop at +2 and use a shared material. Balance remains provisional.
- Title-screen Settings persists music volume and fullscreen preferences in `.comp2120-game/settings.properties`. The in-game Esc volume slider changes the current music session; its callback does not persist that change to the settings file.
- Restart preserves slots; a later save or victory may replace the corresponding older timeline.
- Debug use disables saving for the live session, even after switches are turned off. Restart to return to a clean session.
- Legacy weapon/coordinate drawing implementations were removed in the 1 October cleanup. Inspect equipment in the bag; the refreshed Activity 2 reports reflect the current implementation.

## Future improvements

Priorities are keeping test evidence aligned with the final submission commit, measured test coverage, completed packaging acceptance and broader manual platform checks. Product improvements could include stronger onboarding, more equipment/art variety, consistent persistence of in-game volume changes and further guardian balance testing. These are proposals, not delivered features.

## Supporting records

- [Architecture: original plan and delivered status](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/ARCHITECTURE.md). The original smoke-tester row is historical planning; the current automated verification entry point is documented in Game Tester.
- [Save/load scope and limits](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/SAVE_LOAD_REPORT.md).
- [Test review and 1 October cleanup](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/TEST_COVERAGE_REVIEW.md).
- [Persisted title-menu settings](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/core/src/main/java/au/edu/anu/comp2120/screen/GameSettings.java) and [gameplay volume callback](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/core/src/main/java/au/edu/anu/comp2120/screen/GameScreen.java).
