package com.autoporter;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Writes and reads the reference templates in {@code auto-porter/templates/<version>/}: a {@code gradle.properties}
 * and a {@code template-info.json} per Minecraft version, generated from {@link VersionDatabase} and
 * {@link BuildTooling}. The porter itself reads versions straight from those classes; the templates are a
 * human-readable copy kept in sync by {@code --setup-templates}.
 */
public class TemplateManager {
    private final Path templatesDir;

    public TemplateManager(Path templatesDir) {
        this.templatesDir = templatesDir;
    }

    /** (Re)writes the template of every supported version. */
    public void writeAll() throws IOException {
        for (String version : VersionDatabase.allVersions()) {
            writeTemplate(version, templatesDir.resolve(version));
            System.out.println("  [OK]   " + version);
        }
    }

    public void writeTemplate(String version, Path dest) throws IOException {
        VersionDatabase.VersionInfo info = VersionDatabase.get(version);
        if (info == null) return;
        Files.createDirectories(dest);
        BuildTooling.Tooling tooling = BuildTooling.get(version);

        StringBuilder props = new StringBuilder();
        props.append("# Auto-Porter template for Minecraft ").append(version).append("\n");
        props.append("minecraft_version=").append(info.mcVersion()).append("\n");
        props.append("fabric_loader_version=").append(info.fabricLoaderVersion()).append("\n");
        props.append("fabric_api_version=").append(info.fabricApiVersion()).append("\n");
        if (info.architecturyVersion() != null) props.append("architectury_api_version=").append(info.architecturyVersion()).append("\n");
        if (info.neoforgeVersion() != null) props.append("neoforge_version=").append(info.neoforgeVersion()).append("\n");
        if (info.forgeVersion() != null) props.append("forge_version=").append(info.forgeVersion()).append("\n");
        props.append("loom_version=").append(info.loomVersion()).append("\n");
        if (tooling != null && tooling.modDevGradle() != null) props.append("moddev_version=").append(tooling.modDevGradle()).append("\n");
        Files.writeString(dest.resolve("gradle.properties"), props.toString());

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("mcVersion",    info.mcVersion());
        meta.put("fabricLoader", info.fabricLoaderVersion());
        meta.put("fabricApi",    info.fabricApiVersion());
        meta.put("architectury", info.architecturyVersion());
        meta.put("neoforge",     info.neoforgeVersion());
        meta.put("forge",        info.forgeVersion());
        meta.put("hasFabric",    info.hasFabric());
        meta.put("hasForge",     info.hasForge());
        meta.put("hasNeoForge",  info.hasNeoForge());
        meta.put("loom",         info.loomVersion());
        if (tooling != null) {
            meta.put("java",           tooling.javaVersion());
            meta.put("fabricGradle",   tooling.fabricGradle());
            meta.put("neoForgeGradle", tooling.neoForgeGradle());
            meta.put("modDevGradle",   tooling.modDevGradle());
        }
        Files.writeString(dest.resolve("template-info.json"),
            new GsonBuilder().setPrettyPrinting().serializeNulls().create().toJson(meta) + "\n");
    }

    /** Load the template info for a version. */
    public Map<String, Object> loadTemplateInfo(String version) throws IOException {
        Path info = templatesDir.resolve(version).resolve("template-info.json");
        if (!Files.exists(info)) return Map.of();
        @SuppressWarnings("unchecked")
        Map<String, Object> map = new Gson().fromJson(Files.readString(info), Map.class);
        return map;
    }

    public boolean hasTemplate(String version) {
        return Files.exists(templatesDir.resolve(version).resolve("gradle.properties"));
    }
}
