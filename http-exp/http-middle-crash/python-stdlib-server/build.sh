#!/usr/bin/env bash
# "Builds" this server implementation. No arguments; safe to re-run.
#
# There's no compilation step for Python -- unlike go-gin-server/java-jdk-server's
# build.sh, this doesn't produce an artifact that run.sh depends on, so
# running run.sh without ever running build.sh first still works fine.
# build.sh's job here is just to fail fast, before run.sh gets a chance to,
# by checking for a usable Python 3 and doing a syntax-only compile pass
# (py_compile) over the source -- and to exist at all, for contract
# uniformity with the other implementations.
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

PYTHON="${PYTHON:-python3}"
"$PYTHON" -c 'import sys; assert sys.version_info >= (3, 8), "Python 3.8+ required"'
"$PYTHON" -m py_compile http_middle_crash_server.py
