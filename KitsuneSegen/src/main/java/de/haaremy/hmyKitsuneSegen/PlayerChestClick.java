package de.haaremy.hmykitsunesegen;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles right-clicking chests on the game map.
 * Shows a bossbar loading animation; on completion items drop and the block is removed.
 */
public class PlayerChestClick implements Listener {

    private final HmyKitsuneSegen plugin;
    private final Map<UUID, BukkitTask> activeTasks = new HashMap<>();

    public PlayerChestClick(HmyKitsuneSegen plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChestClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        var block = event.getClickedBlock();
        if (block == null) return;

        Material type = block.getType();
        if (type != Material.CHEST && type != Material.ENDER_CHEST) return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        if (player == null) return;
        Location loc = block.getLocation().clone();

        if (!plugin.getChestManager().hasChest(loc)) return;
        if (activeTasks.containsKey(player.getUniqueId())) return;

        BossBar bar = Bukkit.createBossBar("§b§lTruhe öffnen…", BarColor.BLUE, BarStyle.SOLID);
        bar.addPlayer(player);
        bar.setProgress(0.0);

        double[] progress = {0.0};

        BukkitTask[] taskRef = new BukkitTask[1];
        taskRef[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            progress[0] += 0.1;
            if (progress[0] >= 1.0) {
                bar.removePlayer(player);
                bar.setVisible(false);
                activeTasks.remove(player.getUniqueId());
                taskRef[0].cancel();
                plugin.getChestManager().openChest(loc);
            } else {
                bar.setProgress(Math.min(progress[0], 1.0));
                if (loc.getWorld() != null) {
                    loc.getWorld().spawnParticle(
                            org.bukkit.Particle.END_ROD,
                            loc.clone().add(0.5, 1.0, 0.5), 5);
                }
            }
        }, 0L, 5L);

        activeTasks.put(player.getUniqueId(), taskRef[0]);
    }
}
