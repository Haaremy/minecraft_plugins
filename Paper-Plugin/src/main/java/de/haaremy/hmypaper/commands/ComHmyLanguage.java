package de.haaremy.hmypaper.commands;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import de.haaremy.hmypaper.HmyLanguageManager;
import net.kyori.adventure.text.Component;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PermissionNode;

public class ComHmyLanguage implements CommandExecutor {

    private final LuckPerms luckPerms;
    private final HmyLanguageManager languageManager;

    public ComHmyLanguage(LuckPerms luckPerms, HmyLanguageManager languageManager) {
        this.luckPerms = luckPerms;
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
