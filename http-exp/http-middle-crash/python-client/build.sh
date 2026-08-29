#!/usr/bin/env bash
# "Builds" this client implementation. No arguments; safe to re-run.
# See ../python-stdlib-server/build.sh for why this is a syntax check rather than
# a real compile step, and why run.sh doesn't depend on it having run.
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

PYTHON="${PYTHON:-python3}"
"$PYTHON" -c 'import sys; assert sys.version_info >= (3, 8), "Python 3.8+ required"'
"$PYTHON" -m py_compile http_middle_crash_test.py
