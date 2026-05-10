package de.haaremy.hmycore.storage;

import de.haaremy.hmycore.HmyCore;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Einmalige Migration bestehender SQLite-Daten in die neue MariaDB.
 * Verschiebt data.db nach data.db.migrated-<ts>, wenn fertig, damit kein
 * Re-Import stattfindet.
 */
public final class LegacySqliteMigrator {

    private LegacySqliteMigrator() {}

    public static void migrate(HmyCore plugin) {
        DatabaseManager db = plugin.getDatabaseManager();
        if (db.getBackend() != DatabaseManager.Backend.MARIADB) return;

        File dataFolder = plugin.getDataFolder();
        File legacy = new File(dataFolder, "data.db");
        if (!legacy.exists() || legacy.length() == 0) return;

        plugin.getLogger().info("Legacy-SQLite data.db gefunden - starte Einmal-Migration nach MariaDB...");
        int economyMigrated = 0;
        int statsMigrated = 0;
        String sqliteUrl = "jdbc:sqlite:" + legacy.getAbsolutePath();
        try (Connection src = DriverManager.getConnection(sqliteUrl);
             Connection dst = db.getDataSource().getConnection()) {

            dst.setAutoCommit(false);
            try (PreparedStatement selE = src.prepareStatement("SELECT uuid, coins FROM economy");
                 ResultSet rs = selE.executeQuery();
                 PreparedStatement insE = dst.prepareStatement(db.upsertEconomySql())) {
                while (rs.next()) {
                    insE.setString(1, rs.getString("uuid"));
                    insE.setLong(2, rs.getLong("coins"));
                    insE.addBatch();
                    economyMigrated++;
                }
                insE.executeBatch();
            } catch (SQLException ex) {
                plugin.getLogger().warning("Economy-Migration fehlgeschlagen (Legacy-Tabelle evtl. leer): " + ex.getMessage());
            }

            try (PreparedStatement selS = src.prepareStatement(
                    "SELECT uuid, game_type, wins, losses, kills, deaths, playtime_seconds FROM stats");
                 ResultSet rs = selS.executeQuery();
                 PreparedStatement insS = dst.prepareStatement(db.upsertStatsSql())) {
                while (rs.next()) {
                    insS.setString(1, rs.getString("uuid"));
                    insS.setString(2, rs.getString("game_type"));
                    insS.setInt(3, rs.getInt("wins"));
                    insS.setInt(4, rs.getInt("losses"));
                    insS.setInt(5, rs.getInt("kills"));
                    insS.setInt(6, rs.getInt("deaths"));
                    insS.setLong(7, rs.getLong("playtime_seconds"));
                    insS.addBatch();
                    statsMigrated++;
                }
                insS.executeBatch();
            } catch (SQLException ex) {
                plugin.getLogger().warning("Stats-Migration fehlgeschlagen (Legacy-Tabelle evtl. leer): " + ex.getMessage());
            }

            dst.commit();
        } catch (SQLException ex) {
            plugin.getLogger().severe("Legacy-Migration konnte nicht oeffnen: " + ex.getMessage());
            return;
        }

        File archive = new File(dataFolder, "data.db.migrated-" + System.currentTimeMillis());
        if (legacy.renameTo(archive)) {
            plugin.getLogger().info("Legacy-Migration fertig: " + economyMigrated + " Economy, "
                    + statsMigrated + " Stats. data.db verschoben nach " + archive.getName());
        } else {
            plugin.getLogger().warning("Legacy-Migration fertig, aber data.db konnte nicht umbenannt werden. "
                    + "Bitte manuell nach " + archive.getName() + " verschieben, um Re-Migration zu vermeiden.");
        }
    }
}
