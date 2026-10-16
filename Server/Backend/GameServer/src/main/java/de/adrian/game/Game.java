// Modified by danlp6: EventManager before plugins, onEnable/onDisable lifecycle, stop(), getGame()
package de.adrian.game;

import de.adrian.game.listeners.EventManager;
import de.adrian.game.setup.PluginDef;
import de.adrian.game.setup.PluginDefsInitiator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class Game {

    private static final Map<UUID,Game> games = new ConcurrentHashMap<>();


    public static void startGame(Consumer<Game> consumer) {
        UUID uuid = UUID.randomUUID();

        Thread.startVirtualThread(() -> {
            Game game = new Game(uuid);
            // published only after all onEnable() calls, the plugin map is never changed afterwards
            if (!game.stopped.get()) games.put(uuid,game);
            if (consumer != null) consumer.accept(game);
        });

    }
    public static Game getGame(UUID uuid) {
        return games.get(uuid);
    }
    private static Map<Class<? extends GamePlugin>,GamePlugin> createPlugins(Game game) {
        Set<PluginDef> defs = PluginDefsInitiator.getDefs();
        Map<Class<? extends GamePlugin>,GamePlugin> plugins = new LinkedHashMap<>();
        for (var def : defs) {
            try {
                GamePlugin plugin = def.main().getDeclaredConstructor().newInstance();
                plugin.init(game, def);
                plugins.put(def.main(),plugin);
            }catch (Exception | LinkageError ex) {
                System.out.println("Couldnt create " + def.name());
                ex.printStackTrace();
            }
        }

        return plugins;
    }


    //============= INSTANCE ================

    private final UUID uuid;
    private final EventManager eventManager;
    private final Map<Class<? extends GamePlugin>,GamePlugin> plugins;
    private final AtomicBoolean stopped = new AtomicBoolean();

    private Game(UUID uuid) {
        this.uuid = uuid;
        // EventManager first: plugins register their listeners in onEnable()
        eventManager = new EventManager();
        plugins = createPlugins(this);
        enablePlugins();
    }

    // All plugins exist before the first onEnable(), so getPlugin() works inside onEnable().
    private void enablePlugins() {
        System.out.println("Starting Game-Plugins...");
        var it = plugins.values().iterator();
        while (it.hasNext()) {
            GamePlugin plugin = it.next();
            try {
                plugin.onEnable();
                System.out.println(plugin.getDef().name() + " has been started!");
            } catch (Exception | LinkageError ex) {
                System.out.println("Couldnt start " + plugin.getDef().name());
                ex.printStackTrace();
                it.remove();
            }
        }
    }

    public void stop() {
        if (stopped.getAndSet(true)) return;
        games.remove(uuid);
        for (GamePlugin plugin : plugins.values()) {
            try {
                plugin.onDisable();
            } catch (Exception | LinkageError ex) {
                System.out.println("Couldnt stop " + plugin.getDef().name());
                ex.printStackTrace();
            }
        }
    }

    public UUID getUuid() {
        return uuid;
    }
    protected GamePlugin getPlugin(Class<? extends GamePlugin> main) {
        return plugins.getOrDefault(main,null);
    }
    protected EventManager getEventManager() {
        return eventManager;
    }


}
