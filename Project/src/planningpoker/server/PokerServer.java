// src/planningpoker/server/PokerServer.java
package planningpoker.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class PokerServer {

    private int port;
    private final List<ClientHandler> clients = new ArrayList<>();
    private final GameRoom room = new GameRoom();   // oyun durumu

    private ClientHandler owner;                     // patron
    private static final String OWNER_SECRET = "owner123"; // 🔑 Patron şifresi (istersen değiştir)

    public PokerServer(int port) {
        this.port = port;
    }

    public GameRoom getRoom() {
        return room;
    }

    public synchronized void setOwner(ClientHandler owner) {
        this.owner = owner;
    }

    public synchronized ClientHandler getOwner() {
        return owner;
    }

    public boolean isValidOwnerKey(String key) {
        return OWNER_SECRET.equals(key);
    }

    // Bütün clientlara mesaj
    public synchronized void broadcast(String message) {
        for (ClientHandler ch : clients) {
            ch.sendMessage(message);
        }
    }

    // Sadece worker'lara mesaj
    public synchronized void broadcastToWorkers(String message) {
        for (ClientHandler ch : clients) {
            if (!ch.isOwner()) {
                ch.sendMessage(message);
            }
        }
    }

    public synchronized void addClient(ClientHandler ch) {
        clients.add(ch);
    }

    public synchronized void removeClient(ClientHandler ch) {
        clients.remove(ch);
        if (owner == ch) {
            owner = null; // owner ayrılırsa boşalt
        }
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("PokerServer başlatıldı. Port: " + port);

            while (true) {
                System.out.println("Client bekleniyor...");
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client bağlandı: " + clientSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(clientSocket, this);
                addClient(handler);
                handler.start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        PokerServer server = new PokerServer(5005);
        server.start();
    }
}
