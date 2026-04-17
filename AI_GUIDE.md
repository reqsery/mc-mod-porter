# mc-mod-porter — AI Guide

> ## ⚠️ CRITICAL RULE: DO NOT GUESS
> **Every version number, class name, method signature, and API change in this repo must come from the knowledge-base or a verified source.**
> **Never invent, assume, or extrapolate version data. If it is not in the knowledge-base, say so and look it up before proceeding.**
>
> ## 📖 SOURCE OF TRUTH
> **The knowledge-base is the single source of truth.**
> If any auto-porter rule conflicts with a knowledge-base file, the rule must be fixed or removed.
> The knowledge-base always wins.

This repo contains three standalone Java tools for Minecraft mod development. Each lives in its own subfolder with its own Gradle build.

---

## Repo Layout

```
mc-mod-porter/
├── auto-porter/        # Ports mods between MC versions (1.16 → 26.x)
├── visual-tester/      # Fabric mod: in-game screenshots + automated UI testing
├── deep-debugger/      # Static analysis + fuzz-test generator for mod source
├── knowledge-base/     # Verified MC/Fabric/NeoForge migration notes (markdown)
├── examples/           # Before/after Java files showing real migrations
├── patterns/           # Quick-reference method renames, class moves, signatures
├── docs/               # CONTRIBUTING, FAQ, TROUBLESHOOTING, INDEX
├── java/               # Java 17/21/25 change notes relevant to MC mods
└── templates/          # Version template format (not version data — that's in auto-porter)
```

---

## auto-porter

**Purpose:** Given a mod folder and a target MC version, produces a new `<mod>-ported-<version>` folder with all known API changes applied. Never modifies the original.

**Build:**
```bash
cd auto-porter
./gradlew build
# → build/libs/auto-porter-1.0.0.jar
```

**Key commands:**
```bash
java -jar auto-porter-1.0.0.jar --list-versions
java -jar auto-porter-1.0.0.jar <modPath> <fromVersion> <toVersion>
java -jar auto-porter-1.0.0.jar <modPath> <fromVersion> <toVersion> --dry-run
java -jar auto-porter-1.0.0.jar <modPath> <fromVersion> <toVersion> --no-build
java -jar auto-porter-1.0.0.jar --setup-templates   # regenerate gradle.properties templates
```

**Key source files:**
- `VersionDatabase.java` — all supported MC versions (1.16–26.1.x) with Fabric/NeoForge version numbers
- `ApiChangeRule.java` — text/import/signature rules applied during a port (e.g. KeyEvent, PlayerSkin, blit signatures)
- `AutoPorterMain.java` — CLI entry point; `portMod()` copies then patches
- `BuildFilePatcher.java` — patches `build.gradle` for structural changes (26.1 Loom plugin rename, mappings removal, Java 25)
- `GradlePropertiesPatcher.java` — updates version numbers in `gradle.properties`
- `SourcePatcher.java` — applies `ApiChangeRule` list to all `.java` files
- `ModMetaPatcher.java` — updates `fabric.mod.json` / `mods.toml`
- `TemplateManager.java` — reads/writes per-version `gradle.properties` templates

**Adding a new MC version:**
1. Add a row to `VersionDatabase.java` `static { }` block
2. Add migration rules to `ApiChangeRule.java` (use `addBidirectional` for two-way, or `new ApiChangeRule(from, to, ...)` for one-way)
3. Run `--setup-templates` to generate the template files
4. Rebuild

**Version coverage:** 1.16 through 26.1.2. Templates auto-generated for all versions in the DB.

---

## visual-tester

**Purpose:** Fabric mod that runs inside Minecraft. Reads `test-commands.json` from the working directory, executes UI actions (open screen, click, type), and saves screenshots to `test-screenshots/`.

**Working directory under Fabric Loom:** `<projectDir>/run/` — all file paths in the mod must be relative to that (i.e., `new File("test-screenshots")` resolves to `run/test-screenshots` — do NOT prefix with `run/`).

**Key source files:**
- `CommandProcessor.java` — polls `test-commands.json` and dispatches actions
- `ScreenshotManager.java` — captures and saves PNG screenshots
- `VisualTesterMod.java` — mod init, registers tick handler

---

## deep-debugger

**Purpose:** Scans mod source, generates fuzz tests, reports unreachable branches and null-risk paths.

**Config:** Copy `debugger-config.example.json` → `debugger-config.json` and fill in `modPath` and `package`.

---

## knowledge-base

Verified migration notes. Never guess — if a version change is not confirmed by an official source, mark it `— **verify before use**` or omit it.

Structure:
```
knowledge-base/
├── minecraft/      # Per-version migration docs (1.16_to_1.17.md, etc.)
└── loaders/
    ├── fabric/versions.md      # Fabric Loader + Fabric API version table
    └── neoforge/versions.md    # NeoForge version table + format notes
```

---

## Important facts to remember

- **26.1 is calendar-versioned** — `YY.drop.hotfix`. First unobfuscated MC release. Requires Java 25.
- **Fabric Loom 26.1 breaking change:** plugin ID `fabric-loom` → `net.fabricmc.fabric-loom`; remove `mappings` line; `modImplementation` → `implementation` for fabric-api.
- **NeoForge 26.1 version format:** four-component — `26.1.0.<build>[-beta]`
- **`KeyMapping.Category`** first appeared in MC 1.21.9 (NeoForge) / 1.21.10 (Fabric). Pre-1.21.9 uses a plain `String` category.
- **`PlayerSkin`** is at `net.minecraft.client.resources` in ≤1.21.8, moved to `net.minecraft.world.entity.player` in 1.21.9+.
- **`GuiGraphics.blit`** takes `RenderType::guiTextured` as first arg in 1.21.4–1.21.5, then it was removed in 1.21.6+.
- **Empty-string replace bug:** never use `String.replace("", something)` — it inserts `something` between every character. Always check that `oldPattern` is non-empty before adding a rule.
- **auto-porter copies first** — `portMod()` always creates `<mod>-ported-<version>` and patches the copy. The original is never touched.
