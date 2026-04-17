package com.debugger;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Generates JUnit 5 test classes for each discovered class.
 * Tests are written to src/test/java/generated/ for compilation and running.
 */
public class TestGenerator {

    private final Path outputDir;

    public TestGenerator(Path outputDir) {
        this.outputDir = outputDir;
    }

    public List<Path> generate(List<MethodInfo> methods) throws IOException {
        // Group methods by class
        Map<String, List<MethodInfo>> byClass = new LinkedHashMap<>();
        for (MethodInfo m : methods) {
            byClass.computeIfAbsent(m.className(), k -> new ArrayList<>()).add(m);
        }

        List<Path> generated = new ArrayList<>();
        for (var entry : byClass.entrySet()) {
            Path p = generateClassTests(entry.getKey(), entry.getValue());
            if (p != null) generated.add(p);
        }
        return generated;
    }

    private Path generateClassTests(String className, List<MethodInfo> methods) throws IOException {
        String simpleName = className.contains(".")
            ? className.substring(className.lastIndexOf('.') + 1)
            : className;
        String pkg = className.contains(".")
            ? className.substring(0, className.lastIndexOf('.'))
            : "";

        String testClassName = simpleName + "AutoTest";
        Path dir = pkg.isEmpty()
            ? outputDir
            : outputDir.resolve(pkg.replace('.', '/'));
        Files.createDirectories(dir);

        Path file = dir.resolve(testClassName + ".java");
        StringBuilder sb = new StringBuilder();

        if (!pkg.isEmpty()) sb.append("package ").append(pkg).append(";\n\n");
        sb.append("import org.junit.jupiter.api.*;\n");
        sb.append("import org.junit.jupiter.params.ParameterizedTest;\n");
        sb.append("import org.junit.jupiter.params.provider.NullSource;\n");
        sb.append("import org.junit.jupiter.params.provider.ValueSource;\n");
        sb.append("import static org.junit.jupiter.api.Assertions.*;\n\n");
        sb.append("/**\n * Auto-generated tests for ").append(className).append("\n */\n");
        sb.append("@DisplayName(\"Auto-tests: ").append(simpleName).append("\")\n");
        sb.append("class ").append(testClassName).append(" {\n\n");

        for (MethodInfo m : methods) {
            generateMethodTests(sb, m, className, simpleName);
        }

        sb.append("}\n");
        Files.writeString(file, sb.toString());
        return file;
    }

    private void generateMethodTests(StringBuilder sb, MethodInfo m, String fqClass, String simpleName) {
        // Basic invocation test
        sb.append("    @Test\n");
        sb.append("    @DisplayName(\"").append(m.methodName()).append("() basic invocation\")\n");
        sb.append("    void test_").append(m.methodName()).append("_basic() {\n");
        sb.append("        assertDoesNotThrow(() -> {\n");
        if (m.isStatic()) {
            sb.append("            ").append(fqClass).append(".").append(m.methodName())
              .append("(").append(defaultArgs(m.paramTypes())).append(");\n");
        } else {
            sb.append("            // Instance method - requires construction; skip if no default ctor\n");
            sb.append("            // Instantiation test placeholder for ").append(simpleName).append("\n");
        }
        sb.append("        });\n    }\n\n");

        // Null / edge case tests for String params
        for (int i = 0; i < m.paramTypes().size(); i++) {
            String pt = m.paramTypes().get(i);
            if (pt.equals("String") || pt.equals("java.lang.String")) {
                sb.append("    @Test\n");
                sb.append("    @DisplayName(\"").append(m.methodName()).append("() - null string param ").append(i).append("\")\n");
                sb.append("    void test_").append(m.methodName()).append("_nullParam").append(i).append("() {\n");
                sb.append("        // Null string edge case\n");
                sb.append("        try {\n");
                if (m.isStatic()) {
                    sb.append("            ").append(fqClass).append(".").append(m.methodName())
                      .append("(").append(nullifiedArgs(m.paramTypes(), i)).append(");\n");
                } else {
                    sb.append("            // instance null test placeholder\n");
                }
                sb.append("        } catch (NullPointerException | IllegalArgumentException e) {\n");
                sb.append("            // Expected: null input rejected\n");
                sb.append("        }\n    }\n\n");

                // Empty string test
                sb.append("    @Test\n");
                sb.append("    @DisplayName(\"").append(m.methodName()).append("() - empty string param ").append(i).append("\")\n");
                sb.append("    void test_").append(m.methodName()).append("_emptyParam").append(i).append("() {\n");
                sb.append("        try {\n");
                if (m.isStatic()) {
                    sb.append("            ").append(fqClass).append(".").append(m.methodName())
                      .append("(").append(emptyStringArgs(m.paramTypes(), i)).append(");\n");
                } else {
                    sb.append("            // instance empty test placeholder\n");
                }
                sb.append("        } catch (IllegalArgumentException e) {\n");
                sb.append("            // Expected: empty input rejected\n");
                sb.append("        }\n    }\n\n");
            }
        }
    }

    /** Generate sensible default argument values for a parameter type list. */
    private String defaultArgs(List<String> types) {
        List<String> args = new ArrayList<>();
        for (String t : types) args.add(defaultValue(t));
        return String.join(", ", args);
    }

    private String nullifiedArgs(List<String> types, int nullIndex) {
        List<String> args = new ArrayList<>();
        for (int i = 0; i < types.size(); i++) {
            args.add(i == nullIndex ? "null" : defaultValue(types.get(i)));
        }
        return String.join(", ", args);
    }

    private String emptyStringArgs(List<String> types, int emptyIndex) {
        List<String> args = new ArrayList<>();
        for (int i = 0; i < types.size(); i++) {
            args.add(i == emptyIndex ? "\"\"" : defaultValue(types.get(i)));
        }
        return String.join(", ", args);
    }

    private String defaultValue(String type) {
        return switch (type) {
            case "String", "java.lang.String" -> "\"test\"";
            case "int", "Integer"             -> "0";
            case "long", "Long"               -> "0L";
            case "double", "Double"           -> "0.0";
            case "float", "Float"             -> "0.0f";
            case "boolean", "Boolean"         -> "false";
            case "char", "Character"          -> "'a'";
            case "byte", "Byte"               -> "(byte)0";
            case "short", "Short"             -> "(short)0";
            default                           -> "null";
        };
    }
}
