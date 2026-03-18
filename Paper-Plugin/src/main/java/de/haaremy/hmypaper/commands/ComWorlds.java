package de.haaremy.hmypaper.commands;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ComWorlds implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("hmy.worlds")) {
            sender.sendMessage("§cDu hast keine Berechtigung, diesen Befehl zu verwenden.");
            return true;
        }

        sender.sendMessage("§8§m                    ");
        sender.sendMessage("§6§lVerfügbare Welten");
        for (World world : Bukkit.getWorlds()) {
            String type = switch (world.getEnvironment()) {
                case NETHER     -> "§c☠ Nether";
                case THE_END    -> "§5✦ End";
                default         -> "§a⛏ Normal";
            };
            sender.sendMessage(" §7» §e" + world.getName() + " §8| " + type
                    + " §8| §7Spieler: §d" + world.getPlayerCount());
        }
        sender.sendMessage("§8§m                    ");
        return true;
    }
}
