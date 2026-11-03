package de.adrian.game;

import com.google.gson.JsonElement;
import de.adrian.game.listeners.CancelableEvent;
import de.adrian.utils.Identifier;

public class C2SPacketEvent implements CancelableEvent {

    public final Identifier identifier;
    public final JsonElement data;
    private boolean canceled = false;

    public C2SPacketEvent(Identifier identifier, JsonElement data) {
        this.identifier = identifier;
        this.data = data;
    }



    @Override
    public boolean isCanceled() {
        return canceled;
    }
    @Override
    public void setCanceled(boolean b) {
        canceled = b;
    }
}
