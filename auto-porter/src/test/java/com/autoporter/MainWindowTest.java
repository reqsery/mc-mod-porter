package com.autoporter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MainWindowTest {
    @TempDir Path tempDir;

    @Test
    void noArgumentsSelectGuiButCliArgumentsDoNot() {
        assertTrue(AutoPorterMain.shouldLaunchGui(new String[0]));
        assertFalse(AutoPorterMain.shouldLaunchGui(new String[] { "--help" }));
        assertFalse(AutoPorterMain.shouldLaunchGui(new String[] { "project", "1.21.1", "26.2" }));
    }

    @Test
    void detectsFabricVersionFromProjectData() throws Exception {
        Files.writeString(tempDir.resolve("gradle.properties"), "minecraft_version=1.21.11\n");
        Path resources = Files.createDirectories(tempDir.resolve("src/main/resources"));
        Files.writeString(resources.resolve("fabric.mod.json"), "{\"schemaVersion\":1}");

        MainWindow.ProjectInfo info = MainWindow.inspectProject(tempDir);
        assertEquals("1.21.11", info.version());
        assertEquals("Fabric", info.loader());
    }

    @Test
    void detectsNeoForgeWithoutFabricMetadata() throws Exception {
        Files.writeString(tempDir.resolve("gradle.properties"), "minecraft_version=26.2\nneoforge_version=26.2.0.10-beta\n");

        MainWindow.ProjectInfo info = MainWindow.inspectProject(tempDir);
        assertEquals("26.2", info.version());
        assertEquals("NeoForge", info.loader());
    }
}
