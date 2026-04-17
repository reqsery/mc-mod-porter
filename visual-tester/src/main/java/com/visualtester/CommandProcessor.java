package com.visualtester;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Polls test-commands.json every tick (relative to Minecraft run dir).
 * When the file changes, loads commands and executes them sequentially,
 * writing results to test-output/results.json when complete.
 *
 * Supported commands (JSON array or NDJSON):
 *   {"action":"screenshot","name":"my_shot"}
 *   {"action":"press_key","key":"H"}
 *   {"action":"click","x":400,"y":300}
 *   {"action":"type_text","text":"Hello!"}
 *   {"action":"read_screen","output":"test-output/screen_info.json"}
 *   {"action":"read_chat","output":"test-output/chat_log.json"}
 *   {"action":"wait","ms":1000}
 */
public class CommandProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger("CommandProcessor");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final File commandsFile = new File("test-commands.json");
    private final File outputDir    = new File("test-output");

    private List<JsonObject> queue   = new ArrayList<>();
    private int index                = 0;
    private boolean running          = false;
    private long waitUntil           = 0;
    private long lastModified        = 0;
    private final List<Map<String, Object>> results = new ArrayList<>();

    public CommandProcessor() { outputDir.mkdirs(); }

    public void tick(Minecraft client) {
        // Watch for new/changed commands file
        if (!running && commandsFile.exists()) {
            long mod = commandsFile.lastModified();
            if (mod != lastModified) { lastModified = mod; loadCommands(); }
        }

        if (!running) return;

        if (index >= queue.size()) {
            writeResults();
            running = false;
            LOGGER.info("Batch done. {}/{} commands executed.", results.size(), queue.size());
            return;
        }

        if (System.currentTimeMillis() < waitUntil) return;

        executeNext(client);
    }

    private void loadCommands() {
        queue.clear(); index = 0; results.clear(); waitUntil = 0;
        try {
            String raw = Files.readString(commandsFile.toPath()).trim();
            if (raw.startsWith("[")) {
                for (JsonElement el : JsonParser.parseString(raw).getAsJsonArray())
                    queue.add(el.getAsJsonObject());
            } else {
                for (String line : raw.split("\r?\n")) {
                    line = line.trim();
                    if (line.startsWith("{")) queue.add(JsonParser.parseString(line).getAsJsonObject());
                }
            }
            if (!queue.isEmpty()) { running = true; LOGGER.info("Loaded {} commands", queue.size()); }
        } catch (Exception e) { LOGGER.error("Load commands failed: {}", e.getMessage()); }
    }

    private void executeNext(Minecraft client) {
        JsonObject cmd = queue.get(index);
        String action  = optStr(cmd, "action", "unknown");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("index",  index);
        result.put("action", action);
        result.put("time",   new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()));

        try {
            switch (action) {
                case "screenshot" -> {
                    String name = optStr(cmd, "name", "shot_" + index);
                    File f = ScreenshotManager.takeNamedScreenshot(name);
                    result.put("status", "success"); result.put("file", f.getPath());
                }
                case "press_key" -> {
                    String key = cmd.get("key").getAsString();
                    InputSimulator.pressKey(key);
                    result.put("status", "success"); result.put("key", key);
                }
                case "click" -> {
                    int x = cmd.get("x").getAsInt(), y = cmd.get("y").getAsInt();
                    InputSimulator.mouseClick(x, y);
                    result.put("status", "success"); result.put("x", x); result.put("y", y);
                }
                case "type_text" -> {
                    String text = cmd.get("text").getAsString();
                    InputSimulator.typeText(text);
                    result.put("status", "success"); result.put("text", text);
                }
                case "read_screen" -> {
                    String out = optStr(cmd, "output", "test-output/screen_info.json");
                    Map<String, Object> info = ScreenReader.readScreen(client);
                    writeJson(new File(out), info);
                    result.put("status", "success"); result.put("output", out);
                    result.put("screenClass", info.get("screenSimpleName"));
                }
                case "read_chat" -> {
                    String out = optStr(cmd, "output", "test-output/chat_log.json");
                    List<String> msgs = ChatCapture.getMessages();
                    Map<String, Object> chatData = Map.of("count", msgs.size(), "messages", msgs);
                    writeJson(new File(out), chatData);
                    result.put("status", "success"); result.put("output", out);
                    result.put("messageCount", msgs.size());
                }
                case "wait" -> {
                    int ms = cmd.get("ms").getAsInt();
                    waitUntil = System.currentTimeMillis() + ms;
                    result.put("status", "success"); result.put("waitMs", ms);
                }
                default -> {
                    result.put("status", "error");
                    result.put("error", "Unknown action: " + action);
                }
            }
        } catch (Exception e) {
            result.put("status", "error");
            result.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
            LOGGER.error("Command '{}' error: {}", action, e.getMessage());
        }

        results.add(result);
        index++;
    }

    private void writeResults() {
        long ok = results.stream().filter(r -> "success".equals(r.get("status"))).count();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalCommands",    queue.size());
        summary.put("successCount",     ok);
        summary.put("errorCount",       results.size() - ok);
        summary.put("generatedAt",      new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        summary.put("results",          results);
        try { writeJson(new File("test-output/results.json"), summary); }
        catch (Exception e) { LOGGER.error("Write results failed: {}", e.getMessage()); }
    }

    private void writeJson(File f, Object data) throws IOException {
        f.getParentFile().mkdirs();
        try (FileWriter w = new FileWriter(f)) { GSON.toJson(data, w); }
    }

    private static String optStr(JsonObject o, String k, String def) {
        return o.has(k) ? o.get(k).getAsString() : def;
    }
}
