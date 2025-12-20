package planningpoker.server;

import java.sql.*;

public class DbManager {

    private static final String DB_URL = "jdbc:sqlite:unipoker.db";

    static {
        try {
            Class.forName("org.sqlite.JDBC");
            createTables();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private static void createTables() {

        String users = """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE,
                role TEXT,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP
            );
        """;

        // Boss’un açtığı görevler
        String tasks = """
            CREATE TABLE IF NOT EXISTS tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                task_text TEXT NOT NULL,
                owner_username TEXT,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP
            );
        """;

        // Aynı task için birden fazla oylama turu (RESET vs.)
        String rounds = """
            CREATE TABLE IF NOT EXISTS rounds (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                task_id INTEGER NOT NULL,
                round_no INTEGER NOT NULL,
                started_at TEXT DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY(task_id) REFERENCES tasks(id)
            );
        """;

        // Oylar (round bazlı)
        String votes = """
            CREATE TABLE IF NOT EXISTS votes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                round_id INTEGER NOT NULL,
                username TEXT NOT NULL,
                value INTEGER NOT NULL,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(round_id, username),
                FOREIGN KEY(round_id) REFERENCES rounds(id)
            );
        """;

        // Sonuç (round bazlı)
        String results = """
            CREATE TABLE IF NOT EXISTS results (
                round_id INTEGER PRIMARY KEY,
                min_val INTEGER,
                max_val INTEGER,
                avg_val REAL,
                total_votes INTEGER,
                computed_at TEXT DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY(round_id) REFERENCES rounds(id)
            );
        """;

        try (Connection conn = getConnection()) {
            Statement st = conn.createStatement();
            st.execute(users);
            st.execute(tasks);
            st.execute(rounds);
            st.execute(votes);
            st.execute(results);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Zaten vardı, aynen durabilir
    public static void saveUser(String username, String role) {
        String sql = "INSERT OR IGNORE INTO users(username, role) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, role);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //  Task oluştur
    public static int createTask(String taskText, String ownerUsername) {
        String sql = "INSERT INTO tasks(task_text, owner_username) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, taskText);
            ps.setString(2, ownerUsername);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    //  Round oluştur
    public static int createRound(int taskId, int roundNo) {
        String sql = "INSERT INTO rounds(task_id, round_no) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, taskId);
            ps.setInt(2, roundNo);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    //  Vote kaydet (aynı user aynı round’da tekrar oy atamasın diye UPSERT)
    public static void saveVote(int roundId, String username, int value) {
        String sql = """
            INSERT INTO votes(round_id, username, value)
            VALUES (?, ?, ?)
            ON CONFLICT(round_id, username)
            DO UPDATE SET value=excluded.value, created_at=CURRENT_TIMESTAMP
        """;

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roundId);
            ps.setString(2, username);
            ps.setInt(3, value);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //  Result kaydet (aynı round için 1 kez)
    public static void saveResult(int roundId, int min, int max, double avg, int totalVotes) {
        String sql = """
            INSERT OR REPLACE INTO results(round_id, min_val, max_val, avg_val, total_votes)
            VALUES (?, ?, ?, ?, ?)
        """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roundId);
            ps.setInt(2, min);
            ps.setInt(3, max);
            ps.setDouble(4, avg);
            ps.setInt(5, totalVotes);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
