package com.autoporter;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Function;
import java.util.regex.*;
import java.util.stream.Stream;

/**
 * Patches {@code build.gradle} / {@code build.gradle.kts} and the Gradle wrapper for the target Minecraft version.
 *
 * <p>Handles both Groovy and Kotlin DSL, and both property-driven versions (left to
 * {@link GradlePropertiesPatcher}) and versions hard-coded in the build script:
 * <ul>
 *   <li>Java toolchain / source-target compatibility / {@code options.release} → the target's Java version</li>
 *   <li>{@code com.mojang:minecraft:<version>} → target version</li>
 *   <li>Fabric: Loom plugin ID and hard-coded plugin version, fabric-loader / fabric-api literals, Yarn literals,
 *       and for 26.x: no mappings line, {@code mod*} configurations → plain ones, {@code remapJar} → {@code jar}</li>
 *   <li>NeoForge: {@code net.neoforged.moddev} plugin version, {@code neoForge { version = "…" }},
 *       {@code net.neoforged:neoforge:<version>} (NeoGradle), and Parchment blocks on 26.x</li>
 *   <li>Gradle wrapper: the version used by the official Fabric example mod / NeoForge MDK for the target</li>
 * </ul>
 */
public class BuildFilePatcher {

    private static final String LOOM_IDS = "fabric-loom|net\\.fabricmc\\.fabric-loom|net\\.fabricmc\\.fabric-loom-remap";
    private static final List<String> MOD_CONFIGURATIONS = List.of(
        "modImplementation", "modCompileOnlyApi", "modCompileOnly", "modApi", "modRuntimeOnly", "modLocalRuntime");

    public PatchResult patch(Path modRoot, String targetVersion) throws IOException {
        return patch(modRoot, null, targetVersion, false, false);
    }

    public PatchResult patch(Path modRoot, String targetVersion, boolean useYarn) throws IOException {
        return patch(modRoot, null, targetVersion, useYarn, false);
    }

    /**
     * @param useYarn          backport from 26.x: re-add Yarn instead of Mojang mappings
     * @param sourcesRemapped  the Java sources were remapped Yarn → Mojang, so the project must use Mojang mappings
     */
    public PatchResult patch(Path modRoot, String fromVersion, String targetVersion, boolean useYarn,
                             boolean sourcesRemapped) throws IOException {
        VersionDatabase.VersionInfo info = VersionDatabase.get(targetVersion);
        if (info == null) return PatchResult.failure("Unknown version: " + targetVersion);
        BuildTooling.Tooling tooling = BuildTooling.get(targetVersion);
        int java = BuildTooling.javaFor(targetVersion);
        boolean is26x = VersionDatabase.isUnobfuscated(targetVersion);

        List<Path> buildFiles = findBuildFiles(modRoot);
        List<String> changes = new ArrayList<>();
        boolean fabricProject = false, neoForgeProject = false, otherToolchain = false;

        for (Path file : buildFiles) {
            String original = Files.readString(file);
            boolean fabric = original.contains("fabric-loom");
            boolean neo = original.contains("net.neoforged.moddev") || original.contains("net.neoforged.gradle")
                || original.contains("net.neoforged:neoforge:");
            fabricProject |= fabric;
            neoForgeProject |= neo;
            otherToolchain |= original.contains("dev.architectury.loom") || original.contains("net.minecraftforge.gradle");

            Edit edit = new Edit(file, original);
            patchJavaVersion(edit, java);
            edit.replace("(com\\.mojang:minecraft:)([^'\"$\\s)]+)", m -> m.group(1) + info.mcVersion(),
                "set the Minecraft dependency to " + info.mcVersion());
            if (fabric) patchFabric(edit, info, is26x, useYarn, sourcesRemapped);
            if (neo) patchNeoForge(edit, info, tooling, is26x);
            if (!edit.content.equals(original)) Files.writeString(file, edit.content);
            changes.addAll(edit.log);
        }

        changes.addAll(patchWrapperFor(modRoot, tooling, fabricProject, neoForgeProject, otherToolchain));
        if (changes.isEmpty()) {
            changes.add("Scanned " + buildFiles.size() + " build file(s) — no changes needed");
        }
        return PatchResult.success("Build files patched", changes);
    }

    // ── Java toolchain ─────────────────────────────────────────────────────────────────────────

    static void patchJavaVersion(Edit edit, int java) {
        String javaVersionConstant = java <= 8 ? "1_" + java : String.valueOf(java);
        edit.replace("(JavaLanguageVersion\\.of\\(\\s*)(\\d+)(\\s*\\))", m -> m.group(1) + java + m.group(3),
            "set the Java toolchain to " + java);
        edit.replace("(JavaVersion\\.VERSION_)(1_\\d+|\\d+)\\b", m -> m.group(1) + javaVersionConstant,
            "set JavaVersion constants to VERSION_" + javaVersionConstant);
        edit.replace("(\\brelease\\s*=\\s*)(\\d+)\\b", m -> m.group(1) + java, "set options.release to " + java);
        edit.replace("(\\brelease\\.set\\(\\s*)(\\d+)(\\s*\\))", m -> m.group(1) + java + m.group(3),
            "set options.release to " + java);
        edit.replace("((?:source|target)Compatibility\\s*=\\s*)(['\"]?)(\\d+(?:\\.\\d+)?)(['\"]?)",
            m -> m.group(1) + m.group(2) + (java <= 8 ? "1." + java : String.valueOf(java)) + m.group(4),
            "set source/target compatibility to " + java);
    }

    // ── Fabric ─────────────────────────────────────────────────────────────────────────────────

    private void patchFabric(Edit edit, VersionDatabase.VersionInfo info, boolean is26x, boolean useYarn,
                             boolean sourcesRemapped) {
        // Plugin ID: 26.x uses the non-remapping plugin, older versions the remapping one.
        if (is26x) {
            edit.replace("(id\\s*\\(?\\s*['\"])(fabric-loom|net\\.fabricmc\\.fabric-loom-remap)(['\"])",
                m -> m.group(1) + "net.fabricmc.fabric-loom" + m.group(3),
                "renamed the Loom plugin id to 'net.fabricmc.fabric-loom' (26.x is unobfuscated)");
        } else {
            edit.replace("(id\\s*\\(?\\s*['\"])(net\\.fabricmc\\.fabric-loom)(['\"])",
                m -> m.group(1) + "net.fabricmc.fabric-loom-remap" + m.group(3),
                "renamed the Loom plugin id to 'net.fabricmc.fabric-loom-remap' (pre-26.x needs remapping)");
        }
        // Hard-coded Loom plugin version.
        edit.replace("(id\\s*\\(?\\s*['\"](?:" + LOOM_IDS + ")['\"]\\s*\\)?\\s*version\\s*\\(?\\s*['\"])([^'\"$]+)(['\"])",
            m -> m.group(1) + info.loomVersion() + m.group(3), "set the Loom plugin version to " + info.loomVersion());
        // Hard-coded dependency versions.
        edit.replace("(net\\.fabricmc:fabric-loader:)([^'\"$\\s)]+)", m -> m.group(1) + info.fabricLoaderVersion(),
            "set fabric-loader to " + info.fabricLoaderVersion());
        edit.replace("(net\\.fabricmc\\.fabric-api:fabric-api:)([^'\"$\\s)]+)", m -> m.group(1) + info.fabricApiVersion(),
            "set fabric-api to " + info.fabricApiVersion());

        if (is26x) {
            removeMappingsLines(edit, "26.x is unobfuscated");
            for (String config : MOD_CONFIGURATIONS) {
                String plain = Character.toLowerCase(config.charAt(3)) + config.substring(4);
                edit.replace("\\b" + config + "\\b", m -> plain, "changed " + config + " → " + plain + " (no remapping in 26.x)");
            }
            edit.replace("\\bremapSourcesJar\\b", m -> "sourcesJar", "changed remapSourcesJar → sourcesJar (no remapping in 26.x)");
            edit.replace("\\bremapJar\\b", m -> "jar", "changed remapJar → jar (no remapping in 26.x)");
            return;
        }

        // Pre-26.x target.
        if (sourcesRemapped) {
            edit.replace("(?m)^([ \\t]*)mappings\\s*\\(?\\s*['\"]net\\.fabricmc:yarn:[^'\"]+['\"]\\s*\\)?[ \\t]*(\\r?\\n)",
                m -> m.group(1) + (edit.kotlin ? "mappings(loom.officialMojangMappings())" : "mappings loom.officialMojangMappings()") + m.group(2),
                "switched Yarn mappings to loom.officialMojangMappings() (sources were remapped to Mojang names)");
        } else if (info.yarnMappings() != null) {
            edit.replace("(net\\.fabricmc:yarn:)([^:'\"$]+)(:v2)?", m -> m.group(1) + info.yarnMappings() + (m.group(3) == null ? "" : m.group(3)),
                "set Yarn mappings to " + info.yarnMappings());
        }
        if (!Pattern.compile("(?m)^\\s*mappings\\b").matcher(edit.content).find()
                && edit.content.contains("com.mojang:minecraft:")) {
            String line;
            String desc;
            if (useYarn && info.yarnMappings() != null && !sourcesRemapped) {
                line = edit.kotlin ? "mappings(\"net.fabricmc:yarn:" + info.yarnMappings() + ":v2\")"
                                   : "mappings \"net.fabricmc:yarn:" + info.yarnMappings() + ":v2\"";
                desc = "added Yarn mappings line: " + info.yarnMappings();
            } else {
                line = edit.kotlin ? "mappings(loom.officialMojangMappings())" : "mappings loom.officialMojangMappings()";
                desc = "added mappings loom.officialMojangMappings() (Mojang official)";
            }
            edit.replaceFirst("(?m)^([ \\t]*)(minecraft\\s*\\(?\\s*['\"]com\\.mojang:minecraft:[^'\"]+['\"]\\s*\\)?)[ \\t]*(\\r?\\n)",
                m -> m.group(1) + m.group(2) + m.group(3) + m.group(1) + line + m.group(3), desc);
        }
        // implementation → modImplementation for loader and API (reverse of the 26.x change).
        edit.replace("(?<![\\w])implementation(\\s*\\(?\\s*['\"]net\\.fabricmc(?::fabric-loader|\\.fabric-api:fabric-api):)",
            m -> "modImplementation" + m.group(1), "changed implementation → modImplementation for fabric-loader / fabric-api");
    }

    private void removeMappingsLines(Edit edit, String reason) {
        edit.replace("(?m)^[ \\t]*mappings\\s*=?\\s*\\(?\\s*loom\\.officialMojangMappings\\(\\)\\s*\\)?[ \\t]*\\r?\\n", m -> "",
            "removed the loom.officialMojangMappings() line (" + reason + ")");
        edit.replace("(?m)^[ \\t]*mappings\\s*\\(?\\s*['\"]net\\.fabricmc:yarn:[^'\"]+['\"]\\s*\\)?[ \\t]*\\r?\\n", m -> "",
            "removed the Yarn mappings line (" + reason + ")");
        edit.replace("(?m)^[ \\t]*mappings\\s*\\(?\\s*loom\\.layered\\s*(?:\\(\\))?\\s*\\{[^}]*\\}\\s*\\)?[ \\t]*\\r?\\n", m -> "",
            "removed the layered mappings block (" + reason + ")");
    }

    // ── NeoForge ───────────────────────────────────────────────────────────────────────────────

    private void patchNeoForge(Edit edit, VersionDatabase.VersionInfo info, BuildTooling.Tooling tooling, boolean is26x) {
        if (tooling != null && tooling.modDevGradle() != null) {
            edit.replace("(id\\s*\\(?\\s*['\"]net\\.neoforged\\.moddev['\"]\\s*\\)?\\s*version\\s*\\(?\\s*['\"])([^'\"$]+)(['\"])",
                m -> m.group(1) + tooling.modDevGradle() + m.group(3),
                "set the ModDevGradle plugin (net.neoforged.moddev) to " + tooling.modDevGradle());
        }
        if (info.neoforgeVersion() != null) {
            String neo = info.neoforgeVersion();
            edit.replace("(neoForge\\.version\\s*=\\s*['\"])([^'\"$]+)(['\"])", m -> m.group(1) + neo + m.group(3),
                "set neoForge.version to " + neo);
            edit.replaceInBlocks("neoForge", "(?<![\\w.])(version\\s*=\\s*['\"])([^'\"$]+)(['\"])",
                m -> m.group(1) + neo + m.group(3), "set neoForge { version } to " + neo);
            edit.replace("(net\\.neoforged:neoforge:)([^'\"$\\s)]+)", m -> m.group(1) + neo,
                "set the NeoForge dependency to " + neo);
        }
        if (is26x) {
            edit.commentOutBlocks("parchment",
                "// Removed by MC Mod Porter: Parchment mappings are not used on unobfuscated 26.x — ",
                "commented out the parchment { } block (26.x has official parameter names)");
        }
    }

    // ── Gradle wrapper ─────────────────────────────────────────────────────────────────────────

    private List<String> patchWrapperFor(Path root, BuildTooling.Tooling tooling, boolean fabric, boolean neo,
                                         boolean otherToolchain) throws IOException {
        if (tooling == null) return List.of();
        if (otherToolchain) {
            return List.of("Gradle wrapper left unchanged: the project uses Architectury Loom or ForgeGradle — check "
                + "that plugin's documentation for the Gradle version it needs");
        }
        if (fabric && neo) {
            return List.of("Mixed Fabric/NeoForge project: Gradle wrapper left unchanged — official templates use Gradle "
                + tooling.fabricGradle() + " (Fabric) and " + tooling.neoForgeGradle() + " (NeoForge)");
        }
        String version = fabric ? tooling.fabricGradle() : neo ? tooling.neoForgeGradle() : null;
        if (version == null) return List.of();
        return patchGradleWrapper(root, version, fabric ? "official Fabric example mod" : "official NeoForge MDK");
    }

    static List<String> patchGradleWrapper(Path root, String version, String source) throws IOException {
        Path wrapper = root.resolve("gradle/wrapper/gradle-wrapper.properties");
        if (!Files.isRegularFile(wrapper)) return List.of();
        String content = Files.readString(wrapper);
        Matcher current = Pattern.compile("gradle-([\\d.]+(?:-rc-\\d+)?)-(?:bin|all)\\.zip").matcher(content);
        if (current.find() && current.group(1).equals(version)) return List.of();
        String updated = Pattern.compile("(gradle-)\\d+(?:\\.\\d+){1,2}(?:-rc-\\d+)?(-(?:bin|all)\\.zip)")
            .matcher(content).replaceAll(match -> match.group(1) + version + match.group(2));
        // A checksum for the old distribution would make the new download fail verification.
        updated = updated.replaceAll("(?m)^distributionSha256Sum=.*\\r?\\n?", "");
        if (updated.equals(content)) return List.of();
        Files.writeString(wrapper, updated);
        return List.of(wrapper + ": updated the Gradle wrapper to " + version + " (" + source + ")");
    }

    // ── Helpers ────────────────────────────────────────────────────────────────────────────────

    static List<Path> findBuildFiles(Path root) throws IOException {
        List<Path> files = new ArrayList<>();
        if (!Files.exists(root)) return files;
        // Only walk depth 2: root + direct submodules (fabric/, neoforge/, common/).
        try (Stream<Path> walk = Files.walk(root, 2)) {
            walk.filter(p -> {
                String name = p.getFileName().toString();
                if (!name.equals("build.gradle") && !name.equals("build.gradle.kts")) return false;
                Path dir = p.getParent();
                if (dir.equals(root)) return true;
                // A subdirectory with its own settings.gradle is a separate project, not a submodule.
                return !Files.exists(dir.resolve("settings.gradle")) && !Files.exists(dir.resolve("settings.gradle.kts"));
            }).sorted().forEach(files::add);
        }
        return files;
    }

    /** A build script being edited, with a change log. */
    static final class Edit {
        final Path file;
        final boolean kotlin;
        String content;
        final List<String> log = new ArrayList<>();

        Edit(Path file, String content) {
            this.file = file;
            this.kotlin = file.getFileName().toString().endsWith(".kts");
            this.content = content;
        }

        void replace(String regex, Function<MatchResult, String> replacement, String description) {
            apply(Pattern.compile(regex).matcher(content).replaceAll(m -> Matcher.quoteReplacement(replacement.apply(m))),
                description);
        }

        void replaceFirst(String regex, Function<MatchResult, String> replacement, String description) {
            apply(Pattern.compile(regex).matcher(content).replaceFirst(m -> Matcher.quoteReplacement(replacement.apply(m))),
                description);
        }

        /** Applies a regex only inside {@code name { … }} blocks (brace-matched). */
        void replaceInBlocks(String name, String regex, Function<MatchResult, String> replacement, String description) {
            StringBuilder out = new StringBuilder();
            int from = 0;
            for (int[] block : blocks(name)) {
                out.append(content, from, block[0]);
                String body = content.substring(block[0], block[1]);
                out.append(Pattern.compile(regex).matcher(body)
                    .replaceAll(m -> Matcher.quoteReplacement(replacement.apply(m))));
                from = block[1];
            }
            out.append(content.substring(from));
            apply(out.toString(), description);
        }

        /** Prefixes every line of each {@code name { … }} block with a comment. */
        void commentOutBlocks(String name, String prefix, String description) {
            StringBuilder out = new StringBuilder();
            int from = 0;
            for (int[] block : blocks(name)) {
                int lineStart = content.lastIndexOf('\n', block[0]) + 1;
                int end = content.indexOf('\n', block[1]);
                end = end < 0 ? content.length() : end;
                // Only blocks that sit on their own lines can be commented out safely.
                if (!content.substring(lineStart, block[0]).isBlank() || !content.substring(block[1], end).isBlank()) {
                    log.add(file + ": MANUAL — remove the " + name + " { } block by hand (it shares a line with other code)");
                    continue;
                }
                out.append(content, from, lineStart);
                String[] lines = content.substring(lineStart, end).split("\n", -1);
                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i];
                    String indent = line.substring(0, line.length() - line.stripLeading().length());
                    out.append(indent).append(i == 0 ? prefix : "// ").append(line.stripLeading());
                    if (i < lines.length - 1) out.append('\n');
                }
                from = end;
            }
            out.append(content.substring(from));
            apply(out.toString(), description);
        }

        /** [start, endExclusive] ranges of {@code name { … }} blocks, start at the name. */
        private List<int[]> blocks(String name) {
            List<int[]> found = new ArrayList<>();
            Matcher m = Pattern.compile("(?<![\\w.])" + Pattern.quote(name) + "\\s*\\{").matcher(content);
            while (m.find()) {
                int depth = 0;
                for (int i = m.end() - 1; i < content.length(); i++) {
                    char c = content.charAt(i);
                    if (c == '{') depth++;
                    else if (c == '}' && --depth == 0) {
                        found.add(new int[] { m.start(), i + 1 });
                        break;
                    }
                }
            }
            // Drop nested matches so a block is only handled once.
            List<int[]> outer = new ArrayList<>();
            for (int[] b : found) {
                if (outer.isEmpty() || b[0] >= outer.get(outer.size() - 1)[1]) outer.add(b);
            }
            return outer;
        }

        private void apply(String updated, String description) {
            if (!updated.equals(content)) {
                content = updated;
                log.add(file + ": " + description);
            }
        }
    }
}
