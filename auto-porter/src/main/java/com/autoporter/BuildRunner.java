package com.autoporter;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Runs the mod's Gradle build and collects any remaining errors after porting.
 */
public class BuildRunner {

    public BuildResult build(Path modRoot) throws IOException, InterruptedException {
        // Detect gradlew
        Path gradlew = modRoot.resolve(isWindows() ? "gradlew.bat" : "gradlew");
        if (!Files.exists(gradlew)) {
            return new BuildResult(false, -1, "", "No gradlew found at " + modRoot, "");
        }

        System.out.println("  Running: " + gradlew.getFileName() + " genSources downloadAssets build --no-daemon");
        ProcessBuilder pb = new ProcessBuilder(gradlew.toAbsolutePath().toString(), "genSources", "downloadAssets", "build", "--no-daemon");
        pb.directory(modRoot.toFile());
        pb.redirectErrorStream(true);

        // Set JAVA_HOME to Java 21 if available
        String java21 = detectJava21();
        if (java21 != null) pb.environment().put("JAVA_HOME", java21);

        Process process = pb.start();
        StringBuilder output = new StringBuilder();
        try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        boolean success = exitCode == 0;

        // Write full output to log file so users can inspect it
        Path logFile = modRoot.resolve("port-build.log");
        Files.writeString(logFile, output.toString());

        // Extract meaningful error lines from output
        // Capture: Java compile errors (error:), task failures (FAILED), Gradle exceptions
        List<String> errorLines = new ArrayList<>();
        String[] lines = output.toString().split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            // Java compiler errors: "path/File.java:42: error: cannot find symbol"
            if (line.matches(".*\\.java:\\d+: error:.*") || line.startsWith("error:")) {
                errorLines.add(line);
                // Include the next 3 lines for context (symbol, location, caret line)
                for (int j = 1; j <= 3 && i + j < lines.length; j++) {
                    String ctx = lines[i + j].trim();
                    if (!ctx.isBlank()) errorLines.add("  " + ctx);
                }
            }
            // Gradle task failure lines
            else if (line.startsWith("> Task") && line.contains("FAILED")) {
                errorLines.add(line);
            }
            // Gradle build exception summary
            else if (line.startsWith("* What went wrong:") || line.startsWith("* Exception is:")) {
                errorLines.add(line);
                // Include next few lines
                for (int j = 1; j <= 5 && i + j < lines.length; j++) {
                    String ctx = lines[i + j].trim();
                    if (ctx.isBlank() || ctx.startsWith("*")) break;
                    errorLines.add("  " + ctx);
                }
            }
        }
        // If we found nothing specific, fall back to any line with "FAILED"
        if (errorLines.isEmpty()) {
            for (String line : lines) {
                if (line.contains("FAILED") || line.contains("BUILD FAILED")) {
                    errorLines.add(line.trim());
                }
            }
        }

        String errors = errorLines.isEmpty() ? "" : String.join("\n", errorLines);
        return new BuildResult(success, exitCode, output.toString(), errors, logFile.toString());
    }

    private String detectJava21() {
        List<String> candidates = List.of(
            System.getProperty("user.home") + "/AppData/Local/Programs/Eclipse Adoptium/jdk-21.0.10.7-hotspot",
            "/usr/lib/jvm/java-21-openjdk-amd64",
            "/usr/lib/jvm/java-21"
        );
        for (String c : candidates) {
            if (new File(c).exists()) return c;
        }
        return null;
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    public record BuildResult(boolean success, int exitCode, String fullOutput, String errors, String logFile) {
        public void print() {
            if (success) {
                System.out.println("  BUILD SUCCESSFUL");
            } else {
                System.out.println("  BUILD FAILED (exit " + exitCode + ")");
                if (!errors.isEmpty()) {
                    System.out.println("  Errors:");
                    for (String line : errors.split("\n")) {
                        if (!line.isBlank()) System.out.println("    " + line);
                    }
                }
                System.out.println("  Full build log: " + logFile);
            }
        }
    }
}
