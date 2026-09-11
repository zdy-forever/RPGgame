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
│   ├── ElementType.java
│   ├── ElementStatus.java
│   ├── ElementRules.java
│   ├── Faction.java
│   ├── Health.java
│   ├── Projectile.java
│   └── ProjectileSpec.java
├── enemy/
│   ├── Boss.java
│   ├── BossConfig.java
│   ├── BossState.java
│   ├── Enemy.java
│   ├── EnemyAI.java
│   ├── EnemyConfig.java
│   └── EnemyState.java
├── inventory/
│   ├── Inventory.java
│   └── MaterialDrop.java
├── npc/NPC.java
├── player/
│   ├── AbilityLoadout.java
│   ├── AbilitySlot.java
│   ├── AbilityState.java
│   ├── AttackShape.java
│   ├── ClassDefinition.java
│   ├── Player.java
│   ├── PlayerAbilities.java
│   ├── PlayerClass.java
│   ├── PlayerClasses.java
│   ├── PlayerCommand.java
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
    ├── CollisionSweep.java
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

- Currently owns health, position, collision dimensions, speed, normalized
  eight-direction movement and the selected immutable ClassDefinition. Final class
  skills and inventory/equipment remain future work.
- Does not load maps, store dialogue, implement AI, or calculate shared damage.
- Depends on world contracts and the shared combat health model; inventory remains future work.
- Public surface: constructor, `updateMovement`, read-only position/geometry/speed,
  collision-constrained `moveBy`, and validated `enterArea`. `GameSession` calls `enterArea` for coordinated map
  changes. Movement continues to use `WalkableArea` rather than map internals.
- Implements `Damageable` by delegating to its own `Health`. `getHealth()` exposes
  queries and validated healing/restoration; the optional constructor capacity
  overrides `Player.DEFAULT_MAX_HP`. Movement/transitions do not reset health.

### Common player abilities

- PlayerInput samples held WASD plus edge-triggered J/K/L/O into PlayerCommand;
  it never selects class-specific damage rules. PlayerAbilities is the single
  controller per player, using injected immutable AbilityLoadout tuning.
- J/K/O submit rectangular area attacks or configured projectile volleys through the same canonical
  Combatant and CombatSystem pipeline. Independent slot cooldowns are owned by
  PlayerAbilities, so these invocations use zero source-level cooldown. Enemy
  source cooldowns and all shared damage/lifetime/hit filtering remain unchanged.
- READY/ACTIVE/COOLDOWN are observational states; update returns an accepted slot
  as the triggered event. Dead or unregistered owners report UNAVAILABLE and cannot
  move or trigger. No new state classes, factories or class hierarchy are required.
- L captures the normalized current/last direction (right initially), has fixed
  duration/speed and cooldown, and uses Player.moveBy -> WalkableArea.move. It
  cannot tunnel through walls; diagonal sliding follows existing axis sweeps.
  Only the remaining frame time after dash is used for normal movement. There is
  no invulnerability, damage, animation or cooldown refund when blocked.
- An active/new dash blocks new attacks. Otherwise O > K > J chooses the first
  ready requested attack; one new ability per step and no queued requests. Slot
  timers advance after input arbitration, matching combat-core's timing convention.
- CombatDemo coordinates player abilities, then every EnemyAI, then exactly one
  shared combat update. GameScreen handles fast travel before that step and portal
  overlap afterward. Transition cleanup cancels old attacks/dash immediately and
  rebinds the canonical player adapter, preserving slot cooldowns. R resets all
  arena actors and ability timers explicitly. Pause freezes simulation.
- Rendering reads existing attack rectangles; native window-title text shows slot
  lifecycle. No ability/controller/model loads art or depends on graphics APIs.
- This foundation exposes common slots, selected class tuning, AttackShape strategies and straight
  projectile delivery, not final class skill designs. Ultimate energy,
  swept portal triggers, combo buffering and global wall occlusion remain future work.

### Class composition and selection

- PlayerClass is an enum identity with stable explicit IDs, not a Player superclass
  or strategy interface. ClassDefinition composes that identity with AbilityLoadout.
  PlayerClasses provides all three immutable built-in configurations; Mage is the
  default identity, using a magic bolt for J and fixed area pulses for K/O. GameConfig aliases
  that default instead of duplicating class tuning or introducing a screen dependency.
- Every class uses the same final Player, Health, movement/collision, Combatant,
  PlayerAbilities, shared J/K/L/O slots, cooldowns and map/portal interactions.
  No subclass duplicates HP, movement, timing or damage formulas.
- AttackShape is the only new behaviour interface: a pure Strategy returning an
  immutable CollisionBox from source hurtbox, reach and facing. AREA and FORWARD
  supply two actual behaviours. The existing AreaAttack record gains this strategy
  component; its two-argument constructor still means AREA for compatibility.
  No code in the common executor dispatches on Archer/Mage/Katana identities.
- Per-class J/K/O definitions choose hitbox strategy, range, damage, active window
  and cooldown. Dash varies speed/duration/cooldown through existing validated data.
  Archer attacks and Mage J select shared projectile delivery rather than AttackShape.
  Future spells/combos must keep the same damage pipeline rather than calling Health
  separately; there is no universal speculative skill execution framework.
- Player owns the selected definition. Its selection setter is package-private;
  running selection goes through PlayerAbilities.selectClass. The three-argument
  ability constructor respects the initial class; the legacy explicit-loadout
  constructor overrides initial tuning while retaining that identity.
- Selection rejects null, dead/stale, identical-definition or active player
  attack/dash cases, including projectile launch windows and flight (null throws,
  the others return false). Successful selection
  preserves the player/health/adapter, map, position, speed, facing and remaining
  slot cooldown seconds. No registration or health reset is involved.
- PlayerInput samples F1/F2/F3 separately from PlayerCommand, with fixed F1/F2/F3
  priority and key-edge semantics. CombatDemo consumes same-frame new movement and
  abilities on selection attempts but still advances existing activity/AI/combat.
  Travel and R reset have precedence. Class persists across both map changes and R.
- F4 is another independent key-edge request, handled after class selection. It cycles
  NONE/WATER/FIRE/LIGHTNING/ICE/WIND through `ClassDefinition.withElement`, retaining
  all slot geometry/projectile/dash tuning. It uses the same guarded selection path and
  preserves cooldowns/HP/position. Rejected attempts consume new commands without queuing.
  F1/F2/F3 preserve the selected light-slot tag on the new class's J/K/O; F4 deliberately
  gives all three attack slots one debug element. Catalog definitions are not mutated.
- Rendering continues to consume generic attack rectangles, without class-specific
  assets or renderer branches. The native title displays the selected identity.

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
- Health has no graphics, input, callbacks, or entity-specific rules. PlayerAbilities
  blocks dead-player movement/abilities; debug travel still permits dead players.
  Effects and full respawn orchestration are absent. Use mutations on the game-loop thread.
- `Combatant` adapts `Damageable` with a stable faction and mutable reference to
  immutable world hurtbox data. It is not a new entity hierarchy or health model.
  Visual/collision sizes do not determine combat geometry; owners update hurtboxes
  after movement. Player and Enemy retain their existing health implementations.
- `AttackSpec` is reusable type/damage/duration/cooldown/element data; its legacy constructor uses NONE. `Attack` is one
  system-created invocation: unique scope-local ID, source, fixed or projectile hitbox, active
  time, and lifetime receiver-identity ledger. Only the system mutates invocations.
- `CombatSystem` registers one adapter per receiver and owns per-source cooldowns.
  It filters self, same faction, dead, and already-hit targets before positive-area
  overlap, calculates base damage plus any reaction bonus centrally, then calls `Damageable.takeDamage`.
  Border contact alone is a miss. It never dispatches on Player/Enemy/Boss classes.
- Update samples at the start of each step then ages attacks/cooldowns. Expired
  invocations cannot settle later; large steps sample once rather than doing swept
  detection. Attack order then registration order determines settlement; dead sources
  cannot retaliate later in the step. Invalid time is rejected before any mutation.
- Register/attack requests reject nulls and invalid membership. Duplicate receiver
  aliases are rejected. Unregister cancels that source's attacks; target hit ledgers
  survive re-registration, while elemental attachments are removed. Clear cancels all attacks/registrations/cooldowns/attachments without
  changing HP, and invocation identifiers are not reused after clear.
- `screen/CombatDemo` is an asset-free debug adapter, not part of core combat rules.
  It places multiple enemies near valid battle-map entries and clears scopes using
  GameSession's transition stamp. J/K/L/O delegate to PlayerAbilities; enemies attack
  automatically through EnemyAI, replacing the former H shortcut. R explicitly
  restores HP, resets AI awareness, and clears attacks/cooldowns without repositioning.
  GameScreen routes requests. PlaceholderRenderer draws hurtbox/hitbox rectangles;
  HP, AI and ability states use native window-title text. All AI updates precede exactly one
  shared combat update per simulation step.
- No global area-attack wall occlusion, armour, final class skills, combos, or
  damage effects are implemented. Future class/AI code supplies attack tuning and
  geometry to this pipeline instead of applying Health damage separately. Receivers
  must not re-enter the pipeline during damage delivery; simulation is single-threaded.

### Elemental metadata and one-generation reaction delivery

- ElementType provides NONE plus WATER/FIRE/LIGHTNING/ICE/WIND without string dispatch
  or visual dependencies. AttackSpec's optional fifth constructor argument supplies the
  element; its four-argument compatibility constructor supplies NONE. PlayerAbilities
  preserves that value when building a zero-source-cooldown invocation. Existing class
  definitions remain unchanged and opt-in, not implicitly assigned elements.
- CombatSystem owns sparse per-canonical-target ElementStatus snapshots. One attachment
  lasts three seconds by default. Same-element direct hits refresh; unknown pairs replace;
  NONE and derived hits preserve attachment without refresh. Reactions consume it. Invalid
  targets cannot attach, and death/unregister/clear remove status. Updates sample attacks
  before aging statuses, including new ones, consistently with existing frame semantics.
- ElementRules is a pure immutable recipe table with validated unique unordered pairs and
  configurable duration. Its two provisional defaults are water/fire (+10 triggering damage)
  and water/lightning (a 5-damage, 40-half-extent, 0.15-second child attack). Other pairs,
  resistances, frozen/burning states, final element assignments and final balance are undefined.
- The existing confirmed-hit path performs rule lookup, saturated integer base-plus-bonus
  calculation and Damageable/Health delivery, then consumes/applies attachment and queues any
  child. There is no elemental damage engine or new mutation inside Player/Enemy/Boss.
- Attack gains parentAttackId/isDerived provenance. Each direct attack-target reaction
  can create one child; the original attack hit ledger prevents repeat events. Children use
  the triggering source and normal target filtering, and never consume extra cooldown.
  They carry NONE and are explicitly forbidden from attaching/reacting, limiting depth to one.
- CombatSystem iterates a start-of-update attack snapshot. New children join the same active
  list but resolve next update, retaining their full initial duration until then. Each child
  has its own lifetime ledger and can hit each eligible target once. Distinct reaction
  events may produce overlapping child attacks; no global per-root damage suppression is assumed.
- A hard bound of 64 live children limits fan-out. Full capacity, exhausted IDs or invalid
  effect geometry discard only the optional child without retrying a consumed reaction.
  Source death/cancellation/unregister/scope clear cancel children; end-step cleanup also
  catches sources killed after they generated a child earlier that same update.
- CombatDemo reads attached names for native-title text, and the existing renderer displays
  derived Attack rectangles. No color-based logic, art, new input system or speculative
  element UI is introduced. Tests cover the full pair matrix, status timing, deduplication,
  bounded children, all three player classes, ordinary enemy attacks and Boss outcomes.
- The outgoing element is visible in the native title and can be cycled with the existing
  PlayerInput adapter's F4 debug request. No new reaction rule or profession assignment is
  needed for the playable acceptance path. GameplayIntegrationTest combines real maps,
  movement, portals, camera math, class abilities, enemy/Boss combat and reaction cleanup;
  it does not instantiate a graphics context or pretend to validate OS keyboard delivery.

### Archer and shared projectile delivery

- Archer J is one ordinary arrow, K one stronger/faster arrow, and O five arrows in
  a symmetric 40-degree fan. K/O are explicitly provisional: the repository's Activity 0
  file links to an online document that could not be read, so no final design is assumed.
  L keeps the existing shared Archer dash configuration and collision rules.
- The existing AbilityLoadout.AreaAttack record gains optional ProjectileSpec data;
  null means the original shape delivery used by Mage K/O and Katana.
  Range is the projectile distance cap; AttackSpec.activeSeconds is the launch
  window, and ProjectileSpec.lifetime is independent flight expiry. PlayerAbilities
  still owns slot cooldowns and calls the same CombatSystem for both delivery modes.
- Projectile contains only flight geometry/direction/speed/range/time and captured
  WalkableArea. CombatSystem owns its lifecycle through the same active Attack list;
  no separate projectile manager or damage formula is introduced. Attack.getHitbox
  exposes live arrow geometry to the existing renderer, which draws small rectangles.
- CombatSystem.tryProjectiles validates a full bounded volley before allocating IDs
  or consuming cooldown. Source/dead/capacity rejection is atomic. Each arrow is its
  own invocation and non-piercing; O can hit one broad target with multiple arrows,
  but an individual arrow never settles a target more than once.
- During the existing one-per-frame combat update, projectile travel is capped by
  both lifetime and range. CollisionSweep finds continuous full-body interior entry
  against current hostile hurtboxes. WalkableArea.firstBlock supplies static wall
  and world-boundary contact fractions; GameMap implements this without changing
  the existing axis-separated actor movement. Its test adapter delegates the new query.
- The first valid target/wall wins; walls win equal-time ties, and targets tie by
  registration order. Self, friendly, dead and unregistered targets are ignored.
  Detection then calls the same central damage calculation/delivery/Health pipeline
  as area attacks. Invocation-order death filtering is preserved across both modes.
- Arrow impact/expiry removes active membership, and clear/unregister/source death
  cancels retained flight references. Capacity is capped at 256 live arrows per arena,
  with at most nine per volley. Failed capacity checks do not charge player slots.
- Independent launch-window timers prevent flight lifetime from blocking the next
  ready J shot. Class selection, however, rejects active launch windows or any flying
  arrow. Map transitions clear all arrows and preserve slot cooldowns; R explicitly
  cancels flight and resets cooldowns. No duplicate Player/Health/CombatSystem exists.
- Current target geometry is sampled after actor movement; relative swept target
  motion is not implemented. No homing/gravity/piercing/ricochet or katana combo,
  particle, sprite, texture, sound or other art asset is introduced.

### Mage composition on shared spell delivery

- Mage adds configuration, not a new Player, projectile implementation, manager or
  damage formula. Its J slot composes the existing ProjectileSpec with AttackSpec:
  one 10-unit square, 400 units/s, 360 travel range, 1.5-second lifetime, 25 damage,
  0.25-second launch window and 0.5-second slot cooldown. Current/last WASD facing is
  normalized and captured at launch. All flight/collision/expiry/cleanup is shared.
- K is a 40-damage small area pulse (120-unit hurtbox expansion, 0.3 seconds active,
  2-second cooldown); O is a 75-damage large pulse (180 expansion, 0.5 seconds active,
  8-second cooldown). These are provisional replacements for undefined final spells.
  AttackShape.AREA captures fixed geometry; CombatSystem handles late entrants and
  per-invocation hit deduplication. Area pulses retain the existing no-wall-occlusion
  rule, while J uses swept wall collision. There is no separate spell update loop.
- L remains the common collision-safe dash (600 units/s for 0.2 seconds, 1-second
  cooldown), not a teleport. HP, hurtboxes, input arbitration, timing and transitions
  use existing code. The placeholder renderer already reads moving/fixed Attack boxes.
- Stable mage-light/mage-special/mage-ultimate IDs provide identification for future
  extensions; Mage contains no separate elemental reaction logic. New skill geometry/configuration
  can replace these prototypes through ClassDefinition/AbilityLoadout; all damage stays
  at the shared combat calculation boundary. Archer/Katana tuning is not changed.
- MageAbilitiesTest covers real spell execution and lifecycle without graphics.
  PlayerAbilitiesTest now uses an explicit area-only fixture so its generic executor
  regressions do not incorrectly assume that the default class's J is an area attack.
  Class and demo tests verify delayed Mage hits and shared selection/travel/reset rules.

### Katana composition on shared melee delivery

- Katana uses the existing Player, PlayerAbilities, AttackShape, Attack, CombatSystem
  and Health. No new production class is necessary: class composition already supports
  fixed melee attacks. PlayerClasses centralizes J/K/O and dash tuning; Archer/Mage
  definitions and shared engine behavior are unchanged by this feature.
- J is a 30-damage forward light slash (48 reach, 0.15 seconds active, 0.35 cooldown).
  K replaces its former area shape with a forward heavy slash (45 damage, 80 reach,
  0.25 seconds active, 2-second cooldown). Both reuse AttackShape.FORWARD, constructed
  from the independent source hurtbox and normalized current/last WASD facing.
- A slash captures geometry at cast time, so later movement/turning cannot redirect
  it; subsequent swings use the new facing. Diagonal shapes are conservative AABBs,
  not precise sword arcs. Collision geometry includes the source hurtbox, independent
  of the player's visual body; self filtering remains combat-core's responsibility.
- O is one surrounding area slash (85 damage per target, 110-unit expansion,
  0.35-second active window, 7-second cooldown), not a multi-hit timeline. CombatSystem
  samples currently overlapping targets before aging the attack. Its existing identity
  ledger allows one hit per target per invocation, including late entrants, and avoids
  re-entry/frame-spam damage. Expiry removes the attack; the next swing has a new ID.
- L retains the shared dash executor with Katana tuning: 750 units/s for 0.16 seconds,
  0.9-second cooldown and up to 120 units unobstructed. Swept map collision/axis sliding,
  input arbitration, independent slot timers, death and transition cleanup are reused.
- K/O are provisional skill definitions; there is no combo, animation dependency,
  invulnerability or asset loading. The existing renderer reads attack rectangles.
  Fixed J/K/O slashes retain the common no-wall-occlusion rule; this is distinct from
  collision-safe dash movement and swept projectile world collision.
- KatanaAbilitiesTest exercises eight-direction J/K hits, exact range boundaries,
  captured facing, active-window late entry, repeated-hit prevention, cooldown recovery,
  special/ultimate tuning, lethal/invalid targets, death and dash wall/map collision.

### Boss encounters and material drops

- Boss is a small encounter coordinator in `enemy`, composed of an existing Enemy
  health owner and EnemyAI navigation controller. The canonical Combatant wraps that
  Enemy; Boss does not introduce a second Damageable/Health identity. Collision body
  and hurtbox come from EnemyAI and are independent of final visual dimensions.
- EnemyAI's three-argument update optionally disables basic attack submission while
  preserving detection/loss hysteresis, A*, collision and attack readiness. Its original
  update delegates with submission enabled, preserving ordinary enemy behavior.
- BossConfig holds max HP, EnemyConfig navigation, threshold, transition duration,
  immutable ordered per-phase Pattern lists and material metadata. Patterns compose
  existing AttackSpec/AttackShape, with cooldown >= active duration and reach >= stopping
  distance. Boss advances the cycle only after CombatSystem accepts a cast.
- Phase 1 changes to phase 2 at the inclusive HP threshold, never reverses on healing,
  and pauses new movement/attacks once. CombatSystem.cancelAttacks cancels this source's
  fixed/projectile invocations without resetting registrations or cooldowns. It is also
  reused by unregister; other sources and damage ledgers remain unchanged.
- Boss.update runs before the arena's single combat update; synchronizeOutcome runs
  afterwards to process settled HP. Outcome handling is never called inside a damage
  receiver callback. Death wins over phase change and latches the encounter as defeated.
- MaterialDrop is immutable data in `inventory`: unique reward ID, material type,
  quantity, map ID and collision rectangle. Boss creates it once on defeat at its last
  valid location. getDrop/claimDrop provide a minimal read/one-time-transfer boundary;
  Inventory remains untouched because it has no storage API yet. Future adapters must
  check pickup distance and capacity before claiming, on the simulation thread.
- CombatDemo owns a single optional sentinel on Crossroads, alongside existing enemies.
  Safe placement candidates and provisional pattern tuning are in GameConfig. Map changes
  clear registrations/attacks; reset replaces the Boss rather than reviving the rewarded
  instance. Unclaimed drops are not persisted and old-scope Bosses cannot expose/claim
  rewards after detachment. Multiple Boss instances can share the same combat system.
- PlaceholderRenderer draws the existing large enemy hurtbox and gold MaterialDrop
  rectangles. The native title displays phase/HP/state/material text, with no asset load.
  There is no Boss UI, cutscene, animation, pickup UI or inventory rewrite.
- Limitations: two static rectangular pattern cycles, conservative navigation corridors,
  no actor-body collision, no post-cast wall clipping, no save/defeat persistence. An
  attack requires an initially clear target corridor but secondary area targets may be
  hit through walls. A defeated encounter stays stopped even if its Health is restored;
  a deliberate new encounter/reset is needed to fight and reward it again.

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
CombatDemo -> GameSession, PlayerAbilities, EnemyAI/EnemyConfig, Enemy, CombatSystem, Combatant
PlayerInput -> PlayerCommand, LibGDX Input (mapping only)
PlayerAbilities -> Player, ClassDefinition, AbilityLoadout/Slot/State, AttackShape, PlayerCommand, CombatSystem, Combatant, AttackSpec
PlayerClasses -> ClassDefinition, PlayerClass, AbilityLoadout, AttackShape, AttackSpec (immutable Java data)
AttackShape -> CollisionBox (pure geometry strategy)
EnemyAI    -> Enemy, EnemyState, EnemyConfig, WalkableArea, AStarPathfinder, CombatSystem, Combatant
AStarPathfinder -> WalkableArea, CollisionBox (no entity, input, or graphics dependencies)
CombatSystem -> Combatant, Attack, AttackSpec, Projectile/ProjectileSpec, Damageable, CollisionBox/CollisionSweep, WalkableArea
Projectile -> CollisionBox, WalkableArea (no input, rendering or damage application)
CameraFollow -> CollisionBox (pure mathematics)
TravelSystem -> GameSession, Portal/FastTravelDestination data
GameSession -> GameMap/SpawnPoint, Player (transition coordination)
Player     -> WalkableArea, CollisionBox, Health, Damageable, ClassDefinition/PlayerClass/PlayerClasses
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

PlayerClass is now a stable enum, and AttackShape is a small geometry Strategy.
ClassDefinition/AbilityLoadout composition supplies class differences without
Player subclasses. No Observer, Factory, repository or universal skill hierarchy
has been introduced; future execution abstractions need a concrete skill use case.

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
- Agree on final class targeting, projectile/weapon execution and balance before
  expanding the current geometry-only strategy beyond placeholder attacks.
- Agree on the first save-file version and snapshot fields before implementing
  save/load; do not serialize complete LibGDX objects directly.
- Decide which P2 features will be dropped if the P0/P1 schedule slips.
