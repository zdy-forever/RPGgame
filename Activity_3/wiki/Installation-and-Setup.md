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

## Build, test, audit JavaDoc and measure coverage

macOS / Linux:

```bash
./gradlew clean build javadocAudit :core:jacocoTestReport --no-daemon
```

Windows PowerShell:

```powershell
./gradlew.bat clean build javadocAudit :core:jacocoTestReport --no-daemon
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

The desktop window opens the title menu. Select **New Game** to enter Hub, or **Continue** to attempt to load the most recent readable save. Close it to finish the run task. The Gradle configuration supplies native-access options and the macOS first-thread option. Assets are included through the desktop module's resource configuration; no manual copying is required.

In IntelliJ, open/import `Activity_2/build.gradle` as a Gradle project, choose JDK 25 for the project and Gradle JVM, reload Gradle, and run the `lwjgl3:run` task. Using that task applies the same launch settings as the terminal.

## Tutor verification route

1. Run the full build/test/JavaDoc command above and inspect both test reports.
2. Run `lwjgl3:run`, choose New Game, and check the Hub appears. Use [User Manual](User-Manual) for controls, quests and portal selection.
3. Enter Training through Portal Guide for a short combat/control check; use the [Game Tester](game-tester) scenario commands for repeatable automatic checks.
4. Follow [Playtest and Screenshots](Playtest-and-Screenshots) for expected UI, Hall and Credits states. Its temporary capture wrapper and position fixtures are not installation prerequisites or normal player steps.

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
| Core JaCoCo HTML | `core/build/reports/jacoco/test/html/index.html` |
| Core JaCoCo XML | `core/build/reports/jacoco/test/jacocoTestReport.xml` |
| Strict JavaDoc output | Each module's `build/docs/javadoc-audit/index.html` |

See [Game Tester](game-tester) for selected scenarios and interpretation of results.

## Save files and starting again

The game stores `automatic.properties` and `manual.properties` in `.comp2120-game/saves/` under the Java user's home directory. They are outside the repository and survive builds and Git branch changes. Startup shows the title menu. New Game creates a fresh live session; Continue selects the newest readable slot. In-game Manage saves lets you explicitly choose automatic or manual. Restart game resets current progress while preserving both files. To remove a slot, use the game's confirmed Delete action.

Music/fullscreen preferences set through either the title-menu Settings screen or Esc → Settings are stored separately in `.comp2120-game/settings.properties`. Both entry points apply changes immediately and share the same persisted values.

## Troubleshooting

| Symptom | Action |
| --- | --- |
| No matching Java 25 toolchain | Install/select JDK 25 and check both terminal Java and Gradle JVM settings. |
| Wrapper permission denied on macOS/Linux | Run `chmod +x gradlew`, then retry. |
| Dependency download fails | Check network, proxy and repository access; retry after connectivity is restored. |
| Gradle cannot find the project/tasks | Confirm the working directory is `Activity_2`. |
| Window cannot open over SSH or in CI | Use a graphical desktop to play; run the model tests in headless environments. |
| F5/F9/F10 changes a macOS system setting | Use Fn with the function key. Esc provides save management; backtick also opens the console. |

## Build a distribution

From `Activity_2`:

```sh
./gradlew :lwjgl3:installDist
```

On macOS/Linux, run `lwjgl3/build/install/lwjgl3/bin/lwjgl3`; on Windows, build with `./gradlew.bat :lwjgl3:installDist` and run the generated `lwjgl3.bat`. Keep the distribution's `lib/` directory with its launcher. This is a distribution with dependencies, not a standalone `java -jar` archive. Generate macOS launch scripts on macOS so the first-thread JVM option is included. These are build instructions; platform packaging acceptance must be recorded separately.

## Implementation references

- [Game README](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/64310bfa5869a213c6e9025bc6eff2d45effcc3e/Activity_2/README.md): setup, resource packaging and distribution workflow.
- [Build toolchain](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/64310bfa5869a213c6e9025bc6eff2d45effcc3e/Activity_2/build.gradle), [dependency versions](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/64310bfa5869a213c6e9025bc6eff2d45effcc3e/Activity_2/gradle.properties) and [desktop build configuration](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/64310bfa5869a213c6e9025bc6eff2d45effcc3e/Activity_2/lwjgl3/build.gradle): source configuration at `64310bf`.
- [Main menu](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/64310bfa5869a213c6e9025bc6eff2d45effcc3e/Activity_2/core/src/main/java/au/edu/anu/comp2120/screen/MainMenuScreen.java): New Game/Continue/Settings/Quit behavior.
