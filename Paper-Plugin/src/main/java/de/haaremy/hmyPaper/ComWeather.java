package de.haaremy.hmypaper;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ComWeather implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("hmy.weather")) {
            sender.sendMessage("§cDu hast keine Berechtigung, diesen Befehl zu verwenden.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("§cVerwendung: /weather [sun|storm|rain] [duration]");
            return true;
        }

        World world = Bukkit.getWorlds().get(0); // Standardwelt
        String weatherType = args[0].toLowerCase();
        int duration = args.length > 1 ? Integer.parseInt(args[1]) * 20 : 6000; // Standard 5 Minuten

        switch (weatherType) {
            case "sun":
                world.setStorm(false);
                world.setThundering(false);
                sender.sendMessage("§aWetter geändert zu Sonnenschein.");
                break;
            case "storm":
                world.setStorm(true);
                world.setThundering(true);
                sender.sendMessage("§aWetter geändert zu Gewitter.");
                break;
            case "rain":
                world.setStorm(true);
                world.setThundering(false);
                sender.sendMessage("§aWetter geändert zu Regen.");
                break;
            default:
                sender.sendMessage("§cUngültiges Wetter.");
        }

        world.setWeatherDuration(duration);
        return true;
    }
}
