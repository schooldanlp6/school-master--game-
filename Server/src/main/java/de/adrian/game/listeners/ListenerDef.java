package de.adrian.game.listeners;

import java.lang.reflect.Method;

record ListenerDef(Listener listener, Method method,boolean ignoreCanceled) {
}
