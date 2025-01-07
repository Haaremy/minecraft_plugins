package de.haaremy.hmypaper;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ComSocialSpy implements CommandExecutor {

    private final Set<Player> spyingPlayers = new HashSet<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cNur Spieler können diesen Befehl ausführen.");
            return true;
        }

        Player player = (Player) sender;

        if (spyingPlayers.contains(player)) {
            spyingPlayers.remove(player);
            player.sendMessage("§eSocial Spy wurde deaktiviert.");
        } else {
            spyingPlayers.add(player);
            player.sendMessage("§aSocial Spy wurde aktiviert.");
        }
        return true;
    }

    public boolean isSpying(Player player) {
        return spyingPlayers.contains(player);
    }

    public void logPrivateMessage(Player sender, Player recipient, String message) {
        for (Player spy : spyingPlayers) {
            if (!spy.equals(sender) && !spy.equals(recipient)) {
                spy.sendMessage("§7[Spy] " + sender.getName() + " -> " + recipient.getName() + ": §f" + message);
            }
        }
    }
}
