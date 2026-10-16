package de.adrian.cardapi;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;

class CardFile {

    private static final File file = new File(CardInitiator.folder,"CardMeta.json");

    public static JsonElement init() {
        try {
            if (!file.exists()) file.createNewFile();
            return readFile(file);
        } catch (Exception e) {
           return null;
        }
    }
    private static JsonElement readFile(File file) {
        if (!file.exists()) return null;
        try (FileReader reader = new FileReader(file)) {
            return JsonParser.parseReader(reader);
        } catch (Exception e) {
            return null;
        }
    }

}
