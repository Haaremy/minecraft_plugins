package de.haaremy.hmypaper;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ComSpawn implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cDieser Befehl kann nur von Spielern ausgeführt werden.");
            return true;
        }

        Player player = (Player) sender;
        World world = player.getWorld();
        Location spawnLocation = world.getSpawnLocation(); // Weltspawn

        player.teleport(spawnLocation); // Teleportiere den Spieler zum Spawn
        player.sendMessage("§eDu wurdest zum Spawn teleportiert!");
        return true;
    }
}
