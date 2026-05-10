package de.haaremy.hmyparkour;

import org.bukkit.Bukkit;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class ParkourLeaderboard {

    private Connection connection;

    public ParkourLeaderboard(File dataFolder) {
        initDatabase(dataFolder);
    }

    private void initDatabase(File dataFolder) {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        try {
            String url = "jdbc:sqlite:" + new File(dataFolder, "parkour_times.db").getAbsolutePath();
            connection = DriverManager.getConnection(url);

            try (PreparedStatement stmt = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS parkour_times (" +
                    "course_name TEXT NOT NULL, " +
                    "uuid TEXT NOT NULL, " +
                    "best_time_ms BIGINT NOT NULL, " +
                    "completed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "PRIMARY KEY (course_name, uuid))")) {
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Fehler beim Initialisieren der Parkour-Datenbank", e);
        }
    }

    public void saveTime(String courseName, UUID uuid, long timeMs) {
        try {
            Long existing = getBestTime(courseName, uuid);
            if (existing != null && existing <= timeMs) {
                return; // Bestehende Zeit ist besser
            }

            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT OR REPLACE INTO parkour_times (course_name, uuid, best_time_ms, completed_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)")) {
                stmt.setString(1, courseName);
                stmt.setString(2, uuid.toString());
                stmt.setLong(3, timeMs);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.WARNING, "Fehler beim Speichern der Parkour-Zeit", e);
        }
    }

    public Long getBestTime(String courseName, UUID uuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT best_time_ms FROM parkour_times WHERE course_name = ? AND uuid = ?")) {
            stmt.setString(1, courseName);
            stmt.setString(2, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("best_time_ms");
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.WARNING, "Fehler beim Laden der Parkour-Bestzeit", e);
        }
        return null;
    }

    public List<LeaderboardEntry> getTopTimes(String courseName, int limit) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT uuid, best_time_ms FROM parkour_times WHERE course_name = ? ORDER BY best_time_ms ASC LIMIT ?")) {
            stmt.setString(1, courseName);
            stmt.setInt(2, limit);
            ResultSet rs = stmt.executeQuery();
            int rank = 1;
            while (rs.next()) {
                String uuidStr = rs.getString("uuid");
                long time = rs.getLong("best_time_ms");
                String playerName = Bukkit.getOfflinePlayer(UUID.fromString(uuidStr)).getName();
                if (playerName == null) playerName = "Unbekannt";
                entries.add(new LeaderboardEntry(rank++, playerName, time));
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.WARNING, "Fehler beim Laden des Parkour-Leaderboards", e);
        }
        return entries;
    }

    public void shutdown() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.WARNING, "Fehler beim Schliessen der Parkour-Datenbank", e);
        }
    }

    public static class LeaderboardEntry {
        private final int rank;
        private final String playerName;
        private final long timeMs;

        public LeaderboardEntry(int rank, String playerName, long timeMs) {
            this.rank = rank;
            this.playerName = playerName;
            this.timeMs = timeMs;
        }

        public int getRank() {
            return rank;
        }

        public String getPlayerName() {
            return playerName;
        }

        public long getTimeMs() {
            return timeMs;
        }
    }
}
