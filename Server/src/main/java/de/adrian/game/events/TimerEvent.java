package de.adrian.game.events;

import de.adrian.game.listeners.Event;

public class TimerEvent implements Event {

    public int timeLeft;

    public TimerEvent(int timeLeft) {
        this.timeLeft = timeLeft;
    }

}
