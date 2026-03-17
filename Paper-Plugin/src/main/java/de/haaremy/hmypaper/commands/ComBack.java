package de.haaremy.hmypaper.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ComBack implements CommandExecutor, Listener {

    private final Map<UUID, Location> lastLocations = new HashMap<>();

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        // Speichere Position vor dem Teleport (nicht bei /back selbst)
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.COMMAND
                || event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN) {
            lastLocations.put(event.getPlayer().getUniqueId(), event.getFrom().clone());
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        lastLocations.put(player.getUniqueId(), player.getLocation().clone());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("hmy.back")) {
            sender.sendMessage("§cDu hast keine Berechtigung, diesen Befehl zu verwenden.");
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cNur Spieler können diesen Befehl ausführen.");
            return true;
        }

        Location last = lastLocations.get(player.getUniqueId());
        if (last == null) {
            sender.sendMessage("§cKeine vorherige Position gespeichert.");
            return true;
        }

        player.teleport(last);
        sender.sendMessage("§aDu wurdest zu deiner letzten Position teleportiert.");
        return true;
    }
}
