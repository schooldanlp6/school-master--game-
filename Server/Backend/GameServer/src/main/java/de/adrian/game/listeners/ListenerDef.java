// Modified by danlp6: eventType, so EventManager can keep one ordered list
package de.adrian.game.listeners;

import java.lang.reflect.Method;

record ListenerDef(Listener listener, Method method, Class<? extends Event> eventType, boolean ignoreCanceled) {
}
