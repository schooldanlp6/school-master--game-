package de.adrian;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.adrian.cardparser.CardParser;
import de.adrian.utils.Identifier;
import de.adrian.interfaces.CardMeta;

import java.util.HashMap;
import java.util.Map;

public class SW {

    private final Map<String, Identifier> identifierMap = new HashMap<>();
    private Map<Identifier, CardMeta> cardMetas;

    public Identifier identifier(String value) {
        return identifierMap.computeIfAbsent(value, Identifier::new);
    }
    public CardMeta getMeta(Identifier identifier) {
        return cardMetas.getOrDefault(identifier,null);
    }



    public SW(JsonElement cardMetas) {
        parseCardMetas(cardMetas);
    }
    private void parseCardMetas(JsonElement cardMetas) {
        System.out.println("Parsing CardMetas...");
        if (cardMetas == null || !cardMetas.isJsonObject() || cardMetas.getAsJsonObject().isEmpty()) throw new NullPointerException("CardMeta file is empty or corrupted!");
        this.cardMetas = CardParser.parse(cardMetas.getAsJsonObject(),this);
        System.out.println("Successfully parsed " + this.cardMetas.size() + " CardMetas!");
    }
}
