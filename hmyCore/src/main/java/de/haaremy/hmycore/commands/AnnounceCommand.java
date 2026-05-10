package de.haaremy.hmycore.commands;

import de.haaremy.hmycore.HmyCore;
import de.haaremy.hmycore.lang.Lang;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

public class AnnounceCommand implements CommandExecutor {

    public static final String TOPIC = "system.announce.broadcast";
    public static final String TOPIC_PATTERN = "system.announce.*";
    public static final String TARGET_BROADCAST = "*";
    public static final int MAX_TEXT_LENGTH = 256;

    private final HmyCore plugin;

    public AnnounceCommand(HmyCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            Lang.send(sender, "core.announce.usage", "label", label);
            return true;
        }
        String text = String.join(" ", args).trim();
        if (text.isEmpty()) {
            Lang.send(sender, "core.announce.empty");
            return true;
        }
        if (text.length() > MAX_TEXT_LENGTH) {
            Lang.send(sender, "core.announce.toolong", "max", String.valueOf(MAX_TEXT_LENGTH));
            return true;
        }

        plugin.getMessagingService().publish(
                TOPIC,
                TARGET_BROADCAST,
                text.getBytes(StandardCharsets.UTF_8));

        AnnounceRenderer.renderLocally(plugin, text);

        Lang.send(sender, "core.announce.sent", "text", AnnounceRenderer.escape(text));
        return true;
    }
}
