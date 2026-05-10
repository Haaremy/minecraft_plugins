package de.haaremy.hmysumo;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class SumoElo {

    private static final int DEFAULT_ELO = 1000;
    private static final int K_FACTOR = 32;

    private final HmySumo plugin;
    private Connection connection;

    public SumoElo(HmySumo plugin) {
        this.plugin = plugin;
        initDatabase();
    }

    private void initDatabase() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            String url = "jdbc:sqlite:" + new File(dataFolder, "sumo_elo.db").getAbsolutePath();
            connection = DriverManager.getConnection(url);

            try (PreparedStatement stmt = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS sumo_elo (" +
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
                "SELECT elo FROM sumo_elo WHERE uuid = ?")) {
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
                "INSERT INTO sumo_elo (uuid, elo) VALUES (?, ?) " +
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
                "SELECT " + column + " FROM sumo_elo WHERE uuid = ?")) {
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
                "UPDATE sumo_elo SET wins = wins + 1 WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Fehler beim Hinzufuegen eines Wins fuer " + uuid + ": " + e.getMessage());
        }
    }

    public void addLoss(UUID uuid) {
        ensurePlayerExists(uuid);
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE sumo_elo SET losses = losses + 1 WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Fehler beim Hinzufuegen einer Niederlage fuer " + uuid + ": " + e.getMessage());
        }
    }

    private void ensurePlayerExists(UUID uuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT OR IGNORE INTO sumo_elo (uuid, elo, wins, losses) VALUES (?, ?, 0, 0)")) {
            stmt.setString(1, uuid.toString());
            stmt.setInt(2, DEFAULT_ELO);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Fehler beim Anlegen des Spielers " + uuid + ": " + e.getMessage());
        }
    }

    /**
     * Berechnet die neuen ELO-Werte nach einem Spiel.
     * Gibt ein Array zurueck: [neuerWinnerElo, neuerLoserElo]
     */
    public int[] calculateNewElo(int winnerElo, int loserElo) {
        double expectedWinner = 1.0 / (1.0 + Math.pow(10, (loserElo - winnerElo) / 400.0));
        double expectedLoser = 1.0 / (1.0 + Math.pow(10, (winnerElo - loserElo) / 400.0));

        int newWinnerElo = (int) Math.round(winnerElo + K_FACTOR * (1.0 - expectedWinner));
        int newLoserElo = (int) Math.round(loserElo + K_FACTOR * (0.0 - expectedLoser));

        // ELO kann nicht unter 0 fallen
        if (newLoserElo < 0) newLoserElo = 0;

        return new int[]{newWinnerElo, newLoserElo};
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
