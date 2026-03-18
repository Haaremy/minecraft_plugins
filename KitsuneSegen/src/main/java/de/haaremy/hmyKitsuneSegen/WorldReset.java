package de.haaremy.hmykitsunesegen;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Resets the game world by copying it back from a backup directory.
 *
 * Backup path (from config: world-backup-path) must be a copy of the
 * game world folder made before the server starts.
 */
public class WorldReset {

    private final HmyKitsuneSegen plugin;

    public WorldReset(HmyKitsuneSegen plugin) {
        this.plugin = plugin;
    }

    public void resetWorld() {
        String worldName   = plugin.getGameConfig().getGameWorld();
        String backupPath  = plugin.getGameConfig().getWorldBackupPath();

        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            // Kick remaining players out of the world first
            World hub = Bukkit.getWorld(plugin.getGameConfig().getHubWorld());
            if (hub != null) {
                world.getPlayers().forEach(p -> p.teleport(hub.getSpawnLocation()));
            }
            Bukkit.unloadWorld(world, false);
        }

        File worldFolder  = new File(Bukkit.getWorldContainer(), worldName);
        File backupFolder = new File(backupPath);

        if (!backupFolder.exists()) {
            plugin.getLogger().warning("Backup-Ordner existiert nicht: " + backupPath
                    + " – Welt wird ohne Reset neu geladen.");
        } else {
            deleteFolder(worldFolder);
            try {
                copyFolder(backupFolder.toPath(), worldFolder.toPath());
                plugin.getLogger().info("Welt '" + worldName + "' wurde zurückgesetzt.");
            } catch (IOException e) {
                plugin.getLogger().severe("Fehler beim Zurücksetzen der Welt: " + e.getMessage());
            }
        }

        // Reload world
        Bukkit.createWorld(new WorldCreator(worldName));

        // Re-scan spawn points and chest spots on the fresh world
        World fresh = Bukkit.getWorld(worldName);
        if (fresh != null) {
            plugin.getSpawnPoints().clear();
            plugin.getSpawnPoints().addAll(
                    ChestManager.findBlocks(fresh, plugin.getGameConfig().getSpawnBlock(), true));
            plugin.getChestSpots().clear();
            plugin.getChestSpots().addAll(
                    ChestManager.findBlocks(fresh, plugin.getGameConfig().getChestSpawnBlock(), false));
            plugin.getLogger().info("Welt neu gescannt: "
                    + plugin.getSpawnPoints().size() + " Spawnpunkte, "
                    + plugin.getChestSpots().size() + " Truhenplätze.");
        }
    }

    private void deleteFolder(File folder) {
        if (folder == null || !folder.exists()) return;
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File f : files) deleteFolder(f);
            }
        }
        folder.delete();
    }

    private void copyFolder(Path source, Path target) throws IOException {
        Files.walk(source).forEach(path -> {
            try {
                Path dest = target.resolve(source.relativize(path));
                if (Files.isDirectory(path)) {
                    Files.createDirectories(dest);
                } else {
                    Files.copy(path, dest);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
