package planningpoker.server;

import java.util.*;

public class GameRoom {

    private String currentTask;
    private int currentTaskId = -1;

    private int currentRoundId = -1;
    private int roundNo = 0;

    private final Set<String> workers = new HashSet<>();
    private final Map<String, Integer> votes = new HashMap<>();

    public synchronized void setCurrentTask(String task, String ownerUsername) {
        this.currentTask = task;
        votes.clear();

        // DB: task + round(1)
        this.currentTaskId = DbManager.createTask(task, ownerUsername);
        this.roundNo = 1;
        this.currentRoundId = DbManager.createRound(currentTaskId, roundNo);
    }

    public synchronized int startNewRoundSameTask() {
        votes.clear();
        roundNo++;
        currentRoundId = DbManager.createRound(currentTaskId, roundNo);
        return currentRoundId;
    }

    public synchronized String getCurrentTask() {
        return currentTask;
    }

    public synchronized int getCurrentRoundId() {
        return currentRoundId;
    }

    public synchronized void addWorker(String username) {
        workers.add(username);
    }

    public synchronized void removeWorker(String username) {
        workers.remove(username);
        votes.remove(username);
    }

    public synchronized void addVote(String username, int value) {
        if (workers.contains(username)) {
            votes.put(username, value);
        }
    }

    public synchronized boolean allWorkersVoted() {
        return !workers.isEmpty() && votes.size() == workers.size();
    }

    public synchronized ResultStats computeStats() {
        if (votes.isEmpty()) return null;

        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int sum = 0;

        for (int v : votes.values()) {
            min = Math.min(min, v);
            max = Math.max(max, v);
            sum += v;
        }
        double avg = sum / (double) votes.size();
        return new ResultStats(min, max, avg, votes.size());
    }

    public synchronized String calculateResultText() {
        ResultStats stats = computeStats();
        if (stats == null) return "RESULT: Henüz oy yok.";

        String taskInfo = (currentTask != null) ? ("TASK=\"" + currentTask + "\" ") : "";
        return "RESULT: " + taskInfo +
                "MIN=" + stats.min + ", MAX=" + stats.max + ", AVG=" + String.format("%.2f", stats.avg) +
                " (TOPLAM OY=" + stats.total + ")";
    }

    public synchronized void resetVotes() {
        votes.clear();
    }

    // Küçük DTO
    public static class ResultStats {
        public final int min, max, total;
        public final double avg;

        public ResultStats(int min, int max, double avg, int total) {
            this.min = min;
            this.max = max;
            this.avg = avg;
            this.total = total;
        }
    }
}
