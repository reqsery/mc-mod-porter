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

| MC Version | NeoForge Version | ModDevGradle | Gradle wrapper | Notes |
|------------|------------------|--------------|----------------|-------|
| 1.20.2     | 20.2.93          | —            | —              | First NeoForge version |
| 1.20.3     | 20.3.8-beta      | —            | —              | beta |
| 1.20.4     | 20.4.253         | 2.0.148      | 9.2.1          |  |
| 1.20.5     | 20.5.21-beta     | 2.0.92       | 8.14.2         | beta |
| 1.20.6     | 20.6.144         | 2.0.148      | 9.2.1          |  |
| 1.21       | 21.0.167         | 2.0.148      | 9.2.1          |  |
| 1.21.1     | 21.1.267         | 2.0.148      | 9.2.1          |  |
| 1.21.2     | 21.2.1-beta      | 2.0.148      | 9.2.1          | beta |
| 1.21.3     | 21.3.100         | 2.0.148      | 9.2.1          |  |
| 1.21.4     | 21.4.161         | 2.0.148      | 9.2.1          |  |
| 1.21.5     | 21.5.101         | 2.0.148      | 9.2.1          |  |
| 1.21.6     | 21.6.20-beta     | 2.0.148      | 9.2.1          | beta |
| 1.21.7     | 21.7.25-beta     | 2.0.148      | 9.2.1          | beta |
| 1.21.8     | 21.8.57          | 2.0.148      | 9.2.1          |  |
| 1.21.9     | 21.9.16-beta     | 2.0.148      | 9.2.1          | beta |
| 1.21.10    | 21.10.64         | 2.0.148      | 9.2.1          |  |
| 1.21.11    | 21.11.48         | 2.0.148      | 9.2.1          |  |
| 26.1       | 26.1.0.19-beta   | 2.0.148      | 9.2.1          | New 4-component format |
| 26.1.1     | 26.1.1.15-beta   | 2.0.148      | 9.2.1          | beta |
| 26.1.2     | 26.1.2.129       | 2.0.148      | 9.2.1          |  |
| 26.2       | 26.2.0.95        | 2.0.148      | 9.2.1          |  |
| 26.3       | 26.3.0.115       | 2.0.148      | 9.2.1          |  |

---

## Key Notes

- `-beta` suffix = breaking changes still allowed
- No `-beta` = stable API, breaking changes require major bump
- NeoForge versions before 1.21.4 build 121 used `-beta` even for "stable" builds
- For 26.x, update ModDevGradle to 2.0.141+ or NeoGradle to 7.1.21+
- `FMLEnvironment.getDist()` → `FMLEnvironment.dist` (changed in 21.10.x, verified via compile)
