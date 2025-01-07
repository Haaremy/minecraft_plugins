package de.haaremy.hmyvelocityplugin;

import java.util.Optional;
import java.util.UUID;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import net.kyori.adventure.text.Component;

public class BanC implements SimpleCommand {

    private final ProxyServer server;

    public BanC(ProxyServer server) {
        this.server = server;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length < 2) {
            source.sendMessage(Component.text("Usage: /ban <player> <reason>"));
            return;
        }

        String playerName = args[0];
        String reason = String.join(" ", args).substring(playerName.length() + 1);

        Optional<Player> targetPlayer = server.getPlayer(playerName);

        if (targetPlayer.isPresent()) {
            Player player = targetPlayer.get();
            UUID playerUUID = player.getUniqueId();

            // Ban-Logik: Speichere den Ban in einer Datei oder Datenbank
            saveBan(playerUUID, reason);

            // Spieler trennen
            player.disconnect(Component.text("You have been banned: " + reason));
            source.sendMessage(Component.text(player.getUsername() + " has been banned for: " + reason));
        } else {
            source.sendMessage(Component.text("Player not found."));
        }
    }

    private void saveBan(UUID playerUUID, String reason) {
        // Speichere den Ban in einer Datei oder Datenbank
        // Diese Funktion muss entsprechend implementiert werden
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("hmy.ban");
    }
}
