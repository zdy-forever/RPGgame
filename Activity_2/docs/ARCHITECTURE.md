# Current architecture

Reviewed against `7fea86a` on 2026-10-02. [Setup and controls](../README.md) are maintained in
the game README; [automated verification](TEST_COVERAGE_REVIEW.md) records execution evidence.

## Modules and ownership

`core` is a Java library containing domain logic, LibGDX screens and graphics-free tests.
`lwjgl3` is the desktop application and packages the shared assets. Being in `core` does
not imply that every class is graphics-free: screen constructors may allocate native resources.

All production Java packages below are under `au.edu.anu.comp2120`.

| Owner | Responsibility |
| --- | --- |
| `MainGame` | Title-screen startup, screen replacement and deferred fresh-game restart |
| `GameSession` | Map catalogue, current map, persistent Player and transition version |
| `TravelSystem` | Portal routes/enabled state, named-spawn travel, unlocks and arrival protection |
| `EncounterFlow`, `ActiveEncounter`, `BattleResult` | Admission identity, result settlement, replay protection, victory counts and reward outbox |
| `player` | Player health/bag/equipment, input intents, class definitions, abilities and per-player debug state |
| `combat` | Shared damage delivery, attacks, projectiles, element rules, statuses, fields and hit observations |
| `world` | Validated map/spawn geometry, collision/visibility, portal definitions and A* pathfinding |
| `enemy` | Enemy navigation/state, Boss phases/traits/scaling and immortal TrainingDummy |
| `npc` | NPC interaction data and adapters for class, portal and merchant services |
| `quest/BossQuests` | One accepted target, five distinct completions, Hall guidance and one-time ending |
| `inventory` | Ordered stacks, equipment catalogue/modifiers, potion stock, upgrades and reward collection |
| `save` | Bounded/versioned properties files, component snapshots, validation and full restoration |
| `screen` | Gameplay wiring, input ownership, title/settings/save menus, rendering and HUD |
| `audio/BackgroundMusic` | Screen-lifetime streamed music, cached tracks, gain and lifecycle pause |

Primary integration files: [GameScreen](../core/src/main/java/au/edu/anu/comp2120/screen/GameScreen.java),
[CombatDemo](../core/src/main/java/au/edu/anu/comp2120/screen/CombatDemo.java),
[GameSaves](../core/src/main/java/au/edu/anu/comp2120/save/GameSaves.java).
Despite their retained names, `CombatDemo` coordinates production encounters and
`PlaceholderRenderer` delegates to the current imported-art renderer.

## State and dependency boundaries

GameSession owns one Player across ordinary travel, victory, defeat and load. Player owns
Health, bag, Equipment, class identity, per-class ultimate choices, Mage element and DebugOptions.
PlayerAbilities composes ClassSkills and per-slot timers; immutable ClassDefinition/AbilityLoadout
values select behavior without separate Player subclasses.

Combatant adapts a Damageable entity to faction, hurtbox, modifiers, targetability, status and
optional hit interception. CombatSystem settles all ordinary attack/projectile/reaction damage.
Boss composes EnemyAI and a fresh BossTrait; it does not implement a parallel damage engine.
Inventory exposes modifiers to the existing settlement and player movement/cooldown paths.

Quest/NPC code uses EncounterFlow for admission and outcomes. InventoryRewards drains the
outbox only after validating stack capacity. Persistence reads explicit component snapshots
and restores through public APIs, without reflection or Java object-graph serialization.
Screen/art/audio code observes gameplay; textures and sprite dimensions never define collision
or damage. Tests drive these same domain coordinators without constructing the rendered screen.

## Frame order and modal input

1. GameScreen checks lifecycle pause, skips the first update after creation/resume and caps
   gameplay delta at 0.1 seconds. Console, menus, dialogue and item/map panels own input and
   return before movement/skill sampling. Opening/closing frames are consumed.
2. Before ordinary play, settled Battle death/success is handled. Player death has priority.
3. PlayerAbilities processes movement and commands. Boss decisions and Training shots feed
   the same combat engine; Training recovery advances in simulation time.
4. CombatSystem settles damage once. `PlayerAbilities.afterCombat()` handles interruption,
   confirmed thrust and deferred counters. Battle outcomes are checked again after settlement.
5. Portal overlap may transition maps; the new scope synchronizes immediately without carrying
   old attacks or consuming a second movement command.
6. GameScreen collects pending rewards, reconciles equipment, synchronizes quests, then invokes
   victory autosave. Music selection and rendering observe this settled state.

Modal panels pause world simulation and animation; save/outcome feedback uses UI timing.
Dialogue-triggered travel calls `synchronizePausedMap()` to rebind without simulating combat.
Application pause also pauses music; an in-game menu leaves it playing for volume preview.

## World, movement and camera

MapCatalog defines eight validated maps in Java: Hub 1080 × 400, Memorial Hall 640 × 360,
and Training/five guardian arenas 1600 × 1200. Coordinates are bottom-left world units.
Current interiors have no obstacle rectangles. Outer bounds still block movement, while
explicit obstacle fixtures exercise wall behavior in automated tests.

The Player body is 28 × 28 with base speed 180; hurtbox inset is separately configured.
WASD diagonals normalize. Walking, dash, knockback and thrust use collision-safe displacement.
Movement resolves X then Y with swept checks: neither axis tunnels, but large diagonal steps
can route differently around corners from multiple smaller steps. Dynamic body-to-body collision
is not implemented. EnemyAI uses direct pursuit where clear and bounded A* navigation where needed;
the campaign spawns one Boss per Battle and no ordinary mob population.

CameraFollow supplies pure follow/clamp math to a 640 × 360 FitViewport. Rendering restores
the full framebuffer viewport for screen-space HUD. Sprite artwork and map border decoration
remain separate from walkable bounds.

See [collision/occlusion](COMBAT_OCCLUSION_REPORT.md) for fixed attacks and projectiles.

## Combat and classes

J/K/L/O commands are independent of class identity. Archer uses arrows, a fan, rain or
untargetability; Mage uses selected-element bolts, element cycling, a prepared field or rapid
random-element bolts; Katana uses slashes, parry/counter, stance or thrust/follow-up.
Class Master selects class/O mode in Hub. Choices survive travel and are saved.

Health validates integer HP operations. Shared combat checks eligibility, geometry, visibility,
invulnerability/interception and modifiers before delivery. Attacks track per-target hits;
projectiles use swept collision and walls win equal-time ties. ElementRules defines ordered
reactions. Derived effects are bounded to one generation and cannot recursively reattach/react.
Status and field timers advance in simulation time and clear with the encounter scope.

Class/element/ultimate selection does not refresh existing cooldowns. Travel clears active
casts, dash, attacks, fields, targets and defensive hooks while retaining slot cooldowns and
persistent choices. [Skills](FINAL_CLASS_SKILLS_REPORT.md), [reactions](ELEMENT_REACTION_COMPLETION_REPORT.md)
and [Boss traits](BOSS_TRAIT_REPORT.md) contain the specific behavior and tuning sources.

## Quest, travel and outcomes

Production uses one configurable Hub portal plus the Hall return portal. Quest acceptance
prepares an exact permission and closes the Hub portal; Portal Guide explicitly activates
Battle or reroutes to Training/Hall. Walking into the trigger performs travel. Training/Hall
visits retain the accepted quest. Arrival protection requires leaving a trigger before re-entry.
The fast-travel API exists for internal adapters/tests; it is not a keyboard fast-travel menu.

A successful admitted battle returns/heals the Player, records one result and pending reward,
closes admission and increments that guardian's win count. Quest synchronization marks distinct
completion. Failure returns/heals without reward, retains the acceptance for retry and does not
autosave. Repeating the same completed acceptance cannot duplicate a reward. Training has no
campaign outcome. After five distinct wins, the Hall Sage consumes the ending once; optional
credits are presented after the dialogue. Rematches do not add extra distinct stones.

See [quest/portal flow](QUEST_PORTAL_FLOW_REPORT.md) and [defeat](PLAYER_DEFEAT_REPORT.md).

## Inventory and persistence

Equipment refers to owned bag item IDs; worn items remain counted in the bag. Reconciliation
removes a worn reference when its last carried unit leaves. Chest is a separate Inventory.
Gear applies attack/defence, maximum HP, K/L/O cooldown and ordinary movement modifiers. Upgrades
replace one carried item with its +1/+2 identity, retaining its worn slot. Potion stock services
count bag and chest, while potion use consumes only the bag.

GameSaves coordinates exact safe-map location/HP, class/O/element/cooldowns, both inventories,
equipment, quest/ending/guidance flags, NPC interaction flags, admission/reward replay state,
win counts and travel state. It validates before live mutation. SaveStore/SaveFile use bounded
versioned properties and atomic replacement; unsupported atomic moves fail without non-atomic
fallback. Manual and automatic slots are independent.

Saving/loading/deleting requires living idle Hub/Hall state. Active combat, skill effects,
Training metrics, RNG and debug switches are excluded. Cheat use blocks writes for that live
session even after loading or disabling switches. New Game/restart creates fresh state while
leaving slot files intact. Continue explicitly chooses the newest readable slot on the title menu.

GameSettings is separate from snapshots: title Settings and Esc → Settings share the same
persisted volume/fullscreen values beside the save directory and apply changes immediately.
The pause menu also provides a confirmed application-exit callback; quitting does not modify
either save slot. See [save/load](SAVE_LOAD_REPORT.md).

## Presentation and assets

ArtAssets owns lazy nearest-filtered textures and strip frames; SceneArt selects pictures/VFX.
CombatSystem's bounded HitFeedback mailbox supplies settled observations without causing damage.
Travel clears old scene effects. StatusLabels displays control/damage states above actors.
A non-blocking three-second outcome banner survives the immediate Hub return without retaining a Boss.

HUD includes HP, objectives below HP, skill/element/potion slots, minimap, dialogue, inventories
and menus. Detailed HUD hides below 640 × 360; health has a separate threshold. Memorial Hall
hides minimap and objective panel, showing stones and the Sage. No coordinate or top-right weapon
HUD remains. BackgroundMusic owns three streamed tracks and is disposed with its screen.
See [art](ART_INTEGRATION.md) and [debug/Training/UI](DEBUG_TRAINING_UI_REPORT.md).

## Configuration and extension

| Change | Source of truth |
| --- | --- |
| Map bounds, obstacle rectangles, spawns | `world/MapCatalog`, `GameMap` |
| Portals and internal destination definitions | `world/TravelCatalog` |
| Player size/speed, camera, NPC placements, shared Boss template | `screen/GameConfig` |
| Class damage/cooldowns and execution timing | `player/PlayerClasses`, `SkillTuning` |
| Reactions and status/field tuning | `combat/ElementRules`, `ReactionTuning` |
| Guardian mechanics and rematches | `enemy/BossTraits`, `BossScaling` |
| Items, potion stock and upgrade recipes | `inventory/ItemCatalog`, `PotionSupply`, `GearUpgrades` |
| User settings | `screen/GameSettings` and user-home properties file |

Gameplay catalogues are Java configuration and require rebuilding. No external JSON/map loader
is shipped. New adapters must bind the correct world, preserve encounter identities and use the
shared damage/reward paths; adding a new renderer must not change model rules.

## Contribution and planning evidence

The repository's recorded implementation and author tags identify these overlapping areas;
this is not a claim of exclusive ownership or a numerical contribution score.

| Contributor | Recorded areas |
| --- | --- |
| Daoyan Zhu (`u7782042`) | Build/CI, movement/world/combat/classes/Boss coordination, reactions, art/audio integration, tests and documentation |
| Jiaqi Chen (`u7851266`) | NPC/dialogue and inventory prototypes, storage/minimap/HUD, title/settings screens, objective and endgame UI, associated tests |
| Hao Liu (`u7998420`) | Inventory/equipment integration, save/load/menu coordination, merchant services, potion supply and gear upgrades |

Useful implementation evidence in Git includes inventory integration `21de2898`, save integration
`82f6a450` and `c8d14bc9`, pause/menu/potion services `ec0a8fb`, equipment refinements `a4c7e9f`
and `bbe814b`, Hall/objective work `9da1e65` and objective placement `07a28d7`.
These are completed development references, not instructions to create branches or merge work.
The [Activity_1 work plan](../../Activity_1/1.2.md) records planning, dependencies and task owners;
use the actual records for assessment rather than obsolete allocation tables or inferred meeting notes.

## Reduced or cancelled scope

Separate achievements and NPC following were non-core and were dropped. Achievements depended
on save/load, which was implemented relatively late, leaving no time for achievement persistence.
The tutorial was reduced to NPC/Notice Board guidance and an objective tracker; crafting,
forging and enchanting were reduced to merchant upgrades. Damage-number display settings were
not implemented. These reductions kept attention on the core playable loop.

Safe-map saving, inventory/equipment, final class behaviors, elemental fields, debug console
and command/action integration tests are implemented and must not be listed as future work.
No standalone native UI automation runner, arbitrary mid-battle snapshot, external map loader
or multi-profile UI is shipped. The [tester guide](TEST_COVERAGE_REVIEW.md) distinguishes the
current gameplay scenario suite from ordinary unit tests and remaining native acceptance.
