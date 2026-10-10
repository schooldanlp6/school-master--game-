package dlp6.danlp6;

import de.adrian.utils.Identifier;
import de.adrian.utils.Profession;
import de.adrian.utils.Rarity;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Resolves assets on the static assets server.
 * <p>
 * Every asset lives at {@code <type>/<group>/<name>.<ext>} (all lower-case) below one or more
 * base URLs (CDN, mirrors, local http.server). The layout is defined in
 * {@code data/cards/assets.defs.json}; keep {@link Type} and the group constants in sync with it.
 */
public final class AssetAdapter {

    public enum Type {
        PICTURE("png"),
        SOUND("mp3");

        public final String extension;

        Type(String extension) {
            this.extension = extension;
        }

        public String folder() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final String PROFESSIONS = "professions";
    public static final String RARITY = "rarity";
    public static final String CARDS = "cards";

    public static final String ENV_BASE_URLS = "ASSET_BASE_URLS";
    public static final String DEFAULT_BASE_URL = "http://localhost:8080/";

    private final List<URI> baseUrls;

    public AssetAdapter(String... baseUrls) {
        if (baseUrls.length == 0) throw new IllegalArgumentException("At least one asset base URL is required!");
        this.baseUrls = Arrays.stream(baseUrls).map(AssetAdapter::normalize).toList();
    }

    /** Base URLs from the comma separated {@code ASSET_BASE_URLS} env var, else localhost. */
    public static AssetAdapter fromEnv() {
        String env = System.getenv(ENV_BASE_URLS);
        if (env == null || env.isBlank()) return new AssetAdapter(DEFAULT_BASE_URL);
        return new AssetAdapter(Arrays.stream(env.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toArray(String[]::new));
    }

    // --- relative paths (identical on every mirror) ---

    public static String path(String group, String name, Type type) {
        return type.folder() + "/" + group + "/" + name.toLowerCase(Locale.ROOT) + "." + type.extension;
    }
    public static String path(Profession profession, Type type) {
        return path(PROFESSIONS, profession.name(), type);
    }
    public static String path(Rarity rarity, Type type) {
        return path(RARITY, rarity.name(), type);
    }
    public static String path(Identifier card, Type type) {
        return path(CARDS, card.value(), type);
    }

    // --- URLs ---

    /** Primary URL for an asset path. */
    public URI url(String path) {
        return baseUrls.getFirst().resolve(path);
    }
    /** All URLs for an asset path in mirror order, for fallback when a mirror is down. */
    public List<URI> urls(String path) {
        List<URI> out = new ArrayList<>(baseUrls.size());
        for (URI base : baseUrls) out.add(base.resolve(path));
        return out;
    }
    public URI url(Profession profession, Type type) {
        return url(path(profession, type));
    }
    public URI url(Rarity rarity, Type type) {
        return url(path(rarity, type));
    }
    public URI url(Identifier card, Type type) {
        return url(path(card, type));
    }
    /** The index (path -> size, sha256) written by {@code make asset-index}. */
    public URI indexUrl() {
        return url("index.json");
    }

    // --- local files (when the asset root is mounted next to the server) ---

    public static Path local(Path assetRoot, String path) {
        return assetRoot.resolve(path);
    }

    public List<URI> getBaseUrls() {
        return baseUrls;
    }

    private static URI normalize(String base) {
        return URI.create(base.endsWith("/") ? base : base + "/");
    }
}
