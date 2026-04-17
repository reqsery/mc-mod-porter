package com.debugger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.nio.file.*;

/**
 * Reads debugger-config.json:
 * {"modPath": "C:\Users\...\groupchat", "package": "com.groupchat"}
 */
public record DebuggerConfig(String modPath, String pkg) {
    private static final Gson GSON = new GsonBuilder().create();

    public static DebuggerConfig load(String configFile) throws IOException {
        String json = Files.readString(Path.of(configFile));
        Raw raw = GSON.fromJson(json, Raw.class);
        return new DebuggerConfig(raw.modPath, raw.pkg != null ? raw.pkg : "");
    }

    public static DebuggerConfig fromArgs(String modPath, String pkg) {
        return new DebuggerConfig(modPath, pkg);
    }

    private static class Raw {
        String modPath;
        String pkg;
        String package_; // allow "package" key via alias
    }
}
