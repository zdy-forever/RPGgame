# COMP6120 — Group Fri11 — Assignment 3

## Project Summary

Our game is a small single-player action RPG built in Java with libGDX. The central Hub connects NPC services, equipment management, a Training Ground, five guardian arenas and a Memorial Hall. Players select Archer, Mage or Katana, accept a guardian quest, configure the Hub portal, and fight the selected Boss. Victory returns the player to the Hub with rewards and an automatic checkpoint. Defeating all five distinct guardians unlocks the Memorial Hall ending.

The game combines class-specific skills and elemental reactions with inventory, armour bonuses, material-based equipment upgrades and manual/automatic save slots. The title menu offers New Game, Continue, Settings and Quit. The Training Ground and in-game debug console support focused manual testing, while the Gradle-run JUnit suite automatically sends gameplay commands and checks resulting states/events. Keyboard and mouse controls are provided for the main menus and item interfaces.

The engine separates gameplay models from desktop rendering. Java configuration objects define maps, encounters and class abilities; shared collision and combat systems enforce gameplay rules. Configuration changes still require rebuilding the Java project: this is not an external editor or a runtime scripting system.

## Team

| UID | Name |
| --- | --- |
| u7782042 | Daoyan Zhu |
| u7998420 | Hao Liu |
| u7851266 | Jiaqi Chen |
| u8350466 | Not in the active development allocation recorded in Architecture |

See [Planning and Schedule](Planning-and-Schedule) for delivered team responsibilities, milestone dates and traceable implementation evidence.

## Technology

| Component | Version / use |
| --- | --- |
| Java | JDK 25 build toolchain; Java 17 bytecode target |
| libGDX | 1.14.2; LWJGL3 desktop backend |
| LWJGL | 3.4.1 dependency constraints |
| Gradle | 9.7.1 through the included wrapper |
| JUnit | Jupiter 5.13.4 |
| GitLab CI | Build, tests, strict JavaDoc audit and JaCoCo coverage |

Docker is not required to play or test locally. The [Docker page](Docker) explains the CI image declaration and the `Activity_4` container deliverable, including what is still unverified.

## Quick Links

- [Installation and Setup](installation-and-setup)
- [Features](features)
- [Playtest and Screenshots](Playtest-and-Screenshots)
- [Documentation Index and References](Documentation-Index)
- [Game Engine](game-engine)
- [Game Tester](game-tester)
- [Activity 3.3: tests and coverage report](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/64310bfa5869a213c6e9025bc6eff2d45effcc3e/Activity_3/3.3/REPORT.md)
- [User Manual](User-Manual)
- [User Stories](User-Stories)
- [Planning and Schedule](Planning-and-Schedule)
- [License](License)
- [Docker](Docker)
- [Known Issues and Future Work](Known-Issues-and-Future-Work)
- [Reflection](Reflection)

## Documentation baseline

Reviewed on **3 October 2026** against team `main` commit [64310bf](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/commit/64310bfa5869a213c6e9025bc6eff2d45effcc3e), including Activity 3.3 tests/coverage, the merged JavaDoc review and warning cleanup, Activity 2 documentation, 41 native screenshots and the Training HUD fix. Repository references below are pinned to this revision so later changes do not silently change the evidence.

Activity 2 feature reports describe the reviewed implementation. Dated test records retain their original execution scope: the campaign captures used `1ae3c78`; screenshots 40–41 captured the Training fix later committed in `626e88e` and merged into this baseline. See [Playtest and Screenshots](Playtest-and-Screenshots) for provenance and limits. The repository/package identifiers remain `comp2120`; the coursework title is COMP6120.

## Current automated verification

[Pipeline #1052939](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/pipelines/1052939) for `64310bf` passed. The downloaded Gradle reports contain **1,449 core + 4 desktop tests**, with zero failures or skipped tests; the CI test summary reports zero errors. Both modules' JavaDoc audits also passed. Core JaCoCo coverage is **97.61% lines, 88.61% branches and 97.50% instructions**. [Game Tester](game-tester) provides the latest test results, exact coverage counters, job artifacts and reporting notes.

## Repository documentation

| Start here | What it supports |
| --- | --- |
| [Documentation index](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/64310bfa5869a213c6e9025bc6eff2d45effcc3e/Activity_2/docs/README.md) | Implementation reports and their dated verification boundaries |
| [Game README](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/64310bfa5869a213c6e9025bc6eff2d45effcc3e/Activity_2/README.md) | Setup, controls, gameplay and module overview |
| [Architecture](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/64310bfa5869a213c6e9025bc6eff2d45effcc3e/Activity_2/docs/ARCHITECTURE.md) | Engine boundaries, original planning and delivered contribution records |
| [Test coverage review](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/64310bfa5869a213c6e9025bc6eff2d45effcc3e/Activity_2/docs/TEST_COVERAGE_REVIEW.md) | Automated scenario map, recorded execution status and remaining native QA |

The [Documentation Index](Documentation-Index) maps every Activity 2 report to the relevant Wiki page and assignment requirement. Build/run/test instructions are on [Installation and Setup](installation-and-setup); gameplay steps are on [User Manual](User-Manual).

## Game preview

![Memorial Hall after five distinct guardian victories; the ending and all Credits pages are documented in the playtest gallery.](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/raw/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/screenshots/29-memorial-hall-five-stones.jpg)

Memorial Hall after five distinct guardian victories; the ending and all Credits pages are documented in the playtest gallery.
