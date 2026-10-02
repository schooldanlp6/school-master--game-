package de.adrian.cardapi;

import de.adrian.App;
import de.adrian.CardAPI;

import java.io.File;

public class CardInitiator {

    protected static final File folder = new File(App.folder,"cards");


    public static CardAPI init() {
        if (!folder.exists()) folder.mkdir();
        CardAssets.init();
        return new CardAPI(CardFile.init());
    }

    public static File getAsset(Object o) {
        return CardAssets.getAsset(o);
    }

}
