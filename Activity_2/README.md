# COMP2120 Group Game

Java and LibGDX foundation for COMP2120 Assignment 3. The current application
demonstrates a Hub and multiple Battle Maps, named spawn points, eight-direction
player movement, static rectangle collision, automatic portals, and unlocked fast
travel, and a map-bounded follow camera using geometric placeholders. Player and
enemy models share validated integer health and one attack pipeline, independent
of the demonstration view. Battle maps include a stationary combat-debug dummy.
It loads no images, fonts, animation, sound, or music assets.

## Author

- Daoyan Zhu (`u7782042`)

## Requirements

- JDK 25
- No system Gradle installation is required; use the included Gradle wrapper.

The project is compiled with JDK 25 while targeting Java 17 bytecode so that the
game remains usable on common Java runtimes.

Gradle 9.7.1 runs on JDK 25. The desktop module constrains LWJGL to 3.4.1 because
LibGDX 1.14.2 otherwise selects LWJGL 3.3.3, which is not suitable for running
on JDK 25. Keep Gradle at 9.1 or newer and LWJGL at 3.4 or newer when updating
dependencies.

## Project modules

- `core`: platform-independent game code and unit tests.
- `lwjgl3`: desktop launcher and LWJGL3 runtime dependencies.
- `assets`: textures, maps, audio, fonts, and UI resources.

The Stage 2 package boundaries and collaboration rules are documented in
[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

## Run the desktop game

On macOS or Linux:

```bash
./gradlew lwjgl3:run
```

On Windows:

```powershell
gradlew.bat lwjgl3:run
```

The run task automatically uses `assets/` as the working directory and applies
the macOS first-thread JVM option when required.

## Build and test

```bash
./gradlew clean build
./gradlew test
./gradlew core:javadoc
```

Build output is written under each module's `build/` directory and is ignored by
Git.

## Map and movement demonstration

The game starts in the Hub. Walk into a purple rectangle to use a portal. Number
keys now execute fast travel through its unlock checks, replacing unrestricted
debug map switching. The window title shows the active map and travel feedback.

| Key | Action | Initial state |
| --- | --- | --- |
| 1 | Fast travel to Hub `entry` | Unlocked |
| 2 | Fast travel to Training `entry` | Locked |
| 3 | Fast travel to Crossroads `entry` | Locked |
| U | Unlock both Battle fast-travel destinations for this session | Debug only |

The old key 4 map-selection shortcut has been removed. Visiting through a portal
does not automatically unlock fast travel; future quest/discovery code can call
`unlockDestination(id)`. Portal enabled state and fast-travel unlock state are
independent. Closing/restarting the game restores the initial state.

Hub has a blue-green floor and Battle Maps have a red-brown floor. Grey rectangles
are obstacles, gold circles mark spawn anchors, and the green rectangle is the
player. Bright purple rectangles are enabled portals; dark purple rectangles are
disabled examples. A fixed-size camera view scrolls over maps larger than the
view, instead of fitting the entire map into the window.

| Portal | Source trigger (x, y, width, height) | Destination |
| --- | --- | --- |
| hub-training | Hub (160, 64, 48, 64) | Training `entry` |
| hub-crossroads | Hub (160, 400, 48, 64) | Crossroads `entry` |
| hub-disabled | Hub (220, 64, 48, 64), disabled | Crossroads `east` |
| training-hub | Training (64, 64, 80, 80) | Hub `entry` |
| crossroads-hub | Crossroads (64, 64, 80, 80) | Hub `north` |

- Hold W/A/S/D to move up/left/down/right. Releasing keys stops movement.
- Movement supports eight directions. Opposite keys cancel on each axis. W+D
  moves diagonally up-right; diagonal input is normalized so its total speed
  equals straight movement. When one axis is blocked, the other can slide along
  the wall at its original component speed.
- The green rectangle is the player; grey rectangles are solid obstacles.
- The full player rectangle must stay inside the world and outside obstacles.
  Touching edges is permitted. Swept collision stops at the first obstacle even
  when a requested move would otherwise cross an entire thin wall. Collision
  resolves X first and then Y from the resolved position.
- Coordinates use a bottom-left origin, positive x right, and positive y up.
  World coordinates are independent of screen pixels; resizing letterboxes the
  view without changing map bounds, position, collision size, or movement speed.
- Tune player speed/size, camera world-view dimensions, and the frame-time limit
  in `screen/GameConfig.java`.
  Define map IDs, names, types, dimensions, walls, and spawns in
  `world/MapCatalog.java`. These are ordinary Java data definitions; no external
  JSON loader or new dependencies have been introduced.
- Speed is measured in world units/second. The screen caps simulation time per
  frame at 0.1 seconds and discards the first update after startup/resume, so a
  stall does not cause a large jump. Model tests can exercise larger deltas.

To check manually, launch the game, hold each movement key, walk into each side
of a wall, reverse away from contact, try W+D and opposing keys, and resize the
window. Initially press 2/3: the title should report that the destination is locked
and position should not change. Walk right from the Hub spawn into the nearby
purple portal to reach Training at (80, 80). Stay still: the overlapping return
portal must not send you back. Walk fully outside it (for example right past
x=144), then re-enter to return to Hub. Try U then 1/2/3 to exercise fast travel.
The disabled dark-purple portal must have no effect. A successful travel command
consumes the frame, leaving the player exactly at the selected spawn.

## Camera behaviour and verification

- `CameraFollow.viewFor(subjectBounds, mapBounds, viewWidth, viewHeight)` computes
  the visible world rectangle using only immutable geometry. `PlaceholderRenderer`
  applies its centre to the LibGDX camera before drawing. Player and world rules
  have no camera dependency.
- The default view is 640 x 360 world units (`GameConfig.CAMERA_VIEW_WIDTH` and
  `CAMERA_VIEW_HEIGHT`). It follows the player's rectangle centre directly on both
  axes, providing continuous horizontal/vertical scrolling. There is no room
  snapping, interpolation, camera shake, zoom animation, or cutscene state.
- For a map axis larger than the view, the view origin is clamped between the
  map's minimum coordinate and its maximum coordinate minus the visible extent.
  The camera stops at the edge while the player continues toward the map boundary.
- For a map axis smaller than or equal to the view, that axis stays centred on the
  map, independent of player movement. The remaining area is solid background with
  equal margins. A negative visible-rectangle origin in this case is deliberate;
  it does not create negative gameplay boundaries or move the player's coordinates.
- The `FitViewport` preserves the fixed world extent and aspect ratio on resize,
  adding letterboxing/pillarboxing as needed. Resize does not recenter the camera;
  zero-sized/minimized windows retain the last valid viewport. Different window
  sizes do not reveal extra map area or change follow/clamp calculations.
- After portal/fast travel, the next draw uses the new map and player's spawn
  directly. No previous-map camera position or clamp interval is retained.

To check manually:

1. Launch with `./gradlew lwjgl3:run`, press U then 3 to enter Crossroads.
2. Walk right from its entry along the unobstructed lower part of the map. The
   view begins scrolling once the player reaches its centre, then stops at the
   right boundary. Reverse direction and verify the left boundary similarly.
3. Re-enter Crossroads with 3 and walk up near the left side to check vertical
   scrolling and the top boundary; reverse toward the bottom. Avoid re-entering
   the return portal if you want to stay on the same map during the check.
4. Resize the window wide, tall, and small while stationary and while moving.
   Shapes must retain their proportions and the same world region must remain
   visible. Minimize/restore and check that the camera still follows normally.
5. Use 1/2/3 or the portals and check that the new spawn is visible immediately.
6. Small-map centring is covered by JUnit. For an optional visual check, temporarily
   set the configured view to 1280 x 720 and relaunch: Hub should remain centred
   with equal background margins even while the player moves. Restore 640 x 360
   afterward. Existing demo maps were not changed merely to test this case.

## Map API and extension

- `GameMap` is immutable data plus the existing collision implementation. Its
  walkable space is the map rectangle minus static obstacles. IDs are unique
  within a `GameSession`; names and `MapType` describe the map's role.
- Every map requires named `SpawnPoint` anchors and an existing default spawn.
  Coordinates specify the entering body's bottom-left corner. Map construction
  checks the anchor; entry checks that the entire player fits at that point.
- `GameSession` owns the current map and one persistent player. Use
  `switchMap(mapId)` for default entry or `switchMap(mapId, spawnId)` for a named
  spawn. Re-entering the same map also resets the player to the requested spawn.
- Unknown IDs and blocked/oversized entries throw `IllegalArgumentException`;
  null arguments throw `NullPointerException`. A failed transition preserves the
  old map, player position, and collision environment. Size and speed are retained
  across successful transitions. Coordinate session changes on the game-loop thread.
- Add another `GameMap` definition to `MapCatalog.createMaps()` to register a new
  level. No new subclass or movement/collision implementation is needed. A future
  caller can select it by ID through `GameSession`; add a debug key separately if
  it should be reachable through this temporary demo UI.
- Colours and spawn markers belong exclusively to `PlaceholderRenderer`. Map
  data has no artwork paths, texture objects, or other rendering dependencies.

## Portal and fast-travel API

- `world/TravelCatalog.java` defines portal routes/regions and initial destination
  unlock states. `Portal` and `FastTravelDestination` are immutable data snapshots.
- `TravelSystem` receives the existing `GameSession` and validates duplicate IDs,
  map/spawn references, and full trigger geometry during registration. It never
  reimplements player movement or map switching. Source trigger rectangles must
  be fully walkable; portals are non-solid trigger regions, not obstacles.
- Call `updatePortals()` once after movement. Full-body interior overlap with an
  enabled current-map portal triggers it; edge contact alone does not. Overlapping
  enabled portals choose the first registered match, with at most one transition
  per call. Disabled portals never execute a transition.
- On startup inside a portal, after any successful travel, or after an external
  `GameSession.switchMap`, the body must be observed outside all portal regions
  before another automatic entry is permitted. This includes disabled regions
  and same-map re-entry. No timer, animation, or frame-rate-based cooldown is used.
- `fastTravel(destinationId)` requires a known unlocked destination. Unknown IDs
  or invalid target placement throw `IllegalArgumentException`; locked travel
  throws `IllegalStateException`; null IDs throw `NullPointerException`.
  GameSession validates the full body at execution, preserving old map/player
  state on failure. Failed automatic portal attempts wait for exit before retry.
- `unlockDestination(id)` changes in-memory state; repeated calls are harmless.
  `setPortalEnabled(id, enabled)` changes activation. Getters return immutable
  snapshots, so presentation cannot bypass these state operations.
- Coordinate travel on the game-loop thread. Use GameSession for every map/spawn
  transition so its transition stamp can protect arrivals. Direct calls to
  `Player.enterArea` are a low-level operation and bypass that session coordination.

## Shared health API

- `combat/Health` owns immutable positive `maxHp` and private `currentHp` in
  `[0, maxHp]`. Both are integers, matching the existing `Damageable` contract;
  fractional values, NaN, and infinity are not accepted by the Java API.
- `new Health(maxHp)` starts full and alive. Zero/negative capacity throws
  `IllegalArgumentException`; `Integer.MAX_VALUE` is supported.
- `takeDamage(amount)` accepts non-negative integers and stops at zero. Zero is
  a no-op, including after death. `isAlive()` and `isDead()` derive from current
  HP, so a separate death flag cannot become inconsistent.
- `heal(amount)` returns the actual restored HP, capped at the missing amount.
  It does not revive dead entities. Negative damage/healing throws
  `IllegalArgumentException` before mutation, even when full or dead. Extreme
  positive amounts cannot overflow the integer HP range.
- `restoreFull()` explicitly restores full HP and can revive. It does not change
  position, map, movement speed, or any other entity state. It is a foundation
  for a later respawn coordinator, not an implemented respawn workflow.
- `Player` and the existing minimal `Enemy` implement `Damageable` by delegation.
  Each owns a separate `Health`, exposed by `getHealth()` for queries and
  validated healing/restoration. Existing constructors remain valid and use
  each entity's `DEFAULT_MAX_HP` (currently 100). Custom capacities use
  `new Player(bounds, speed, area, maxHp)` or `new Enemy(maxHp)`.
- Movement and map/portal/fast-travel transitions preserve HP. This branch does
  not decide whether dead players may move/travel: future gameplay coordination
  should apply that policy using `isAlive()`. The combat pipeline rejects attacks
  from dead sources; health itself owns no input, AI, effects, or respawn workflow.
- Use the model on the game-loop thread. It does not depend on LibGDX or load assets.

For example, this helper demonstrates the API with an existing player. The HP
comments assume a fresh default player; call it only for debugging because it
intentionally damages and restores that player.

```java
import au.edu.anu.comp2120.player.Player;

/**
 * Demonstrates health operations without rendering or assets.
 *
 * @author Daoyan Zhu (u7782042)
 */
final class HealthExample {
    /** Prevents construction of this example utility. */
    private HealthExample() {
    }

    /**
     * Exercises damage, healing, death, and explicit restoration.
     *
     * @param player existing player to modify for demonstration
     * @throws NullPointerException if player is null
     */
    static void demonstrate(Player player) {
        player.takeDamage(25);                // Default player: 100 -> 75 HP
        player.getHealth().heal(10);           // Returns 10; now 85 HP
        player.takeDamage(Integer.MAX_VALUE); // Zero HP; isAlive() is false
        player.getHealth().heal(10);           // Returns 0; remains dead
        player.getHealth().restoreFull();      // Full HP; position unchanged
    }
}
```

Run the graphics-independent checks with:

```bash
./gradlew core:test --tests '*HealthTest'
```

`HealthTest` covers model boundaries and `EntityHealthTest` covers delegation,
independent ownership, movement, and successful/failed map transitions. The combat
demonstration below exercises this same model rather than a separate debug HP counter.

## Combat core and placeholder verification

The common pipeline is:

```text
AttackSpec + registered source + world hitbox
  -> active Attack invocation
  -> self/faction/alive/already-hit filtering
  -> positive-area hitbox/hurtbox overlap
  -> shared raw-damage calculation
  -> Damageable.takeDamage
  -> existing Health clamping and derived death state
```

- `Combatant` adapts an existing `Damageable`, a `PLAYER`/`ENEMY` faction, and an
  independent world-space `CollisionBox` hurtbox. It adds no second health state.
  Maintain one stable adapter per entity in one arena; `register` rejects duplicate
  receivers, even through another adapter. Dead/removed actors cannot be valid targets.
- `AttackSpec(type, damage, activeSeconds, cooldownSeconds)` is reusable data.
  Type is a label, not a class-specific formula. Damage is non-negative integer;
  duration must be positive and finite; cooldown must be non-negative and finite.
- `CombatSystem.tryAttack(source, spec, hitbox)` returns an `Optional<Attack>`.
  Dead sources/cooling-down sources return empty; null arguments and unregistered
  sources fail explicitly. Validation happens before consuming cooldown.
- Each accepted invocation gets its own ID, fixed world-space hitbox, remaining
  duration, and identity-based hit ledger. The same type can be reused by any actor.
  One invocation hits each hostile receiver at most once, even across exit/re-entry,
  healing, revival, or re-registration of the same receiver. Multiple enemies can
  each receive one hit; a new invocation can hit them again.
- `update(delta)` samples currently active attacks against current hurtboxes **before**
  advancing duration/cooldown by delta. Creation is included; expiry is excluded.
  Zero-time samples are safe. A large finite delta samples once then expires the attack;
  there is no swept/moving-projectile detection. Negative/non-finite delta is rejected
  before mutation. Registration order and attack creation order make resolution deterministic;
  if an earlier strike kills an attacker, its later queued strike is cancelled.
- Damage calculation currently passes validated raw damage unchanged. Health performs
  lethal clamping. Armour, critical hits, elements, and class-specific modifiers are absent.
  All future damage formulas should extend the shared calculation boundary.
- Cooldown is per registered source across attack types, measured from creation.
  A zero cooldown intentionally allows distinct invocations; it does not remove the
  once-per-target rule. `clear()` cancels old attacks and removes registrations/cooldowns
  without healing actors. `CombatDemo` uses the existing session transition stamp for
  every map change, including same-map re-entry, portals, and fast travel.
- The owner updates hurtboxes after movement, then submits attacks, then calls `update`
  once per simulation step. Use one game-loop thread; custom damage receivers must not
  call back into/mutate the combat system while damage is being delivered.

Temporary controls (not final character skills):

| Key | Debug action |
| --- | --- |
| J | One player strike; holding the key does not auto-repeat |
| H | One stationary dummy strike; no enemy AI |
| R | Restore player/dummy HP and clear attacks/cooldowns; no repositioning |

1. Run `./gradlew lwjgl3:run`, then press U and 2 to enter Training. A red dummy
   appears near the entry. It is non-solid; its placement is checked against map walls.
2. The cyan outlines are hurtboxes. The player's is inset from its green body,
   demonstrating independence from visual size. Press J: a yellow attack rectangle
   lasts 0.25 simulation seconds, and the native title's dummy HP changes 100 -> 75.
3. Stand still: one rectangle must not keep reducing HP. Press J again after at
   least 0.5 simulation seconds to apply another 25 damage. Fast presses during
   cooldown are ignored. Four successful strikes make the dummy grey and dead.
4. Press R, then H: player HP changes 100 -> 75 through the same pipeline. Repeat
   with cooldown gaps to reach zero; J must then do no damage until R restores HP.
5. Walk upward far enough from the dummy and press J/H to verify misses. The debug
   attack expands 96 units around the source hurtbox in all directions; it is not
   a directional/class-specific weapon attack.
6. Attack then fast-travel or enter a portal. Old yellow rectangles disappear and
   cannot damage actors on the new map. Player HP is preserved; each new battle
   scope gets a fresh dummy. Hub has no dummy. Resize still uses the existing camera.

All tuning is in `GameConfig`; neither combat models nor detection import rendering
or asset APIs. `PlaceholderRenderer` only draws rectangles/outlines; HP feedback uses
the native title rather than loading fonts. `CombatDemo` only handles temporary input
requests, placement, hurtbox synchronization, and reset/transition policy.

Combat limitations: fixed axis-aligned hitboxes, discrete overlap samples, no wall
occlusion/line-of-sight or projectile motion, and no armour, knockback, attack wind-up,
invulnerability, combos, classes, elements, bosses, or final skill inputs. This debug
strike may hit through nearby walls. Dead-player movement/travel is still allowed;
only attacking is disabled. Dummy HP is not persisted across transitions. Pause freezes
combat, and the existing frame-time cap discards excess stall time.

Run combat-specific checks with:

```bash
./gradlew core:test --tests '*Combat*Test'
```

## Current class responsibilities

- `MainGame`: application lifecycle and active-screen selection.
- `GameSession`: map registration, current map, player ownership, and safe transitions.
- `TravelSystem`: portal triggering, arrival protection, unlock state, and fast-travel checks.
- `GameScreen`: coordinates input, simulation, lifecycle, and the temporary view.
- `Player`: validated position/size/speed, eight-direction movement, and owned shared health.
- `Health`: bounded integer HP, damage, healing, life status, and explicit restoration.
- `Damageable`: common validated damage/life-status contract for combat targets.
- `Combatant`: entity/faction adapter with independently supplied hurtbox geometry.
- `Faction`: minimal player/enemy friendly-fire distinction.
- `AttackSpec`: reusable validated attack tuning.
- `Attack`: one invocation's identifier, hitbox, active lifetime, and hit ledger.
- `CombatSystem`: registration, cooldowns, hit detection, damage calculation, and delivery.
- `CombatDemo`: temporary battle dummy, attack requests, hurtbox sync, and map/reset cleanup.
- `PlayerInput`: translates held WASD keys into movement intent.
- `GameConfig`: shared player, camera, combat-debug, and frame-time tuning.
- `CameraFollow`: stateless subject-centre, map-clamp, and small-map view mathematics.
- `PlaceholderRenderer`: applies the follow camera and renders shapes through a fixed `FitViewport`.
- `Enemy`: minimal hostile entity with independent shared health; no AI or rendering yet.
- `NPC`: future non-player character.
- `GameMap`: map identity/type, dimensions, obstacles, named spawns, and existing collision.
- `MapCatalog`: built-in Hub and Battle Map definitions.
- `MapType`: Hub/Battle metadata.
- `SpawnPoint`: validated world-space placement anchor.
- `Portal`: source map/region, destination map/spawn, and enabled snapshot.
- `FastTravelDestination`: named destination and unlocked snapshot.
- `TravelCatalog`: built-in portal routes and initial destination states.
- `CollisionBox`: immutable geometry with finite, positive extents.
- `WalkableArea`: point/body occupancy and collision-resolved movement.
- `Inventory`: future item storage and item operations.
- `Lwjgl3Launcher`: desktop window configuration and application entry point.

## Scope

This is intentionally a small foundation. Movement currently supports static,
axis-aligned rectangles and three built-in maps. It does not include dynamic entity
collision, external map-file loading, bosses, enemy AI, elemental map rules,
saved map state, final character skills, or final artwork. Collision uses X-then-Y
axis separation rather than a continuous diagonal sweep, so large diagonal steps
can take a different route around corners than many smaller steps. Normal screen
updates cap delta to keep this effect limited; each axis still cannot tunnel.
Player/world model code is independent of LibGDX graphics and asset APIs; future
artwork can replace the renderer without changing movement or collision rules.

Portal triggers sample overlap once per update, not a continuous swept trigger
path; a custom very large movement that crosses an entire portal between updates
can miss it. Built-in regions and the demo frame-time cap avoid that in normal
play. There is no travel menu, distance/cost restriction, automatic discovery,
quest gating, persisted unlock state, or portal animation/audio in this branch.
