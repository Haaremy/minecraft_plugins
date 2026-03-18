package de.haaremy.hmypaper.commands;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class ComWorlds implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("hmy.worlds")) {
            sender.sendMessage("§cDu hast keine Berechtigung, diesen Befehl zu verwenden.");
            return true;
        }

        // Geladene Welten mit Name → World-Objekt
        Set<String> loadedNames = Bukkit.getWorlds().stream()
                .map(World::getName)
                .collect(Collectors.toSet());

        sender.sendMessage("§8§m                    ");
        sender.sendMessage("§6§lVerfügbare Welten");

        // Geladene Welten mit Details
        for (World world : Bukkit.getWorlds()) {
            String type = switch (world.getEnvironment()) {
                case NETHER  -> "§c☠ Nether";
                case THE_END -> "§5✦ End";
                default      -> "§a⛏ Normal";
            };
            sender.sendMessage(" §a● §e" + world.getName() + " §8| " + type
                    + " §8| §7Spieler: §d" + world.getPlayerCount());
        }

        // Auf Disk vorhandene, aber nicht geladene Welten
        File container = Bukkit.getWorldContainer();
        File[] dirs = container.listFiles(f -> f.isDirectory() && new File(f, "level.dat").exists());
        if (dirs != null) {
            boolean hasUnloaded = false;
            for (File dir : Arrays.stream(dirs)
                    .filter(d -> !loadedNames.contains(d.getName()))
                    .sorted()
                    .toList()) {
                if (!hasUnloaded) {
                    sender.sendMessage("§8§m          §r §7Nicht geladen §8§m          ");
                    hasUnloaded = true;
                }
                sender.sendMessage(" §8● §7" + dir.getName() + " §8(nicht geladen)");
            }
        }

        sender.sendMessage("§8§m                    ");
        return true;
    }
}
