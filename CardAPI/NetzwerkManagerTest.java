package CardAPI;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class NetzwerkManagerTest {

    private static final int PORT = 8080;

    public static void main(String[] args) {


        Thread serverThread = new Thread(() -> {
            try {
                System.out.println("[SERVER] Gestartet. Warte auf Spieler...");
                ServerSocket serverSocket = new ServerSocket(PORT);
                Socket verbindungZumClient = serverSocket.accept();
                System.out.println("[SERVER] Client ist dauerhaft verbunden!");

                InputStream eingang = verbindungZumClient.getInputStream();


                while (true) {
                    byte[] puffer = new byte[1024];
                    int geleseneBytes = eingang.read(puffer);

                    if (geleseneBytes == -1) {
                        System.out.println("[SERVER] Client hat die Verbindung getrennt.");
                        break;
                    }


                    Object empfangen = NetzwerkSender.binAerZuObject(puffer);

                    System.out.println("[SERVER] Nachricht empfangen: " + empfangen);
                }

                verbindungZumClient.close();
                serverSocket.close();
            } catch (Exception e) {
                System.out.println("[SERVER] Beendet oder Fehler.");
            }
        });
        serverThread.start();



        try { Thread.sleep(500); } catch (Exception e) {}



        try {
            System.out.println("[CLIENT] Verbinde dauerhaft zum Server...");
            Socket clientSocket = new Socket("localhost", PORT);
            OutputStream ausgang = clientSocket.getOutputStream();

            // Nachricht 1 senden
            String nachricht1 = "Spieler 1 ist beigetreten!";
            ausgang.write(NetzwerkSender.objektZuBinAer(nachricht1));
            ausgang.flush();


            Thread.sleep(1000);

            // Nachricht 2 senden
            String nachricht2 = "Ich ziehe Karte Herz 10.";
            ausgang.write(NetzwerkSender.objektZuBinAer(nachricht2));
            ausgang.flush();


            Thread.sleep(1000);

            // Nachricht 3 senden
            String nachricht3 = "Ich passe.";
            ausgang.write(NetzwerkSender.objektZuBinAer(nachricht3));
            ausgang.flush();

            System.out.println("[CLIENT] Alle 3 Spielzüge wurden nacheinander gesendet!");


            clientSocket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
