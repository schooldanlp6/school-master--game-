package de.adrian.cardapi;

import de.adrian.utils.Profession;

import java.io.File;

class ProfessionAsset extends AssetElement {

    ProfessionAsset() {
        super("professions", Profession.class);
        for (var prof : Profession.values()) CardAssets.createFile(folder,prof.name().toLowerCase());
    }


    @Override
    File getAsset(Object o) {
        return o instanceof Profession profession ? new File(folder,profession.name().toLowerCase()) : null;
    }
}
