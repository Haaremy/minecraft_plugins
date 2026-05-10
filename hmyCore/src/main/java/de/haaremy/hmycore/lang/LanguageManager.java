package de.haaremy.hmycore.lang;

import de.haaremy.hmycore.HmyCore;
import de.haaremy.hmycore.storage.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LanguageManager implements Listener {

    public static final String DEFAULT_LOCALE = "de";
    public static final String FALLBACK_LOCALE = "en";
    public static final List<String> SUPPORTED = List.of("de", "en");

    private final HmyCore plugin;
    private final Map<String, Properties> bundles = new HashMap<>();
    private final Map<UUID, String> cache = new ConcurrentHashMap<>();

    public LanguageManager(HmyCore plugin) {
        this.plugin = plugin;
    }

    public void init() {
        ensureSchema();
        loadBundles();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        for (Player p : Bukkit.getOnlinePlayers()) {
            asyncLoadFromDb(p);
        }
    }

    private void ensureSchema() {
        DatabaseManager dbm = plugin.getDatabaseManager();
        if (dbm == null) return;
        String ddl = dbm.getBackend() == DatabaseManager.Backend.MARIADB
                ? "CREATE TABLE IF NOT EXISTS player_prefs ("
                        + "uuid CHAR(36) NOT NULL,"
                        + "language VARCHAR(4) NOT NULL DEFAULT 'de',"
                        + "stats_on TINYINT(1) NOT NULL DEFAULT 0,"
                        + "flags LONGTEXT NULL,"
                        + "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP "
                        + "ON UPDATE CURRENT_TIMESTAMP,"
                        + "PRIMARY KEY (uuid)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
                : "CREATE TABLE IF NOT EXISTS player_prefs ("
                        + "uuid TEXT PRIMARY KEY,"
                        + "language TEXT NOT NULL DEFAULT 'de',"
                        + "stats_on INTEGER NOT NULL DEFAULT 0,"
                        + "flags TEXT NULL"
                        + ")";
        try (Connection c = dbm.getDataSource().getConnection();
             Statement s = c.createStatement()) {
            s.executeUpdate(ddl);
        } catch (SQLException ex) {
            plugin.getLogger().warning("LanguageManager: player_prefs DDL fehlgeschlagen: " + ex.getMessage());
        }
    }

    public void loadBundles() {
        bundles.clear();
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }
        for (String lang : SUPPORTED) {
            String resource = "lang/messages_" + lang + ".properties";
            File f = new File(langDir, "messages_" + lang + ".properties");
            if (!f.exists()) {
                try {
                    plugin.saveResource(resource, false);
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("Default-Resource " + resource + " fehlt im JAR: " + ex.getMessage());
                }
            }
            Properties p = new Properties();
            // JAR-Resource als Base: garantiert, dass neue Keys nach einem
            // Plugin-Update sofort verfuegbar sind, auch wenn die Disk-Datei
            // aus einer aelteren Version stammt.
            try (java.io.InputStream is = plugin.getResource(resource)) {
                if (is != null) {
                    try (InputStreamReader r = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                        p.load(r);
                    }
                }
            } catch (IOException ex) {
                plugin.getLogger().warning("Konnte JAR-Resource " + resource + " nicht laden: " + ex.getMessage());
            }
            if (f.exists()) {
                try (InputStreamReader r = new InputStreamReader(
                        Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
                    p.load(r);
                } catch (IOException ex) {
                    plugin.getLogger().warning("Konnte messages_" + lang + ".properties nicht laden: " + ex.getMessage());
                }
            }
            bundles.put(lang, p);
        }
    }

    public String getLocale(UUID uuid) {
        return cache.getOrDefault(uuid, DEFAULT_LOCALE);
    }

    public void setLocale(UUID uuid, String locale) {
        if (!SUPPORTED.contains(locale)) {
            return;
        }
        cache.put(uuid, locale);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> persistLocale(uuid, locale));
    }

    public String resolve(String locale, String key) {
        Properties p = bundles.get(locale);
        String val = p != null ? p.getProperty(key) : null;
        if (val == null) {
            Properties en = bundles.get(FALLBACK_LOCALE);
            if (en != null) val = en.getProperty(key);
        }
        if (val == null) {
            Properties de = bundles.get(DEFAULT_LOCALE);
            if (de != null) val = de.getProperty(key);
        }
        return val != null ? val : key;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (!cache.containsKey(p.getUniqueId())) {
            cache.put(p.getUniqueId(), clientLocaleHint(p));
        }
        asyncLoadFromDb(p);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        cache.remove(e.getPlayer().getUniqueId());
    }

    private String clientLocaleHint(Player p) {
        try {
            Locale loc = p.locale();
            if (loc != null) {
                String l = loc.getLanguage();
                if (SUPPORTED.contains(l.toLowerCase(Locale.ROOT))) {
                    return l.toLowerCase(Locale.ROOT);
                }
            }
        } catch (Throwable ignored) {
            // Spieler-Locale kann beim sehr frühen Join NULL sein
        }
        return DEFAULT_LOCALE;
    }

    private void asyncLoadFromDb(Player p) {
        if (plugin.getDatabaseManager() == null) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String dbLocale = readLocale(p.getUniqueId());
            if (dbLocale != null && SUPPORTED.contains(dbLocale)) {
                cache.put(p.getUniqueId(), dbLocale);
            } else {
                String current = cache.getOrDefault(p.getUniqueId(), DEFAULT_LOCALE);
                persistLocale(p.getUniqueId(), current);
            }
        });
    }

    private String readLocale(UUID uuid) {
        String sql = "SELECT language FROM player_prefs WHERE uuid = ?";
        try (Connection c = plugin.getDatabaseManager().getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (SQLException ex) {
            plugin.getLogger().warning("readLocale fehlgeschlagen: " + ex.getMessage());
        }
        return null;
    }

    private void persistLocale(UUID uuid, String locale) {
        DatabaseManager dbm = plugin.getDatabaseManager();
        if (dbm == null) return;
        String sql = dbm.getBackend() == DatabaseManager.Backend.MARIADB
                ? "INSERT INTO player_prefs (uuid, language) VALUES (?, ?) "
                        + "ON DUPLICATE KEY UPDATE language = VALUES(language)"
                : "INSERT INTO player_prefs (uuid, language) VALUES (?, ?) "
                        + "ON CONFLICT(uuid) DO UPDATE SET language = excluded.language";
        try (Connection c = dbm.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, locale);
            ps.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().warning("persistLocale fehlgeschlagen: " + ex.getMessage());
        }
    }
}
