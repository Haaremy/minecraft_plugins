package de.haaremy.hmypaper;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ComTime implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("hmy.time")) {
            sender.sendMessage("§cDu hast keine Berechtigung, diesen Befehl zu verwenden.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("§cVerwendung: /time [number]");
            return true;
        }

        try {
            long time = Long.parseLong(args[0]);
            World world = Bukkit.getWorlds().get(0); // Standardwelt
            world.setTime(time);
            sender.sendMessage("§aZeit auf " + time + " gesetzt.");
        } catch (NumberFormatException e) {
            sender.sendMessage("§cUngültige Zahl.");
        }
        return true;
    }
}
