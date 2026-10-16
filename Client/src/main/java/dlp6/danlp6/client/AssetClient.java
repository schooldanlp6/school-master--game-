// Author: danlp6
package dlp6.danlp6.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Fetches assets from the Assets CDN (or any mirror of it) and caches them on disk.
 * <p>
 * The client is stateless: the backend only sends asset <em>paths</em> ({@code picture/rarity/elite.png}),
 * this class turns them into local files. The cache is disposable, deleting it only costs a re-download.
 * <ul>
 *     <li>{@link #refreshIndex()} loads {@code index.json} (path, size, sha256) from the first mirror that
 *     answers, else the last cached copy (offline).</li>
 *     <li>{@link #get(String)} returns the cached file if it matches the index, otherwise downloads it,
 *     trying every mirror in order and verifying size + sha256.</li>
 *     <li>A missing asset is not an error: {@link #get(String)} returns {@link Optional#empty()} and the
 *     caller shows a placeholder.</li>
 * </ul>
 * See API.md for the CDN contract.
 */
public final class AssetClient {

    public static final String ENV_BASE_URLS = "ASSET_BASE_URLS";
    public static final String ENV_CACHE_DIR = "ASSET_CACHE_DIR";
    public static final String DEFAULT_BASE_URL = "http://localhost:8080/";
    public static final String INDEX = "index.json";

    /** Relative asset path: at least two segments, each [a-z0-9][a-z0-9_.-]* (no "..", no "/" at the start). */
    private static final Pattern PATH = Pattern.compile("[a-z0-9][a-z0-9_.-]*(/[a-z0-9][a-z0-9_.-]*)+");
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");

    /** One index.json entry. */
    public record Entry(long size, String sha256) {
    }

    private final List<URI> baseUrls;
    private final Path cacheDir;
    private final Path files;
    private final HttpClient http;
    private final Duration requestTimeout = Duration.ofSeconds(30);

    /** null until an index was loaded (from a mirror or the cache); while null every path is tried on the mirrors. */
    private volatile Map<String, Entry> index;
    /** Paths whose cached file was already hashed and matched this sha256, so it's not re-hashed on every get. */
    private final Map<String, String> verified = new ConcurrentHashMap<>();

    /**
     * @param baseUrls CDN first, then mirrors; http(s) only
     * @param cacheDir folder for the cache, created if missing
     */
    public AssetClient(List<String> baseUrls, Path cacheDir) {
        if (baseUrls.isEmpty()) throw new IllegalArgumentException("At least one asset base URL is required!");
        this.baseUrls = baseUrls.stream().map(AssetClient::normalize).toList();
        this.cacheDir = cacheDir;
        this.files = cacheDir.resolve("files");
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Base URLs from the comma separated {@code ASSET_BASE_URLS} env var (default localhost:8080), cache in
     * {@code ASSET_CACHE_DIR} (default {@code $XDG_CACHE_HOME/school-master/assets} or {@code ~/.cache/...}).
     */
    public static AssetClient fromEnv() {
        String env = System.getenv(ENV_BASE_URLS);
        List<String> urls = env == null ? List.of() : Arrays.stream(env.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        return new AssetClient(urls.isEmpty() ? List.of(DEFAULT_BASE_URL) : urls, defaultCacheDir());
    }

    private static Path defaultCacheDir() {
        String dir = System.getenv(ENV_CACHE_DIR);
        if (dir != null && !dir.isBlank()) return Path.of(dir);
        String xdg = System.getenv("XDG_CACHE_HOME");
        Path base = xdg != null && !xdg.isBlank() ? Path.of(xdg) : Path.of(System.getProperty("user.home"), ".cache");
        return base.resolve("school-master").resolve("assets");
    }

    // --- index ---

    /**
     * Loads index.json from the first mirror that returns a valid one and caches it. If no mirror answers,
     * falls back to the cached index (if any).
     *
     * @return true if a fresh index came from a mirror, false if the cached one (or none) is used
     */
    public boolean refreshIndex() {
        for (URI base : baseUrls) {
            byte[] body = fetch(base.resolve(INDEX), true);
            if (body == null) continue;
            Map<String, Entry> parsed = parseIndex(body);
            if (parsed == null) {
                System.err.println("Ignoring invalid " + INDEX + " from " + base);
                continue;
            }
            index = parsed;
            try {
                writeAtomically(cacheDir.resolve(INDEX), body);
            } catch (IOException ex) {
                System.err.println("Couldnt cache " + INDEX + ": " + ex);
            }
            return true;
        }
        Path cached = cacheDir.resolve(INDEX);
        if (index == null && Files.isRegularFile(cached)) {
            try {
                index = parseIndex(Files.readAllBytes(cached));
            } catch (IOException ex) {
                System.err.println("Couldnt read cached " + INDEX + ": " + ex);
            }
        }
        return false;
    }

    /** The current index (path -> size, sha256), empty if none was loaded. */
    public Map<String, Entry> getIndex() {
        Map<String, Entry> current = index;
        return current == null ? Map.of() : current;
    }

    // --- assets ---

    /**
     * Local file for an asset path, downloading it if it isn't cached or doesn't match the index.
     * If every mirror fails, an older cached copy is returned rather than nothing.
     *
     * @return the file, or empty if the asset isn't available (show a placeholder)
     * @throws IllegalArgumentException if the path doesn't follow the asset path rule (see API.md)
     */
    public Optional<Path> get(String path) {
        if (!isValidPath(path)) throw new IllegalArgumentException("Invalid asset path: " + path);
        Path file = files.resolve(path);
        Map<String, Entry> current = index;
        Entry entry = current == null ? null : current.get(path);

        if (Files.isRegularFile(file) && (entry == null || matches(path, file, entry))) return Optional.of(file);
        // a loaded index is authoritative: not listed means the CDN doesn't have it
        if (current != null && entry == null) return Optional.empty();

        for (URI base : baseUrls) {
            // ?v=<sha256> busts CDN caches when an asset changes; static servers ignore the query
            byte[] body = fetch(base.resolve(entry == null ? path : path + "?v=" + entry.sha256()), false);
            if (body == null) continue;
            if (entry != null && (body.length != entry.size() || !entry.sha256().equals(sha256(body)))) {
                System.err.println("Ignoring " + path + " from " + base + ": size/sha256 doesn't match " + INDEX);
                continue;
            }
            try {
                writeAtomically(file, body);
            } catch (IOException ex) {
                System.err.println("Couldnt cache " + path + ": " + ex);
                return Optional.empty();
            }
            if (entry != null) verified.put(path, entry.sha256());
            return Optional.of(file);
        }
        return Files.isRegularFile(file) ? Optional.of(file) : Optional.empty();
    }

    /**
     * Downloads everything in the index that isn't cached yet (e.g. on a loading screen).
     *
     * @return how many of the indexed assets are available locally afterwards
     */
    public int prefetchAll() {
        int available = 0;
        for (String path : getIndex().keySet()) if (get(path).isPresent()) available++;
        return available;
    }

    public static boolean isValidPath(String path) {
        return path != null && PATH.matcher(path).matches();
    }

    public List<URI> getBaseUrls() {
        return baseUrls;
    }
    public Path getCacheDir() {
        return cacheDir;
    }

    // --- internals ---

    private boolean matches(String path, Path file, Entry entry) {
        if (entry.sha256().equals(verified.get(path))) return true;
        try {
            if (Files.size(file) != entry.size()) return false;
            if (!entry.sha256().equals(sha256(Files.readAllBytes(file)))) return false;
        } catch (IOException ex) {
            return false;
        }
        verified.put(path, entry.sha256());
        return true;
    }

    /** Body of a 200 response, or null on any error / other status. */
    private byte[] fetch(URI uri, boolean revalidate) {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri).timeout(requestTimeout).GET();
        if (revalidate) request.header("Cache-Control", "no-cache");
        try {
            HttpResponse<byte[]> response = http.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
            return response.statusCode() == 200 ? response.body() : null;
        } catch (IOException | IllegalArgumentException ex) {
            return null;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /** null if the document isn't {"assets": {...}}; entries with an invalid path or values are skipped. */
    static Map<String, Entry> parseIndex(byte[] body) {
        try {
            JsonElement root = JsonParser.parseString(new String(body, StandardCharsets.UTF_8));
            if (!root.isJsonObject() || !root.getAsJsonObject().has("assets") || !root.getAsJsonObject().get("assets").isJsonObject()) return null;
            Map<String, Entry> out = new HashMap<>();
            for (var e : root.getAsJsonObject().getAsJsonObject("assets").entrySet()) {
                if (!isValidPath(e.getKey()) || !e.getValue().isJsonObject()) continue;
                JsonObject o = e.getValue().getAsJsonObject();
                JsonElement size = o.get("size"), sha = o.get("sha256");
                if (size == null || !size.isJsonPrimitive() || !size.getAsJsonPrimitive().isNumber()) continue;
                if (sha == null || !sha.isJsonPrimitive() || !sha.getAsJsonPrimitive().isString()) continue;
                long bytes = size.getAsLong();
                if (bytes < 0 || !SHA256.matcher(sha.getAsString()).matches()) continue;
                out.put(e.getKey(), new Entry(bytes, sha.getAsString()));
            }
            return Map.copyOf(out);
        } catch (RuntimeException ex) { // JsonParseException, NumberFormatException, ...
            return null;
        }
    }

    /** Temp file + rename, so readers never see a half written file. */
    private static void writeAtomically(Path target, byte[] data) throws IOException {
        Files.createDirectories(target.getParent());
        Path tmp = Files.createTempFile(target.getParent(), ".download-", ".tmp");
        try {
            Files.write(tmp, data);
            try {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    private static String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is always available", ex);
        }
    }

    private static URI normalize(String base) {
        URI uri = URI.create(base.endsWith("/") ? base : base + "/");
        if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme()))
            throw new IllegalArgumentException("Asset base URL must be http(s): " + base);
        return uri;
    }
}
