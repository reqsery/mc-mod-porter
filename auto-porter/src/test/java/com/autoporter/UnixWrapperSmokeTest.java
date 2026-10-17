package com.autoporter;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

@EnabledOnOs({OS.LINUX, OS.MAC})
class UnixWrapperSmokeTest {
    @TempDir Path temp;

    @Test void copiedNonExecutableWrapperRunsBuildAndClientTasks() throws Exception {
        Path source = Files.createDirectories(temp.resolve("project with spaces"));
        Files.writeString(source.resolve("build.gradle"), "java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }");
        Files.writeString(source.resolve("gradle.properties"), "minecraft_version=1.21.10\n");
        Path wrapper = source.resolve("gradlew");
        Files.writeString(wrapper, "#!/bin/sh\nprintf 'task=%s\\n' \"$1\"\n");
        Files.setPosixFilePermissions(wrapper, EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        Files.writeString(source.resolve("gradlew.bat"), "Windows wrapper unchanged");

        AutoPorterMain.portMod(source.toString(), "1.21.10", "1.21.11", false, false);
        Path output = AutoPorterMain.outputDirectoryFor(source, "1.21.11");
        Path copiedWrapper = output.resolve("gradlew");
        assertTrue(Files.isExecutable(copiedWrapper));
        assertFalse(Files.isExecutable(wrapper), "The source permissions must remain unchanged");
        assertEquals("Windows wrapper unchanged", Files.readString(output.resolve("gradlew.bat")));

        // CLI Build must also recover a wrapper whose executable bit was removed later.
        Files.setPosixFilePermissions(copiedWrapper, EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        var build = new BuildRunner().build(output);
        assertTrue(build.success(), build.errors());
        assertTrue(build.fullOutput().contains("task=compileJava"));

        // GUI Build/Run Client use this same preparation immediately before ProcessBuilder.start().
        for (String task : List.of("build", "runClient")) {
            Files.setPosixFilePermissions(copiedWrapper, EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
            BuildRunner.ensureUnixGradleWrapperExecutable(output);
            Process process = new ProcessBuilder(copiedWrapper.toString(), task, "--no-daemon").start();
            String text = new String(process.getInputStream().readAllBytes());
            assertEquals(0, process.waitFor());
            assertTrue(text.contains("task=" + task));
        }
    }
}
