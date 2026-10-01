package de.adrian.interfaces;


import de.adrian.utils.StatModifierValue;

import java.util.Set;

public interface EquipmentCardMeta extends CardMeta {

    Set<StatModifierValue> getModifiers();


}
