package com.autoporter;

import java.util.*;

/**
 * Embedded knowledge base of all supported Minecraft versions (1.16 - 26.3).
 *
 * <p>Values were verified on 2026-11-22 against:
 * <ul>
 *   <li>Fabric: the official FabricMC/fabric-example-mod branch for each version (loader, Fabric API, Loom) and
 *       meta.fabricmc.net (Yarn builds)</li>
 *   <li>NeoForge: the official NeoForgeMDKs/MDK-&lt;version&gt;-ModDevGradle templates and
 *       maven.neoforged.net metadata (latest build per Minecraft version)</li>
 *   <li>Forge: maven.minecraftforge.net metadata (latest build per Minecraft version; none exists for 1.16)</li>
 *   <li>Architectury API: the architectury-api branch for each version and maven.architectury.dev (null when no
 *       release could be matched to that exact version)</li>
 * </ul>
 * 26.x has no Yarn mappings (the game is unobfuscated). Build-tool versions (Gradle, ModDevGradle, Java) live in
 * {@link BuildTooling}.
 */
public class VersionDatabase {

    public record VersionInfo(
        String mcVersion,
        String fabricLoaderVersion,
        String fabricApiVersion,
        String neoforgeVersion,
        String forgeVersion,
        String architecturyVersion,
        String loomVersion,
        /** Yarn mappings build string (e.g. "1.21.4+build.8"), or null if not available. */
        String yarnMappings,
        boolean hasFabric,
        boolean hasForge,
        boolean hasNeoForge
    ) {}

    private static final Map<String, VersionInfo> DB = new LinkedHashMap<>();

    static {
        // Columns: mc, fabric-loader, fabric-api, neoforge, forge, architectury, loom, yarn, hasFabric, hasForge, hasNeoForge
        add("1.16",   "0.20.1", "0.42.0+1.16",     null,             null,      null,      "1.19-SNAPSHOT", "1.16+build.4",    true, false, false);
        add("1.16.1", "0.20.1", "0.42.0+1.16",     null,             "32.0.108",null,      "1.19-SNAPSHOT", "1.16.1+build.21", true, true , false);
        add("1.16.2", "0.20.1", "0.42.0+1.16",     null,             "33.0.61", null,      "1.19-SNAPSHOT", "1.16.2+build.47", true, true , false);
        add("1.16.3", "0.20.1", "0.42.0+1.16",     null,             "34.1.42", null,      "1.19-SNAPSHOT", "1.16.3+build.47", true, true , false);
        add("1.16.4", "0.20.1", "0.42.0+1.16",     null,             "35.1.37", null,      "1.19-SNAPSHOT", "1.16.4+build.9",  true, true , false);
        add("1.16.5", "0.20.1", "0.42.0+1.16",     null,             "36.2.42", "1.32.68", "1.19-SNAPSHOT", "1.16.5+build.10", true, true , false);
        add("1.17.1", "0.20.1", "0.46.1+1.17",     null,             "37.1.1",  "2.10.12", "1.19-SNAPSHOT", "1.17.1+build.65", true, true , false);
        add("1.18",   "0.20.1", "0.44.0+1.18",     null,             "38.0.17", "3.9.66",  "1.19-SNAPSHOT", "1.18+build.1",    true, true , false);
        add("1.18.1", "0.20.1", "0.46.6+1.18",     null,             "39.1.2",  "3.9.66",  "1.19-SNAPSHOT", "1.18.1+build.22", true, true , false);
        add("1.18.2", "0.20.1", "0.77.0+1.18.2",   null,             "40.3.12", "4.12.94", "1.19-SNAPSHOT", "1.18.2+build.4",  true, true , false);
        add("1.19",   "0.20.1", "0.58.0+1.19",     null,             "41.1.0",  "5.14.84", "1.19-SNAPSHOT", "1.19+build.4",    true, true , false);
        add("1.19.1", "0.20.1", "0.58.5+1.19.1",   null,             "42.0.9",  "5.14.84", "1.19-SNAPSHOT", "1.19.1+build.6",  true, true , false);
        add("1.19.2", "0.20.1", "0.77.0+1.19.2",   null,             "43.5.2",  "6.6.92",  "1.19-SNAPSHOT", "1.19.2+build.28", true, true , false);
        add("1.19.3", "0.20.1", "0.76.1+1.19.3",   null,             "44.1.23", "7.1.86",  "1.19-SNAPSHOT", "1.19.3+build.5",  true, true , false);
        add("1.19.4", "0.20.1", "0.87.2+1.19.4",   null,             "45.4.5",  "8.2.91",  "1.19-SNAPSHOT", "1.19.4+build.2",  true, true , false);
        add("1.20",   "0.20.1", "0.83.0+1.20",     null,             "46.0.14", "9.2.14",  "1.19-SNAPSHOT", "1.20+build.1",    true, true , false);
        add("1.20.1", "0.20.1", "0.92.12+1.20.1",  null,             "47.4.29", "9.2.14",  "1.19-SNAPSHOT", "1.20.1+build.10", true, true , false);
        add("1.20.2", "0.20.1", "0.91.6+1.20.2",   "20.2.93",        null,      "10.1.20", "1.19-SNAPSHOT", "1.20.2+build.4",  true, false, true);
        add("1.20.3", "0.20.1", "0.91.1+1.20.3",   "20.3.8-beta",    null,      null,      "1.19-SNAPSHOT", "1.20.3+build.1",  true, false, true);
        add("1.20.4", "0.20.1", "0.97.3+1.20.4",   "20.4.253",       null,      "11.1.17", "1.19-SNAPSHOT", "1.20.4+build.3",  true, false, true);
        add("1.20.5", "0.20.1", "0.97.8+1.20.5",   "20.5.21-beta",   null,      "12.0.29", "1.19-SNAPSHOT", "1.20.5+build.1",  true, false, true);
        add("1.20.6", "0.20.1", "0.100.8+1.20.6",  "20.6.144",       null,      "12.1.4",  "1.19-SNAPSHOT", "1.20.6+build.3",  true, false, true);
        add("1.21",   "0.20.1", "0.102.0+1.21",    "21.0.167",       null,      "13.0.12", "1.19-SNAPSHOT", "1.21+build.9",    true, false, true);
        add("1.21.1", "0.20.1", "0.116.18+1.21.1", "21.1.267",       null,      "13.0.12", "1.19-SNAPSHOT", "1.21.1+build.3",  true, false, true);
        add("1.21.2", "0.20.1", "0.106.1+1.21.2",  "21.2.1-beta",    null,      "14.0.4",  "1.19-SNAPSHOT", "1.21.2+build.1",  true, false, true);
        add("1.21.3", "0.20.1", "0.114.1+1.21.3",  "21.3.100",       null,      "14.0.4",  "1.19-SNAPSHOT", "1.21.3+build.2",  true, false, true);
        add("1.21.4", "0.20.1", "0.119.4+1.21.4",  "21.4.161",       null,      "15.0.3",  "1.19-SNAPSHOT", "1.21.4+build.8",  true, false, true);
        add("1.21.5", "0.20.1", "0.128.2+1.21.5",  "21.5.101",       null,      "16.1.4",  "1.19-SNAPSHOT", "1.21.5+build.1",  true, false, true);
        add("1.21.6", "0.20.1", "0.128.2+1.21.6",  "21.6.20-beta",   null,      "17.0.8",  "1.19-SNAPSHOT", "1.21.6+build.1",  true, false, true);
        add("1.21.7", "0.20.1", "0.129.0+1.21.7",  "21.7.25-beta",   null,      "17.0.8",  "1.19-SNAPSHOT", "1.21.7+build.8",  true, false, true);
        add("1.21.8", "0.20.1", "0.136.1+1.21.8",  "21.8.57",        null,      "17.0.8",  "1.19-SNAPSHOT", "1.21.8+build.1",  true, false, true);
        add("1.21.9", "0.20.1", "0.134.1+1.21.9",  "21.9.16-beta",   null,      "18.0.8",  "1.19-SNAPSHOT", "1.21.9+build.1",  true, false, true);
        add("1.21.10","0.20.1", "0.138.4+1.21.10", "21.10.64",       null,      "18.0.8",  "1.19-SNAPSHOT", "1.21.10+build.3", true, false, true);
        add("1.21.11","0.20.1", "0.141.6+1.21.11", "21.11.48",       null,      "19.0.1",  "1.19-SNAPSHOT", "1.21.11+build.6", true, false, true);
        add("26.1",   "0.20.1", "0.145.1+26.1",    "26.1.0.19-beta", null,      null,      "1.19-SNAPSHOT", null,              true, false, true);
        add("26.1.1", "0.20.1", "0.145.4+26.1.1",  "26.1.1.15-beta", null,      null,      "1.19-SNAPSHOT", null,              true, false, true);
        add("26.1.2", "0.20.1", "0.155.3+26.1.2",  "26.1.2.129",     null,      "20.1.18", "1.19-SNAPSHOT", null,              true, false, true);
        add("26.2",   "0.20.1", "0.161.0+26.2",    "26.2.0.95",      null,      "21.1.13", "1.19-SNAPSHOT", null,              true, false, true);
        add("26.3",   "0.20.1", "0.166.2+26.3",    "26.3.0.115",     null,      "22.0.6",  "1.19-SNAPSHOT", null,              true, false, true);
    }

    private static void add(String mc, String fl, String fa, String neo, String forge,
                            String arch, String loom, String yarn,
                            boolean hasFabric, boolean hasForge, boolean hasNeoForge) {
        DB.put(mc, new VersionInfo(mc, fl, fa, neo, forge, arch, loom, yarn, hasFabric, hasForge, hasNeoForge));
    }

    public static VersionInfo get(String version)    { return DB.get(normalize(version)); }
    public static boolean supports(String version)   { return DB.containsKey(normalize(version)); }
    public static List<String> allVersions()         { return new ArrayList<>(DB.keySet()); }
    public static List<VersionInfo> allVersionInfos(){ return new ArrayList<>(DB.values()); }
    public static String normalize(String v)         { return v == null ? "" : v.trim(); }

    /** Position of a version in the supported list, or -1. */
    public static int indexOf(String version)        { return allVersions().indexOf(normalize(version)); }

    /** True for the unobfuscated calendar-versioned releases (26.1 and later). */
    public static boolean isUnobfuscated(String version) { return normalize(version).startsWith("26."); }
}
