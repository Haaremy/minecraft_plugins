package de.haaremy.hmykitsunesegen;

import org.bukkit.Bukkit;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;

import io.papermc.paper.event.player.AsyncChatEvent;

/**
 * Handles all events specific to the hub world:
 *  – AGB enforcement: freeze + book until accepted
 *  – Adventure mode on join / teleport to spawn
 *  – No damage, no hunger, no item drops
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
        event.joinMessage(null);

        World hub = plugin.getServer().getWorld(plugin.getGameConfig().getHubWorld());
        if (hub == null) return;

        player.setGameMode(GameMode.ADVENTURE);
        player.teleport(hub.getSpawnLocation());
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.getInventory().clear();

        if (!plugin.getAgbManager().hasAccepted(player.getUniqueId())) {
            // AGB ausstehend: nur Buch, eingefroren
            player.getInventory().setItem(4, plugin.getAgbManager().createBook());
            player.sendTitle(
                    ChatColor.RED + "AGB erforderlich",
                    ChatColor.GRAY + "Bitte lies und akzeptiere die AGB.",
                    10, 80, 20
            );
            // Buch nach kurzer Verzögerung öffnen (Login-Sequenz muss abgeschlossen sein)
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.openBook(plugin.getAgbManager().createBook());
                }
            }, 10L);
            refreshBar();
            return; // GameManager noch nicht einschalten
        }

        plugin.getGameManager().onPlayerJoin(player);
        refreshBar();
    }

    // ── Quit ──────────────────────────────────────────────────────────────────

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        event.quitMessage(null);
        Player player = event.getPlayer();
        hubBar.removePlayer(player);
        plugin.getGameManager().onPlayerQuit(player);
        plugin.getScoreboardManager().clearScoreboard(player);
        Bukkit.getScheduler().runTask(plugin, this::refreshBar);
    }

    // ── AGB: Bewegungssperre ──────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!isInHub(player)) return;
        if (plugin.getAgbManager().hasAccepted(player.getUniqueId())) return;

        Location from = event.getFrom();
        Location to   = event.getTo();

        // Nur Positionsänderung sperren, Kopfrotation erlauben
        if (from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ()) {

            Location reset = from.clone();
            reset.setYaw(to.getYaw());
            reset.setPitch(to.getPitch());
            event.setTo(reset);

            player.sendActionBar(ChatColor.RED + "Akzeptiere zuerst die AGB! "
                    + ChatColor.YELLOW + "Öffne das Buch in deiner Hand.");
        }
    }

    // ── AGB: Inventar-Interaktion sperren ─────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!isInHub(player)) return;
        if (plugin.getAgbManager().hasAccepted(player.getUniqueId())) return;

        // Eigenes Inventar (E-Taste) blockieren, Buch-GUI läuft separat und feuert kein Event
        if (event.getInventory().getType() == InventoryType.CRAFTING) {
            event.setCancelled(true);
            player.sendActionBar(ChatColor.RED + "Akzeptiere zuerst die AGB!");
        }
    }

    // ── AGB: Buch erneut öffnen bei Interaktion ───────────────────────────────

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!isInHub(player)) return;
        if (plugin.getAgbManager().hasAccepted(player.getUniqueId())) return;

        // Buch per Rechtsklick öffnen
        if (event.getItem() != null
                && event.getItem().getType().name().contains("WRITTEN_BOOK")) {
            // Vanilla öffnet das Buch bereits – kein extra Aufruf nötig
            return;
        }
        event.setCancelled(true);
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

        if (isInGame(player)) {
            if (plugin.getGameConfig().getBreakableBlocks().contains(event.getBlock().getType())) return;
        }

        event.setCancelled(true);
        player.sendActionBar(ChatColor.RED + "Du kannst hier keine Bloecke abbauen.");
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
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!isInHub(player)) return;
        event.setCancelled(true);
        player.sendMessage(ChatColor.GRAY + "Oeffentlicher Chat ist deaktiviert. "
                + ChatColor.AQUA + "Nutze " + ChatColor.YELLOW + "/dm <Spieler> <Nachricht> "
                + ChatColor.AQUA + "fuer Direktnachrichten.");
    }

    // ── Hub world stay ────────────────────────────────────────────────────────

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        World hub = plugin.getServer().getWorld(plugin.getGameConfig().getHubWorld());
        if (hub == null) return;

        if (isInHub(player)) {
            hubBar.addPlayer(player);
            refreshBar();
        } else {
            hubBar.removePlayer(player);
        }

        if (!isInHub(player) && !isInGame(player) && !plugin.getGameManager().isRunning()) {
            player.teleport(hub.getSpawnLocation());
        }
    }

    // ── Hub bossbar ───────────────────────────────────────────────────────────

    /** Öffentlich, damit ComAgb nach Zustimmung die Leiste aktualisieren kann. */
    public void refreshBar() {
        World hub = plugin.getServer().getWorld(plugin.getGameConfig().getHubWorld());
        int current = hub != null ? hub.getPlayers().size() : 0;
        int min     = plugin.getGameConfig().getMinPlayers();
        int max     = plugin.getGameConfig().getMaxPlayers();

        hubBar.setTitle(buildBarText(current));
        hubBar.setProgress(max > 0 ? Math.min(1.0, (double) current / max) : 0);
        hubBar.setColor(current >= min ? BarColor.GREEN : BarColor.BLUE);

        if (hub != null) {
            for (Player p : hub.getPlayers()) {
                if (!hubBar.getPlayers().contains(p)) hubBar.addPlayer(p);
            }
        }
    }

    private String buildBarText(int current) {
        int min = plugin.getGameConfig().getMinPlayers();
        int max = plugin.getGameConfig().getMaxPlayers();
        String status = current >= min
                ? ChatColor.GREEN + "Countdown laeuft!"
                : ChatColor.GRAY + "Warte auf Spieler...";
        return ChatColor.AQUA + "" + ChatColor.BOLD + current + " " + ChatColor.RESET + ChatColor.GRAY + "/ " + ChatColor.GREEN + max
                + " " + ChatColor.GRAY + "Spieler  " + ChatColor.DARK_GRAY + "|  " + status;
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
