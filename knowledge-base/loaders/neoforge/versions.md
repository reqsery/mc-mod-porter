# NeoForge — Version Reference

> NeoForge forked from Forge at Minecraft 1.20.2.
> Source: https://neoforged.net/

---

## Version Format

### 1.20.2 → 1.21.x format
`<mc-major>.<mc-minor>.<neoforge-build>[-beta]`

Examples:
- `20.2.59-beta` = MC 1.20.2, build 59, beta
- `21.4.136` = MC 1.21.4, build 136, stable (no `-beta` from build 121+)

### 26.x format (new)
`<year>.<drop>.<hotfix>.<neoforge-build>[-beta]`

Examples:
- `26.1.0.10-beta` = MC 26.1 (hotfix 0), NeoForge build 10, beta
- Source: https://neoforged.net/news/26.1release/

---

## Version Table

| MC Version | NeoForge Version | Notes |
|------------|-----------------|-------|
| 1.20.2     | 20.2.59-beta    | First NeoForge version |
| 1.20.3     | 20.3.8-beta     | |
| 1.20.4     | 20.4.80-beta    | |
| 1.20.5     | 20.5.21-beta    | |
| 1.20.6     | 20.6.99-beta    | |
| 1.21       | 21.0.168-beta   | |
| 1.21.1     | 21.1.172        | Stable (no -beta) |
| 1.21.2     | 21.2.38-beta    | |
| 1.21.3     | 21.3.38-beta    | |
| 1.21.4     | 21.4.136        | Stable from build 121+ |
| 1.21.5     | 21.5.35-beta    | |
| 1.21.6     | 21.6.25-beta    | |
| 1.21.7     | 21.7.15-beta    | |
| 1.21.8     | 21.8.15-beta    | |
| 1.21.9     | 21.9.15-beta    | |
| 1.21.10    | 21.10.50-beta   | |
| 1.21.11    | 21.11.0-beta    | |
| **26.1**   | **26.1.0.x-beta** | New 4-component format |
| 26.1.1     | 26.1.1.x-beta   | |
| 26.1.2     | 26.1.2.x-beta   | |

---

## Key Notes

- `-beta` suffix = breaking changes still allowed
- No `-beta` = stable API, breaking changes require major bump
- NeoForge versions before 1.21.4 build 121 used `-beta` even for "stable" builds
- For 26.x, update ModDevGradle to 2.0.141+ or NeoGradle to 7.1.21+
- `FMLEnvironment.getDist()` → `FMLEnvironment.dist` (changed in 21.10.x, verified via compile)
