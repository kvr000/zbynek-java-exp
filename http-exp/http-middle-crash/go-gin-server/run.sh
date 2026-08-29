#!/usr/bin/env bash
# Runs the already-built server in the foreground (run build.sh first).
# Listens on $PORT (default 8089) and exposes GET /health for readiness
# checks -- that's the contract the top-level run.sh relies on.
#
# cd's into this directory and uses only relative paths from then on, so
# this works the same under Cygwin as anywhere else -- see java-client/run.sh
# for why. Also checks for a .exe suffix: `go build -o target/server` on
# Windows silently produces target/server.exe regardless.
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"
PORT="${PORT:-8089}"

BIN=target/server
[[ -x "$BIN" ]] || BIN=target/server.exe

if [[ ! -x "$BIN" ]]; then
    echo "target/server (or target/server.exe) not found -- run build.sh first" >&2
    exit 1
fi

PORT="$PORT" exec "$BIN"
