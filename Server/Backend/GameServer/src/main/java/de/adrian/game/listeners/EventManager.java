// Modified by danlp6: fixed Event type check, super type dispatch, private handlers, thread safety; merged with Server branch (ignoreCanceled)
package de.adrian.game.listeners;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventManager {
    // concurrent: plugins may (un)register or fire events from other threads, and listeners may (un)register while an event runs
    private final Map<Class<? extends Event>, List<ListenerDef>> listeners = new ConcurrentHashMap<>();

    public void registerListener(Listener listener) {
        for (var method : listener.getClass().getDeclaredMethods()) if (method.isAnnotationPresent(EventHandler.class) && method.getParameterCount() == 1) {
            var type = method.getParameterTypes()[0];
            if (!Event.class.isAssignableFrom(type)) {
                System.out.println("Ignoring @EventHandler " + method + ": parameter is not an Event");
                continue;
            }

            method.setAccessible(true);
            boolean ignoreCanceled = method.getAnnotation(EventHandler.class).ignoreCanceled();
            ListenerDef regListener = new ListenerDef(listener, method, ignoreCanceled);
            listeners.computeIfAbsent(type.asSubclass(Event.class), k -> new CopyOnWriteArrayList<>()).add(regListener);
        }
    }
    public void unregisterListener(Listener listener) {
        // empty lists stay: removing them could race with a concurrent registerListener and lose that listener
        listeners.values().forEach(list -> list.removeIf(def -> def.listener() == listener));
    }

    /** Calls every handler whose parameter type the event is an instance of (so super types / interfaces work too). */
    public void callEvent(Event event) {
        for (var entry : listeners.entrySet()) {
            if (!entry.getKey().isInstance(event)) continue;
            for (ListenerDef regListener : entry.getValue()) {
                if (event instanceof CancelableEvent e && e.isCanceled() && regListener.ignoreCanceled()) continue;
                try {
                    regListener.method().invoke(regListener.listener(), event);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
