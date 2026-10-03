# Docker verification record

Prepared on 3 October 2026 from game source at `291153a`. The Java toolchain remains 25;
Activity 4 does not modify Java gameplay source. The current runtime is **Selkies with
PulseAudio browser output**, replacing the initial silent noVNC draft.

## Execution status

The Docker CLI is installed (29.8.0), but the tool environment denies access to the
engine socket. An execution-permission request again remained unresolved and was
cancelled. The user was given the Compose rebuild command to run in their terminal.

On 3 October 2026, the user reported a successful build after running the audio-version
Compose command from `Activity_2`. This is user-reported build evidence; the agent has
not inspected the build log, image identity or container test totals.

The agent attempted to open `https://localhost:6080/`, but the automated browser stopped
with `ERR_CERT_AUTHORITY_INVALID` at the local self-signed certificate. The user was
asked to open the page manually and verify Hub music. Browser gameplay, audible output
and a full playthrough remain unverified. An earlier noVNC run, if performed, does not
validate this replacement runtime.

The user subsequently supplied a screenshot showing the Selkies sidebar but a black
canvas. This is a failed visual startup check, not a gameplay/audio pass. Source review
found that Openbox reads its autostart file with `sh`, while the initial integration
copied a Bash-only script there. Locally, `dash -n` reproduced a syntax error at its
arithmetic loop. The integration now uses a POSIX-shell entry to invoke the executable
Bash launcher, refreshes the persisted entry at startup and saves game output to
`/config/game.log`. After the fix, the user reported that the black-screen problem was
resolved. This establishes user-reported visual recovery; audible output and the full
acceptance checklist have not yet been explicitly confirmed. Container logs were not
available to the agent.

The user then supplied errors from all four status/log/restart/stop commands: their
terminal was already in `Activity_4`, so `-f Activity_4/compose.yaml` incorrectly resolved
to `Activity_4/Activity_4/compose.yaml`. The README now locates
the repository root in path-dependent runnable blocks, separates status/restart/stop
operations, treats plain Docker as an alternative to Compose, and clarifies build-stage
report export. Compose configuration resolution was checked from the repository root,
`Activity_2` and `Activity_4`; this does not require Docker engine access.

## Completed static checks

| Check | Result |
| --- | --- |
| Shell syntax for launch and health scripts | Passed |
| `docker compose -f Activity_4/compose.yaml config` | Passed; context, user IDs, HTTPS port and existing save volume resolved |
| Local Markdown links and code fences | Passed |
| Whitespace check across Activity 4 files | Passed |

These checks establish configuration structure, not successful image execution.

## Acceptance checklist

- Build the image and retain the successful Gradle test/JavaDoc/build result.
- Record source revision, image identity, Docker version, host architecture and browser.
- Start the service and confirm health, title menu, New Game, Hub rendering and controls.
- Confirm a PulseAudio `output` sink, `output.monitor` source and live game playback stream.
- Enable browser audio, enter Hub and **listen** to confirm music reaches host speakers.
- Change game volume and confirm the audible effect; enter battle and check track changes.
- Save, recreate the container without deleting volumes and confirm load/settings persistence.
- Quit and restart the service; verify the game and audio recover.
- Retain a browser/game screenshot and a dated audio listening result. A screenshot alone
  cannot prove sound playback.

Cross-platform, cross-architecture and browser compatibility require actual runs. Docker
service health and JUnit success must not be reported as an audio acceptance pass.
