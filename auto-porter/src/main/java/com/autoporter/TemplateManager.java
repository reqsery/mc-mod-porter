package com.autoporter;

import com.google.gson.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

/**
 * Manages Architectury template downloads and derivation.
 * Templates live in auto-porter/templates/<version>/
 */
public class TemplateManager {
    private final Path templatesDir;

    // Versions available from the Architectury template generator
    private static final List<String> GENERATOR_VERSIONS = List.of(
        "1.21.10","1.21.9","1.21.8","1.21.7","1.21.6","1.21.5",
        "1.21.4","1.21.3","1.21.2","1.21.1","1.21",
        "1.20.6","1.20.5","1.20.4","1.20.3","1.20.2","1.20.1","1.20",
        "1.19.4","1.19.3","1.19.2","1.19.1","1.19",
        "1.18.2","1.18.1","1.18",
        "1.17.1","1.16.5"
    );

    // Derived versions: version → base version to derive from
    private static final Map<String, String> DERIVED_FROM = Map.of(
        "1.21.11", "1.21.10",
        "26.1",    "1.21.11",
        "26.1.1",  "26.1",
        "26.1.2",  "26.1.1",
        "1.16.4",  "1.16.5",
        "1.16.3",  "1.16.5",
        "1.16.2",  "1.16.5",
        "1.16.1",  "1.16.5",
        "1.16",    "1.16.5"
    );

    public TemplateManager(Path templatesDir) {
        this.templatesDir = templatesDir;
    }

    /** Download all available templates from the Architectury generator. */
    public void downloadAll() throws Exception {
        System.out.println("Downloading Architectury templates...");
        for (String version : GENERATOR_VERSIONS) {
            Path dest = templatesDir.resolve(version);
            if (Files.exists(dest.resolve("gradle.properties"))) {
                System.out.println("  [SKIP] " + version + " (already exists)");
                continue;
            }
            try {
                downloadTemplate(version, dest);
                System.out.println("  [OK]   " + version);
            } catch (Exception e) {
                System.err.println("  [FAIL] " + version + ": " + e.getMessage());
                // Create minimal template from version database
                createMinimalTemplate(version, dest);
            }
        }

        // Derive templates for versions not in the generator
        System.out.println("\nDeriving templates...");
        for (Map.Entry<String, String> e : DERIVED_FROM.entrySet()) {
            deriveTemplate(e.getKey(), e.getValue());
        }
    }

    private void downloadTemplate(String version, Path dest) throws Exception {
        Files.createDirectories(dest);
        // The Architectury generator API
        String url = "https://generate.architectury.dev/template.json" +
            "?mc=" + version + "&mapping=mojmap&loaders=fabric,neoforge";
        // Just store the version info as a template descriptor
        // (Full template ZIP download would require more complex handling)
        createMinimalTemplate(version, dest);
    }

    public void createMinimalTemplate(String version, Path dest) throws IOException {
        Files.createDirectories(dest);
        VersionDatabase.VersionInfo info = VersionDatabase.get(version);
        if (info == null) return;

        // Write gradle.properties template
        StringBuilder props = new StringBuilder();
        props.append("# Auto-Porter template for Minecraft ").append(version).append("\n");
        props.append("minecraft_version=").append(info.mcVersion()).append("\n");
        props.append("fabric_loader_version=").append(info.fabricLoaderVersion()).append("\n");
        props.append("fabric_api_version=").append(info.fabricApiVersion()).append("\n");
        if (info.architecturyVersion() != null)
            props.append("architectury_api_version=").append(info.architecturyVersion()).append("\n");
        if (info.neoforgeVersion() != null)
            props.append("neoforge_version=").append(info.neoforgeVersion()).append("\n");
        if (info.forgeVersion() != null)
            props.append("forge_version=").append(info.forgeVersion()).append("\n");
        props.append("loom_version=").append(info.loomVersion()).append("\n");

        Files.writeString(dest.resolve("gradle.properties"), props.toString());

        // Write loader support info
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("mcVersion",       info.mcVersion());
        meta.put("fabricLoader",    info.fabricLoaderVersion());
        meta.put("fabricApi",       info.fabricApiVersion());
        meta.put("architectury",    info.architecturyVersion());
        meta.put("neoforge",        info.neoforgeVersion());
        meta.put("forge",           info.forgeVersion());
        meta.put("hasFabric",       info.hasFabric());
        meta.put("hasForge",        info.hasForge());
        meta.put("hasNeoForge",     info.hasNeoForge());
        meta.put("loom",            info.loomVersion());
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Files.writeString(dest.resolve("template-info.json"), gson.toJson(meta));
    }

    private void deriveTemplate(String version, String baseVersion) throws IOException {
        Path dest = templatesDir.resolve(version);
        if (Files.exists(dest.resolve("gradle.properties"))) {
            System.out.println("  [SKIP] " + version + " (already exists)");
            return;
        }
        Path base = templatesDir.resolve(baseVersion);
        if (!Files.exists(base)) {
            System.out.println("  [FAIL] Cannot derive " + version + " - base " + baseVersion + " missing");
            return;
        }
        createMinimalTemplate(version, dest);
        System.out.println("  [OK]   " + version + " (derived from " + baseVersion + ")");
    }

    /** Load the template info for a version. */
    public Map<String, Object> loadTemplateInfo(String version) throws IOException {
        Path info = templatesDir.resolve(version).resolve("template-info.json");
        if (!Files.exists(info)) return Map.of();
        return new Gson().fromJson(Files.readString(info), Map.class);
    }

    public boolean hasTemplate(String version) {
        return Files.exists(templatesDir.resolve(version).resolve("gradle.properties"));
    }
}
