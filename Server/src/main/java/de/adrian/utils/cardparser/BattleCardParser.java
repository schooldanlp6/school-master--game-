package de.adrian.utils.cardparser;

import com.google.gson.JsonObject;
import de.adrian.utils.Stat;
import de.adrian.utils.interfaces.CardMeta;

import java.util.EnumMap;
import java.util.Map;

public class BattleCardParser implements CardSupPartParser {
    @Override
    public CardMeta parse(CardTemplate template) {
        if (!template.getData().has("stats") || !template.getData().get("stats").isJsonObject()) return null;
        JsonObject obj = template.getData().get("stats").getAsJsonObject();


        Map<Stat,Integer> map = new EnumMap<>(Stat.class);
        for (var stat : Stat.values()) if (obj.has(stat.name())) map.put(stat,obj.get(stat.name()).getAsInt());

        return new BattleCardTemplate(template,map);
    }
}
