package de.adrian.cardparser;

import com.google.gson.JsonObject;
import de.adrian.utils.Identifier;
import de.adrian.utils.Rarity;
import de.adrian.SW;
import de.adrian.interfaces.CardMeta;

import java.util.*;

public class CardParser {

    private static final List<CardSupPartParser> parsers = List.of(new BattleCardParser());

    public static Map<Identifier,CardMeta> parse(JsonObject main, SW sw) {
        Map<Identifier,CardMeta> map = new HashMap<>();

        for (var key : main.keySet()) if (main.get(key).isJsonObject()) {
            var meta = parseTemplate(key,main.get(key).getAsJsonObject(),sw);
            if (meta == null) continue;

            map.put(meta.getIdentifier(),meta);
        }

        return map;
    }

    private static CardMeta parseTemplate(String key, JsonObject obj, SW sw) {
        try {

            String name = obj.get("name").getAsString();
            String description = obj.get("description").getAsString();
            Rarity rarity = Rarity.parse(obj.get("rarity").getAsString());


            if (name == null || description == null || rarity == null) throw new MissingFormatArgumentException("Core attribute(s) is/are missing");
            Identifier identifier = sw.identifier(key);
            var template = new CardTemplate(identifier,name,description,rarity,obj);

            return parsers.stream().map(p -> p.parse(template)).filter(Objects::nonNull).findFirst().orElse(template);
        } catch (Exception e) {
            System.out.println("Couldnt parse Card: " + key);
            e.printStackTrace();
            return null;
        }
    }
}
