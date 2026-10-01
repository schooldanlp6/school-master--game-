package de.adrian;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.*;

public class Test {

    private static final File file = new File("test.json");

    public static void main(String[] args) {
        createJsonIfNotExist();
        new SW(readFile());
    }

    private static void createJsonIfNotExist() {
        try {
            if (!file.exists()) file.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private static JsonElement readFile() {
        if (!file.exists()) return null;
        try (FileReader reader = new FileReader(file)) {
            return JsonParser.parseReader(reader);
        } catch (Exception e) {
            return null;
        }
    }

}
