# Docker

[Home](home) · [Installation and Setup](installation-and-setup)

## Current support

Docker is not required for the documented local build or desktop game; [Installation and Setup](installation-and-setup) remains the normal assessment path. A container deliverable is now committed under `Activity_4`: `Dockerfile`, `compose.yaml`, `Dockerfile.dockerignore`, `start-game.sh`, `healthcheck.sh`, `autostart.sh` and `init-game.sh`, with `README.md` and `VERIFICATION.md` documenting how to build it and what has actually been checked.

The image is built in stages. An `eclipse-temurin:25-jdk-noble` stage runs `./gradlew clean build javadocAudit :lwjgl3:installDist`, so the image is only produced when the tests and both JavaDoc audits pass. The runtime stage is `ghcr.io/linuxserver/baseimage-selkies:debiantrixie` with an `eclipse-temurin:25-jre-noble` JRE copied in. Selkies supplies the display, window manager, PulseAudio and the browser audio/video stream; an earlier silent noVNC draft was replaced. Activity 4 does not modify Java gameplay source, and the Java toolchain remains 25.

## Running it

Compose builds the `comp6120-game:activity4` image from the repository root and publishes the stream on `https://127.0.0.1:6080` (container port 3001), served with a self-signed certificate that browsers will warn about. Saves and settings persist in the `game-data` volume mounted at `/config/.comp2120-game`, and a health-check script probes the service every 10 seconds after a 90-second start period. Container user IDs are set through `PUID`/`PGID` and the timezone is `Australia/Sydney`.

`start-game.sh` waits for the PulseAudio `output` sink before launching and exits with an error rather than starting a silent game, so a missing audio sink is a visible failure instead of an unnoticed fallback. Audio is configured through `ALSOFT_DRIVERS=pulse`, `PULSE_SINK=output`, `SELKIES_AUDIO_ENABLED=true` and `SELKIES_AUDIO_DEVICE_NAME=output.monitor`; rendering uses `LIBGL_ALWAYS_SOFTWARE=true`.

The existing CI configuration requests `eclipse-temurin:25-jdk` for the Gradle job. That is the build/test image and is not an interactive game deployment.

## Verification status

`Activity_4/VERIFICATION.md` is the authoritative record. The following static checks pass: shell syntax for the launch and health scripts, `docker compose -f Activity_4/compose.yaml config`, local Markdown links and code fences, and a whitespace check across Activity 4. These establish configuration structure, not successful image execution.

A successful build and a fixed black-canvas startup fault have both been reported by a team member. Browser gameplay, audible output and a full playthrough remain **unverified**: a screenshot showing the Selkies sidebar is not evidence that sound reaches the host speakers. The outstanding acceptance checklist covers service health, title menu, New Game, Hub rendering and controls, a PulseAudio `output` sink with an `output.monitor` source, **listening** to confirm Hub music reaches host speakers, volume changes and battle track changes, save persistence across container recreation, and restart recovery. Cross-platform, cross-architecture and browser compatibility require actual runs.

## Limits

- Docker service health and JUnit success must not be reported as an audio acceptance pass.
- A screenshot alone cannot prove sound playback; a dated listening result is required.
- The CI image tag is not pinned to an immutable digest, and first-time builds still need dependency downloads.
