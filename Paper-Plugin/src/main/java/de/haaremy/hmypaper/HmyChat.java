package de.haaremy.hmypaper;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;

public class HmyChat implements Listener {

    private final LuckPerms luckPerms;

    public HmyChat(LuckPerms luckPerms) {
        this.luckPerms = luckPerms;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        User user = luckPerms.getUserManager().getUser(event.getPlayer().getUniqueId());
        if (user != null) {
            // Metadaten des Benutzers abrufen
            CachedMetaData metaData = user.getCachedData().getMetaData();

            // Prefix aus der Gruppe des Benutzers abrufen
            String groupName = user.getPrimaryGroup();
            String groupPrefix = getGroupPrefix(groupName);

            // Fallback: Benutzer-Prefix verwenden
            String userPrefix = metaData.getPrefix() != null ? metaData.getPrefix() : "";

            // Format für den Chat
            String prefix = groupPrefix != null ? groupPrefix : userPrefix;
            event.setFormat(prefix.replace("&", "§") + event.getPlayer().getName() + " §7>> §r" + event.getMessage());
        } else {
            // Fallback, wenn der Benutzer nicht geladen werden kann
            event.setFormat(event.getPlayer().getName() + " §7>> §r" + event.getMessage());
        }
    }

    private String getGroupPrefix(String groupName) {
        Group group = luckPerms.getGroupManager().getGroup(groupName);
        if (group != null) {
            return group.getCachedData().getMetaData().getPrefix();
        }
        return null;
    }
}
