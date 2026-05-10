package de.haaremy.hmytntrun;

import de.haaremy.hmycore.HmyCore;
import de.haaremy.hmycore.countdown.CountdownManager;
import de.haaremy.hmycore.countdown.HmyCountdown;
import de.haaremy.hmycore.economy.EconomyManager;
import de.haaremy.hmycore.scoreboard.HmyScoreboard;
import de.haaremy.hmycore.scoreboard.ScoreboardManager;
import de.haaremy.hmycore.stats.StatsManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TNTRunGame {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final String PREFIX = "<red><bold>TNTRUN</bold></red> ";
    private static final int BLOCK_REMOVE_DELAY_TICKS = 10; // 500ms = 10 Ticks

    public enum GameState {
        WAITING, STARTING, RUNNING, ENDING
    }

    private final HmyTNTRun plugin;
    private final List<Player> players;
    private final Set<UUID> alivePlayers = ConcurrentHashMap.newKeySet();
    private final TNTRunArena.ArenaData arena;
    private GameState state = GameState.WAITING;

    // Bloecke die gerade scheduled sind um zu verschwinden
    private final Map<Location, BukkitTask> scheduledBlocks = new ConcurrentHashMap<>();
    // Originale Bloecke fuer Arena-Reset
    private final Map<Location, Material> originalBlocks = new ConcurrentHashMap<>();

    public TNTRunGame(HmyTNTRun plugin, List<Player> players, TNTRunArena.ArenaData arena) {
        this.plugin = plugin;
        this.players = new ArrayList<>(players);
        this.arena = arena;
    }

    public void start() {
        state = GameState.STARTING;
        arena.setInUse(true);

        // Arena-Bloecke speichern fuer Reset (Bereich um Spawn)
        saveArenaBlocks();

        // Spieler vorbereiten und teleportieren
        for (Player player : players) {
            alivePlayers.add(player.getUniqueId());
            preparePlayer(player);
            player.teleport(arena.getSpawn());
        }

        // Countdown ueber hmyCore
        String countdownId = "tntrun_" + arena.getName() + "_" + System.currentTimeMillis();
        CountdownManager countdownManager = HmyCore.getInstance().getCountdownManager();
        HmyCountdown countdown = countdownManager.createCountdown(countdownId, 5);
        countdown.forPlayers(players);
        countdown.onFinish(() -> {
            if (state == GameState.STARTING) {
                state = GameState.RUNNING;

                for (Player p : players) {
                    if (p.isOnline()) {
                        p.sendMessage(MINI.deserialize(PREFIX + "<green>Los! Der Boden verschwindet unter deinen Fuessen!"));
                        p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);
                    }
                }

                updateAllScoreboards();
            }
        });
        countdown.start();

        updateAllScoreboards();
    }

    private void preparePlayer(Player player) {
        player.getInventory().clear();
        player.setGameMode(GameMode.ADVENTURE);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        player.setFireTicks(0);
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
    }

    /**
     * Speichert die originalen Bloecke der Arena fuer den Reset.
     */
    private void saveArenaBlocks() {
        Location spawn = arena.getSpawn();
        if (spawn.getWorld() == null) return;

        // Speichere Bloecke in einem grossen Radius um den Spawn auf allen Layer-Ebenen
        int radius = 40;
        for (int layerY : arena.getLayers()) {
            for (int x = spawn.getBlockX() - radius; x <= spawn.getBlockX() + radius; x++) {
                for (int z = spawn.getBlockZ() - radius; z <= spawn.getBlockZ() + radius; z++) {
                    Location loc = new Location(spawn.getWorld(), x, layerY, z);
                    Block block = loc.getBlock();
                    if (block.getType() != Material.AIR) {
                        originalBlocks.put(loc.clone(), block.getType());
                    }
                }
            }
        }
    }

    /**
     * Wird aufgerufen wenn ein Spieler sich bewegt — Block unter ihm nach Delay entfernen.
     */
    public void onPlayerMove(Player player, Block blockUnder) {
        if (state != GameState.RUNNING) return;
        if (!alivePlayers.contains(player.getUniqueId())) return;
        if (blockUnder.getType() == Material.AIR) return;

        Location blockLoc = blockUnder.getLocation();

        // Pruefen ob Block auf einer der Layer-Ebenen liegt
        boolean onLayer = false;
        for (int layerY : arena.getLayers()) {
            if (blockLoc.getBlockY() == layerY) {
                onLayer = true;
                break;
            }
        }
        if (!onLayer) return;

        // Schon geplant?
        if (scheduledBlocks.containsKey(blockLoc)) return;

        // Block nach 500ms (10 Ticks) entfernen
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            scheduledBlocks.remove(blockLoc);
            Block b = blockLoc.getBlock();
            if (b.getType() != Material.AIR) {
                // Auch Block darunter entfernen (doppelte Ebene)
                b.setType(Material.AIR, false);

                // Block eine Ebene drunter auch entfernen falls vorhanden (fuer dickere Boeden)
                Block below = b.getRelative(0, -1, 0);
                if (below.getType() != Material.AIR) {
                    Location belowLoc = below.getLocation();
                    if (!originalBlocks.containsKey(belowLoc)) {
                        // Nur entfernen wenn nicht auf einer anderen Layer-Ebene
                    } else {
                        below.setType(Material.AIR, false);
                    }
                }
            }
        }, BLOCK_REMOVE_DELAY_TICKS);

        scheduledBlocks.put(blockLoc, task);
    }

    /**
     * Spieler wird eliminiert (durch Void-Fall oder Verlassen).
     */
    public void onPlayerEliminate(Player player, boolean leftVoluntarily) {
        if (!alivePlayers.remove(player.getUniqueId())) return;

        // Stats
        StatsManager stats = HmyCore.getInstance().getStatsManager();
        stats.addLoss(player.getUniqueId(), "tntrun");

        if (leftVoluntarily) {
            broadcastGame(MINI.deserialize(PREFIX + "<yellow>" + player.getName() + " <gray>hat das Spiel verlassen. <dark_gray>(" + alivePlayers.size() + " uebrig)"));
        } else {
            // Spectator-Modus
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage(MINI.deserialize(PREFIX + "<red>Du bist ausgeschieden!"));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 0.8f);
            player.showTitle(Title.title(
                    MINI.deserialize("<red><bold>AUSGESCHIEDEN!"),
                    MINI.deserialize("<gray>Du schaust jetzt zu"),
                    Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofMillis(500))
            ));

            broadcastGame(MINI.deserialize(PREFIX + "<yellow>" + player.getName() + " <gray>ist ausgeschieden! <dark_gray>(" + alivePlayers.size() + " uebrig)"));
        }

        updateAllScoreboards();
        checkWin();
    }

    /**
     * Prueft ob nur noch ein Spieler uebrig ist.
     */
    private void checkWin() {
        if (state == GameState.ENDING) return;

        if (alivePlayers.size() <= 1) {
            endGame();
        }
    }

    /**
     * Beendet das Spiel und kuert den Gewinner.
     */
    private void endGame() {
        state = GameState.ENDING;

        // Alle geplanten Block-Entfernungen abbrechen
        for (BukkitTask task : scheduledBlocks.values()) {
            task.cancel();
        }
        scheduledBlocks.clear();

        Player winner = null;
        if (!alivePlayers.isEmpty()) {
            UUID winnerUuid = alivePlayers.iterator().next();
            winner = plugin.getServer().getPlayer(winnerUuid);
        }

        if (winner != null && winner.isOnline()) {
            // Gewinner-Belohnungen
            EconomyManager economy = HmyCore.getInstance().getEconomyManager();
            economy.addCoins(winner.getUniqueId(), 50);

            StatsManager stats = HmyCore.getInstance().getStatsManager();
            stats.addWin(winner.getUniqueId(), "tntrun");

            // Gewinner-Nachricht
            winner.showTitle(Title.title(
                    MINI.deserialize("<gold><bold>SIEG!"),
                    MINI.deserialize("<yellow>+50 Coins"),
                    Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(1))
            ));
            winner.playSound(winner.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            winner.sendMessage(MINI.deserialize(
                    "\n<gold><bold>TNTRUN GEWONNEN!</bold></gold>\n" +
                    "<gray>Coins: <yellow>+50\n"
            ));

            broadcastGame(MINI.deserialize(PREFIX + "<yellow>" + winner.getName() + " <green>hat das Spiel gewonnen!"));
        } else {
            broadcastGame(MINI.deserialize(PREFIX + "<gray>Das Spiel ist vorbei. Kein Gewinner."));
        }

        // Scoreboards entfernen
        ScoreboardManager sbManager = HmyCore.getInstance().getScoreboardManager();
        for (Player p : players) {
            if (p.isOnline()) {
                sbManager.removeScoreboard(p);
            }
        }

        // Spieler nach kurzer Verzoegerung zur Lobby schicken und Arena resetten
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (Player p : players) {
                if (p.isOnline()) {
                    HmyCore.getInstance().getLobbyConnector().sendToLobby(p);
                }
                plugin.getTNTRunManager().removeGame(p.getUniqueId());
            }

            // Arena resetten
            resetArena();
            arena.setInUse(false);
        }, 60L); // 3 Sekunden
    }

    /**
     * Erzwungenes Spielende (Server-Shutdown).
     */
    public void forceEnd() {
        state = GameState.ENDING;

        for (BukkitTask task : scheduledBlocks.values()) {
            task.cancel();
        }
        scheduledBlocks.clear();

        ScoreboardManager sbManager = HmyCore.getInstance().getScoreboardManager();
        for (Player p : players) {
            if (p.isOnline()) {
                sbManager.removeScoreboard(p);
                p.sendMessage(MINI.deserialize(PREFIX + "<red>Das Spiel wurde abgebrochen."));
            }
            plugin.getTNTRunManager().removeGame(p.getUniqueId());
        }

        resetArena();
        arena.setInUse(false);
    }

    /**
     * Setzt die Arena zurueck (entfernte Bloecke wiederherstellen).
     */
    private void resetArena() {
        for (Map.Entry<Location, Material> entry : originalBlocks.entrySet()) {
            Location loc = entry.getKey();
            if (loc.getWorld() != null) {
                Block block = loc.getBlock();
                if (block.getType() == Material.AIR) {
                    block.setType(entry.getValue(), false);
                }
            }
        }
        originalBlocks.clear();
    }

    private void updateAllScoreboards() {
        for (Player p : players) {
            if (p.isOnline()) {
                updateScoreboard(p);
            }
        }
    }

    private void updateScoreboard(Player player) {
        ScoreboardManager sbManager = HmyCore.getInstance().getScoreboardManager();
        HmyScoreboard sb = sbManager.getScoreboard(player);
        if (sb == null) {
            sb = sbManager.createScoreboard(player);
        }

        sb.setTitle("<red><bold>TNTRUN</bold></red>");
        sb.setLine(0, "");
        sb.setLine(1, "<gray>Status: " + getStateDisplay());
        sb.setLine(2, "");
        sb.setLine(3, "<gray>Spieler: <yellow>" + alivePlayers.size() + "<gray>/" + players.size());
        sb.setLine(4, "");
        sb.setLine(5, "<gray>Ebenen: <yellow>" + arena.getLayers().size());
        sb.setLine(6, "");
        sb.setLine(7, "<dark_gray>mc.haaremy.de");
    }

    private String getStateDisplay() {
        return switch (state) {
            case WAITING -> "<yellow>Wartend";
            case STARTING -> "<yellow>Countdown";
            case RUNNING -> "<green>Laufend";
            case ENDING -> "<red>Spielende";
        };
    }

    private void broadcastGame(Component message) {
        for (Player p : players) {
            if (p.isOnline()) {
                p.sendMessage(message);
            }
        }
    }

    // Getter

    public GameState getState() {
        return state;
    }

    public boolean isAlive(UUID uuid) {
        return alivePlayers.contains(uuid);
    }

    public boolean isPlayer(UUID uuid) {
        for (Player p : players) {
            if (p.getUniqueId().equals(uuid)) return true;
        }
        return false;
    }

    public TNTRunArena.ArenaData getArena() {
        return arena;
    }

    public Set<UUID> getAlivePlayers() {
        return alivePlayers;
    }

    public List<Player> getPlayers() {
        return players;
    }
}
