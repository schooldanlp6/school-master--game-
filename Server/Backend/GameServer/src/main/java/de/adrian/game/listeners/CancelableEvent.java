package de.adrian.game.listeners;

public interface CancelableEvent extends Event {

    boolean isCanceled();
    void setCanceled(boolean b);

}
