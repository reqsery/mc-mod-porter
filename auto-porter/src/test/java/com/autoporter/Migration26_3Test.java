package com.autoporter;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Migration26_3Test {
    @TempDir Path temp;

    @Test void sourcePatcherAppliesVerifiedExactRenamesAndMoves() throws Exception {
        Path root = temp.resolve("mod");
        Path src = root.resolve("src/main/java/example/Example.java");
        Files.createDirectories(src.getParent());
        Files.writeString(src, """
            package example;

            import com.mojang.blaze3d.pipeline.RenderPipeline;
            import com.mojang.blaze3d.buffers.GpuBuffer;
            import net.minecraft.core.Direction8;
            import net.minecraft.server.commands.ItemCommands;

            class Example {
                ServerboundSwingPacket packet;
                void migrate() {
                    handleAnimate(packet);
                    server.getStructureManager();
                    ExtraCodecs.relaiveNormalizedSubPathCodec();
                    ClientboundAnimatePacket.SWING_MAIN_HAND.toString();
                    InteractionResult.SwingSource.CLIENT.toString();
                    PushReaction.NORMAL.toString();
                    Registries.CONFIGURED_FEATURE.toString();
                    ModConfig.Type.COMMON.toString();
                    ModConfig.Type.SERVER.toString();
                }
            }
            """);

        PatchResult result = new SourcePatcher().patch(root, "26.2", "26.3", false);
        String updated = Files.readString(src);

        assertTrue(result.changes().size() >= 10, result.changes().toString());
        assertTrue(updated.contains("import com.mojang.renderpearl.api.pipeline.RenderPipeline;"));
        assertTrue(updated.contains("import com.mojang.renderpearl.api.buffers.GpuBuffer;"));
        assertTrue(updated.contains("import net.minecraft.core.CompositeDirection.Direction8;"));
        assertTrue(updated.contains("import net.minecraft.server.commands.item.ItemCommands;"));
        assertTrue(updated.contains("ServerboundPunchPacket packet;"));
        assertTrue(updated.contains("handlePunch(packet);"));
        assertTrue(updated.contains("server.getStructureTemplateManager();"));
        assertTrue(updated.contains("ExtraCodecs.relativeNormalizedSubPathCodec();"));
        assertTrue(updated.contains("ClientboundSwingAnimationPacket.SWING_MAIN_HAND"));
        assertTrue(updated.contains("InteractionResult.SwingSource.PREDICTED"));
        assertTrue(updated.contains("PushReaction.PUSH_PULL"));
        assertTrue(updated.contains("Registries.FEATURE"));
        assertTrue(updated.contains("ModConfig.Type.LOCAL"));
        assertTrue(updated.contains("ModConfig.Type.SYNCED"));
    }

    @Test void resourcePatcherMigratesPackFormatsWorldgenTagsAndShaders() throws Exception {
        Path root = temp.resolve("pack");
        Path feature = root.resolve("data/example/worldgen/configured_feature/basalt.json");
        Path tag = root.resolve("data/minecraft/tags/block/convertable_to_mud.json");
        Path shaders = root.resolve("assets/minecraft/shaders/core");
        Files.createDirectories(feature.getParent());
        Files.createDirectories(tag.getParent());
        Files.createDirectories(shaders);
        Files.writeString(root.resolve("pack.mcmeta"), """
            {"pack":{"pack_format":107.1,"description":"test"}}
            """);
        Files.writeString(feature, """
            {"type":"minecraft:basalt_columns","config":{"reach":3,"height":5}}
            """);
        Files.writeString(tag, """
            {"replace":false,"values":["#minecraft:convertable_to_mud"]}
            """);
        Files.writeString(shaders.resolve("rendertype_clouds.vsh"), """
            #version 330
            #moj_import <minecraft:fog.glsl>
            """);

        PatchResult result = new ResourcePatcher().patch(root, "26.2", "26.3", false);
        Path migratedFeature = root.resolve("data/example/worldgen/feature/basalt.json");
        String migrated = Files.readString(migratedFeature);

        assertTrue(result.changes().size() >= 7, result.changes().toString());
        assertTrue(Files.readString(root.resolve("pack.mcmeta")).contains("\"pack_format\": 121"));
        assertFalse(Files.exists(feature));
        assertTrue(migrated.contains("\"type\": \"minecraft:stepped_column_cluster\""));
        assertTrue(migrated.contains("\"column_reach\": 3"));
        assertFalse(migrated.contains("\"config\""));
        assertTrue(Files.exists(root.resolve("data/minecraft/tags/block/convertible_to_mud.json")));
        assertTrue(Files.readString(root.resolve("data/minecraft/tags/block/convertible_to_mud.json"))
            .contains("minecraft:convertible_to_mud"));
        assertTrue(Files.exists(shaders.resolve("clouds.vsh")));
        assertTrue(Files.readString(shaders.resolve("clouds.vsh")).contains("#include"));
    }

    @Test void warningScannerReportsSemanticFabricNeoForgeAndVanillaChanges() throws Exception {
        Path root = temp.resolve("warnings");
        Path src = root.resolve("src/main/java/example/Legacy.java");
        Path biome = root.resolve("src/main/resources/data/example/worldgen/biome/test.json");
        Files.createDirectories(src.getParent());
        Files.createDirectories(biome.getParent());
        Files.writeString(src, """
            import org.lwjgl.glfw.GLFW;
            class Legacy {
                ConfiguredFeature<?, ?> feature;
                void run() {
                    FuelRegistry.INSTANCE.add(null, 1);
                    ModConfig.Type.COMMON.toString();
                    TillableBlockRegistry.register(null, null, null);
                }
            }
            """);
        Files.writeString(biome, """
            {"spawners":{},"spawn_costs":{}}
            """);

        PatchResult result = new MigrationWarningScanner().scan(root, "26.2", "26.3");

        assertTrue(result.changes().stream().anyMatch(s -> s.contains("GLFW")));
        assertTrue(result.changes().stream().anyMatch(s -> s.contains("ConfiguredFeature")));
        assertTrue(result.changes().stream().anyMatch(s -> s.contains("FuelRegistry")));
        assertTrue(result.changes().stream().anyMatch(s -> s.contains("ModConfig.Type.COMMON")));
        assertTrue(result.changes().stream().anyMatch(s -> s.contains("TillableBlockRegistry")));
        assertTrue(result.changes().stream().anyMatch(s -> s.contains("\"spawners\"")));
    }

    @Test void gradlePropertiesUpdatesCurrentFabricAndNeoForgeKeys() throws Exception {
        Path root = temp.resolve("build-properties");
        Files.createDirectories(root);
        Files.writeString(root.resolve("gradle.properties"), """
            minecraft_version=26.2
            minecraft_version_range=[26.2]
            loader_version=0.19.3
            loom_version=1.17.13
            fabric_api_version=0.154.2+26.2
            neo_version=26.2.0.10-beta
            """);

        new GradlePropertiesPatcher().patch(root, "26.3");
        String updated = Files.readString(root.resolve("gradle.properties"));

        assertTrue(updated.contains("minecraft_version=26.3"));
        assertTrue(updated.contains("minecraft_version_range=[26.3]"));
        assertTrue(updated.contains("loader_version=0.19.5"));
        assertTrue(updated.contains("loom_version=1.17.21"));
        assertTrue(updated.contains("fabric_api_version=0.163.0+26.3"));
        assertTrue(updated.contains("neo_version=26.3.0.88"));
    }

    @Test void buildPatcherUsesLoaderSpecificOfficialGradleWrapper() throws Exception {
        Path fabric = temp.resolve("fabric-build");
        Path neo = temp.resolve("neo-build");
        Files.createDirectories(fabric.resolve("gradle/wrapper"));
        Files.createDirectories(neo.resolve("gradle/wrapper"));
        Files.writeString(fabric.resolve("build.gradle"), """
            plugins { id 'fabric-loom' version '1.17.21' }
            dependencies {
                minecraft "com.mojang:minecraft:${project.minecraft_version}"
                mappings loom.officialMojangMappings()
                modImplementation "net.fabricmc:fabric-loader:${project.loader_version}"
                modImplementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_api_version}"
            }
            """);
        Files.writeString(neo.resolve("build.gradle"), """
            plugins { id 'net.neoforged.moddev' version '2.0.141' }
            """);
        Files.writeString(fabric.resolve("gradle/wrapper/gradle-wrapper.properties"),
            "distributionUrl=https\\://services.gradle.org/distributions/gradle-8.14-bin.zip\n");
        Files.writeString(neo.resolve("gradle/wrapper/gradle-wrapper.properties"),
            "distributionUrl=https\\://services.gradle.org/distributions/gradle-8.14-all.zip\n");

        new BuildFilePatcher().patch(fabric, "26.3");
        new BuildFilePatcher().patch(neo, "26.3");

        assertTrue(Files.readString(fabric.resolve("gradle/wrapper/gradle-wrapper.properties"))
            .contains("gradle-9.6.0-bin.zip"));
        String fabricBuild = Files.readString(fabric.resolve("build.gradle"));
        assertTrue(fabricBuild.contains("id 'net.fabricmc.fabric-loom'"));
        assertFalse(fabricBuild.contains("mappings loom.officialMojangMappings()"));
        assertTrue(fabricBuild.contains("implementation \"net.fabricmc:fabric-loader:"));
        assertTrue(fabricBuild.contains("implementation \"net.fabricmc.fabric-api:fabric-api:"));
        assertTrue(Files.readString(neo.resolve("gradle/wrapper/gradle-wrapper.properties"))
            .contains("gradle-9.2.1-all.zip"));
    }

    @Test void longUpgradePathIncludesBoth26xResourceHops() throws Exception {
        Path root = temp.resolve("long-pack");
        Files.createDirectories(root.resolve("assets/example"));
        Files.writeString(root.resolve("pack.mcmeta"), """
            {"pack":{"pack_format":84,"description":"test"}}
            """);

        new ResourcePatcher().patch(root, "1.21.1", "26.3", false);

        assertTrue(Files.readString(root.resolve("pack.mcmeta")).contains("\"pack_format\": 97.1"));
    }

    @Test void unixGradleWrapperPreparationLeavesWindowsBatchFileAlone() throws Exception {
        Path root = temp.resolve("wrapper-permissions");
        Files.createDirectories(root);
        Path unixWrapper = root.resolve("gradlew");
        Path windowsWrapper = root.resolve("gradlew.bat");
        Files.writeString(unixWrapper, "#!/bin/sh\n");
        Files.writeString(windowsWrapper, "@echo off\n");
        byte[] batchBefore = Files.readAllBytes(windowsWrapper);

        BuildRunner.ensureUnixGradleWrapperExecutable(root, false);

        assertTrue(Files.isExecutable(unixWrapper));
        assertArrayEquals(batchBefore, Files.readAllBytes(windowsWrapper));
    }
}
