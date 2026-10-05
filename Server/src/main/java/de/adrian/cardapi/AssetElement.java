package de.adrian.cardapi;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

abstract class AssetElement<T> {

    private final Class<T> clazz;
    private final Map<CardAssetType,File> folders = new EnumMap<>(CardAssetType.class);

    public AssetElement(String name, Class<T> clazz, CardAssetType[] types) {
        this.clazz = clazz;

        for (var type : types) {
            File folder = new File(CardAssets.folderMap.get(type),name);
            if (!folder.exists()) folder.mkdir();
            folders.put(type,folder);
        }
    }

    public File getFolder(CardAssetType type) {
        return folders.getOrDefault(type,null);
    }
    public Set<CardAssetType> getAllowedTypes() {
        return folders.keySet();
    }

    abstract File getAsset(Object o, CardAssetType type);

}
