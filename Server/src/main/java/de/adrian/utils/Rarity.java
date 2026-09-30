package de.adrian.utils;

public enum Rarity {

    BASIC,
    ADVANCED,
    ELITE;


    public static Rarity parse(String s) {
        if (s == null) return null;
        for (var r : Rarity.values()) if (s.equalsIgnoreCase(r.name())) return r;
        return null;
    }

}
