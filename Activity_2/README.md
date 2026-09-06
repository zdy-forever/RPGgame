# COMP2120 Group Game

Java and LibGDX foundation for COMP2120 Assignment 3. The current application
demonstrates a Hub and multiple Battle Maps, named spawn points, eight-direction
player movement, static rectangle collision, automatic portals, and unlocked fast
travel using geometric placeholders.
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
disabled examples. The entire map fits into the window, including differently sized maps.

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
- Tune player speed/size and the frame-time limit in `screen/GameConfig.java`.
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

## Current class responsibilities

- `MainGame`: application lifecycle and active-screen selection.
- `GameSession`: map registration, current map, player ownership, and safe transitions.
- `TravelSystem`: portal triggering, arrival protection, unlock state, and fast-travel checks.
- `GameScreen`: coordinates input, simulation, lifecycle, and the temporary view.
- `Player`: validated position/size/speed and normalized eight-direction movement.
- `PlayerInput`: translates held WASD keys into movement intent.
- `GameConfig`: shared player dimensions, speed, and frame-time tuning.
- `PlaceholderRenderer`: renders solid rectangles with `ShapeRenderer`.
- `Enemy`: future hostile entity.
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
collision, external map-file loading, bosses, enemies, elemental map rules,
saved map state, combat, or final artwork. Collision uses X-then-Y
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
