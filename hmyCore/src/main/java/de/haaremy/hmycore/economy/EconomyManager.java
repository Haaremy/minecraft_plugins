package de.haaremy.hmycore.economy;

import de.haaremy.hmycore.HmyCore;
import de.haaremy.hmycore.storage.DatabaseManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EconomyManager implements Listener {

    private final HmyCore plugin;
    private final DatabaseManager db;
    private final Map<UUID, Integer> coinCache = new ConcurrentHashMap<>();

    public EconomyManager(HmyCore plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            int coins = loadCoins(uuid);
            coinCache.put(uuid, coins);
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        Integer coins = coinCache.remove(uuid);
        if (coins != null) {
            int finalCoins = coins;
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> saveCoins(uuid, finalCoins));
        }
    }

    private int loadCoins(UUID uuid) {
        DataSource ds = db.getDataSource();
        try (Connection c = ds.getConnection()) {
            try (PreparedStatement stmt = c.prepareStatement("SELECT coins FROM economy WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        long stored = rs.getLong("coins");
                        if (stored > Integer.MAX_VALUE) return Integer.MAX_VALUE;
                        if (stored < Integer.MIN_VALUE) return Integer.MIN_VALUE;
                        return (int) stored;
                    }
                }
            }
            int start = plugin.getConfig().getInt("economy.starting-coins", 0);
            try (PreparedStatement insert = c.prepareStatement(db.upsertEconomySql())) {
                insert.setString(1, uuid.toString());
                insert.setLong(2, start);
                insert.executeUpdate();
            }
            return start;
        } catch (SQLException e) {
            plugin.getLogger().warning("Fehler beim Laden der Coins fuer " + uuid + ": " + e.getMessage());
            return 0;
        }
    }

    private void saveCoins(UUID uuid, int coins) {
        try (Connection c = db.getDataSource().getConnection();
             PreparedStatement stmt = c.prepareStatement(db.upsertEconomySql())) {
            stmt.setString(1, uuid.toString());
            stmt.setLong(2, coins);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Fehler beim Speichern der Coins fuer " + uuid + ": " + e.getMessage());
        }
    }

    public int getCoins(UUID uuid) {
        return coinCache.getOrDefault(uuid, 0);
    }

    public void addCoins(UUID uuid, int amount) {
        coinCache.merge(uuid, amount, Integer::sum);
    }

    public void removeCoins(UUID uuid, int amount) {
        coinCache.merge(uuid, -amount, (oldV, delta) -> Math.max(0, oldV + delta));
    }

    public boolean hasEnough(UUID uuid, int amount) {
        return getCoins(uuid) >= amount;
    }

    public void shutdown() {
        for (Map.Entry<UUID, Integer> entry : coinCache.entrySet()) {
            saveCoins(entry.getKey(), entry.getValue());
        }
        coinCache.clear();
    }
}
