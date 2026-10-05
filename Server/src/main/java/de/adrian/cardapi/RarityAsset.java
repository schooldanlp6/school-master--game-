package de.adrian.cardapi;

import de.adrian.utils.Profession;
import de.adrian.utils.Rarity;

import java.io.File;

class RarityAsset extends AssetElement {

    private static final CardAssetType[] types = {CardAssetType.PICTURE, CardAssetType.SOUND};

    RarityAsset() {
        super("rarity", Rarity.class,types);
        for (var prof : Rarity.values()) for (var type : types) CardAssets.createFile(getFolder(type),prof.name().toLowerCase(),type);
    }

    @Override
    File getAsset(Object o, CardAssetType type) {
        if (!(o instanceof Rarity asset)) return null;
        File file = new File(getFolder(type),asset.name() + type.fileSuffix);
        return file.exists() ? file : null;
    }


}
