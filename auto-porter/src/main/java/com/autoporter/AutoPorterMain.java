package com.autoporter;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.*;
import java.nio.file.StandardCopyOption;

/**
 * Auto-Porter — ports a Minecraft mod between versions.
 *
 * Usage:
 *   java -jar auto-porter.jar <modPath> <fromVersion> <toVersion> [--dry-run] [--no-build]
 *   java -jar auto-porter.jar --setup-templates        (generate all gradle.properties templates)
 *   java -jar auto-porter.jar --list-versions          (show all supported versions)
 *
 * Examples:
 *   java -jar auto-porter.jar C:/mods/mymod 1.21.4 1.21.10
 *   java -jar auto-porter.jar C:/mods/mymod 1.21.4 1.21.10 --dry-run
 */
public class AutoPorterMain {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════╗");
        System.out.println("║      MC Mod Auto-Porter v1.0     ║");
        System.out.println("╚══════════════════════════════════╝\n");

        if (args.length == 0 || args[0].equals("--help")) {
            printHelp();
            return;
        }

        if (args[0].equals("--list-versions")) {
            listVersions();
            return;
        }

        if (args[0].equals("--setup-templates")) {
            setupTemplates();
            return;
        }

        if (args.length < 3) {
            System.err.println("Usage: auto-porter <modPath> <fromVersion> <toVersion> [--dry-run] [--no-build]");
            System.exit(1);
        }

        String modPath = args[0];
        String fromVer = args[1];
        String toVer   = args[2];

        List<String> flags = Arrays.asList(args).subList(3, args.length);
        boolean dryRun     = flags.contains("--dry-run");
        boolean buildAfter = !flags.contains("--no-build") && !dryRun;

        portMod(modPath, fromVer, toVer, buildAfter, dryRun);
    }

    // ── Port a mod ────────────────────────────────────────────────────────────

    static void portMod(String modPath, String fromVer, String toVer, boolean buildAfter, boolean dryRun) throws Exception {
        Path srcRoot = Path.of(modPath);

        if (dryRun) System.out.println("*** DRY RUN — no files will be modified ***\n");

        System.out.println("Source mod : " + srcRoot.getFileName());
        System.out.println("From       : " + fromVer);
        System.out.println("To         : " + toVer);
        System.out.println();

        if (!Files.exists(srcRoot)) {
            System.err.println("ERROR: Mod path does not exist: " + modPath);
            System.exit(1);
        }
        if (!VersionDatabase.supports(fromVer)) {
            System.err.println("ERROR: Unknown source version: " + fromVer);
            System.err.println("Supported: " + VersionDatabase.allVersions());
            System.exit(1);
        }
        if (!VersionDatabase.supports(toVer)) {
            System.err.println("ERROR: Unknown target version: " + toVer);
            System.exit(1);
        }

        // In dry-run mode, scan the original source — no copy, no writes
        if (dryRun) {
            System.out.println("[DRY RUN] Scanning sources for changes...\n");
            SourcePatcher sourcePatcher = new SourcePatcher();
            PatchResult sourceResult = sourcePatcher.patch(srcRoot, fromVer, toVer, true);
            System.out.println();
            sourceResult.print();
            System.out.println("\n[DRY RUN] build.gradle and gradle.properties would also be updated.");
            System.out.println("[DRY RUN] No files were modified.");
            return;
        }

        // Copy mod to a new output directory — never modify the original
        String outName = srcRoot.getFileName() + "-ported-" + toVer.replace(".", "_");
        Path modRoot = srcRoot.getParent().resolve(outName);
        if (Files.exists(modRoot)) {
            System.out.println("Output dir already exists, removing: " + modRoot);
            deleteDir(modRoot);
        }
        System.out.println("Copying to : " + modRoot);
        copyDir(srcRoot, modRoot);
        System.out.println();

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("sourceMod",   modPath);
        report.put("outputMod",   modRoot.toString());
        report.put("fromVersion", fromVer);
        report.put("toVersion",   toVer);
        report.put("startedAt",   new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        List<Map<String, Object>> steps = new ArrayList<>();

        // Step 0: Update Loom/plugin versions in build.gradle
        System.out.println("[0/4] Updating build.gradle plugin versions...");
        BuildFilePatcher buildPatcher = new BuildFilePatcher();
        PatchResult buildFileResult = buildPatcher.patch(modRoot, toVer);
        buildFileResult.print();
        steps.add(stepMap("build.gradle", buildFileResult));

        // Step 1: Update gradle.properties
        System.out.println("[1/4] Updating gradle.properties...");
        GradlePropertiesPatcher gradlePatcher = new GradlePropertiesPatcher();
        PatchResult gradleResult = gradlePatcher.patch(modRoot, toVer);
        gradleResult.print();
        steps.add(stepMap("gradle.properties", gradleResult));

        // Step 2: Patch Java sources
        System.out.println("\n[2/4] Patching Java sources...");
        SourcePatcher sourcePatcher = new SourcePatcher();
        PatchResult sourceResult = sourcePatcher.patch(modRoot, fromVer, toVer, false);
        sourceResult.print();
        steps.add(stepMap("sources", sourceResult));

        // Step 3: Update mod metadata
        System.out.println("\n[3/4] Updating mod metadata (fabric.mod.json / mods.toml)...");
        ModMetaPatcher metaPatcher = new ModMetaPatcher();
        PatchResult metaResult = metaPatcher.patch(modRoot, toVer);
        metaResult.print();
        steps.add(stepMap("metadata", metaResult));

        // Step 4: Build
        Map<String, Object> buildStep = new LinkedHashMap<>();
        BuildRunner.BuildResult buildResult = null;
        if (buildAfter) {
            System.out.println("\n[4/4] Building ported mod...");
            BuildRunner runner = new BuildRunner();
            buildResult = runner.build(modRoot);
            buildResult.print();
            buildStep.put("step", "build");
            buildStep.put("success", buildResult.success());
            buildStep.put("exitCode", buildResult.exitCode());
            buildStep.put("errors", buildResult.errors());
            buildStep.put("logFile", buildResult.logFile());
        } else {
            System.out.println("\n[4/4] Build skipped (--no-build)");
            buildStep.put("step", "build");
            buildStep.put("success", "skipped");
        }
        steps.add(buildStep);

        // Write report
        report.put("steps", steps);
        report.put("success", buildResult == null || buildResult.success());
        report.put("completedAt", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

        // Write to mod's build dir
        Path reportDir = modRoot.resolve("build");
        Files.createDirectories(reportDir);
        Path reportFile = reportDir.resolve("port-report.json");
        try (FileWriter w = new FileWriter(reportFile.toFile())) { GSON.toJson(report, w); }

        System.out.println("\n═══════════════════════════════════");
        System.out.println("Port report: " + reportFile.toAbsolutePath());

        boolean allOk = gradleResult.success() && sourceResult.success() && metaResult.success()
                        && (buildResult == null || buildResult.success());
        if (allOk) {
            System.out.println("STATUS: ✓ Port complete — mod ready for " + toVer);
        } else {
            System.out.println("STATUS: ⚠ Port complete with issues — review port-report.json");
            System.exit(1);
        }
    }

    // ── File utilities ────────────────────────────────────────────────────────

    static void copyDir(Path src, Path dst) throws IOException {
        Files.createDirectories(dst);
        try (var s = Files.walk(src)) {
            for (var it = s.iterator(); it.hasNext(); ) {
                Path from = it.next();
                // Skip build/, .gradle/ dirs to keep the copy lean
                String rel = src.relativize(from).toString().replace("\\", "/");
                if (rel.startsWith("build/") || rel.startsWith(".gradle/") || rel.startsWith(".git/")) continue;
                Path to = dst.resolve(src.relativize(from));
                if (Files.isDirectory(from)) Files.createDirectories(to);
                else Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    static void deleteDir(Path dir) throws IOException {
        try (var s = Files.walk(dir)) {
            s.sorted(Comparator.reverseOrder()).forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
        }
    }

    // ── Setup templates ───────────────────────────────────────────────────────

    static void setupTemplates() throws Exception {
        Path templatesDir = Path.of("templates");
        TemplateManager manager = new TemplateManager(templatesDir);
        manager.downloadAll();
        System.out.println("\nTemplates ready in: " + templatesDir.toAbsolutePath());
    }

    // ── List versions ─────────────────────────────────────────────────────────

    static void listVersions() {
        System.out.println("Supported Minecraft versions:\n");
        for (String v : VersionDatabase.allVersions()) {
            VersionDatabase.VersionInfo info = VersionDatabase.get(v);
            List<String> loaders = new ArrayList<>();
            if (info.hasFabric())   loaders.add("Fabric");
            if (info.hasForge())    loaders.add("Forge");
            if (info.hasNeoForge()) loaders.add("NeoForge");
            System.out.printf("  %-10s  %s%n", v, String.join(", ", loaders));
        }
    }

    static void printHelp() {
        System.out.println("Usage:");
        System.out.println("  auto-porter <modPath> <fromVer> <toVer> [--dry-run] [--no-build]");
        System.out.println("  auto-porter --setup-templates");
        System.out.println("  auto-porter --list-versions");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  auto-porter C:/mods/mymod 1.21.4 1.21.10");
        System.out.println("  auto-porter C:/mods/mymod 1.21.4 1.21.10 --dry-run");
        System.out.println("  auto-porter C:/mods/mymod 1.20.1 26.1 --no-build");
    }

    private static Map<String, Object> stepMap(String name, PatchResult r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("step",    name);
        m.put("success", r.success());
        m.put("message", r.message());
        m.put("changes", r.changes());
        return m;
    }
}
