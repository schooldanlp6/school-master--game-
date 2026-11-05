package CardAPI;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

public class NetzwerkSender {

    public static byte[] objektZuBinAer(Object beliebigesObjekt) {
        try {

            ByteArrayOutputStream speicher = new ByteArrayOutputStream();


            ObjectOutputStream uebersetzer = new ObjectOutputStream(speicher);


            uebersetzer.writeObject(beliebigesObjekt);


            uebersetzer.flush();

           
            return speicher.toByteArray();

        } catch (Exception e) {
            System.out.println("Fehler beim Übersetzen in Binärcode: " + e.getMessage());
            return null;
        }
    }

    public static Object binAerZuObject (byte[] binaerdaten) {
    try{
        ByteArrayInputStream speicher = new ByteArrayInputStream(binaerdaten);

        ObjectInputStream uebersetzer = new ObjectInputStream(speicher);

        return uebersetzer.readObject();


    }catch (Exception e) {
        System.out.println("Fehler beim  Zurückverwandeln: " + e.getMessage());
        return null;
    }
    }

}
