package de.adrian.utils.cardparser;

import com.google.gson.JsonObject;
import de.adrian.utils.Identifier;
import de.adrian.utils.Rarity;
import de.adrian.utils.SW;
import de.adrian.utils.interfaces.CardMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CardParser {

    private static final List<CardSupPartParser> parsers = List.of(new BattleCardParser());

    public static Map<Identifier,CardMeta> parse(JsonObject main) {
        Map<Identifier,CardMeta> map = new HashMap<>();

        for (var key : main.keySet()) if (main.get(key).isJsonObject()) {
            var meta = parseTemplate(key,main.get(key).getAsJsonObject());
            if (meta == null) continue;

            map.put(meta.getIdentifier(),meta);
        }

        return map;
    }

    private static CardMeta parseTemplate(String key, JsonObject obj) {
        try {

            String name = obj.get("name").getAsString();
            String description = obj.get("description").getAsString();
            Rarity rarity = Rarity.parse(obj.get("rarity").getAsString());


            if (name == null || description == null || rarity == null) return null;
            Identifier identifier = SW.identifier(key);
            var template = new CardTemplate(identifier,name,description,rarity,obj);

            return parsers.stream().map(p -> p.parse(template)).filter(Objects::nonNull).findFirst().orElse(template);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
