package de.adrian.cardparser;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.adrian.utils.Profession;
import de.adrian.utils.Stat;
import de.adrian.interfaces.CardMeta;

import java.util.*;

class BattleCardParser implements CardSupPartParser {
    @Override
    public CardMeta parse(CardTemplate template) {
        if (!template.getData().has("battle") || !template.getData().get("battle").isJsonObject()) return null;
        JsonObject obj = template.getData().get("battle").getAsJsonObject();

        var stats = parseStats(obj);
        var professions = parseProfessions(obj);


        return  (stats != null && professions != null) ? new BattleCardTemplate(template,professions,stats) : null;
    }

    private Map<Stat,Integer> parseStats(JsonObject object) {
        if (!object.has("stats") || !object.get("stats").isJsonObject()) return null;

        JsonObject stats = object.get("stats").getAsJsonObject();
        Map<Stat,Integer> map = new EnumMap<>(Stat.class);


        for (var stat : Stat.values()) if (stats.has(stat.name())) map.put(stat,stats.get(stat.name()).getAsInt());

        return map;
    }
    private Set<Profession> parseProfessions(JsonObject object) {
        if (!object.has("professions") || !object.get("professions").isJsonArray()) return null;

        JsonArray array = object.get("professions").getAsJsonArray();
        Set<Profession> professions = new HashSet<>();


        for (JsonElement e : array) {
            Profession profession = Profession.parse(e.getAsString());
            if (profession != null) professions.add(profession);
        }

        return professions;
    }
}
