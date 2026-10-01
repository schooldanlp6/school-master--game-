package de.adrian.utils.cardparser;

import com.google.gson.JsonObject;
import de.adrian.utils.Identifier;
import de.adrian.utils.Rarity;
import de.adrian.utils.interfaces.CardMeta;

record CardTemplate(Identifier getIdentifier, String getName, String getDescription, Rarity getRarity, JsonObject getData) implements CardMeta {
}
