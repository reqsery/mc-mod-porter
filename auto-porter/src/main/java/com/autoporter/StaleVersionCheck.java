package com.autoporter;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Post-patch check: after the build files were updated, make sure {@code build.gradle(.kts)},
 * {@code gradle.properties} and {@code gradle-wrapper.properties} no longer contain the source project's version
 * numbers (Minecraft, loader, NeoForge, ModDevGradle, Loom, Gradle…). Anything left over is reported before Gradle
 * runs, so a hard-coded version the porter could not handle is easy to find.
 */
public final class StaleVersionCheck {
    private static final Pattern PROPERTY = Pattern.compile(
        "(?m)^\\s*((?:minecraft|neo|neoforge|moddev|mdg|loom|loader|fabric_loader|fabric|fabric_api|yarn|forge|"
        + "architectury_api)_version|yarn_mappings)\\s*=\\s*([^\\s#]+)");
    private static final List<Pattern> SCRIPT_VERSIONS = List.of(
        Pattern.compile("id\\s*\\(?\\s*['\"](?:fabric-loom|net\\.fabricmc\\.fabric-loom(?:-remap)?|net\\.neoforged\\.moddev)['\"]\\s*\\)?\\s*version\\s*\\(?\\s*['\"]([^'\"$]+)['\"]"),
        Pattern.compile("neoForge\\.version\\s*=\\s*['\"]([^'\"$]+)['\"]"),
        Pattern.compile("(?s)neoForge\\s*\\{[^}]*?\\bversion\\s*=\\s*['\"]([^'\"$]+)['\"]"),
        Pattern.compile("(?:net\\.fabricmc:fabric-loader|net\\.fabricmc\\.fabric-api:fabric-api|net\\.neoforged:neoforge|com\\.mojang:minecraft):([^'\"$\\s)]+)"));
    private static final Pattern WRAPPER = Pattern.compile("gradle-([\\d.]+(?:-rc-\\d+)?)-(?:bin|all)\\.zip");

    private final Map<String, String> oldValues = new LinkedHashMap<>(); // value → where it came from

    private StaleVersionCheck() {}

    /** Records the version numbers in the project before patching. */
    public static StaleVersionCheck snapshot(Path root, String fromVersion) throws IOException {
        StaleVersionCheck check = new StaleVersionCheck();
        check.oldValues.put(fromVersion, "source Minecraft version");
        for (Path props : propertiesFiles(root)) {
            Matcher m = PROPERTY.matcher(Files.readString(props));
            while (m.find()) check.oldValues.putIfAbsent(m.group(2), m.group(1));
        }
        for (Path script : BuildFilePatcher.findBuildFiles(root)) {
            String text = Files.readString(script);
            for (Pattern p : SCRIPT_VERSIONS) {
                Matcher m = p.matcher(text);
                while (m.find()) check.oldValues.putIfAbsent(m.group(1), script.getFileName() + " version");
            }
        }
        Path wrapper = root.resolve("gradle/wrapper/gradle-wrapper.properties");
        if (Files.isRegularFile(wrapper)) {
            Matcher m = WRAPPER.matcher(Files.readString(wrapper));
            if (m.find()) check.oldValues.putIfAbsent(m.group(1), "Gradle wrapper version");
        }
        return check;
    }

    /** Lines that still contain an old version number after patching. */
    public PatchResult verify(Path root, String targetVersion) throws IOException {
        Set<String> current = currentTargetValues(targetVersion);
        Map<String, String> stale = new LinkedHashMap<>();
        oldValues.forEach((value, source) -> {
            if (!current.contains(value) && value.matches(".*\\d.*") && value.length() >= 3) stale.put(value, source);
        });

        List<Path> files = new ArrayList<>(BuildFilePatcher.findBuildFiles(root));
        files.addAll(propertiesFiles(root));
        Path wrapper = root.resolve("gradle/wrapper/gradle-wrapper.properties");
        if (Files.isRegularFile(wrapper)) files.add(wrapper);

        List<String> findings = new ArrayList<>();
        for (Path file : files) {
            List<String> lines = Files.readAllLines(file);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.startsWith("#") || line.startsWith("//")) continue;
                for (var e : stale.entrySet()) {
                    if (Pattern.compile("(?<![\\w.+])" + Pattern.quote(e.getKey()) + "(?![\\w+]|\\.\\d)").matcher(line).find()) {
                        findings.add(root.relativize(file).toString().replace('\\', '/') + ":" + (i + 1)
                            + ": still contains " + e.getKey() + " (" + e.getValue() + ") — " + line);
                    }
                }
            }
        }
        return PatchResult.success(findings.isEmpty()
            ? "No stale version numbers left in build files"
            : "WARNING: " + findings.size() + " stale version number(s) left in build files — review before building",
            findings);
    }

    private static Set<String> currentTargetValues(String targetVersion) {
        Set<String> values = new HashSet<>();
        VersionDatabase.VersionInfo info = VersionDatabase.get(targetVersion);
        if (info != null) {
            Collections.addAll(values, info.mcVersion(), info.fabricLoaderVersion(), info.fabricApiVersion(),
                info.neoforgeVersion(), info.forgeVersion(), info.architecturyVersion(), info.loomVersion(), info.yarnMappings());
        }
        BuildTooling.Tooling tooling = BuildTooling.get(targetVersion);
        if (tooling != null) Collections.addAll(values, tooling.fabricGradle(), tooling.neoForgeGradle(), tooling.modDevGradle());
        values.remove(null);
        return values;
    }

    private static List<Path> propertiesFiles(Path root) {
        List<Path> found = new ArrayList<>();
        for (String rel : List.of("gradle.properties", "common/gradle.properties", "fabric/gradle.properties",
                "neoforge/gradle.properties")) {
            Path p = root.resolve(rel);
            if (Files.isRegularFile(p)) found.add(p);
        }
        return found;
    }
}
