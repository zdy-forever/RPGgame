# COMP6120 — Group Fri11 — Assignment 3

## Project Summary

Our game is a small single-player action RPG built in Java with libGDX. The central Hub connects NPC services, equipment management, a Training Ground, five guardian arenas and a Memorial Hall. Players select Archer, Mage or Katana, accept a guardian quest, configure the Hub portal, and fight the selected Boss. Victory returns the player to the Hub with rewards and an automatic checkpoint. Defeating all five distinct guardians unlocks the Memorial Hall ending.

The game combines class-specific skills and elemental reactions with inventory, armour bonuses, material-based equipment upgrades and manual/automatic save slots. The Training Ground and in-game debug console support focused testing. Keyboard and mouse controls are provided for the main menus and item interfaces.

The engine separates gameplay models from desktop rendering. Java configuration objects define maps, encounters and class abilities; shared collision and combat systems enforce gameplay rules. Configuration changes still require rebuilding the Java project: this is not an external editor or a runtime scripting system.

## Team

| UID | Name | Role |
| --- | --- | --- |
| u7782042 | Daoyan Zhu | |
| u7998420 | | |
| u7851266 | Jiaqi Chen | |
| u8350466 | | |

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

These pages describe team `main` at merge commit `1a6ed22`, whose source tree includes `6d8b8a3` and the functional changes in `3d5638e`. They distinguish implemented behavior, dated test evidence and unfinished work. Repository names, Java packages, save paths and runner tags retain their existing `comp2120` identifiers; the coursework title on this Wiki is COMP6120.
