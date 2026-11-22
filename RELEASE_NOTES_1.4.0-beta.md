## MC Mod Porter v1.4.0-beta

The porter now updates hard-coded build versions and the Gradle wrapper for every Minecraft version, remaps Yarn mods to Mojang names, and comes as a Windows app that needs no Java install.

### Highlights

- **Build tooling migration.** Version numbers written directly in `build.gradle` / `build.gradle.kts` are now updated too: Loom, ModDevGradle, NeoForge, Fabric Loader/API and Minecraft, in Groovy and Kotlin DSL. The Gradle wrapper and Java version are now set for every Minecraft version (previously only for 26.x), and mixin `compatibilityLevel` is updated as well.
- **NeoForge fix.** Ports no longer keep the old `moddev` plugin and `neoForge { version }` values.
- **Yarn → Mojang remapping.** Yarn sources are remapped automatically when porting to 26.x. Use `--remap`, `--no-remap` or `--source-mappings` to control it. Works on source projects only, and anything it can't map safely is flagged instead of guessed.
- **Stale version check.** Lists any old version numbers still left in your build files.
- **Verified version data.** All 39 versions were rechecked against the official Fabric example mod, the NeoForge MDKs and Maven.
- **GUI.** New Source Mappings and Remap options, and the progress bar now moves through every step instead of staying at 0%.
- **Windows app.** `MC-Mod-Porter.exe` with a bundled Java runtime, plus an install script for Start Menu and Desktop shortcuts.

### Fixes

- Unknown CLI options now show an error instead of being ignored.
- Version rules that span several versions now apply to longer ports (e.g. the `GuiGraphics` rules on 1.19.2 → 1.20.4).
- Projects stored under a `build` folder are no longer skipped.
- The JAR is about half the size after removing unused libraries.

### Download

| File | For |
|---|---|
| `MC-Mod-Porter-1.4.0-beta-windows.zip` | Windows: unzip and run `MC-Mod-Porter.exe`, no Java needed |
| `auto-porter-1.4.0-beta.jar` | Any OS: `java -jar auto-porter-1.4.0-beta.jar` (Java 21+) |
| `auto-porter-1.4.0-beta.zip` / `.tar` | Packaged distributions with start scripts |

SHA256 files are included for each download. Building a ported mod needs the target's JDK (Java 25 for 26.x, 21 for 1.20.5–1.21.11; see the [docs](https://reqsery.github.io/mc-mod-porter/supported-versions/)).

> **Beta:** always review the port report and any `MANUAL` lines. Some API changes, mixins and third-party dependencies still need manual fixes.

[Docs](https://reqsery.github.io/mc-mod-porter/) · [Report a bug](https://github.com/reqsery/mc-mod-porter/issues/new?template=bug_report.md) · [Request a feature](https://github.com/reqsery/mc-mod-porter/issues/new?template=feature_request.md)

**Full changelog:** https://github.com/reqsery/mc-mod-porter/compare/v1.3.0-beta...v1.4.0-beta
