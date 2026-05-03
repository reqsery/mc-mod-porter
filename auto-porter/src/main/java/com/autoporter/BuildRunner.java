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

        // Determine the required Java version from the mod's build.gradle
        int requiredJava = detectRequiredJavaVersion(modRoot);
        String javaHome  = detectJavaHome(requiredJava);

        System.out.println("  Requires Java " + requiredJava
            + (javaHome != null ? " — found at: " + javaHome : " — NOT FOUND on this machine"));

        if (javaHome == null && requiredJava >= 25) {
            return new BuildResult(false, -1, "",
                "Java " + requiredJava + " is required to build this mod but was not found.\n"
                + "Install JDK " + requiredJava + " and make sure JAVA_HOME points to it, then retry.",
                "");
        }

        System.out.println("  Running: " + gradlew.getFileName() + " compileJava --no-daemon");
        ProcessBuilder pb = new ProcessBuilder(gradlew.toAbsolutePath().toString(), "compileJava", "--no-daemon");
        pb.directory(modRoot.toFile());
        pb.redirectErrorStream(true);

        if (javaHome != null) pb.environment().put("JAVA_HOME", javaHome);

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

    /**
     * Read the Java toolchain version from the mod's build.gradle.
     * Falls back to 21 if it can't be determined.
     */
    private int detectRequiredJavaVersion(Path modRoot) {
        Path buildGradle = modRoot.resolve("build.gradle");
        if (!Files.exists(buildGradle)) return 21;
        try {
            String content = Files.readString(buildGradle);
            // Match: languageVersion = JavaLanguageVersion.of(25)
            var m = java.util.regex.Pattern.compile("JavaLanguageVersion\\.of\\((\\d+)\\)").matcher(content);
            if (m.find()) return Integer.parseInt(m.group(1));
            // Match: sourceCompatibility = JavaVersion.VERSION_25 or = '25' or = 25
            m = java.util.regex.Pattern.compile("sourceCompatibility\\s*=\\s*(?:JavaVersion\\.VERSION_)?(\\d+)").matcher(content);
            if (m.find()) return Integer.parseInt(m.group(1));
        } catch (IOException ignored) {}
        return 21;
    }

    /**
     * Find a JDK home for the requested Java version.
     * Checks common install locations on Windows and Linux/Mac.
     * Returns null if not found (caller decides what to do).
     */
    private String detectJavaHome(int version) {
        String home = System.getProperty("user.home");
        List<String> candidates = new ArrayList<>();

        // Windows — Eclipse Adoptium / Temurin, Oracle, Microsoft, Amazon Corretto
        candidates.add(home + "/AppData/Local/Programs/Eclipse Adoptium/jdk-" + version + ".0.0-hotspot");
        candidates.add(home + "/AppData/Local/Programs/Eclipse Adoptium/jdk-" + version);
        candidates.add("C:/Program Files/Eclipse Adoptium/jdk-" + version);
        candidates.add("C:/Program Files/Microsoft/jdk-" + version);
        candidates.add("C:/Program Files/Java/jdk-" + version);
        candidates.add("C:/Program Files/Amazon Corretto/jdk" + version);
        // Exact version strings seen on user's machine (from previous detection)
        if (version == 21) {
            candidates.add(home + "/AppData/Local/Programs/Eclipse Adoptium/jdk-21.0.10.7-hotspot");
        }

        // Linux / macOS
        candidates.add("/usr/lib/jvm/java-" + version + "-openjdk-amd64");
        candidates.add("/usr/lib/jvm/java-" + version + "-openjdk");
        candidates.add("/usr/lib/jvm/java-" + version);
        candidates.add("/usr/local/lib/jvm/java-" + version);
        candidates.add("/Library/Java/JavaVirtualMachines/jdk-" + version + ".jdk/Contents/Home");

        // Also check JAVA_HOME already set in environment
        String envJavaHome = System.getenv("JAVA_HOME");
        if (envJavaHome != null) {
            // Verify it matches the required version
            File releaseFile = new File(envJavaHome, "release");
            if (releaseFile.exists()) {
                try {
                    String release = Files.readString(releaseFile.toPath());
                    if (release.contains("JAVA_VERSION=\"" + version + ".")
                        || release.contains("JAVA_VERSION=\"" + version + "\"")) {
                        return envJavaHome;
                    }
                } catch (IOException ignored) {}
            }
        }

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
