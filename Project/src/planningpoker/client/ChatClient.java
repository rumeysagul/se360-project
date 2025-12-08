// src/planningpoker/client/ChatClient.java
package planningpoker.client;

import java.io.*;
import java.net.Socket;

public class ChatClient {

    public static void main(String[] args) {
        String host = "localhost";
        int port = 5000;

        try (Socket socket = new Socket(host, port)) {
            System.out.println("Server'a bağlanıldı.");

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );
            PrintWriter out = new PrintWriter(
                    socket.getOutputStream(), true
            );
            BufferedReader userIn = new BufferedReader(
                    new InputStreamReader(System.in)
            );

            // Server’dan gelecek mesajları dinleyen thread
            Thread listener = new Thread(() -> {
                try {
                    String serverMsg;
                    while ((serverMsg = in.readLine()) != null) {
                        System.out.println(">> " + serverMsg);
                    }
                } catch (IOException e) {
                    System.out.println("Server bağlantısı kesildi.");
                }
            });
            listener.start();

            // Kullanıcıdan okuyup server’a gönderen kısım
            String line;
            while ((line = userIn.readLine()) != null) {
                out.println(line);

                if (line.equalsIgnoreCase("quit")) {
                    System.out.println("Çıkış komutu gönderildi.");
                    break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
