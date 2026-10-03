# Activity 4 — Dockerise

This directory packages the Activity 2 game with **browser video, keyboard/mouse input
and audio streaming**. It uses JDK 25 to build and Java 25 to run, matching the existing
project toolchain; the Java 17 compilation target is unchanged.

The runtime uses the LinuxServer Selkies desktop base. The actual libGDX/LWJGL3 game
runs inside Linux with Mesa software rendering. OpenAL sends music to PulseAudio's
`output` sink; Selkies captures `output.monitor` and streams it to the browser. The game
is not converted to JavaScript. This replaces the earlier silent noVNC draft.

## Requirements

- Docker Desktop running with Linux containers on macOS/Windows, or Docker Engine
  with the Compose plugin on Linux.
- A current browser; Chrome or Edge is the recommended first verification target.
- Internet access for Docker Hub/GHCR, Debian packages, Gradle and Maven dependencies.
- The complete repository checkout, several GB of free disk space, and sufficient
  Docker VM memory for Gradle, the desktop and streaming (start with at least 4 GB).

No host JDK, XQuartz, host PulseAudio server, sound-device mount or GPU passthrough is
required. The image does not request microphone or webcam access. The runtime base
provides x86-64 and ARM64 images; actual platform results must be recorded separately.

## Start Docker before building

Docker Desktop must be running, not just installed. On macOS, open **Docker** from
Applications and wait until the engine is running. On Windows, open **Docker Desktop**
and use Linux containers. On Linux, start your installed Docker Engine service.

With a recent Docker Desktop CLI, you can also start it from the terminal:

```sh
docker desktop start --timeout 60
```

Confirm that the engine is reachable before continuing:

```sh
docker info
```

This must succeed and show a **Server** section without a connection error. Seeing a
Docker client version alone does not prove the engine is running. Repeat this check
after a reboot or after quitting Docker Desktop.

## Build and play with sound

Run this from **any directory inside this Git checkout**, including `Activity_2` or
`Activity_4` (macOS/Linux shell or Git Bash):

```sh
cd "$(git rev-parse --show-toplevel)" &&
docker compose -f Activity_4/compose.yaml up --build -d
```

The first line locates the repository root. The runnable blocks below use the same
pattern, so their paths do not depend on which activity directory you started in.
Run only the block for the operation you need; alternative launch methods are optional.
In zsh, `cmdand>` after the first line is the normal continuation prompt for `&&`;
paste the second command without copying the prompt itself.

Alternatively, choose the short command matching your **current directory**:

| Current directory | Command |
| --- | --- |
| Repository root (`comp6120`) | `docker compose -f Activity_4/compose.yaml up --build -d` |
| `Activity_2` | `docker compose -f ../Activity_4/compose.yaml up --build -d` |
| `Activity_4` | `docker compose up --build -d` |

The build executes all tests, strict JavaDoc checks and `installDist` before assembling
its runtime image. A failed build/check prevents a new image from being produced.

1. Open **https://localhost:6080/** (HTTPS, without the old `/vnc.html` suffix).
2. The local container generates a self-signed certificate. The browser may require
   you to acknowledge its certificate warning for this local address before connecting.
3. Connect/start the desktop session if prompted, then click inside the game window.
4. Enable/unmute audio in the Selkies sidebar if needed. Browser autoplay rules may
   require a click before sound starts. Make sure the browser tab and computer are unmuted.
5. Select **New Game**, enter Hub and listen for its music. Set the game's music volume
   above zero. The title menu alone is not sufficient evidence of gameplay audio.

Use the [Activity 2 controls](../Activity_2/README.md) to play. Browser/OS shortcuts may
intercept F5/F9/F10; the in-game Esc menu also provides save management.

If an older noVNC container is running, the same `up --build -d` command replaces it.
The existing `comp6120-activity4_game-data` volume is retained, now mounted at
`/config/.comp2120-game`. The desktop user keeps UID 10001. Do not remove volumes during
this upgrade. Use the new HTTPS URL, not the previous HTTP/noVNC link.

## Status, logs, restart and stop

Check status:

```sh
cd "$(git rev-parse --show-toplevel)" &&
docker compose -f Activity_4/compose.yaml ps
```

Read service logs:

```sh
cd "$(git rev-parse --show-toplevel)" &&
docker compose -f Activity_4/compose.yaml logs --tail=100 game
```

Restart the game when needed:

```sh
cd "$(git rev-parse --show-toplevel)" &&
docker compose -f Activity_4/compose.yaml restart game
```

Stop the service **only when you have finished playing**:

```sh
cd "$(git rev-parse --show-toplevel)" &&
docker compose -f Activity_4/compose.yaml down
```

Quit closes the Java game; the streaming desktop can remain running. Restart the service
to relaunch the game. `down` stops the whole service. The named `game-data` volume keeps
automatic/manual saves and settings across container replacement and ordinary `down`.
It is independent of native host saves. Adding `--volumes` to `down` deletes its progress.

Only **127.0.0.1:6080** is published. Keep this local, unauthenticated desktop off public
networks. It needs neither `--privileged` nor the host Docker socket.

## Plain Docker commands (optional alternative)

**Skip this section if Compose is already working.** This is a second way to launch
one game container, not another step required after Compose. Both methods use port
6080, so first stop the Compose service:

```sh
cd "$(git rev-parse --show-toplevel)" &&
docker compose -f Activity_4/compose.yaml down
```

Build the image:

```sh
cd "$(git rev-parse --show-toplevel)" &&
docker build -f Activity_4/Dockerfile -t comp6120-game:activity4 .
```

After the build succeeds, start the game (this command stays attached to its logs;
Ctrl+C stops the container):

```sh
docker run --rm --name comp6120-game --shm-size=1g -e PUID=10001 -e PGID=10001 -p 127.0.0.1:6080:3001 -v comp6120-game-data:/config/.comp2120-game comp6120-game:activity4
```

Open **https://localhost:6080/**. Stop this container before returning to Compose.
If Docker reports the name `comp6120-game` is already in use, inspect `docker ps -a`
and stop your existing plain-Docker game before launching another one.

The final `.` in the build command is the repository root build context.
`Dockerfile.dockerignore` excludes Git metadata, local builds and documentation
screenshots. The image compiles the current Activity 2 source inside Linux; it does
not copy host-built binaries. The plain Docker example uses a different named volume
from Compose.

## Tests and evidence

The Dockerfile runs `./gradlew clean build javadocAudit :lwjgl3:installDist --no-daemon`
**inside the build container**, from `/workspace/Activity_2`. This is an explanation
of the image build, not an additional host command. Docker reuses a successful build
layer if its inputs have not changed.

To explicitly rerun the build checks and retain their reports, build only the build
stage without its cache (optional; this does not start or replace the running game):

```sh
cd "$(git rev-parse --show-toplevel)" &&
docker build --no-cache --progress=plain --target build -f Activity_4/Dockerfile -t comp6120-game:build .
```

After that succeeds, export the reports from that build. This block creates a temporary
container without starting it, copies both report directories, then removes only that
temporary container. Docker generates its name so the command can be repeated:

```sh
(
  cd "$(git rev-parse --show-toplevel)" || exit 1
  report_container=$(docker create comp6120-game:build /bin/true) || exit 1
  trap 'docker rm "$report_container" >/dev/null' EXIT
  mkdir -p Activity_4/core-reports Activity_4/desktop-reports || exit 1
  docker cp "$report_container:/workspace/Activity_2/core/build/reports/." Activity_4/core-reports/ &&
  docker cp "$report_container:/workspace/Activity_2/lwjgl3/build/reports/." Activity_4/desktop-reports/
)
```

These are the reports retained in the build image. Do not run tests in a disposable
`docker run --rm` container and then expect its changed reports in the original image.

The health check probes the Java process, HTTPS gateway, PulseAudio and its monitor
source. It cannot tell whether browser output is muted or whether a person heard sound.
Use [VERIFICATION.md](VERIFICATION.md) to record real results, including Hub/battle music,
volume changes, save persistence and browser input.

## Troubleshooting and limitations

- **No sound:** connect/click the page, enable Selkies audio, unmute the tab/OS, enter
  Hub and raise the game volume. Check the container's PulseAudio state using:

  ```sh
  cd "$(git rev-parse --show-toplevel)" &&
  docker compose -f Activity_4/compose.yaml exec -T -u abc game pactl list short sinks &&
  docker compose -f Activity_4/compose.yaml exec -T -u abc game pactl list short sources &&
  docker compose -f Activity_4/compose.yaml exec -T -u abc game pactl list short sink-inputs
  ```

  Expect `output`, `output.monitor`, and a game playback stream while music is playing.
  These establish server-side routing, not audible browser playback.
- **Old page / connection reset:** switch from `http://.../vnc.html` to
  `https://localhost:6080/`. The new host port maps to container HTTPS port 3001.
- **Blank desktop after Quit:** restart the service to launch the game again.
- **Black screen / game missing:** rebuild after updating the files in Activity 4.
  The Openbox entry must be compatible with `sh`; it launches the separate Bash script
  and records its output in `/config/game.log`. Startup refreshes this entry even when
  desktop configuration survives image replacement. Inspect both game and service logs:

  ```sh
  cd "$(git rev-parse --show-toplevel)" &&
  docker compose -f Activity_4/compose.yaml exec -T game cat /config/game.log
  ```

  Service logs can still be read if the game log is missing:

  ```sh
  cd "$(git rev-parse --show-toplevel)" &&
  docker compose -f Activity_4/compose.yaml logs --tail=160 game
  ```

  An empty browser canvas alone cannot distinguish game startup failure from a streaming
  failure. Keep the log output when reporting this problem.
- **`docker.sock: connect: no such file or directory`:** Docker cannot reach its engine;
  the image build has not started. Follow [Start Docker before building](#start-docker-before-building),
  wait for `docker info` to succeed, then retry the same Compose command. The macOS
  endpoint `unix:///Users/<user>/.docker/run/docker.sock` is normal for Docker Desktop;
  Docker creates the socket when its engine starts. Do not create that file manually.
  If Docker Desktop says it is running but the connection still fails, inspect
  `docker context ls`. For this Docker Desktop workflow, test its context explicitly:

  ```sh
  docker --context desktop-linux info
  ```

  If that succeeds while plain `docker info` fails, check terminal `DOCKER_HOST` or
  `DOCKER_CONTEXT` overrides and the selected context before retrying. A stopped engine
  is not fixed by changing the Compose file path or rebuilding the image.
- **Port in use:** stop the old service or change the mapping to
  `127.0.0.1:6081:3001`, then use `https://localhost:6081/`.
- **Download failure:** check registry/package connectivity and retry. Runtime base
  tags are mutable; record the actual image identity with validation evidence.
- **Performance:** rendering and encoding run on CPU. Allocate more Docker VM resources
  if required. Audio/video latency depends on the browser and host; this deployment
  is not a native-rendering performance benchmark.

## CI and coursework

The existing `.gitlab-ci.yml` builds/tests Java in a provided Java image; it does not
build this Dockerfile. The Activity 4 acceptance evidence must include an actual image
build and the game running from that image, including the requested sound check.

Local Docker build/run satisfies the execution workflow without assuming shared runners
support Docker-in-Docker. If a Docker CI job is added later, confirm builder availability
first. `tags: [comp2120]` belongs in GitLab CI jobs/defaults, not Dockerfile stages; the
repository already declares this default tag.

## References

- [Start Docker Desktop](https://docs.docker.com/reference/cli/docker/desktop/start/)
- [Docker Desktop on macOS](https://docs.docker.com/desktop/setup/install/mac-install/)
- [Docker multi-stage builds](https://docs.docker.com/build/building/multi-stage/)
- [Docker build contexts](https://docs.docker.com/build/concepts/context/)
- [Eclipse Temurin images](https://hub.docker.com/_/eclipse-temurin)
- [Selkies application image integration](https://docs.linuxserver.io/selkies/developer-guide/building-images/)
- [Selkies audio and configuration](https://docs.linuxserver.io/selkies/user-guide/configuration/)
- [Selkies browser client](https://docs.linuxserver.io/selkies/user-guide/web-client/)
- [GitLab runner tags](https://docs.gitlab.com/ci/yaml/#tags)
