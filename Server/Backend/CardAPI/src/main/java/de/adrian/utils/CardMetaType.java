// Modified by danlp6: clazz final (enum constants are global, nobody may reassign it)
package de.adrian.utils;

import de.adrian.interfaces.BattleCardMeta;
import de.adrian.interfaces.CardMeta;
import de.adrian.interfaces.EquipmentCardMeta;

public enum CardMetaType {

    DEFAULT(CardMeta.class),
    BATTLE(BattleCardMeta.class),
    EQUIPMENT(EquipmentCardMeta.class);


    CardMetaType(Class<? extends CardMeta> clazz) {
        this.clazz = clazz;
    }

    public final Class<? extends CardMeta> clazz;

}
