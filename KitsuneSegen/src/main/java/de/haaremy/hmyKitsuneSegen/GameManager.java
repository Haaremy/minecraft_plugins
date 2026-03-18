package de.haaremy.hmykitsunesegen;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.*;

/**
 * Central game state machine for Kitsune Segen.
 *
 * States: WAITING → COUNTDOWN → RUNNING → ENDED → WAITING
 */
public class GameManager {

    public enum State { WAITING, COUNTDOWN, RUNNING, ENDED }

    private final HmyKitsuneSegen plugin;

    private State state = State.WAITING;
    private final List<UUID> alivePlayers  = new ArrayList<>();
    private final List<UUID> spectators    = new ArrayList<>();
    private final Map<UUID, Integer> kills = new HashMap<>();
    // ordered from first-eliminated to last (winner prepended in endGame)
    private final List<String> placements  = new ArrayList<>();

    private BukkitTask countdownTask;
    private int countdownLeft;

    public GameManager(HmyKitsuneSegen plugin) {
        this.plugin = plugin;
    }

    // ── Getters ────────────────────────────────────────────────────────────────

    public State  getState()             { return state; }
    public boolean isJoinable()          { return state == State.WAITING || state == State.COUNTDOWN; }
    public boolean isRunning()           { return state == State.RUNNING; }
    public boolean isAlive(Player p)     { return alivePlayers.contains(p.getUniqueId()); }
    public boolean isSpectator(Player p) { return spectators.contains(p.getUniqueId()); }
    public int     getAliveCount()       { return alivePlayers.size(); }
    public int     getKills(Player p)    { return kills.getOrDefault(p.getUniqueId(), 0); }

    public List<Player> getAlivePlayers() {
        List<Player> result = new ArrayList<>();
        for (UUID uuid : alivePlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) result.add(p);
        }
        return result;
    }

    // ── Join / Quit ────────────────────────────────────────────────────────────

    public void onPlayerJoin(Player player) {
        if (!isJoinable()) {
            sendToLobby(player);
            return;
        }
        World hub = Bukkit.getWorld(plugin.getGameConfig().getHubWorld());
        if (hub == null) return;

        int hubCount = hub.getPlayers().size();
        int min = plugin.getGameConfig().getMinPlayers();
        int max = plugin.getGameConfig().getMaxPlayers();

        if (hubCount > max) {
            sendToLobby(player);
            return;
        }
        if (hubCount >= min && state == State.WAITING) {
            startCountdown();
        }
    }

    public void onPlayerQuit(Player player) {
        UUID uuid = player.getUniqueId();
        boolean wasAlive = alivePlayers.remove(uuid);
        spectators.remove(uuid);
        kills.remove(uuid);

        if (wasAlive && isRunning()) {
            checkWinCondition();
        }
        // If countdown active and we drop below min players, cancel
        if (state == State.COUNTDOWN) {
            World hub = Bukkit.getWorld(plugin.getGameConfig().getHubWorld());
            if (hub != null && hub.getPlayers().size() < plugin.getGameConfig().getMinPlayers()) {
                cancelCountdown();
                broadcast("§cZu wenige Spieler – Countdown abgebrochen.");
            }
        }
    }

    // ── Countdown ─────────────────────────────────────────────────────────────

    public void startCountdown() {
        if (state != State.WAITING) return;
        state = State.COUNTDOWN;
        countdownLeft = plugin.getGameConfig().getCountdownSeconds();

        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (countdownLeft <= 0) {
                countdownTask.cancel();
                startGame();
                return;
            }
            if (countdownLeft <= 10 || countdownLeft % 15 == 0) {
                World hub = Bukkit.getWorld(plugin.getGameConfig().getHubWorld());
                if (hub != null) {
                    for (Player p : hub.getPlayers()) {
                        p.sendTitle(
                            ChatColor.GOLD + String.valueOf(countdownLeft),
                            ChatColor.GRAY + "Spiel startet bald!",
                            5, 30, 5
                        );
                    }
                }
            }
            countdownLeft--;
        }, 0L, 20L);
    }

    public void cancelCountdown() {
        if (countdownTask != null) { countdownTask.cancel(); countdownTask = null; }
        state = State.WAITING;
        countdownLeft = 0;
    }

    // ── Game Start ─────────────────────────────────────────────────────────────

    public void startGame() {
        state = State.RUNNING;
        World hub  = Bukkit.getWorld(plugin.getGameConfig().getHubWorld());
        World game = Bukkit.getWorld(plugin.getGameConfig().getGameWorld());
        if (hub == null || game == null) {
            plugin.getLogger().severe("Hub oder Spielwelt nicht geladen!");
            state = State.WAITING;
            return;
        }

        List<Player> players = new ArrayList<>(hub.getPlayers());

        // Spawn chests
        plugin.getChestManager().spawnChests(game);

        // Prepare spawn points
        List<Location> spawnPoints = new ArrayList<>(plugin.getSpawnPoints());
        Collections.shuffle(spawnPoints);
        String mode = plugin.getGameConfig().getSpawnMode();

        // Brief pre-start countdown in game world position
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            alivePlayers.add(p.getUniqueId());
            kills.put(p.getUniqueId(), 0);

            if (mode.equals("flight")) {
                Location center = game.getSpawnLocation().clone();
                center.setY(plugin.getGameConfig().getElytraHeight());
                teleportWithElytra(p, center);
            } else {
                Location loc = spawnPoints.isEmpty()
                    ? game.getSpawnLocation()
                    : spawnPoints.get(i % spawnPoints.size());
                teleportNormal(p, loc);
            }
        }

        broadcast("§6§lKitsune Segen §egestartet! §a" + players.size() + " Spieler kämpfen!");
        plugin.getScoreboardManager().startUpdating();
    }

    private void teleportWithElytra(Player p, Location loc) {
        p.setGameMode(GameMode.SURVIVAL);
        p.teleport(loc);
        resetStats(p);
        giveStartItems(p);
        p.getInventory().setChestplate(new ItemStack(Material.ELYTRA));
        p.setGliding(true);
        p.sendTitle(ChatColor.GOLD + "§lKitsune Segen", ChatColor.GRAY + "Spring!", 10, 40, 10);
    }

    private void teleportNormal(Player p, Location loc) {
        p.setGameMode(GameMode.SURVIVAL);
        p.teleport(loc);
        resetStats(p);
        giveStartItems(p);
        p.sendTitle(ChatColor.GOLD + "§lKitsune Segen", ChatColor.GRAY + "Los!", 10, 40, 10);
    }

    private void resetStats(Player p) {
        double maxHp = plugin.getGameConfig().getMaxHealth();
        var attr = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr != null) attr.setBaseValue(maxHp);
        p.setHealth(maxHp);
        p.setFoodLevel(20);
        p.setSaturation(20f);
        p.setExp(0f);
        p.setLevel(0);
    }

    private void giveStartItems(Player p) {
        p.getInventory().clear();
        // Slot 0: Axe (locked)
        ItemStack axe = new ItemStack(Material.WOODEN_HOE);
        ItemMeta axeMeta = axe.getItemMeta();
        axeMeta.setDisplayName(ChatColor.GOLD + "Axt");
        axeMeta.setLore(List.of(ChatColor.GRAY + "Verteidige dich so lange du musst."));
        axe.setItemMeta(axeMeta);
        p.getInventory().setItem(0, axe);
    }

    // ── Death / Elimination ────────────────────────────────────────────────────

    public void eliminatePlayer(Player killed, Player killer) {
        UUID uuid = killed.getUniqueId();
        alivePlayers.remove(uuid);
        spectators.add(uuid);
        placements.add(0, killed.getName()); // most recent death at front

        int placement = alivePlayers.size() + 1;
        int total = alivePlayers.size() + spectators.size();

        if (killer != null) {
            kills.merge(killer.getUniqueId(), 1, Integer::sum);
            killer.sendMessage(ChatColor.GREEN + "+" + 1 + " Kill: §e" + killed.getName());
        }

        killed.sendTitle(
            ChatColor.RED + "Ausgeschieden!",
            ChatColor.YELLOW + "Platz §e" + placement + " §7/ §e" + total,
            10, 80, 20
        );

        // Spectator setup
        killed.setGameMode(GameMode.SPECTATOR);
        killed.getInventory().clear();

        ItemStack leave = new ItemStack(Material.RED_DYE);
        ItemMeta lm = leave.getItemMeta();
        lm.setDisplayName(ChatColor.RED + "§lVerlassen");
        lm.setLore(List.of(ChatColor.GRAY + "Zurück zur Lobby"));
        leave.setItemMeta(lm);
        killed.getInventory().setItem(0, leave);

        ItemStack report = new ItemStack(Material.BOOK);
        ItemMeta rm = report.getItemMeta();
        rm.setDisplayName(ChatColor.YELLOW + "§lReport");
        rm.setLore(List.of(ChatColor.GRAY + "Spieler melden"));
        report.setItemMeta(rm);
        killed.getInventory().setItem(8, report);

        // Hide from alive players (but not from other spectators)
        for (Player alive : getAlivePlayers()) {
            alive.hidePlayer(plugin, killed);
        }
        // Spectators see each other
        for (UUID sUuid : spectators) {
            Player spec = Bukkit.getPlayer(sUuid);
            if (spec != null && !spec.equals(killed)) {
                spec.showPlayer(plugin, killed);
                killed.showPlayer(plugin, spec);
            }
        }

        checkWinCondition();
    }

    private void checkWinCondition() {
        if (!isRunning()) return;
        if (alivePlayers.size() <= 1) {
            Player winner = alivePlayers.isEmpty() ? null : Bukkit.getPlayer(alivePlayers.get(0));
            endGame(winner);
        }
    }

    // ── Game End ───────────────────────────────────────────────────────────────

    public void endGame(Player winner) {
        state = State.ENDED;
        plugin.getScoreboardManager().stopUpdating();

        if (winner != null) {
            broadcast(ChatColor.GOLD + "§l" + winner.getName() + " §ehat gewonnen!");
            winner.sendTitle(ChatColor.GOLD + "§lGEWONNEN!", ChatColor.YELLOW + "Herzlichen Glückwunsch!", 10, 100, 20);
        } else {
            broadcast(ChatColor.GRAY + "Unentschieden – niemand hat gewonnen.");
        }

        // Build placement list (winner at #1)
        List<String> allPlacements = new ArrayList<>();
        if (winner != null) allPlacements.add(winner.getName());
        allPlacements.addAll(placements);

        StringBuilder sb = new StringBuilder(ChatColor.GOLD + "§lErgebnisse:\n");
        for (int i = 0; i < allPlacements.size(); i++) {
            String prefix = switch (i) {
                case 0 -> ChatColor.GOLD + "§l🥇 ";
                case 1 -> ChatColor.GRAY + "§l🥈 ";
                case 2 -> ChatColor.DARK_RED + "§l🥉 ";
                default -> ChatColor.WHITE.toString() + (i + 1) + ". ";
            };
            sb.append(prefix).append(ChatColor.WHITE).append(allPlacements.get(i)).append("\n");
        }
        String results = sb.toString();
        for (Player p : Bukkit.getOnlinePlayers()) p.sendMessage(results);

        // Return to hub + reset after 10 seconds
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            World hub = Bukkit.getWorld(plugin.getGameConfig().getHubWorld());
            if (hub == null) return;
            Location hubSpawn = hub.getSpawnLocation();
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.teleport(hubSpawn);
                p.setGameMode(GameMode.ADVENTURE);
                plugin.getScoreboardManager().clearScoreboard(p);
            }
            reset();
            // World reset after another 5 seconds
            Bukkit.getScheduler().runTaskLater(plugin, plugin.getWorldReset()::resetWorld, 100L);
        }, 200L);
    }

    public void reset() {
        state = State.WAITING;
        alivePlayers.clear();
        spectators.clear();
        kills.clear();
        placements.clear();
        countdownLeft = 0;
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private void broadcast(String msg) {
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(msg));
    }

    void sendToLobby(Player player) {
        player.sendMessage(ChatColor.YELLOW + "Spiel läuft bereits – du wirst weitergeleitet…");
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(bos);
                dos.writeUTF("Connect");
                dos.writeUTF(plugin.getGameConfig().getLobbyServer());
                player.sendPluginMessage(plugin, "BungeeCord", bos.toByteArray());
            } catch (Exception e) {
                plugin.getLogger().warning("Fehler beim Senden zur Lobby: " + e.getMessage());
            }
        }, 10L);
    }
}
