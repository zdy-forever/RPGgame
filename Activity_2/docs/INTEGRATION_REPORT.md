# Gameplay integration

Reviewed against `7fea86a` on 2026-10-02. This report describes the current production loop.
It replaces the old placeholder-only walkthrough and removed prototype controls.

## Startup, configuration and packaging

[Lwjgl3Launcher](../lwjgl3/src/main/java/au/edu/anu/comp2120/lwjgl3/Lwjgl3Launcher.java) starts
MainGame, which applies the stored fullscreen preference and opens MainMenuScreen. New Game
constructs fresh GameScreen state; Continue selects the newest readable save and attempts load;
Settings stores fullscreen/music preferences; Quit exits.

The game uses JDK 25, Gradle wrapper 9.7.1, LibGDX 1.14.2 and LWJGL3 3.4.1, with Java 17
compilation target. Gradle supplies native-access and macOS first-thread flags. The desktop
module packages `assets/` on the runtime classpath. Generated distributions include launcher
scripts and library dependencies. Use [README setup/build instructions](../README.md#install-and-run)
for an IDE, command line or packaged install.

Gameplay definitions are Java catalogues: MapCatalog/TravelCatalog for world and routes,
GameConfig for integration tuning/NPCs/shared Boss, PlayerClasses/SkillTuning for abilities,
ElementRules/ReactionTuning for reactions, BossTraits/BossScaling for guardians and ItemCatalog
for equipment. Change these and rebuild; there is no external map/JSON loader.

## Production path

| Stage | Integration contract |
| --- | --- |
| Fresh Hub | Mage, empty bag, starter chest, closed configurable Hub portal, fresh NPC/quest state |
| NPC services | Dialogue and menus adapt class/O selection, one-time staff, potion supply, upgrades and storage |
| Quest | BossQuests prepares one exact EncounterFlow permission, leaving portal closed |
| Guide | Opens Battle for that permission or reroutes to quest-free Training/Hall |
| Arrival | Walking into portal switches via TravelSystem; transition stamp resets the old combat scope and prevents bounce |
| Training | One immortal stationary target, random per-visit art, upward test shots and recovery/score; no campaign reward |
| Battle | One guardian, shared combat/AI/skills, elemental reactions, signature trait and rematch scaling |
| Outcome | Failure-first settlement; immediate full-HP Hub return; only success yields one reward/progress/checkpoint |
| Hall | Five distinct stones, explicit Sage ending interaction and optional credits |
| Persistence | Safe-state manual slot and post-victory automatic slot, explicit management/Continue and independent settings |

Internal demo constructors and direct travel calls used by unit fixtures are not alternate
production controls. No U or numeric-key travel UI is wired into GameScreen.

## Input and frame ownership

[GameScreen](../core/src/main/java/au/edu/anu/comp2120/screen/GameScreen.java) owns keyboard/
mouse routing and the modal panels. A console/menu/dialogue/item/map frame returns before
normal player command sampling. Opening/closing a panel consumes input. Gameplay simulation
and world animation pause together; feedback timers and menu music preview remain independent.

[CombatDemo](../core/src/main/java/au/edu/anu/comp2120/screen/CombatDemo.java) coordinates
PlayerAbilities, Boss decisions/Training shots, one CombatSystem settlement and deferred skill
responses. Outcomes are processed outside damage callbacks. Portal transitions immediately
rebind the scope; paused dialogue travel synchronizes the map without advancing combat.

After gameplay, GameScreen collects rewards, reconciles equipment, synchronizes quests and
requests automatic saving before music/rendering observe the result. Frame delta is capped;
creation/resume skips the first simulation update to avoid a jump.

## Persistent versus transient state

Travel preserves Player/Health/bag/equipment, class/O/element and remaining cooldowns. It clears
active skills, attacks, status/fields, old target/AI references and scene effects. Full load
validates component snapshots first and restores the chosen timeline without merging or writing
slots. New Game/restart creates fresh live state while retaining both files.

Title Settings persists volume/fullscreen under the user home. Esc volume changes only current
BackgroundMusic gain and carries through in-game restart. Music streams belong to the screen,
not maps; selecting the same track does not restart it. Lifecycle pause pauses audio; modal
pause leaves it playing. Render resources are disposed with their owning screen.

## Automated and manual verification

From Activity_2:

```sh
./gradlew clean build javadocAudit --no-daemon
```

The [tester guide](TEST_COVERAGE_REVIEW.md) contains scenario commands, expected state and the
verified 1,302-pass CI result at `1a2c60c`. Production encounter scenarios use the real admission,
movement, combat and outcome coordinators; isolated unrestricted component fixtures are labelled
separately. Tests cover resources/model contracts but do not create a full native campaign recording.

The [2026-10-03 native report](PLAYTEST_REPORT.md) records actual startup, settings/fullscreen,
manual persistence, inventory/NPC/portal integration, five guardian wins, victory checkpoints,
application relaunch/Continue, Training score/reset, Hall ending and all Credits pages, with screenshots. It uses a temporary
app wrapper and disclosed position-only fixtures. Remaining checks include mouse/held input,
resize matrix, all class/Boss interactions, audio listening and release-package startup.
This partial run does not establish full campaign acceptance.

See [Architecture](ARCHITECTURE.md) for ownership and
[scope decisions](../README.md#scope-decisions-and-remaining-limits) for non-core reductions.
