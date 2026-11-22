package com.autoporter;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.*;

/**
 * Updates mod metadata for the target Minecraft version: {@code fabric.mod.json}, {@code mods.toml} /
 * {@code neoforge.mods.toml} and the {@code compatibilityLevel} of {@code *.mixins.json}.
 *
 * <p>Values that are Gradle template placeholders (e.g. {@code "~${minecraft_version}"}) are left alone — they are
 * filled from {@code gradle.properties}, which {@link GradlePropertiesPatcher} already updates.
 */
public class ModMetaPatcher {

    public PatchResult patch(Path modRoot, String targetVersion) throws IOException {
        VersionDatabase.VersionInfo info = VersionDatabase.get(targetVersion);
        if (info == null) return PatchResult.failure("Unknown version: " + targetVersion);

        List<String> changes = new ArrayList<>();
        changes.addAll(patchFabricModJson(modRoot, info));
        changes.addAll(patchModsToml(modRoot, info));
        changes.addAll(patchMixinsJson(modRoot, BuildTooling.javaFor(targetVersion)));
        return PatchResult.success("Mod metadata updated", changes);
    }

    private static boolean isPlaceholder(JsonElement value) {
        return value != null && value.isJsonPrimitive() && value.getAsString().contains("${");
    }

    // ── fabric.mod.json ──────────────────────────────────────────────────────

    private List<String> patchFabricModJson(Path root, VersionDatabase.VersionInfo info) throws IOException {
        List<String> changes = new ArrayList<>();
        for (Path file : findFiles(root, "fabric.mod.json")) {
            JsonObject obj;
            try { obj = JsonParser.parseString(Files.readString(file)).getAsJsonObject(); }
            catch (Exception e) { continue; }

            JsonObject depends = obj.has("depends") && obj.get("depends").isJsonObject()
                ? obj.getAsJsonObject("depends") : new JsonObject();
            String name = root.relativize(file).toString().replace('\\', '/');

            String mcConstraint = "~" + info.mcVersion();
            if (isPlaceholder(depends.get("minecraft"))) {
                changes.add(name + ": minecraft is a placeholder (" + depends.get("minecraft").getAsString()
                    + ") — filled from gradle.properties");
            } else {
                if (depends.has("minecraft")) {
                    changes.add(name + ": minecraft " + depends.get("minecraft").getAsString() + " → " + mcConstraint);
                }
                depends.addProperty("minecraft", mcConstraint);
            }

            if (!isPlaceholder(depends.get("fabricloader"))) {
                String loaderConstraint = ">=" + info.fabricLoaderVersion();
                depends.addProperty("fabricloader", loaderConstraint);
                changes.add(name + ": fabricloader → " + loaderConstraint);
            }

            if (depends.has("architectury") && info.architecturyVersion() != null && !isPlaceholder(depends.get("architectury"))) {
                String archConstraint = ">=" + info.architecturyVersion();
                depends.addProperty("architectury", archConstraint);
                changes.add(name + ": architectury → " + archConstraint);
            }

            // The legacy "fabric" mod id for Fabric API is gone in 26.x.
            if (VersionDatabase.isUnobfuscated(info.mcVersion()) && depends.has("fabric") && !depends.has("fabric-api")) {
                JsonObject renamed = new JsonObject();
                for (var entry : depends.entrySet()) {
                    renamed.add(entry.getKey().equals("fabric") ? "fabric-api" : entry.getKey(), entry.getValue());
                }
                depends = renamed;
                changes.add(name + ": renamed dependency \"fabric\" → \"fabric-api\"");
            }

            obj.add("depends", depends);
            Files.writeString(file, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(obj));
        }
        return changes;
    }

    // ── META-INF/mods.toml / neoforge.mods.toml ──────────────────────────────

    private List<String> patchModsToml(Path root, VersionDatabase.VersionInfo info) throws IOException {
        List<String> changes = new ArrayList<>();
        List<Path> files = findFiles(root, "mods.toml");
        files.addAll(findFiles(root, "neoforge.mods.toml"));
        for (Path file : files) {
            String content = Files.readString(file);
            // loaderVersion is the FML loader range, not the NeoForge artifact version: left alone.
            String updated = patchTomlVersionRange(content, "minecraft", "[" + info.mcVersion() + ",)", changes);
            if (info.neoforgeVersion() != null) {
                updated = patchTomlVersionRange(updated, "neoforge", "[" + info.neoforgeVersion() + ",)", changes);
            }
            if (!updated.equals(content)) Files.writeString(file, updated);
        }
        return changes;
    }

    /** Replaces the literal versionRange of the dependency with {@code modId = depId}; placeholders are kept. */
    private String patchTomlVersionRange(String content, String depId, String newRange, List<String> changes) {
        Pattern p = Pattern.compile(
            "(modId\\s*=\\s*\"" + Pattern.quote(depId) + "\"[^\\[]*?versionRange\\s*=\\s*\")([^\"]+)(\")", Pattern.DOTALL);
        Matcher m = p.matcher(content);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String old = m.group(2);
            String value = old.contains("${") ? old : newRange;
            if (!value.equals(old)) changes.add("mods.toml " + depId + " versionRange: " + old + " → " + newRange);
            m.appendReplacement(out, Matcher.quoteReplacement(m.group(1) + value + m.group(3)));
        }
        m.appendTail(out);
        return out.toString();
    }

    // ── *.mixins.json ────────────────────────────────────────────────────────

    private List<String> patchMixinsJson(Path root, int java) throws IOException {
        List<String> changes = new ArrayList<>();
        Pattern level = Pattern.compile("(\"compatibilityLevel\"\\s*:\\s*\")JAVA_(\\d+)(\")");
        List<Path> files = new ArrayList<>();
        try (var walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().endsWith(".mixins.json"))
                .filter(p -> !ProjectPaths.isGenerated(root, p))
                .forEach(files::add);
        }
        for (Path file : files) {
            String content = Files.readString(file);
            Matcher m = level.matcher(content);
            if (!m.find() || m.group(2).equals(String.valueOf(java))) continue;
            String old = m.group(2);
            Files.writeString(file, m.replaceAll(r -> Matcher.quoteReplacement(r.group(1) + "JAVA_" + java + r.group(3))));
            changes.add(root.relativize(file) + ": compatibilityLevel JAVA_" + old + " → JAVA_" + java);
        }
        return changes;
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private List<Path> findFiles(Path root, String filename) throws IOException {
        List<Path> found = new ArrayList<>();
        if (!Files.exists(root)) return found;
        try (var walk = Files.walk(root)) {
            walk.filter(p -> p.getFileName().toString().equals(filename))
                .filter(p -> !ProjectPaths.isGenerated(root, p))
                .forEach(found::add);
        }
        return found;
    }
}
