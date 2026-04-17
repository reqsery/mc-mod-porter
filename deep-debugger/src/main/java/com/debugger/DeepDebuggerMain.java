package com.debugger;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Entry point for the Deep Debugger.
 *
 * Usage:
 *   java -jar deep-debugger.jar [debugger-config.json]
 *   java -jar deep-debugger.jar --modPath <path> --package <pkg>
 *
 * Or configure via debugger-config.json:
 *   {"modPath": "C:\Users\...\groupchat", "package": "com.groupchat"}
 */
public class DeepDebuggerMain {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) throws Exception {
        DebuggerConfig config = parseConfig(args);
        System.out.println("=== Deep Debugger ===");
        System.out.println("Mod path : " + config.modPath());
        System.out.println("Package  : " + config.pkg());

        // 1. Scan sources
        System.out.println("\n[1/4] Scanning mod sources...");
        SourceScanner scanner = new SourceScanner();
        List<MethodInfo> methods = scanner.scan(config.modPath());
        System.out.printf("Found %d public methods across %d classes%n",
            methods.size(),
            methods.stream().map(MethodInfo::className).distinct().count());

        // 2. Generate test classes
        System.out.println("\n[2/4] Generating unit tests...");
        Path genDir = Path.of("src/test/java/generated");
        TestGenerator generator = new TestGenerator(genDir);
        List<Path> generated = generator.generate(methods);
        System.out.printf("Generated %d test class(es) in %s%n", generated.size(), genDir);

        // 3. State machine tests
        System.out.println("\n[3/4] Running state machine analysis...");
        StateMachineTester stateTester = new StateMachineTester();
        List<Map<String, Object>> stateResults = stateTester.runStateTests(config.modPath(), config.pkg());
        long statePassed = stateResults.stream().filter(r -> "PASSED".equals(r.get("status"))).count();
        System.out.printf("State machine: %d/%d checks passed%n", statePassed, stateResults.size());

        // 4. Fuzz analysis
        System.out.println("\n[4/4] Generating fuzz test cases...");
        FuzzTester fuzzer = new FuzzTester();
        int fuzzTotal = 0;
        for (MethodInfo m : methods) {
            fuzzTotal += fuzzer.generateFuzzCases(m).size();
        }
        System.out.printf("Fuzz cases generated: %d%n", fuzzTotal);

        // Write summary
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("generatedAt", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        summary.put("modPath",     config.modPath());
        summary.put("package",     config.pkg());
        summary.put("methodsFound", methods.size());
        summary.put("testClassesGenerated", generated.size());
        summary.put("stateTests", stateResults);
        summary.put("fuzzCasesGenerated", fuzzTotal);
        summary.put("note", "Run './gradlew test' to execute generated tests and produce test-report.json");

        Path summaryFile = Path.of("build/debug-summary.json");
        Files.createDirectories(summaryFile.getParent());
        try (FileWriter w = new FileWriter(summaryFile.toFile())) {
            GSON.toJson(summary, w);
        }
        System.out.println("\nSummary written to: " + summaryFile.toAbsolutePath());
        System.out.println("Next: ./gradlew test   (to run generated tests)");
    }

    private static DebuggerConfig parseConfig(String[] args) throws IOException {
        // Try config file first
        Path cfgFile = Path.of("debugger-config.json");
        if (Files.exists(cfgFile)) {
            return DebuggerConfig.load(cfgFile.toString());
        }
        // Parse CLI args
        String modPath = null, pkg = "";
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals("--modPath") || args[i].equals("-m")) modPath = args[i + 1];
            if (args[i].equals("--package") || args[i].equals("-p")) pkg     = args[i + 1];
        }
        if (modPath == null && args.length > 0 && args[0].endsWith(".json")) {
            return DebuggerConfig.load(args[0]);
        }
        if (modPath == null) {
            // Default to GroupChat
            modPath = Path.of(System.getProperty("user.home"), "Desktop/groupchat").toString();
            pkg = "com.groupchat";
            System.out.println("No config found, defaulting to: " + modPath);
        }
        return DebuggerConfig.fromArgs(modPath, pkg);
    }
}
