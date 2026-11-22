package com.autoporter;

import java.util.Optional;
import java.util.regex.*;

/**
 * Turns the porter's console output into GUI progress: a percentage that only moves forward and a short status
 * text for the current stage.
 *
 * <pre>
 *   Inspecting project          10%
 *   Copying to : …              15%
 *   [n/9] &lt;stage&gt;               20% … 82%
 *   [build] Verifying with Gradle 85%, then +1% per Gradle task up to 97%
 *   STATUS: …                   100%
 * </pre>
 */
final class PortProgress {
    record Update(int percent, String status) {}

    private static final Pattern STEP = Pattern.compile("^\\[(\\d+)/(\\d+)]\\s*(.*?)\\.*$");

    private PortProgress() {}

    static Optional<Update> parse(String line, int current) {
        if (line == null) return Optional.empty();
        String text = line.strip();
        Update update = null;
        if (text.startsWith("Inspecting project")) {
            update = new Update(10, "Inspecting project");
        } else if (text.startsWith("Copying to")) {
            update = new Update(15, "Copying project");
        } else if (text.startsWith("[build]")) {
            update = new Update(85, text.contains("skipped") ? "Build skipped" : "Verifying Gradle build");
        } else if (text.startsWith("STATUS:")) {
            update = new Update(100, text.contains("✓") ? "Port complete" : "Completed with issues");
        } else if (text.startsWith("> Task") && current >= 85) {
            update = new Update(Math.min(current + 1, 97), null);
        } else {
            Matcher m = STEP.matcher(text);
            if (m.matches()) {
                int step = Integer.parseInt(m.group(1)), total = Math.max(1, Integer.parseInt(m.group(2)));
                update = new Update(20 + step * 62 / total, m.group(3).isBlank() ? null : m.group(3));
            }
        }
        if (update == null || update.percent() < current) return Optional.empty();
        return Optional.of(update);
    }
}
