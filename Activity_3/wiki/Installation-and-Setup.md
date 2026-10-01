# Installation and Setup

[Home](home) · [User Manual](User-Manual) · [Game Tester](game-tester)

## Requirements

- Git and access to the team GitLab repository.
- A full JDK 25, including `java`, `javac` and `javadoc`. Select a build matching the operating system and CPU architecture; Apple Silicon uses ARM64/aarch64.
- Internet access for the first Gradle/dependency download.
- A graphical desktop with working OpenGL and audio support to run the game. Model tests do not require opening the desktop game.

No separately installed Gradle, database, server or Docker container is required. Use the committed wrapper. Build configuration requests JDK 25 and compiles with `--release 17`; use JDK 25 for the documented build and runtime workflow.

## Obtain the project

```bash
git clone https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466.git
cd comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466
git switch main
cd Activity_2
```

All commands below run from `Activity_2`. For an existing clone, preserve any uncommitted work before synchronizing with the team repository. A personal fork's `main` can differ from the team's `main`.

## Check Java and Gradle

```bash
java -version
javac -version
./gradlew --version
```

On Windows, use `java -version`, `javac -version` and `./gradlew.bat --version` in PowerShell. Confirm JDK 25 is available and selected. If necessary, set `JAVA_HOME` to the installed JDK directory and put its `bin` directory on `PATH`.

On macOS, a registered JDK 25 can be selected for the current terminal with:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
export PATH="$JAVA_HOME/bin:$PATH"
```

An IDE-downloaded JDK may require selecting its actual installation directory instead. IntelliJ Project SDK and Gradle JVM settings are separate from terminal `JAVA_HOME`.

## Build, test and audit JavaDoc

macOS / Linux:

```bash
./gradlew clean build javadocAudit --no-daemon
```

Windows PowerShell:

```powershell
./gradlew.bat clean build javadocAudit --no-daemon
```

Successful completion ends with `BUILD SUCCESSFUL`. Tests or JavaDoc failures make the command fail. The first build downloads the wrapper and dependencies; later builds use Gradle's cache.

## Start the game

```bash
./gradlew lwjgl3:run
```

Windows:

```powershell
./gradlew.bat lwjgl3:run
```

The desktop window opens in the Hub. Close it to finish the run task. The Gradle configuration supplies native-access options and the macOS first-thread option. Assets are included through the desktop module's resource configuration; no manual copying is required.

In IntelliJ, open/import `Activity_2/build.gradle` as a Gradle project, choose JDK 25 for the project and Gradle JVM, reload Gradle, and run the `lwjgl3:run` task. Using that task applies the same launch settings as the terminal.

## Test and documentation reports

```bash
./gradlew test
./gradlew javadocAudit
```

| Output | Path relative to Activity_2 |
| --- | --- |
| Core test report | `core/build/reports/tests/test/index.html` |
| Desktop test report | `lwjgl3/build/reports/tests/test/index.html` |
| JUnit XML | Each module's `build/test-results/test/` |
| Strict JavaDoc output | Each module's `build/docs/javadoc-audit/index.html` |

See [Game Tester](game-tester) for selected scenarios and interpretation of results.

## Save files and starting again

The game stores `automatic.properties` and `manual.properties` in `.comp2120-game/saves/` under the Java user's home directory. They are outside the repository and survive builds and Git branch changes. Startup creates a fresh live session; use Manage saves to load a slot. Restart game resets current progress while preserving both files. To remove a slot, use the game's confirmed Delete action.

## Troubleshooting

| Symptom | Action |
| --- | --- |
| No matching Java 25 toolchain | Install/select JDK 25 and check both terminal Java and Gradle JVM settings. |
| Wrapper permission denied on macOS/Linux | Run `chmod +x gradlew`, then retry. |
| Dependency download fails | Check network, proxy and repository access; retry after connectivity is restored. |
| Gradle cannot find the project/tasks | Confirm the working directory is `Activity_2`. |
| Window cannot open over SSH or in CI | Use a graphical desktop to play; run the model tests in headless environments. |
| F5/F9/F10 changes a macOS system setting | Use Fn with the function key. Esc provides save management; backtick also opens the console. |

The earlier `/tmp/inventory-java26.gradle` workaround is not required for this JDK 25 setup and is not part of the distributed project.
