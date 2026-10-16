package de.adrian.utils;

public enum Profession {

    SPORT,
    WISSENSCHAFTEN,
    GESELLSCHAFTS_LEHRE,
    KUNST,
    STAFF;

    public static Profession parse(String s) {
        if (s == null) return null;
        for (var r : Profession.values()) if (s.equalsIgnoreCase(r.name())) return r;
        return null;
    }

}
