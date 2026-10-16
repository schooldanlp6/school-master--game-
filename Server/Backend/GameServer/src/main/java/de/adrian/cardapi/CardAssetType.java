package de.adrian.cardapi;

public enum CardAssetType {

    PICTURE("png"),
    SOUND("mp3");

    CardAssetType(String fileSuffix) {
        this.fileSuffix = "." + fileSuffix;
    }

    public String fileSuffix;

}
