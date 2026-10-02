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
| GitLab CI | Build, tests and strict JavaDoc audit |

Docker is not required to play or test locally. The [Docker page](Docker) explains the CI image declaration and the limits of project-level container support.

## Quick Links

- [Installation and Setup](installation-and-setup)
- [Features](features)
- [Game Engine](game-engine)
- [Game Tester](game-tester)
- [User Manual](User-Manual)
- [User Stories](User-Stories)
- [Planning and Schedule](Planning-and-Schedule)
- [License](License)
- [Docker](Docker)
- [Known Issues and Future Work](Known-Issues-and-Future-Work)
- [Reflection](Reflection)

## Documentation baseline

Reviewed on **3 October 2026** against team `main` commit [`7fea86a`](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/commit/7fea86a83d8eae720650bed84511ad3c989f7c1b). The delivered `Activity_2` implementation is pinned in detailed source references at `1a2c60c`; later commits update documentation and Activity 0 material without changing that implementation baseline. Fixed commit links keep the reviewed contents inspectable even after later branch work.

The original Wiki baseline was `1a6ed22`. Dated implementation reports retain historical test results and may describe earlier controls; use the updated Installation, User Manual and Game Tester pages for current instructions. A source review is not a fresh passing test run or visual acceptance. Repository names, Java packages, save paths and runner tags retain their existing `comp2120` identifiers; the coursework title on this Wiki is COMP6120.

## Current automated verification

[Pipeline #1052111](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/pipelines/1052111) for `7fea86a` passed **1,302 tests, with zero failures, errors or skipped tests**. The build job also passed both modules' JavaDoc audits. [Game Tester](game-tester) provides the exact command, test scenarios, job log, report screenshot and limits of this evidence.

## Repository documentation

| Start here | What it supports |
| --- | --- |
| [Documentation index](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/7fea86a83d8eae720650bed84511ad3c989f7c1b/Activity_2/docs/README.md) | Implementation reports and their dated verification boundaries |
| [Game README](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/7fea86a83d8eae720650bed84511ad3c989f7c1b/Activity_2/README.md) | Setup, controls, gameplay and module overview |
| [Architecture](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/7fea86a83d8eae720650bed84511ad3c989f7c1b/Activity_2/docs/ARCHITECTURE.md) | Engine boundaries, original planning and delivered contribution records |
| [Test coverage review](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/7fea86a83d8eae720650bed84511ad3c989f7c1b/Activity_2/docs/TEST_COVERAGE_REVIEW.md) | Automated scenario map, recorded execution status and remaining native QA |

Feature-specific references are provided on Features, Game Engine, Game Tester and User Manual. The six early integration/combat/class/element reports are historical design evidence, not replacements for current controls.
