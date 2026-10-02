package de.adrian.cardapi;

import java.io.File;

abstract class AssetElement<T> {

    private final Class<T> clazz;
    protected final File folder;

    public AssetElement(String name, Class<T> clazz) {
        this.clazz = clazz;

        folder = new File(CardAssets.folder,name);
        if (!folder.exists()) folder.mkdir();
    }

    abstract File getAsset(Object o);


}
