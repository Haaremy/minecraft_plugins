package de.haaremy.hmypaper;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;

public class HmyTab extends BukkitRunnable implements Listener {

    private final LuckPerms luckPerms;

    public HmyTab(LuckPerms luckPerms) {
        this.luckPerms = luckPerms;
    }

@Override
public void run() {
    for (Player player : Bukkit.getOnlinePlayers()) {
        // Spieler in der aktuellen Welt filtern
        List<Player> playersInWorld = player.getWorld().getPlayers();

        // Spieler nach Rang sortieren
        List<Player> sortedPlayers = playersInWorld.stream()
                .sorted(Comparator.comparing(this::getLuckPermsRank).reversed())
                .collect(Collectors.toList());

        // Header und Footer setzen
        String header = String.join("\n",
                "§l========================",
                "§e§7 Willkommen auf",
                "",
                "§dMC.HAAREMY.DE ",
                "",
                "§7Online Spieler: §d" + playersInWorld.size(),
                "§r§l========================"
        );

        String footer = String.join("\n",
                "§r§l========================",
                "",
                "§7Ping: §d" + player.getPing() + " ms",
                "§r§l========================"
        );

        player.setPlayerListHeaderFooter(header, footer);

        // Tab-Namen setzen
        for (Player targetPlayer : sortedPlayers) {
            String tabName = createTabEntry(targetPlayer);
            targetPlayer.setPlayerListName(tabName);
        }
    }
}

private String createTabEntry(Player player) {
    // Spielername
    String name = String.format("%-10s", player.getName());

    // Spieler-Rang (LuckPerms)
    String rank = getLuckPermsRankString(player);

    // Spieler-Ping
    String ping = String.format("%-6s", player.getPing() + "ms");

    // Formatieren: [rang] [name] [ping]
    return String.format(" %s §e%s §d%s", rank, name, ping);
}

private String getLuckPermsRankString(Player player) {
    // LuckPerms-Benutzer abrufen
    User user = luckPerms.getUserManager().getUser(player.getUniqueId());
    if (user != null) {
        String primaryGroup = user.getPrimaryGroup();
        Group group = luckPerms.getGroupManager().getGroup(primaryGroup);
        if (group != null) {
            return group.getCachedData().getMetaData().getPrefix().replace("&", "§");
            // Wandelt &-Farbcodes in §-Farbcodes um
        } 
    }
    return "Unbekannt";
}

private int getLuckPermsRank(Player player) {
    // LuckPerms-Benutzer abrufen
    User user = luckPerms.getUserManager().getUser(player.getUniqueId());
    if (user != null) {
        String weight = user.getCachedData().getMetaData().getMetaValue("weight");
        return weight != null ? Integer.parseInt(weight) : Integer.MAX_VALUE;
    }
    return Integer.MAX_VALUE; // Kein Rang
}


}
