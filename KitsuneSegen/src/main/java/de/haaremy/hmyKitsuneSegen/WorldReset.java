package de.haaremy.hmykitsunesegen;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class WorldReset extends JavaPlugin {
    private final HmyKitsuneSegen plugin;
    private final String gameworld;

    private WorldReset(HmyKitsuneSegen plugin, String gameworld){
        this.plugin = plugin;
        this.gameworld = gameworld;
    }


    private void kickPlayers() {
        String kickMessage = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.kick_message"));
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.kickPlayer(kickMessage);
        }
    }

    private void resetWorlds(String gameworld) {
        String backupFolder = getConfig().getString("backup_folder");
        String worldFolder = getConfig().getString(gameworld);

        for (World world : Bukkit.getWorlds()) {
            String worldName = world.getName();
            File worldFile = new File(worldFolder, worldName);

            // Entlade die Welt
            Bukkit.unloadWorld(world, false);

            // Lösche die Welt
            deleteFolder(worldFile);

            // Kopiere das Backup zurück
            File backupFile = new File(backupFolder, worldName);
            try {
                copyFolder(backupFile.toPath(), worldFile.toPath());
                getLogger().info("Welt " + worldName + " wurde zurückgesetzt.");
            } catch (IOException e) {
                getLogger().severe("Fehler beim Zurücksetzen der Welt " + worldName + ": " + e.getMessage());
            }

            // Lade die Welt neu
            Bukkit.createWorld(new org.bukkit.WorldCreator(worldName));
        }
    }

    private void deleteFolder(File folder) {
        if (folder.isDirectory()) {
            for (File file : folder.listFiles()) {
                deleteFolder(file);
            }
        }
        folder.delete();
    }

    private void copyFolder(Path source, Path target) throws IOException {
        Files.walk(source).forEach(path -> {
            try {
                Files.copy(path, target.resolve(source.relativize(path)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
