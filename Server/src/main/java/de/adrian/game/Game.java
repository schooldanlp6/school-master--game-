package de.adrian.game;

import de.adrian.game.listeners.EventManager;
import de.adrian.game.setup.PluginDef;
import de.adrian.game.setup.PluginDefsInitiator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class Game {

    private static Map<UUID,Game> games = new ConcurrentHashMap<>();


    public static void startGame(Consumer<Game> consumer) {
        UUID uuid = UUID.randomUUID();

        Thread.startVirtualThread(() -> {
            Game game = new Game(uuid);
            games.put(uuid,game);
            if (consumer != null) consumer.accept(game);
        });

    }
    private static Map<Class<? extends GamePlugin>,GamePlugin> startPlugins(Game game) {
        Set<PluginDef> defs = PluginDefsInitiator.getDefs();
        Map<Class<? extends GamePlugin>,GamePlugin> plugins = new HashMap<>();
        System.out.println("Starting Game-Plugins...");
        for (var def : defs) {
            try {
                GamePlugin plugin = def.main().getDeclaredConstructor().newInstance();
                plugin.init(game);
                plugins.put(def.main(),plugin);
                System.out.println(def.name() + " has been started!");
            }catch (Exception ex) {
                System.out.println("Couldnt start " + def.name());
                ex.printStackTrace();
            }
        }

        return plugins;
    }


    //============= INSTANCE ================

    private final UUID uuid;
    private final Map<Class<? extends GamePlugin>,GamePlugin> plugins;
    private final EventManager eventManager;

    private Game(UUID uuid) {
        this.uuid = uuid;
        plugins = startPlugins(this);
        eventManager = new EventManager();
    }

    protected GamePlugin getPlugin(Class<? extends GamePlugin> main) {
        return plugins.getOrDefault(main,null);
    }
    protected EventManager getEventManager() {
        return eventManager;
    }


}
