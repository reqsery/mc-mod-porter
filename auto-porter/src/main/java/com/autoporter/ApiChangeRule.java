package com.autoporter;

import java.util.*;

/**
 * Represents a single API rename/change rule between two MC versions.
 * Used by the SourcePatcher to apply refactors.
 *
 * NOTE: ApiChangeRule is temporary and should eventually be generated from the knowledge-base
 * (knowledge-base/minecraft/*.md). Rules here are manually kept in sync with the KB files.
 * When adding new version support, add a KB file first, then mirror the changes here.
 *
 * IMPORTANT: Rule order matters! When multiple rules can apply to the same content,
 * the FIRST matching rule wins. Specific/longer patterns should be added BEFORE generic/shorter ones.
 * See example: FriendlyByteBuf constructor rule at 1.20.4→1.20.5 must come before the
 * general FriendlyByteBuf→RegistryFriendlyByteBuf rule.
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
        // ⚠️ IMPORTANT: The specific constructor rule MUST come BEFORE the general FriendlyByteBuf rule.
        // The general rule "FriendlyByteBuf" → "RegistryFriendlyByteBuf" matches as a substring.
        // If it fires first, the specific constructor rule can never match.
        // Order is enforced by addBidirectional() and addGeneralRule() placement.
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

        // ── 1.20.1 → 1.20.2 (NeoForge split) ─────────────────────────
        addBidirectional(rules, "1.20.1", "1.20.2", RuleType.IMPORT_CHANGE,
            "net.minecraftforge",
            "net.neoforged",
            "Forge → NeoForge package rename");
        addBidirectional(rules, "1.20.1", "1.20.2", RuleType.TEXT_REPLACE,
            "minecraftforge",
            "neoforged",
            "minecraftforge → neoforged");

        // ── 1.20.4 → 1.20.5 (Networking API) ─────────────────────────
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
        // ChunkPos.asLong → ChunkPos.pack
        addBidirectional(rules, "1.21.11", "26.1", RuleType.TEXT_REPLACE,
            "ChunkPos.asLong(",
            "ChunkPos.pack(",
            "ChunkPos.asLong() → ChunkPos.pack() in 26.1");
        // NOTE: new ChunkPos(long) → ChunkPos.unpack(long) also changed in 26.1,
        // but a safe text replacement is not possible (can't distinguish BlockPos vs long arg by text).
        // The BlockPos variant is already handled above. The long variant requires manual migration.
        // GuiGraphics → GuiGraphicsExtractor (GUI rendering refactor in 26.1)
        // oldPattern = name in SOURCE version, newPattern = name in TARGET version
        addBidirectional(rules, "1.21.11", "26.1", RuleType.CLASS_RENAME,
            "GuiGraphics",
            "GuiGraphicsExtractor",
            "GuiGraphics (pre-26.1) → GuiGraphicsExtractor (26.1)");
        addBidirectional(rules, "1.21.11", "26.1", RuleType.IMPORT_CHANGE,
            "import net.minecraft.client.gui.GuiGraphics;",
            "import net.minecraft.client.gui.GuiGraphicsExtractor;",
            "GuiGraphics import → GuiGraphicsExtractor in 26.1");
        // renderSlot → extractSlot in @Inject(method = "...") annotation strings
        addBidirectional(rules, "1.21.11", "26.1", RuleType.METHOD_RENAME,
            "method = \"renderSlot\"",
            "method = \"extractSlot\"",
            "renderSlot (pre-26.1) → extractSlot (26.1) in @Inject method attribute");
        // renderItem → item in dot-notation @At(target = "ClassName.method(...)") strings
        // JVM-format target strings (Lnet/.../Class;method(...)) are handled by MixinTargetResolver
        addBidirectional(rules, "1.21.11", "26.1", RuleType.METHOD_RENAME,
            "target = \"GuiGraphics.renderItem(",
            "target = \"GuiGraphicsExtractor.item(",
            "GuiGraphics.renderItem (pre-26.1) → GuiGraphicsExtractor.item (26.1) in dot-notation target");

        // ── 1.21.9/1.21.10 Entity API ─────────────────────────────────────────
        // Entity#getWorld → Entity#getEntityWorld (Fabric 1.21.9/1.21.10)
        addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
            "Minecraft.getInstance().setScreen(",
            "Minecraft.getInstance().gui.setScreen(",
            "26.2 GUI: Minecraft#setScreen moved to Gui#setScreen");
        addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
            "this.minecraft.setScreen(",
            "this.minecraft.gui.setScreen(",
            "26.2 GUI: this.minecraft#setScreen moved to Gui#setScreen");
        addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
            "minecraft.setScreen(",
            "minecraft.gui.setScreen(",
            "26.2 GUI: minecraft#setScreen moved to Gui#setScreen");
        addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
            "Minecraft.getInstance().screen",
            "Minecraft.getInstance().gui.screen()",
            "26.2 GUI: Minecraft#screen moved to Gui#screen()");
        addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
            "this.minecraft.screen",
            "this.minecraft.gui.screen()",
            "26.2 GUI: this.minecraft#screen moved to Gui#screen()");
        addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
            "minecraft.screen",
            "minecraft.gui.screen()",
            "26.2 GUI: minecraft#screen moved to Gui#screen()");
        addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
            "Minecraft.getInstance().openChatScreen(",
            "Minecraft.getInstance().gui.openChatScreen(",
            "26.2 GUI: Minecraft#openChatScreen moved to Gui#openChatScreen");
        addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
            "Minecraft.getInstance().getToastManager()",
            "Minecraft.getInstance().gui.toastManager()",
            "26.2 GUI: Minecraft#getToastManager moved to Gui#toastManager");
        addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
            "Minecraft.getInstance().getChatListener()",
            "Minecraft.getInstance().gui.chatListener()",
            "26.2 GUI: Minecraft#getChatListener moved to Gui#chatListener");

        String[][] hudMoves26_2 = {
            {"setOverlayMessage", "26.2 HUD: Gui#setOverlayMessage moved to Hud#setOverlayMessage"},
            {"resetTitleTimes",   "26.2 HUD: Gui#resetTitleTimes moved to Hud#resetTitleTimes"},
            {"setNowPlaying",     "26.2 HUD: Gui#setNowPlaying moved to Hud#setNowPlaying"},
            {"setTimes",          "26.2 HUD: Gui#setTimes moved to Hud#setTimes"},
            {"setSubtitle",       "26.2 HUD: Gui#setSubtitle moved to Hud#setSubtitle"},
            {"setTitle",          "26.2 HUD: Gui#setTitle moved to Hud#setTitle"},
            {"clearTitles",       "26.2 HUD: Gui#clearTitles moved to Hud#clearTitles"},
            {"getChat",           "26.2 HUD: Gui#getChat moved to Hud#getChat"},
            {"getGuiTicks",       "26.2 HUD: Gui#getGuiTicks moved to Hud#getGuiTicks"},
            {"getFont",           "26.2 HUD: Gui#getFont moved to Hud#getFont"},
            {"getSpectatorGui",   "26.2 HUD: Gui#getSpectatorGui moved to Hud#getSpectatorGui"},
            {"getTabList",        "26.2 HUD: Gui#getTabList moved to Hud#getTabList"},
            {"onDisconnected",    "26.2 HUD: Gui#onDisconnected moved to Hud#onDisconnected"},
            {"getBossOverlay",    "26.2 HUD: Gui#getBossOverlay moved to Hud#getBossOverlay"},
            {"getDebugOverlay",   "26.2 HUD: Gui#getDebugOverlay moved to Hud#getDebugOverlay"},
            {"clearCache",        "26.2 HUD: Gui#clearCache moved to Hud#clearCache"}
        };
        for (String[] move : hudMoves26_2) {
            addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
                "Minecraft.getInstance().gui." + move[0] + "(",
                "Minecraft.getInstance().gui.hud." + move[0] + "(",
                move[1]);
            addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
                "this.minecraft.gui." + move[0] + "(",
                "this.minecraft.gui.hud." + move[0] + "(",
                move[1]);
            addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
                "minecraft.gui." + move[0] + "(",
                "minecraft.gui.hud." + move[0] + "(",
                move[1]);
        }

        addForward(rules, "26.1.2", "26.2", RuleType.IMPORT_CHANGE,
            "import net.minecraft.world.entity.monster.MagmaCube;",
            "import net.minecraft.world.entity.monster.cubemob.MagmaCube;",
            "26.2 class move: MagmaCube moved to monster.cubemob");
        addForward(rules, "26.1.2", "26.2", RuleType.IMPORT_CHANGE,
            "import net.minecraft.world.entity.monster.Slime;",
            "import net.minecraft.world.entity.monster.cubemob.Slime;",
            "26.2 class move: Slime moved to monster.cubemob");
        addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
            "net.minecraft.world.entity.monster.MagmaCube",
            "net.minecraft.world.entity.monster.cubemob.MagmaCube",
            "26.2 fully-qualified class move: MagmaCube moved to monster.cubemob");
        addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
            "net.minecraft.world.entity.monster.Slime",
            "net.minecraft.world.entity.monster.cubemob.Slime",
            "26.2 fully-qualified class move: Slime moved to monster.cubemob");
        addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
            "PointedDripstoneFeature", "SpeleothemFeature",
            "26.2 worldgen rename: PointedDripstoneFeature -> SpeleothemFeature");
        addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
            "DripstoneClusterFeature", "SpeleothemClusterFeature",
            "26.2 worldgen rename: DripstoneClusterFeature -> SpeleothemClusterFeature");
        addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
            "PointedDripstoneConfiguration", "SpeleothemConfiguration",
            "26.2 worldgen rename: PointedDripstoneConfiguration -> SpeleothemConfiguration");
        addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
            "DripstoneClusterConfiguration", "SpeleothemClusterConfiguration",
            "26.2 worldgen rename: DripstoneClusterConfiguration -> SpeleothemClusterConfiguration");
        addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
            "DripstoneUtils", "SpeleothemUtils",
            "26.2 worldgen rename: DripstoneUtils -> SpeleothemUtils");
        addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
            "DripstoneThickness", "SpeleothemThickness",
            "26.2 block-state enum rename: DripstoneThickness -> SpeleothemThickness");
        addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
            "BlockStateProperties.DRIPSTONE_THICKNESS", "BlockStateProperties.SPELEOTHEM_THICKNESS",
            "26.2 property rename: DRIPSTONE_THICKNESS -> SPELEOTHEM_THICKNESS");
        addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
            "Feature.DRIPSTONE_CLUSTER", "Feature.SPELEOTHEM_CLUSTER",
            "26.2 feature constant rename: DRIPSTONE_CLUSTER -> SPELEOTHEM_CLUSTER");
        addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
            "Feature.POINTED_DRIPSTONE", "Feature.SPELEOTHEM",
            "26.2 feature constant rename: POINTED_DRIPSTONE -> SPELEOTHEM");
        addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
            "TreeConfiguration.CAN_PLACE_BELOW_OVERWORLD_TRUNKS", "TreeConfiguration.CAN_PLACE_BELOW_TREE_TRUNKS",
            "26.2 tree config constant rename: CAN_PLACE_BELOW_OVERWORLD_TRUNKS -> CAN_PLACE_BELOW_TREE_TRUNKS");
        addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
            "ChunkPos.MAX_COORDINATE_VALUE", "ChunkPyramid.MAX_CHUNK_COORDINATE_VALUE",
            "26.2 chunk coordinate constant moved to ChunkPyramid");
        addForward(rules, "26.1.2", "26.2", RuleType.IMPORT_CHANGE,
            "import net.minecraft.world.level.ChunkPos;",
            "import net.minecraft.world.level.ChunkPos;\nimport net.minecraft.world.level.ChunkPyramid;",
            "26.2 add ChunkPyramid import for MAX_CHUNK_COORDINATE_VALUE");
        addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
            ".markPosForPostprocessing(", ".markPosForPostProcessing(",
            "26.2 method spelling change: markPosForPostprocessing -> markPosForPostProcessing");
        addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
            ".getLightBlockInto(", ".getLightDampeningInto(",
            "26.2 lighting rename: getLightBlockInto -> getLightDampeningInto");
        addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
            "WorldPresets.createFlatWorldDimensions(", "WorldPresets.createTestWorldDimensions(",
            "26.2 world preset rename: createFlatWorldDimensions -> createTestWorldDimensions");
        addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
            "PlayerSpawnFinder.getOverworldRespawnPos(", "PlayerSpawnFinder.getLevelRespawnPos(",
            "26.2 respawn helper rename: getOverworldRespawnPos -> getLevelRespawnPos");
        addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
            "GamePacketTypes.SERVERBOUND_SPECTATE_ENTITY", "GamePacketTypes.SERVERBOUND_SPECTATOR_ACTION",
            "26.2 networking packet type rename: SERVERBOUND_SPECTATE_ENTITY -> SERVERBOUND_SPECTATOR_ACTION");
        addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
            "handleSpectateEntity(", "handleSpectatorAction(",
            "26.2 networking handler rename: handleSpectateEntity -> handleSpectatorAction");
        addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
            "ServerboundSpectateEntityPacket", "ServerboundSpectatorActionPacket",
            "26.2 networking class rename: ServerboundSpectateEntityPacket -> ServerboundSpectatorActionPacket");
        addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
            "ColorArgument", "TeamColorArgument",
            "26.2 command argument rename: ColorArgument -> TeamColorArgument");
        addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
            "InstantenousMobEffect", "InstantaneousMobEffect",
            "26.2 spelling fix: InstantenousMobEffect -> InstantaneousMobEffect");
        addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
            "applyInstantenousEffect(", "applyInstantaneousEffect(",
            "26.2 spelling fix: applyInstantenousEffect -> applyInstantaneousEffect");
        addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
            "isInstantenous(", "isInstantaneous(",
            "26.2 spelling fix: isInstantenous -> isInstantaneous");
        addForward(rules, "26.1.2", "26.2", RuleType.IMPORT_CHANGE,
            "import net.minecraft.world.Bucketable;",
            "import net.minecraft.world.entity.Bucketable;",
            "26.2 class move: Bucketable moved to world.entity");
        addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
            "ContextualBarRenderer", "ContextualBar",
            "26.2 HUD class rename: ContextualBarRenderer -> ContextualBar");
        addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
            "ExperienceBarRenderer", "ExperienceBar",
            "26.2 HUD class rename: ExperienceBarRenderer -> ExperienceBar");
        addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
            "JumpableVehicleBarRenderer", "JumpableVehicleBar",
            "26.2 HUD class rename: JumpableVehicleBarRenderer -> JumpableVehicleBar");
        addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
            "LocatorBarRenderer", "LocatorBar",
            "26.2 HUD class rename: LocatorBarRenderer -> LocatorBar");
        addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
            "GlyphRenderTypes.createForIntensityTexture(",
            "GlyphRenderTypes.createForGrayscaleTexture(",
            "26.2 font rendering rename: createForIntensityTexture -> createForGrayscaleTexture");
        addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
            ".isPauseScreen(",
            ".isPausing(",
            "26.2 overlay rename: Overlay#isPauseScreen -> isPausing");
        addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
            "RenderType.TEXT_INTENSITY", "RenderType.TEXT_GRAYSCALE",
            "26.2 render type rename: TEXT_INTENSITY -> TEXT_GRAYSCALE");
        addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
            "RenderType.GUI_TEXT_INTENSITY", "RenderType.GUI_TEXT_GRAYSCALE",
            "26.2 render type rename: GUI_TEXT_INTENSITY -> GUI_TEXT_GRAYSCALE");
        addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
            "RenderType.TEXT_INTENSITY_SEE_THROUGH", "RenderType.TEXT_GRAYSCALE_SEE_THROUGH",
            "26.2 render type rename: TEXT_INTENSITY_SEE_THROUGH -> TEXT_GRAYSCALE_SEE_THROUGH");
        addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
            "createPointedDripstoneVariant(", "createSpeleothemVariant(",
            "26.2 helper rename: createPointedDripstoneVariant -> createSpeleothemVariant");
        addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
            "createPointedDripstone(", "createSpeleothem(",
            "26.2 helper rename: createPointedDripstone -> createSpeleothem");
        String[][] advancementImports26_2 = {
            {"net.minecraft.advancements.CriteriaTriggers", "net.minecraft.advancements.triggers.CriteriaTriggers"},
            {"net.minecraft.advancements.Criterion", "net.minecraft.advancements.triggers.Criterion"},
            {"net.minecraft.advancements.CriterionTrigger", "net.minecraft.advancements.triggers.CriterionTrigger"},
            {"net.minecraft.advancements.BlockPredicate", "net.minecraft.advancements.predicates.BlockPredicate"},
            {"net.minecraft.advancements.CollectionContentsPredicate", "net.minecraft.advancements.predicates.CollectionContentsPredicate"},
            {"net.minecraft.advancements.CollectionCountsPredicate", "net.minecraft.advancements.predicates.CollectionCountsPredicate"},
            {"net.minecraft.advancements.CollectionPredicate", "net.minecraft.advancements.predicates.CollectionPredicate"},
            {"net.minecraft.advancements.ContextAwarePredicate", "net.minecraft.advancements.predicates.ContextAwarePredicate"},
            {"net.minecraft.advancements.DamagePredicate", "net.minecraft.advancements.predicates.DamagePredicate"},
            {"net.minecraft.advancements.DamageSourcePredicate", "net.minecraft.advancements.predicates.DamageSourcePredicate"},
            {"net.minecraft.advancements.DataComponentMatchers", "net.minecraft.advancements.predicates.DataComponentMatchers"},
            {"net.minecraft.advancements.DistancePredicate", "net.minecraft.advancements.predicates.DistancePredicate"},
            {"net.minecraft.advancements.EnchantmentPredicate", "net.minecraft.advancements.predicates.EnchantmentPredicate"},
            {"net.minecraft.advancements.EntityEquipmentPredicate", "net.minecraft.advancements.predicates.entity.EntityEquipmentPredicate"},
            {"net.minecraft.advancements.EntityFlagsPredicate", "net.minecraft.advancements.predicates.entity.EntityFlagsPredicate"},
            {"net.minecraft.advancements.EntityPredicate", "net.minecraft.advancements.predicates.entity.EntityPredicate"},
            {"net.minecraft.advancements.EntitySubPredicate", "net.minecraft.advancements.predicates.entity.EntitySubPredicate"},
            {"net.minecraft.advancements.EntitySubPredicates", "net.minecraft.advancements.predicates.entity.EntitySubPredicates"},
            {"net.minecraft.advancements.EntityTypePredicate", "net.minecraft.advancements.predicates.entity.EntityTypePredicate"},
            {"net.minecraft.advancements.FishingHookPredicate", "net.minecraft.advancements.predicates.entity.FishingHookPredicate"},
            {"net.minecraft.advancements.FluidPredicate", "net.minecraft.advancements.predicates.FluidPredicate"},
            {"net.minecraft.advancements.FoodPredicate", "net.minecraft.advancements.predicates.FoodPredicate"},
            {"net.minecraft.advancements.GameTypePredicate", "net.minecraft.advancements.predicates.GameTypePredicate"},
            {"net.minecraft.advancements.InputPredicate", "net.minecraft.advancements.predicates.InputPredicate"},
            {"net.minecraft.advancements.ItemPredicate", "net.minecraft.advancements.predicates.ItemPredicate"},
            {"net.minecraft.advancements.LightningBoltPredicate", "net.minecraft.advancements.predicates.entity.LightningBoltPredicate"},
            {"net.minecraft.advancements.LightPredicate", "net.minecraft.advancements.predicates.LightPredicate"},
            {"net.minecraft.advancements.LocationPredicate", "net.minecraft.advancements.predicates.LocationPredicate"},
            {"net.minecraft.advancements.MinMaxBounds", "net.minecraft.advancements.predicates.MinMaxBounds"},
            {"net.minecraft.advancements.MobEffectsPredicate", "net.minecraft.advancements.predicates.MobEffectsPredicate"},
            {"net.minecraft.advancements.MovementPredicate", "net.minecraft.advancements.predicates.entity.MovementPredicate"},
            {"net.minecraft.advancements.NbtPredicate", "net.minecraft.advancements.predicates.NbtPredicate"},
            {"net.minecraft.advancements.PlayerPredicate", "net.minecraft.advancements.predicates.entity.PlayerPredicate"},
            {"net.minecraft.advancements.RaiderPredicate", "net.minecraft.advancements.predicates.entity.RaiderPredicate"},
            {"net.minecraft.advancements.SheepPredicate", "net.minecraft.advancements.predicates.entity.SheepPredicate"},
            {"net.minecraft.advancements.SingleComponentItemPredicate", "net.minecraft.advancements.predicates.SingleComponentItemPredicate"},
            {"net.minecraft.advancements.SlimePredicate", "net.minecraft.advancements.predicates.entity.CubeMobPredicate"},
            {"net.minecraft.advancements.SlotsPredicate", "net.minecraft.advancements.predicates.SlotsPredicate"},
            {"net.minecraft.advancements.StatePropertiesPredicate", "net.minecraft.advancements.predicates.StatePropertiesPredicate"},
            {"net.minecraft.advancements.TagPredicate", "net.minecraft.advancements.predicates.TagPredicate"}
        };
        for (String[] move : advancementImports26_2) {
            addForward(rules, "26.1.2", "26.2", RuleType.IMPORT_CHANGE,
                "import " + move[0] + ";",
                "import " + move[1] + ";",
                "26.2 advancement package move: " + move[0] + " -> " + move[1]);
            addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
                move[0],
                move[1],
                "26.2 fully-qualified advancement package move: " + move[0] + " -> " + move[1]);
        }
        addForward(rules, "26.1.2", "26.2", RuleType.TEXT_REPLACE,
            "SlimePredicate", "CubeMobPredicate",
            "26.2 advancement predicate rename: SlimePredicate -> CubeMobPredicate");

        addBidirectional(rules, "1.21.8", "1.21.9", RuleType.METHOD_RENAME,
            ".getEntityWorld()",
            ".getWorld()",
            "Entity#getEntityWorld() (1.21.9+) ↔ Entity#getWorld() (pre-1.21.9)");
        // MinecraftClient.IS_SYSTEM_MAC → SystemKeycodes.IS_MAC_OS
        addBidirectional(rules, "1.21.8", "1.21.9", RuleType.TEXT_REPLACE,
            "SystemKeycodes.IS_MAC_OS",
            "MinecraftClient.IS_SYSTEM_MAC",
            "SystemKeycodes.IS_MAC_OS (1.21.9+) ↔ MinecraftClient.IS_SYSTEM_MAC (pre-1.21.9)");

        // ── 1.21.5/1.21.6 BlockRenderLayerMap ────────────────────────────────
        addBidirectional(rules, "1.21.5", "1.21.6", RuleType.TEXT_REPLACE,
            "BlockRenderLayerMap.putBlock(",
            "BlockRenderLayerMap.INSTANCE.putBlock(",
            "BlockRenderLayerMap.INSTANCE removed in 1.21.6; use static method");
        addBidirectional(rules, "1.21.5", "1.21.6", RuleType.IMPORT_CHANGE,
            "import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;",
            "import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;",
            "BlockRenderLayerMap import moved in 1.21.6");

        // ── 1.20.6/1.21 Identifier constructor ───────────────────────────────
        rules.add(new ApiChangeRule("1.20.6", "1.21", RuleType.TEXT_REPLACE,
            "new Identifier(",
            "Identifier.of(",
            "Identifier constructor protected in 1.21; use Identifier.of()"));
        rules.add(new ApiChangeRule("1.21", "1.20.6", RuleType.TEXT_REPLACE,
            "Identifier.of(",
            "new Identifier(",
            "Identifier.of() → new Identifier() for pre-1.21"));

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

    private static void addForward(List<ApiChangeRule> list,
        String from, String to, RuleType type,
        String oldPattern, String newPattern, String desc) {
        list.add(new ApiChangeRule(from, to, type, oldPattern, newPattern, desc));
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
