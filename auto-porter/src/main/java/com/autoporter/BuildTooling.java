package com.autoporter;

import java.util.*;

/**
 * Build tooling that goes with each Minecraft version: the Java toolchain, the Gradle wrapper used by the official
 * Fabric example mod and NeoForge MDK, and the ModDevGradle plugin version.
 *
 * <p>Verified on 2026-11-22 from the official templates (FabricMC/fabric-example-mod branches and
 * NeoForgeMDKs/MDK-&lt;version&gt;-ModDevGradle repositories). A {@code null} value means there is no official
 * template for that loader and version, so the porter leaves the project's own value alone.
 */
public final class BuildTooling {

    /**
     * @param javaVersion       Java version the game (and the mod toolchain) needs
     * @param fabricGradle      Gradle wrapper version of the official Fabric example mod
     * @param neoForgeGradle    Gradle wrapper version of the official NeoForge ModDevGradle MDK
     * @param modDevGradle      net.neoforged.moddev plugin version of the official MDK
     */
    public record Tooling(int javaVersion, String fabricGradle, String neoForgeGradle, String modDevGradle) {}

    private static final Map<String, Tooling> TOOLING = new LinkedHashMap<>();

    static {
        // Columns: mc, java, fabric gradle, neoforge gradle, moddevgradle
        put("1.16",   8,  "9.8.2", null,     null);
        put("1.16.1", 8,  "9.8.2", null,     null);
        put("1.16.2", 8,  "9.8.2", null,     null);
        put("1.16.3", 8,  "9.8.2", null,     null);
        put("1.16.4", 8,  "9.8.2", null,     null);
        put("1.16.5", 8,  "9.8.2", null,     null);
        put("1.17.1", 16, "9.8.2", null,     null);
        put("1.18",   17, "9.8.2", null,     null);
        put("1.18.1", 17, "9.8.2", null,     null);
        put("1.18.2", 17, "9.8.2", null,     null);
        put("1.19",   17, "9.8.2", null,     null);
        put("1.19.1", 17, "9.8.2", null,     null);
        put("1.19.2", 17, "9.8.2", null,     null);
        put("1.19.3", 17, "9.8.2", null,     null);
        put("1.19.4", 17, "9.8.2", null,     null);
        put("1.20",   17, "9.8.2", null,     null);
        put("1.20.1", 17, "9.8.2", null,     null);
        put("1.20.2", 17, "9.8.2", null,     null);
        put("1.20.3", 17, "9.8.2", null,     null);
        put("1.20.4", 17, "9.8.2", "9.2.1",  "2.0.148");
        put("1.20.5", 21, "9.8.2", "8.14.2", "2.0.92");
        put("1.20.6", 21, "9.8.2", "9.2.1",  "2.0.148");
        put("1.21",   21, "9.8.2", "9.2.1",  "2.0.148");
        put("1.21.1", 21, "9.8.2", "9.2.1",  "2.0.148");
        put("1.21.2", 21, "9.8.2", "9.2.1",  "2.0.148");
        put("1.21.3", 21, "9.8.2", "9.2.1",  "2.0.148");
        put("1.21.4", 21, "9.8.2", "9.2.1",  "2.0.148");
        put("1.21.5", 21, "9.8.2", "9.2.1",  "2.0.148");
        put("1.21.6", 21, "9.8.2", "9.2.1",  "2.0.148");
        put("1.21.7", 21, "9.8.2", "9.2.1",  "2.0.148");
        put("1.21.8", 21, "9.8.2", "9.2.1",  "2.0.148");
        put("1.21.9", 21, "9.8.2", "9.2.1",  "2.0.148");
        put("1.21.10",21, "9.8.2", "9.2.1",  "2.0.148");
        put("1.21.11",21, "9.8.2", "9.2.1",  "2.0.148");
        put("26.1",   25, "9.8.2", "9.2.1",  "2.0.148");
        put("26.1.1", 25, "9.8.2", "9.2.1",  "2.0.148");
        put("26.1.2", 25, "9.8.2", "9.2.1",  "2.0.148");
        put("26.2",   25, "9.8.2", "9.2.1",  "2.0.148");
        put("26.3",   25, "9.8.2", "9.2.1",  "2.0.148");
    }

    private static void put(String mc, int java, String fabricGradle, String neoGradle, String modDev) {
        TOOLING.put(mc, new Tooling(java, fabricGradle, neoGradle, modDev));
    }

    private BuildTooling() {}

    /** Tooling for a supported version, or null. */
    public static Tooling get(String mcVersion) { return TOOLING.get(VersionDatabase.normalize(mcVersion)); }

    /** Java version needed by a Minecraft version (21 when unknown). */
    public static int javaFor(String mcVersion) {
        Tooling t = get(mcVersion);
        return t == null ? 21 : t.javaVersion();
    }
}
