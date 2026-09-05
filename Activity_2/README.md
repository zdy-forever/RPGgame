# COMP2120 Group Game

Java and LibGDX foundation for COMP2120 Assignment 3. The current application
demonstrates player movement and static rectangle collision using only geometric
placeholders. It loads no images, fonts, animation, sound, or music assets.

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

## Movement demonstration

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
- Tune speed, spawn/size, world dimensions, walls, and the frame-time limit in
  `screen/MovementDemoConfig.java`. This is temporary demo configuration, not a
  final map format. The player and map constructors also accept explicit values.
- Speed is measured in world units/second. The screen caps simulation time per
  frame at 0.1 seconds and discards the first update after startup/resume, so a
  stall does not cause a large jump. Model tests can exercise larger deltas.

To check manually, launch the game, hold each movement key, walk into each side
of a wall, reverse away from contact, try W+D and opposing keys, and resize the
window. The player should remain inside the map without penetrating grey walls.

## Current class responsibilities

- `MainGame`: application lifecycle and active-screen selection.
- `GameScreen`: coordinates input, simulation, lifecycle, and the temporary view.
- `Player`: validated position/size/speed and normalized eight-direction movement.
- `PlayerInput`: translates held WASD keys into movement intent.
- `MovementDemoConfig`: central demo geometry and movement tuning.
- `PlaceholderRenderer`: renders solid rectangles with `ShapeRenderer`.
- `Enemy`: future hostile entity.
- `NPC`: future non-player character.
- `GameMap`: immutable rectangular map and swept static-obstacle collision.
- `CollisionBox`: immutable geometry with finite, positive extents.
- `WalkableArea`: point/body occupancy and collision-resolved movement.
- `Inventory`: future item storage and item operations.
- `Lwjgl3Launcher`: desktop window configuration and application entry point.

## Scope

This is intentionally a small foundation. Movement currently supports static,
axis-aligned rectangles and a single demo map. It does not include dynamic entity
collision, map loading, portals, combat, or final artwork. Collision uses X-then-Y
axis separation rather than a continuous diagonal sweep, so large diagonal steps
can take a different route around corners than many smaller steps. Normal screen
updates cap delta to keep this effect limited; each axis still cannot tunnel.
Player/world model code is independent of LibGDX graphics and asset APIs; future
artwork can replace the renderer without changing movement or collision rules.
