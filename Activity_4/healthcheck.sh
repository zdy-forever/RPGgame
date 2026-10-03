#!/usr/bin/env bash
# Service readiness does not prove that the browser has unmuted or played the audio.
set -euo pipefail
test -s /tmp/comp6120-game.pid
kill -0 "$(cat /tmp/comp6120-game.pid)"
# Local probe of the container's generated self-signed HTTPS endpoint.
curl --insecure --fail --silent --max-time 3 https://127.0.0.1:3001/ >/dev/null
s6-setuidgid abc pactl info >/dev/null
s6-setuidgid abc pactl list short sources | awk '$2 == "output.monitor" { found=1 } END { exit !found }'
