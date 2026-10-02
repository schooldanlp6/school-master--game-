package de.adrian.cardparser;

import com.google.gson.JsonObject;
import de.adrian.utils.Identifier;
import de.adrian.utils.Rarity;
import de.adrian.interfaces.CardMeta;

record CardTemplate(Identifier getIdentifier, String getName, String getDescription, Rarity getRarity, JsonObject getData) implements CardMeta {

    @Override
    public String toString() {
        return getIdentifier.value();
    }

}
