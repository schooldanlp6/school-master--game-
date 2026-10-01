package de.adrian.utils.interfaces;

import com.google.gson.JsonObject;
import de.adrian.utils.Identifier;
import de.adrian.utils.Rarity;

public interface CardMeta {

    Identifier getIdentifier();
    String getName();
    String getDescription();
    Rarity getRarity();

    JsonObject getData();

}
