# mc-mod-porter

A source-backed system for porting Minecraft Java Edition mods across versions — from 1.16 through the 26.x calendar versioning format.

Includes:
- **auto-porter** — CLI tool that applies known API changes automatically
- **visual-tester** — Fabric mod for in-game screenshot capture and automated UI testing
- **deep-debugger** — Static analysis and fuzz-test generator for mod source code
- **knowledge-base** — Verified migration docs, version tables, and patterns

---

## Getting Started

### Step 1 — Clone the repository

```bash
git clone https://github.com/reqsery/mc-mod-porter.git
cd mc-mod-porter
```

### Step 2 — Build the auto-porter

Make sure you have **Java 21+** installed, then:

```bash
cd auto-porter
./gradlew build
```

> Windows: use `gradlew.bat build` instead of `./gradlew build`

The built JAR will be at:
```
auto-porter/build/libs/auto-porter-1.0.0.jar
```

### Step 3 — Check supported versions

```bash
java -jar auto-porter/build/libs/auto-porter-1.0.0.jar --list-versions
```

### Step 4 — Dry run first (always)

Before touching your mod, run a dry run to see exactly what would change:

```bash
java -jar auto-porter/build/libs/auto-porter-1.0.0.jar C:/path/to/mymod 1.20.4 1.20.5 --dry-run
```

This prints every change that would be applied — without modifying any files.

### Step 5 — Port your mod

```bash
java -jar auto-porter/build/libs/auto-porter-1.0.0.jar C:/path/to/mymod 1.20.4 1.20.5
```

Your original mod is never touched. A copy is created at `C:/path/to/mymod-ported-1_20_5`.

The tool will:
1. Update `build.gradle` plugin versions
2. Update `gradle.properties` dependency versions
3. Patch Java source files (renames, import changes, signature updates)
4. Update `fabric.mod.json` / `mods.toml` metadata
5. Attempt a Gradle build and report any remaining errors

### Step 6 — Fix what the tool can't

The auto-porter handles straightforward renames. Anything that requires actual logic changes will show up as a build error. Open the relevant knowledge-base file for that version hop and follow the migration notes, or hand it to an AI (see below).

---

## Using with AI

The knowledge-base files are designed to be pasted directly into an AI prompt as verified context. This prevents hallucination — the AI applies documented changes instead of guessing.

### Recommended AI tools

**Web-based:**
| Tool | Plan to use | Notes |
|------|-------------|-------|
| [Claude](https://claude.ai) | Pro or Max | Best for long files and multi-file context. Max plan handles very large mods. |
| [ChatGPT](https://chat.openai.com) | Plus | Good general-purpose, works well with structured prompts. |
| [Google Gemini](https://gemini.google.com) | Advanced | Very large context window — good for pasting multiple files at once. |

**Desktop / IDE (runs on your PC, integrated into your editor):**
| Tool | Notes |
|------|-------|
| [Cursor](https://cursor.sh) | AI-powered IDE (VS Code fork). Paste the KB file as a rule or in the chat. Pro plan recommended. |
| [Windsurf](https://codeium.com/windsurf) | Similar to Cursor. Free tier available, paid for larger context. |
| [GitHub Copilot](https://github.com/features/copilot) | Integrated into VS Code and JetBrains. Good for file-by-file fixes. |
| [Claude Code](https://claude.ai/claude-code) | Terminal-based agent. Can read your whole mod folder and apply changes autonomously. |

> **Tip:** Premium/paid plans are worth it for porting. Free tiers have short context limits — porting a real mod often requires pasting multiple files at once.

---

### What to tell the AI

Open the knowledge-base file for your version transition (e.g. `knowledge-base/minecraft/1.20.4_to_1.20.5.md`) and paste it along with your source file. Use this prompt structure:

```
I am porting a Fabric mod from [FROM VERSION] to [TO VERSION].

Here is the verified migration guide for this transition:
---
[paste the contents of the knowledge-base file here]
---

Here is my source file:
---
[paste your .java file here]
---

Apply only the changes documented in the migration guide.
Do not guess or invent any API changes not listed above.
```

### Example

```
I am porting a Fabric mod from 1.20.4 to 1.20.5.

Here is the verified migration guide for this transition:
---
[paste knowledge-base/minecraft/1.20.4_to_1.20.5.md]
---

Here is my source file:
---
[paste MyHudOverlay.java]
---

Apply only the changes documented in the migration guide.
Do not guess or invent any API changes not listed above.
```

If you are porting across multiple versions (e.g. 1.19.4 → 1.21), paste each step file in order and apply one at a time.

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

### 1.x Format
`1.16` – `1.16.5` | `1.17.1` | `1.18` – `1.18.2` | `1.19` – `1.19.4` | `1.20` – `1.20.6` | `1.21` – `1.21.11`

### 26.x Format (calendar-based, from 2026)
`26.1` | `26.1.1` | `26.1.2`

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
> Never add version data outside the knowledge-base without a verified source.

See [docs/CONTRIBUTING.md](docs/CONTRIBUTING.md) for the full accuracy policy.

---

## Documentation

| File | What it covers |
|------|----------------|
| [AI_GUIDE.md](AI_GUIDE.md) | How to use this repo with any AI assistant |
| [docs/CONTRIBUTING.md](docs/CONTRIBUTING.md) | How to add or update version data, formatting rules, source requirements |
| [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md) | Common build errors and how to fix them |
| [docs/FAQ.md](docs/FAQ.md) | Why the version format changed, loader vs MC versioning, common questions |

---

## License

MIT — see [LICENSE](LICENSE).
