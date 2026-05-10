package de.haaremy.hmycore.stats;

import de.haaremy.hmycore.HmyCore;
import de.haaremy.hmycore.storage.DatabaseManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StatsManager implements Listener {

    private final HmyCore plugin;
    private final DatabaseManager db;
    private final Map<String, PlayerStats> statsCache = new ConcurrentHashMap<>();

    public StatsManager(HmyCore plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
    }

    private String cacheKey(UUID uuid, String gameType) {
        return uuid.toString() + ":" + gameType;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> loadAllStats(uuid));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            saveAllStats(uuid);
            statsCache.entrySet().removeIf(entry -> entry.getKey().startsWith(uuid.toString()));
        });
    }

    private void loadAllStats(UUID uuid) {
        try (Connection c = db.getDataSource().getConnection();
             PreparedStatement stmt = c.prepareStatement("SELECT * FROM stats WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String gameType = rs.getString("game_type");
                    PlayerStats stats = new PlayerStats(uuid, gameType);
                    stats.setWins(rs.getInt("wins"));
                    stats.setLosses(rs.getInt("losses"));
                    stats.setKills(rs.getInt("kills"));
                    stats.setDeaths(rs.getInt("deaths"));
                    stats.setPlaytimeSeconds(rs.getLong("playtime_seconds"));
                    statsCache.put(cacheKey(uuid, gameType), stats);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Fehler beim Laden der Stats fuer " + uuid + ": " + e.getMessage());
        }
    }

    private void saveAllStats(UUID uuid) {
        String prefix = uuid.toString();
        try (Connection c = db.getDataSource().getConnection();
             PreparedStatement stmt = c.prepareStatement(db.upsertStatsSql())) {
            c.setAutoCommit(false);
            int batched = 0;
            for (Map.Entry<String, PlayerStats> entry : statsCache.entrySet()) {
                if (!entry.getKey().startsWith(prefix)) continue;
                PlayerStats s = entry.getValue();
                stmt.setString(1, s.getUuid().toString());
                stmt.setString(2, s.getGameType());
                stmt.setInt(3, s.getWins());
                stmt.setInt(4, s.getLosses());
                stmt.setInt(5, s.getKills());
                stmt.setInt(6, s.getDeaths());
                stmt.setLong(7, s.getPlaytimeSeconds());
                stmt.addBatch();
                batched++;
            }
            if (batched > 0) stmt.executeBatch();
            c.commit();
        } catch (SQLException e) {
            plugin.getLogger().warning("Fehler beim Speichern der Stats fuer " + uuid + ": " + e.getMessage());
        }
    }

    private void saveStatsSingle(PlayerStats stats) {
        try (Connection c = db.getDataSource().getConnection();
             PreparedStatement stmt = c.prepareStatement(db.upsertStatsSql())) {
            stmt.setString(1, stats.getUuid().toString());
            stmt.setString(2, stats.getGameType());
            stmt.setInt(3, stats.getWins());
            stmt.setInt(4, stats.getLosses());
            stmt.setInt(5, stats.getKills());
            stmt.setInt(6, stats.getDeaths());
            stmt.setLong(7, stats.getPlaytimeSeconds());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Fehler beim Speichern der Stats: " + e.getMessage());
        }
    }

    public PlayerStats getStats(UUID uuid, String gameType) {
        String key = cacheKey(uuid, gameType);
        return statsCache.computeIfAbsent(key, k -> new PlayerStats(uuid, gameType));
    }

    public void addWin(UUID uuid, String gameType) { getStats(uuid, gameType).addWin(); }
    public void addLoss(UUID uuid, String gameType) { getStats(uuid, gameType).addLoss(); }
    public void addKill(UUID uuid, String gameType) { getStats(uuid, gameType).addKill(); }
    public void addDeath(UUID uuid, String gameType) { getStats(uuid, gameType).addDeath(); }
    public void addPlaytime(UUID uuid, String gameType, long seconds) { getStats(uuid, gameType).addPlaytime(seconds); }

    public void shutdown() {
        for (PlayerStats stats : statsCache.values()) {
            saveStatsSingle(stats);
        }
        statsCache.clear();
    }
}
