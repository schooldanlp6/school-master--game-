package de.adrian.cardapi;

import de.adrian.utils.Rarity;

import java.io.File;

class RarityAsset extends AssetElement {

    RarityAsset() {
        super("rarity", Rarity.class);
        for (var prof : Rarity.values()) CardAssets.createFile(folder,prof.name().toLowerCase());
    }


    @Override
    File getAsset(Object o) {
        return o instanceof Rarity rarity ? new File(folder,rarity.name().toLowerCase()) : null;
    }




}
