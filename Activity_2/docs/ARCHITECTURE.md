# Stage 2 Architecture

This document defines the module boundaries for the gameplay loop:

```text
Hub -> accept quest -> portal -> combat map -> combat -> rewards
    -> return to hub -> crafting/preparation -> next quest
```

## 1. Current package structure

```text
core/src/main/java/au/edu/anu/comp2120/
├── MainGame.java
├── combat/Damageable.java
├── enemy/Enemy.java
├── inventory/Inventory.java
├── npc/NPC.java
├── player/Player.java
├── screen/GameScreen.java
└── world/
    ├── GameMap.java
    └── WalkableArea.java

lwjgl3/src/main/java/au/edu/anu/comp2120/lwjgl3/
└── Lwjgl3Launcher.java
```

Dialogue remains part of `npc`, and Equipment remains part of `inventory` until
either area contains enough behaviour to justify another package. Quest and UI
are documented boundaries but do not have empty packages yet.

## 2. Module boundaries

### Application and screen

- Responsible for LibGDX lifecycle, update/render order, screen transitions, and
  coordination of feature modules.
- Does not implement movement, combat, AI, quests, dialogue, or inventory rules.
- May call feature modules, but feature modules must not depend on `GameScreen`.
- Public surface: LibGDX lifecycle methods on `MainGame` and `GameScreen`.

### Player

- Owns player position/state, movement state, current class, and references to
  inventory/equipment.
- Does not load maps, store dialogue, implement AI, or calculate shared damage.
- Depends on `inventory` and the `world`/`combat` contracts.
- Public surface is currently the constructor. Movement must use `WalkableArea`
  instead of accessing map internals.

### Combat

- Owns shared damage validation/calculation and attack resolution.
- Does not handle input, rendering, collision, loot storage, or AI.
- Depends on stable combat contracts rather than concrete entity types.
- Public surface: `Damageable.takeDamage(int)` and `Damageable.isAlive()`.

### Enemy

- Owns enemy state and, later, enemy decision-making.
- Does not define separate damage formulas, load maps, or handle player input.
- Depends on `combat` and `world` contracts.
- Public surface is currently the constructor. It implements `Damageable` when
  health is introduced rather than using fake placeholder behaviour.

### World / Map

- Owns map loading, collision, walkable regions, Hub/Combat Map identity, and
  portals.
- Does not implement attacks, dialogue, inventory, or quest rewards.
- Does not depend on concrete Player, Enemy, NPC, or UI classes.
- Public surface: `WalkableArea.isWalkable(float, float)`.

### NPC / Dialogue

- Owns NPC identity, interaction, dialogue data, and dialogue flow.
- Does not implement movement, map collision, combat, or enemy AI.
- Depends only on read-only player information and the future Quest boundary.
- Public surface is currently the constructor.

### Quest

- Will own quest acceptance, progress, completion, and reward requests.
- Will not render UI or implement inventory, combat, or map loading.
- Will depend on stable World/NPC/Combat events and Inventory reward operations.
- No public surface or package exists until objective/reward data is agreed.

### Inventory / Equipment

- Owns item storage, add/remove operations, equipped items, and equipment values.
- Does not apply final combat damage, render UI, or track quests.
- Has no module dependency initially; consumers use public methods.
- Public surface is currently the constructor. Equipment remains here.

### UI

- Will own HUD, menus, dialogue presentation, and inventory display.
- Will not mutate feature internals or implement gameplay rules.
- Will read public APIs from Player, Quest, Inventory, and NPC.
- No separate package exists until there are reusable UI classes.

### Save / Game State

- Will own save-file creation, loading, validation, and versioning.
- Will coordinate immutable snapshots supplied by gameplay modules rather than
  reaching into their private fields.
- Will not implement player, world, quest, achievement, or inventory rules.
- No package or save format exists until the first required state snapshot is
  agreed by all module owners.

### Achievement

- Will own achievement definitions, progress, and unlock state.
- Will consume stable gameplay events and expose read-only progress to UI.
- Will not duplicate quest progress or combat calculations.
- No package exists until the initial achievement list is agreed.

## 3. Dependency direction

```text
GameScreen -> feature modules (coordination only)
Player     -> Inventory, WalkableArea, combat contracts
Enemy      -> WalkableArea, combat contracts
NPC        -> read-only Player API, Quest boundary
Quest      -> World/NPC/Combat events, Inventory reward API
UI         -> read-only feature APIs
Combat     -> Damageable
World      -> no concrete Player, Enemy, NPC, or UI classes
Achievement -> stable gameplay events
Save       -> snapshot contracts supplied by feature modules
```

Feature modules do not depend on `GameScreen`, and Combat does not contain
type-specific branches. This prevents circular dependencies.

## 4. Necessary public interfaces

- `Damageable` provides one damage entry point for players, enemies, and bosses.
- `WalkableArea` exposes collision queries without exposing tile arrays or map
  loader details.

No `PlayerClass`, Observer, Factory, or repository abstraction exists yet.
`PlayerClass` should become a Strategy only after the first two classes and
their attack inputs and targets are known.

## 5. Three-developer boundaries

| Owner | Primary boundary | Integration boundary |
| --- | --- | --- |
| Developer A — Daoyan Zhu (`u7782042`) | `player`, `combat`, `world`, `enemy`, portals | Inventory/equipment values, quest combat events, save snapshots, and screen integration |
| Developer B — name to confirm (`u7998420`) | `npc`, dialogue, `quest`, achievements, save/load | Read-only gameplay events, inventory reward operations, and module state snapshots |
| Developer C — name to confirm (`u7851266`) | `inventory`, equipment, crafting, UI, settings/debug UI | Read-only gameplay APIs, world data for the minimap, and combat equipment values |

Developer A owns the core gameplay path from player movement, maps, collision,
and portals through enemy behaviour and combat resolution. Developer B owns
conversations, quest flow, achievements, and persistence coordination.
Developer C owns items, equipment, crafting, and presentation/configuration of
gameplay state. Every gameplay owner must expose a small snapshot API needed by
Developer B; the save module must not access another module's private fields.

`u8350466` is not included in the active development allocation.

Each developer should change files inside their boundary. Changes to another
boundary or Shared Core require agreement before implementation.

## 6. Feature branches

Use one focused branch per feature instead of one long-lived branch per person.
Priority 0 is the minimum playable loop, Priority 1 completes the planned game,
and Priority 2 is optional polish if time remains.

### Developer A — Daoyan Zhu (`u7782042`)

| Priority | Feature | Suggested branch |
| --- | --- | --- |
| P0 | Player position, WASD movement, and collision checks | `feature/player-movement` |
| P0 | Hub/combat maps, boundaries, and map switching | `feature/world-maps` |
| P0 | Portals and fast travel between maps | `feature/portal-fast-travel` |
| P0 | Health, shared damage rules, and attack resolution | `feature/combat-system` |
| P0 | Enemy state, movement/following, and basic AI | `feature/enemy-ai` |
| P1 | Camera following and horizontal/room scrolling | `feature/world-camera` |
| P1 | J/K/L/O actions: light attack, special, dash, and ultimate | `feature/player-actions` |
| P1 | Archer, mage, and katana class behaviour | `feature/player-classes` |
| P1 | Boss combat and boss-material drop events | `feature/boss-combat` |
| P2 | Derived attacks and elemental reactions | `feature/advanced-attacks` |

### Developer B — `u7998420`

| Priority | Feature | Suggested branch |
| --- | --- | --- |
| P0 | NPC identity, interaction range, and interaction flow | `feature/npc-interaction` |
| P0 | Dialogue data and dialogue progression | `feature/npc-dialogue` |
| P0 | Quest acceptance, progress, completion, and rewards | `feature/quest-system` |
| P1 | Quest manager, priest, and beginner-guide hub flows | `feature/hub-npcs` |
| P1 | NPC movement and player-following behaviour | `feature/npc-following` |
| P1 | Save/load of player, map, quest, inventory, and achievement state, including the agreed special-save rule | `feature/save-load` |
| P1 | Achievement definitions, progress, and unlock events | `feature/achievements` |
| P2 | Guided tutorial/onboarding sequence | `feature/tutorial-flow` |

### Developer C — `u7851266`

| Priority | Feature | Suggested branch |
| --- | --- | --- |
| P0 | Item model and inventory add/remove/use operations | `feature/inventory-items` |
| P0 | Weapon/armour equipment and stat values | `feature/equipment-system` |
| P0 | HUD, menus, dialogue view, and inventory view | `feature/game-ui` |
| P1 | Four armour slots and two-/four-piece set bonuses | `feature/armour-set-bonuses` |
| P1 | Boss-material crafting, weapon forging, and enchanting | `feature/crafting-enchanting` |
| P1 | Minimap presentation using read-only world data | `feature/minimap-ui` |
| P1 | Health-bar and damage-number display settings | `feature/combat-display-settings` |
| P1 | Automated gameplay smoke tester for the main loop | `feature/gameplay-smoke-tester` |
| P2 | Debug-mode controls and diagnostic overlay | `feature/debug-mode-ui` |

### Shared integration

- Assign only one developer to each `GameScreen` integration change.
- Integrate completed P0 domain features before beginning P1 features.
- Use stable events/read-only interfaces for quest, achievement, save, minimap,
  and UI integration instead of depending on private fields.

## 7. Shared Core

- `core/src/main/java/au/edu/anu/comp2120/MainGame.java`
- `core/src/main/java/au/edu/anu/comp2120/screen/GameScreen.java`
- root and module `build.gradle` files
- `settings.gradle`
- `gradle.properties`

Changes to these files must be minimal and state the integration reason in the
commit message.

## 8. Git collaboration rules

1. Branch from the same up-to-date integration point and keep commits focused.
2. Do not mix package moves or unrelated refactors into a feature commit.
3. Agree on changes to public interfaces or Shared Core before implementation.
4. Only one branch performs a given `GameScreen` integration change.
5. Merge feature code before its small Shared Core integration change.
6. Never commit `.gradle/`, `build/`, IDE files, or local environment files.

## Decisions still requiring the team

- Confirm the names for Developers B and C.
- Choose the map format, coordinate unit, and transition spawn-point format
  before implementing `GameMap` and portals.
- Agree on quest objective/reward data and the Inventory reward API before
  creating the `quest` package.
- Agree on attack target, cooldown, and input rules before defining the
  `PlayerClass` Strategy interface.
- Agree on the first save-file version and snapshot fields before implementing
  save/load; do not serialize complete LibGDX objects directly.
- Decide which P2 features will be dropped if the P0/P1 schedule slips.
