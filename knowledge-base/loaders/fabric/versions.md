# Fabric Loader & API — Version Reference

> All versions verified against Modrinth API and Fabric meta API.
> Source: https://meta.fabricmc.net/v2/versions/loader
> Yarn builds: https://meta.fabricmc.net/v2/versions/yarn/{mc_version}

---

## Version Table

| MC Version | Fabric Loader | Fabric API | Loom | Yarn Mappings |
|------------|---------------|------------|------|---------------|
| 1.16       | 0.19.2 | 0.42.0+1.16     | 1.16-SNAPSHOT | 1.16+build.4      |
| 1.16.1     | 0.19.2 | 0.42.0+1.16     | 1.16-SNAPSHOT | 1.16.1+build.21   |
| 1.16.2     | 0.19.2 | 0.42.0+1.16     | 1.16-SNAPSHOT | 1.16.2+build.47   |
| 1.16.3     | 0.19.2 | 0.42.0+1.16     | 1.16-SNAPSHOT | 1.16.3+build.47   |
| 1.16.4     | 0.19.2 | 0.42.0+1.16     | 1.16-SNAPSHOT | 1.16.4+build.9    |
| 1.16.5     | 0.19.2 | 0.42.0+1.16     | 1.16-SNAPSHOT | 1.16.5+build.10   |
| 1.17.1     | 0.19.2 | 0.46.1+1.17     | 1.16-SNAPSHOT | 1.17.1+build.65   |
| 1.18       | 0.19.2 | 0.44.0+1.18     | 1.16-SNAPSHOT | 1.18+build.1      |
| 1.18.1     | 0.19.2 | 0.46.6+1.18     | 1.16-SNAPSHOT | 1.18.1+build.22   |
| 1.18.2     | 0.19.2 | 0.77.0+1.18.2   | 1.16-SNAPSHOT | 1.18.2+build.4    |
| 1.19       | 0.19.2 | 0.58.0+1.19     | 1.16-SNAPSHOT | 1.19+build.4      |
| 1.19.1     | 0.19.2 | 0.58.5+1.19.1   | 1.16-SNAPSHOT | 1.19.1+build.6    |
| 1.19.2     | 0.19.2 | 0.77.0+1.19.2   | 1.16-SNAPSHOT | 1.19.2+build.28   |
| 1.19.3     | 0.19.2 | 0.76.1+1.19.3   | 1.16-SNAPSHOT | 1.19.3+build.5    |
| 1.19.4     | 0.19.2 | 0.87.2+1.19.4   | 1.16-SNAPSHOT | 1.19.4+build.2    |
| 1.20       | 0.19.2 | 0.83.0+1.20     | 1.16-SNAPSHOT | 1.20+build.1      |
| 1.20.1     | 0.19.2 | 0.92.8+1.20.1   | 1.16-SNAPSHOT | 1.20.1+build.10   |
| 1.20.2     | 0.19.2 | 0.91.6+1.20.2   | 1.16-SNAPSHOT | 1.20.2+build.4    |
| 1.20.3     | 0.19.2 | 0.91.1+1.20.3   | 1.16-SNAPSHOT | 1.20.3+build.1    |
| 1.20.4     | 0.19.2 | 0.97.3+1.20.4   | 1.16-SNAPSHOT | 1.20.4+build.3    |
| 1.20.5     | 0.19.2 | 0.97.8+1.20.5   | 1.16-SNAPSHOT | 1.20.5+build.1    |
| 1.20.6     | 0.19.2 | 0.100.8+1.20.6  | 1.16-SNAPSHOT | 1.20.6+build.3    |
| 1.21       | 0.19.2 | 0.102.0+1.21    | 1.16-SNAPSHOT | 1.21+build.9      |
| 1.21.1     | 0.19.2 | 0.116.11+1.21.1 | 1.16-SNAPSHOT | 1.21.1+build.3    |
| 1.21.2     | 0.19.2 | 0.106.1+1.21.2  | 1.16-SNAPSHOT | 1.21.2+build.1    |
| 1.21.3     | 0.19.2 | 0.114.1+1.21.3  | 1.16-SNAPSHOT | 1.21.3+build.2    |
| 1.21.4     | 0.19.2 | 0.119.4+1.21.4  | 1.16-SNAPSHOT | 1.21.4+build.8    |
| 1.21.5     | 0.19.2 | 0.128.2+1.21.5  | 1.16-SNAPSHOT | 1.21.5+build.1    |
| 1.21.6     | 0.19.2 | 0.128.2+1.21.6  | 1.16-SNAPSHOT | 1.21.6+build.1    |
| 1.21.7     | 0.19.2 | 0.129.0+1.21.7  | 1.16-SNAPSHOT | 1.21.7+build.8    |
| 1.21.8     | 0.19.2 | 0.136.1+1.21.8  | 1.16-SNAPSHOT | 1.21.8+build.1    |
| 1.21.9     | 0.19.2 | 0.134.1+1.21.9  | 1.16-SNAPSHOT | 1.21.9+build.1    |
| 1.21.10    | 0.19.2 | 0.138.4+1.21.10 | 1.16-SNAPSHOT | 1.21.10+build.3   |
| 1.21.11    | 0.19.2 | 0.141.3+1.21.11 | 1.16-SNAPSHOT | 1.21.11+build.5   |
| 26.1       | 0.19.2 | 0.145.1+26.1    | 1.16-SNAPSHOT | *(none — unobfuscated)* |
| 26.1.1     | 0.19.2 | 0.145.4+26.1.1  | 1.16-SNAPSHOT | *(none)*          |
| 26.1.2     | 0.19.2 | 0.147.0+26.1.2  | 1.16-SNAPSHOT | *(none)*          |

---

## Notes

- Fabric Loader is **not** tied to Minecraft version — the same loader version often spans many MC versions
- Loom is a **build tool**, not a runtime dependency — do not confuse with loader version
- For 26.x: `mappings` line removed from `build.gradle`; no more Yarn/Mojang remapping needed
- Yarn format in `build.gradle`: `mappings "net.fabricmc:yarn:${project.yarn_mappings}:v2"`
- Yarn format in `gradle.properties`: `yarn_mappings=1.21.9+build.1`
- To verify the latest build number for any version: `https://meta.fabricmc.net/v2/versions/yarn/{mc_version}`
- Source for 26.1 loader: https://fabricmc.net/2026/03/14/261.html
