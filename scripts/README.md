# scripts/

Utility scripts for working with mc-mod-porter tools.

## Scripts

### `port.sh`
Wrapper around the auto-porter JAR. Requires the JAR to be built first.

```bash
# Build first
cd auto-porter && ./gradlew build

# List supported versions
./scripts/port.sh --list-versions

# Port a mod
./scripts/port.sh --port /path/to/mymod --to 1.21.10
```

### `validate-kb.sh`
Validates all `knowledge-base/minecraft/` files for required format fields.

```bash
./scripts/validate-kb.sh
```

Checks each file for:
- `## Version:` header
- Java requirement note
- At least one `source:` reference
- Warns if any `verify before use` entries remain

Exit code is `0` if all valid, `1` if errors found.

## Notes

- Scripts require bash
- Run from the repo root or any directory (paths are resolved relative to the script)
