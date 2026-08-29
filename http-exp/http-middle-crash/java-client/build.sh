#!/usr/bin/env bash
# "Builds" this client implementation. No arguments; safe to re-run.
#
# There's no compile step: the source file itself is directly executable --
# it starts with a shebang line (#!/usr/bin/env -S java --source 17) and
# JEP 330's single-file source-launcher compiles it in memory and runs it
# in one step, no javac, no .class files on disk. (It lives at this
# directory's root rather than under src/main/java: on at least one dev
# setup, the shebang launcher fails when the script path is long/deeply
# nested.) build.sh's job here is just to (a) make sure the executable bit
# is set, in case it didn't survive a checkout, and (b) fail fast, before
# run.sh gets a chance to, by checking that java itself is new enough to
# run source level 17 (the source-launcher's runtime must be at least the
# --source version).
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

chmod +x HttpMiddleCrashTest

JAVA="${JAVA:-java}"
VERSION_LINE="$("$JAVA" -version 2>&1 | head -1)"
MAJOR="$(echo "$VERSION_LINE" | grep -oE '"?[0-9]+' | head -1 | tr -d '"')"

if [[ -z "$MAJOR" || "$MAJOR" -lt 17 ]]; then
    echo "java --source 17 needs a JDK 17+ runtime, found: $VERSION_LINE" >&2
    exit 1
fi
