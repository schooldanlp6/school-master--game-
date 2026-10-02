package de.adrian.cardparser;

import com.google.gson.JsonObject;
import de.adrian.interfaces.BattleCardMeta;
import de.adrian.interfaces.EquipmentCardMeta;
import de.adrian.utils.Identifier;
import de.adrian.utils.Rarity;
import de.adrian.utils.Stat;
import de.adrian.utils.StatModifierValue;

import java.util.Map;
import java.util.Set;

record EquipmentCardTemplate(CardTemplate template, Set<StatModifierValue> getModifiers) implements EquipmentCardMeta {

    @Override
    public Identifier getIdentifier() {
        return template.getIdentifier();
    }

    @Override
    public String getName() {
        return template.getName();
    }

    @Override
    public String getDescription() {
        return template.getDescription();
    }

    @Override
    public Rarity getRarity() {
        return template.getRarity();
    }

    @Override
    public JsonObject getData() {
        return template.getData();
    }

    @Override
    public String toString() {
        return template.toString();
    }
}
