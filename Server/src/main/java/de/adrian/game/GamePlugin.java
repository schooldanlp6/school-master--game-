package de.adrian.game;

import de.adrian.game.listeners.Event;
import de.adrian.game.listeners.Listener;
import de.adrian.game.setup.PluginDef;

public abstract class GamePlugin {

    private Game game;
    private PluginDef def;

    protected void init(Game game, PluginDef def) {
        this.game = game;
        this.def  = def;
    }

    public <T extends GamePlugin> T getPlugin(Class<T> clazz) {
        GamePlugin plugin = game.getPlugin(clazz);
        return clazz.isInstance(plugin) ? clazz.cast(plugin) : null;
    }
    public PluginDef getDef() {
        return def;
    }

    void registerEvents(Listener listener) {
        game.getEventManager().registerListener(listener);
    }
    void unregisterEvents(Listener listener) {
        game.getEventManager().unregisterListener(listener);
    }
    void callEvent(Event event) {
        game.getEventManager().callEvent(event);
    }

    protected abstract void onEnable();
    protected abstract void onDisable();

}
