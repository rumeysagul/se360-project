// src/planningpoker/server/ClientHandler.java
package planningpoker.server;

import java.io.*;
import java.net.Socket;

public class ClientHandler extends Thread {

    private Socket socket;
    private PokerServer server;
    private BufferedReader in;
    private PrintWriter out;
    private String username;
    private boolean isOwner = false;

    public ClientHandler(Socket socket, PokerServer server) {
        this.socket = socket;
        this.server = server;
        try {
            in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );
            out = new PrintWriter(
                    socket.getOutputStream(), true
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String msg) {
        out.println(msg);
    }

    public boolean isOwner() {
        return isOwner;
    }

    @Override
    public void run() {
        try {
            // 1) Kullanıcı adı
            out.println("Kullanıcı adını gir:");
            username = in.readLine();
            System.out.println("Yeni kullanıcı: " + username);

            // 2) Rol seçimi
            out.println("Rolünü yaz (OWNER veya WORKER):");
            String role = in.readLine();

            if (role != null && role.equalsIgnoreCase("OWNER")) {
                // OWNER olmak istiyor → şifre iste
                out.println("Owner şifresini gir:");
                String key = in.readLine();

                if (server.getOwner() == null && server.isValidOwnerKey(key)) {
                    // Şifre doğru ve daha önce owner yok → owner yap
                    isOwner = true;
                    server.setOwner(this);
                    out.println("[SERVER] Patron (OWNER) olarak giriş yaptın.");
                } else {
                    // Şifre yanlış ya da zaten owner var
                    out.println("[SERVER] Owner olma isteği reddedildi, WORKER olarak giriş yapıyorsun.");
                    isOwner = false;
                    server.getRoom().addWorker(username);
                }
            } else {
                // Direkt worker
                isOwner = false;
                server.getRoom().addWorker(username);
                out.println("[SERVER] WORKER olarak giriş yaptın.");
            }

            server.broadcast("[SERVER] " + username + " katıldı.");

            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("[" + username + "]: " + line);

                if (line.equalsIgnoreCase("quit")) {
                    break;
                }

                // OWNER → TASK tanımlar
                if (line.startsWith("TASK:")) {
                    handleTask(line);
                }
                // WORKER → VOTE verir
                else if (line.startsWith("VOTE:")) {
                    handleVote(line);
                }
                // OWNER → RESET ile oyları temizler
                else if (line.equalsIgnoreCase("RESET")) {
                    handleReset();
                }
                // Normal chat mesajı (istersen kalsın)
                else {
                    server.broadcast("[" + username + "]: " + line);
                }
            }

        } catch (IOException e) {
            System.out.println("Kullanıcı bağlantısı koptu: " + username);
        } finally {
            // Worker ise room'dan da çıkar
            if (!isOwner) {
                server.getRoom().removeWorker(username);
            }

            server.removeClient(this);
            server.broadcast("[SERVER] " + username + " ayrıldı.");
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("ClientHandler sonlandı: " + username);
        }
    }

    // Sadece OWNER kullanmalı: "TASK: bir şey"
    private void handleTask(String line) {
        if (!isOwner) {
            sendMessage("[HATA] Sadece OWNER yeni task tanımlayabilir.");
            return;
        }
        String task = line.substring("TASK:".length()).trim();
        if (task.isEmpty()) {
            sendMessage("[HATA] Task boş olamaz. Örnek: TASK: Login ekranı için şifre resetleme");
            return;
        }

        server.getRoom().setCurrentTask(task);
        sendMessage("[SERVER] Task ayarlandı: " + task);

        // Worker'lara task duyurulur
        server.broadcastToWorkers("[TASK] Yeni görev: " + task);
    }

    // "VOTE:5" gibi bir satırı işler (sadece WORKER)
    private void handleVote(String line) {
        if (isOwner) {
            sendMessage("[HATA] OWNER oy veremez, sadece sonucu görür.");
            return;
        }

        try {
            String valueStr = line.substring("VOTE:".length()).trim();
            int value = Integer.parseInt(valueStr);

            server.getRoom().addVote(username, value);
            sendMessage("[SERVER] Oyunuz kaydedildi: " + value);

            // Eğer tüm worker'lar oy verdiyse:
            if (server.getRoom().allWorkersVoted()) {
                String result = server.getRoom().calculateResultText();

                // Sonuç SADECE OWNER'a gönderilir
                ClientHandler owner = server.getOwner();
                if (owner != null) {
                    owner.sendMessage(result);
                }

                // Worker'lara sadece bilgilendirme
                server.broadcastToWorkers("[SERVER] Tüm oylar toplandı, sonuç patrona (OWNER) gönderildi.");
            }
        } catch (NumberFormatException e) {
            sendMessage("[HATA] VOTE komutu 'VOTE:5' şeklinde olmalı.");
        }
    }

    // "RESET" sadece OWNER
    private void handleReset() {
        if (!isOwner) {
            sendMessage("[HATA] Sadece OWNER reset yapabilir.");
            return;
        }

        server.getRoom().resetVotes();
        sendMessage("[SERVER] Oylar sıfırlandı. Aynı task için yeni oylama başlayabilir.");
        server.broadcastToWorkers("[SERVER] Oylar sıfırlandı, lütfen tekrar oy verin.");
    }
}
