package com.autoporter;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.*;

/**
 * Updates fabric.mod.json and mods.toml with correct version constraints
 * for the target Minecraft version.
 */
public class ModMetaPatcher {

    public PatchResult patch(Path modRoot, String targetVersion) throws IOException {
        VersionDatabase.VersionInfo info = VersionDatabase.get(targetVersion);
        if (info == null) return PatchResult.failure("Unknown version: " + targetVersion);

        List<String> changes = new ArrayList<>();
        changes.addAll(patchFabricModJson(modRoot, info));
        changes.addAll(patchModsToml(modRoot, info));
        changes.addAll(patchMixinsJson(modRoot, info));
        return PatchResult.success("Mod metadata updated", changes);
    }

    // ── fabric.mod.json ──────────────────────────────────────────────────────

    private List<String> patchFabricModJson(Path root, VersionDatabase.VersionInfo info) throws IOException {
        List<String> changes = new ArrayList<>();
        List<Path> files = findFiles(root, "fabric.mod.json");
        for (Path file : files) {
            String content = Files.readString(file);
            JsonObject obj;
            try { obj = JsonParser.parseString(content).getAsJsonObject(); }
            catch (Exception e) { continue; }

            JsonObject depends = obj.has("depends") ? obj.getAsJsonObject("depends") : new JsonObject();

            // Update minecraft version constraint
            String mcConstraint = "~" + info.mcVersion();
            if (depends.has("minecraft")) {
                changes.add("fabric.mod.json minecraft: " + depends.get("minecraft").getAsString() + " → " + mcConstraint);
            }
            depends.addProperty("minecraft", mcConstraint);

            // Update fabricloader constraint
            String loaderConstraint = ">=" + info.fabricLoaderVersion();
            depends.addProperty("fabricloader", loaderConstraint);
            changes.add("fabric.mod.json fabricloader → " + loaderConstraint);

            // Update architectury constraint if present
            if (depends.has("architectury") && info.architecturyVersion() != null) {
                String archConstraint = ">=" + info.architecturyVersion();
                depends.addProperty("architectury", archConstraint);
                changes.add("fabric.mod.json architectury → " + archConstraint);
            }

            obj.add("depends", depends);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.writeString(file, gson.toJson(obj));
        }
        return changes;
    }

    // ── META-INF/mods.toml (NeoForge) ────────────────────────────────────────

    private List<String> patchModsToml(Path root, VersionDatabase.VersionInfo info) throws IOException {
        List<String> changes = new ArrayList<>();
        List<Path> files = findFiles(root, "mods.toml");
        for (Path file : files) {
            String content = Files.readString(file);
            String updated = content;

            // Patch loaderVersion
            updated = patchTomlKey(updated, "loaderVersion", "\"[" + info.neoforgeVersion() + ",)\"", changes);
            // Patch minecraft version dependency
            updated = patchTomlVersionRange(updated, "minecraft", info.mcVersion(), changes);

            if (!updated.equals(content)) Files.writeString(file, updated);
        }
        return changes;
    }

    private String patchTomlKey(String content, String key, String newVal, List<String> changes) {
        Pattern p = Pattern.compile("(?m)^(" + Pattern.quote(key) + "\s*=\s*)(.+)$");
        Matcher m = p.matcher(content);
        if (m.find()) {
            String old = m.group(2).trim();
            if (!old.equals(newVal)) changes.add("mods.toml " + key + ": " + old + " → " + newVal);
            return m.replaceAll("$1" + Matcher.quoteReplacement(newVal));
        }
        return content;
    }

    private String patchTomlVersionRange(String content, String depId, String mcVer, List<String> changes) {
        // matches: versionRange = "[1.21.4,1.22)" near modId = "minecraft"
        Pattern p = Pattern.compile(
            "(modId\s*=\s*\"" + depId + "\".*?versionRange\s*=\s*\")([^\"]+)(\")",
            Pattern.DOTALL
        );
        Matcher m = p.matcher(content);
        if (m.find()) {
            String newRange = "[" + mcVer + ",)";
            changes.add("mods.toml " + depId + " versionRange → " + newRange);
            return m.replaceAll("$1" + Matcher.quoteReplacement(newRange) + "$3");
        }
        return content;
    }

    // ── *.mixins.json ────────────────────────────────────────────────────────

    private List<String> patchMixinsJson(Path root, VersionDatabase.VersionInfo info) throws IOException {
        List<String> changes = new ArrayList<>();
        // Mixins don't usually need version changes, but compatibilityLevel should match Java
        return changes;
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private List<Path> findFiles(Path root, String filename) throws IOException {
        List<Path> found = new ArrayList<>();
        if (!Files.exists(root)) return found;
        Files.walk(root)
            .filter(p -> p.getFileName().toString().equals(filename))
            .filter(p -> !p.toString().contains("build") && !p.toString().contains(".gradle"))
            .forEach(found::add);
        return found;
    }
}
