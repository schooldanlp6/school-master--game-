package de.adrian.utils.interfaces;


import de.adrian.utils.Stat;

public interface BattleCardMeta extends CardMeta {

    Integer getStatValue(Stat stat);

}
