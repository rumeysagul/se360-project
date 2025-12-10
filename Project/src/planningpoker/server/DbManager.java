package planningpoker.server;

import java.sql.*;

public class DbManager {

    private static final String DB_URL = "jdbc:sqlite:unipoker.db";

    static {
        try {
            // SQLite JDBC driver'ı yükle
            Class.forName("org.sqlite.JDBC");

            // Tablolar yoksa oluştur
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

        try (Connection conn = getConnection()) {
            conn.createStatement().execute(users);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
}
