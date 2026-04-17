#!/usr/bin/env bash
# port.sh — Wrapper around auto-porter JAR for common use
#
# Usage:
#   ./scripts/port.sh --list-versions
#   ./scripts/port.sh --port <modPath> --to <version>
#
# The JAR is expected at auto-porter/build/libs/auto-porter-1.0.0.jar
# Build it first with: cd auto-porter && ./gradlew build

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="$SCRIPT_DIR/../auto-porter/build/libs/auto-porter-1.0.0.jar"

if [ ! -f "$JAR" ]; then
  echo "ERROR: auto-porter JAR not found at $JAR"
  echo "Build it first:"
  echo "  cd auto-porter && ./gradlew build"
  exit 1
fi

exec java -jar "$JAR" "$@"
