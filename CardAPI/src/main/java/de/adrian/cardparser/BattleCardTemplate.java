package de.adrian.cardparser;

import com.google.gson.JsonObject;
import de.adrian.utils.Identifier;
import de.adrian.utils.Profession;
import de.adrian.utils.Rarity;
import de.adrian.utils.Stat;
import de.adrian.interfaces.BattleCardMeta;

import java.util.Map;
import java.util.Set;

public record BattleCardTemplate(CardTemplate template, Set<Profession> getProfessions, Map<Stat,Integer> stats) implements BattleCardMeta {

    @Override
    public Integer getStatValue(Stat stat) {
        if (stat == null) return 0;
        return stats.getOrDefault(stat,0);
    }

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
