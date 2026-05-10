package de.haaremy.hmycore.commands;

import de.haaremy.hmycore.HmyCore;
import de.haaremy.hmycore.lang.Lang;
import de.haaremy.hmycore.lang.LanguageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class HmyCommand implements CommandExecutor {

    private final HmyCore plugin;

    public HmyCommand(HmyCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("language") || sub.equals("lang")) {
            handleLanguage(sender, args);
            return true;
        }
        sendUsage(sender);
        return true;
    }

    private void handleLanguage(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Lang.send(sender, "core.player_only");
            return;
        }
        if (args.length < 2) {
            Lang.send(player, "core.hmy.lang.usage");
            return;
        }
        String requested = args[1].toLowerCase(Locale.ROOT);
        if (!LanguageManager.SUPPORTED.contains(requested)) {
            Lang.send(player, "core.hmy.lang.unsupported");
            return;
        }
        plugin.getLanguageManager().setLocale(player.getUniqueId(), requested);
        Lang.send(player, "core.hmy.lang.set", "lang", requested);
    }

    private void sendUsage(CommandSender sender) {
        Lang.send(sender, "core.hmy.usage.header");
        Lang.send(sender, "core.hmy.usage.lang");
    }
}
