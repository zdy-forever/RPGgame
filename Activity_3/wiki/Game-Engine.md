# Game Engine

[Home](home) · [Features](features) · [Game Tester](game-tester)

## Modules and responsibilities

`core` contains game models and most automated tests. `lwjgl3` supplies the desktop launcher and runtime resources. `assets` contains artwork and audio. libGDX provides graphics, input and audio; our engine code implements world rules, combat, encounters, progression and persistence.

| Area | Responsibility / examples |
| --- | --- |
| World | `GameMap`, `MapCatalog`, collision boxes, spawn points and portal destinations |
| Player | Player state, classes, abilities and input commands |
| Combat | Attack/projectile settlement, health, faction checks, elements and status effects |
| Enemy | Enemy AI, pathfinding, two-phase Boss and guardian trait strategies |
| Progression | Encounter outcomes, quest acceptance/completion and NPC services |
| Inventory | Stack ownership, equipment, potions, shared-material upgrades |
| Save | Validated snapshots, independent slots and safe restoration |
| Screen | Gameplay coordination, modal input, rendering, camera, HUD and notifications |

## Configuration and reuse

Maps, class definitions and Boss configuration are Java data objects. For example, changing a class's configured skill tuning changes the values consumed by the shared ability system; changing a guardian trait alters its signature behavior while preserving the common Boss lifecycle. Rebuild after editing configuration. There is no graphical level editor or runtime configuration reload.

Five guardians share a Boss implementation with composed `BossTrait` strategies. Player and enemy attacks share collision, hostility and damage settlement. Equipment bonuses feed that settlement instead of implementing separate damage rules in the UI.

## Alternatives considered

Hard-coding content inside the game loop was rejected because every new map or enemy would modify coordination code. External JSON/YAML would separate data more strictly and is a common production approach, but typed Java configuration gives compile-time validation without requiring a parser, schema or editor in this project. A larger third-party engine was also considered unnecessary because libGDX already supplied the platform layer while the assignment required us to design and test our own gameplay architecture.

Typed configuration still has a cost: designers must edit Java and rebuild, and presentation assets may need renderer/asset work. We therefore describe it as a configuration-driven Java engine rather than claiming a no-code content editor.

## Simulation flow

The gameplay coordinator advances player abilities and movement, settles combat, processes deferred skill effects, then resolves encounter outcomes. Deferred handling prevents an attack callback from changing maps halfway through settlement. Success/failure cleans up old attacks and returns the player through the existing world-travel boundary.

Movement and dash use collision queries against map geometry. Projectiles use swept collision; fixed attacks and reaction areas require line of sight. Map changes clear map-local attack/status activity so effects do not follow the player into Hub.

Modal interfaces consume their input before movement/skill sampling and pause simulation. NPC-triggered travel synchronizes map bindings without advancing combat during a dialogue frame.

## Persistence boundaries

Snapshots include player state, class choices, cooldowns, location, inventory, chest, equipment, quest progress and relevant NPC/ending flags. Loading validates data before replacing live state; derived equipment bonuses are recomputed. Active combat is excluded by save-location/activity rules. Debug switches are session-only and using them blocks subsequent save writes for that live game.

## Engine examples and limitations

The same damage pipeline supports a player arrow, a Boss bolt, an elemental field and the Training target's upward shot. Training changes outcome rules rather than introducing a separate combat formula. Tests can advance the model with commands and controlled time without creating the desktop window.

The current campaign uses predefined arenas and one Boss per battle. It does not provide procedural dungeon generation, multiplayer, achievements, a general crafting system or a scripting editor.

For implementation details and historical design decisions, see the repository's [architecture document](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/main/Activity_2/docs/ARCHITECTURE.md) and [report index](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/main/Activity_2/docs/README.md). Dated sections describe their recorded development stage.
