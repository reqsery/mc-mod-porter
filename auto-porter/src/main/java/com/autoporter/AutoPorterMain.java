package com.autoporter;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Auto-Porter — ports a Minecraft mod source project between versions.
 *
 * Usage:
 *   java -jar auto-porter.jar                                   (desktop GUI)
 *   java -jar auto-porter.jar <modPath> <fromVersion> <toVersion> [options]
 *   java -jar auto-porter.jar --list-versions | --setup-templates | --help
 */
public class AutoPorterMain {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    static final int STEPS = 9;

    /** Command-line options for a port. */
    record Options(boolean dryRun, boolean build, boolean remap, boolean noRemap, SourceRemapper.Mappings sourceMappings) {
        static Options parse(List<String> flags) {
            boolean dryRun = false, noBuild = false, remap = false, noRemap = false;
            SourceRemapper.Mappings mappings = SourceRemapper.Mappings.AUTO;
            for (int i = 0; i < flags.size(); i++) {
                String flag = flags.get(i);
                switch (flag) {
                    case "--dry-run" -> dryRun = true;
                    case "--no-build" -> noBuild = true;
                    case "--remap" -> remap = true;
                    case "--no-remap" -> noRemap = true;
                    case "--source-mappings" -> {
                        if (i + 1 >= flags.size()) throw new IllegalArgumentException("--source-mappings needs a value: auto, yarn or mojang");
                        mappings = SourceRemapper.Mappings.parse(flags.get(++i));
                    }
                    default -> {
                        if (flag.startsWith("--source-mappings=")) {
                            mappings = SourceRemapper.Mappings.parse(flag.substring("--source-mappings=".length()));
                        } else {
                            throw new IllegalArgumentException("Unknown option: " + flag + " (see --help)");
                        }
                    }
                }
            }
            if (remap && noRemap) throw new IllegalArgumentException("--remap and --no-remap cannot be used together");
            return new Options(dryRun, !noBuild && !dryRun, remap, noRemap, mappings);
        }
    }

    public static void main(String[] args) throws Exception {
        if (shouldLaunchGui(args)) {
            MainWindow.launch();
            return;
        }

        ConsoleText.install();
        System.out.println("╔══════════════════════════════════╗");
        System.out.println("║        MC Mod Auto-Porter        ║");
        System.out.println("╚══════════════════════════════════╝\n");

        switch (args[0]) {
            case "--help", "-h" -> { printHelp(); return; }
            case "--list-versions" -> { listVersions(); return; }
            case "--setup-templates" -> { setupTemplates(); return; }
            default -> { }
        }

        if (args.length < 3) {
            System.err.println("Usage: auto-porter <modPath> <fromVersion> <toVersion> [--dry-run] [--no-build] "
                + "[--remap | --no-remap] [--source-mappings auto|yarn|mojang]");
            System.exit(1);
        }

        Options options;
        try {
            options = Options.parse(Arrays.asList(args).subList(3, args.length));
        } catch (IllegalArgumentException e) {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(1);
            return;
        }
        portMod(args[0], args[1], args[2], options);
    }

    static boolean shouldLaunchGui(String[] args) {
        return args.length == 0;
    }

    static void portMod(String modPath, String fromVer, String toVer, boolean buildAfter, boolean dryRun) throws Exception {
        portMod(modPath, fromVer, toVer, new Options(dryRun, buildAfter && !dryRun, false, false, SourceRemapper.Mappings.AUTO));
    }

    // ── Port a mod ────────────────────────────────────────────────────────────

    static void portMod(String modPath, String fromVer, String toVer, Options options) throws Exception {
        Path srcRoot = Path.of(modPath).toAbsolutePath().normalize();
        if (Files.exists(srcRoot) && !Files.isDirectory(srcRoot)) {
            String fileName = srcRoot.getFileName().toString().toLowerCase(Locale.ROOT);
            System.err.println("ERROR: Auto-Porter needs a mod source/project folder, not a compiled file.");
            System.err.println();
            System.err.println("You provided: " + srcRoot.getFileName());
            System.err.println();
            if (fileName.endsWith(".jar")) {
                System.err.println("Compiled mod JARs from Modrinth/CurseForge cannot be ported directly.");
            } else if (fileName.endsWith(".zip")) {
                System.err.println("If this is a source ZIP, extract it first and pass the extracted folder.");
            }
            System.err.println("Use a folder containing build.gradle, gradle.properties, gradlew/gradlew.bat, and src/.");
            System.exit(1);
        }

        if (options.dryRun()) System.out.println("*** DRY RUN — no files will be modified ***\n");

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

        System.out.println("Inspecting project...");
        RemapPlan plan = RemapPlan.decide(srcRoot, fromVer, toVer, options);
        System.out.println("  Source mappings: " + plan.detection().mappings() + " (" + plan.detection().reason() + ")");
        System.out.println("  Remap sources  : " + (plan.remap() ? "yes (Yarn → Mojang)" : "no") + " — " + plan.why());
        System.out.println();

        // In dry-run mode, scan the original source — no copy, no writes
        if (options.dryRun()) {
            if (plan.remap()) {
                System.out.println("[DRY RUN] Remapping preview...");
                plan.run(srcRoot, true).print();
                System.out.println();
            }
            System.out.println("[DRY RUN] Scanning sources for changes...\n");
            new SourcePatcher().patch(srcRoot, fromVer, toVer, true).print();
            System.out.println();
            new ResourcePatcher().patch(srcRoot, fromVer, toVer, true).print();
            System.out.println();
            new MigrationWarningScanner().scan(srcRoot, fromVer, toVer).print();
            System.out.println("\n[DRY RUN] build.gradle, gradle.properties, the Gradle wrapper and metadata would also be updated.");
            System.out.println("[DRY RUN] No files were modified.");
            return;
        }

        // Copy mod to a new output directory — never modify the original
        Path modRoot = outputDirectoryFor(srcRoot, toVer);
        if (Files.exists(modRoot)) {
            System.out.println("Output dir already exists, removing: " + modRoot);
            deleteDir(modRoot);
        }
        System.out.println("Copying to : " + modRoot);
        copyDir(srcRoot, modRoot);
        BuildRunner.ensureUnixGradleWrapperExecutable(modRoot);
        System.out.println();

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("sourceMod",   modPath);
        report.put("outputMod",   modRoot.toString());
        report.put("fromVersion", fromVer);
        report.put("toVersion",   toVer);
        report.put("sourceMappings", plan.detection().mappings().name());
        report.put("remapped",    plan.remap());
        report.put("startedAt",   new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        List<Map<String, Object>> steps = new ArrayList<>();
        StaleVersionCheck staleCheck = StaleVersionCheck.snapshot(modRoot, fromVer);

        // Ask about mappings when backporting from 26.x to a Fabric pre-26.x version
        // (26.x has no mappings; earlier versions need one re-added)
        boolean useYarn = false;
        VersionDatabase.VersionInfo toInfo = VersionDatabase.get(toVer);
        if (VersionDatabase.isUnobfuscated(fromVer) && !VersionDatabase.isUnobfuscated(toVer) && toInfo != null && toInfo.hasFabric()) {
            System.out.println("─────────────────────────────────────────");
            System.out.println("Mappings choice (required for pre-26.x Fabric builds):");
            System.out.println("  1) Mojang official  [loom.officialMojangMappings()]");
            String yarnLabel = toInfo.yarnMappings() != null
                ? "2) Yarn              [" + toInfo.yarnMappings() + ":v2]"
                : "2) Yarn              [not available for " + toVer + "]";
            System.out.println("  " + yarnLabel);
            System.out.print("Enter 1 or 2 [default: 1]: ");
            System.out.flush();
            String choice = readLine();
            if (choice.equals("2") && toInfo.yarnMappings() != null) {
                useYarn = true;
                System.out.println("→ Using Yarn mappings: " + toInfo.yarnMappings());
            } else if (choice.equals("2")) {
                System.out.println("→ Yarn not available for " + toVer + ", falling back to Mojang official.");
            } else {
                System.out.println("→ Using Mojang official mappings.");
            }
            System.out.println("─────────────────────────────────────────");
            System.out.println();
        }

        System.out.println(header(1, "Updating build scripts and Gradle wrapper..."));
        PatchResult buildFileResult = new BuildFilePatcher().patch(modRoot, fromVer, toVer, useYarn, plan.remap());
        buildFileResult.print();
        steps.add(stepMap("build.gradle", buildFileResult));

        System.out.println(header(2, "Updating gradle.properties..."));
        PatchResult gradleResult = new GradlePropertiesPatcher().patch(modRoot, toVer, useYarn, plan.remap());
        gradleResult.print();
        steps.add(stepMap("gradle.properties", gradleResult));

        System.out.println("\n" + header(3, "Remapping sources (Yarn → Mojang)..."));
        PatchResult remapResult = plan.run(modRoot, false);
        remapResult.print();
        steps.add(stepMap("remap", remapResult));

        System.out.println("\n" + header(4, "Applying version rules to Java sources..."));
        PatchResult sourceResult = new SourcePatcher().patch(modRoot, fromVer, toVer, false);
        sourceResult.print();
        steps.add(stepMap("sources", sourceResult));

        System.out.println("\n" + header(5, "Patching resources and data files..."));
        PatchResult resourceResult = new ResourcePatcher().patch(modRoot, fromVer, toVer, false);
        resourceResult.print();
        steps.add(stepMap("resources", resourceResult));

        System.out.println("\n" + header(6, "Scanning for manual migration items..."));
        PatchResult warningResult = new MigrationWarningScanner().scan(modRoot, fromVer, toVer);
        warningResult.print();
        steps.add(stepMap("warnings", warningResult));

        System.out.println("\n" + header(7, "Updating mod metadata (fabric.mod.json / mods.toml / mixins)..."));
        PatchResult metaResult = new ModMetaPatcher().patch(modRoot, toVer);
        metaResult.print();
        steps.add(stepMap("metadata", metaResult));

        System.out.println("\n" + header(8, "Patching access widener / class tweaker..."));
        PatchResult awResult = new AccessWidenerPatcher().patch(modRoot, toVer);
        awResult.print();
        steps.add(stepMap("accesswidener", awResult));

        System.out.println("\n" + header(9, "Checking build files for stale version numbers..."));
        PatchResult staleResult = staleCheck.verify(modRoot, toVer);
        staleResult.print();
        steps.add(stepMap("stale-versions", staleResult));

        Map<String, Object> buildStep = new LinkedHashMap<>();
        BuildRunner.BuildResult buildResult = null;
        buildStep.put("step", "build");
        if (options.build()) {
            System.out.println("\n[build] Verifying with Gradle...");
            buildResult = new BuildRunner().build(modRoot);
            buildResult.print();
            buildStep.put("success", buildResult.success());
            buildStep.put("exitCode", buildResult.exitCode());
            buildStep.put("errors", buildResult.errors());
            buildStep.put("logFile", buildResult.logFile());
        } else {
            System.out.println("\n[build] Build skipped (--no-build)");
            buildStep.put("success", "skipped");
        }
        steps.add(buildStep);

        report.put("steps", steps);
        report.put("success", buildResult == null || buildResult.success());
        report.put("completedAt", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

        Path reportDir = modRoot.resolve("build");
        Files.createDirectories(reportDir);
        Path reportFile = reportDir.resolve("port-report.json");
        try (Writer w = Files.newBufferedWriter(reportFile)) { GSON.toJson(report, w); }

        System.out.println("\n═══════════════════════════════════");
        System.out.println("Port report: " + reportFile.toAbsolutePath());

        boolean allOk = buildFileResult.success() && gradleResult.success() && remapResult.success()
                        && sourceResult.success() && resourceResult.success() && warningResult.success()
                        && metaResult.success() && awResult.success()
                        && (buildResult == null || buildResult.success());
        if (allOk) {
            System.out.println("STATUS: ✓ Port complete — mod ready for " + toVer);
        } else {
            System.out.println("STATUS: ⚠ Port complete with issues — review port-report.json");
            System.exit(1);
        }
    }

    static String header(int step, String text) {
        return "[" + step + "/" + STEPS + "] " + text;
    }

    private static String readLine() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            String line = in.readLine();
            return line == null ? "" : line.trim();
        } catch (IOException e) {
            return "";
        }
    }

    /** Whether (and why) the sources are remapped from Yarn to Mojang names. */
    record RemapPlan(SourceRemapper.Detection detection, boolean remap, String why, String fromVersion) {
        static RemapPlan decide(Path root, String from, String to, Options options) throws IOException {
            SourceRemapper.Detection detected = SourceRemapper.detect(root, from);
            SourceRemapper.Mappings mode = options.sourceMappings();
            SourceRemapper.Detection effective = mode == SourceRemapper.Mappings.AUTO ? detected
                : new SourceRemapper.Detection(mode, detected.yarnBuild(), "set with --source-mappings");
            boolean yarn = effective.mappings() == SourceRemapper.Mappings.YARN;
            if (options.noRemap()) return new RemapPlan(effective, false, "--no-remap", from);
            if (VersionDatabase.isUnobfuscated(from)) {
                return new RemapPlan(effective, false, "26.x sources already use official names", from);
            }
            if (!yarn) {
                return new RemapPlan(effective, false, options.remap()
                    ? "--remap given, but the sources do not use Yarn names" : "sources do not use Yarn names", from);
            }
            if (options.remap()) return new RemapPlan(effective, true, "--remap", from);
            if (VersionDatabase.isUnobfuscated(to)) {
                return new RemapPlan(effective, true, "26.x has no Yarn: official Mojang names are required", from);
            }
            return new RemapPlan(effective, false, "target still supports Yarn (use --remap to switch to Mojang names)", from);
        }

        PatchResult run(Path root, boolean dryRun) {
            if (!remap) return PatchResult.success("Remapping skipped (" + why + ")", List.of());
            String yarnBuild = detection.yarnBuild();
            if (yarnBuild == null) {
                VersionDatabase.VersionInfo info = VersionDatabase.get(fromVersion);
                yarnBuild = info == null ? null : info.yarnMappings();
            }
            if (yarnBuild == null) return PatchResult.failure("Remapping failed: no Yarn build is known for " + fromVersion);
            MappingProvider provider = new MappingProvider();
            try {
                System.out.println("  Loading Yarn " + yarnBuild + " + Mojang " + fromVersion + " mappings (cache: "
                    + provider.cacheDir() + ")");
                MappingTable table = provider.yarnToMojang(fromVersion, yarnBuild);
                System.out.println("  " + table.classCount() + " classes mapped");
                return SourceRemapper.remapProject(root, table, dryRun);
            } catch (IOException e) {
                return PatchResult.failure("Remapping failed: " + e.getMessage()
                    + " — check your internet connection (mappings are downloaded once and cached)");
            }
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

    static Path outputDirectoryFor(Path srcRoot, String toVer) {
        Path absoluteSource = srcRoot.toAbsolutePath().normalize();
        String outName = absoluteSource.getFileName() + "-ported-" + toVer.replace(".", "_");
        return absoluteSource.getParent().resolve(outName);
    }

    // ── Setup templates ───────────────────────────────────────────────────────

    static void setupTemplates() throws Exception {
        Path templatesDir = Path.of("templates");
        new TemplateManager(templatesDir).writeAll();
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
            System.out.printf("  %-10s  Java %-3d %s%n", v, BuildTooling.javaFor(v), String.join(", ", loaders));
        }
    }

    static void printHelp() {
        System.out.println("Usage:");
        System.out.println("  auto-porter                                  open the desktop GUI");
        System.out.println("  auto-porter <modPath> <fromVer> <toVer> [options]");
        System.out.println("  auto-porter --list-versions");
        System.out.println("  auto-porter --setup-templates");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --dry-run                      preview changes on the original folder, write nothing");
        System.out.println("  --no-build                     skip the gradlew compileJava check");
        System.out.println("  --remap                        remap Yarn names to Mojang names in the sources");
        System.out.println("  --no-remap                     never remap (default: remap when Yarn sources go to 26.x)");
        System.out.println("  --source-mappings <mode>       auto (default), yarn or mojang");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  auto-porter C:/mods/mymod 1.21.4 1.21.10");
        System.out.println("  auto-porter C:/mods/mymod 1.21.4 1.21.10 --dry-run");
        System.out.println("  auto-porter C:/mods/mymod 1.21.1 26.1.2              (Yarn sources are remapped automatically)");
        System.out.println("  auto-porter C:/mods/mymod 1.20.1 1.21.1 --remap      (switch a Yarn mod to Mojang names)");
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
