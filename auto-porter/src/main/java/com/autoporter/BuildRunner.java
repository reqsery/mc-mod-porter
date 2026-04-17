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
            return new BuildResult(false, -1, "", "No gradlew found at " + modRoot);
        }

        System.out.println("  Running: " + gradlew.getFileName() + " build --no-daemon");
        ProcessBuilder pb = new ProcessBuilder(gradlew.toAbsolutePath().toString(), "build", "--no-daemon");
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

        // Extract errors from output
        List<String> errors = new ArrayList<>();
        for (String line : output.toString().split("\n")) {
            if (line.contains("error:") || line.contains("ERROR") || line.contains("FAILED")) {
                errors.add(line.trim());
            }
        }

        return new BuildResult(success, exitCode, output.toString(), String.join("\n", errors));
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

    public record BuildResult(boolean success, int exitCode, String fullOutput, String errors) {
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
            }
        }
    }
}
