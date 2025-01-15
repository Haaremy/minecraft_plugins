package de.haaremy.hmyvelocityplugin;

import java.util.Optional;
import java.util.UUID;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import de.haaremy.hmyvelocityplugin.utils.PermissionUtils;
import net.kyori.adventure.text.Component;

public class ComBan implements SimpleCommand {

    private final ProxyServer server;
    private final HmyLanguageManager language;

    public ComBan(ProxyServer server, HmyLanguageManager language) {
        this.server = server;
        this.language = language;
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
        if (source instanceof Player player) {
            if (! PermissionUtils.hasPermission(player, "hmy.kick")) {
                language.getMessage("p_no_permission", "Keine Berechtigung.");
                return;
            }
        }
        if (targetPlayer.isPresent()) {
            Player player = targetPlayer.get();
            UUID playerUUID = player.getUniqueId();

            // Ban-Logik: Speichere den Ban in einer Datei oder Datenbank
            saveBan(playerUUID, reason);

            // Spieler trennen
            player.disconnect(Component.text(language.getMessage(playerName,"p_was_banned")));
            source.sendMessage(Component.text(language.getMessage(playerName,"p_has_banned")));
        } else {
            source.sendMessage(Component.text("Player not found."));
        }
    }

    private void saveBan(UUID playerUUID, String reason) {
        // Speichere den Ban in einer Datei oder Datenbank
        // Diese Funktion muss entsprechend implementiert werden
    }


}
