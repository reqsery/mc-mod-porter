package com.autoporter;

import java.util.*;

/**
 * Represents a single API rename/change rule between two MC versions.
 * Used by the SourcePatcher to apply refactors.
 *
 * NOTE: ApiChangeRule is temporary and should eventually be generated from the knowledge-base
 * (knowledge-base/minecraft/*.md). Rules here are manually kept in sync with the KB files.
 * When adding new version support, add a KB file first, then mirror the changes here.
 */
public record ApiChangeRule(
    String fromVersion,
    String toVersion,
    RuleType type,
    String oldPattern,
    String newPattern,
    String description
) {
    public enum RuleType {
        CLASS_RENAME,
        METHOD_RENAME,
        METHOD_SIGNATURE,
        FIELD_RENAME,
        IMPORT_CHANGE,
        MIXIN_TARGET,
        TEXT_REPLACE
    }

    /** All known API change rules across supported version ranges. */
    public static List<ApiChangeRule> all() {
        List<ApiChangeRule> rules = new ArrayList<>();

        // ── 1.16 → 1.17 ────────────────────────────────────────────────────
        addBidirectional(rules, "1.16.5", "1.17.1", RuleType.METHOD_SIGNATURE,
            "KeyBinding(",
            "KeyMapping(",
            "KeyBinding renamed to KeyMapping in 1.17");
        // addButton → addRenderableWidget in 1.17 (Screen widget registration)
        addBidirectional(rules, "1.16.5", "1.17.1", RuleType.TEXT_REPLACE,
            "addButton(",
            "addRenderableWidget(",
            "Screen.addButton() → addRenderableWidget() in 1.17");
        // SLF4J introduced in 1.17; 1.16 used Log4j directly
        rules.add(new ApiChangeRule("1.17.1", "1.16.5", RuleType.IMPORT_CHANGE,
            "import org.slf4j.Logger;",
            "import org.apache.logging.log4j.Logger;",
            "SLF4J Logger → Log4j Logger for pre-1.17"));
        rules.add(new ApiChangeRule("1.17.1", "1.16.5", RuleType.IMPORT_CHANGE,
            "import org.slf4j.LoggerFactory;",
            "import org.apache.logging.log4j.LogManager;",
            "SLF4J LoggerFactory → Log4j LogManager for pre-1.17"));
        rules.add(new ApiChangeRule("1.17.1", "1.16.5", RuleType.TEXT_REPLACE,
            "LoggerFactory.getLogger(",
            "LogManager.getLogger(",
            "LoggerFactory.getLogger → LogManager.getLogger for pre-1.17"));
        rules.add(new ApiChangeRule("1.16.5", "1.17.1", RuleType.IMPORT_CHANGE,
            "import org.apache.logging.log4j.Logger;",
            "import org.slf4j.Logger;",
            "Log4j Logger → SLF4J Logger in 1.17+"));
        rules.add(new ApiChangeRule("1.16.5", "1.17.1", RuleType.IMPORT_CHANGE,
            "import org.apache.logging.log4j.LogManager;",
            "import org.slf4j.LoggerFactory;",
            "Log4j LogManager → SLF4J LoggerFactory in 1.17+"));
        rules.add(new ApiChangeRule("1.16.5", "1.17.1", RuleType.TEXT_REPLACE,
            "LogManager.getLogger(",
            "LoggerFactory.getLogger(",
            "LogManager.getLogger → LoggerFactory.getLogger in 1.17+"));
        // RenderSystem.setShaderTexture introduced in 1.17 (shader-based rendering)
        rules.add(new ApiChangeRule("1.17.1", "1.16.5", RuleType.TEXT_REPLACE,
            "RenderSystem.setShaderTexture(0, skin);",
            "Minecraft.getInstance().getTextureManager().bind(skin);",
            "RenderSystem.setShaderTexture → TextureManager.bind for pre-1.17"));
        rules.add(new ApiChangeRule("1.16.5", "1.17.1", RuleType.TEXT_REPLACE,
            "Minecraft.getInstance().getTextureManager().bind(skin);",
            "RenderSystem.setShaderTexture(0, skin);",
            "TextureManager.bind → RenderSystem.setShaderTexture in 1.17+"));

        // ── 1.17 → 1.18 ────────────────────────────────────────────────────
        addBidirectional(rules, "1.17.1", "1.18.2", RuleType.TEXT_REPLACE,
            "net.minecraft.world.World",
            "net.minecraft.world.level.Level",
            "World → Level");
        addBidirectional(rules, "1.17.1", "1.18.2", RuleType.TEXT_REPLACE,
            "net.minecraft.entity.player.PlayerEntity",
            "net.minecraft.world.entity.player.Player",
            "PlayerEntity → Player");

        // ── 1.18 → 1.19 ────────────────────────────────────────────────────
        // Component.literal() introduced in 1.19.0; pre-1.19 uses new TextComponent()
        rules.add(new ApiChangeRule("1.19.0", "1.18.2", RuleType.TEXT_REPLACE,
            "Component.literal(",
            "new net.minecraft.network.chat.TextComponent(",
            "Component.literal() → new TextComponent() for pre-1.19"));
        rules.add(new ApiChangeRule("1.18.2", "1.19.0", RuleType.TEXT_REPLACE,
            "new net.minecraft.network.chat.TextComponent(",
            "Component.literal(",
            "new TextComponent() → Component.literal() in 1.19+"));
        rules.add(new ApiChangeRule("1.19.0", "1.18.2", RuleType.IMPORT_CHANGE,
            "import net.minecraft.network.chat.Component;",
            "",
            "Remove Component import when downgrading to pre-1.19 (TextComponent is fully qualified)"));
        // rebuildWidgets() introduced in 1.19.x; pre-1.19 uses init(minecraft, width, height)
        rules.add(new ApiChangeRule("1.19.0", "1.18.2", RuleType.TEXT_REPLACE,
            "this.rebuildWidgets();",
            "this.init(this.minecraft, this.width, this.height);",
            "Screen.rebuildWidgets() → init(minecraft, width, height) for pre-1.19"));
        rules.add(new ApiChangeRule("1.18.2", "1.19.0", RuleType.TEXT_REPLACE,
            "this.init(this.minecraft, this.width, this.height);",
            "this.rebuildWidgets();",
            "init(minecraft,width,height) → rebuildWidgets() in 1.19+"));

        // ── 1.19 → 1.20 (GuiGraphics introduced) ──────────────────────────
        // GuiGraphics replaced PoseStack as the render parameter in 1.20
        addBidirectional(rules, "1.19.4", "1.20.1", RuleType.IMPORT_CHANGE,
            "import com.mojang.blaze3d.vertex.PoseStack;",
            "import net.minecraft.client.gui.GuiGraphics;",
            "PoseStack import → GuiGraphics import in render methods");
        addBidirectional(rules, "1.19.4", "1.20.1", RuleType.METHOD_SIGNATURE,
            "render(PoseStack poseStack,",
            "render(GuiGraphics graphics,",
            "render(PoseStack) → render(GuiGraphics) in Screen.render()");
        addBidirectional(rules, "1.19.4", "1.20.1", RuleType.TEXT_REPLACE,
            "super.render(poseStack, mouseX, mouseY, delta);",
            "super.render(graphics, mouseX, mouseY, delta);",
            "super.render(poseStack) → super.render(graphics)");
        addBidirectional(rules, "1.19.4", "1.20.1", RuleType.TEXT_REPLACE,
            "fill(poseStack, ",
            "graphics.fill(",
            "fill(poseStack,...) → graphics.fill(...) in 1.20");
        addBidirectional(rules, "1.19.4", "1.20.1", RuleType.TEXT_REPLACE,
            "drawCenteredString(poseStack, this.font,",
            "graphics.drawCenteredString(this.font,",
            "drawCenteredString(poseStack,...) → graphics.drawCenteredString(...)");
        addBidirectional(rules, "1.19.4", "1.20.1", RuleType.TEXT_REPLACE,
            "drawString(poseStack, this.font,",
            "graphics.drawString(this.font,",
            "drawString(poseStack,...) → graphics.drawString(...)");
        // HUD render callback: PoseStack+float → GuiGraphics+DeltaTracker in 1.20
        rules.add(new ApiChangeRule("1.19.4", "1.20.1", RuleType.TEXT_REPLACE,
            "(poseStack, tickDelta) ->",
            "(graphics, deltaTracker) ->",
            "HUD callback: (PoseStack, float) → (GuiGraphics, DeltaTracker)"));
        rules.add(new ApiChangeRule("1.20.1", "1.19.4", RuleType.TEXT_REPLACE,
            "(graphics, deltaTracker) ->",
            "(poseStack, tickDelta) ->",
            "HUD callback: (GuiGraphics, DeltaTracker) → (PoseStack, float)"));

        // ── 1.19.2 → 1.19.3 (Button.builder, Widget getters, EditBox.setHint) ──
        // Button.builder() fluent API was added in 1.19.3; older versions use constructor
        rules.add(new ApiChangeRule("1.19.4", "1.19.2", RuleType.TEXT_REPLACE,
            "Button.builder(",
            "new Button(",
            "Button.builder() → new Button() constructor for pre-1.19.3 (reorder args: move .bounds() values to start)"));
        // Widget getX()/getY() public getters added in 1.19.3; older uses public fields
        addBidirectional(rules, "1.19.2", "1.19.3", RuleType.TEXT_REPLACE,
            ".getX()",
            ".x",
            "AbstractWidget.getX() → .x field (pre-1.19.3 had public int x)");
        addBidirectional(rules, "1.19.2", "1.19.3", RuleType.TEXT_REPLACE,
            ".getY()",
            ".y",
            "AbstractWidget.getY() → .y field (pre-1.19.3 had public int y)");
        // EditBox.setHint() added in 1.19.3; remove in older versions
        rules.add(new ApiChangeRule("1.19.3", "1.19.2", RuleType.TEXT_REPLACE,
            ".setHint(Component.literal(",
            "// .setHint removed (not available pre-1.19.3)",
            "EditBox.setHint() not available pre-1.19.3; remove call"));
        // setFocused(boolean) protected in pre-1.19.3; use Screen.setFocused(listener)
        rules.add(new ApiChangeRule("1.19.3", "1.19.2", RuleType.TEXT_REPLACE,
            "nameField.setFocused(true);",
            "this.setFocused(nameField);",
            "setFocused(boolean) protected pre-1.19.3; use Screen.setFocused(GuiEventListener)"));

        // ── 1.19.2 → 1.19.1 (GuiMessageTag.systemSinglePlayer removed) ──────
        // GuiMessageTag.systemSinglePlayer() added after 1.19.2
        rules.add(new ApiChangeRule("1.19.2", "1.19.1", RuleType.TEXT_REPLACE,
            " || tag == GuiMessageTag.systemSinglePlayer()",
            "",
            "GuiMessageTag.systemSinglePlayer() not in 1.19.1; remove the check"));
        // GuiMessageTag itself doesn't exist in 1.19.0 - need full mixin rewrite
        rules.add(new ApiChangeRule("1.19.1", "1.19.0", RuleType.MIXIN_TARGET,
            "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
            "addMessage(Lnet/minecraft/network/chat/Component;)V",
            "GuiMessageTag not in 1.19.0; target 1-arg addMessage instead"));

        // ── 1.20.4 → 1.20.5 (Networking API) ─────────────────────────────────
        // FriendlyByteBuf (pre-1.20.5) vs RegistryFriendlyByteBuf (1.20.5+)
        // Note: Mojang mappings use FriendlyByteBuf, not PacketByteBuf
        // IMPORTANT: The specific constructor rule MUST come before the general FriendlyByteBuf
        // rule. The general rule "FriendlyByteBuf" → "RegistryFriendlyByteBuf" matches as a
        // substring — if it fires first, the specific constructor rule can never match.
        rules.add(new ApiChangeRule("1.20.4", "1.20.5", RuleType.TEXT_REPLACE,
            "new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer())",
            "new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), player.registryAccess())",
            "RegistryFriendlyByteBuf requires RegistryAccess param in 1.20.5+ (must be before general rule)"));
        addBidirectional(rules, "1.20.4", "1.20.5", RuleType.IMPORT_CHANGE,
            "net.minecraft.network.FriendlyByteBuf",
            "net.minecraft.network.RegistryFriendlyByteBuf",
            "FriendlyByteBuf import change for 1.20.5");
        addBidirectional(rules, "1.20.4", "1.20.5", RuleType.TEXT_REPLACE,
            "FriendlyByteBuf",
            "RegistryFriendlyByteBuf",
            "FriendlyByteBuf → RegistryFriendlyByteBuf in 1.20.5 (Mojang mappings)");

        // ── 1.20.1 → 1.20.2 (PlayerSkin introduced) ──────────────────────────
        // PlayerSkin record was added in 1.20.2; pre-1.20.2 uses getSkinTextureLocation()
        rules.add(new ApiChangeRule("1.20.2", "1.20.1", RuleType.TEXT_REPLACE,
            "acp.getSkin().texture()",
            "acp.getSkinTextureLocation()",
            "PlayerSkin.texture() → getSkinTextureLocation() pre-1.20.2"));
        rules.add(new ApiChangeRule("1.20.1", "1.20.2", RuleType.TEXT_REPLACE,
            "acp.getSkinTextureLocation()",
            "acp.getSkin().texture()",
            "getSkinTextureLocation() → PlayerSkin.texture() in 1.20.2+"));
        rules.add(new ApiChangeRule("1.20.2", "1.20.1", RuleType.IMPORT_CHANGE,
            "import net.minecraft.client.resources.PlayerSkin;",
            "",
            "Remove PlayerSkin import (not available pre-1.20.2)"));

        // ── 1.20.6 → 1.21 (DeltaTracker introduced) ──────────────────────────
        // DeltaTracker class added in MC 1.21; pre-1.21 uses float tickDelta
        rules.add(new ApiChangeRule("1.21", "1.20.6", RuleType.IMPORT_CHANGE,
            "import net.minecraft.client.DeltaTracker;",
            "",
            "DeltaTracker not available pre-1.21; remove import"));
        // Note: HUD callback lambda body is mod-specific (depends on method called).
        // The deltaTracker.getGameTimeDeltaPartialTick(true) → tickDelta conversion
        // must be done manually per-mod. Only the import is handled generically above.

        // ── NeoForge HUD API changes ───────────────────────────────────────────
        // NeoForge 1.20.3/1.20.4: RegisterGuiOverlaysEvent + IGuiOverlay(Gui,Graphics,float,w,h)
        // NeoForge 1.20.5/1.20.6: RegisterGuiLayersEvent + LayeredDraw.Layer(Graphics,float)
        // NeoForge 1.21+: RegisterGuiLayersEvent + LayeredDraw.Layer(Graphics,DeltaTracker)
        addBidirectional(rules, "1.20.4", "1.20.5", RuleType.IMPORT_CHANGE,
            "net.neoforged.neoforge.client.event.RegisterGuiOverlaysEvent",
            "net.neoforged.neoforge.client.event.RegisterGuiLayersEvent",
            "NeoForge: RegisterGuiOverlaysEvent → RegisterGuiLayersEvent in 1.20.5");
        // NeoForge HUD lambda body is mod-specific — only the event class import is handled generically.
        // NeoForge ClientTickEvent API changed in 1.20.5
        addBidirectional(rules, "1.20.4", "1.20.5", RuleType.IMPORT_CHANGE,
            "net.neoforged.neoforge.event.TickEvent",
            "net.neoforged.neoforge.client.event.ClientTickEvent",
            "NeoForge: TickEvent → ClientTickEvent package in 1.20.5");
        // Note: The TickEvent body (Phase.END check) must be migrated manually per-mod.

        // ── 1.19 → 1.20 ────────────────────────────────────────────────────
        addBidirectional(rules, "1.19.4", "1.20.1", RuleType.TEXT_REPLACE,
            "import net.minecraft.client.util.math.MatrixStack",
            "// MatrixStack removed - use GuiGraphics",
            "MatrixStack replaced by GuiGraphics in render methods");
        addBidirectional(rules, "1.19.4", "1.20.1", RuleType.METHOD_SIGNATURE,
            "render(MatrixStack matrices,",
            "render(GuiGraphics guiGraphics,",
            "render() MatrixStack → GuiGraphics");
        addBidirectional(rules, "1.19.4", "1.20.1", RuleType.IMPORT_CHANGE,
            "net.minecraft.client.gui.DrawableHelper",
            "net.minecraft.client.gui.GuiGraphics",
            "DrawableHelper → GuiGraphics");
        // NOTE: drawTextWithShadow and fill are already handled above with
        // specific poseStack-parameterized patterns. Do NOT add a generic
        // "fill(" → "guiGraphics.fill(" rule — it would corrupt already-converted
        // "graphics.fill(" calls since String.replace matches substrings.

        // ── 1.20.1 → 1.20.2 (NeoForge split) ─────────────────────────────
        addBidirectional(rules, "1.20.1", "1.20.2", RuleType.IMPORT_CHANGE,
            "net.minecraftforge",
            "net.neoforged",
            "Forge → NeoForge package rename");
        addBidirectional(rules, "1.20.1", "1.20.2", RuleType.TEXT_REPLACE,
            "minecraftforge",
            "neoforged",
            "minecraftforge → neoforged");

        // ── 1.20.4 → 1.20.5 (Networking API) ─────────────────────────────
        addBidirectional(rules, "1.20.4", "1.20.5", RuleType.TEXT_REPLACE,
            "PacketByteBuf",
            "RegistryFriendlyByteBuf",
            "PacketByteBuf → RegistryFriendlyByteBuf for registry-aware packets");
        addBidirectional(rules, "1.20.4", "1.20.5", RuleType.IMPORT_CHANGE,
            "net.minecraft.network.PacketByteBuf",
            "net.minecraft.network.RegistryFriendlyByteBuf",
            "PacketByteBuf import change");

        // ── 1.21 → 1.21.1 ─────────────────────────────────────────────────
        addBidirectional(rules, "1.21", "1.21.1", RuleType.TEXT_REPLACE,
            "minecraft ~1.21\"",
            "minecraft ~1.21.1\"",
            "fabric.mod.json minecraft version constraint");

        // ── 1.21.1 → 1.21.4 ───────────────────────────────────────────────
        addBidirectional(rules, "1.21.1", "1.21.4", RuleType.TEXT_REPLACE,
            "minecraft ~1.21.1\"",
            "minecraft ~1.21.4\"",
            "fabric.mod.json minecraft version constraint");

        // ── 1.21.10 → 1.21.4 (downgrade: KeyEvent, KeyMapping.Category, PlayerSkin) ──
        // KeyEvent: introduced in 1.21.10; 1.21.4 uses int params for keyPressed
        rules.add(new ApiChangeRule("1.21.10", "1.21.4", RuleType.TEXT_REPLACE,
            "import net.minecraft.client.input.KeyEvent;\n",
            "",
            "Remove KeyEvent import (not in 1.21.4)"));
        rules.add(new ApiChangeRule("1.21.10", "1.21.4", RuleType.TEXT_REPLACE,
            "public boolean keyPressed(KeyEvent event) {",
            "public boolean keyPressed(int keyCode, int scanCode, int modifiers) {",
            "keyPressed(KeyEvent) → keyPressed(int,int,int) in 1.21.4"));
        rules.add(new ApiChangeRule("1.21.10", "1.21.4", RuleType.TEXT_REPLACE,
            "event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER",
            "keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER",
            "event.key() → keyCode in keyPressed body"));
        rules.add(new ApiChangeRule("1.21.10", "1.21.4", RuleType.TEXT_REPLACE,
            "return super.keyPressed(event);",
            "return super.keyPressed(keyCode, scanCode, modifiers);",
            "super.keyPressed(KeyEvent) → super.keyPressed(int,int,int)"));
        // NOTE: KeyMapping.Category → String conversion depends on the mod's category field name.
        // This cannot be handled generically — do this manually: replace
        //   KeyMapping.Category MY_CAT = KeyMapping.Category.register(ResourceLocation.fromNamespaceAndPath(...))
        // with:
        //   String MY_CAT = "key.categories.misc"
        // and remove the ResourceLocation import if no longer needed.
        // PlayerSkin: net.minecraft.world.entity.player → net.minecraft.client.resources in 1.21.4
        rules.add(new ApiChangeRule("1.21.10", "1.21.4", RuleType.TEXT_REPLACE,
            "import net.minecraft.world.entity.player.PlayerSkin;",
            "import net.minecraft.client.resources.PlayerSkin;",
            "PlayerSkin: world.entity.player → client.resources in 1.21.4"));
        // MouseButtonEvent: introduced in 1.21.10; 1.21.4 uses (double,double,int)
        rules.add(new ApiChangeRule("1.21.10", "1.21.4", RuleType.TEXT_REPLACE,
            "import net.minecraft.client.input.MouseButtonEvent;\n",
            "",
            "Remove MouseButtonEvent import (not in 1.21.4)"));
        rules.add(new ApiChangeRule("1.21.10", "1.21.4", RuleType.TEXT_REPLACE,
            "public boolean mouseClicked(MouseButtonEvent event, boolean dragging) {",
            "public boolean mouseClicked(double mouseX, double mouseY, int button) {",
            "mouseClicked(MouseButtonEvent) → mouseClicked(double,double,int) in 1.21.4"));
        rules.add(new ApiChangeRule("1.21.10", "1.21.4", RuleType.TEXT_REPLACE,
            "double mouseX = event.x();\n        double mouseY = event.y();",
            "",
            "Remove event.x()/event.y() — mouseX/mouseY are now method params"));
        rules.add(new ApiChangeRule("1.21.10", "1.21.4", RuleType.TEXT_REPLACE,
            "return super.mouseClicked(event, dragging);",
            "return super.mouseClicked(mouseX, mouseY, button);",
            "super.mouseClicked(MouseButtonEvent) → super.mouseClicked(double,double,int)"));
        // PlayerSkin API: body().texturePath() → texture() in 1.21.4
        rules.add(new ApiChangeRule("1.21.10", "1.21.4", RuleType.TEXT_REPLACE,
            "playerSkin.body().texturePath()",
            "playerSkin.texture()",
            "PlayerSkin.body().texturePath() → PlayerSkin.texture() in 1.21.4"));
        // GuiGraphics.blit: fractional UV → pixel UV with RenderType in 1.21.4
        // Face UV (8,8)-(16,16) on 64x64 skin, scaled to 14x14
        rules.add(new ApiChangeRule("1.21.10", "1.21.4", RuleType.TEXT_REPLACE,
            "graphics.blit(skin, x + 1, y + 1, 14, 14, 8f / 64f, 8f / 64f, 16f / 64f, 16f / 64f);",
            "graphics.blit(net.minecraft.client.renderer.RenderType::guiTextured, skin, x + 1, y + 1, 8f, 8f, 14, 14, 64, 64);",
            "blit face UV: fractional → pixel with RenderType in 1.21.4"));
        // Hat UV (40,8)-(48,16) on 64x64 skin, scaled to 14x14
        rules.add(new ApiChangeRule("1.21.10", "1.21.4", RuleType.TEXT_REPLACE,
            "graphics.blit(skin, x + 1, y + 1, 14, 14, 40f / 64f, 8f / 64f, 48f / 64f, 16f / 64f);",
            "graphics.blit(net.minecraft.client.renderer.RenderType::guiTextured, skin, x + 1, y + 1, 40f, 8f, 14, 14, 64, 64);",
            "blit hat UV: fractional → pixel with RenderType in 1.21.4"));
        // NeoForge FMLEnvironment.getDist() → .dist field (changed in 21.4.x)
        rules.add(new ApiChangeRule("1.21.10", "1.21.4", RuleType.TEXT_REPLACE,
            "FMLEnvironment.getDist()",
            "FMLEnvironment.dist",
            "FMLEnvironment.getDist() → FMLEnvironment.dist in NeoForge 21.4.x"));
        // Reverse: 1.21.4 → 1.21.10
        rules.add(new ApiChangeRule("1.21.4", "1.21.10", RuleType.TEXT_REPLACE,
            "FMLEnvironment.dist",
            "FMLEnvironment.getDist()",
            "FMLEnvironment.dist → FMLEnvironment.getDist() in NeoForge 21.10.x"));
        rules.add(new ApiChangeRule("1.21.4", "1.21.10", RuleType.TEXT_REPLACE,
            "import net.minecraft.client.resources.PlayerSkin;",
            "import net.minecraft.world.entity.player.PlayerSkin;",
            "PlayerSkin: client.resources → world.entity.player in 1.21.10"));
        // keyPressed: add KeyEvent import, change signature and body
        rules.add(new ApiChangeRule("1.21.4", "1.21.10", RuleType.TEXT_REPLACE,
            "import org.lwjgl.glfw.GLFW;\n",
            "import net.minecraft.client.input.KeyEvent;\nimport org.lwjgl.glfw.GLFW;\n",
            "Add KeyEvent import for keyPressed(KeyEvent) in 1.21.10"));
        rules.add(new ApiChangeRule("1.21.4", "1.21.10", RuleType.TEXT_REPLACE,
            "public boolean keyPressed(int keyCode, int scanCode, int modifiers) {",
            "public boolean keyPressed(KeyEvent event) {",
            "keyPressed(int,int,int) → keyPressed(KeyEvent) in 1.21.10"));
        rules.add(new ApiChangeRule("1.21.4", "1.21.10", RuleType.TEXT_REPLACE,
            "keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER",
            "event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER",
            "keyCode → event.key() in keyPressed body for 1.21.10"));
        rules.add(new ApiChangeRule("1.21.4", "1.21.10", RuleType.TEXT_REPLACE,
            "return super.keyPressed(keyCode, scanCode, modifiers);",
            "return super.keyPressed(event);",
            "super.keyPressed(int,int,int) → super.keyPressed(KeyEvent) in 1.21.10"));

        // ── 1.21.5 → 1.21.6 (RenderType::guiTextured removed, player.server private) ──
        // RenderType::guiTextured removed; use float-UV blit(ResourceLocation, x, y, w, h, u0, v0, u1, v1)
        rules.add(new ApiChangeRule("1.21.5", "1.21.6", RuleType.TEXT_REPLACE,
            "graphics.blit(RenderType::guiTextured, skin, x + 1, y + 1, 8f, 8f, 14, 14, 64, 64);",
            "graphics.blit(skin, x + 1, y + 1, 14, 14, 8f / 64f, 8f / 64f, 16f / 64f, 16f / 64f);",
            "blit face UV: RenderType::guiTextured → float-UV in 1.21.6 (guiTextured removed)"));
        rules.add(new ApiChangeRule("1.21.5", "1.21.6", RuleType.TEXT_REPLACE,
            "graphics.blit(RenderType::guiTextured, skin, x + 1, y + 1, 40f, 8f, 14, 14, 64, 64);",
            "graphics.blit(skin, x + 1, y + 1, 14, 14, 40f / 64f, 8f / 64f, 48f / 64f, 16f / 64f);",
            "blit hat UV: RenderType::guiTextured → float-UV in 1.21.6 (guiTextured removed)"));
        rules.add(new ApiChangeRule("1.21.6", "1.21.5", RuleType.TEXT_REPLACE,
            "graphics.blit(skin, x + 1, y + 1, 14, 14, 8f / 64f, 8f / 64f, 16f / 64f, 16f / 64f);",
            "graphics.blit(RenderType::guiTextured, skin, x + 1, y + 1, 8f, 8f, 14, 14, 64, 64);",
            "blit face UV: float-UV → RenderType::guiTextured for 1.21.5"));
        rules.add(new ApiChangeRule("1.21.6", "1.21.5", RuleType.TEXT_REPLACE,
            "graphics.blit(skin, x + 1, y + 1, 14, 14, 40f / 64f, 8f / 64f, 48f / 64f, 16f / 64f);",
            "graphics.blit(RenderType::guiTextured, skin, x + 1, y + 1, 40f, 8f, 14, 14, 64, 64);",
            "blit hat UV: float-UV → RenderType::guiTextured for 1.21.5"));
        rules.add(new ApiChangeRule("1.21.5", "1.21.6", RuleType.IMPORT_CHANGE,
            "import net.minecraft.client.renderer.RenderType;",
            "",
            "Remove RenderType import in 1.21.6+ (guiTextured method reference removed)"));
        // Add RenderType import when downgrading to 1.21.5 (needed for RenderType::guiTextured)
        // Anchor to GuiGraphics import which is present in all rendering files
        rules.add(new ApiChangeRule("1.21.6", "1.21.5", RuleType.TEXT_REPLACE,
            "import net.minecraft.client.gui.GuiGraphics;\n",
            "import net.minecraft.client.gui.GuiGraphics;\nimport net.minecraft.client.renderer.RenderType;\n",
            "Add RenderType import when downgrading to 1.21.5 (needed for ::guiTextured)"));
        // ServerPlayer.server field made private in 1.21.6; access via level().getServer()
        rules.add(new ApiChangeRule("1.21.5", "1.21.6", RuleType.TEXT_REPLACE,
            "sender.server",
            "((net.minecraft.server.level.ServerLevel) sender.level()).getServer()",
            "ServerPlayer.server field private in 1.21.6; use level().getServer()"));
        rules.add(new ApiChangeRule("1.21.5", "1.21.6", RuleType.TEXT_REPLACE,
            "player.server",
            "((net.minecraft.server.level.ServerLevel) player.level()).getServer()",
            "ServerPlayer.server field private in 1.21.6; use level().getServer()"));
        rules.add(new ApiChangeRule("1.21.6", "1.21.5", RuleType.TEXT_REPLACE,
            "((net.minecraft.server.level.ServerLevel) sender.level()).getServer()",
            "sender.server",
            "Inline level().getServer() → sender.server for 1.21.5"));
        rules.add(new ApiChangeRule("1.21.6", "1.21.5", RuleType.TEXT_REPLACE,
            "((net.minecraft.server.level.ServerLevel) player.level()).getServer()",
            "player.server",
            "Inline level().getServer() → player.server for 1.21.5"));

        // ── 1.21.8 → 1.21.9 (PlayerSkin moved, KeyMapping.Category, keyPressed(KeyEvent), FMLEnvironment.getDist()) ──
        // PlayerSkin moved from net.minecraft.client.resources to net.minecraft.world.entity.player in 1.21.9
        // Also .texture() became .body().texturePath()
        addBidirectional(rules, "1.21.8", "1.21.9", RuleType.IMPORT_CHANGE,
            "import net.minecraft.client.resources.PlayerSkin;",
            "import net.minecraft.world.entity.player.PlayerSkin;",
            "PlayerSkin moved: client.resources → world.entity.player in 1.21.9");
        rules.add(new ApiChangeRule("1.21.8", "1.21.9", RuleType.TEXT_REPLACE,
            "playerSkin.texture()",
            "playerSkin.body().texturePath()",
            "PlayerSkin.texture() → PlayerSkin.body().texturePath() in 1.21.9"));
        rules.add(new ApiChangeRule("1.21.9", "1.21.8", RuleType.TEXT_REPLACE,
            "playerSkin.body().texturePath()",
            "playerSkin.texture()",
            "PlayerSkin.body().texturePath() → PlayerSkin.texture() for pre-1.21.9"));
        // NOTE: KeyMapping.Category → String conversion is mod-specific (field name varies).
        // Handle manually. See knowledge-base/minecraft/1.21.4_to_1.21.10.md for guidance.

        // keyPressed(KeyEvent) introduced in 1.21.9; pre-1.21.9 uses keyPressed(int, int, int)
        // Add KeyEvent import by anchoring to the GLFW import (present in any keybind-using file)
        rules.add(new ApiChangeRule("1.21.8", "1.21.9", RuleType.TEXT_REPLACE,
            "import org.lwjgl.glfw.GLFW;\n",
            "import net.minecraft.client.input.KeyEvent;\nimport org.lwjgl.glfw.GLFW;\n",
            "Add KeyEvent import before GLFW import for keyPressed(KeyEvent) in 1.21.9"));
        rules.add(new ApiChangeRule("1.21.8", "1.21.9", RuleType.TEXT_REPLACE,
            "public boolean keyPressed(int keyCode, int scanCode, int modifiers) {",
            "public boolean keyPressed(KeyEvent event) {",
            "keyPressed(int,int,int) → keyPressed(KeyEvent) in 1.21.9"));
        rules.add(new ApiChangeRule("1.21.8", "1.21.9", RuleType.TEXT_REPLACE,
            "keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER",
            "event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER",
            "keyCode → event.key() in keyPressed body for 1.21.9+"));
        rules.add(new ApiChangeRule("1.21.8", "1.21.9", RuleType.TEXT_REPLACE,
            "return super.keyPressed(keyCode, scanCode, modifiers);",
            "return super.keyPressed(event);",
            "super.keyPressed(int,int,int) → super.keyPressed(KeyEvent) in 1.21.9+"));
        rules.add(new ApiChangeRule("1.21.9", "1.21.8", RuleType.IMPORT_CHANGE,
            "import net.minecraft.client.input.KeyEvent;",
            "",
            "Remove KeyEvent import when downgrading to pre-1.21.9"));
        rules.add(new ApiChangeRule("1.21.9", "1.21.8", RuleType.TEXT_REPLACE,
            "public boolean keyPressed(KeyEvent event) {",
            "public boolean keyPressed(int keyCode, int scanCode, int modifiers) {",
            "keyPressed(KeyEvent) → keyPressed(int,int,int) for pre-1.21.9"));
        rules.add(new ApiChangeRule("1.21.9", "1.21.8", RuleType.TEXT_REPLACE,
            "event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER",
            "keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER",
            "event.key() → keyCode in keyPressed body for pre-1.21.9"));
        rules.add(new ApiChangeRule("1.21.9", "1.21.8", RuleType.TEXT_REPLACE,
            "return super.keyPressed(event);",
            "return super.keyPressed(keyCode, scanCode, modifiers);",
            "super.keyPressed(KeyEvent) → super.keyPressed(int,int,int) for pre-1.21.9"));
        // NeoForge FMLEnvironment.dist field → getDist() method in 1.21.9
        rules.add(new ApiChangeRule("1.21.8", "1.21.9", RuleType.TEXT_REPLACE,
            "FMLEnvironment.dist",
            "FMLEnvironment.getDist()",
            "FMLEnvironment.dist field → getDist() method in NeoForge 1.21.9+"));
        rules.add(new ApiChangeRule("1.21.9", "1.21.8", RuleType.TEXT_REPLACE,
            "FMLEnvironment.getDist()",
            "FMLEnvironment.dist",
            "FMLEnvironment.getDist() → .dist field for NeoForge pre-1.21.9"));

        // ── 1.21.10 → 1.21.11 (ResourceLocation renamed to Identifier) ──────
        addBidirectional(rules, "1.21.10", "1.21.11", RuleType.IMPORT_CHANGE,
            "import net.minecraft.resources.ResourceLocation;",
            "import net.minecraft.resources.Identifier;",
            "ResourceLocation renamed to Identifier in 1.21.11");
        rules.add(new ApiChangeRule("1.21.10", "1.21.11", RuleType.TEXT_REPLACE,
            "ResourceLocation",
            "Identifier",
            "ResourceLocation → Identifier in 1.21.11"));
        rules.add(new ApiChangeRule("1.21.11", "1.21.10", RuleType.TEXT_REPLACE,
            "Identifier",
            "ResourceLocation",
            "Identifier → ResourceLocation for pre-1.21.11"));

        // ── 1.21.11 → 26.1 (calendar versioning, Java 25, unobfuscated) ─────
        // ItemGroupEvents → CreativeModeTabEvents (Fabric API 0.145.x)
        addBidirectional(rules, "1.21.11", "26.1", RuleType.IMPORT_CHANGE,
            "import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;",
            "import net.fabricmc.fabric.api.item.v1.CreativeModeTabEvents;",
            "ItemGroupEvents → CreativeModeTabEvents in Fabric API 0.145 (26.1)");
        addBidirectional(rules, "1.21.11", "26.1", RuleType.TEXT_REPLACE,
            "ItemGroupEvents.modifyEntriesEvent(",
            "CreativeModeTabEvents.modify(",
            "ItemGroupEvents.modifyEntriesEvent → CreativeModeTabEvents.modify in Fabric API 0.145");
        addBidirectional(rules, "1.21.11", "26.1", RuleType.TEXT_REPLACE,
            "ItemGroupEvents",
            "CreativeModeTabEvents",
            "ItemGroupEvents → CreativeModeTabEvents general rename");
        // ColorProviderRegistry → BlockColorRegistry / ItemColorRegistry (Fabric API 0.145.x)
        addBidirectional(rules, "1.21.11", "26.1", RuleType.IMPORT_CHANGE,
            "import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;",
            "import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;",
            "ColorProviderRegistry → BlockColorRegistry in Fabric API 0.145 (26.1)");
        addBidirectional(rules, "1.21.11", "26.1", RuleType.TEXT_REPLACE,
            "ColorProviderRegistry.BLOCK.register(",
            "BlockColorRegistry.register(",
            "ColorProviderRegistry.BLOCK.register → BlockColorRegistry.register in 26.1");
        addBidirectional(rules, "1.21.11", "26.1", RuleType.TEXT_REPLACE,
            "ColorProviderRegistry.ITEM.register(",
            "ItemColorRegistry.register(",
            "ColorProviderRegistry.ITEM.register → ItemColorRegistry.register in 26.1");
        // HudRenderCallback removed; replaced by HudElementRegistry (Fabric API 0.145.x)
        // Note: HudElementRegistry API is not a drop-in replacement — manual migration required
        // but we can at least fix the import
        addBidirectional(rules, "1.21.11", "26.1", RuleType.IMPORT_CHANGE,
            "import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;",
            "import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;",
            "HudRenderCallback → HudElementRegistry import in Fabric API 0.145 (26.1)");
        // ChunkPos constructor: new ChunkPos(blockPos) → ChunkPos.containing(blockPos)
        addBidirectional(rules, "1.21.11", "26.1", RuleType.TEXT_REPLACE,
            "new ChunkPos(blockPos)",
            "ChunkPos.containing(blockPos)",
            "new ChunkPos(BlockPos) constructor removed; use ChunkPos.containing(blockPos) in 26.1");

        // ── Screen API (general - applies to many versions) ─────────────────
        addBidirectional(rules, "1.20.1", "1.21.10", RuleType.IMPORT_CHANGE,
            "net.minecraft.client.gui.screen.Screen",
            "net.minecraft.client.gui.screens.Screen",
            "Screen moved to screens package (Mojang mappings)");

        // ── KeyMapping constructor ─────────────────────────────────────────
        addBidirectional(rules, "1.16.5", "1.21.10", RuleType.TEXT_REPLACE,
            "new KeyBinding(",
            "new KeyMapping(",
            "KeyBinding → KeyMapping (Mojang mappings)");
        addBidirectional(rules, "1.16.5", "1.21.10", RuleType.IMPORT_CHANGE,
            "net.minecraft.client.settings.KeyBinding",
            "net.minecraft.client.KeyMapping",
            "KeyBinding import → KeyMapping");

        return rules;
    }

    private static void addBidirectional(List<ApiChangeRule> list,
        String v1, String v2, RuleType type,
        String oldPattern, String newPattern, String desc) {
        list.add(new ApiChangeRule(v1, v2, type, oldPattern, newPattern, desc));
        list.add(new ApiChangeRule(v2, v1, type, newPattern, oldPattern, desc + " (reverse)"));
    }

    /** Get rules applicable for a specific version transition. */
    public static List<ApiChangeRule> forTransition(String fromVer, String toVer) {
        List<ApiChangeRule> applicable = new ArrayList<>();
        for (ApiChangeRule rule : all()) {
            if (rule.fromVersion().equals(fromVer) && rule.toVersion().equals(toVer)) {
                applicable.add(rule);
            }
        }
        return applicable;
    }
}
