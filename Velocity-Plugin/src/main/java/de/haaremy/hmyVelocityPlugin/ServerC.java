package de.haaremy.hmyvelocityplugin;

import java.util.Optional;
import java.util.Properties;
import java.util.logging.Logger;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import net.kyori.adventure.text.Component;
import net.luckperms.api.LuckPerms;

public class ServerC implements SimpleCommand {

    private final ProxyServer server;
    private final Logger logger;
    private final LuckPerms luckPerms;
    private final Properties language;

    // Hauptkonstruktor mit java.util.logging.Logger
    public ServerC(ProxyServer server, Logger logger, LuckPerms luckPerms, Properties language) {
        this.server = server;
        this.logger = logger;
        this.luckPerms = luckPerms;
        this.language = language;
    }

    // Overload-Konstruktor für org.slf4j.Logger
    public ServerC(ProxyServer server, org.slf4j.Logger slf4jLogger, LuckPerms luckPerms, Properties language) {
        this(server, Logger.getLogger(slf4jLogger.getName()), luckPerms, language);
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!(source instanceof Player)) {
            source.sendMessage(Component.text(getLocalizedMessage("language.config.playerOnly", "Dieser Befehl kann nur von Spielern ausgeführt werden.")));
            return;
        }

        Player player = (Player) source;

        if (args.length != 2 || !args[0].equalsIgnoreCase("server")) {
            source.sendMessage(Component.text(getLocalizedMessage("language.config.usage", "/hmy server <name>")));
            return;
        }

        String serverName = args[1];

        if (!hasPermission(player, "hmy.server." + serverName)) {
            player.sendMessage(Component.text(getLocalizedMessage("language.config.noPermission", "Keine Berechtigung.")));
            return;
        }

        connectToServer(player, serverName,
            getLocalizedMessage("language.config.connected", "Du wurdest erfolgreich verbunden."),
            getLocalizedMessage("language.config.failed", "Verbindung zum Server fehlgeschlagen.")
        );
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return true; // LuckPerms regelt die Permissions in der Logik
    }

    private boolean hasPermission(Player player, String permission) {
        try {
            return luckPerms.getUserManager().loadUser(player.getUniqueId()).join()
                   .getCachedData().getPermissionData().checkPermission(permission).asBoolean();
        } catch (Exception e) {
            logger.warning("Fehler beim Überprüfen der Berechtigung für Spieler " + player.getUsername() + ": " + e.getMessage());
            return false;
        }
    }

    private void connectToServer(Player player, String serverName, String successMessage, String failureMessage) {
        Optional<RegisteredServer> targetServer = server.getServer(serverName);

        if (targetServer.isPresent()) {
            player.createConnectionRequest(targetServer.get()).connect().thenAccept(result -> {
                if (result.isSuccessful()) {
                    player.sendMessage(Component.text(successMessage));
                } else {
                    player.sendMessage(Component.text(failureMessage));
                }
            });
        } else {
            player.sendMessage(Component.text(getLocalizedMessage("language.config.serverNotFound", "Server nicht gefunden.")));
            logger.warning("Server " + serverName + " wurde nicht gefunden.");
        }
    }

    private String getLocalizedMessage(String key, String defaultMessage) {
        return language.getProperty(key, defaultMessage);
    }
}
