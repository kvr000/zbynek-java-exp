#!/usr/bin/env bash
# Runs this client implementation against a running server.
#
# Usage: run.sh <base-url>
#
# The source file itself is the executable: it starts with a shebang line
# (#!/usr/bin/env -S java --source 17) and JEP 330's single-file
# source-launcher compiles it in memory and runs it in one step when the OS
# execs it directly -- no javac, no .class files on disk, and no separate
# `java` invocation needed here. (Per JEP 330 the shebang mechanism only
# applies to files that do NOT end in .java, which is why the file has no
# extension. It also lives at this directory's root rather than under
# src/main/java: on at least one dev setup, the shebang launcher fails when
# the script path is long/deeply nested.)
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

BASE_URL="${1:?usage: run.sh <base-url>}"

./HttpMiddleCrashTest "$BASE_URL"
