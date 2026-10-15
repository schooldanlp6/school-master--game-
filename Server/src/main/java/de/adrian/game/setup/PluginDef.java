// Modified by danlp6: keeps the plugin class loader open
package de.adrian.game.setup;

import de.adrian.game.GamePlugin;

import java.io.File;
import java.net.URLClassLoader;

/**
 * A plugin jar found in {@code data/plugins}. The class loader stays open for the whole server
 * lifetime, otherwise the plugin's other classes (listeners, helpers) can't be loaded later.
 */
public record PluginDef(File file, Class<? extends GamePlugin> main, String version, String name, URLClassLoader classLoader) {
}
