// Modified by danlp6: fixed Event type check, super type dispatch, private handlers, thread safety; merged with Server branch (ignoreCanceled); deterministic handler order
package de.adrian.game.listeners;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventManager {
    // one list in registration order: with CancelableEvent the handler order decides who sees a cancel, so it must not depend on hashing.
    // copy-on-write: plugins may (un)register or fire events from other threads, and listeners may (un)register while an event runs
    private final List<ListenerDef> listeners = new CopyOnWriteArrayList<>();

    /** Handlers run in registration order; inside one listener in method name order. */
    public void registerListener(Listener listener) {
        List<ListenerDef> defs = new ArrayList<>();
        Method[] methods = listener.getClass().getDeclaredMethods();
        Arrays.sort(methods, Comparator.comparing(Method::getName).thenComparing(Method::toString));
        for (var method : methods) if (method.isAnnotationPresent(EventHandler.class) && method.getParameterCount() == 1) {
            var type = method.getParameterTypes()[0];
            if (!Event.class.isAssignableFrom(type)) {
                System.out.println("Ignoring @EventHandler " + method + ": parameter is not an Event");
                continue;
            }

            method.setAccessible(true);
            boolean ignoreCanceled = method.getAnnotation(EventHandler.class).ignoreCanceled();
            defs.add(new ListenerDef(listener, method, type.asSubclass(Event.class), ignoreCanceled));
        }
        listeners.addAll(defs);
    }
    public void unregisterListener(Listener listener) {
        listeners.removeIf(def -> def.listener() == listener);
    }

    /** Calls every handler whose parameter type the event is an instance of (so super types / interfaces work too). */
    public void callEvent(Event event) {
        for (ListenerDef regListener : listeners) {
            if (!regListener.eventType().isInstance(event)) continue;
            if (event instanceof CancelableEvent e && e.isCanceled() && regListener.ignoreCanceled()) continue;
            try {
                regListener.method().invoke(regListener.listener(), event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
