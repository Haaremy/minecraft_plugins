package de.haaremy.hmycore.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.haaremy.hmycore.HmyCore;
import org.bukkit.configuration.ConfigurationSection;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    public enum Backend { MARIADB, SQLITE }

    private final HmyCore plugin;
    private HikariDataSource dataSource;
    private Backend backend;

    public DatabaseManager(HmyCore plugin) {
        this.plugin = plugin;
    }

    public void init() {
        ConfigurationSection cfg = plugin.getConfig().getConfigurationSection("storage");
        String type = cfg != null ? cfg.getString("type", "mariadb") : "mariadb";

        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName("hmyCore-Pool");
        hikari.setMaximumPoolSize(cfg != null ? cfg.getInt("pool-size", 8) : 8);
        hikari.setMinimumIdle(2);
        hikari.setConnectionTimeout(10_000);
        hikari.setIdleTimeout(600_000);
        hikari.setMaxLifetime(1_800_000);

        if ("mariadb".equalsIgnoreCase(type) || "mysql".equalsIgnoreCase(type)) {
            this.backend = Backend.MARIADB;
            String host = cfg.getString("host", "127.0.0.1");
            int port = cfg.getInt("port", 3306);
            String database = cfg.getString("database", "hmycore");
            String user = cfg.getString("user", "hmycore");
            String password = cfg.getString("password", "");
            hikari.setJdbcUrl("jdbc:mariadb://" + host + ":" + port + "/" + database
                    + "?useUnicode=true&characterEncoding=utf8&autoReconnect=true");
            hikari.setUsername(user);
            hikari.setPassword(password);
            hikari.setDriverClassName("org.mariadb.jdbc.Driver");
        } else {
            this.backend = Backend.SQLITE;
            java.io.File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) dataFolder.mkdirs();
            hikari.setJdbcUrl("jdbc:sqlite:" + new java.io.File(dataFolder, "data.db").getAbsolutePath());
            hikari.setMaximumPoolSize(1);
            hikari.setMinimumIdle(1);
            hikari.setDriverClassName("org.sqlite.JDBC");
        }

        try {
            this.dataSource = new HikariDataSource(hikari);
            try (Connection c = dataSource.getConnection()) {
                plugin.getLogger().info("hmyCore DB-Verbindung (" + backend + ") OK: "
                        + c.getMetaData().getDatabaseProductName() + " "
                        + c.getMetaData().getDatabaseProductVersion());
            }
            applySchema();
        } catch (SQLException ex) {
            plugin.getLogger().severe("DatabaseManager konnte nicht initialisiert werden: " + ex.getMessage());
            throw new IllegalStateException("hmyCore DB-Init fehlgeschlagen", ex);
        }
    }

    private void applySchema() throws SQLException {
        String economyDdl;
        String statsDdl;
        if (backend == Backend.MARIADB) {
            economyDdl = "CREATE TABLE IF NOT EXISTS economy (" +
                    "uuid CHAR(36) NOT NULL," +
                    "coins BIGINT NOT NULL DEFAULT 0," +
                    "shards BIGINT NOT NULL DEFAULT 0," +
                    "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                    "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                    "PRIMARY KEY (uuid)," +
                    "INDEX idx_updated (updated_at)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
            statsDdl = "CREATE TABLE IF NOT EXISTS stats (" +
                    "uuid VARCHAR(36) NOT NULL," +
                    "game_type VARCHAR(32) NOT NULL," +
                    "wins INT NOT NULL DEFAULT 0," +
                    "losses INT NOT NULL DEFAULT 0," +
                    "kills INT NOT NULL DEFAULT 0," +
                    "deaths INT NOT NULL DEFAULT 0," +
                    "playtime_seconds BIGINT NOT NULL DEFAULT 0," +
                    "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                    "PRIMARY KEY (uuid, game_type)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        } else {
            economyDdl = "CREATE TABLE IF NOT EXISTS economy (" +
                    "uuid TEXT PRIMARY KEY, coins INTEGER NOT NULL DEFAULT 0, shards INTEGER NOT NULL DEFAULT 0)";
            statsDdl = "CREATE TABLE IF NOT EXISTS stats (" +
                    "uuid TEXT NOT NULL, game_type TEXT NOT NULL," +
                    "wins INTEGER NOT NULL DEFAULT 0, losses INTEGER NOT NULL DEFAULT 0," +
                    "kills INTEGER NOT NULL DEFAULT 0, deaths INTEGER NOT NULL DEFAULT 0," +
                    "playtime_seconds INTEGER NOT NULL DEFAULT 0," +
                    "PRIMARY KEY (uuid, game_type))";
        }
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            s.executeUpdate(economyDdl);
            s.executeUpdate(statsDdl);
        }
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public Backend getBackend() {
        return backend;
    }

    public String upsertEconomySql() {
        return backend == Backend.MARIADB
                ? "INSERT INTO economy (uuid, coins) VALUES (?, ?) ON DUPLICATE KEY UPDATE coins = VALUES(coins)"
                : "INSERT OR REPLACE INTO economy (uuid, coins) VALUES (?, ?)";
    }

    public String upsertStatsSql() {
        return backend == Backend.MARIADB
                ? "INSERT INTO stats (uuid, game_type, wins, losses, kills, deaths, playtime_seconds) " +
                  "VALUES (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
                  "wins=VALUES(wins), losses=VALUES(losses), kills=VALUES(kills), " +
                  "deaths=VALUES(deaths), playtime_seconds=VALUES(playtime_seconds)"
                : "INSERT OR REPLACE INTO stats (uuid, game_type, wins, losses, kills, deaths, playtime_seconds) " +
                  "VALUES (?, ?, ?, ?, ?, ?, ?)";
    }

    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
