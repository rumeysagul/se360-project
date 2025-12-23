package planningpoker.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GameRoomTest {

    @Test
    void allWorkersVoted_returnsFalse_whenNoVotes() {
        GameRoom room = new GameRoom();
        room.addWorker("Ali");
        room.addWorker("Ayse");

        assertFalse(room.allWorkersVoted());
    }

    @Test
    void allWorkersVoted_returnsTrue_whenEveryoneVoted() {
        GameRoom room = new GameRoom();
        room.addWorker("Ali");
        room.addWorker("Ayse");

        room.addVote("Ali", 5);
        room.addVote("Ayse", 8);

        assertTrue(room.allWorkersVoted());
    }

    @Test
    void computeStats_calculatesMinMaxAvgCorrectly() {
        GameRoom room = new GameRoom();
        room.addWorker("Ali");
        room.addWorker("Ayse");
        room.addWorker("Mehmet");

        room.addVote("Ali", 3);
        room.addVote("Ayse", 5);
        room.addVote("Mehmet", 8);

        GameRoom.ResultStats stats = room.computeStats();

        assertNotNull(stats);
        assertEquals(3, stats.min);
        assertEquals(8, stats.max);
        assertEquals(3, stats.total);
        assertEquals(5.33, stats.avg, 0.01); // tolerance
    }

    @Test
    void addVote_ignoresUserNotInWorkers() {
        GameRoom room = new GameRoom();
        room.addWorker("Ali");

        room.addVote("Ali", 5);
        room.addVote("Hacker", 100); // worker değil

        GameRoom.ResultStats stats = room.computeStats();
        assertNotNull(stats);
        assertEquals(1, stats.total);
        assertEquals(5, stats.min);
        assertEquals(5, stats.max);
    }
}
