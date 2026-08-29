#!/usr/bin/env bash
# Runs the client against the base URL passed as $1 -- that's the contract
# the top-level run.sh relies on. Exits 0 if all cases behaved as expected,
# nonzero otherwise.
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"
BASE_URL="${1:?usage: run.sh <base-url>}"
PYTHON="${PYTHON:-python3}"

exec "$PYTHON" http_middle_crash_test.py "$BASE_URL"
