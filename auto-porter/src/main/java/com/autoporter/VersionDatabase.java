package com.autoporter;

import java.util.*;

/**
 * Embedded knowledge base of all supported Minecraft versions (1.16 - 26.1.x).
 * Versions verified against live Modrinth/Fabric APIs where possible.
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
        boolean hasFabric,
        boolean hasForge,
        boolean hasNeoForge
    ) {}

    private static final Map<String, VersionInfo> DB = new LinkedHashMap<>();

    static {
        // ── 1.16.x (Forge only, Fabric added mid-cycle) ──────────────────
        add("1.16",   "0.9.2+build.206",  "0.25.0+1.16",           null, "36.1.2",  "1.0.6",  "0.6-SNAPSHOT", true,true,false);
        add("1.16.1", "0.9.2+build.206",  "0.25.0+1.16",           null, "36.1.2",  "1.0.6",  "0.6-SNAPSHOT", true,true,false);
        add("1.16.2", "0.9.3+build.207",  "0.29.4+1.16",           null, "36.1.32", "1.0.6",  "0.6-SNAPSHOT", true,true,false);
        add("1.16.3", "0.10.8+build.218", "0.31.0+1.16",           null, "36.2.2",  "1.0.8",  "0.6-SNAPSHOT", true,true,false);
        add("1.16.4", "0.10.8+build.218", "0.33.2+1.16",           null, "36.2.2",  "1.0.12", "0.6-SNAPSHOT", true,true,false);
        add("1.16.5", "0.11.7+build.2",   "0.40.1+1.16",           null, "36.2.39", "1.0.15", "0.6-SNAPSHOT", true,true,false);
        // ── 1.17.x ───────────────────────────────────────────────────────
        add("1.17.1", "0.11.7+build.2",   "0.46.1+1.17",           null, "37.1.1",  "2.0.12", "0.7-SNAPSHOT", true,true,false);
        // ── 1.18.x ───────────────────────────────────────────────────────
        add("1.18",   "0.12.12+build.1",  "0.46.3+1.18",           null, "38.0.17", "2.0.20", "0.8-SNAPSHOT", true,true,false);
        add("1.18.1", "0.12.12+build.1",  "0.46.6+1.18",           null, "38.0.21", "2.1.49", "0.8-SNAPSHOT", true,true,false);
        add("1.18.2", "0.13.3+build.1",   "0.55.3+1.18.2",         null, "40.2.0",  "2.3.4",  "0.8-SNAPSHOT", true,true,false);
        // ── 1.19.x ───────────────────────────────────────────────────────
        add("1.19",   "0.14.9+build.1",   "0.56.0+1.19",           null, "41.0.9",  "3.3.4",  "0.10-SNAPSHOT",true,true,false);
        add("1.19.1", "0.14.9+build.1",   "0.58.5+1.19.1",         null, "41.1.0",  "3.3.4",  "0.10-SNAPSHOT",true,true,false);
        add("1.19.2", "0.14.21+build.1",  "0.58.5+1.19.2",         null, "43.2.14", "4.1.96", "0.10-SNAPSHOT",true,true,false);
        add("1.19.3", "0.14.21+build.1",  "0.67.1+1.19.3",         null, "44.1.0",  "4.9.0",  "0.11-SNAPSHOT",true,true,false);
        add("1.19.4", "0.15.11+build.1",  "0.83.0+1.19.4",         null, "45.1.0",  "6.5.2",  "0.12-SNAPSHOT",true,true,false);
        // ── 1.20.x ───────────────────────────────────────────────────────
        add("1.20",   "0.14.21+build.1",  "0.83.0+1.20",           null, "46.0.14", "7.1.4",  "1.2-SNAPSHOT", true,true,false);
        add("1.20.1", "0.15.11+build.1",  "0.92.7+1.20.1",         null, "47.2.30", "9.1.4",  "1.2-SNAPSHOT", true,true,false);
        add("1.20.2", "0.15.11+build.1",  "0.91.2+1.20.2",         "20.2.59-beta",  null,     "9.1.12", "1.3-SNAPSHOT", true,false,true);
        add("1.20.3", "0.15.11+build.1",  "0.91.0+1.20.3",         "20.3.8-beta",   null,     "10.1.16","1.3-SNAPSHOT", true,false,true);
        add("1.20.4", "0.15.11+build.1",  "0.97.0+1.20.4",         "20.4.80-beta",  null,     "11.1.2", "1.4-SNAPSHOT", true,false,true);
        add("1.20.5", "0.15.11+build.1",  "0.97.2+1.20.5",         "20.5.21-beta",  null,     "13.0.6", "1.6-SNAPSHOT", true,false,true);
        add("1.20.6", "0.15.11+build.1",  "0.100.8+1.20.6",        "20.6.99-beta",  null,     "13.0.6", "1.6-SNAPSHOT", true,false,true);
        // ── 1.21.x ───────────────────────────────────────────────────────
        add("1.21",   "0.15.11+build.1",  "0.100.8+1.21",          "21.0.168-beta", null,     "13.0.8", "1.7-SNAPSHOT", true,false,true);
        add("1.21.1", "0.18.6",           "0.116.9+1.21.1",        "21.1.172",      null,     "13.0.8", "1.7-SNAPSHOT", true,false,true);
        add("1.21.2", "0.18.6",           "0.110.0+1.21.2",        "21.2.38-beta",  null,     "14.0.4", "1.8-SNAPSHOT", true,false,true);
        add("1.21.3", "0.18.6",           "0.110.6+1.21.3",        "21.3.38-beta",  null,     "14.0.4", "1.8-SNAPSHOT", true,false,true);
        // ── Verified via Modrinth API ─────────────────────────────────────
        add("1.21.4", "0.18.6",           "0.119.4+1.21.4",        "21.4.136",      null,     "15.0.3", "1.8-SNAPSHOT", true,false,true);
        add("1.21.5", "0.18.6",           "0.120.0+1.21.5",        "21.5.35-beta",  null,     "14.1.0", "1.9-SNAPSHOT", true,false,true);
        add("1.21.6", "0.18.6",           "0.124.0+1.21.6",        "21.6.25-beta",  null,     "15.0.0", "1.9-SNAPSHOT", true,false,true);
        add("1.21.7", "0.18.6",           "0.128.0+1.21.7",        "21.7.15-beta",  null,     "15.0.4", "1.9-SNAPSHOT", true,false,true);
        add("1.21.8", "0.18.6",           "0.132.0+1.21.8",        "21.8.15-beta",  null,     "16.0.0", "1.9-SNAPSHOT", true,false,true);
        add("1.21.9", "0.18.6",           "0.136.0+1.21.9",        "21.9.15-beta",  null,     "17.0.0", "1.9-SNAPSHOT", true,false,true);
        // ── Current target ────────────────────────────────────────────────
        add("1.21.10","0.18.4",           "0.138.4+1.21.10",       "21.10.50-beta", null,     "18.0.6", "1.11-SNAPSHOT",true,false,true);
        add("1.21.11","0.18.6",           "0.140.0+1.21.11",       "21.11.0-beta",  null,     "18.0.8", "1.11-SNAPSHOT",true,false,true);
        // ── 26.x (calendar versioning, Java 25, fully unobfuscated) ─────────
        add("26.1",   "0.18.4",           "0.145.0+26.1",          "26.1.0.1-beta", null,     null,     "1.15",         true,false,true);
        add("26.1.1", "0.18.4",           "0.145.3+26.1.1",        null,            null,     null,     "1.15",         true,false,false);
        add("26.1.2", "0.18.4",           null,                    null,            null,     null,     "1.15",         true,false,false);
    }

    private static void add(String mc, String fl, String fa, String neo, String forge,
                            String arch, String loom,
                            boolean hasFabric, boolean hasForge, boolean hasNeoForge) {
        DB.put(mc, new VersionInfo(mc, fl, fa, neo, forge, arch, loom, hasFabric, hasForge, hasNeoForge));
    }

    public static VersionInfo get(String version)   { return DB.get(normalize(version)); }
    public static boolean supports(String version)  { return DB.containsKey(normalize(version)); }
    public static List<String> allVersions()        { return new ArrayList<>(DB.keySet()); }
    public static List<VersionInfo> allVersionInfos(){ return new ArrayList<>(DB.values()); }
    public static String normalize(String v)        { return v == null ? "" : v.trim(); }
}
