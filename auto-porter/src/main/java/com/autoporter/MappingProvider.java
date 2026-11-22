package com.autoporter;

import com.google.gson.*;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.zip.*;

/**
 * Downloads and caches the mapping files needed for Yarn → Mojang source remapping:
 * <ul>
 *   <li>Mojang's official mappings ({@code client.txt}, {@code server.txt}) from the Minecraft version manifest</li>
 *   <li>Yarn {@code mergedv2} (official / intermediary / named) from maven.fabricmc.net</li>
 * </ul>
 * Files are cached in {@code ~/.mc-mod-porter/mappings/} (override with {@code -Dmcmodporter.cache=<dir>}) and only
 * downloaded once.
 */
public final class MappingProvider {
    static final String MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    static final String FABRIC_MAVEN = "https://maven.fabricmc.net/net/fabricmc/yarn/";

    private final Path cacheDir;
    private final HttpClient http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(20)).followRedirects(HttpClient.Redirect.NORMAL).build();

    public MappingProvider() {
        this(Path.of(System.getProperty("mcmodporter.cache",
            Path.of(System.getProperty("user.home"), ".mc-mod-porter", "mappings").toString())));
    }

    public MappingProvider(Path cacheDir) { this.cacheDir = cacheDir; }

    public Path cacheDir() { return cacheDir; }

    /** Yarn → Mojang table for a Minecraft version and Yarn build (e.g. "1.21.1+build.3"). */
    public MappingTable yarnToMojang(String mcVersion, String yarnBuild) throws IOException {
        Path yarn = yarnTiny(yarnBuild);
        List<Path> mojang = mojangMappings(mcVersion);
        List<Reader> readers = new ArrayList<>();
        try (Reader tiny = Files.newBufferedReader(yarn, StandardCharsets.UTF_8)) {
            for (Path p : mojang) readers.add(Files.newBufferedReader(p, StandardCharsets.UTF_8));
            return MappingTable.join(tiny, readers);
        } finally {
            for (Reader r : readers) r.close();
        }
    }

    /** Mojang client.txt (+ server.txt when published) for a version, downloading them if needed. */
    public List<Path> mojangMappings(String mcVersion) throws IOException {
        Path dir = cacheDir.resolve("mojang").resolve(mcVersion);
        Path client = dir.resolve("client.txt");
        Path server = dir.resolve("server.txt");
        if (!Files.isRegularFile(client)) {
            JsonObject manifest = JsonParser.parseString(getText(MANIFEST)).getAsJsonObject();
            String versionUrl = null;
            for (JsonElement v : manifest.getAsJsonArray("versions")) {
                if (v.getAsJsonObject().get("id").getAsString().equals(mcVersion)) {
                    versionUrl = v.getAsJsonObject().get("url").getAsString();
                }
            }
            if (versionUrl == null) throw new IOException("Minecraft " + mcVersion + " is not in Mojang's version manifest");
            JsonObject downloads = JsonParser.parseString(getText(versionUrl)).getAsJsonObject().getAsJsonObject("downloads");
            if (downloads == null || !downloads.has("client_mappings")) {
                throw new IOException("Mojang publishes no official mappings for " + mcVersion);
            }
            Files.createDirectories(dir);
            download(downloads.getAsJsonObject("client_mappings").get("url").getAsString(), client);
            if (downloads.has("server_mappings")) {
                download(downloads.getAsJsonObject("server_mappings").get("url").getAsString(), server);
            }
        }
        List<Path> files = new ArrayList<>(List.of(client));
        if (Files.isRegularFile(server)) files.add(server);
        return files;
    }

    /** The mappings.tiny of yarn-&lt;build&gt;-mergedv2.jar, downloading it if needed. */
    public Path yarnTiny(String yarnBuild) throws IOException {
        Path tiny = cacheDir.resolve("yarn").resolve(yarnBuild.replace('+', '_') + "-mergedv2.tiny");
        if (Files.isRegularFile(tiny)) return tiny;
        String url = FABRIC_MAVEN + yarnBuild + "/yarn-" + yarnBuild + "-mergedv2.jar";
        Path jar = Files.createTempFile("yarn-", ".jar");
        try {
            download(url, jar);
            try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(jar))) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    if (entry.getName().equals("mappings/mappings.tiny")) {
                        Files.createDirectories(tiny.getParent());
                        Path part = tiny.resolveSibling(tiny.getFileName() + ".part");
                        Files.copy(zip, part, StandardCopyOption.REPLACE_EXISTING);
                        Files.move(part, tiny, StandardCopyOption.REPLACE_EXISTING);
                        return tiny;
                    }
                }
            }
            throw new IOException("mappings/mappings.tiny not found in " + url);
        } finally {
            Files.deleteIfExists(jar);
        }
    }

    private String getText(String url) throws IOException {
        return new String(fetch(url), StandardCharsets.UTF_8);
    }

    private void download(String url, Path target) throws IOException {
        Path part = target.resolveSibling(target.getFileName() + ".part");
        Files.write(part, fetch(url));
        Files.move(part, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private byte[] fetch(String url) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofMinutes(3))
            .header("User-Agent", "mc-mod-porter").GET().build();
        try {
            HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) throw new IOException("HTTP " + response.statusCode() + " for " + url);
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while downloading " + url, e);
        }
    }
}
