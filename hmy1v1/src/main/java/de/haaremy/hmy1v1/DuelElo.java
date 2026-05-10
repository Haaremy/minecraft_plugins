package de.haaremy.hmy1v1;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DuelElo {

    private static final int DEFAULT_ELO = 1000;
    private static final int K_FACTOR = 32;

    private final Hmy1v1 plugin;
    private Connection connection;

    public DuelElo(Hmy1v1 plugin) {
        this.plugin = plugin;
        initDatabase();
    }

    private void initDatabase() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            String url = "jdbc:sqlite:" + new File(dataFolder, "duel_elo.db").getAbsolutePath();
            connection = DriverManager.getConnection(url);

            try (PreparedStatement stmt = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS duel_elo (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "elo INTEGER NOT NULL DEFAULT " + DEFAULT_ELO + ", " +
                    "wins INTEGER NOT NULL DEFAULT 0, " +
                    "losses INTEGER NOT NULL DEFAULT 0)")) {
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Fehler beim Initialisieren der ELO-Datenbank: " + e.getMessage());
        }
    }

    public int getElo(UUID uuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT elo FROM duel_elo WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("elo");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Fehler beim Laden der ELO fuer " + uuid + ": " + e.getMessage());
        }
        return DEFAULT_ELO;
    }

    public void setElo(UUID uuid, int elo) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO duel_elo (uuid, elo) VALUES (?, ?) " +
                "ON CONFLICT(uuid) DO UPDATE SET elo = ?")) {
            stmt.setString(1, uuid.toString());
            stmt.setInt(2, elo);
            stmt.setInt(3, elo);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Fehler beim Speichern der ELO fuer " + uuid + ": " + e.getMessage());
        }
    }

    public int getWins(UUID uuid) {
        return getStatValue(uuid, "wins");
    }

    public int getLosses(UUID uuid) {
        return getStatValue(uuid, "losses");
    }

    private int getStatValue(UUID uuid, String column) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT " + column + " FROM duel_elo WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(column);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Fehler beim Laden von " + column + " fuer " + uuid + ": " + e.getMessage());
        }
        return 0;
    }

    public void addWin(UUID uuid) {
        ensurePlayerExists(uuid);
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE duel_elo SET wins = wins + 1 WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Fehler beim Hinzufuegen eines Wins fuer " + uuid + ": " + e.getMessage());
        }
    }

    public void addLoss(UUID uuid) {
        ensurePlayerExists(uuid);
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE duel_elo SET losses = losses + 1 WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Fehler beim Hinzufuegen einer Niederlage fuer " + uuid + ": " + e.getMessage());
        }
    }

    private void ensurePlayerExists(UUID uuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT OR IGNORE INTO duel_elo (uuid, elo, wins, losses) VALUES (?, ?, 0, 0)")) {
            stmt.setString(1, uuid.toString());
            stmt.setInt(2, DEFAULT_ELO);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Fehler beim Anlegen des Spielers " + uuid + ": " + e.getMessage());
        }
    }

    public int[] calculateNewElo(int winnerElo, int loserElo) {
        double expectedWinner = 1.0 / (1.0 + Math.pow(10, (loserElo - winnerElo) / 400.0));
        double expectedLoser = 1.0 / (1.0 + Math.pow(10, (winnerElo - loserElo) / 400.0));

        int newWinnerElo = (int) Math.round(winnerElo + K_FACTOR * (1.0 - expectedWinner));
        int newLoserElo = (int) Math.round(loserElo + K_FACTOR * (0.0 - expectedLoser));

        if (newLoserElo < 0) newLoserElo = 0;

        return new int[]{newWinnerElo, newLoserElo};
    }

    /**
     * Gibt die Top-10 Spieler sortiert nach ELO zurueck.
     * Jeder Eintrag: [uuid, elo, wins, losses]
     */
    public List<String[]> getTopTen() {
        List<String[]> top = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT uuid, elo, wins, losses FROM duel_elo ORDER BY elo DESC LIMIT 10")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                top.add(new String[]{
                        rs.getString("uuid"),
                        String.valueOf(rs.getInt("elo")),
                        String.valueOf(rs.getInt("wins")),
                        String.valueOf(rs.getInt("losses"))
                });
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Fehler beim Laden der Top-10: " + e.getMessage());
        }
        return top;
    }

    public void shutdown() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Fehler beim Schliessen der ELO-Datenbankverbindung: " + e.getMessage());
        }
    }
}
