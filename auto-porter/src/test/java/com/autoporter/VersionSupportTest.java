package com.autoporter;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class VersionSupportTest {
    private static final Path TEMPLATES = Path.of("templates");

    @Test void recognizesAndOrders262() {
        assertTrue(VersionDatabase.supports("26.2"));
        List<String> versions = VersionDatabase.allVersions();
        assertEquals(versions.indexOf("26.1.2") + 1, versions.indexOf("26.2"));
    }

    @Test void resolvesTemplateFor262() throws Exception {
        TemplateManager manager = new TemplateManager(TEMPLATES);
        assertTrue(manager.hasTemplate("26.2"));
        assertEquals("26.2", manager.loadTemplateInfo("26.2").get("mcVersion"));
    }

    @Test void adjacentAndChainedMigrationPathsReach262() {
        List<String> versions = VersionDatabase.allVersions();
        assertEquals(List.of("26.1.2", "26.2"), path(versions, "26.1.2", "26.2"));
        assertEquals(List.of("26.1", "26.1.1", "26.1.2", "26.2"), path(versions, "26.1", "26.2"));
    }

    @Test void databaseAndTemplatesAreConsistent() throws Exception {
        Gson gson = new Gson();
        for (String version : List.of("26.1.2", "26.2")) {
            var info = VersionDatabase.get(version);
            Path dir = TEMPLATES.resolve(info.mcVersion());
            assertTrue(Files.exists(dir.resolve("gradle.properties")), info.mcVersion());
            Map<?, ?> json = gson.fromJson(Files.readString(dir.resolve("template-info.json")), Map.class);
            Properties props = new Properties();
            try (var reader = Files.newBufferedReader(dir.resolve("gradle.properties"))) { props.load(reader); }
            assertEquals(info.mcVersion(), json.get("mcVersion"), info.mcVersion());
            assertEquals(info.mcVersion(), props.getProperty("minecraft_version"), info.mcVersion());
            assertEquals(info.fabricLoaderVersion(), json.get("fabricLoader"), info.mcVersion());
            assertEquals(info.fabricLoaderVersion(), props.getProperty("fabric_loader_version"), info.mcVersion());
            assertEquals(info.fabricApiVersion(), json.get("fabricApi"), info.mcVersion());
            assertEquals(info.fabricApiVersion(), props.getProperty("fabric_api_version"), info.mcVersion());
            assertEquals(info.loomVersion(), json.get("loom"), info.mcVersion());
            assertEquals(info.loomVersion(), props.getProperty("loom_version"), info.mcVersion());
        }
    }

    private static List<String> path(List<String> versions, String from, String to) {
        int start = versions.indexOf(from), end = versions.indexOf(to);
        assertTrue(start >= 0 && end >= start);
        return versions.subList(start, end + 1);
    }
}
