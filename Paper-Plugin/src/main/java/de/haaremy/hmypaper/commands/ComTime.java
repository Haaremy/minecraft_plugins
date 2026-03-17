package de.haaremy.hmypaper.commands;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

public class ComTime implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("hmy.time")) {
            sender.sendMessage("§cDu hast keine Berechtigung, diesen Befehl zu verwenden.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("§cVerwendung: /time <day|night|Zahl>");
            return true;
        }

        World world = Bukkit.getWorlds().get(0);
        long time;

        switch (args[0].toLowerCase()) {
            case "day":   time = 1000;  break;
            case "noon":  time = 6000;  break;
            case "night": time = 13000; break;
            case "midnight": time = 18000; break;
            default:
                try {
                    time = Long.parseLong(args[0]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cUngültige Zeit. Nutze: day, night oder eine Zahl (0–24000).");
                    return true;
                }
        }

        world.setTime(time);
        sender.sendMessage("§aZeit auf §e" + args[0] + " §a(" + time + ") §agesetzt.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("day", "noon", "night", "midnight");
        }
        return List.of();
    }
}