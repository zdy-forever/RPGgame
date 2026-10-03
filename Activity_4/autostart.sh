#!/bin/sh
# Openbox explicitly reads this file with sh, regardless of its shebang.
# Invoke the separate executable so its Bash shebang is honoured.
/usr/local/bin/start-game.sh > /config/game.log 2>&1 &
