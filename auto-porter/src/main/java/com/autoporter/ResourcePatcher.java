package com.autoporter;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/** Applies deterministic non-Java resource/data migrations. */
public class ResourcePatcher {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public PatchResult patch(Path modRoot, String fromVersion, String toVersion, boolean dryRun) throws IOException {
        if (!fromVersion.equals("26.1.2") || !toVersion.equals("26.2")) {
            return PatchResult.success("No resource rules for " + fromVersion + " -> " + toVersion, List.of());
        }

        List<String> changes = new ArrayList<>();
        changes.addAll(updatePackMcmeta(modRoot, dryRun));
        changes.addAll(renameCoreShaders(modRoot, dryRun));
        return PatchResult.success("Resource/data files patched", changes);
    }

    private List<String> updatePackMcmeta(Path root, boolean dryRun) throws IOException {
        List<String> changes = new ArrayList<>();
        for (Path file : findFiles(root, "pack.mcmeta")) {
            String content = Files.readString(file);
            JsonObject obj;
            try {
                obj = JsonParser.parseString(content).getAsJsonObject();
            } catch (Exception ignored) {
                continue;
            }
            if (!obj.has("pack") || !obj.get("pack").isJsonObject()) continue;
            JsonObject pack = obj.getAsJsonObject("pack");
            if (!pack.has("pack_format") || !pack.get("pack_format").isJsonPrimitive()) continue;

            JsonPrimitive primitive = pack.getAsJsonPrimitive("pack_format");
            if (!primitive.isNumber()) continue;

            double old = primitive.getAsDouble();
            Double replacement = replacementPackFormat(file, old);
            if (replacement == null) continue;

            if (replacement % 1 == 0) pack.addProperty("pack_format", replacement.intValue());
            else pack.addProperty("pack_format", replacement);

            changes.add(root.relativize(file) + ": pack_format " + old + " -> " + replacement);
            if (!dryRun) Files.writeString(file, GSON.toJson(obj));
        }
        return changes;
    }

    private Double replacementPackFormat(Path packMcmeta, double old) throws IOException {
        Path dir = packMcmeta.getParent();
        boolean hasAssets = hasChild(dir, "assets");
        boolean hasData = hasChild(dir, "data");
        if (old == 84.0 && hasAssets && !hasData) return 88.0;
        if (old == 101.1 && hasData && !hasAssets) return 107.1;
        return null;
    }

    private boolean hasChild(Path dir, String name) throws IOException {
        if (dir == null || !Files.isDirectory(dir)) return false;
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.anyMatch(path -> path.getFileName().toString().equals(name));
        }
    }

    private List<String> renameCoreShaders(Path root, boolean dryRun) throws IOException {
        List<String> changes = new ArrayList<>();
        Map<String, String> renames = Map.of(
            "rendertype_text.json", "text.json",
            "rendertype_text_background.json", "text_background.json"
        );
        for (Path dir : findCoreShaderDirs(root)) {
            for (Map.Entry<String, String> entry : renames.entrySet()) {
                Path oldPath = dir.resolve(entry.getKey());
                Path newPath = dir.resolve(entry.getValue());
                if (!Files.isRegularFile(oldPath) || Files.exists(newPath)) continue;
                changes.add(root.relativize(oldPath) + " -> " + root.relativize(newPath));
                if (!dryRun) Files.move(oldPath, newPath);
            }
        }
        return changes;
    }

    private List<Path> findCoreShaderDirs(Path root) throws IOException {
        List<Path> dirs = new ArrayList<>();
        if (!Files.exists(root)) return dirs;
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isDirectory)
                .filter(path -> path.toString().replace("\\", "/").endsWith("/assets/minecraft/shaders/core"))
                .forEach(dirs::add);
        }
        return dirs;
    }

    private List<Path> findFiles(Path root, String filename) throws IOException {
        List<Path> found = new ArrayList<>();
        if (!Files.exists(root)) return found;
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().equals(filename))
                .filter(path -> !path.toString().contains("build") && !path.toString().contains(".gradle"))
                .forEach(found::add);
        }
        return found;
    }
}
