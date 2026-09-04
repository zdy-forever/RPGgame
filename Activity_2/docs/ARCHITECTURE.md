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
| Developer A — Daoyan Zhu (`u7782042`) | `player`, `combat` | Inventory values and `WalkableArea` |
| Developer B — name/UID to confirm | `world`, `enemy`, future `quest` | Combat contracts and quest triggers |
| Developer C — name/UID to confirm | `npc`, `inventory`, equipment, future UI | Read-only player/quest APIs |

Each developer should change files inside their boundary. Changes to another
boundary or Shared Core require agreement before implementation.

## 6. Feature branches

- `feature/player-combat`
- `feature/world-enemy-quest`
- `feature/npc-inventory-ui`

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

- Confirm the names and UIDs for Developers B and C.
- Choose the map format and coordinate unit before implementing `GameMap`.
- Agree on quest objective/reward data before creating the `quest` package.
- Agree on attack target, cooldown, and input rules before defining the
  `PlayerClass` Strategy interface.
