package de.haaremy.hmypaper.commands;

import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import de.haaremy.hmypaper.HmyLanguageManager;
import de.haaremy.hmypaper.utils.PermissionUtils;
import net.kyori.adventure.text.Component;

public class ComWorld implements CommandExecutor {

    private final HmyLanguageManager languageManager;

    public ComWorld(HmyLanguageManager languageManager) {
        this.languageManager = languageManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Überprüfen, ob der Befehl von einem Spieler kommt
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text(languageManager.getMessage("l_player_only", "Dieser Befehl kann nur von einem Spieler ausgeführt werden.")));
            return true;
        }

        Player player = (Player) sender;


        String world = args[0].toLowerCase();

        

        // Unterstützte Sprachen prüfen
        // Liste aller existierenden Welten
        List<String> worlds = Bukkit.getWorlds().stream()
            .map(World::getName)
            .map(String::toLowerCase)
            .collect(Collectors.toList());

        if(world.contains("list")){
            worlds.forEach(existent -> sender.sendMessage(Component.text("- " + existent)));
            return true;
        }
            
        if (!worlds.contains(world)) {
            player.sendMessage(Component.text("World not found."));
            return true;
        }

        if (PermissionUtils.hasPermission(player,"hmy.world."+world)) {
            org.bukkit.World targetWorld = Bukkit.getWorld(world);
            if (targetWorld == null) {
                player.sendMessage(Component.text("The specified world is not loaded."));
                return true;
            }
            Location location = targetWorld.getSpawnLocation();
            player.teleport(location);
        }

        return true;
    }
}
