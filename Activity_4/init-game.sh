#!/bin/sh
set -eu
# Refresh the application-owned entry even when /config survives image replacement.
install -d -o abc -g abc /config/.config/openbox
install -o abc -g abc -m 0755 /defaults/autostart /config/.config/openbox/autostart
