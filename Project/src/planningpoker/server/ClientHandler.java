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
            System.out.println("New user: " + username);

            // 2) Rol seçimi
            out.println("Rolünü yaz (OWNER veya WORKER):");
            String role = in.readLine();

            if (role != null && role.equalsIgnoreCase("OWNER")) {
                // OWNER olmak istiyor -> şifre iste
                out.println("Owner şifresini gir:");
                String key = in.readLine();

                // Zaten bir owner varsa
                if (server.getOwner() != null) {
                    out.println("[SERVER] Owner is already connected! Access denied.");
                    return;  // run() biter, finally'de socket kapanır
                }

                // Şifre yanlışsa
                if (!server.isValidOwnerKey(key)) {
                    out.println("[SERVER] Incorrect owner password! Access denied.");
                    return;  //  WORKER yapma, direkt çık
                }

                // Şifre doğruysa
                isOwner = true;
                server.setOwner(this);
                out.println("[SERVER] Patron (OWNER) olarak giriş yaptın.");

            } else {
                // Direkt worker
                isOwner = false;
                server.getRoom().addWorker(username);
                out.println("[SERVER] WORKER olarak giriş yaptın.");
            }
            DbManager.saveUser(username, isOwner ? "OWNER" : "WORKER");

            // buraya geldiyse gerçekten oyuna girmiş demektir
            server.broadcast("[SERVER] " + username + " has joined the session.");


            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("[" + username + "]: " + line);

                if (line.equalsIgnoreCase("quit")) {
                    break;
                }

                // OWNER -> TASK tanımlar
                if (line.startsWith("TASK:")) {
                    handleTask(line);
                }
                // WORKER -> VOTE verir
                else if (line.startsWith("VOTE:")) {
                    handleVote(line);
                }
                // OWNER -> RESET ile oyları temizler
                else if (line.equalsIgnoreCase("RESET")) {
                    handleReset();
                }
                // Normal chat mesajı
                else {
                    server.broadcast("[" + username + "]: " + line);
                }
            }

        } catch (IOException e) {
            System.out.println("User connection lost: " + username);
        } finally {
            // Worker ise room'dan da çıkar
            if (!isOwner) {
                server.getRoom().removeWorker(username);
            }

            server.removeClient(this);
            server.broadcast("[SERVER] " + username + " has left the session.");
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("ClientHandler terminated: " + username);
        }
    }

    // Sadece OWNER kullanmalı: "TASK: bir şey"

    private void handleTask(String line) {
        if (!isOwner) {
            sendMessage("[ERROR] Only the OWNER can define a new task.");
            return;
        }
        String task = line.substring("TASK:".length()).trim();
        if (task.isEmpty()) {
            sendMessage("[ERROR] Task cannot be empty. Example: TASK: Password reset feature");
            return;
        }

        // Oyun state + DB
        server.getRoom().setCurrentTask(task, username);

        sendMessage("[SERVER] Task set: " + task);
        server.broadcastToWorkers("[TASK] New Task: " + task);
    }

    private void handleVote(String line) {
        if (isOwner) {
            sendMessage("[ERROR] OWNER cannot vote, can only view the results.");
            return;
        }

        try {
            String valueStr = line.substring("VOTE:".length()).trim();
            int value = Integer.parseInt(valueStr);

            server.getRoom().addVote(username, value);

            // DB: vote kaydet
            int roundId = server.getRoom().getCurrentRoundId();
            if (roundId != -1) {
                DbManager.saveVote(roundId, username, value);
            }

            sendMessage("[SERVER] Your vote has been recorded: " + value);

            if (server.getRoom().allWorkersVoted()) {
                String result = server.getRoom().calculateResultText();

                // DB: sonucu kaydet
                GameRoom.ResultStats stats = server.getRoom().computeStats();
                if (stats != null && roundId != -1) {
                    DbManager.saveResult(roundId, stats.min, stats.max, stats.avg, stats.total);
                }

                ClientHandler owner = server.getOwner();
                if (owner != null) owner.sendMessage(result);

                server.broadcastToWorkers("[SERVER] All votes collected, results sent to the OWNER.");
            }
        } catch (NumberFormatException e) {
            sendMessage("[ERROR] VOTE command must be in 'VOTE:5' format.");
        }
    }

    private void handleReset() {
        if (!isOwner) {
            sendMessage("[ERROR] Only the OWNER can reset the voting session.");
            return;
        }

        // Aynı task için yeni round başlat
        int newRoundId = server.getRoom().startNewRoundSameTask();

        sendMessage("[SERVER] The Votes have been reset. New round started for the same task. (ROUND_ID=" + newRoundId + ")");
        server.broadcastToWorkers("[SERVER] The Votes have been reset, please vote again.");
    }
}