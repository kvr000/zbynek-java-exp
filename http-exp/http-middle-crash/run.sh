#!/usr/bin/env bash
# Orchestrates one server implementation against one client implementation.
#
# Each implementation directory must provide two scripts, which is the
# entire contract -- this script doesn't know or care what language or
# framework is inside:
#
#   build.sh            builds the implementation. No arguments. Safe to
#                        re-run. Exits nonzero on failure.
#
#   server/run.sh        starts the built server in the FOREGROUND, listening
#                        on 127.0.0.1:$PORT (PORT is exported by this script,
#                        default 8089), and exposes GET /health returning
#                        200 once ready.
#
#   client/run.sh <url>  runs the built client's test(s) against the base
#                        URL passed as $1. Exits 0 if every case behaved as
#                        expected, nonzero otherwise -- that exit code is
#                        this script's own exit code.
#
# This means adding a new implementation later (a second server in, say,
# rust-server/, or a second client in python-client/) needs nothing here --
# just give it a build.sh and run.sh following the same contract and point
# --server/--client at it.
#
# No absolute paths are constructed anywhere here, so this works the same
# under Cygwin as on a regular Linux/macOS shell: we cd once into the repo
# root (wherever this script itself lives) and refer to everything else by
# plain relative paths from there.
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

SERVER_DIR="go-gin-server"
CLIENT_DIR="java-client"
PORT=8089

usage() {
    cat <<EOF
Usage: $(basename "$0") [--server DIR] [--client DIR] [--port PORT]

  --server DIR       server implementation to build+run.
                      Relative to the repo root, or absolute. (default: go-gin-server)
  --client DIR       client implementation to build+run.
                      Relative to the repo root, or absolute. (default: java-client)
  --port PORT        port the server listens on and the client targets (default: 8089)

Examples:
  $(basename "$0")
  $(basename "$0") --client python-client
  $(basename "$0") --server rust-server --client python-client --port 9090
EOF
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --server) SERVER_DIR="$2"; shift 2 ;;
        --client) CLIENT_DIR="$2"; shift 2 ;;
        --port)   PORT="$2"; shift 2 ;;
        -h|--help) usage; exit 0 ;;
        *) echo "unknown argument: $1" >&2; usage; exit 1 ;;
    esac
done

BASE_URL="http://127.0.0.1:${PORT}"

for d in "$SERVER_DIR" "$CLIENT_DIR"; do
    [[ -f "$d/build.sh" ]] || { echo "missing $d/build.sh" >&2; exit 1; }
    [[ -f "$d/run.sh"   ]] || { echo "missing $d/run.sh"   >&2; exit 1; }
done

echo "== building server: $SERVER_DIR =="
( cd "$SERVER_DIR" && ./build.sh )

echo "== building client: $CLIENT_DIR =="
( cd "$CLIENT_DIR" && ./build.sh )

echo "== starting server: $SERVER_DIR (port $PORT) =="
( cd "$SERVER_DIR" && PORT="$PORT" exec bash ./run.sh ) &
SERVER_PID=$!
trap 'echo "== stopping server (pid $SERVER_PID) =="; kill "$SERVER_PID" && wait "$SERVER_PID" || true' EXIT

echo "== waiting for server health =="
for i in $(seq 1 50); do
    if curl -fsS "$BASE_URL/health" >/dev/null 2>&1; then
        break
    fi
    sleep 0.2
done

echo "== running client: $CLIENT_DIR against $BASE_URL =="
( cd "$CLIENT_DIR" && exec ./run.sh "$BASE_URL" )
