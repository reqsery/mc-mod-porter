---
name: How to Contribute
---

# Contributing to mc-mod-porter

Thanks for wanting to help! 🎉 Here's how you can contribute.

## 👋 First Time?

Start here:
1. **Read** the [README](../README.md) to understand what this tool does
2. **Check** existing [issues](../../issues) and [discussions](../../discussions) to see what needs help
3. **Try it** — port a mod yourself to understand the tool
4. **Pick a task** — check for `good first issue` or `help wanted` labels

## 🐛 Found a Bug?

Open a [Bug Report](./ISSUE_TEMPLATE/bug_report.md). Include:
- Your environment (MC version, loader, Java version)
- Steps to reproduce
- What you expected vs. what actually happened
- Any error messages

## 💡 Have an Idea?

Open a [Feature Request](./ISSUE_TEMPLATE/feature_request.md). Tell us:
- The problem you're solving
- Your proposed solution
- Why it matters

## 🔧 Want to Code?

### Local Setup

#### Prerequisites
- **Git**
- **Java 21+** (required to build)
- **Gradle** (included via `gradlew`)

#### Clone & Build

```bash
# Clone the repo
git clone https://github.com/reqsery/mc-mod-porter.git
cd mc-mod-porter

# List available tasks
./gradlew tasks

# Build the auto-porter JAR
cd auto-porter
./gradlew build

# The JAR is now at: auto-porter/build/libs/auto-porter-1.0.0.jar
```

**Windows?** Use `gradlew.bat` instead of `./gradlew`.

#### Test Your Build

```bash
# List supported versions
java -jar auto-porter/build/libs/auto-porter-1.0.0.jar --list-versions

# Test a dry run (no files modified)
java -jar auto-porter/build/libs/auto-porter-1.0.0.jar /path/to/test/mod 1.20.4 1.20.5 --dry-run
```

### Project Structure

```
auto-porter/        ← CLI tool that does the porting
  src/main/java/    ← Core logic
  src/test/java/    ← Unit tests
  build.gradle      ← Build config

visual-tester/      ← Fabric mod for testing (optional)
deep-debugger/      ← Static analyzer (optional)

knowledge-base/     ← Migration reference data (see docs/CONTRIBUTING.md)
  minecraft/        ← Version migration files
  loaders/          ← Fabric/NeoForge version tables
```

### Making Changes

1. **Create a branch** from `main`
   ```bash
   git checkout -b feature/my-feature
   # or
   git checkout -b fix/bug-name
   ```

2. **Write your changes**
   - Keep commits focused and descriptive
   - One feature/fix per branch

3. **Run tests** (if applicable)
   ```bash
   cd auto-porter
   ./gradlew test
   ```

4. **Test manually** with a real mod
   ```bash
   ./gradlew build
   java -jar build/libs/auto-porter-1.0.0.jar /test/mod 1.20.4 1.20.5
   ```

5. **Push & open a PR**
   ```bash
   git push origin feature/my-feature
   ```
   Then open a PR at [github.com/reqsery/mc-mod-porter/pulls](../../pulls)

### Adding Knowledge Base Data

If you're adding **version migration data** (new Minecraft version supported):

1. Copy `templates/version-template.md`
2. Save as `knowledge-base/minecraft/X.XX_to_Y.YY.md`
3. Fill in only what you've verified from official sources
4. Include source URLs for every entry (Mojang, Fabric docs, NeoForge changelogs, etc.)
5. See [docs/CONTRIBUTING.md](../../docs/CONTRIBUTING.md) for the full spec

**Golden Rule:** No guessing. No hallucinations. Source required. ✅

## 📝 Pull Request Guidelines

When you open a PR:

- **Title:** Clear and concise (e.g., `Fix: Handle GuiGraphics null in 1.19.4 → 1.20 ports`)
- **Description:** Explain *what* and *why*, not just *how*
- **Link issues:** Write `Fixes #123` if it closes an issue
- **Test results:** Show what you tested (dry-run output, manual test results, etc.)

**Example:**
```markdown
## What?
Adds support for the new GuiGraphics API in 1.20 auto-patching.

## Why?
Currently crashes on GuiGraphics.drawCenteredString → crash. This fixes it by 
recognizing the signature change and rewriting calls automatically.

## How tested?
- Ported a test Fabric mod from 1.19.4 → 1.20 ✅
- Ran dry-run on 5 different mods ✅
- Manual inspection of patch output looks correct ✅

Fixes #42
```

## ❓ Questions?

- **Discord:** [https://discord.gg/vV2USr9phF](https://discord.gg/vV2USr9phF)
- **Discussions:** [github.com/reqsery/mc-mod-porter/discussions](../../discussions)
- **Docs:** [docs/FAQ.md](../../docs/FAQ.md) and [docs/TROUBLESHOOTING.md](../../docs/TROUBLESHOOTING.md)

---

## 📚 Resources

| Resource | Purpose |
|----------|----------|
| [README](../README.md) | Quick start & overview |
| [docs/FAQ.md](../../docs/FAQ.md) | Common questions |
| [docs/TROUBLESHOOTING.md](../../docs/TROUBLESHOOTING.md) | Build errors & fixes |
| [docs/CONTRIBUTING.md](../../docs/CONTRIBUTING.md) | Data contribution rules |
| [AI_GUIDE.md](../../AI_GUIDE.md) | Using this repo with AI assistants |

---

## 🚀 Ways to Contribute (Besides Code)

- **Test** the tool on your mods and report issues
- **Improve docs** — typos, unclear sections, missing examples?
- **Add migration data** — port a mod, verified it works? Add it to the knowledge-base
- **Share** — tell modders about this tool on Discord, Reddit, or Twitter
- **Ideas** — open a discussion with your thoughts

**All contributions are valued!** 💚