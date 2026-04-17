# Fabric Loader & API — Version Reference

> All versions verified against Modrinth API and Fabric meta API.
> Source: https://meta.fabricmc.net/v2/versions/loader

---

## Version Table

| MC Version | Fabric Loader | Fabric API | Loom |
|------------|---------------|------------|------|
| 1.16       | 0.9.2+build.206  | 0.25.0+1.16    | 0.6-SNAPSHOT |
| 1.16.1     | 0.9.2+build.206  | 0.25.0+1.16    | 0.6-SNAPSHOT |
| 1.16.2     | 0.9.3+build.207  | 0.29.4+1.16    | 0.6-SNAPSHOT |
| 1.16.3     | 0.10.8+build.218 | 0.31.0+1.16    | 0.6-SNAPSHOT |
| 1.16.4     | 0.10.8+build.218 | 0.33.2+1.16    | 0.6-SNAPSHOT |
| 1.16.5     | 0.11.7+build.2   | 0.40.1+1.16    | 0.6-SNAPSHOT |
| 1.17.1     | 0.11.7+build.2   | 0.46.1+1.17    | 0.7-SNAPSHOT |
| 1.18       | 0.12.12+build.1  | 0.46.3+1.18    | 0.8-SNAPSHOT |
| 1.18.1     | 0.12.12+build.1  | 0.46.6+1.18    | 0.8-SNAPSHOT |
| 1.18.2     | 0.13.3+build.1   | 0.55.3+1.18.2  | 0.8-SNAPSHOT |
| 1.19       | 0.14.9+build.1   | 0.56.0+1.19    | 0.10-SNAPSHOT |
| 1.19.1     | 0.14.9+build.1   | 0.58.5+1.19.1  | 0.10-SNAPSHOT |
| 1.19.2     | 0.14.21+build.1  | 0.58.5+1.19.2  | 0.10-SNAPSHOT |
| 1.19.3     | 0.14.21+build.1  | 0.67.1+1.19.3  | 0.11-SNAPSHOT |
| 1.19.4     | 0.15.11+build.1  | 0.83.0+1.19.4  | 0.12-SNAPSHOT |
| 1.20       | 0.14.21+build.1  | 0.83.0+1.20    | 1.2-SNAPSHOT |
| 1.20.1     | 0.15.11+build.1  | 0.92.7+1.20.1  | 1.2-SNAPSHOT |
| 1.20.2     | 0.15.11+build.1  | 0.91.2+1.20.2  | 1.3-SNAPSHOT |
| 1.20.3     | 0.15.11+build.1  | 0.91.0+1.20.3  | 1.3-SNAPSHOT |
| 1.20.4     | 0.15.11+build.1  | 0.97.0+1.20.4  | 1.4-SNAPSHOT |
| 1.20.5     | 0.15.11+build.1  | 0.97.2+1.20.5  | 1.6-SNAPSHOT |
| 1.20.6     | 0.15.11+build.1  | 0.100.8+1.20.6 | 1.6-SNAPSHOT |
| 1.21       | 0.15.11+build.1  | 0.100.8+1.21   | 1.7-SNAPSHOT |
| 1.21.1     | 0.18.6           | 0.116.9+1.21.1 | 1.7-SNAPSHOT |
| 1.21.2     | 0.18.6           | 0.110.0+1.21.2 | 1.8-SNAPSHOT |
| 1.21.3     | 0.18.6           | 0.110.6+1.21.3 | 1.8-SNAPSHOT |
| 1.21.4     | 0.18.6           | 0.119.4+1.21.4 | 1.8-SNAPSHOT |
| 1.21.5     | 0.18.6           | 0.120.0+1.21.5 | 1.9-SNAPSHOT |
| 1.21.6     | 0.18.6           | 0.124.0+1.21.6 | 1.9-SNAPSHOT |
| 1.21.7     | 0.18.6           | 0.128.0+1.21.7 | 1.9-SNAPSHOT |
| 1.21.8     | 0.18.6           | 0.132.0+1.21.8 | 1.9-SNAPSHOT |
| 1.21.9     | 0.18.6           | 0.136.0+1.21.9 | 1.9-SNAPSHOT |
| 1.21.10    | 0.18.4           | 0.138.4+1.21.10 | 1.11-SNAPSHOT |
| 1.21.11    | 0.18.6           | 0.140.0+1.21.11 | 1.11-SNAPSHOT |
| **26.1**   | **0.18.4**       | **0.145.0+26.1** | **1.15** |
| 26.1.1     | 0.18.4           | 0.145.3+26.1.1 | 1.15 |
| 26.1.2     | 0.18.4           | not yet released | 1.15 |

---

## Notes

- Fabric Loader is **not** tied to Minecraft version — the same loader version often spans many MC versions
- Loom is a **build tool**, not a runtime dependency — do not confuse with loader version
- For 26.x: `mappings` line removed from `build.gradle`; no more Yarn/Mojang remapping
- Source for 26.1 loader: https://fabricmc.net/2026/03/14/261.html
