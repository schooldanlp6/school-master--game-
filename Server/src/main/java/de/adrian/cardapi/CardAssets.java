package de.adrian.cardapi;


import de.adrian.utils.Profession;
import de.adrian.utils.Rarity;

import java.io.File;
import java.io.IOException;
import java.util.Map;

class CardAssets {

    protected static final File folder = new File(CardInitiator.folder,"assets");
    private static Map<Class<?>,AssetElement<?>> elementMap;

    public static void init() {
        if (!folder.exists()) folder.mkdir();


        elementMap = Map.of(
                Profession.class, new ProfessionAsset(),
                Rarity.class, new RarityAsset()
        );


    }

    public static void createFile(File folder, String name) {
        try {
            File file = new File(folder,name + ".png");
            if (!file.exists()) file.createNewFile();
        }catch (IOException ex) {}
    }

    public static File getAsset(Object o) {
        AssetElement<?> element = elementMap.getOrDefault(o.getClass(),null);
        return element != null ? element.getAsset(o) : null;
    }
}
