package de.haaremy.hmycore.leaderboard;

import de.haaremy.hmycore.HmyCore;
import de.haaremy.hmycore.storage.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Liefert Top-N-Listen aus der gemeinsamen MariaDB / SQLite. Ergebnisse werden
 * pro (metric, gameType) fuer eine kurze TTL gecacht; abgelaufene Eintraege
 * werden lazy asynchron erneuert, der Read-Path liefert weiterhin den letzten
 * gueltigen Stand. Damit bleibt der Main-Thread frei, auch wenn die DB einen
 * kurzen Hiccup hat.
 */
public class LeaderboardManager {

    public enum Metric {
        COINS,
        WINS,
        LOSSES,
        KILLS,
        DEATHS,
        KD,
        WINRATE,
        PLAYTIME;

        public static Metric fromString(String s) {
            if (s == null) return null;
            try {
                return Metric.valueOf(s.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }

        public boolean isCoinMetric() {
            return this == COINS;
        }
    }

    public static final Set<String> METRIC_NAMES = Set.of(
            "coins", "wins", "losses", "kills", "deaths", "kd", "winrate", "playtime");

    public static final long DEFAULT_TTL_MILLIS = 60_000L;
    public static final int MAX_LIMIT = 50;
    public static final int DEFAULT_LIMIT = 10;

    private static final class CacheEntry {
        final List<LeaderboardEntry> entries;
        final long timestamp;
        final AtomicBoolean refreshing;

        CacheEntry(List<LeaderboardEntry> entries, long timestamp) {
            this.entries = entries;
            this.timestamp = timestamp;
            this.refreshing = new AtomicBoolean(false);
        }
    }

    private final HmyCore plugin;
    private final DatabaseManager db;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final long ttlMillis;

    public LeaderboardManager(HmyCore plugin) {
        this(plugin, DEFAULT_TTL_MILLIS);
    }

    public LeaderboardManager(HmyCore plugin, long ttlMillis) {
        this.plugin = plugin;
        this.db = plugin != null ? plugin.getDatabaseManager() : null;
        this.ttlMillis = ttlMillis;
    }

    /**
     * Liefert den letzten gecachten Stand sofort zurueck und triggert bei
     * abgelaufener TTL eine asynchrone Aktualisierung. Beim allerersten
     * Aufruf existiert noch kein Cache; in dem Fall wird {@code null}
     * zurueckgegeben und die Aktualisierung parallel gestartet.
     */
    public List<LeaderboardEntry> getTopCached(Metric metric, String gameType, int limit) {
        if (metric == null) return Collections.emptyList();
        int safeLimit = clampLimit(limit);
        String key = cacheKey(metric, gameType);
        CacheEntry entry = cache.get(key);
        long now = System.currentTimeMillis();
        if (entry == null || now - entry.timestamp > ttlMillis) {
            triggerRefresh(metric, gameType, safeLimit, key, entry);
        }
        if (entry == null) return null;
        if (entry.entries.size() <= safeLimit) {
            return entry.entries;
        }
        return Collections.unmodifiableList(entry.entries.subList(0, safeLimit));
    }

    /**
     * Synchroner Datenbank-Abruf. NICHT auf dem Main-Thread aufrufen.
     * Wird sowohl von {@link #getTopCached} (async) als auch direkt von Tests
     * benutzt.
     */
    public List<LeaderboardEntry> queryTop(Metric metric, String gameType, int limit) throws SQLException {
        if (metric == null) return Collections.emptyList();
        int safeLimit = clampLimit(limit);
        if (db == null || db.getDataSource() == null) return Collections.emptyList();

        String sql = buildSql(metric);
        DataSource ds = db.getDataSource();
        List<LeaderboardEntry> result = new ArrayList<>(safeLimit);
        try (Connection c = ds.getConnection();
             PreparedStatement stmt = c.prepareStatement(sql)) {
            int paramIndex = 1;
            if (!metric.isCoinMetric()) {
                stmt.setString(paramIndex++, gameType != null ? gameType : "");
            }
            stmt.setInt(paramIndex, safeLimit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String uuidStr = rs.getString("uuid");
                    if (uuidStr == null) continue;
                    UUID uuid;
                    try {
                        uuid = UUID.fromString(uuidStr);
                    } catch (IllegalArgumentException ex) {
                        continue;
                    }
                    double score = rs.getDouble("score");
                    String name = resolveName(uuid);
                    result.add(new LeaderboardEntry(uuid, name, score));
                }
            }
        }
        return result;
    }

    private void triggerRefresh(Metric metric, String gameType, int limit,
                                String key, CacheEntry existing) {
        if (plugin == null || !plugin.isEnabled()) return;
        if (existing != null && !existing.refreshing.compareAndSet(false, true)) return;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                List<LeaderboardEntry> fresh = queryTop(metric, gameType, MAX_LIMIT);
                cache.put(key, new CacheEntry(Collections.unmodifiableList(fresh), System.currentTimeMillis()));
            } catch (SQLException ex) {
                plugin.getLogger().warning("Leaderboard-Refresh fehlgeschlagen ("
                        + key + "): " + ex.getMessage());
                if (existing != null) existing.refreshing.set(false);
            }
        });
    }

    private String resolveName(UUID uuid) {
        try {
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            String n = op != null ? op.getName() : null;
            if (n != null && !n.isEmpty()) return n;
        } catch (Throwable ignored) {
            // In Tests laeuft kein Bukkit-Server — fallback auf Kurz-UUID.
        }
        String s = uuid.toString();
        return s.length() >= 8 ? s.substring(0, 8) : s;
    }

    public static int clampLimit(int limit) {
        if (limit < 1) return 1;
        if (limit > MAX_LIMIT) return MAX_LIMIT;
        return limit;
    }

    static String cacheKey(Metric metric, String gameType) {
        if (metric.isCoinMetric()) return metric.name();
        return metric.name() + ":" + (gameType == null ? "" : gameType.toLowerCase(Locale.ROOT));
    }

    /**
     * Baut die SQL-Query. ACHTUNG: Spaltennamen kommen aus der Metric-Enum,
     * nicht aus Spielereingaben. gameType bleibt parametrisiert.
     */
    static String buildSql(Metric metric) {
        switch (metric) {
            case COINS:
                return "SELECT uuid, coins AS score FROM economy "
                        + "ORDER BY coins DESC LIMIT ?";
            case WINS:
                return "SELECT uuid, wins AS score FROM stats "
                        + "WHERE game_type = ? ORDER BY wins DESC LIMIT ?";
            case LOSSES:
                return "SELECT uuid, losses AS score FROM stats "
                        + "WHERE game_type = ? ORDER BY losses DESC LIMIT ?";
            case KILLS:
                return "SELECT uuid, kills AS score FROM stats "
                        + "WHERE game_type = ? ORDER BY kills DESC LIMIT ?";
            case DEATHS:
                return "SELECT uuid, deaths AS score FROM stats "
                        + "WHERE game_type = ? ORDER BY deaths DESC LIMIT ?";
            case PLAYTIME:
                return "SELECT uuid, playtime_seconds AS score FROM stats "
                        + "WHERE game_type = ? ORDER BY playtime_seconds DESC LIMIT ?";
            case KD:
                // Wenn deaths=0 wird kills genutzt (entspricht PlayerStats.getKdr).
                return "SELECT uuid, "
                        + "(CASE WHEN deaths = 0 THEN CAST(kills AS DOUBLE) "
                        + "ELSE CAST(kills AS DOUBLE) / deaths END) AS score "
                        + "FROM stats WHERE game_type = ? AND (kills + deaths) > 0 "
                        + "ORDER BY score DESC LIMIT ?";
            case WINRATE:
                return "SELECT uuid, "
                        + "(CAST(wins AS DOUBLE) * 100 / (wins + losses)) AS score "
                        + "FROM stats WHERE game_type = ? AND (wins + losses) > 0 "
                        + "ORDER BY score DESC LIMIT ?";
            default:
                throw new IllegalArgumentException("Unsupported metric: " + metric);
        }
    }

    public void invalidate() {
        cache.clear();
    }

    public int cacheSize() {
        return cache.size();
    }
}
