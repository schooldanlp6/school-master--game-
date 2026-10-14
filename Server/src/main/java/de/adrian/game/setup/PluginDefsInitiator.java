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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class PluginDefsInitiator {

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

        for (File file : files) {

            try (JarFile jarFile = new JarFile(file)) {
                PluginDef def = parseDef(jarFile,file);
                if (def != null) defs.add(def);
            } catch (Exception ex) {}


        }

        if (!defs.isEmpty()) System.out.println("Found " + defs.size() + " Game-Plugin files!");
    }
    private static PluginDef parseDef(JarFile jar, File file) throws Exception {
        JarEntry entry = jar.getJarEntry("plugin.json");
        if (entry == null) return null;

        JsonElement element = loadJson(jar.getInputStream(entry));
        if (element == null || !element.isJsonObject()) return null;


        JsonObject object = element.getAsJsonObject();
        String main = object.get("main").getAsString();
        String version = object.get("version").getAsString();
        String name = object.get("name").getAsString();
        var mainClass = loadMainClass(file,main);

        return mainClass != null ? new PluginDef(file,mainClass,version,name) : null;
    }
    private static JsonElement loadJson(InputStream stream) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            return element;
        }
    }


    //Todo evtl code verändern! KI ist manchmal nd so clean
    private static Class<? extends GamePlugin> loadMainClass(File file, String main) throws Exception {
        URL url = file.toURI().toURL();
        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{url}, PluginDefsInitiator.class.getClassLoader())) {
            Class<?> loadedClass = classLoader.loadClass(main);


            if (GamePlugin.class.isAssignableFrom(loadedClass)) {
                @SuppressWarnings("unchecked")
                Class<? extends GamePlugin> mainClass = (Class<? extends GamePlugin>) loadedClass;
                return mainClass;
            }


        }
        return null;
    }
}
