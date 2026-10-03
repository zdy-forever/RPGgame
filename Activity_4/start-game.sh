#!/usr/bin/env bash
# Selkies invokes this as the desktop user after creating its graphical session.
set -euo pipefail

# Wait for the audio service instead of allowing libGDX to silently fall back to no audio.
audio_ready=false
for ((attempt=0; attempt<150; attempt++)); do
    if pactl info >/dev/null 2>&1 && pactl list short sinks | awk '$2 == "output" { found=1 } END { exit !found }'; then
        audio_ready=true
        break
    fi
    sleep 0.2
done
if [[ "$audio_ready" != true ]]; then
    echo 'PulseAudio output sink did not become ready; refusing a silent game launch.' >&2
    exit 1
fi

mkdir -p /config/.comp2120-game
printf '%s\n' "$$" > /tmp/comp6120-game.pid
cd /opt/game
exec /opt/game/bin/lwjgl3
