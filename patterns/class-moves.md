# Class Moves & Renames — Cross-Version Pattern Reference

> All entries verified via actual compilation or official sources.
> Do NOT add entries without a source.

---

## GUI / Screen

| Old Package/Class | New Package/Class | Version | Source |
|-------------------|-------------------|---------|--------|
| `net.minecraft.client.gui.screen.Screen` | `net.minecraft.client.gui.screens.Screen` | 1.20.x | Mojang mappings |
| `net.minecraft.client.gui.DrawableHelper` | `net.minecraft.client.gui.GuiGraphics` | 1.19.4 → 1.20.1 | Verified compile |
| `net.minecraft.client.util.math.MatrixStack` | Removed — use GuiGraphics | 1.19.4 → 1.20.1 | Verified compile |

---

## Input

| Old | New | Version | Source |
|-----|-----|---------|--------|
| `net.minecraft.client.input.KeyEvent` | Does not exist | Pre-1.21.10 | Verified compile (not present in 1.21.4) |
| `net.minecraft.client.input.MouseButtonEvent` | Does not exist | Pre-1.21.10 | Verified compile (not present in 1.21.4) |

---

## Player

| Old | New | Version | Source |
|-----|-----|---------|--------|
| `net.minecraft.client.resources.PlayerSkin` | `net.minecraft.world.entity.player.PlayerSkin` | 1.21.4 → 1.21.10 | Verified compile |

---

## Networking

| Old | New | Version | Source |
|-----|-----|---------|--------|
| `net.minecraft.network.PacketByteBuf` | `net.minecraft.network.RegistryFriendlyByteBuf` | 1.20.4 → 1.20.5 | Verified compile |

---

## Forge/NeoForge Package

| Old | New | Version | Source |
|-----|-----|---------|--------|
| `net.minecraftforge.*` | `net.neoforged.*` | 1.20.1 → 1.20.2 | NeoForge fork; verified compile |
