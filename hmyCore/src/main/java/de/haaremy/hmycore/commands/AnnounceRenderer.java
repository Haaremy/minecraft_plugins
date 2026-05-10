package de.haaremy.hmycore.commands;

import de.haaremy.hmycore.HmyCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.time.Duration;

public final class AnnounceRenderer {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private AnnounceRenderer() {}

    public static void renderLocally(HmyCore plugin, String text) {
        String safe = escape(text);
        Component chat = MINI.deserialize(
                "<gradient:#ff6b35:#a8d8ea><bold>[Ankuendigung]</bold></gradient> <white>" + safe);
        Component titleMain = MINI.deserialize(
                "<gradient:#ff6b35:#a8d8ea><bold>Ankuendigung</bold></gradient>");
        Component titleSub = MINI.deserialize("<white>" + safe);
        Title title = Title.title(titleMain, titleSub,
                Title.Times.times(
                        Duration.ofMillis(300),
                        Duration.ofMillis(3000),
                        Duration.ofMillis(700)));

        for (Player p : plugin.getServer().getOnlinePlayers()) {
            p.sendMessage(chat);
            p.showTitle(title);
        }
    }

    static String escape(String text) {
        return text.replace("<", "&lt;").replace(">", "&gt;");
    }
}
