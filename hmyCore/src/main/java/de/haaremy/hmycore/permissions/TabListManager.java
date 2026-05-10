package de.haaremy.hmycore.permissions;

import de.haaremy.hmycore.HmyCore;
import de.haaremy.hmycore.lang.Lang;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitTask;

/**
 * Setzt PlayerListName mit LuckPerms-Prefix sowie TabList-Header/Footer.
 *
 * Why: Spieler sehen in der TabList ihren Rang vor dem Namen. Header/Footer
 * tragen das hmyCubed-Branding. Sortierung via Scoreboard-Teams kollidiert mit
 * HmyScoreboard.setLine — wir verzichten daher bewusst auf TabList-Sort und
 * liefern nur Anzeige-Prefix; echte Sortierung kann ein dediziertes
 * Pseudo-Scoreboard in v2.7 nachliefern.
 */
public class TabListManager implements Listener {

    private final HmyCore plugin;
    private final LuckPermsService luckPerms;
    private BukkitTask refreshTask;

    public TabListManager(HmyCore plugin, LuckPermsService luckPerms) {
        this.plugin = plugin;
        this.luckPerms = luckPerms;
    }

    public void start() {
        refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshAll, 600L, 600L);
    }

    public void stop() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> applyTo(p), 5L);
    }

    private void refreshAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            applyTo(p);
        }
    }

    private void applyTo(Player player) {
        if (!player.isOnline()) return;
        applyDisplayName(player);
        applyHeaderFooter(player);
    }

    private void applyDisplayName(Player player) {
        String prefix = luckPerms.getPrefix(player);
        if (prefix == null) prefix = "";
        Component name = prefix.isEmpty()
            ? Component.text(player.getName())
            : Lang.parse(prefix).append(Component.text(player.getName()));
        player.playerListName(name);
    }

    private void applyHeaderFooter(Player player) {
        Component header = Lang.parse(
            "<gradient:#ff6b35:#a8d8ea><bold>hmyCubed</bold></gradient> "
                + "<gray>(hmy)³ <dark_gray>|</dark_gray> "
                + "<aqua>" + Bukkit.getOnlinePlayers().size() + "</aqua><gray> Online"
        );
        String group = luckPerms.getPrimaryGroup(player);
        Component footer = Lang.parse(
            "<gray>Dein Rang: <yellow>" + group + "</yellow> <dark_gray>|</dark_gray> "
                + "<gray>mc.haaremy.de"
        );
        player.sendPlayerListHeaderAndFooter(header, footer);
    }
}
