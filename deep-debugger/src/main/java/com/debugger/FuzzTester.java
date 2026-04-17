package com.debugger;

import java.util.*;

/**
 * Generates fuzz test cases for String parameters:
 * null, empty, very long, special chars, duplicates, unicode.
 */
public class FuzzTester {

    public static final List<String> STRING_FUZZ_VALUES = List.of(
        "null_sentinel",          // treated as null by test gen
        "",                       // empty
        " ",                      // whitespace
        "a".repeat(1000),         // very long
        "Hello World!",           // normal
        "group-with-hyphen",      // hyphens
        "group_with_underscore",  // underscores
        "GROUP",                  // uppercase
        "group123",               // alphanumeric
        "<script>alert(1)</script>", // XSS attempt
        "'; DROP TABLE groups; --",  // SQL injection
        "\0\1\2",                 // null bytes + control chars
        "🎮🎲🎯",                 // emoji / unicode
        "com.group/test:path",    // path-like
        "                 ",      // spaces only
        "duplicate",              // used twice to test duplicate handling
        "duplicate"               // the duplicate
    );

    /** Returns a list of test case descriptions with input/expected behavior. */
    public List<Map<String, Object>> generateFuzzCases(MethodInfo method) {
        List<Map<String, Object>> cases = new ArrayList<>();
        for (int i = 0; i < method.paramTypes().size(); i++) {
            String type = method.paramTypes().get(i);
            if (type.equals("String") || type.equals("java.lang.String")) {
                for (String value : STRING_FUZZ_VALUES) {
                    Map<String, Object> c = new LinkedHashMap<>();
                    c.put("method",     method.signature());
                    c.put("paramIndex", i);
                    c.put("input",      value.equals("null_sentinel") ? null : value);
                    c.put("category",   categorizeFuzz(value));
                    cases.add(c);
                }
            }
        }
        return cases;
    }

    private String categorizeFuzz(String v) {
        if (v.equals("null_sentinel")) return "NULL";
        if (v.isBlank())               return "BLANK";
        if (v.length() > 100)          return "TOO_LONG";
        if (v.contains("<script>"))    return "XSS";
        if (v.contains("DROP TABLE"))  return "SQL_INJECTION";
        if (v.chars().anyMatch(c -> c < 32 && c != '\n' && c != '\r' && c != '\t')) return "CONTROL_CHARS";
        return "NORMAL";
    }
}
