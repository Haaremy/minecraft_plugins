package de.haaremy.hmykitsunesegen;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.*;

/**
 * Handles all events specific to the hub world:
 *  – Adventure mode on join / teleport to spawn
 *  – No damage
 *  – No hunger drain
 *  – No item drops
 *  – Public chat suppressed (DM hint shown instead)
 *  – Starts game countdown when min players reached
 */
public class HubListener implements Listener {

    private final HmyKitsuneSegen plugin;

    public HubListener(HmyKitsuneSegen plugin) {
        this.plugin = plugin;
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
    }

    // ── Quit ──────────────────────────────────────────────────────────────────

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
        plugin.getGameManager().onPlayerQuit(event.getPlayer());
        plugin.getScoreboardManager().clearScoreboard(event.getPlayer());
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

        // If player somehow left hub but game isn't running, pull back
        if (!isInHub(player) && !isInGame(player) && !plugin.getGameManager().isRunning()) {
            player.teleport(hub.getSpawnLocation());
        }
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
