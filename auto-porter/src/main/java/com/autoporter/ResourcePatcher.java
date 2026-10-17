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
        boolean includes26_2 = includesTransition(fromVersion, toVersion, "26.1.2", "26.2");
        boolean includes26_3 = includesTransition(fromVersion, toVersion, "26.2", "26.3");
        if (!includes26_2 && !includes26_3) {
            return PatchResult.success("No resource rules for " + fromVersion + " -> " + toVersion, List.of());
        }

        List<String> changes = new ArrayList<>();
        if (includes26_2) {
            if (includes26_3 && dryRun) {
                changes.addAll(updatePackMcmeta(modRoot, 84.0, 97.1, 101.1, 121.0, true));
            } else {
                changes.addAll(updatePackMcmeta(modRoot, 84.0, 88.0, 101.1, 107.1, dryRun));
            }
            changes.addAll(renameCoreShaders26_2(modRoot, dryRun));
        }
        if (includes26_3) {
            if (!includes26_2 || !dryRun) {
                changes.addAll(updatePackMcmeta(modRoot, 88.0, 97.1, 107.1, 121.0, dryRun));
            }
            changes.addAll(migrateConfiguredWorldgen(modRoot, "configured_feature", "feature", dryRun));
            changes.addAll(migrateConfiguredWorldgen(modRoot, "configured_carver", "carver", dryRun));
            changes.addAll(rewriteWorldgenJson26_3(modRoot, dryRun));
            changes.addAll(renameTagFiles26_3(modRoot, dryRun));
            changes.addAll(rewriteResourceIdentifiers26_3(modRoot, dryRun));
            changes.addAll(rewriteShaderIncludes26_3(modRoot, dryRun));
            changes.addAll(renameCoreShaders26_3(modRoot, dryRun));
        }
        return PatchResult.success("Resource/data files patched", changes);
    }

    private boolean includesTransition(String fromVersion, String toVersion, String stepFrom, String stepTo) {
        List<String> versions = VersionDatabase.allVersions();
        int fromIdx = versions.indexOf(VersionDatabase.normalize(fromVersion));
        int toIdx = versions.indexOf(VersionDatabase.normalize(toVersion));
        if (fromIdx < 0 || toIdx < 0 || fromIdx >= toIdx) return false;

        for (int i = fromIdx; i < toIdx; i++) {
            if (versions.get(i).equals(stepFrom) && versions.get(i + 1).equals(stepTo)) {
                return true;
            }
        }
        return false;
    }

    private List<String> updatePackMcmeta(Path root, double oldResource, double newResource,
                                          double oldData, double newData, boolean dryRun) throws IOException {
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
            Double replacement = replacementPackFormat(file, old, oldResource, newResource, oldData, newData);
            if (replacement == null) continue;

            if (replacement % 1 == 0) pack.addProperty("pack_format", replacement.intValue());
            else pack.addProperty("pack_format", replacement);

            changes.add(root.relativize(file) + ": pack_format " + old + " -> " + replacement);
            if (!dryRun) Files.writeString(file, GSON.toJson(obj));
        }
        return changes;
    }

    private Double replacementPackFormat(Path packMcmeta, double old, double oldResource,
                                         double newResource, double oldData, double newData) throws IOException {
        Path dir = packMcmeta.getParent();
        boolean hasAssets = hasChild(dir, "assets");
        boolean hasData = hasChild(dir, "data");
        if (Double.compare(old, oldResource) == 0 && hasAssets) return newResource;
        if (Double.compare(old, oldData) == 0 && hasData) return newData;
        return null;
    }

    private boolean hasChild(Path dir, String name) throws IOException {
        if (dir == null || !Files.isDirectory(dir)) return false;
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.anyMatch(path -> path.getFileName().toString().equals(name));
        }
    }

    private List<String> renameCoreShaders26_2(Path root, boolean dryRun) throws IOException {
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

    private List<String> migrateConfiguredWorldgen(Path root, String oldName, String newName,
                                                    boolean dryRun) throws IOException {
        List<String> changes = new ArrayList<>();
        for (Path oldDir : findDirectoriesEnding(root, "/worldgen/" + oldName)) {
            for (Path json : findFilesUnder(oldDir, ".json")) {
                String content = Files.readString(json);
                JsonObject object;
                try {
                    object = JsonParser.parseString(content).getAsJsonObject();
                } catch (Exception ignored) {
                    continue;
                }
                if (!object.has("config") || !object.get("config").isJsonObject()) continue;
                JsonObject config = object.getAsJsonObject("config");
                boolean collision = config.entrySet().stream()
                    .anyMatch(entry -> object.has(entry.getKey()) && !entry.getKey().equals("config"));
                if (collision) continue;

                JsonObject flattened = new JsonObject();
                if (object.has("type")) flattened.add("type", object.get("type").deepCopy());
                for (Map.Entry<String, JsonElement> entry : config.entrySet()) {
                    flattened.add(entry.getKey(), entry.getValue().deepCopy());
                }
                for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                    if (!entry.getKey().equals("type") && !entry.getKey().equals("config")) {
                        flattened.add(entry.getKey(), entry.getValue().deepCopy());
                    }
                }
                changes.add(root.relativize(json) + ": inlined config object for 26.3 worldgen schema");
                if (!dryRun) Files.writeString(json, GSON.toJson(flattened));
            }

            Path newDir = oldDir.resolveSibling(newName);
            changes.add(root.relativize(oldDir) + " -> " + root.relativize(newDir));
            if (!dryRun) moveDirectoryContents(oldDir, newDir);
        }
        return changes;
    }

    private List<String> rewriteWorldgenJson26_3(Path root, boolean dryRun) throws IOException {
        List<String> changes = new ArrayList<>();
        for (Path file : findJsonFiles(root)) {
            String content = Files.readString(file);
            JsonElement parsed;
            try {
                parsed = JsonParser.parseString(content);
            } catch (Exception ignored) {
                continue;
            }
            int changed = rewriteWorldgenElement(parsed);
            if (changed == 0) continue;
            changes.add(root.relativize(file) + ": updated " + changed + " exact 26.3 worldgen key/value(s)");
            if (!dryRun) Files.writeString(file, GSON.toJson(parsed));
        }
        return changes;
    }

    private int rewriteWorldgenElement(JsonElement element) {
        int changed = 0;
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) changed += rewriteWorldgenElement(child);
            return changed;
        }
        if (!element.isJsonObject()) return 0;

        JsonObject object = element.getAsJsonObject();
        if (object.has("type") && object.get("type").isJsonPrimitive()
                && "minecraft:basalt_columns".equals(object.get("type").getAsString())) {
            object.addProperty("type", "minecraft:stepped_column_cluster");
            changed++;
            if (object.has("reach") && !object.has("column_reach")) {
                object.add("column_reach", object.remove("reach"));
                changed++;
            }
        }
        for (Map.Entry<String, JsonElement> entry : new ArrayList<>(object.entrySet())) {
            changed += rewriteWorldgenElement(entry.getValue());
        }
        return changed;
    }

    private List<String> renameTagFiles26_3(Path root, boolean dryRun) throws IOException {
        Map<String, String> renames = new LinkedHashMap<>();
        renames.put("convertable_to_mud.json", "convertible_to_mud.json");
        renames.put("on_jungle_explorer_maps.json", "on_jungle_pyramid_maps.json");
        renames.put("on_ocean_explorer_maps.json", "on_ocean_monument_maps.json");
        renames.put("on_swamp_explorer_maps.json", "on_swamp_hut_maps.json");
        renames.put("on_trial_chambers_maps.json", "on_burial_trial_chambers_maps.json");
        renames.put("on_woodland_explorer_maps.json", "on_woodland_mansion_maps.json");

        List<String> changes = new ArrayList<>();
        for (Path file : findJsonFiles(root)) {
            String replacement = renames.get(file.getFileName().toString());
            if (replacement == null || !file.toString().replace('\\', '/').contains("/tags/")) continue;
            Path target = file.resolveSibling(replacement);
            if (Files.exists(target)) continue;
            changes.add(root.relativize(file) + " -> " + root.relativize(target));
            if (!dryRun) Files.move(file, target);
        }
        return changes;
    }

    private List<String> rewriteResourceIdentifiers26_3(Path root, boolean dryRun) throws IOException {
        Map<String, String> replacements = new LinkedHashMap<>();
        replacements.put("minecraft:convertable_to_mud", "minecraft:convertible_to_mud");
        replacements.put("minecraft:on_jungle_explorer_maps", "minecraft:on_jungle_pyramid_maps");
        replacements.put("minecraft:on_ocean_explorer_maps", "minecraft:on_ocean_monument_maps");
        replacements.put("minecraft:on_swamp_explorer_maps", "minecraft:on_swamp_hut_maps");
        replacements.put("minecraft:on_trial_chambers_maps", "minecraft:on_burial_trial_chambers_maps");
        replacements.put("minecraft:on_woodland_explorer_maps", "minecraft:on_woodland_mansion_maps");
        replacements.put("minecraft:worldgen/configured_feature", "minecraft:worldgen/feature");
        replacements.put("minecraft:worldgen/configured_carver", "minecraft:worldgen/carver");
        replacements.put("minecraft:rendertype_clouds", "minecraft:clouds");
        replacements.put("minecraft:rendertype_world_border", "minecraft:world_border");

        List<String> changes = new ArrayList<>();
        for (Path file : findResourceTextFiles(root)) {
            String content = Files.readString(file);
            String updated = content;
            for (Map.Entry<String, String> replacement : replacements.entrySet()) {
                updated = updated.replace(replacement.getKey(), replacement.getValue());
            }
            if (updated.equals(content)) continue;
            changes.add(root.relativize(file) + ": updated exact 26.3 resource identifiers");
            if (!dryRun) Files.writeString(file, updated);
        }
        return changes;
    }

    private List<String> rewriteShaderIncludes26_3(Path root, boolean dryRun) throws IOException {
        List<String> changes = new ArrayList<>();
        for (Path file : findFilesWithSuffixes(root, ".vsh", ".fsh", ".glsl")) {
            String content = Files.readString(file);
            String updated = content.replace("#moj_import", "#include");
            if (updated.equals(content)) continue;
            changes.add(root.relativize(file) + ": #moj_import -> #include");
            if (!dryRun) Files.writeString(file, updated);
        }
        return changes;
    }

    private List<String> renameCoreShaders26_3(Path root, boolean dryRun) throws IOException {
        List<String> changes = new ArrayList<>();
        for (Path dir : findCoreShaderDirs(root)) {
            for (String extension : List.of(".json", ".vsh", ".fsh")) {
                changes.addAll(renameIfPresent(root, dir.resolve("rendertype_clouds" + extension),
                    dir.resolve("clouds" + extension), dryRun));
                changes.addAll(renameIfPresent(root, dir.resolve("rendertype_world_border" + extension),
                    dir.resolve("world_border" + extension), dryRun));
            }
        }
        return changes;
    }

    private List<String> renameIfPresent(Path root, Path oldPath, Path newPath, boolean dryRun) throws IOException {
        if (!Files.isRegularFile(oldPath) || Files.exists(newPath)) return List.of();
        if (!dryRun) Files.move(oldPath, newPath);
        return List.of(root.relativize(oldPath) + " -> " + root.relativize(newPath));
    }

    private void moveDirectoryContents(Path source, Path target) throws IOException {
        Files.createDirectories(target);
        try (Stream<Path> walk = Files.walk(source)) {
            for (Path path : walk.sorted().toList()) {
                if (!Files.isRegularFile(path)) continue;
                Path destination = target.resolve(source.relativize(path));
                if (Files.exists(destination)) continue;
                Files.createDirectories(destination.getParent());
                Files.move(path, destination);
            }
        }
        try (Stream<Path> walk = Files.walk(source)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
                try { Files.deleteIfExists(path); } catch (DirectoryNotEmptyException ignored) {}
            }
        }
    }

    private List<Path> findDirectoriesEnding(Path root, String normalizedSuffix) throws IOException {
        List<Path> found = new ArrayList<>();
        if (!Files.exists(root)) return found;
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isDirectory)
                .filter(path -> path.toString().replace('\\', '/').endsWith(normalizedSuffix))
                .filter(path -> !isGeneratedPath(path))
                .forEach(found::add);
        }
        return found;
    }

    private List<Path> findFilesUnder(Path root, String suffix) throws IOException {
        if (!Files.exists(root)) return List.of();
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(suffix))
                .toList();
        }
    }

    private List<Path> findJsonFiles(Path root) throws IOException {
        return findFilesWithSuffixes(root, ".json");
    }

    private List<Path> findResourceTextFiles(Path root) throws IOException {
        return findFilesWithSuffixes(root, ".json", ".mcmeta", ".vsh", ".fsh", ".glsl");
    }

    private List<Path> findFilesWithSuffixes(Path root, String... suffixes) throws IOException {
        List<Path> found = new ArrayList<>();
        if (!Files.exists(root)) return found;
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile)
                .filter(path -> !isGeneratedPath(path))
                .filter(path -> Arrays.stream(suffixes).anyMatch(path.getFileName().toString()::endsWith))
                .forEach(found::add);
        }
        return found;
    }

    private boolean isGeneratedPath(Path path) {
        String normalized = path.toString().replace('\\', '/');
        return normalized.contains("/build/") || normalized.contains("/.gradle/");
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
