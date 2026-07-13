# Troubleshooting

This is a general troubleshooting guide for ports across supported Minecraft versions. Names differ by mapping namespace: examples labeled **Mojang mappings** use names such as `KeyMapping` and `GuiGraphics`; the same classes are named `KeyBinding` and `DrawContext` in **Yarn**.

## Build failures

### `error: cannot find symbol — KeyEvent`

**Cause:** Minecraft changed GUI keyboard callbacks in **1.21.9**, not 1.21.10. In Mojang mappings the event type is `net.minecraft.client.input.KeyEvent`; Yarn calls the corresponding type `KeyInput`. Minecraft 1.21.8 and earlier use three integer parameters.

**Fix when targeting 1.21.8 or earlier:**

```java
// 1.21.8 and earlier
boolean keyPressed(int keyCode, int scanCode, int modifiers)
```

Use `keyCode` where newer code uses `event.key()` or the equivalent accessor in the selected mappings. When targeting 1.21.9 or later, keep the event-object callback and use the exact name from the selected mappings.

Sources: [Yarn 1.21.4 GUI callbacks](https://maven.fabricmc.net/docs/yarn-1.21.4%2Bbuild.1/net/minecraft/client/gui/Element.html), [Yarn 1.21.9 GUI callbacks](https://maven.fabricmc.net/docs/yarn-1.21.9%2Bbuild.1/net/minecraft/client/gui/Element.html)

### `error: cannot find symbol — MouseButtonEvent`

**Cause:** Mouse callbacks also changed in **1.21.9**. Mojang mappings use `MouseButtonEvent`; Yarn calls the corresponding type `Click`. Minecraft 1.21.8 and earlier use coordinates and a button number.

**Fix when targeting 1.21.8 or earlier:**

```java
// 1.21.8 and earlier
boolean mouseClicked(double mouseX, double mouseY, int button)
```

Replace event coordinate accessors with `mouseX` and `mouseY`. For 1.21.9 or later, retain the event object and verify its mapped name.

Sources: [Yarn 1.21.4 GUI callbacks](https://maven.fabricmc.net/docs/yarn-1.21.4%2Bbuild.1/net/minecraft/client/gui/Element.html), [Yarn 1.21.9 GUI callbacks](https://maven.fabricmc.net/docs/yarn-1.21.9%2Bbuild.1/net/minecraft/client/gui/Element.html)

### `error: cannot find symbol — KeyMapping.Category`

**Cause:** `KeyMapping.Category` (Yarn: `KeyBinding.Category`) was introduced in **1.21.9**. Minecraft 1.21.8 and earlier store the category as a `String`.

**Fix when targeting 1.21.8 or earlier:**

```java
String category = "key.categories.misc";
```

For 1.21.9 or later, use the category record and its registration/creation API in the selected mappings.

Sources: [Yarn 1.21.4 KeyBinding](https://maven.fabricmc.net/docs/yarn-1.21.4%2Bbuild.4/net/minecraft/client/option/KeyBinding.html), [Yarn 1.21.9 KeyBinding.Category](https://maven.fabricmc.net/docs/yarn-1.21.9%2Bbuild.1/net/minecraft/client/option/KeyBinding.Category.html)

### `error: no suitable method found for blit(...)`

**Cause:** GUI texture overloads changed more than once during the 1.21.x rendering work. The previous guide incorrectly treated this as a single 1.21.10 change and reversed the old/new forms.

Minecraft 1.21.4's Mojang-mapped `GuiGraphics.blit` requires a render-type function and pixel UV coordinates:

```java
graphics.blit(
    RenderType::guiTextured,
    texture,
    x, y,
    u, v,
    width, height,
    textureWidth, textureHeight
);
```

Later 1.21.x versions use a `RenderPipeline` for the corresponding overload. Do not convert pixel UVs to fractional UVs unless the target overload explicitly requests them; select the overload from the target version's mappings.

Sources: [Yarn 1.21.4 DrawContext](https://maven.fabricmc.net/docs/yarn-1.21.4%2Bbuild.1/net/minecraft/client/gui/DrawContext.html), [Yarn 1.21.7 DrawContext](https://maven.fabricmc.net/docs/yarn-1.21.7%2Bbuild.1/net/minecraft/client/gui/DrawContext.html), [NeoForged 1.21.5 rendering primer](https://github.com/neoforged/.github/blob/main/primers/1.21.5/index.md)

### `error: cannot find symbol — PlayerSkin` (wrong package)

**Cause:** `PlayerSkin` moved and changed shape in **1.21.9**:

- 1.21.8 and earlier: `net.minecraft.client.resources.PlayerSkin`
- 1.21.9 and later: `net.minecraft.world.entity.player.PlayerSkin`

The old `texture()`/`textureUrl()` representation became a `body()` client texture. Code that needs the resource path generally uses `skin.body().texturePath()` after the migration.

Source: [NeoForged 1.21.8 → 1.21.9 migration primer](https://docs.neoforged.net/primer/docs/1.21.9/)

### `error: cannot find symbol — FMLEnvironment.getDist()`

**Cause:** This API belongs to FancyModLoader, so its shape follows the FML version rather than vanilla Minecraft mappings. NeoForge's official 1.21.1 documentation uses the public `FMLEnvironment.dist` field. Newer FML source may expose `getDist()` instead. The previous guide's claim that 21.10 changed `getDist()` into `dist` was backwards.

**Fix:** For a target whose FML API exposes the field, use:

```java
FMLEnvironment.dist
```

For a target whose FML API exposes the accessor, use:

```java
FMLEnvironment.getDist()
```

Do not apply this as an unconditional text replacement; check the FML dependency resolved by the target NeoForge artifact.

Sources: [NeoForge 1.21.1 sides documentation](https://docs.neoforged.net/docs/1.21.1/concepts/sides/), [FancyModLoader source](https://github.com/neoforged/FancyModLoader)

### `error: package net.minecraftforge does not exist`

**Cause:** NeoForge forked from Forge in the 1.20.2 development cycle and renamed its own packages from `net.minecraftforge` to `net.neoforged`.

**Fix:** Update imports and loader-owned references using the actual NeoForge class locations. Do not replace every occurrence blindly: Maven coordinates, plugin IDs, metadata, service declarations, third-party APIs, and user-facing text do not all follow one package substitution.

Sources: [NeoForge source](https://github.com/neoforged/NeoForge), [NeoForged migration primers](https://github.com/neoforged/.github/tree/main/primers)

### `error: PacketByteBuf cannot be resolved`

**Cause:** Usually a mapping-namespace mismatch, not removal in Minecraft 1.20.5. `PacketByteBuf` is the Yarn name. In 1.20.5 it still exists, and Yarn's `RegistryByteBuf` extends it. Mojang mappings call the corresponding registry-aware type `RegistryFriendlyByteBuf`.

**Fix:**

- Stay within one mapping namespace.
- Use the base packet buffer for ordinary data.
- Use the registry-aware buffer only when serialization needs registry access.
- Do not globally replace every `PacketByteBuf` with `RegistryFriendlyByteBuf`.

Sources: [Yarn 1.20.5 RegistryByteBuf](https://maven.fabricmc.net/docs/yarn-1.20.5-rc2%2Bbuild.1/net/minecraft/network/RegistryByteBuf.html), [NeoForged 1.20.5 migration primer](https://github.com/neoforged/.github/blob/main/primers/1.20.5/index.md#stream-codecs)

## Java and mapping issues

### `invalid source release: 21`

The selected JDK is older than the configured Java toolchain.

1. Run `java -version`, `javac -version`, and `./gradlew --version`.
2. Install the required JDK.
3. Point `JAVA_HOME` at it and reopen the terminal.

Do not lower the project's Java target merely to make an older local JDK compile it.

### `UnsupportedClassVersionError`

The runtime is older than the Java version used to compile the class. Common class-file major versions are:

- `61`: Java 17
- `65`: Java 21
- `69`: Java 25

Run the tool or mod with a compatible JDK. Auto-Porter itself is configured for Java 21; Minecraft 26.2 development requires Java 25.

Source: [Java Virtual Machine Specification, class-file format](https://docs.oracle.com/javase/specs/jvms/se25/html/jvms-4.html)

## Dependency resolution

### Gradle cannot find a NeoForge artifact

NeoForge `21.4.0-beta` through `21.4.120-beta` use the `-beta` suffix. Starting at `21.4.121`, the published 21.4 artifacts omit it. This cutoff is specific to the 21.4 line; do not generalize it to other Minecraft/NeoForge lines.

Always copy the exact published version from the official repository: [NeoForged Maven releases](https://maven.neoforged.net/releases/net/neoforged/neoforge/)

Then refresh dependencies:

```bash
./gradlew --refresh-dependencies
```

### A Fabric API module cannot be found

Fabric API modules can be deprecated, replaced, or removed. For example, current 26.x Fabric API source contains `fabric-convention-tags-v2`, not `fabric-convention-tags-v1`.

**Fix:** Check the exact Fabric API release/source tree. If a major-version replacement exists, migrate its API and data semantics; merely renaming or deleting the dependency may leave broken imports or tags.

Sources: [Fabric API 26.2 source tree](https://github.com/FabricMC/fabric-api/tree/26.2), [Fabric API releases](https://github.com/FabricMC/fabric-api/releases)

## Loom issues

### `Could not find dev.architectury.loom:...-SNAPSHOT`

**Cause:** The requested Architectury Loom snapshot is not available from the configured plugin/Maven repositories, or that version was never published there.

**Fix:** Verify the version against Architectury Loom's official releases and configure the repository documented by that project. Do not assume an arbitrary Loom version supports every target: Loom is not tied one-to-one to a Minecraft version, but compatibility changes with Minecraft, Gradle, Java, mappings, and loader tooling.

Source: [Architectury Loom](https://github.com/architectury/architectury-loom)

### Fabric Loom fails after changing Minecraft versions

Use the Loom line required by Fabric for the target Minecraft version and then select an actually published build from Fabric Maven. For Minecraft 26.2, Fabric requires Loom 1.17; this repository's verified preset uses `1.17.13`.

Sources: [Fabric development for Minecraft 26.2](https://fabricmc.net/2026/06/15/262.html), [Fabric Loom Maven](https://maven.fabricmc.net/net/fabricmc/fabric-loom/)

### `silentMojangMappingsLicense() not found`

**Cause:** The selected Fabric/Architectury Loom version does not expose this older DSL helper. The helper suppresses the Mojang mappings license prompt; it does not select the mappings dependency.

**Fix:** Remove only the `silentMojangMappingsLicense()` call. Keep the actual mappings declaration, such as `mappings loom.officialMojangMappings()`, when the project uses Mojang mappings. Do not claim a fixed removal boundary without checking the selected Loom implementation.

Sources: [Fabric Loom source](https://github.com/FabricMC/fabric-loom), [Architectury Loom source](https://github.com/architectury/architectury-loom)

## Minecraft 26.2 pack metadata

Minecraft Java Edition 26.2 uses:

- Data Pack version `107.1`
- Resource Pack version `88.0`

Snapshot pack versions are provisional; use the final release notes for a release target.

Source: [Minecraft Java Edition 26.2](https://www.minecraft.net/en-us/article/minecraft-java-edition-26-2)

## Gradle cache and daemon problems

First stop daemons and retry with refreshed dependencies:

```bash
./gradlew --stop
./gradlew clean build --refresh-dependencies
```

On Windows, also check whether an IDE, antivirus scanner, or another Gradle process holds a file lock. Avoid deleting the entire global Gradle cache unless the error identifies corrupt cached content and a refresh does not repair it.

## Reporting an unresolved failure

Include:

- source and target Minecraft versions;
- mapping namespace;
- loader and exact loader version;
- Fabric API or NeoForge version;
- Loom and Gradle versions;
- `java -version` and `./gradlew --version` output;
- the first relevant compiler or dependency-resolution error;
- the generated `gradle.properties` and dependency block.
