package com.autoporter;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Migration26_2Test {
    @TempDir Path temp;

    @Test void sourcePatcherAppliesExactGuiAndSymbolRules() throws Exception {
        Path root = temp.resolve("mod");
        Path src = root.resolve("src/main/java/example/ExampleClient.java");
        Files.createDirectories(src.getParent());
        Files.writeString(src, """
            package example;

            import net.minecraft.client.Minecraft;
            import net.minecraft.world.entity.monster.Slime;
            import net.minecraft.world.level.block.state.properties.DripstoneThickness;

            class ExampleClient {
                void open(Minecraft minecraft) {
                    Minecraft.getInstance().setScreen(null);
                    if (Minecraft.getInstance().screen != null) {}
                    minecraft.gui.setOverlayMessage(null, false);
                    minecraft.gui.setTitle(null);
                    Feature.POINTED_DRIPSTONE.toString();
                    BlockStateProperties.DRIPSTONE_THICKNESS.toString();
                    DripstoneThickness.TIP.toString();
                    new ServerboundSpectateEntityPacket(null);
                    GamePacketTypes.SERVERBOUND_SPECTATE_ENTITY.toString();
                }
            }
            """);

        PatchResult result = new SourcePatcher().patch(root, "26.1.2", "26.2", false);
        String updated = Files.readString(src);

        assertTrue(result.changes().size() >= 8, result.changes().toString());
        assertTrue(updated.contains("Minecraft.getInstance().gui.setScreen(null);"));
        assertTrue(updated.contains("Minecraft.getInstance().gui.screen()"));
        assertTrue(updated.contains("minecraft.gui.hud.setOverlayMessage(null, false);"));
        assertTrue(updated.contains("minecraft.gui.hud.setTitle(null);"));
        assertTrue(updated.contains("import net.minecraft.world.entity.monster.cubemob.Slime;"));
        assertTrue(updated.contains("Feature.SPELEOTHEM.toString();"));
        assertTrue(updated.contains("BlockStateProperties.SPELEOTHEM_THICKNESS.toString();"));
        assertTrue(updated.contains("SpeleothemThickness.TIP.toString();"));
        assertTrue(updated.contains("new ServerboundSpectatorActionPacket(null);"));
        assertTrue(updated.contains("GamePacketTypes.SERVERBOUND_SPECTATOR_ACTION.toString();"));
    }

    @Test void resourcePatcherUpdatesPackFormatsAndExactShaderNames() throws Exception {
        Path resourcePack = temp.resolve("resource-pack");
        Path assets = resourcePack.resolve("assets/minecraft/shaders/core");
        Files.createDirectories(assets);
        Files.writeString(resourcePack.resolve("pack.mcmeta"), """
            {"pack":{"pack_format":84,"description":"test"}}
            """);
        Files.writeString(assets.resolve("rendertype_text.json"), "{}");
        Files.writeString(assets.resolve("rendertype_text_background.json"), "{}");

        PatchResult resourceResult = new ResourcePatcher().patch(resourcePack, "26.1.2", "26.2", false);
        String pack = Files.readString(resourcePack.resolve("pack.mcmeta"));

        assertTrue(pack.contains("\"pack_format\": 88"));
        assertTrue(Files.exists(assets.resolve("text.json")));
        assertTrue(Files.exists(assets.resolve("text_background.json")));
        assertFalse(Files.exists(assets.resolve("rendertype_text.json")));
        assertEquals(3, resourceResult.changes().size(), resourceResult.changes().toString());

        Path dataPack = temp.resolve("data-pack");
        Files.createDirectories(dataPack.resolve("data/example/tags/block"));
        Files.writeString(dataPack.resolve("pack.mcmeta"), """
            {"pack":{"pack_format":101.1,"description":"test"}}
            """);
        PatchResult dataResult = new ResourcePatcher().patch(dataPack, "26.1.2", "26.2", false);
        assertTrue(Files.readString(dataPack.resolve("pack.mcmeta")).contains("\"pack_format\": 107.1"));
        assertEquals(1, dataResult.changes().size(), dataResult.changes().toString());
    }

    @Test void warningScannerReportsManualOnlyMigrationItems() throws Exception {
        Path root = temp.resolve("mod");
        Path src = root.resolve("src/main/java/example/DataGen.java");
        Path atlas = root.resolve("src/main/resources/assets/example/atlases/signs.json");
        Files.createDirectories(src.getParent());
        Files.createDirectories(atlas.getParent());
        Files.writeString(src, """
            class DataGen extends IntrinsicHolderTagsProvider<Object> {
                void run() {
                    valueLookupBuilder(null);
                    Font.draw(null, null, 0, 0, 0);
                    StructureProcessorType<?> type;
                }
            }
            """);
        Files.writeString(atlas, "{\"sources\":[{\"type\":\"single\",\"resource\":\"minecraft:signs\"}]}");

        PatchResult result = new MigrationWarningScanner().scan(root, "26.1.2", "26.2");

        assertTrue(result.changes().stream().anyMatch(s -> s.contains("valueLookupBuilder")));
        assertTrue(result.changes().stream().anyMatch(s -> s.contains("IntrinsicHolderTagsProvider")));
        assertTrue(result.changes().stream().anyMatch(s -> s.contains("Font.draw")));
        assertTrue(result.changes().stream().anyMatch(s -> s.contains("StructureProcessorType")));
        assertTrue(result.changes().stream().anyMatch(s -> s.contains("minecraft:signs")));
    }
}
