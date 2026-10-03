# Playtest and Screenshots

[Home](home) · [Features](features) · [Game Tester](game-tester) · [Documentation Index](Documentation-Index)

## Evidence and provenance

Native desktop session: **3 October 2026, Australia/Sydney**, macOS 27.0.1 ARM64, JDK 25.0.4.1. Screenshots 01–39 used game code `1ae3c78`. Screenshots 40–41 used the Training fix later committed as `626e88e`, now merged into `59de4e6`. The temporary app wrapper used current compiled classes/assets and isolated test saves; it did not validate the release ZIP.

NPC/portal approaches used **position-only save fixtures loaded through F9** because instantaneous tool WASD input was unreliable. No Boss HP, victories, rewards or ending flags were edited; no cheat features were used. Fire, Water, Lightning and Ice were won with ordinary Mage attacks. **The user took over and won Wind normally, explicitly confirming no debug use.** Its final fight was not fully observed and its transient victory banner was not captured. This is a combined native test session, not an uninterrupted walking campaign.

[Full steps, expected/actual results and limitations](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/PLAYTEST_REPORT.md) · [Screenshot index (all 41 captures)](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/screenshots/README.md) · [SHA-256 manifest](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/screenshots/SHA256SUMS.txt)

## Five guardians and victory settlement

Fire's checkpoint and Water/Lightning/Ice victory captures are available in the full index (16, 22–24). The Wind encounter screenshot below shows a failed attempt; the next capture records the resulting five-win automatic save after the user's successful retry.

![Wind encounter before the subsequent user-operated victory.](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/raw/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/screenshots/25-wind-battle.jpg)

Wind encounter before the subsequent user-operated victory.

![11:53:32 AEST AUTO: five distinct guardians complete. The older MANUAL remains at four; five cores were also verified in the saved state.](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/raw/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/screenshots/27-five-guardians-autosave.jpg)

11:53:32 AEST AUTO: five distinct guardians complete. The older MANUAL remains at four; five cores were also verified in the saved state.

## Memorial Hall and ending

After five wins, Hub Sage guidance directs the player to Portal Guide. Selecting Memorial Hall and entering the real portal reveals all five stones. Speaking to the Hall Sage advances three ending lines, then journey-completion feedback and the Credits choice.


![Hub Sage completion guidance.](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/raw/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/screenshots/28-hub-completion-guidance.jpg)

Hub Sage completion guidance.

![Memorial Hall: all five guardian stones lit; objectives and minimap hidden.](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/raw/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/screenshots/29-memorial-hall-five-stones.jpg)

Memorial Hall: all five guardian stones lit; objectives and minimap hidden.

![Ending dialogue 1: the guardians have fallen.](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/raw/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/screenshots/30-ending-guardians-fallen.jpg)

Ending dialogue 1: the guardians have fallen.

![Ending dialogue 2: the seal is restored.](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/raw/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/screenshots/31-ending-seal-restored.jpg)

Ending dialogue 2: the seal is restored.

![Ending dialogue 3: the journey is complete.](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/raw/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/screenshots/32-ending-journey-complete.jpg)

Ending dialogue 3: the journey is complete.

![Actual all-five journey-completion message and View credits choice.](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/raw/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/screenshots/33-credits-choice.jpg)

Actual all-five journey-completion message and View credits choice.

## Credits — all five pages

![Credits: Special Thanks.](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/raw/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/screenshots/34-credits-special-thanks.jpg)

Credits: Special Thanks.

![Credits: Development Team.](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/raw/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/screenshots/35-credits-development-team.jpg)

Credits: Development Team.

![Credits: Art and Audio.](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/raw/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/screenshots/36-credits-art-audio.jpg)

Credits: Art and Audio.

![Credits: Tools.](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/raw/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/screenshots/37-credits-tools.jpg)

Credits: Tools.

![Credits: Thank You.](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/raw/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/screenshots/38-credits-thank-you.jpg)

Credits: Thank You.

## Ending persistence

The 11:58:16 AEST manual save recorded Memorial Hall, all five wins and `quest.ending=true`; F9 restored that file. AUTO remained the earlier five-win Hub checkpoint.

![Hall restored after completing Credits, saving and loading MANUAL.](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/raw/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/screenshots/39-hall-ending-restored.jpg)

Hall restored after completing Credits, saving and loading MANUAL.

## Title, settings and Training

![Fresh launch: title menu with Continue unavailable before any save.](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/raw/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/screenshots/01-title.jpg)

Fresh launch: title menu with Continue unavailable before any save.

![Settings retained volume 52 and fullscreen Off after application relaunch.](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/raw/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/screenshots/18-settings-after-relaunch.jpg)

Settings retained volume 52 and fullscreen Off after application relaunch.

![After the Training fix: wrapped Objectives and every practice information line remain separate.](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/raw/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/screenshots/41-training-layout-fixed-resized.jpg)

After the Training fix: wrapped Objectives and every practice information line remain separate.

Historical screenshots 20–21 show the original overlap; they are not fixed-build evidence. The correction passed fresh direct-JDK compilation, 83 selected JUnit tests and strict JavaDoc locally, followed by the full merged-main CI recorded on [Game Tester](game-tester).

## Remaining checks

Physical walking/aiming/mouse input, all class/guardian combinations, detailed Training damage/recovery, successful upgrade UI, destructive/corrupt save UI, audio listening, exact 640 × 360/HiDPI layout and release distribution launch remain outside the completed acceptance scope. See [Known Issues](Known-Issues-and-Future-Work) for observed defects and the full report for the remaining checklist.
