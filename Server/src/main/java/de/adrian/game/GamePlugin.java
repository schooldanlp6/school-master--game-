package de.adrian.game;

import de.adrian.game.listeners.Event;
import de.adrian.game.listeners.Listener;

public abstract class GamePlugin {

    private Game game;

    protected void init(Game game) {
        this.game = game;
    }

    public <T extends GamePlugin> T getPlugin(Class<T> clazz) {
        GamePlugin plugin = game.getPlugin(clazz);
        return clazz.isInstance(plugin) ? clazz.cast(plugin) : null;
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

    abstract void onEnable();
    abstract void onDisable();

}
