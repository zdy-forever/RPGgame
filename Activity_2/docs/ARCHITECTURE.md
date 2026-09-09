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
├── GameSession.java
├── TravelSystem.java
├── combat/
│   ├── Attack.java
│   ├── AttackSpec.java
│   ├── Combatant.java
│   ├── CombatSystem.java
│   ├── Damageable.java
│   ├── Faction.java
│   └── Health.java
├── enemy/
│   ├── Enemy.java
│   ├── EnemyAI.java
│   ├── EnemyConfig.java
│   └── EnemyState.java
├── inventory/Inventory.java
├── npc/NPC.java
├── player/
│   ├── Player.java
│   └── PlayerInput.java
├── screen/
│   ├── CameraFollow.java
│   ├── CombatDemo.java
│   ├── GameConfig.java
│   ├── GameScreen.java
│   └── PlaceholderRenderer.java
└── world/
    ├── AStarPathfinder.java
    ├── CollisionBox.java
    ├── FastTravelDestination.java
    ├── GameMap.java
    ├── MapCatalog.java
    ├── MapType.java
    ├── Portal.java
    ├── SpawnPoint.java
    ├── TravelCatalog.java
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
- `GameSession` coordinates registered maps, the current map, and the same player
  instance across transitions. The screen reads its state and invokes its public
  transition methods. Model classes do not require a graphics context.
- A transition validates the target ID, spawn ID, and full-body occupancy before
  changing player state or current map. Failure preserves the previous state.
- Fast-travel/debug-unlock keys and window-title feedback belong to `GameScreen`;
  geometric rendering and role colours belong to `PlaceholderRenderer`.
- `CameraFollow` calculates a visible rectangle using only subject/map bounds and
  viewport world dimensions. `PlaceholderRenderer` applies its centre before
  drawing; the player, map, and travel modules have no camera dependencies.

### Camera / Scrolling

- The current camera follows the player rectangle centre directly, independently
  on both axes. This provides continuous horizontal and vertical scrolling for
  the current open rectangular maps; room snapping and cinematic effects are absent.
- `GameConfig` defines a fixed 640 x 360 world-unit view, independent of map size.
  Maps are no longer scaled to fit the whole map into the window on transition.
- On a large map axis, clamp the view inside the map. On an equal/smaller axis,
  centre the map with equal solid-background margins instead of using an inverted
  clamp interval. Map/player geometry is never modified for camera purposes.
- The existing `FitViewport` maintains world extent and aspect ratio on window
  resize. Its resize call does not recenter the camera; minimized/zero-size
  windows leave the previous valid screen viewport in place.
- Follow is stateless: every draw uses current map/player bounds, so portal and
  fast-travel arrivals snap to their valid view without stale bounds or lag.
- Pure mathematical tests cover targets, all map edges/corners, small/equal/mixed
  axes, alternate viewport dimensions, and actual map/spawn transitions. Graphics
  lifecycle and visual resize checks remain desktop verification concerns.

### Player

- Currently owns health, position, collision dimensions, speed, and normalized
  eight-direction movement. Classes and inventory/equipment remain future work.
- Does not load maps, store dialogue, implement AI, or calculate shared damage.
- Depends on world contracts and the shared combat health model; inventory remains future work.
- Public surface: constructor, `updateMovement`, read-only position/geometry/speed,
  and validated `enterArea`. `GameSession` calls `enterArea` for coordinated map
  changes. Movement continues to use `WalkableArea` rather than map internals.
- Implements `Damageable` by delegating to its own `Health`. `getHealth()` exposes
  queries and validated healing/restoration; the optional constructor capacity
  overrides `Player.DEFAULT_MAX_HP`. Movement/transitions do not reset health.

### Combat

- Owns shared hit-point validation, attack lifetimes, cooldowns, hit detection,
  raw-damage calculation, and delivery. Advanced modifiers remain future work.
- Does not handle input, rendering, movement collision, loot storage, or AI.
- Depends on stable combat contracts rather than concrete entity types.
- Public surface: `Damageable.takeDamage(int)` and `Damageable.isAlive()`.
- `Health` implements that contract and provides `getMaxHp()`, `getCurrentHp()`,
  `isDead()`, `heal(int)`, and `restoreFull()`. Capacity is a fixed positive integer;
  current HP stays within `[0, maxHp]`. Every new instance starts full and alive.
- Negative damage/healing throws before mutation, zero is harmless, and positive
  amounts saturate without overflow. Healing returns the actual HP restored.
  Death is derived from zero HP. Healing does not revive; only explicit
  `restoreFull()` restores a dead entity to full health.
- Health has no graphics, input, callbacks, or entity-specific rules. Effects,
  dead-player movement/travel policy, and full respawn orchestration are not
  implemented. Use mutations on the game-loop thread.
- `Combatant` adapts `Damageable` with a stable faction and mutable reference to
  immutable world hurtbox data. It is not a new entity hierarchy or health model.
  Visual/collision sizes do not determine combat geometry; owners update hurtboxes
  after movement. Player and Enemy retain their existing health implementations.
- `AttackSpec` is reusable type/damage/duration/cooldown data. `Attack` is one
  system-created invocation: unique scope-local ID, source, fixed hitbox, active
  time, and lifetime receiver-identity ledger. Only the system mutates invocations.
- `CombatSystem` registers one adapter per receiver and owns per-source cooldowns.
  It filters self, same faction, dead, and already-hit targets before positive-area
  overlap, calculates raw damage centrally, then calls `Damageable.takeDamage`.
  Border contact alone is a miss. It never dispatches on Player/Enemy/Boss classes.
- Update samples at the start of each step then ages attacks/cooldowns. Expired
  invocations cannot settle later; large steps sample once rather than doing swept
  detection. Attack order then registration order determines settlement; dead sources
  cannot retaliate later in the step. Invalid time is rejected before any mutation.
- Register/attack requests reject nulls and invalid membership. Duplicate receiver
  aliases are rejected. Unregister cancels that source's attacks; target hit ledgers
  survive re-registration. Clear cancels all attacks/registrations/cooldowns without
  changing HP, and invocation identifiers are not reused after clear.
- `screen/CombatDemo` is an asset-free debug adapter, not part of core combat rules.
  It places multiple enemies near valid battle-map entries and clears scopes using
  GameSession's transition stamp. J requests a player strike; enemies now attack
  automatically through EnemyAI, replacing the former H shortcut. R explicitly
  restores HP, resets AI awareness, and clears attacks/cooldowns without repositioning.
  GameScreen routes requests. PlaceholderRenderer draws hurtbox/hitbox rectangles;
  HP and AI states use native window-title text. All AI updates precede exactly one
  shared combat update per simulation step.
- No projectile simulation, wall occlusion, armour, skills, elements, combos, or
  damage effects are implemented. Future class/AI code supplies attack tuning and
  geometry to this pipeline instead of applying Health damage separately. Receivers
  must not re-enter the pipeline during damage delivery; simulation is single-threaded.

### Enemy

- Owns enemy health, per-enemy decision state, and collision-constrained pursuit.
- Does not define separate damage formulas, load maps, or handle player input.
- Depends on `combat` and `world` contracts, never on screens or graphics APIs.
- Constructors create full, independently owned `Health`, using `DEFAULT_MAX_HP`
  or an explicit capacity. Implements `Damageable` by delegation and exposes
  `getHealth()` with the same semantics as Player. Health/damage implementation is unchanged.
- `EnemyAI` composes Enemy, an independent CollisionBox, WalkableArea, EnemyConfig,
  and a registered Combatant. A normal collection supports multiple controllers;
  no ECS, entity inheritance hierarchy, or singleton enemy is introduced.
- `EnemyState` is an enum-based finite-state model: IDLE acquires within detection
  radius; CHASE approaches through WalkableArea.move; ATTACK stops and requests
  attacks via CombatSystem; DEAD disables movement/new requests. Loss beyond a
  separately configured larger radius returns to IDLE without teleporting home.
- Ranges use centre distance. Attack comparison has a four-coordinate-ULP allowance
  to absorb normalized-movement rounding at the stop boundary. Map collision stays
  unchanged. Large steps stop at attack range instead of overshooting the target.
- Clear pursuit stays direct; blocked pursuit uses `world/AStarPathfinder`. The
  bounded four-neighbour grid samples full-body placements and validates complete
  edge envelopes, including off-grid connectors and thin-wall/corner clearance.
  Queue priority is travelled distance plus an admissible Euclidean stand-off
  heuristic. Grid membership is static; search arrays are local to each query.
- EnemyAI caches a route, waypoint index, target snapshot, and replan timer. All
  waypoint motion shares one speed-times-delta budget and uses WalkableArea.move.
  Moving-target and failed-path retries are throttled; clear direct approaches
  bypass A*. Death, lost targets, resets, and map changes discard route state.
  AI also checks a conservative clear corridor before initiating an attack.
- AI owns no damage formula or cooldown clock. The arena updates all AI, then one
  CombatSystem update settles hits, health/death, and timing. A health-aware state
  getter reports death immediately after settlement. Invalid/dead/friendly/absent
  targets and controllers removed from the arena stop pursuing.
- EnemyConfig validates positive finite speed/ranges, attack <= detection <= loss,
  and positive autonomous cooldown. GameConfig supplies demo values; generic AI
  accepts per-enemy tuning through its constructor.
- Navigation defaults are a 16-unit grid and a 0.5-second blocked-path replan
  interval, configurable through EnemyConfig with a backward-compatible constructor.
  WalkableArea exposes outer bounds for finite navigation. Grids over 20,000 nodes
  fail explicitly; grid resolution can miss passages narrower than its sampling.
- Demo spawn candidates are checked against map geometry/player/earlier enemies;
  later movement has no dynamic actor collision. New battle scopes create fresh
  enemies, while player HP is retained. This is not a persistent spawn/save system.
- No behaviour trees, patrol/home return, crowd avoidance, or dynamic navigation
  graph updates are implemented. Sealed areas return no route; thin/off-grid
  passages may need finer sampling. Static-map collision occupancy is cached.
  AI attack-initiation clearance is not global combat occlusion; existing player
  attacks and collateral overlap hits retain their previous rules.

### World / Map

- Owns immutable map identity/name/type, dimensions, static collision rectangles,
  named spawn points, and default-spawn selection. File loading remains future work.
- `Portal`, `FastTravelDestination`, and `TravelCatalog` add immutable travel data
  without referencing players, input, rendering, or the screen.
- Does not implement attacks, dialogue, inventory, or quest rewards.
- Does not depend on concrete Player, Enemy, NPC, or UI classes.
- `WalkableArea` exposes point/full-body occupancy and swept X-then-Y movement.
  The previous collision algorithm is reused unchanged.
- `GameMap` validates metadata, obstacles, and spawn anchors on construction.
  Spawn entry separately checks the entering body's complete dimensions.
- `MapCatalog` supplies one Hub and two Battle Maps as Java data definitions.
  Additional maps use the same class; there are no per-map subclasses.
- World units have a bottom-left origin (x right, y up), independent of pixels.
  Walkable space is the outer rectangle minus obstacles; edge contact is allowed.

### Portal / Fast Travel

- `TravelSystem` is an application-level coordinator alongside `GameSession`.
  It owns trigger eligibility, arrival protection, and in-memory unlock/enabled
  state. Every actual transition calls the existing `GameSession.switchMap`.
- Portal registration validates source map/region and target map/spawn references.
  Full-player target occupancy is validated by GameSession when execution occurs.
- Automatic activation uses positive body/trigger overlap after movement. Only
  enabled current-map portals can trigger; registration order resolves ties.
- Arrival suppresses automatic triggering until an update observes the player
  outside every portal region, including disabled regions. The guard handles
  startup inside portals, portal arrivals, fast travel, and external/same-map
  transitions via a session transition stamp. Failed portal attempts also wait
  for exit before retrying, without changing map/player state.
- Fast travel requires a known unlocked destination; portal activation does not
  depend on fast-travel unlock state. Unlocks remain explicit and in-memory.
- Rendering receives immutable portal snapshots and draws only coloured
  rectangles. No visual assets, particles, or audio are part of travel logic.

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
GameScreen -> GameSession, TravelSystem, PlayerInput, CombatDemo, PlaceholderRenderer
PlaceholderRenderer -> CameraFollow, observational player/map/portal/combat geometry
CombatDemo -> GameSession, EnemyAI/EnemyConfig, Enemy, CombatSystem, Combatant, AttackSpec
EnemyAI    -> Enemy, EnemyState, EnemyConfig, WalkableArea, AStarPathfinder, CombatSystem, Combatant
AStarPathfinder -> WalkableArea, CollisionBox (no entity, input, or graphics dependencies)
CombatSystem -> Combatant, Attack, AttackSpec, Damageable, CollisionBox
CameraFollow -> CollisionBox (pure mathematics)
TravelSystem -> GameSession, Portal/FastTravelDestination data
GameSession -> GameMap/SpawnPoint, Player (transition coordination)
Player     -> WalkableArea, CollisionBox, Health, Damageable
Enemy      -> Health, Damageable
Health     -> Damageable (no graphics, world, or entity dependencies)
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
- `Health` is the shared implementation of bounded HP; Player and Enemy delegate
  instead of maintaining separate damage, healing, or death rules.
- `CombatSystem.register`, `tryAttack`, `update`, `unregister`, and `clear` manage
  arena membership and attack settlement. `Combatant.setHurtbox` synchronizes only
  vulnerable geometry; getters expose debug data without rendering dependencies.
- `WalkableArea` exposes collision queries and resolved movement without exposing
  obstacle storage or future map-loader details.
- `GameSession.switchMap(mapId[, spawnId])` remains the transition entry point.
  It preserves player identity, size, speed, and health and increments its transition
  stamp only on success, including same-map re-entry.
- `TravelSystem.updatePortals()`, `fastTravel(destinationId)`,
  `unlockDestination(destinationId)`, and `setPortalEnabled(portalId, enabled)`
  expose travel behaviour without UI or graphics dependencies.
- `CameraFollow.viewFor(subjectBounds, mapBounds, viewWidth, viewHeight)` returns
  the visible world rectangle without reading input or mutating gameplay state.

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
| P0 | Hub/combat maps, boundaries, and map switching | `feature/map-system` |
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
- Current maps use Java rectangle data and named bottom-left spawn anchors in
  world units. Agree on any future external file format and portal triggers when
  those features are implemented.
- Agree on quest objective/reward data and the Inventory reward API before
  creating the `quest` package.
- Agree on attack target, cooldown, and input rules before defining the
  `PlayerClass` Strategy interface.
- Agree on the first save-file version and snapshot fields before implementing
  save/load; do not serialize complete LibGDX objects directly.
- Decide which P2 features will be dropped if the P0/P1 schedule slips.
