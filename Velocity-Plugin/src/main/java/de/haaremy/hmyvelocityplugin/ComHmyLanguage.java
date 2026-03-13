package de.haaremy.hmyvelocityplugin;

import java.util.List;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;

import net.kyori.adventure.text.Component;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PermissionNode;

public class ComHmyLanguage implements SimpleCommand {

    private final LuckPerms luckPerms;
    private final HmyLanguageManager languageManager;

    public ComHmyLanguage(LuckPerms luckPerms, HmyLanguageManager languageManager) {
        this.luckPerms = luckPerms;
        this.languageManager = languageManager;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!(source instanceof com.velocitypowered.api.proxy.Player)) {
            source.sendMessage(Component.text((languageManager.getMessage("l_player_only","Dieser Befehl kann nur von einem Spieler ausgeführt werden."))));
            return;
        }

        if (args.length < 1) {
            source.sendMessage(Component.text("§cVerwendung: §e/hmy language <de|en>"));
            return;
        }

        String language = args[0].toLowerCase();

        // Unterstützte Sprachen prüfen
        List<String> supportedLanguages = List.of("en", "de");
        if (!supportedLanguages.contains(language)) {
            source.sendMessage(Component.text("Unsupported Language, use: en, de."));
            return;
        }

        com.velocitypowered.api.proxy.Player player = (com.velocitypowered.api.proxy.Player) source;
        String permission = "language." + language;

        // LuckPerms User abrufen
        luckPerms.getUserManager().loadUser(player.getUniqueId()).thenAccept(user -> {
            if (user == null) {
                return;
            }

            // Vorherige Sprachberechtigungen entfernen
            user.data().clear(node -> node.getType() == NodeType.PERMISSION && node.getKey().startsWith("language."));

            // Neue Sprachberechtigung setzen
            Node languageNode = PermissionNode.builder(permission).value(true).build();
            user.data().add(languageNode);

            // Änderungen speichern
            luckPerms.getUserManager().saveUser(user);

            player.sendMessage(Component.text(languageManager.getMessage(player,"p_language_switched","Sprache erfolgreich auf '" + language + "' gesetzt!")));
        });
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return List.of("en", "de"); // Automatische Vervollständigung
    }

    
}
