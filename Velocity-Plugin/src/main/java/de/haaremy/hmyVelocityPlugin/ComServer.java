package de.haaremy.hmyvelocityplugin;

import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import de.haaremy.hmyvelocityplugin.utils.PermissionUtils;
import net.kyori.adventure.text.Component;
import net.luckperms.api.LuckPerms;

public class ComServer implements SimpleCommand {

    private final ProxyServer server;
    private final Logger logger;
    private final LuckPerms luckPerms;
    private final HmyLanguageManager language;
    private Map<String, String> placeholders;

    // Hauptkonstruktor mit java.util.logging.Logger
    public ComServer(ProxyServer server, Logger logger, LuckPerms luckPerms,  HmyLanguageManager language) {
        this.server = server;
        this.logger = logger;
        this.luckPerms = luckPerms;
        this.language = language;
    }

    // Overload-Konstruktor für org.slf4j.Logger
    public ComServer(ProxyServer server, org.slf4j.Logger slf4jLogger, LuckPerms luckPerms, HmyLanguageManager language) {
        this(server, Logger.getLogger(slf4jLogger.getName()), luckPerms, language);
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();
        Player player = (Player) source;

        if (!(source instanceof Player)) {
            source.sendMessage(Component.text(language.getMessage(player,"l_player_only", "Dieser Befehl kann nur von Spielern ausgeführt werden.")));
            return;
        }

        

        if (args[0].contains("server")) {
            if ((args.length != 2)) {
                        source.sendMessage(Component.text("Fehler: Ungültiges Argument. Verwende: /hmy server <server>"));
                        return;
            }
        }

        String serverName = args[1];

        if (! PermissionUtils.hasPermission(player, "hmy.server." + serverName)) {
            player.sendMessage(Component.text(language.getMessage(player,"p_no_permission", "Keine Berechtigung.")));
            return;
        } else connectToServer(player, serverName);


    }


    private void connectToServer(Player player, String serverName) {
        Optional<RegisteredServer> targetServer = server.getServer(serverName);

        if (targetServer.isPresent()) {
            player.createConnectionRequest(targetServer.get()).connect().thenAccept(result -> {
                if (!result.isSuccessful()) {
                    player.sendMessage(Component.text(language.getMessage(player,"language.config.failed", "Verbindung zum Server fehlgeschlagen.")));
                }
            });
        } else {
            String notfound = language.getMessage("p_server_not_found", "Server nicht gefunden.");
            player.sendMessage(Component.text(notfound));
        }
    }

}
