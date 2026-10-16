// Author: danlp6
package dlp6.danlp6;

import de.adrian.utils.Identifier;
import de.adrian.utils.Profession;
import de.adrian.utils.Rarity;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Builds asset paths for the Assets CDN (see API.md).
 * <p>
 * Every asset lives at {@code <type>/<group>/<name>.<ext>} (all lower-case). The backend only ever
 * sends these relative paths to clients; resolving them against CDN/mirror URLs and caching is the
 * client's job ({@code Client/.../AssetClient}). The layout is defined in
 * {@code Server/assets.defs.json}; keep {@link Type} and the group constants in sync with it.
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
    public static final String PLUGINS = "plugins";

    /** One path segment after lower-casing; same rule as {@code assets.py} and the client. */
    private static final Pattern SEGMENT = Pattern.compile("[a-z0-9][a-z0-9_.-]*");

    private AssetAdapter() {
    }

    /** @throws IllegalArgumentException if a group segment or the name isn't URL safe (see API.md) */
    public static String path(String group, String name, Type type) {
        for (String segment : group.split("/", -1)) check(segment);
        return type.folder() + "/" + group + "/" + check(name.toLowerCase(Locale.ROOT)) + "." + type.extension;
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
    /** {@code <type>/plugins/<plugin>/<name>.<ext>}, for assets a Game-Plugin ships in its jar. */
    public static String pluginPath(String plugin, String name, Type type) {
        return path(PLUGINS + "/" + plugin.toLowerCase(Locale.ROOT), name, type);
    }

    private static String check(String segment) {
        if (!SEGMENT.matcher(segment).matches())
            throw new IllegalArgumentException("asset path segment '" + segment + "' must match " + SEGMENT.pattern());
        return segment;
    }
}
