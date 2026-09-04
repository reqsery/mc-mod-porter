#!/usr/bin/env bash
# port.sh — Wrapper around auto-porter JAR for common use
#
# Usage:
#   ./scripts/port.sh --list-versions
#   ./scripts/port.sh <modPath> <fromVersion> <toVersion> [--dry-run] [--no-build]
#
# The JAR is expected at auto-porter/build/libs/auto-porter-*.jar
# Build it first with: cd auto-porter && ./gradlew build

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR_DIR="$SCRIPT_DIR/../auto-porter/build/libs"
JAR="$(find "$JAR_DIR" -maxdepth 1 -type f -name 'auto-porter-*.jar' | sort -V | tail -n 1)"

if [ -z "$JAR" ] || [ ! -f "$JAR" ]; then
  echo "ERROR: auto-porter JAR not found in $JAR_DIR"
  echo "Build it first:"
  echo "  cd auto-porter && ./gradlew build"
  exit 1
fi

exec java -jar "$JAR" "$@"
