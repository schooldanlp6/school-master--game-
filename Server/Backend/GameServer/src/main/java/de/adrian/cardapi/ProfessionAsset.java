package de.adrian.cardapi;

import de.adrian.utils.Profession;

import java.io.File;

class ProfessionAsset extends AssetElement {

    private static final CardAssetType[] types = {CardAssetType.PICTURE};

    ProfessionAsset() {
        super("professions", Profession.class,types);
        for (var prof : Profession.values()) CardAssets.createFile(getFolder(types[0]),prof.name().toLowerCase(),types[0]);
    }

    @Override
    File getAsset(Object o, CardAssetType type) {
        if (!(o instanceof Profession asset)) return null;
        File file = new File(getFolder(type),asset.name() + type.fileSuffix);
        return file.exists() ? file : null;
    }
}
