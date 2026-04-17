# Troubleshooting

---

## Build Failures

### `error: cannot find symbol — KeyEvent`

**Cause:** `net.minecraft.client.input.KeyEvent` was added in 1.21.10. It does not exist in 1.21.4 or earlier.

**Fix:** Replace `keyPressed(KeyEvent event)` with `keyPressed(int keyCode, int scanCode, int modifiers)`. Use `keyCode` directly instead of `event.key()`.

See: `patterns/signatures.md`

---

### `error: cannot find symbol — MouseButtonEvent`

**Cause:** `net.minecraft.client.input.MouseButtonEvent` was added in 1.21.10. Not present in 1.21.4 or earlier.

**Fix:** Replace `mouseClicked(MouseButtonEvent event, boolean dragging)` with `mouseClicked(double mouseX, double mouseY, int button)`. Remove `event.x()` / `event.y()` calls.

---

### `error: cannot find symbol — KeyMapping.Category`

**Cause:** `KeyMapping.Category` inner class added in 1.21.10. In 1.21.4 and earlier, `KeyMapping` takes a plain `String` category.

**Fix:** Replace `KeyMapping.Category CATEGORY = KeyMapping.Category.register(...)` with `String CATEGORY = "key.categories.misc"`.

---

### `error: no suitable method found for blit(ResourceLocation, int, int, int, int, float, float, float, float)`

**Cause:** `GuiGraphics.blit` signature changed in 1.21.10. In 1.21.4, `blit` requires `RenderType` as first argument and pixel UV (not fractional).

**Fix:**
```java
// 1.21.4 and earlier
graphics.blit(RenderType::guiTextured, skin, x, y, uPixel, vPixel, width, height, texWidth, texHeight);
// 1.21.10+
graphics.blit(skin, x, y, width, height, u0, v0, u1, v1);  // fractional UV
```

---

### `error: cannot find symbol — PlayerSkin` (wrong package)

**Cause:** `PlayerSkin` moved packages between 1.21.4 and 1.21.10.
- 1.21.4: `net.minecraft.client.resources.PlayerSkin`
- 1.21.10+: `net.minecraft.world.entity.player.PlayerSkin`

Also: `.texture()` in 1.21.4 vs `.body().texturePath()` in 1.21.10+.

---

### `error: cannot find symbol — FMLEnvironment.getDist()`

**Cause:** NeoForge changed `getDist()` method to `dist` field in 21.10.x.

**Fix:** `FMLEnvironment.getDist()` → `FMLEnvironment.dist`

---

### `error: package net.minecraftforge does not exist`

**Cause:** NeoForge forked from Forge at 1.20.2. Package renamed from `net.minecraftforge` to `net.neoforged`.

**Fix:** Find/replace `net.minecraftforge` → `net.neoforged` everywhere (imports, annotations, strings).

---

### `error: PacketByteBuf cannot be resolved`

**Cause:** `PacketByteBuf` removed in 1.20.5. Replaced by `RegistryFriendlyByteBuf`.

**Fix:** Replace class name and import.

---

## Mapping Issues

### `UnsupportedClassVersionError` when running auto-porter

**Cause:** The JAR was compiled with Java 21 but you're running with Java 17.

**Fix:** Set `JAVA_HOME` to your Java 21 installation before running:
```
JAVA_HOME="/path/to/jdk-21" java -jar auto-porter.jar ...
```

---

### Gradle can't find NeoForge artifact

**Cause:** NeoForge versions before 21.4.121 use `-beta` suffix. From 21.4.121+, `-beta` is dropped.

**Fix:** Check the exact version at `https://maven.neoforged.net/releases/net/neoforged/neoforge/`

---

## Loader Incompatibility

### Fabric mod loads but crashes immediately

**Cause:** Fabric API module was removed (e.g., `fabric-convention-tags-v1` in 26.x).

**Fix:** Remove the deprecated dependency and update code to the replacement API.

---

### `java.lang.UnsupportedClassVersionError: ... class file version 65.0`

**Cause:** Class file version 65 = Java 21. Running on older JVM.
- Java 17 = class file version 61
- Java 21 = class file version 65
- Java 25 = class file version 69

**Fix:** Use the correct Java version for your Minecraft version. See `java/` folder.

---

## Loom Issues

### `Could not find dev.architectury.loom:1.x-SNAPSHOT`

**Cause:** Architectury Loom versions are tied to specific Maven snapshot repos.

**Fix:** Loom is a build tool, not MC-version-specific. Keep your existing Loom version unless it explicitly doesn't support the target MC version.

---

### Build fails with `silentMojangMappingsLicense() not found`

**Cause:** `silentMojangMappingsLicense()` was removed in Loom 1.11+.

**Fix:** Remove the `loom { silentMojangMappingsLicense() }` block entirely.
