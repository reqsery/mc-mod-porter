#!/usr/bin/env bash
# validate-kb.sh — Validates knowledge-base/minecraft/ files for required format
#
# Checks each version file for:
#   1. A header line matching "## Version: X → Y"
#   2. A Java requirement line
#   3. A source line
#
# Usage: ./scripts/validate-kb.sh
# Exit code: 0 if all valid, 1 if any errors found

set -euo pipefail

KB_DIR="$(cd "$(dirname "$0")/.." && pwd)/knowledge-base/minecraft"
ERRORS=0

if [ ! -d "$KB_DIR" ]; then
  echo "ERROR: knowledge-base/minecraft/ not found at $KB_DIR"
  exit 1
fi

echo "Validating knowledge-base files in: $KB_DIR"
echo ""

for file in "$KB_DIR"/*.md; do
  name=$(basename "$file")

  # Check for version header
  if ! grep -q "^## Version:" "$file"; then
    echo "FAIL [$name]: Missing '## Version:' header"
    ERRORS=$((ERRORS + 1))
  fi

  # Check for Java requirement
  if ! grep -qi "java requirement" "$file"; then
    echo "FAIL [$name]: Missing Java requirement line"
    ERRORS=$((ERRORS + 1))
  fi

  # Check for source
  if ! grep -qi "source:" "$file"; then
    echo "FAIL [$name]: Missing 'source:' reference"
    ERRORS=$((ERRORS + 1))
  fi

  # Warn about unverified entries
  if grep -q "verify before use" "$file"; then
    echo "WARN [$name]: Contains 'verify before use' — review and confirm or remove"
  fi
done

echo ""
if [ "$ERRORS" -eq 0 ]; then
  echo "All files valid."
  exit 0
else
  echo "$ERRORS error(s) found."
  exit 1
fi
