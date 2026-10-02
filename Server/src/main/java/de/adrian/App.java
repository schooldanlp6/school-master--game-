package de.adrian;

import de.adrian.cardapi.CardInitiator;

import java.io.*;

public class App{

    public static final File folder = new File("data");
    public static CardAPI api;

    public static void main(String[] args) {
        if (!folder.exists()) folder.mkdir();
        api = CardInitiator.init();
    }

}
