package com.debugger;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Reads JUnit XML test results and produces build/test-report.json.
 */
public class TestReporter {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public void generate(String projectDir) throws IOException {
        Path reportsDir = Path.of(projectDir, "build/test-results/test");
        List<Map<String, Object>> allTests = new ArrayList<>();
        int pass = 0, fail = 0, error = 0, skip = 0;

        if (Files.exists(reportsDir)) {
            try (var stream = Files.walk(reportsDir)) {
                List<Path> xmlFiles = stream.filter(p -> p.toString().endsWith(".xml")).toList();
                for (Path xml : xmlFiles) {
                    List<Map<String, Object>> tests = parseJUnitXml(xml);
                    for (var t : tests) {
                        allTests.add(t);
                        String status = (String) t.get("status");
                        if ("PASSED".equals(status)) pass++;
                        else if ("FAILED".equals(status)) fail++;
                        else if ("ERROR".equals(status)) error++;
                        else skip++;
                    }
                }
            }
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("generatedAt", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        report.put("totalTests", allTests.size());
        report.put("passed",     pass);
        report.put("failed",     fail);
        report.put("errors",     error);
        report.put("skipped",    skip);
        report.put("tests",      allTests);

        Path out = Path.of(projectDir, "build/test-report.json");
        Files.createDirectories(out.getParent());
        try (FileWriter w = new FileWriter(out.toFile())) {
            GSON.toJson(report, w);
        }
        System.out.println("Test report: " + out.toAbsolutePath());
        System.out.printf("Results: %d passed, %d failed, %d errors, %d skipped%n", pass, fail, error, skip);
    }

    private List<Map<String, Object>> parseJUnitXml(Path xml) {
        List<Map<String, Object>> tests = new ArrayList<>();
        try {
            String content = Files.readString(xml);
            // Simple regex-based parse (no external XML dep needed)
            java.util.regex.Pattern testcase = java.util.regex.Pattern.compile(
                "<testcase[^>]+name=\"([^\"]+)\"[^>]+classname=\"([^\"]+)\"([^/]*/?>)(.*?)(</testcase>|(?<=/>))",
                java.util.regex.Pattern.DOTALL
            );
            java.util.regex.Matcher m = testcase.matcher(content);
            while (m.find()) {
                String name      = m.group(1);
                String classname = m.group(2);
                String body      = m.group(4);

                Map<String, Object> t = new LinkedHashMap<>();
                t.put("class",  classname);
                t.put("method", name);

                if (body.contains("<failure")) {
                    t.put("status", "FAILED");
                    t.put("message", extractAttr(body, "message"));
                } else if (body.contains("<error")) {
                    t.put("status", "ERROR");
                    t.put("message", extractAttr(body, "message"));
                } else if (body.contains("<skipped")) {
                    t.put("status", "SKIPPED");
                } else {
                    t.put("status", "PASSED");
                }
                tests.add(t);
            }
        } catch (Exception e) {
            System.err.println("Error parsing " + xml + ": " + e.getMessage());
        }
        return tests;
    }

    private String extractAttr(String body, String attr) {
        var p = java.util.regex.Pattern.compile(attr + "=\"([^\"]*)\"");
        var m = p.matcher(body);
        return m.find() ? m.group(1) : "";
    }
}
