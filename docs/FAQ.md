# FAQ

---

## Why did Minecraft switch from 1.x to 26.x versioning?

Mojang announced a calendar-based versioning system in December 2025. The new format is:

```
YY.drop.hotfix
```

- `YY` = last two digits of the year (26 = 2026)
- `drop` = content drop number within the year (1, 2, 3...)
- `hotfix` = hotfix number (0 = no hotfix, 1 = first hotfix, etc.)

Examples:
- `26.1` = First content drop of 2026
- `26.1.1` = First hotfix of 26.1
- `26.1.2` = Second hotfix of 26.1
- `26.2` = Second content drop of 2026
- `26.3` = Third content drop of 2026

Source: https://www.minecraft.net/en-us/article/minecraft-new-version-numbering-system

---

## Why is the loader version different from the Minecraft version?

Fabric Loader, Fabric API, and NeoForge all have their own independent versioning:

- **Fabric Loader** handles class loading and mixin injection. It is largely version-agnostic — the same loader (e.g., `0.18.6`) often works across many Minecraft versions.
- **Fabric API** provides common mod APIs. It IS tied to the Minecraft version (e.g., `0.116.9+1.21.1`).
- **NeoForge** versions follow a pattern derived from the MC version (e.g., `21.4.136` for MC 1.21.4).

These are separate projects. A loader update does not mean a Minecraft update, and vice versa.

---

## Why does the porter only use verified data?

Minecraft and its loaders release often, and a wrong method name or version number produces a mod that does not
compile. Every rename rule, version number and migration note in this repository is backed by an official source
(Mojang, FabricMC, NeoForged) and every version number was checked against the official templates and Maven
metadata. Nothing is guessed.

---

## Do I need to recompile my mod for every version?

Not necessarily for older versions, but for 26.x — **yes, always**.

26.1 removed code obfuscation entirely. All mods from 1.21.11 and earlier use class file formats that are incompatible with the unobfuscated 26.x structure. There is **no binary compatibility** — even a mod with zero API usage must be recompiled.

For 1.x → 1.x patches (e.g., 1.21.1 → 1.21.4), you may only need to recompile if API calls changed.

---

## What is Loom and why isn't it in the version tables?

Loom (Fabric Loom / Architectury Loom) is a **Gradle build plugin**, not a runtime dependency. It handles:
- Downloading and deobfuscating Minecraft JARs
- Remapping mod JARs
- Running the development client

It does NOT affect what Minecraft version your mod supports. Loom versions are tracked in `loaders/fabric/versions.md` for reference only.

---

## What happened to Yarn mappings in 26.x?

Yarn (the community mapping project) is no longer officially supported for 26.x. Because 26.1 shipped fully unobfuscated code, there is nothing to remap — official names are available directly. Fabric Loom 1.15+ no longer requires a `mappings` line in `build.gradle`.

Source: https://fabricmc.net/2026/03/14/261.html
