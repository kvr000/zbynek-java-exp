#!/usr/bin/env bash
# Builds this server implementation. No arguments; safe to re-run.
# Produces ./target/server.
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

go mod tidy
mkdir -p target
go build -o target/server .
