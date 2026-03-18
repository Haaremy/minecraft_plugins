package de.haaremy.hmypaper.commands;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import de.haaremy.hmypaper.utils.PermissionUtils;

public class ComWorld implements CommandExecutor, TabCompleter {

    public ComWorld() {}

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cDieser Befehl kann nur von einem Spieler ausgeführt werden.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§cVerwendung: /world <Name>");
            return true;
        }

        String worldName = args[0];

        if (!PermissionUtils.hasPermission(player, "hmy.world." + worldName)) {
            player.sendMessage("§cDu hast keine Berechtigung für die Welt §e" + worldName + "§c.");
            return true;
        }

        // Welt bereits geladen?
        World target = Bukkit.getWorld(worldName);
        if (target == null) {
            // Prüfen ob Welt auf Disk existiert
            File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
            if (!worldFolder.isDirectory() || !new File(worldFolder, "level.dat").exists()) {
                player.sendMessage("§cWelt §e" + worldName + " §cexistiert nicht.");
                return true;
            }
            // Laden
            player.sendMessage("§7Welt §e" + worldName + " §7wird geladen...");
            target = new WorldCreator(worldName).createWorld();
            if (target == null) {
                player.sendMessage("§cDie Welt konnte nicht geladen werden.");
                return true;
            }
        }

        player.teleport(target.getSpawnLocation());
        player.sendMessage("§aDu wurdest zur Welt §e" + target.getName() + " §ateleportiert.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return List.of();
        String input = args[0].toLowerCase();

        // Geladene Welten
        List<String> result = Bukkit.getWorlds().stream()
                .map(World::getName)
                .filter(n -> n.toLowerCase().startsWith(input))
                .collect(Collectors.toList());

        // Nicht geladene Welten vom Disk
        File[] dirs = Bukkit.getWorldContainer().listFiles(
                f -> f.isDirectory() && new File(f, "level.dat").exists());
        if (dirs != null) {
            Arrays.stream(dirs)
                    .map(File::getName)
                    .filter(n -> n.toLowerCase().startsWith(input))
                    .filter(n -> result.stream().noneMatch(n::equalsIgnoreCase))
                    .forEach(result::add);
        }
        return result;
    }
}
