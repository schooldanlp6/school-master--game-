package de.adrian.utils;

public enum StatModifierType {
    ADD,
    SUBTRACT,
    MULTIPLY,
    DIVIDE;

    public static StatModifierType parse(String s) {
        if (s == null) return null;
        for (var r : StatModifierType.values()) if (s.equalsIgnoreCase(r.name())) return r;
        return null;
    }
}
