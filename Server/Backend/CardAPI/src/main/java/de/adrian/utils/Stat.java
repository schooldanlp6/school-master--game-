package de.adrian.utils;

public enum Stat {

    STRENGTH,
    MAX_WILL_POWER,
    SPEED,
    DEFENSE,
    TAUNT,
    MAX_HEALTH;

    public static Stat parse(String s) {
        if (s == null) return null;
        for (var r : Stat.values()) if (s.equalsIgnoreCase(r.name())) return r;
        return null;
    }

}
