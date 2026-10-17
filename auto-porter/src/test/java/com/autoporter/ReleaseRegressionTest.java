package com.autoporter;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReleaseRegressionTest {
    @TempDir Path temp;

    @Test void sharedJavaDetectionHandlesSupportedBuildSyntaxes() throws Exception {
        BuildRunner runner = new BuildRunner();
        for (String syntax : List.of("java.toolchain.languageVersion = JavaLanguageVersion.of(25)",
                "sourceCompatibility = '25'", "sourceCompatibility = JavaVersion.VERSION_25",
                "tasks.withType(JavaCompile).configureEach { it.options.release = 25 }",
                "tasks.withType<JavaCompile> { options.release.set(25) }")) {
            Files.writeString(temp.resolve("build.gradle"), syntax);
            assertEquals(25, runner.detectRequiredJavaVersion(temp), syntax);
        }
        Files.delete(temp.resolve("build.gradle"));
        Files.writeString(temp.resolve("build.gradle.kts"), "java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }");
        assertEquals(21, runner.detectRequiredJavaVersion(temp));
    }

    @Test void jdkDetectionRequiresCompilerAndMatchingRelease() throws Exception {
        Path jdk = Files.createDirectories(temp.resolve("JDK with spaces/bin")).getParent();
        Files.writeString(jdk.resolve("release"), "JAVA_VERSION=\"25.0.4\"\n");
        assertFalse(BuildRunner.isJavaHomeForVersion(jdk, 25));
        Files.writeString(jdk.resolve("bin/javac"), "");
        assertTrue(BuildRunner.isJavaHomeForVersion(jdk, 25));
        assertFalse(BuildRunner.isJavaHomeForVersion(jdk, 21));
        // Also proves package access from the GUI's package compiles.
        assertNotNull(new BuildRunner().detectJavaHome(Runtime.version().feature()));
    }

    @Test void sourceDetectionAndPortsHandleOlderAndNewerProjectsWithSpaces() throws Exception {
        for (String loader : List.of("Fabric", "NeoForge")) {
            for (String[] hop : List.of(new String[]{"1.20.4", "1.20.5"},
                    new String[]{"1.21.10", "1.21.11"}, new String[]{"26.2", "26.3"})) {
                Path root = Files.createDirectories(temp.resolve(loader + " project " + hop[0]));
                Files.writeString(root.resolve("gradle.properties"), "minecraft_version=" + hop[0]
                    + "\nloader_version=0.19.3\nfabric_api_version=old\nneo_version=old\n");
                Path resources = Files.createDirectories(root.resolve("src/main/resources/META-INF"));
                if (loader.equals("Fabric")) Files.writeString(resources.getParent().resolve("fabric.mod.json"),
                    "{\"schemaVersion\":1,\"id\":\"example\",\"version\":\"1\",\"depends\":{\"minecraft\":\"" + hop[0] + "\"}}");
                else Files.writeString(resources.resolve("neoforge.mods.toml"), "modLoader=\"javafml\"\nloaderVersion=\"[4,)\"\n");
                assertEquals(hop[0], MainWindow.inspectProject(root).version());
                assertEquals(loader, MainWindow.inspectProject(root).loader());
                AutoPorterMain.portMod(root.toString(), hop[0], hop[1], false, false);
                Path output = AutoPorterMain.outputDirectoryFor(root, hop[1]);
                assertEquals(hop[1], MainWindow.inspectProject(output).version());
                assertEquals(loader, MainWindow.inspectProject(output).loader());
                assertTrue(Files.isRegularFile(output.resolve("build/port-report.json")));
                assertTrue(Files.readString(root.resolve("gradle.properties")).contains("minecraft_version=" + hop[0]));
            }
        }
    }
}
