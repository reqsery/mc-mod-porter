package com.debugger;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.List;

/** Unit tests for the SourceScanner component. */
@DisplayName("SourceScanner Tests")
class SourceScannerTest {

    @Test
    @DisplayName("Scanner handles non-existent path gracefully")
    void testNonExistentPath() {
        SourceScanner scanner = new SourceScanner();
        assertDoesNotThrow(() -> {
            List<MethodInfo> methods = scanner.scan("/non/existent/path");
            // Should return empty, not throw
            assertNotNull(methods);
        });
    }

    @Test
    @DisplayName("Scanner returns empty list for empty directory")
    void testEmptyDirectory() throws Exception {
        Path tmpDir = Files.createTempDirectory("scanner-test");
        SourceScanner scanner = new SourceScanner();
        List<MethodInfo> methods = scanner.scan(tmpDir.toString());
        assertTrue(methods.isEmpty());
        tmpDir.toFile().delete();
    }

    @Test
    @DisplayName("FuzzTester generates cases for String params")
    void testFuzzTesterStringParams() {
        FuzzTester fuzzer = new FuzzTester();
        MethodInfo m = new MethodInfo(
            "com.test.Foo", "doSomething", "void",
            List.of("String", "int"), false, true
        );
        List<java.util.Map<String, Object>> cases = fuzzer.generateFuzzCases(m);
        assertFalse(cases.isEmpty(), "Should generate fuzz cases for String param");
        assertTrue(cases.size() >= FuzzTester.STRING_FUZZ_VALUES.size(),
            "Should have one case per fuzz value");
    }

    @Test
    @DisplayName("FuzzTester generates no cases for non-String params")
    void testFuzzTesterNoStringParams() {
        FuzzTester fuzzer = new FuzzTester();
        MethodInfo m = new MethodInfo(
            "com.test.Bar", "compute", "int",
            List.of("int", "boolean"), true, false
        );
        List<java.util.Map<String, Object>> cases = fuzzer.generateFuzzCases(m);
        assertTrue(cases.isEmpty(), "No fuzz cases for non-String params");
    }

    @Test
    @DisplayName("MethodInfo signature format is correct")
    void testMethodInfoSignature() {
        MethodInfo m = new MethodInfo(
            "com.groupchat.GroupManager", "createGroup", "void",
            List.of("String", "String", "String"), true, true
        );
        assertEquals(
            "com.groupchat.GroupManager.createGroup(String, String, String)",
            m.signature()
        );
    }
}
