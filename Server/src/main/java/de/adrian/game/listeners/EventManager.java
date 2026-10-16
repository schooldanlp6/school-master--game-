package de.adrian.game.listeners;

import java.util.*;

public class EventManager {
    private final Map<Class<? extends Event>, List<ListenerDef>> listeners = new HashMap<>();

    public void registerListener(Listener listener) {
        for (var method : listener.getClass().getDeclaredMethods()) if (method.isAnnotationPresent(EventHandler.class) && method.getParameterCount() == 1) {
            var type = method.getParameterTypes()[0];
            if (!type.isAssignableFrom(Event.class)) continue;

            @SuppressWarnings("unchecked")
            Class<? extends Event> eventClass = (Class<? extends Event>) type;

            EventHandler annotation = method.getAnnotation(EventHandler.class);
            boolean ignoreCanceled = annotation.ignoreCanceled();

            ListenerDef regListener = new ListenerDef(listener, method,ignoreCanceled);
            listeners.computeIfAbsent(eventClass, k -> new ArrayList<>()).add(regListener);
        }
    }
    public void unregisterListener(Listener listener) {
        listeners.values().forEach(list -> list.removeIf(def -> def.listener() == listener));
        listeners.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    public void callEvent(Event event) {
        List<ListenerDef> eventListeners = listeners.get(event.getClass());
        if (eventListeners == null) return;

        for (ListenerDef regListener : eventListeners) {
            if (event instanceof CancelableEvent e && e.isCanceled() && regListener .ignoreCanceled()) continue;
            try {
                regListener.method().invoke(regListener.listener(), event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
