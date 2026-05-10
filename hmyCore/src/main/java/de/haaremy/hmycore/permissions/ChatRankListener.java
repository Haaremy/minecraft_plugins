package de.haaremy.hmycore.permissions;

import de.haaremy.hmycore.HmyCore;
import de.haaremy.hmycore.lang.Lang;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.chat.ChatRenderer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Renderer fuer den Server-Chat: Rank-Prefix vor dem Spielernamen.
 *
 * Why: Standard-Chat zeigt nur "<Player> Text" — wir wollen "<Prefix>Player: Text"
 * mit LuckPerms-Group-Prefix.
 * How to apply: In HmyCore.onEnable als Listener registrieren. Wenn LuckPerms
 * fehlt, faellt der Renderer auf den Default-Stil zurueck.
 */
public class ChatRankListener implements Listener {

    private final HmyCore plugin;
    private final LuckPermsService luckPerms;

    public ChatRankListener(HmyCore plugin, LuckPermsService luckPerms) {
        this.plugin = plugin;
        this.luckPerms = luckPerms;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onChat(AsyncChatEvent event) {
        event.renderer(new RankRenderer(luckPerms));
    }

    private static final class RankRenderer implements ChatRenderer {

        private final LuckPermsService luckPerms;

        RankRenderer(LuckPermsService luckPerms) {
            this.luckPerms = luckPerms;
        }

        @Override
        public Component render(Player source, Component sourceDisplayName, Component message, Audience viewer) {
            String prefix = luckPerms.getPrefix(source);
            String suffix = luckPerms.getSuffix(source);

            TextComponent.Builder line = Component.text();
            if (prefix != null && !prefix.isEmpty()) {
                line.append(Lang.parse(prefix));
            }
            line.append(sourceDisplayName);
            if (suffix != null && !suffix.isEmpty()) {
                line.append(Lang.parse(suffix));
            }
            line.append(Component.text(": "));
            line.append(message);
            return line.build();
        }
    }
}
