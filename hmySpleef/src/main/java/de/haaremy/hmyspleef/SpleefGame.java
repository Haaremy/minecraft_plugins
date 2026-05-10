package de.haaremy.hmyspleef;

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
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SpleefGame {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final String PREFIX = "<aqua><bold>SPLEEF</bold></aqua> ";
    private static final int COINS_WIN = 40;
    private static final int COINS_PARTICIPATE = 5;

    public enum GameState {
        WAITING, STARTING, RUNNING, ENDING
    }

    private final HmySpleef plugin;
    private final List<Player> players;
    private final Set<UUID> alivePlayers = ConcurrentHashMap.newKeySet();
    private final SpleefArena.ArenaData arena;
    private GameState state = GameState.WAITING;

    // Originale Bloecke fuer Arena-Reset
    private final Map<Location, Material> originalBlocks = new ConcurrentHashMap<>();

    public SpleefGame(HmySpleef plugin, List<Player> players, SpleefArena.ArenaData arena) {
        this.plugin = plugin;
        this.players = new ArrayList<>(players);
        this.arena = arena;
    }

    public void start() {
        state = GameState.STARTING;
        arena.setInUse(true);

        // Arena-Bloecke speichern fuer Reset
        saveArenaBlocks();

        // Spieler vorbereiten und teleportieren
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            alivePlayers.add(player.getUniqueId());
            preparePlayer(player);

            Location spawn = arena.getSpawnForPlayer(i);
            if (spawn != null) {
                player.teleport(spawn);
            }
        }

        // Countdown ueber hmyCore
        String countdownId = "spleef_" + arena.getName() + "_" + System.currentTimeMillis();
        CountdownManager countdownManager = HmyCore.getInstance().getCountdownManager();
        HmyCountdown countdown = countdownManager.createCountdown(countdownId, 5);
        countdown.forPlayers(players);
        countdown.onFinish(() -> {
            if (state == GameState.STARTING) {
                state = GameState.RUNNING;

                for (Player p : players) {
                    if (p.isOnline()) {
                        p.sendMessage(MINI.deserialize(PREFIX + "<green>Los! Zerstoere den Boden unter deinen Gegnern!"));
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
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        player.setFireTicks(0);
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));

        // Diamantschaufel mit Effizienz V
        ItemStack shovel = new ItemStack(Material.DIAMOND_SHOVEL);
        ItemMeta meta = shovel.getItemMeta();
        if (meta != null) {
            meta.displayName(MINI.deserialize("<aqua><bold>Spleef-Schaufel"));
            meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
            meta.setUnbreakable(true);
            shovel.setItemMeta(meta);
        }
        player.getInventory().setItem(0, shovel);
    }

    /**
     * Speichert die originalen Bloecke der Arena fuer den Reset.
     */
    private void saveArenaBlocks() {
        if (arena.getSpawnPoints().isEmpty()) return;
        Location ref = arena.getSpawnPoints().get(0);
        if (ref.getWorld() == null) return;

        int radius = 40;
        int floorY = arena.getFloorY();

        // Speichere Bloecke auf der Floor-Ebene (und +/- 1 Block)
        for (int dy = -1; dy <= 1; dy++) {
            int y = floorY + dy;
            for (int x = ref.getBlockX() - radius; x <= ref.getBlockX() + radius; x++) {
                for (int z = ref.getBlockZ() - radius; z <= ref.getBlockZ() + radius; z++) {
                    Location loc = new Location(ref.getWorld(), x, y, z);
                    Block block = loc.getBlock();
                    if (block.getType() == Material.SNOW_BLOCK) {
                        originalBlocks.put(loc.clone(), Material.SNOW_BLOCK);
                    }
                }
            }
        }
    }

    /**
     * Spieler wird eliminiert (durch Void-Fall oder Verlassen).
     */
    public void onPlayerEliminate(Player player, boolean leftVoluntarily) {
        if (!alivePlayers.remove(player.getUniqueId())) return;

        // Teilnahme-Coins
        EconomyManager economy = HmyCore.getInstance().getEconomyManager();
        economy.addCoins(player.getUniqueId(), COINS_PARTICIPATE);

        // Stats
        StatsManager stats = HmyCore.getInstance().getStatsManager();
        stats.addLoss(player.getUniqueId(), "spleef");

        if (leftVoluntarily) {
            broadcastGame(MINI.deserialize(PREFIX + "<yellow>" + player.getName() + " <gray>hat das Spiel verlassen. <dark_gray>(" + alivePlayers.size() + " uebrig)"));
        } else {
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage(MINI.deserialize(PREFIX + "<red>Du bist ausgeschieden! <gray>(+5 Coins Teilnahme)"));
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

        Player winner = null;
        if (!alivePlayers.isEmpty()) {
            UUID winnerUuid = alivePlayers.iterator().next();
            winner = plugin.getServer().getPlayer(winnerUuid);
        }

        if (winner != null && winner.isOnline()) {
            // Gewinner-Belohnungen
            EconomyManager economy = HmyCore.getInstance().getEconomyManager();
            economy.addCoins(winner.getUniqueId(), COINS_WIN);

            StatsManager stats = HmyCore.getInstance().getStatsManager();
            stats.addWin(winner.getUniqueId(), "spleef");

            winner.showTitle(Title.title(
                    MINI.deserialize("<gold><bold>SIEG!"),
                    MINI.deserialize("<yellow>+" + COINS_WIN + " Coins"),
                    Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(1))
            ));
            winner.playSound(winner.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            winner.sendMessage(MINI.deserialize(
                    "\n<aqua><bold>SPLEEF GEWONNEN!</bold></aqua>\n" +
                    "<gray>Coins: <yellow>+" + COINS_WIN + "\n"
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

        // Spieler zur Lobby schicken und Arena resetten
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (Player p : players) {
                if (p.isOnline()) {
                    HmyCore.getInstance().getLobbyConnector().sendToLobby(p);
                }
                plugin.getSpleefManager().removeGame(p.getUniqueId());
            }

            resetArena();
            arena.setInUse(false);
        }, 60L);
    }

    /**
     * Erzwungenes Spielende (Server-Shutdown).
     */
    public void forceEnd() {
        state = GameState.ENDING;

        ScoreboardManager sbManager = HmyCore.getInstance().getScoreboardManager();
        for (Player p : players) {
            if (p.isOnline()) {
                sbManager.removeScoreboard(p);
                p.sendMessage(MINI.deserialize(PREFIX + "<red>Das Spiel wurde abgebrochen."));
            }
            plugin.getSpleefManager().removeGame(p.getUniqueId());
        }

        resetArena();
        arena.setInUse(false);
    }

    /**
     * Setzt die Arena zurueck (Snow Blocks wiederherstellen).
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

        sb.setTitle("<aqua><bold>SPLEEF</bold></aqua>");
        sb.setLine(0, "");
        sb.setLine(1, "<gray>Status: " + getStateDisplay());
        sb.setLine(2, "");
        sb.setLine(3, "<gray>Spieler: <yellow>" + alivePlayers.size() + "<gray>/" + players.size());
        sb.setLine(4, "");
        sb.setLine(5, "<gray>Boden: <white>SNOW_BLOCK");
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

    public SpleefArena.ArenaData getArena() {
        return arena;
    }

    public Set<UUID> getAlivePlayers() {
        return alivePlayers;
    }

    public List<Player> getPlayers() {
        return players;
    }
}
