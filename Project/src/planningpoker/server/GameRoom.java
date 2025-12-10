// src/planningpoker/server/GameScreen.java
package planningpoker.server;

import java.util.*;

public class GameRoom {

    private String currentTask;                         // Şu anki görev
    private final Set<String> workers = new HashSet<>(); // Oy verecek çalışanlar
    private final Map<String, Integer> votes = new HashMap<>();

    // Patron yeni bir task verdiğinde çağrılır
    public synchronized void setCurrentTask(String task) {
        this.currentTask = task;
        votes.clear(); // yeni task için eski oyları temizle
    }

    public synchronized String getCurrentTask() {
        return currentTask;
    }

    // Yeni worker bağlanınca
    public synchronized void addWorker(String username) {
        workers.add(username);
    }

    // Worker ayrılırsa
    public synchronized void removeWorker(String username) {
        workers.remove(username);
        votes.remove(username);
    }

    // Worker oy verince
    public synchronized void addVote(String username, int value) {
        if (workers.contains(username)) {
            votes.put(username, value);
        }
    }

    // Tüm worker'lar oy verdi mi?
    public synchronized boolean allWorkersVoted() {
        return !workers.isEmpty() && votes.size() == workers.size();
    }

    public synchronized String calculateResultText() {
        if (votes.isEmpty()) {
            return "RESULT: Henüz oy yok.";
        }

        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int sum = 0;

        for (int v : votes.values()) {
            if (v < min) min = v;
            if (v > max) max = v;
            sum += v;
        }
        double avg = sum / (double) votes.size();

        String taskInfo = (currentTask != null) ? ("TASK=\"" + currentTask + "\" ") : "";
        return "RESULT: " + taskInfo +
                "MIN=" + min + ", MAX=" + max + ", AVG=" + String.format("%.2f", avg) +
                " (TOPLAM OY=" + votes.size() + ")";
    }

    // Aynı task için yeni tur oylama istersen
    public synchronized void resetVotes() {
        votes.clear();
    }
}
