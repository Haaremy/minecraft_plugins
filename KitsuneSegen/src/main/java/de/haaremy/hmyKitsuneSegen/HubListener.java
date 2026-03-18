package de.haaremy.hmykitsunesegen;

import org.bukkit.Bukkit;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.*;

/**
 * Handles all events specific to the hub world:
 *  – Adventure mode on join / teleport to spawn
 *  – No damage, no hunger, no item drops
 *  – Public chat suppressed (DM hint shown instead)
 *  – Build protection (bypass: hmy.kitsune.build)
 *  – Hub player count bossbar
 *  – Starts game countdown when min players reached
 */
public class HubListener implements Listener {

    private final HmyKitsuneSegen plugin;
    private final BossBar hubBar;

    public HubListener(HmyKitsuneSegen plugin) {
        this.plugin = plugin;
        this.hubBar = Bukkit.createBossBar(
                buildBarText(0),
                BarColor.BLUE,
                BarStyle.SOLID
        );
    }

    // ── Join ──────────────────────────────────────────────────────────────────

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        event.setJoinMessage(null);

        World hub = plugin.getServer().getWorld(plugin.getGameConfig().getHubWorld());
        if (hub == null) return;

        player.setGameMode(GameMode.ADVENTURE);
        player.teleport(hub.getSpawnLocation());
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.getInventory().clear();

        plugin.getGameManager().onPlayerJoin(player);
        updateHubBar();
    }

    // ── Quit ──────────────────────────────────────────────────────────────────

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
        Player player = event.getPlayer();
        hubBar.removePlayer(player);
        plugin.getGameManager().onPlayerQuit(player);
        plugin.getScoreboardManager().clearScoreboard(player);
        // Update bar after removal (scheduled so the player count is already decremented)
        Bukkit.getScheduler().runTask(plugin, this::updateHubBar);
    }

    // ── No damage in hub ──────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isInHub(player)) return;
        event.setCancelled(true);
    }

    // ── No hunger in hub ──────────────────────────────────────────────────────

    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isInHub(player)) return;
        event.setCancelled(true);
    }

    // ── No item drops in hub ──────────────────────────────────────────────────

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (!isInHub(event.getPlayer())) return;
        event.setCancelled(true);
    }

    // ── Build protection ──────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!isInHub(player) && !isInGame(player)) return;
        if (player.hasPermission("hmy.kitsune.build")) return;

        // In game world: check breakable-blocks whitelist
        if (isInGame(player)) {
            String blockType = event.getBlock().getType().name();
            if (plugin.getGameConfig().getBreakableBlocks().contains(blockType)) return;
        }

        event.setCancelled(true);
        player.sendActionBar(ChatColor.RED + "Du kannst hier keine Blöcke abbauen.");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!isInHub(player) && !isInGame(player)) return;
        if (player.hasPermission("hmy.kitsune.build")) return;

        event.setCancelled(true);
        player.sendActionBar(ChatColor.RED + "Du kannst hier keine Blöcke platzieren.");
    }

    // ── Chat suppression: only /dm allowed ────────────────────────────────────

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!isInHub(player)) return;
        event.setCancelled(true);
        player.sendMessage(ChatColor.GRAY + "Öffentlicher Chat ist deaktiviert. "
                + ChatColor.AQUA + "Nutze §e/dm <Spieler> <Nachricht> §bfür Direktnachrichten.");
    }

    // ── Hub world stay ────────────────────────────────────────────────────────

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        World hub = plugin.getServer().getWorld(plugin.getGameConfig().getHubWorld());
        if (hub == null) return;

        if (isInHub(player)) {
            // Player entered hub — add to bossbar
            hubBar.addPlayer(player);
            updateHubBar();
        } else {
            hubBar.removePlayer(player);
        }

        // If player somehow left hub but game isn't running, pull back
        if (!isInHub(player) && !isInGame(player) && !plugin.getGameManager().isRunning()) {
            player.teleport(hub.getSpawnLocation());
        }
    }

    // ── Hub bossbar helpers ───────────────────────────────────────────────────

    private void updateHubBar() {
        World hub = plugin.getServer().getWorld(plugin.getGameConfig().getHubWorld());
        int current = hub != null ? hub.getPlayers().size() : 0;
        int min = plugin.getGameConfig().getMinPlayers();
        int max = plugin.getGameConfig().getMaxPlayers();

        hubBar.setTitle(buildBarText(current));
        double progress = max > 0 ? Math.min(1.0, (double) current / max) : 0;
        hubBar.setProgress(progress);
        hubBar.setColor(current >= min ? BarColor.GREEN : BarColor.BLUE);

        // Ensure all hub players see the bar
        if (hub != null) {
            for (Player p : hub.getPlayers()) {
                if (!hubBar.getPlayers().contains(p)) {
                    hubBar.addPlayer(p);
                }
            }
        }
    }

    private String buildBarText(int current) {
        int min = plugin.getGameConfig().getMinPlayers();
        int max = plugin.getGameConfig().getMaxPlayers();
        String status = current >= min
                ? ChatColor.GREEN + "Countdown läuft!"
                : ChatColor.GRAY + "Warte auf Spieler…";
        return ChatColor.AQUA + "§l" + current + " §r§7/ §a" + max
                + " §7Spieler  §8│  " + status;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isInHub(Player p) {
        World hub = plugin.getServer().getWorld(plugin.getGameConfig().getHubWorld());
        return hub != null && hub.equals(p.getWorld());
    }

    private boolean isInGame(Player p) {
        World game = plugin.getServer().getWorld(plugin.getGameConfig().getGameWorld());
        return game != null && game.equals(p.getWorld());
    }
}
