// Modified by danlp6: protected plugin API (usable from other packages), getDef(), assetPath()
package de.adrian.game;

import de.adrian.game.listeners.Event;
import de.adrian.game.listeners.Listener;
import de.adrian.game.setup.PluginDef;
import dlp6.danlp6.AssetAdapter;

/**
 * Base class of every Game-Plugin. One instance is created per {@link Game}.
 * Plugins live in other packages (their own jars), so everything they need is protected/public.
 */
public abstract class GamePlugin {

    private Game game;
    private PluginDef def;

    final void init(Game game, PluginDef def) {
        this.game = game;
        this.def = def;
    }

    public Game getGame() {
        return game;
    }
    public PluginDef getDef() {
        return def;
    }

    public <T extends GamePlugin> T getPlugin(Class<T> clazz) {
        GamePlugin plugin = game.getPlugin(clazz);
        return clazz.isInstance(plugin) ? clazz.cast(plugin) : null;
    }

    protected void registerEvents(Listener listener) {
        game.getEventManager().registerListener(listener);
    }
    protected void unregisterEvents(Listener listener) {
        game.getEventManager().unregisterListener(listener);
    }
    protected void callEvent(Event event) {
        game.getEventManager().callEvent(event);
    }

    /** Asset path of a file this plugin ships in its jar under {@code assets/<type>/<name>.<ext>}. */
    public String assetPath(String name, AssetAdapter.Type type) {
        return AssetAdapter.pluginPath(def.name(), name, type);
    }

    protected abstract void onEnable();
    protected abstract void onDisable();

}
