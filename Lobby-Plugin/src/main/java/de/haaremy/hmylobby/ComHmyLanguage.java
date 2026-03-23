package de.haaremy.hmylobby;

import java.util.List;

import de.haaremy.hmylobby.balloon.BalloonManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PermissionNode;

public class ComHmyLanguage implements CommandExecutor {

    private final LuckPerms luckPerms;
    private final HmyLanguageManager languageManager;
    private BalloonManager balloonManager;

    public ComHmyLanguage(LuckPerms luckPerms, HmyLanguageManager languageManager) {
        this.luckPerms = luckPerms;
        this.languageManager = languageManager;
    }

    public void setBalloonManager(BalloonManager balloonManager) {
        this.balloonManager = balloonManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Überprüfen, ob der Befehl von einem Spieler kommt
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text(languageManager.getMessage("l_player_only", "Dieser Befehl kann nur von einem Spieler ausgeführt werden.")));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /hmy <language|ballon> [args...]"));
            return true;
        }

        // Ballon-Subcommand weiterleiten
        if (args[0].equalsIgnoreCase("ballon")) {
            if (balloonManager != null) {
                return balloonManager.handleCommand(player, args);
            }
            player.sendMessage(Component.text("§cBallon-System nicht verfügbar."));
            return true;
        }

        // Argumente überprüfen
        if (args.length != 2 || !args[0].equalsIgnoreCase("language")) {
            player.sendMessage(Component.text("Usage: /hmy language <language>"));
            return true;
        }

        String language = args[1].toLowerCase();

        // Unterstützte Sprachen prüfen
        List<String> supportedLanguages = List.of("en", "de");
        if (!supportedLanguages.contains(language)) {
            player.sendMessage(Component.text("Unsupported Language. Use: en, de."));
            return true;
        }

        // LuckPerms-User laden
        luckPerms.getUserManager().loadUser(player.getUniqueId()).thenAcceptAsync(user -> {
            if (user == null) {
                player.sendMessage(Component.text("Error: Could not load your user data."));
                return;
            }

            // Vorherige Sprachberechtigungen entfernen
            user.data().clear(node -> node.getType() == NodeType.PERMISSION && node.getKey().startsWith("language."));

            // Neue Sprachberechtigung setzen
            Node languageNode = PermissionNode.builder("language." + language).value(true).build();
            user.data().add(languageNode);

            // Änderungen speichern
            luckPerms.getUserManager().saveUser(user).join();

            // Spieler benachrichtigen
            player.sendMessage(Component.text(languageManager.getMessage(player, "p_language_switched", "Sprache erfolgreich auf '" + language + "' gesetzt!")));
        });

        return true;
    }
}
