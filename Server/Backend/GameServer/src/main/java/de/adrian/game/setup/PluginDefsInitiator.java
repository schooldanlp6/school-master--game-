// Modified by danlp6: class loader stays open, errors logged, plugin.json validated
package de.adrian.game.setup;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.adrian.App;
import de.adrian.game.GamePlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

public class PluginDefsInitiator {

    /** Plugin names end up in asset paths (picture/plugins/&lt;name&gt;/...), so keep them URL safe (and no "." / ".."). */
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_.-]*");

    private static final File folder = new File(App.folder, "plugins");
    private static final Set<PluginDef> defs = new CopyOnWriteArraySet<>();

    public static void init() {
        if (!folder.exists()) folder.mkdir();
        loadPluginDefs();
    }
    public static Set<PluginDef> getDefs() {
        return defs;
    }

    private static void loadPluginDefs() {
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".jar"));
        if (files == null) return;
        Arrays.sort(files); // deterministic: on duplicate names the first jar wins (same as assets.py)

        for (File file : files) {
            try (JarFile jarFile = new JarFile(file)) {
                PluginDef def = parseDef(jarFile, file);
                if (def != null) defs.add(def);
            } catch (Exception | LinkageError ex) {
                // LinkageError: e.g. plugin built for a newer Java or missing a dependency
                System.out.println("Couldnt load Game-Plugin " + file.getName() + ": " + ex);
            }
        }

        if (!defs.isEmpty()) System.out.println("Found " + defs.size() + " Game-Plugin files!");
    }
    private static PluginDef parseDef(JarFile jar, File file) throws Exception {
        JarEntry entry = jar.getJarEntry("plugin.json");
        if (entry == null) {
            System.out.println("Skipping " + file.getName() + ": no plugin.json");
            return null;
        }

        JsonElement element = loadJson(jar.getInputStream(entry));
        if (element == null || !element.isJsonObject()) throw new IllegalArgumentException("plugin.json is not a JSON object");

        JsonObject object = element.getAsJsonObject();
        String main = required(object, "main");
        String version = required(object, "version");
        String name = required(object, "name");
        if (!NAME.matcher(name).matches()) throw new IllegalArgumentException("plugin name '" + name + "' must start with A-Z a-z 0-9 and may only contain A-Z a-z 0-9 _ . -");
        if (defs.stream().anyMatch(d -> d.name().equalsIgnoreCase(name))) throw new IllegalArgumentException("a plugin named '" + name + "' is already loaded");

        URLClassLoader classLoader = new URLClassLoader(new URL[]{file.toURI().toURL()}, PluginDefsInitiator.class.getClassLoader());
        try {
            return new PluginDef(file, loadMainClass(classLoader, main), version, name, classLoader);
        } catch (Exception | LinkageError ex) {
            classLoader.close();
            throw ex;
        }
    }
    private static String required(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())
            throw new IllegalArgumentException("plugin.json is missing \"" + key + "\" (must be a string)");
        return value.getAsString();
    }
    private static JsonElement loadJson(InputStream stream) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader);
        }
    }

    private static Class<? extends GamePlugin> loadMainClass(ClassLoader classLoader, String main) throws ClassNotFoundException {
        Class<?> loadedClass = classLoader.loadClass(main);
        if (!GamePlugin.class.isAssignableFrom(loadedClass)) throw new IllegalArgumentException(main + " does not extend GamePlugin");
        return loadedClass.asSubclass(GamePlugin.class);
    }
}
