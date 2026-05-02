package com.autoporter;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Stream;

/**
 * Patches build.gradle files for cross-version migrations.
 *
 * <p>Most version changes are handled entirely by gradle.properties. However some
 * migrations require structural changes to build.gradle itself — most notably the
 * 26.1 Fabric Loom plugin-ID rename and the removal of the mappings line.</p>
 */
public class BuildFilePatcher {

    public PatchResult patch(Path modRoot, String targetVersion) throws IOException {
        return patch(modRoot, targetVersion, false);
    }

    public PatchResult patch(Path modRoot, String targetVersion, boolean useYarn) throws IOException {
        VersionDatabase.VersionInfo info = VersionDatabase.get(targetVersion);
        if (info == null) return PatchResult.failure("Unknown version: " + targetVersion);

        List<Path> buildFiles = findBuildFiles(modRoot);
        List<String> changes = new ArrayList<>();

        boolean is26x = targetVersion.startsWith("26.");

        for (Path file : buildFiles) {
            if (is26x) {
                changes.addAll(patchFor26x(file));
            } else {
                changes.addAll(revertFrom26x(file, useYarn, info));
            }
        }

        if (changes.isEmpty()) {
            changes.add("Scanned " + buildFiles.size() + " build file(s) — no structural changes needed");
        }
        return PatchResult.success("Build files patched", changes);
    }

    // ── 26.1+ patches ──────────────────────────────────────────────────────

    private List<String> patchFor26x(Path file) throws IOException {
        String content = Files.readString(file);
        String original = content;
        List<String> log = new ArrayList<>();
        String name = file.toString();

        // 1. Fabric Loom plugin ID rename
        //    id 'fabric-loom' version ...           →  id 'net.fabricmc.fabric-loom' version ...
        //    id 'net.fabricmc.fabric-loom-remap' …  →  id 'net.fabricmc.fabric-loom' version ...
        String after = content.replaceAll(
            "id ['\"]fabric-loom['\"]",
            "id 'net.fabricmc.fabric-loom'");
        if (!after.equals(content)) {
            log.add(name + ": renamed plugin id 'fabric-loom' → 'net.fabricmc.fabric-loom'");
            content = after;
        }
        after = content.replaceAll(
            "id ['\"]net\\.fabricmc\\.fabric-loom-remap['\"]",
            "id 'net.fabricmc.fabric-loom'");
        if (!after.equals(content)) {
            log.add(name + ": renamed plugin id 'net.fabricmc.fabric-loom-remap' → 'net.fabricmc.fabric-loom'");
            content = after;
        }

        // 2. Remove ALL mappings lines (not needed for unobfuscated 26.x — applies to
        //    officialMojangMappings, Yarn, layered mappings, and any other variant)
        after = content.replaceAll(
            "(?m)^[\\t ]*mappings\\s*=?\\s*loom\\.officialMojangMappings\\(\\)\\s*\\r?\\n", "");
        if (!after.equals(content)) {
            log.add(name + ": removed mappings = loom.officialMojangMappings() line");
            content = after;
        }
        after = content.replaceAll(
            "(?m)^[\\t ]*mappings\\s*loom\\.officialMojangMappings\\(\\)\\s*\\r?\\n", "");
        if (!after.equals(content)) {
            log.add(name + ": removed mappings loom.officialMojangMappings() line");
            content = after;
        }
        // Yarn: mappings "net.fabricmc:yarn:1.21.9+build.1:v2"
        after = content.replaceAll(
            "(?m)^[\\t ]*mappings\\s+['\"]net\\.fabricmc:yarn:[^'\"]+['\"]\\s*\\r?\\n", "");
        if (!after.equals(content)) {
            log.add(name + ": removed Yarn mappings line (not applicable in 26.x — code is unobfuscated)");
            content = after;
        }
        // Layered: mappings loom.layered { ... } (multi-line block)
        after = content.replaceAll(
            "(?m)^[\\t ]*mappings\\s+loom\\.layered\\s*\\{[^}]*\\}\\s*\\r?\\n", "");
        if (!after.equals(content)) {
            log.add(name + ": removed layered mappings block (not applicable in 26.x — code is unobfuscated)");
            content = after;
        }

        // 3. modImplementation → implementation for fabric-loader and fabric-api
        //    Both use modImplementation pre-26.x; 26.x uses plain implementation.
        after = content.replaceAll(
            "modImplementation (['\"])net\\.fabricmc:fabric-loader:",
            "implementation $1net.fabricmc:fabric-loader:");
        if (!after.equals(content)) {
            log.add(name + ": changed modImplementation → implementation for fabric-loader");
            content = after;
        }
        after = content.replaceAll(
            "modImplementation (['\"])net\\.fabricmc\\.fabric-api:fabric-api:",
            "implementation $1net.fabricmc.fabric-api:fabric-api:");
        if (!after.equals(content)) {
            log.add(name + ": changed modImplementation → implementation for fabric-api");
            content = after;
        }

        // 4. Java toolchain: 21 → 25
        after = content.replaceAll(
            "JavaLanguageVersion\\.of\\(21\\)",
            "JavaLanguageVersion.of(25)");
        if (!after.equals(content)) {
            log.add(name + ": updated Java toolchain 21 → 25");
            content = after;
        }
        after = content.replaceAll(
            "sourceCompatibility\\s*=\\s*JavaVersion\\.VERSION_21",
            "sourceCompatibility = JavaVersion.VERSION_25");
        if (!after.equals(content)) {
            log.add(name + ": updated sourceCompatibility 21 → 25");
            content = after;
        }
        after = content.replaceAll(
            "targetCompatibility\\s*=\\s*JavaVersion\\.VERSION_21",
            "targetCompatibility = JavaVersion.VERSION_25");
        if (!after.equals(content)) {
            log.add(name + ": updated targetCompatibility 21 → 25");
            content = after;
        }

        if (!content.equals(original)) {
            Files.writeString(file, content);
        }
        return log;
    }

    // ── Revert 26.x → pre-26.x ─────────────────────────────────────────────

    private List<String> revertFrom26x(Path file, boolean useYarn, VersionDatabase.VersionInfo info) throws IOException {
        String content = Files.readString(file);
        String original = content;
        List<String> log = new ArrayList<>();
        String name = file.toString();

        // 1. Revert plugin ID to fabric-loom-remap (the remapping variant used pre-26.x)
        String after = content.replaceAll(
            "id ['\"]net\\.fabricmc\\.fabric-loom['\"]",
            "id 'net.fabricmc.fabric-loom-remap'");
        if (!after.equals(content)) {
            log.add(name + ": reverted plugin id 'net.fabricmc.fabric-loom' → 'net.fabricmc.fabric-loom-remap'");
            content = after;
        }

        // 2. Re-add mappings line (required pre-26.x; 26.x removed it because code is unobfuscated).
        //    Insert after the minecraft dependency line if no mappings line is present.
        if (!content.contains("mappings ") && content.contains("com.mojang:minecraft:")) {
            String mappingsLine;
            String mappingsDesc;
            if (useYarn && info.yarnMappings() != null) {
                mappingsLine = "\t\tmappings \"net.fabricmc:yarn:" + info.yarnMappings() + ":v2\"";
                mappingsDesc = "added Yarn mappings line: " + info.yarnMappings();
            } else {
                mappingsLine = "\t\tmappings loom.officialMojangMappings()";
                mappingsDesc = "added mappings loom.officialMojangMappings() (Mojang official)";
            }
            after = content.replaceFirst(
                "(?m)(^[ \\t]*minecraft ['\"]com\\.mojang:minecraft:[^'\"]+['\"][ \\t]*)(\\r?\\n)",
                "$1$2" + mappingsLine.replace("\\", "\\\\").replace("$", "\\$") + "$2");
            if (!after.equals(content)) {
                log.add(name + ": " + mappingsDesc);
                content = after;
            }
        }

        // 2. Revert implementation → modImplementation for fabric-loader and fabric-api
        after = content.replaceAll(
            "(?<!mod)implementation (['\"])net\\.fabricmc:fabric-loader:",
            "modImplementation $1net.fabricmc:fabric-loader:");
        if (!after.equals(content)) {
            log.add(name + ": reverted implementation → modImplementation for fabric-loader");
            content = after;
        }
        after = content.replaceAll(
            "(?<!mod)implementation (['\"])net\\.fabricmc\\.fabric-api:fabric-api:",
            "modImplementation $1net.fabricmc.fabric-api:fabric-api:");
        if (!after.equals(content)) {
            log.add(name + ": reverted implementation → modImplementation for fabric-api");
            content = after;
        }

        // 3. Java toolchain: 25 → 21
        after = content.replaceAll(
            "JavaLanguageVersion\\.of\\(25\\)",
            "JavaLanguageVersion.of(21)");
        if (!after.equals(content)) {
            log.add(name + ": updated Java toolchain 25 → 21");
            content = after;
        }
        after = content.replaceAll(
            "sourceCompatibility\\s*=\\s*JavaVersion\\.VERSION_25",
            "sourceCompatibility = JavaVersion.VERSION_21");
        if (!after.equals(content)) {
            log.add(name + ": updated sourceCompatibility 25 → 21");
            content = after;
        }
        after = content.replaceAll(
            "targetCompatibility\\s*=\\s*JavaVersion\\.VERSION_25",
            "targetCompatibility = JavaVersion.VERSION_21");
        if (!after.equals(content)) {
            log.add(name + ": updated targetCompatibility 25 → 21");
            content = after;
        }

        if (!content.equals(original)) {
            Files.writeString(file, content);
        }
        return log;
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private List<Path> findBuildFiles(Path root) throws IOException {
        List<Path> files = new ArrayList<>();
        if (!Files.exists(root)) return files;
        // Only walk depth 2: root + direct submodules (fabric/, neoforge/, common/).
        // Depth 3+ risks picking up nested projects (e.g. another tool cloned inside the mod).
        try (Stream<Path> walk = Files.walk(root, 2)) {
            walk.filter(p -> {
                String name = p.getFileName().toString();
                if (!name.equals("build.gradle") && !name.equals("build.gradle.kts")) return false;
                Path dir = p.getParent();
                // Always include the mod root's build.gradle
                if (dir.equals(root)) return true;
                // For subdirectories: skip if they have their own settings.gradle —
                // that means it's a separate project, not a submodule of this mod.
                return !Files.exists(dir.resolve("settings.gradle"))
                    && !Files.exists(dir.resolve("settings.gradle.kts"));
            }).forEach(files::add);
        }
        return files;
    }
}
