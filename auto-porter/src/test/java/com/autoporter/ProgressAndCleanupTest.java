package com.autoporter;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** GUI progress parsing, and repository hygiene (no dead analyzers, no AI features). */
class ProgressAndCleanupTest {

    @Test void progressMovesFromZeroToHundredThroughEveryStage() {
        List<String> log = List.of(
            "Source mod : mymod", "Inspecting project...", "  Source mappings: YARN (…)", "Copying to : C:/mods/x",
            AutoPorterMain.header(1, "Updating build scripts and Gradle wrapper..."), "  [INFO] something",
            AutoPorterMain.header(2, "Updating gradle.properties..."),
            AutoPorterMain.header(3, "Remapping sources (Yarn → Mojang)..."),
            AutoPorterMain.header(4, "Applying version rules to Java sources..."),
            AutoPorterMain.header(5, "Patching resources and data files..."),
            AutoPorterMain.header(6, "Scanning for manual migration items..."),
            AutoPorterMain.header(7, "Updating mod metadata..."),
            AutoPorterMain.header(8, "Patching access widener / class tweaker..."),
            AutoPorterMain.header(9, "Checking build files for stale version numbers..."),
            "[build] Verifying with Gradle...", "> Task :compileJava", "> Task :classes",
            "STATUS: ✓ Port complete — mod ready for 26.1.2");
        int percent = 0;
        List<Integer> seen = new ArrayList<>();
        List<String> statuses = new ArrayList<>();
        for (String line : log) {
            var update = PortProgress.parse(line, percent);
            if (update.isPresent()) {
                assertTrue(update.get().percent() >= percent, "progress never goes backwards");
                percent = update.get().percent();
                seen.add(percent);
                if (update.get().status() != null) statuses.add(update.get().status());
            }
        }
        assertEquals(10, seen.get(0), "inspecting the project is the first visible step");
        assertEquals(100, percent);
        assertTrue(seen.contains(85) && seen.contains(86) && seen.contains(87), "Gradle tasks keep it moving: " + seen);
        assertTrue(statuses.contains("Remapping sources (Yarn → Mojang)"), statuses.toString());
        assertTrue(statuses.contains("Verifying Gradle build"));
        assertEquals("Port complete", statuses.get(statuses.size() - 1));
        assertTrue(seen.stream().distinct().count() >= 14, "every stage moves the bar: " + seen);
    }

    @Test void failedPortEndsAtHundredWithIssuesStatus() {
        var update = PortProgress.parse("STATUS: ⚠ Port complete with issues — review port-report.json", 40);
        assertEquals(100, update.orElseThrow().percent());
        assertEquals("Completed with issues", update.get().status());
        assertTrue(PortProgress.parse("[2/9] Updating gradle.properties...", 70).isEmpty(), "never backwards");
    }

    /** The repository root (auto-porter/..). */
    private static Path repoRoot() {
        Path here = Path.of("").toAbsolutePath();
        return here.getFileName().toString().equals("auto-porter") ? here.getParent() : here;
    }

    @Test void noDeadAnalyzersOrAiReferencesInTheRepository() throws IOException {
        Path root = repoRoot();
        // Excluded: upstream copies (they talk about *mob* AI), the separate support-bot project kept in this folder,
        // build output, and the license files (a legal "no AI training" clause is not a feature).
        Set<String> skipDirs = Set.of(".git", ".gradle", "build", "bin", "node_modules", "devhub-bot", "fetched");
        Set<String> skipFiles = Set.of("LICENSE", "COMMERCIAL-LICENSE-CONTACT.md", "ProgressAndCleanupTest.java");
        Pattern forbidden = Pattern.compile(
            "DeepAnalyzer|\\bAI\\b|\\bLLMs?\\b|OpenAI|ChatGPT|Gemini|Copilot|Anthropic|language model|AI_GUIDE", Pattern.CASE_INSENSITIVE);
        Pattern textFile = Pattern.compile(".*\\.(java|kt|gradle|kts|md|json|properties|toml|yml|yaml|txt|sh|ps1|bat|cfg)$");
        List<String> hits = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile)
                .filter(p -> root.relativize(p).getNameCount() > 0)
                .filter(p -> { for (Path part : root.relativize(p)) if (skipDirs.contains(part.toString())) return false; return true; })
                .filter(p -> !skipFiles.contains(p.getFileName().toString()))
                .filter(p -> textFile.matcher(p.getFileName().toString()).matches())
                .forEach(p -> {
                    try {
                        List<String> lines = Files.readAllLines(p);
                        for (int i = 0; i < lines.size(); i++) {
                            String line = lines.get(i);
                            var m = forbidden.matcher(line);
                            // "AI" in the case-insensitive sense would also hit words like "ai" in other languages; only
                            // flag the upper-case acronym and the explicit names.
                            while (m.find()) {
                                String hit = m.group();
                                if (hit.equalsIgnoreCase("ai") && !hit.equals("AI")) continue;
                                hits.add(root.relativize(p) + ":" + (i + 1) + ": " + line.strip());
                            }
                        }
                    } catch (IOException ignored) { /* binary or unreadable */ }
                });
        }
        assertEquals(List.of(), hits);
    }

    @Test void unusedDependenciesAreGone() throws IOException {
        String build = Files.readString(repoRoot().resolve("auto-porter/build.gradle"));
        assertFalse(build.contains("javaparser"), "JavaParser was never used");
        assertFalse(build.contains("slf4j"), "slf4j-simple was never used");
    }
}
