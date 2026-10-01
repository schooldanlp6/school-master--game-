package de.adrian.utils;

import java.util.HashMap;
import java.util.Map;

public class SW {

    private static final Map<String,Identifier> identifierMap = new HashMap<>();

    public static Identifier identifier(String value) {
        return identifierMap.computeIfAbsent(value, Identifier::new);
    }

}
