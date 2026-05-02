# mc-mod-porter

A tool for porting Minecraft Java Edition mods across versions — from 1.16 through the 26.x calendar versioning format.

![Status](https://img.shields.io/badge/status-WIP-orange)
## 🚧 Status

Work in progress.

Recently:
- Fixed a lot of bugs in the auto-porter

Next:
- More testing
- Releasing prebuilt JARs soon

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

Includes:
- **auto-porter** — CLI tool that applies known API changes automatically
- **visual-tester** — Fabric mod for in-game screenshot capture and automated UI testing
- **deep-debugger** — Static analysis and fuzz-test generator for mod source code
- **knowledge-base** — Verified migration docs, version tables, and patterns

---

## Using with AI (Recommended)

The easiest way to use this tool is to let an AI run it for you. The best tools for this are ones that run **on your PC inside your editor** — they can read your mod folder, execute the porter, and fix remaining errors automatically.

| Tool | Notes |
|------|-------|
| [Claude Code](https://claude.ai/claude-code) | Terminal agent. Best for autonomous multi-step tasks. **Max plan** handles large mods. |
| [Cursor](https://cursor.sh) | AI-powered VS Code fork. Open your mod, chat with it directly. **Pro plan** recommended. |
| [Windsurf](https://codeium.com/windsurf) | Similar to Cursor. Good free tier, paid for larger context. |
| [GitHub Copilot](https://github.com/features/copilot) | Built into VS Code / JetBrains. Best for fixing individual files. |

> Paid plans are worth it. Free tiers cut off on anything larger than a small mod.

Once you have one set up, see **[AI_GUIDE.md](AI_GUIDE.md)** for the exact workflow and what to say.

---

## Getting Started (Manual)

If you want to run the tool yourself without AI:

### Step 1 — Clone the repository

```bash
git clone https://github.com/reqsery/mc-mod-porter.git
```

This creates a folder called `mc-mod-porter`. Open it in your terminal:

```bash
cd mc-mod-porter
```

### Step 2 — Build the auto-porter

You need **Java 21+** installed. Then:

```bash
cd auto-porter
./gradlew build
```

> Windows: use `gradlew.bat build` instead of `./gradlew build`

The built JAR will be at `auto-porter/build/libs/auto-porter-1.0.0.jar`.

### Step 3 — Check supported versions

```bash
java -jar auto-porter/build/libs/auto-porter-1.0.0.jar --list-versions
```

### Step 4 — Dry run first (always)

See exactly what would change without touching any files:

```bash
java -jar auto-porter/build/libs/auto-porter-1.0.0.jar C:/path/to/mymod 1.20.4 1.20.5 --dry-run
```

### Step 5 — Port your mod

```bash
java -jar auto-porter/build/libs/auto-porter-1.0.0.jar C:/path/to/mymod 1.20.4 1.20.5
```

Your original mod is never modified. The ported copy is created at `C:/path/to/mymod-ported-1_20_5`.

The tool will:
1. Update `build.gradle` plugin versions
2. Update `gradle.properties` dependency versions
3. Patch Java source files (renames, imports, signature changes)
4. Update `fabric.mod.json` / `mods.toml` metadata
5. Attempt a Gradle build and report remaining errors

### Step 6 — Fix remaining errors

The auto-porter handles mechanical changes. Anything requiring logic rewrites shows up as a build error. Open the relevant `knowledge-base/minecraft/` file for that version hop and follow the migration notes — or hand the errors to an AI using the prompt template in [AI_GUIDE.md](AI_GUIDE.md).

---

## How It Works

1. Check `knowledge-base/minecraft/` for breaking API changes between versions
2. Check `patterns/` for quick cross-version rename tables
3. Check `knowledge-base/loaders/` for correct dependency versions
4. Use `java/` for Java toolchain requirements
5. Run `auto-porter` to automatically apply known patterns

---

## Folder Structure

```
auto-porter/        — CLI tool: ports a mod folder to a target MC version
visual-tester/      — Fabric mod: in-game automated UI testing
deep-debugger/      — Static analyzer + fuzz-test generator
scripts/            — Utility scripts (wrappers, validators)

knowledge-base/
  minecraft/        — one file per adjacent version hop
    1.16_to_1.17.md             — KeyMapping rename, addRenderableWidget, SLF4J
    1.17.1_to_1.18.md           — Java 17 required
    1.18_to_1.18.1.md           — No API changes
    1.18.1_to_1.18.2.md         — No API changes
    1.18.2_to_1.19.md           — No API changes
    1.19_to_1.19.1.md           — Chat signing introduced
    1.19.1_to_1.19.2.md         — No API changes
    1.19.2_to_1.19.3.md         — Button.builder, getX/getY, EditBox.setHint
    1.19.3_to_1.19.4.md         — No API changes
    1.19.4_to_1.20.md           — GuiGraphics introduced
    1.20_to_1.20.1.md           — No API changes
    1.20.1_to_1.20.2.md         — NeoForge fork (net.minecraftforge → net.neoforged)
    1.20.2_to_1.20.3.md         — No API changes
    1.20.3_to_1.20.4.md         — No API changes
    1.20.4_to_1.20.5.md         — RegistryFriendlyByteBuf, Java 21, NeoForge HUD API
    1.20.5_to_1.20.6.md         — No API changes
    1.20.6_to_1.21.md           — DeltaTracker introduced in HUD callbacks
    1.21_to_1.21.1.md           — No API changes
    1.21.1_to_1.21.2.md         — No API changes
    1.21.2_to_1.21.3.md         — No API changes
    1.21.3_to_1.21.4.md         — No API changes
    1.21.4_to_1.21.5.md         — No API changes
    1.21.5_to_1.21.6.md         — RenderType::guiTextured removed, player.server private
    1.21.6_to_1.21.7.md         — No API changes
    1.21.7_to_1.21.8.md         — No API changes
    1.21.8_to_1.21.9.md         — PlayerSkin moved, KeyEvent, KeyMapping.Category (NeoForge)
    1.21.9_to_1.21.10.md        — KeyMapping.Category (Fabric side)
    1.21.10_to_1.21.11.md       — ResourceLocation → Identifier
    1.21.11_to_26.1.md          — Java 25, deobfuscation, Loom plugin rename
    26.1_to_26.1.1.md           — Hotfix only, no API changes
    26.1.1_to_26.1.2.md         — Hotfix only, no API changes
  loaders/
    fabric/versions.md          — Fabric Loader + API version table (1.16 → 26.1.x)
    neoforge/versions.md        — NeoForge version table + format explanation

patterns/
  method-renames.md             — Verified method rename table (all versions)
  class-moves.md                — Verified class move/rename table (all versions)
  signatures.md                 — Before/after code for common signature changes

java/
  java17.md                     — Required for Minecraft 1.18+
  java21.md                     — Required for Minecraft 1.20.5+
  java25.md                     — Required for Minecraft 26.1+

docs/
  CONTRIBUTING.md               — Rules for adding data
  TROUBLESHOOTING.md            — Common build errors and fixes
  FAQ.md                        — Common questions

templates/
  version-template.md           — Copy this to add a new version entry
```

---

## Supported Versions

**1.x format:** `1.16` – `1.16.5` · `1.17.1` · `1.18` – `1.18.2` · `1.19` – `1.19.4` · `1.20` – `1.20.6` · `1.21` – `1.21.11`

**26.x format** (calendar versioning, from 2026): `26.1` · `26.1.1` · `26.1.2`

See [docs/FAQ.md](docs/FAQ.md) for why the version format changed at 26.1.

---

## Java Requirements

| Minecraft Versions  | Java Required |
|---------------------|---------------|
| 1.16 – 1.17.x       | Java 16       |
| 1.18 – 1.20.4       | Java 17       |
| 1.20.5 – 1.21.11    | Java 21       |
| 26.1+               | Java 25 (LTS) |

---

## Source of Truth

> **The knowledge-base is the single source of truth.**
> If any rule in `auto-porter` conflicts with a knowledge-base file, the rule must be fixed or removed.

See [docs/CONTRIBUTING.md](docs/CONTRIBUTING.md) for the full accuracy policy.

---

## Documentation

| File | What it covers |
|------|----------------|
| [AI_GUIDE.md](AI_GUIDE.md) | How to use this repo with any AI assistant |
| [docs/CONTRIBUTING.md](docs/CONTRIBUTING.md) | How to add or update version data |
| [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md) | Common build errors and how to fix them |
| [docs/FAQ.md](docs/FAQ.md) | Common questions about versioning and loaders |
