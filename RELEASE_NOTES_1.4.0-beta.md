## MC Mod Porter v1.4.0-beta

This beta makes the porter update the whole build setup — not just `gradle.properties` — adds Yarn → Mojang source remapping, and ships a Windows app that needs no Java install.

### What's new

**Build tooling migration**

- Fixed NeoForge ports leaving old versions behind: hard-coded `id 'net.neoforged.moddev' version '…'` and `neoForge { version = "…" }` values are now updated (for example a 1.21.1 → 26.1.2 port now gets ModDevGradle `2.0.148` and NeoForge `26.1.2.129` instead of keeping `2.0.107` and `21.1.190`).
- Versions written straight into `build.gradle` / `build.gradle.kts` are updated as well as `gradle.properties`, in Groovy and Kotlin DSL: Loom and ModDevGradle plugins, Fabric Loader / Fabric API, NeoForge and Minecraft dependencies.
- The Gradle wrapper is set to the version the official Fabric example mod or NeoForge MDK uses for the target, and the old `distributionSha256Sum` is removed.
- The Java toolchain follows the target for every Java version (8, 16, 17, 21, 25), and mixin `compatibilityLevel` is updated to match.
- 26.x Fabric changes: `net.fabricmc.fabric-loom` plugin id, mappings lines removed, `mod*` configurations → plain ones, `remapJar` → `jar`, and `"fabric"` → `"fabric-api"` in `fabric.mod.json`. Going back to older versions switches to `net.fabricmc.fabric-loom-remap`.
- 26.x NeoForge projects get their Parchment blocks and properties commented out (the 26.x MDKs don't use Parchment).
- New stale version check: after patching, the porter lists every line in the build files that still contains an old version number (usually third-party dependencies to update by hand).

**Yarn → Mojang remapping**

- Fabric sources written with Yarn names are remapped to Mojang names in `.java` and `.kt` files. Yarn's and Mojang's official mapping files are downloaded once and cached in `~/.mc-mod-porter/mappings`.
- Happens automatically when Yarn sources are ported to 26.x (Yarn doesn't exist there). Use `--remap` to do it for other targets, `--no-remap` to skip it, and `--source-mappings auto|yarn|mojang` to override detection.
- Comments, strings and your mod's own names are never changed. Names that can't be mapped safely (overloads with several Mojang names, mixin target strings, wildcard imports) are reported as `MANUAL` instead of guessed.
- Works on source projects only — compiled JARs are never decompiled.

**Verified version data**

- All 39 supported versions were rebuilt from official sources only: the Fabric example mod branches, the NeoForge MDKs, Fabric meta and the Fabric / NeoForged Maven repositories.
- Examples: 26.1.2 → Fabric Loader `0.20.1`, Fabric API `0.155.3+26.1.2`, Loom `1.19-SNAPSHOT` with Gradle `9.8.2`; NeoForge `26.1.2.129` with ModDevGradle `2.0.148` and Gradle `9.2.1`.
- Removed NeoForge versions that were never published and corrected several Forge and Architectury versions.

**GUI**

- New **Source Mappings** (Auto Detect / Yarn / Mojang) and **Remap to Mojang Names** (Auto / Always / Never) options; detection now shows the mappings too, e.g. `Detected 1.21.1 / Fabric / Yarn`.
- Fixed the progress bar staying at 0%: it now moves through every step and the Gradle build, and the status line names the current step.
- Log symbols (`✓`, `⚠`, `→`) display correctly instead of empty boxes or `?`.
- New app icon.

**Windows app**

- `MC-Mod-Porter.exe` (GUI) and `MC-Mod-Porter-CLI.exe` with a bundled Java runtime — nothing else to install.
- `install-windows.ps1` adds **MC Mod Porter** to the Start Menu and Desktop; `uninstall-windows.ps1` removes it.

**CLI and fixes**

- Unknown options now stop with an error instead of being ignored silently (a typo like `--nobuild` no longer runs the build).
- `--list-versions` shows the Java version each target needs.
- Consoles that aren't UTF-8 get plain-text symbols (`->`, `OK`) instead of `?`.
- Fixed version rules that only fired for one exact source → target pair: span rules such as the 1.19.4 → 1.20.1 `GuiGraphics` rules now apply to any port that passes through them.
- Fixed six rules written for `1.19.0` that never ran.
- Fixed metadata, pack-format and warning steps skipping projects stored under a folder named `build` or `builds`.
- Removed two unused libraries; the JAR is about half the size.

### Download

- [`auto-porter-1.4.0-beta.jar`](https://github.com/reqsery/mc-mod-porter/releases/download/v1.4.0-beta/auto-porter-1.4.0-beta.jar) — standalone GUI + CLI JAR (any OS, Java 21+)
- [`MC-Mod-Porter-1.4.0-beta-windows.zip`](https://github.com/reqsery/mc-mod-porter/releases/download/v1.4.0-beta/MC-Mod-Porter-1.4.0-beta-windows.zip) — Windows app with its own Java runtime
- [`auto-porter-1.4.0-beta.zip`](https://github.com/reqsery/mc-mod-porter/releases/download/v1.4.0-beta/auto-porter-1.4.0-beta.zip) — packaged distribution with Windows start scripts
- [`auto-porter-1.4.0-beta.tar`](https://github.com/reqsery/mc-mod-porter/releases/download/v1.4.0-beta/auto-porter-1.4.0-beta.tar) — packaged distribution for macOS/Linux
- SHA256 files are included for verifying each download.

**Windows:** unzip `MC-Mod-Porter-1.4.0-beta-windows.zip` and run `MC-Mod-Porter.exe`. To add Start Menu and Desktop shortcuts, run this in the unzipped folder:

```powershell
powershell -ExecutionPolicy Bypass -File .\install-windows.ps1
```

**Any OS:**

```bash
# Open the desktop GUI
java -jar auto-porter-1.4.0-beta.jar

# Use the CLI
java -jar auto-porter-1.4.0-beta.jar --help
```

The JAR requires Java 21+ (the Windows app brings its own). Building a ported mod needs the target's JDK: Java 8 for 1.16.x, 16 for 1.17.1, 17 for 1.18–1.20.4, 21 for 1.20.5–1.21.11 and 25 for 26.x. Select an extracted mod source/project folder with its Gradle files and `src/`, rather than a compiled mod JAR.

### Beta notice

This is still **Beta**. Review the output, `MANUAL` lines, the stale version check and `build/port-report.json`. Semantic API changes, mixins and third-party dependencies can require manual fixes; a completed port is not a guarantee that every mod compiles or runs.

Docs: [reqsery.github.io/mc-mod-porter](https://reqsery.github.io/mc-mod-porter/)

[Report a bug](https://github.com/reqsery/mc-mod-porter/issues/new?template=bug_report.md) · [Request a feature](https://github.com/reqsery/mc-mod-porter/issues/new?template=feature_request.md)

**Full changelog:** https://github.com/reqsery/mc-mod-porter/compare/v1.3.0-beta...v1.4.0-beta
