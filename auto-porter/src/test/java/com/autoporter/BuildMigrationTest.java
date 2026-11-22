package com.autoporter;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Build tooling migration: NeoForge and Fabric 1.21.1 → 26.1.2, hard-coded and property-driven versions. */
class BuildMigrationTest {
    @TempDir Path temp;

    private static final String NEO = VersionDatabase.get("26.1.2").neoforgeVersion();
    private static final String MDG = BuildTooling.get("26.1.2").modDevGradle();

    private static Path write(Path root, String rel, String content) throws Exception {
        Path file = root.resolve(rel);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        return file;
    }

    /** The reported bug: hard-coded MDG and neoForge.version were left at 1.21.1 values. */
    @Test void neoForge1_21_1To26_1_2UpdatesHardCodedAndPropertyVersions() throws Exception {
        Path root = temp.resolve("neo-mod");
        write(root, "build.gradle", """
            plugins {
                id 'java-library'
                id 'net.neoforged.moddev' version '2.0.107'
            }
            java.toolchain.languageVersion = JavaLanguageVersion.of(21)
            neoForge {
                version = "21.1.190"
                parchment {
                    mappingsVersion = project.parchment_mappings_version
                    minecraftVersion = project.parchment_minecraft_version
                }
                runs { client { client() } }
            }
            """);
        write(root, "gradle.properties", """
            minecraft_version=1.21.1
            minecraft_version_range=[1.21.1]
            neo_version=21.1.190
            neo_version_range=[21.1.0,)
            loader_version_range=[4,)
            moddev_version=2.0.107
            parchment_minecraft_version=1.21.1
            parchment_mappings_version=2024.11.17
            """);
        write(root, "gradle/wrapper/gradle-wrapper.properties", """
            distributionUrl=https\\://services.gradle.org/distributions/gradle-8.10.2-bin.zip
            distributionSha256Sum=0123456789abcdef
            """);
        write(root, "src/main/resources/META-INF/neoforge.mods.toml", """
            loaderVersion="${loader_version_range}"
            [[dependencies.examplemod]]
                modId="neoforge"
                versionRange="[21.1.0,)"
            [[dependencies.examplemod]]
                modId="minecraft"
                versionRange="[1.21.1,1.22)"
            """);
        write(root, "src/main/resources/examplemod.mixins.json", "{ \"compatibilityLevel\": \"JAVA_21\" }");

        StaleVersionCheck check = StaleVersionCheck.snapshot(root, "1.21.1");
        new BuildFilePatcher().patch(root, "1.21.1", "26.1.2", false, false);
        new GradlePropertiesPatcher().patch(root, "26.1.2", false, false);
        new ModMetaPatcher().patch(root, "26.1.2");
        PatchResult stale = check.verify(root, "26.1.2");

        String build = Files.readString(root.resolve("build.gradle"));
        assertTrue(build.contains("id 'net.neoforged.moddev' version '" + MDG + "'"), build);
        assertTrue(build.contains("version = \"" + NEO + "\""), build);
        assertTrue(build.contains("JavaLanguageVersion.of(25)"), build);
        assertTrue(build.contains("// Removed by MC Mod Porter: Parchment mappings are not used on unobfuscated 26.x — parchment {"), build);
        assertFalse(build.contains("\n        parchment {"), "parchment block must be commented out");
        assertTrue(build.contains("runs { client { client() } }"), "the rest of neoForge { } is kept");

        String props = Files.readString(root.resolve("gradle.properties"));
        assertTrue(props.contains("minecraft_version=26.1.2"));
        assertTrue(props.contains("minecraft_version_range=[26.1.2]"));
        assertTrue(props.contains("neo_version=" + NEO));
        assertTrue(props.contains("neo_version_range=[" + NEO + ",)"));
        assertTrue(props.contains("moddev_version=" + MDG));
        assertTrue(props.contains("# parchment_minecraft_version=1.21.1"));

        String wrapper = Files.readString(root.resolve("gradle/wrapper/gradle-wrapper.properties"));
        assertTrue(wrapper.contains("gradle-" + BuildTooling.get("26.1.2").neoForgeGradle() + "-bin.zip"), wrapper);
        assertFalse(wrapper.contains("distributionSha256Sum"), "the old checksum would break the new download");

        String toml = Files.readString(root.resolve("src/main/resources/META-INF/neoforge.mods.toml"));
        assertTrue(toml.contains("versionRange=\"[" + NEO + ",)\""), toml);
        assertTrue(toml.contains("versionRange=\"[26.1.2,)\""), toml);
        assertTrue(toml.contains("loaderVersion=\"${loader_version_range}\""), "placeholders are kept");
        assertTrue(Files.readString(root.resolve("src/main/resources/examplemod.mixins.json")).contains("JAVA_25"));

        assertTrue(stale.changes().isEmpty(), "stale versions left: " + stale.changes());
    }

    @Test void neoForgeKotlinDslIsPatchedToo() throws Exception {
        Path root = temp.resolve("neo-kts");
        write(root, "build.gradle.kts", """
            plugins {
                id("net.neoforged.moddev") version "2.0.107"
            }
            java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))
            neoForge {
                version = "21.1.190"
            }
            """);
        new BuildFilePatcher().patch(root, "1.21.1", "26.1.2", false, false);
        String build = Files.readString(root.resolve("build.gradle.kts"));
        assertTrue(build.contains("id(\"net.neoforged.moddev\") version \"" + MDG + "\""), build);
        assertTrue(build.contains("version = \"" + NEO + "\""), build);
        assertTrue(build.contains("JavaLanguageVersion.of(25)"), build);
    }

    @Test void staleVersionCheckReportsWhatThePorterCouldNotChange() throws Exception {
        Path root = temp.resolve("stale");
        write(root, "build.gradle", """
            plugins { id 'net.neoforged.moddev' version '2.0.107' }
            dependencies { implementation "mezz.jei:jei-1.21.1-neoforge:19.21.0.247" }
            """);
        write(root, "gradle.properties", "minecraft_version=1.21.1\n");
        StaleVersionCheck check = StaleVersionCheck.snapshot(root, "1.21.1");
        new BuildFilePatcher().patch(root, "1.21.1", "26.1.2", false, false);
        new GradlePropertiesPatcher().patch(root, "26.1.2");
        PatchResult result = check.verify(root, "26.1.2");
        assertEquals(1, result.changes().size(), result.changes().toString());
        assertTrue(result.changes().get(0).contains("jei-1.21.1"));
        assertTrue(result.message().startsWith("WARNING"));
    }

    @Test void fabric1_21_1To26_1_2MigratesLoomConfigurationsAndWrapper() throws Exception {
        Path root = temp.resolve("fabric-mod");
        write(root, "build.gradle", """
            plugins {
                id 'fabric-loom' version '1.7-SNAPSHOT'
                id 'maven-publish'
            }
            java {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
            dependencies {
                minecraft "com.mojang:minecraft:${project.minecraft_version}"
                mappings "net.fabricmc:yarn:${project.yarn_mappings}:v2"
                modImplementation "net.fabricmc:fabric-loader:${project.loader_version}"
                modImplementation "net.fabricmc.fabric-api:fabric-api:0.102.0+1.21.1"
                modCompileOnly "maven.modrinth:modmenu:11.0.3"
                modApi "me.shedaniel.cloth:cloth-config-fabric:15.0.140"
                modLocalRuntime "maven.modrinth:modmenu:11.0.3"
            }
            tasks.withType(JavaCompile).configureEach { it.options.release = 21 }
            publishing { publications { mavenJava(MavenPublication) { artifact(remapJar) { builtBy remapJar } } } }
            """);
        write(root, "gradle.properties", """
            minecraft_version=1.21.1
            yarn_mappings=1.21.1+build.3
            loader_version=0.16.5
            fabric_version=0.102.0+1.21.1
            loom_version=1.7-SNAPSHOT
            """);
        write(root, "gradle/wrapper/gradle-wrapper.properties",
            "distributionUrl=https\\://services.gradle.org/distributions/gradle-8.8-bin.zip\n");
        write(root, "src/main/resources/fabric.mod.json", """
            { "id": "examplemod", "depends": { "fabricloader": ">=0.16.5", "minecraft": "~1.21.1", "fabric": "*" } }
            """);

        StaleVersionCheck check = StaleVersionCheck.snapshot(root, "1.21.1");
        new BuildFilePatcher().patch(root, "1.21.1", "26.1.2", false, true);
        new GradlePropertiesPatcher().patch(root, "26.1.2", false, true);
        new ModMetaPatcher().patch(root, "26.1.2");
        PatchResult stale = check.verify(root, "26.1.2");
        VersionDatabase.VersionInfo info = VersionDatabase.get("26.1.2");

        String build = Files.readString(root.resolve("build.gradle"));
        assertTrue(build.contains("id 'net.fabricmc.fabric-loom' version '" + info.loomVersion() + "'"), build);
        assertFalse(build.contains("mappings"), build);
        assertFalse(build.contains("modImplementation") || build.contains("modCompileOnly") || build.contains("modApi")
            || build.contains("modLocalRuntime"), build);
        assertTrue(build.contains("implementation \"net.fabricmc.fabric-api:fabric-api:" + info.fabricApiVersion() + "\""), build);
        assertTrue(build.contains("compileOnly \"maven.modrinth:modmenu"), build);
        assertTrue(build.contains("api \"me.shedaniel.cloth"), build);
        assertTrue(build.contains("localRuntime \"maven.modrinth:modmenu"), build);
        assertTrue(build.contains("JavaVersion.VERSION_25") && build.contains("release = 25"), build);
        assertTrue(build.contains("artifact(jar) { builtBy jar }"), build);

        String props = Files.readString(root.resolve("gradle.properties"));
        assertTrue(props.contains("loader_version=" + info.fabricLoaderVersion()));
        assertTrue(props.contains("fabric_version=" + info.fabricApiVersion()));
        assertTrue(props.contains("loom_version=" + info.loomVersion()));
        assertTrue(props.contains("# yarn_mappings=1.21.1+build.3"), props);

        assertTrue(Files.readString(root.resolve("gradle/wrapper/gradle-wrapper.properties"))
            .contains("gradle-" + BuildTooling.get("26.1.2").fabricGradle() + "-bin.zip"));
        String fmj = Files.readString(root.resolve("src/main/resources/fabric.mod.json"));
        assertTrue(fmj.contains("\"fabric-api\": \"*\"") && !fmj.contains("\"fabric\":"), fmj);
        assertTrue(fmj.contains("\"minecraft\": \"~26.1.2\""), fmj);
        assertTrue(stale.changes().isEmpty(), "stale versions left: " + stale.changes());
    }

    @Test void remappedYarnProjectStaysOnMojangMappingsBeforeTwentySix() throws Exception {
        Path root = temp.resolve("yarn-to-mojang");
        write(root, "build.gradle", """
            plugins { id 'fabric-loom' version '1.7-SNAPSHOT' }
            dependencies {
                minecraft "com.mojang:minecraft:1.20.1"
                mappings "net.fabricmc:yarn:1.20.1+build.10:v2"
                modImplementation "net.fabricmc:fabric-loader:0.15.11"
            }
            """);
        new BuildFilePatcher().patch(root, "1.20.1", "1.21.1", false, true);
        String build = Files.readString(root.resolve("build.gradle"));
        assertTrue(build.contains("mappings loom.officialMojangMappings()"), build);
        assertFalse(build.contains("net.fabricmc:yarn"), build);
        assertTrue(build.contains("com.mojang:minecraft:1.21.1"), build);
        assertTrue(build.contains("fabric-loader:" + VersionDatabase.get("1.21.1").fabricLoaderVersion()), build);
        assertTrue(build.contains("id 'fabric-loom' version '" + VersionDatabase.get("1.21.1").loomVersion() + "'"), build);
    }

    @Test void javaToolchainFollowsTheTargetVersion() throws Exception {
        Path root = temp.resolve("java");
        write(root, "build.gradle", "java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }\n"
            + "java { sourceCompatibility = JavaVersion.VERSION_21 }\n");
        new BuildFilePatcher().patch(root, "1.21.1", "1.18.2", false, false);
        String build = Files.readString(root.resolve("build.gradle"));
        assertTrue(build.contains("JavaLanguageVersion.of(17)") && build.contains("VERSION_17"), build);
        new BuildFilePatcher().patch(root, "1.18.2", "1.16.5", false, false);
        build = Files.readString(root.resolve("build.gradle"));
        assertTrue(build.contains("JavaLanguageVersion.of(8)") && build.contains("VERSION_1_8"), build);
    }

    /** Full CLI pipeline (no Gradle run) on the reported NeoForge project. */
    @Test void fullPortPipelineFixesTheReportedNeoForgeProject() throws Exception {
        Path root = temp.resolve("reported-neo");
        write(root, "build.gradle", """
            plugins { id 'net.neoforged.moddev' version '2.0.107' }
            java.toolchain.languageVersion = JavaLanguageVersion.of(21)
            neoForge { version = "21.1.190" }
            """);
        write(root, "gradle.properties", "minecraft_version=1.21.1\nneo_version=21.1.190\n");
        write(root, "src/main/java/example/Mod.java", "package example;\nclass Mod {}\n");
        AutoPorterMain.portMod(root.toString(), "1.21.1", "26.1.2",
            AutoPorterMain.Options.parse(List.of("--no-build")));
        Path out = AutoPorterMain.outputDirectoryFor(root, "26.1.2");
        String build = Files.readString(out.resolve("build.gradle"));
        assertTrue(build.contains("version '" + MDG + "'") && build.contains("\"" + NEO + "\""), build);
        String report = Files.readString(out.resolve("build/port-report.json"));
        assertTrue(report.contains("\"step\": \"stale-versions\""));
        assertTrue(report.contains("\"sourceMappings\": \"MOJANG\""));
    }

    @Test void cliOptionsAreParsedAndValidated() {
        var opts = AutoPorterMain.Options.parse(List.of("--remap", "--source-mappings", "yarn", "--no-build"));
        assertTrue(opts.remap() && !opts.build() && opts.sourceMappings() == SourceRemapper.Mappings.YARN);
        assertEquals(SourceRemapper.Mappings.MOJANG,
            AutoPorterMain.Options.parse(List.of("--source-mappings=mojang")).sourceMappings());
        assertThrows(IllegalArgumentException.class, () -> AutoPorterMain.Options.parse(List.of("--nobuild")));
        assertThrows(IllegalArgumentException.class, () -> AutoPorterMain.Options.parse(List.of("--remap", "--no-remap")));
        assertThrows(IllegalArgumentException.class, () -> AutoPorterMain.Options.parse(List.of("--source-mappings", "intermediary")));
        assertEquals(List.of("--source-mappings", "yarn", "--remap"), MainWindow.portFlags("Yarn", MainWindow.REMAP_ALWAYS));
        assertEquals(List.of("--no-remap"), MainWindow.portFlags("Auto Detect", MainWindow.REMAP_NEVER));
    }
}
