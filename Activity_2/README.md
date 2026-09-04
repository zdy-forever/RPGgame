# COMP2120 Group Game

Minimal Java and LibGDX project skeleton for COMP2120 Assignment 3. The current
application opens a desktop window and renders a placeholder title. No gameplay
systems have been implemented yet.

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
```

Build output is written under each module's `build/` directory and is ignored by
Git.

## Current class responsibilities

- `MainGame`: application lifecycle and active-screen selection.
- `GameScreen`: minimal rendering screen used to confirm startup.
- `Player`: future player-controlled entity.
- `Enemy`: future hostile entity.
- `NPC`: future non-player character.
- `GameMap`: future map data and world management.
- `Inventory`: future item storage and item operations.
- `Lwjgl3Launcher`: desktop window configuration and application entry point.

## Scope

This is intentionally a small foundation. It does not include gameplay logic,
ECS, networking, databases, dependency injection, or additional frameworks.
