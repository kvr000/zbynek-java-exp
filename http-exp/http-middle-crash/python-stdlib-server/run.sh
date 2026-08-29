#!/usr/bin/env bash
# Runs the server in the foreground. Listens on $PORT (default 8089) and
# exposes GET /health for readiness checks -- that's the contract the
# top-level run.sh relies on.
#
# cd's into this directory and uses only relative paths from then on, so
# this works the same under Cygwin as anywhere else -- see
# ../java-client/run.sh for why.
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"
PORT="${PORT:-8089}"
PYTHON="${PYTHON:-python3}"

PORT="$PORT" exec "$PYTHON" http_middle_crash_server.py
