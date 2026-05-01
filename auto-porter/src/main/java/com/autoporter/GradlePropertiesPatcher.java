package com.autoporter;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Updates gradle.properties with correct version numbers for the target MC version.
 * Handles both Architectury multi-platform and single-platform Fabric/Forge setups.
 */
public class GradlePropertiesPatcher {

    public PatchResult patch(Path modRoot, String targetVersion) throws IOException {
        return patch(modRoot, targetVersion, false);
    }

    public PatchResult patch(Path modRoot, String targetVersion, boolean useYarn) throws IOException {
        VersionDatabase.VersionInfo info = VersionDatabase.get(targetVersion);
        if (info == null) {
            return PatchResult.failure("Unknown version: " + targetVersion);
        }

        List<Path> propsFiles = findGradleProperties(modRoot);
        if (propsFiles.isEmpty()) {
            return PatchResult.failure("No gradle.properties found in " + modRoot);
        }

        List<String> changes = new ArrayList<>();
        for (Path props : propsFiles) {
            changes.addAll(patchFile(props, info, useYarn));
        }

        return PatchResult.success("gradle.properties updated", changes);
    }

    private List<Path> findGradleProperties(Path root) throws IOException {
        List<Path> found = new ArrayList<>();
        for (Path candidate : List.of(
            root.resolve("gradle.properties"),
            root.resolve("common/gradle.properties"),
            root.resolve("fabric/gradle.properties"),
            root.resolve("neoforge/gradle.properties")
        )) {
            if (Files.exists(candidate)) found.add(candidate);
        }
        return found;
    }

    private List<String> patchFile(Path file, VersionDatabase.VersionInfo info, boolean useYarn) throws IOException {
        String content = Files.readString(file);
        List<String> changes = new ArrayList<>();
        String original = content;

        content = replaceProperty(content, "minecraft_version",    info.mcVersion(),           changes);
        content = replaceProperty(content, "fabric_loader_version",info.fabricLoaderVersion(),  changes);
        content = replaceProperty(content, "loader_version",       info.fabricLoaderVersion(),  changes);
        content = replaceProperty(content, "fabric_version",       info.fabricApiVersion(),     changes);
        content = replaceProperty(content, "fabric_api_version",   info.fabricApiVersion(),     changes);
        content = replaceProperty(content, "architectury_api_version", info.architecturyVersion(), changes);

        if (info.neoforgeVersion() != null) {
            content = replaceProperty(content, "neoforge_version", info.neoforgeVersion(), changes);
        }
        if (info.forgeVersion() != null) {
            content = replaceProperty(content, "forge_version", info.forgeVersion(), changes);
        }

        // Update yarn_mappings if Yarn was chosen and the version has a known build
        if (useYarn && info.yarnMappings() != null) {
            String newYarn = info.yarnMappings();
            if (content.contains("yarn_mappings")) {
                // Update existing property
                content = replaceProperty(content, "yarn_mappings", newYarn, changes);
            } else {
                // Add it after minecraft_version line
                String insertion = "\nyarn_mappings=" + newYarn;
                content = content.replaceFirst(
                    "(?m)(^minecraft_version\\s*=\\s*.+$)",
                    "$1" + insertion.replace("\\", "\\\\").replace("$", "\\$"));
                changes.add("yarn_mappings: added " + newYarn);
            }
        }

        if (!content.equals(original)) {
            Files.writeString(file, content);
        }
        return changes;
    }

    private String replaceProperty(String content, String key, String value, List<String> changes) {
        if (value == null) return content;
        // Match: key = old_value  (with optional spaces around =)
        Pattern p = Pattern.compile("(?m)^(" + Pattern.quote(key) + "\s*=\s*)(.+)$");
        Matcher m = p.matcher(content);
        if (m.find()) {
            String oldVal = m.group(2).trim();
            if (!oldVal.equals(value)) {
                changes.add(key + ": " + oldVal + " → " + value);
            }
            return m.replaceAll("$1" + Matcher.quoteReplacement(value));
        }
        return content;
    }
}
