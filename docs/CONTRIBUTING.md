# Contributing

---

## Core Rule

**No guessing. No hallucinations. Source required.**

If you cannot link to an official Mojang, FabricMC, NeoForged, or Gradle source, do not add the entry. Wikis, search snippets, and compile-only claims are not primary evidence.

---

## How to Add a New Version Entry

1. Copy `templates/version-template.md`
2. Rename it: `knowledge-base/minecraft/X.XX_to_Y.YY.md`
3. Fill in only what you have verified
4. Delete any template sections you cannot fill
5. Add a source URL to every entry

---

## Formatting Rules

### Version file header
```markdown
## Version: X.XX → Y.YY

> **Java requirement:** Java XX
> **Source:** [URL]
```

### Each entry
```markdown
- old: `ClassName.methodName(params)`
- new: `NewClass.newMethod(params)`
- source: https://...
```

### Class moves
```markdown
- old: `net.minecraft.old.package.ClassName`
- new: `net.minecraft.new.package.ClassName`
- source: https://...
```

---

## Required Sources

Accepted sources:
- `https://minecraft.net/...` release notes and Mojang launcher/source metadata
- `https://fabricmc.net/...`
- `https://docs.fabricmc.net/...`
- `https://neoforged.net/news/...`
- GitHub commit/PR URLs from: `minecraft/minecraft`, `FabricMC/fabric`, `neoforged/neoforge`
- Official FabricMC and NeoForged Maven metadata

Not accepted:
- "I think"
- "probably"
- Other wikis (unofficial)
- Reddit posts
- Your own mod compiling without a primary source

---

Compilation is validation, not evidence for inventing a version or API change. Record the primary source as well as the build result.

---

## Updating Existing Entries

- If an entry is wrong: fix it and update the source
- If an entry is outdated: add a note with the version range it applies to
- Do NOT delete entries that were true for older versions — mark them with their valid range

---

## What NOT to Add

- Loader version changes in Minecraft change files (keep them in `loaders/`)
- Java language features (keep them in `java/`)
- Mod-specific code that isn't a Minecraft/loader API change
- Hotfix releases with no API changes (just note "no API changes")
