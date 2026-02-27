package de.haaremy.hmyvelocityplugin;

import java.util.Optional;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import de.haaremy.hmyvelocityplugin.utils.PermissionUtils;
import net.kyori.adventure.text.Component;

public class ComKick implements SimpleCommand {

    private final ProxyServer server;
    private final HmyLanguageManager language;

    public ComKick(ProxyServer server, HmyLanguageManager language) {
        this.server = server;
        this.language = language;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length < 2) {
            source.sendMessage(Component.text("Usage: /kick <player> <reason>"));
            return;
        }

        // Überprüfen, ob der Source ein Spieler ist
        if (source instanceof Player player) {
            if (! PermissionUtils.hasPermission(player, "hmy.kick")) {
                language.getMessage("p_no_permission", "Keine Berechtigung.");
                return;
            }
        }
        String playerName = args[0];
        String reason = String.join(" ", args).substring(playerName.length() + 1);

        Optional<Player> targetPlayer = server.getPlayer(playerName);

        if (targetPlayer.isPresent()) {
            Player player_to_kick = targetPlayer.get();
            player_to_kick.disconnect(Component.text("You have been kicked: " + reason));
            source.sendMessage(Component.text(player_to_kick.getUsername() + " has been kicked for: " + reason));
        } else {
            source.sendMessage(Component.text("Player not found."));
        }
    }

}
