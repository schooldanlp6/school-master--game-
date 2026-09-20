package de.adrian.utils.interfaces;

import com.google.gson.JsonObject;
import de.adrian.utils.Identifier;
import de.adrian.utils.Stat;

public interface BattleCardMeta extends CardMeta {

    Integer getStatValue(Stat stat);

}
