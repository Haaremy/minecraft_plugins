package de.haaremy.hmydailyrewards;

import org.bukkit.entity.Player;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class DailyManager {

    private final HmyDailyRewards plugin;
    private Connection connection;

    public DailyManager(HmyDailyRewards plugin) {
        this.plugin = plugin;
        initDatabase();
    }

    private void initDatabase() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            String url = "jdbc:sqlite:" + new File(dataFolder, "daily.db").getAbsolutePath();
            connection = DriverManager.getConnection(url);

            try (PreparedStatement stmt = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS daily_rewards (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "last_claim_day INTEGER NOT NULL DEFAULT 0, " +
                    "current_streak INTEGER NOT NULL DEFAULT 0, " +
                    "total_claims INTEGER NOT NULL DEFAULT 0)")) {
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Fehler beim Initialisieren der Daily-Datenbank: " + e.getMessage());
        }
    }

    /**
     * Aktueller UTC-Tag (epoch seconds / 86400).
     */
    public static long currentDay() {
        return System.currentTimeMillis() / 1000L / 86400L;
    }

    /**
     * Kann der Spieler heute claimen?
     */
    public boolean canClaim(UUID uuid) {
        long today = currentDay();
        long last = getLastClaimDay(uuid);
        return last < today;
    }

    /**
     * Gibt den last_claim_day zurueck (0 wenn noch nie).
     */
    public long getLastClaimDay(UUID uuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT last_claim_day FROM daily_rewards WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("last_claim_day");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Fehler beim Laden last_claim_day fuer " + uuid + ": " + e.getMessage());
        }
        return 0L;
    }

    /**
     * Aktueller Streak (0 wenn noch nie geclaimt).
     * Berechnung beruecksichtigt Streak-Timeout: bricht nach konfigurierter Stundenzahl.
     */
    public int getStreak(UUID uuid) {
        int stored = getStoredStreak(uuid);
        long last = getLastClaimDay(uuid);
        if (stored == 0 || last == 0) {
            return 0;
        }
        long today = currentDay();
        long timeoutHours = plugin.getConfig().getLong("streak_timeout_hours", 48L);
        long maxDaysGap = Math.max(1L, timeoutHours / 24L);
        // Wenn der letzte Claim laenger als maxDaysGap her ist, Streak als gebrochen betrachten
        if (today - last > maxDaysGap) {
            return 0;
        }
        return stored;
    }

    private int getStoredStreak(UUID uuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT current_streak FROM daily_rewards WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("current_streak");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Fehler beim Laden current_streak fuer " + uuid + ": " + e.getMessage());
        }
        return 0;
    }

    public int getTotalClaims(UUID uuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT total_claims FROM daily_rewards WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("total_claims");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Fehler beim Laden total_claims fuer " + uuid + ": " + e.getMessage());
        }
        return 0;
    }

    /**
     * Fuehrt einen Claim aus. Updated DB und gibt den Reward an den Spieler.
     * Returned die erhaltene DailyReward oder null wenn schon geclaimt.
     */
    public DailyReward claim(UUID uuid, Player player) {
        if (!canClaim(uuid)) {
            return null;
        }

        long today = currentDay();
        long last = getLastClaimDay(uuid);
        int storedStreak = getStoredStreak(uuid);
        int totalClaims = getTotalClaims(uuid);

        long timeoutHours = plugin.getConfig().getLong("streak_timeout_hours", 48L);
        long maxDaysGap = Math.max(1L, timeoutHours / 24L);

        int newStreak;
        if (last == 0 || today - last > maxDaysGap) {
            // Streak neu starten
            newStreak = 1;
        } else if (today - last == 1) {
            // Consecutive
            newStreak = storedStreak + 1;
        } else {
            // today - last <= maxDaysGap, aber > 1. Erlaubt aber eigentlich nur 1 Luecke: wir setzen fort
            // Realistisch: timeoutHours = 48 -> maxDaysGap = 2 -> (heute-last==2) = noch im Streak
            newStreak = storedStreak + 1;
        }

        // Belohnung bestimmen basierend auf neuem Streak-Tag (1-30, danach wrap)
        int rewardDay = ((newStreak - 1) % 30) + 1;
        DailyReward reward = plugin.getRewardsConfig().getReward(rewardDay);

        // Reward geben
        if (reward != null) {
            reward.give(player);
        }

        // DB updaten
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO daily_rewards (uuid, last_claim_day, current_streak, total_claims) " +
                "VALUES (?, ?, ?, ?) " +
                "ON CONFLICT(uuid) DO UPDATE SET " +
                "last_claim_day = excluded.last_claim_day, " +
                "current_streak = excluded.current_streak, " +
                "total_claims = excluded.total_claims")) {
            stmt.setString(1, uuid.toString());
            stmt.setLong(2, today);
            stmt.setInt(3, newStreak);
            stmt.setInt(4, totalClaims + 1);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Fehler beim Speichern des Claims fuer " + uuid + ": " + e.getMessage());
        }

        return reward;
    }

    /**
     * Reward fuer den naechsten (ungeclaimten) Tag im Streak.
     */
    public DailyReward getNextReward(UUID uuid) {
        int streak = getStreak(uuid);
        int nextDay = ((streak) % 30) + 1;
        return plugin.getRewardsConfig().getReward(nextDay);
    }

    /**
     * Welcher Tag waere der naechste Claim (1-30).
     */
    public int getNextDay(UUID uuid) {
        int streak = getStreak(uuid);
        return ((streak) % 30) + 1;
    }

    public void shutdown() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Fehler beim Schliessen der Daily-Datenbankverbindung: " + e.getMessage());
        }
    }
}
