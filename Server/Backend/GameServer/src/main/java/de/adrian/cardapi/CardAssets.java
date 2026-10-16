package de.adrian.cardapi;


import de.adrian.utils.Profession;
import de.adrian.utils.Rarity;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

class CardAssets {

    private static final File folder = new File(CardInitiator.folder,"assets");
    protected static final Map<CardAssetType, File> folderMap = new EnumMap<>(CardAssetType.class);
    private static Map<Class<?>,AssetElement<?>> elementMap;

    public static void init() {
        if (!folder.exists()) folder.mkdir();
        for (var type : CardAssetType.values()) {
            File file = new File(folder,type.name().toLowerCase());
            if (!file.exists()) file.mkdir();
            folderMap.put(type,file);
        }


        elementMap = Map.of(
                Profession.class, new ProfessionAsset(),
                Rarity.class, new RarityAsset()
        );


    }

    public static void createFile(File folder, String name,CardAssetType type) {
        try {
            File file = new File(folder,name + type.fileSuffix);
            if (!file.exists()) file.createNewFile();
        }catch (IOException ex) {}
    }

    public static File getAsset(Object o,CardAssetType type) {
        AssetElement<?> element = elementMap.getOrDefault(o.getClass(),null);
        return element != null ? element.getAsset(o,type) : null;
    }
    public static File getAsset(Class<?> clazz, CardAssetType type, String name) {
        AssetElement<?> element = elementMap.getOrDefault(clazz,null);
        if (element == null) return null;

        File folder = element.getFolder(type);
        File file = new File(folder,name + type.fileSuffix);
        return file.exists() ? file : null;
    }
    public static File getAssetFromString(String s) {
        File file = new File(folder,s);
        return file.exists() ? file : null;
    }
}
