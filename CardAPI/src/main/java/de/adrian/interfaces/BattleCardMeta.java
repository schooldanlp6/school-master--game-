package de.adrian.interfaces;


import de.adrian.utils.Profession;
import de.adrian.utils.Stat;

import java.util.Set;

public interface BattleCardMeta extends CardMeta {

    Integer getStatValue(Stat stat);
    Set<Profession> getProfessions();

}
