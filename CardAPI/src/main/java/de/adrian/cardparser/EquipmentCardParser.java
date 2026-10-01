package de.adrian.cardparser;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.adrian.interfaces.CardMeta;
import de.adrian.utils.Stat;
import de.adrian.utils.StatModifierType;
import de.adrian.utils.StatModifierValue;

import java.util.HashSet;

import java.util.Set;

class EquipmentCardParser implements CardSupPartParser {
    @Override
    public CardMeta parse(CardTemplate template) {
        if (!template.getData().has("equipment") || !template.getData().get("equipment").isJsonArray()) return null;
        JsonArray array = template.getData().get("equipment").getAsJsonArray();

        Set<StatModifierValue> set = new HashSet<>();
        for (JsonElement element : array) if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            Stat stat = Stat.parse(object.get("stat").getAsString());
            StatModifierType type = StatModifierType.parse(object.get("modifier").getAsString());
            double amount = object.get("amount").getAsDouble();

            if (stat != null && type != null && amount != 0) set.add(new StatModifierValue(stat,amount,type));
        }

        return new EquipmentCardTemplate(template,set);
    }
}
