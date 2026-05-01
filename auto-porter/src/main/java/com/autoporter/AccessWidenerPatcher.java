package com.autoporter;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * Handles access widener / class tweaker migration between pre-26.x and 26.x.
 *
 * <p>Changes when porting TO 26.x:
 * <ul>
 *   <li>.accesswidener → .classtweaker (file renamed)</li>
 *   <li>Header namespace: "named" → "official"</li>
 *   <li>fabric.mod.json: "accessWidener" key → "classTweaker"</li>
 * </ul>
 *
 * <p>Changes when porting FROM 26.x (backport):
 * <ul>
 *   <li>.classtweaker → .accesswidener (file renamed)</li>
 *   <li>Header namespace: "official" → "named"</li>
 *   <li>fabric.mod.json: "classTweaker" key → "accessWidener"</li>
 * </ul>
 */
public class AccessWidenerPatcher {

    public PatchResult patch(Path modRoot, String targetVersion) throws IOException {
        boolean is26x = targetVersion.startsWith("26.");
        List<String> changes = new ArrayList<>();

        List<Path> resourceDirs = findResourceDirs(modRoot);
        if (resourceDirs.isEmpty()) {
            return PatchResult.success("No resource directories found — access widener skipped", List.of());
        }

        for (Path resourceDir : resourceDirs) {
            if (is26x) {
                changes.addAll(patchFor26x(resourceDir, modRoot));
            } else {
                changes.addAll(revertFrom26x(resourceDir, modRoot));
            }
        }

        if (changes.isEmpty()) {
            changes.add("No access widener / class tweaker files found");
        }
        return PatchResult.success("Access widener patched", changes);
    }

    // ── Forward: pre-26.x → 26.x ────────────────────────────────────────────

    private List<String> patchFor26x(Path resourceDir, Path modRoot) throws IOException {
        List<String> changes = new ArrayList<>();
        List<Path> awFiles = findByExtension(resourceDir, ".accesswidener");

        for (Path aw : awFiles) {
            // 1. Fix namespace in file content: named → official
            String content = Files.readString(aw);
            String updated = content.replaceFirst(
                "(?m)^(\\s*\\S+\\s+v\\d+\\s+)named(\\s*)",
                "$1official$2");
            // 2. Rename file
            String oldName = aw.getFileName().toString();
            String newName = oldName.replace(".accesswidener", ".classtweaker");
            Path ct = aw.resolveSibling(newName);
            Files.writeString(aw, updated);
            Files.move(aw, ct, StandardCopyOption.REPLACE_EXISTING);
            changes.add("Renamed " + oldName + " → " + newName + "; namespace named → official");

            // 3. Update fabric.mod.json and build.gradle references
            changes.addAll(updateFabricModJson(modRoot, oldName, newName, true));
            changes.addAll(updateBuildGradle(modRoot, oldName, newName));
        }
        return changes;
    }

    // ── Reverse: 26.x → pre-26.x ────────────────────────────────────────────

    private List<String> revertFrom26x(Path resourceDir, Path modRoot) throws IOException {
        List<String> changes = new ArrayList<>();
        List<Path> ctFiles = findByExtension(resourceDir, ".classtweaker");

        for (Path ct : ctFiles) {
            // 1. Fix namespace in file content: official → named
            String content = Files.readString(ct);
            String updated = content.replaceFirst(
                "(?m)^(\\s*\\S+\\s+v\\d+\\s+)official(\\s*)",
                "$1named$2");
            // 2. Rename file
            String oldName = ct.getFileName().toString();
            String newName = oldName.replace(".classtweaker", ".accesswidener");
            Path aw = ct.resolveSibling(newName);
            Files.writeString(ct, updated);
            Files.move(ct, aw, StandardCopyOption.REPLACE_EXISTING);
            changes.add("Renamed " + oldName + " → " + newName + "; namespace official → named");

            // 3. Update fabric.mod.json and build.gradle references
            changes.addAll(updateFabricModJson(modRoot, oldName, newName, false));
            changes.addAll(updateBuildGradle(modRoot, oldName, newName));
        }
        return changes;
    }

    // ── fabric.mod.json ──────────────────────────────────────────────────────

    /**
     * Updates fabric.mod.json:
     * - Renames the old filename to the new one in the value
     * - Renames the key: "accessWidener" ↔ "classTweaker"
     *
     * @param to26x true when going to 26.x (accessWidener→classTweaker), false for the reverse
     */
    private List<String> updateFabricModJson(Path modRoot, String oldFilename, String newFilename, boolean to26x) throws IOException {
        List<String> changes = new ArrayList<>();
        List<Path> candidates = List.of(
            modRoot.resolve("src/main/resources/fabric.mod.json"),
            modRoot.resolve("fabric/src/main/resources/fabric.mod.json"),
            modRoot.resolve("common/src/main/resources/fabric.mod.json")
        );

        for (Path fmj : candidates) {
            if (!Files.exists(fmj)) continue;
            String content = Files.readString(fmj);
            String updated = content;

            // Update filename reference
            updated = updated.replace("\"" + oldFilename + "\"", "\"" + newFilename + "\"");

            // Rename the JSON key
            if (to26x) {
                updated = updated.replace("\"accessWidener\"", "\"classTweaker\"");
            } else {
                updated = updated.replace("\"classTweaker\"", "\"accessWidener\"");
            }

            if (!updated.equals(content)) {
                Files.writeString(fmj, updated);
                String keyChange = to26x ? "accessWidener → classTweaker" : "classTweaker → accessWidener";
                changes.add(fmj.getFileName() + ": updated " + keyChange + " key and filename");
            }
        }
        return changes;
    }

    // ── build.gradle ─────────────────────────────────────────────────────────

    /**
     * Updates any build.gradle / build.gradle.kts that references the old filename.
     * Loom declares the access widener via accessWidenerPath or accessWidener, e.g.:
     *   accessWidenerPath = file("src/main/resources/mod.accesswidener")
     */
    private List<String> updateBuildGradle(Path modRoot, String oldFilename, String newFilename) throws IOException {
        List<String> changes = new ArrayList<>();
        List<Path> buildFiles = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(modRoot, 2)) {
            walk.filter(p -> {
                String n = p.getFileName().toString();
                return n.equals("build.gradle") || n.equals("build.gradle.kts");
            }).forEach(buildFiles::add);
        }
        for (Path bg : buildFiles) {
            String content = Files.readString(bg);
            String updated = content.replace(oldFilename, newFilename);
            if (!updated.equals(content)) {
                Files.writeString(bg, updated);
                changes.add(bg.getFileName() + ": updated access widener filename " + oldFilename + " → " + newFilename);
            }
        }
        return changes;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private List<Path> findResourceDirs(Path modRoot) {
        List<Path> dirs = new ArrayList<>();
        for (String candidate : List.of(
            "src/main/resources",
            "fabric/src/main/resources",
            "common/src/main/resources"
        )) {
            Path p = modRoot.resolve(candidate);
            if (Files.exists(p)) dirs.add(p);
        }
        return dirs;
    }

    private List<Path> findByExtension(Path dir, String ext) throws IOException {
        List<Path> results = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.filter(p -> p.toString().endsWith(ext)).forEach(results::add);
        }
        return results;
    }
}
