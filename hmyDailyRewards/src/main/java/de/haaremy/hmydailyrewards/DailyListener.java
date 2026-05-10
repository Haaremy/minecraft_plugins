package de.haaremy.hmydailyrewards;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class DailyListener implements Listener {

    private final HmyDailyRewards plugin;

    public DailyListener(HmyDailyRewards plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        // Leicht verzoegert, damit Economy-Cache etc. geladen ist
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            if (plugin.getDailyManager().canClaim(player.getUniqueId())) {
                DailyGui gui = new DailyGui(plugin, player);
                gui.setup();
                player.openInventory(gui.getInventory());
            }
        }, 20L);
    }
}
