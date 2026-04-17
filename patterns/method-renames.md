# Method Renames — Cross-Version Pattern Reference

> All entries verified via actual compilation or official sources.
> Do NOT add entries without a source.

---

## Input / Controls

| Old | New | Version | Source |
|-----|-----|---------|--------|
| `KeyBinding` | `KeyMapping` | 1.16.5 → 1.17.1 | Mojang mappings; verified compile |
| `KeyMapping(name, type, key, String)` | `KeyMapping(name, type, key, KeyMapping.Category)` | 1.21.4 → 1.21.10 | Verified compile |
| `Screen.keyPressed(int, int, int)` | `Screen.keyPressed(KeyEvent)` | 1.21.4 → 1.21.10 | Verified compile |
| `Screen.mouseClicked(double, double, int)` | `Screen.mouseClicked(MouseButtonEvent, boolean)` | 1.21.4 → 1.21.10 | Verified compile |

---

## Rendering / GUI

| Old | New | Version | Source |
|-----|-----|---------|--------|
| `Screen.addButton(Button)` | `Screen.addRenderableWidget(Widget)` | 1.16.5 → 1.17.1 | Verified compile |
| `render(MatrixStack, int, int, float)` | `render(GuiGraphics, int, int, float)` | 1.19.4 → 1.20.1 | Verified compile |
| `DrawableHelper.fill(...)` | `guiGraphics.fill(...)` | 1.19.4 → 1.20.1 | Verified compile |
| `drawTextWithShadow(...)` | `guiGraphics.drawString(...)` | 1.19.4 → 1.20.1 | Verified compile |
| `blit(RL, x, y, w, h, u0, v0, u1, v1)` | `blit(RenderType::guiTextured, RL, x, y, uPx, vPx, w, h, texW, texH)` | 1.21.4 → 1.21.10 | Verified compile |

---

## Networking

| Old | New | Version | Source |
|-----|-----|---------|--------|
| `PacketByteBuf` | `RegistryFriendlyByteBuf` | 1.20.4 → 1.20.5 | Verified compile |

---

## World / Entity

| Old | New | Version | Source |
|-----|-----|---------|--------|
| `World` | `Level` | 1.17.1 → 1.18.x | Mojang mappings; verified compile |
| `PlayerEntity` | `Player` | 1.17.1 → 1.18.x | Mojang mappings; verified compile |

---

## Player Skin

| Old | New | Version | Source |
|-----|-----|---------|--------|
| `PlayerSkin.texture()` | `PlayerSkin.body().texturePath()` | 1.21.4 → 1.21.10 | Verified compile |

---

## Chunk / World Position

| Old | New | Version | Source |
|-----|-----|---------|--------|
| `new ChunkPos(blockPos)` | `ChunkPos.containing(blockPos)` | 1.21.11 → 26.1 | NeoForge 26.1 release notes |

---

## Logging

| Old | New | Version | Source |
|-----|-----|---------|--------|
| `import org.apache.logging.log4j.Logger` | `import org.slf4j.Logger` | 1.16.x → 1.17+ | Verified compile |
| `LogManager.getLogger(...)` | `LoggerFactory.getLogger(...)` | 1.16.x → 1.17+ | Verified compile |
