package de.haaremy.hmytntrun;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TNTRunManager {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final String PREFIX = "<red><bold>TNTRUN</bold></red> ";
    private static final int MIN_PLAYERS = 2;
    private static final int MAX_PLAYERS = 12;
    private static final int COUNTDOWN_SECONDS = 30;

    private final HmyTNTRun plugin;
    private final Set<UUID> queue = new LinkedHashSet<>();
    private final Map<UUID, TNTRunGame> activeGames = new ConcurrentHashMap<>();
    private BukkitTask countdownTask;
    private int countdownTimer = -1;

    public TNTRunManager(HmyTNTRun plugin) {
        this.plugin = plugin;
    }

    /**
     * Spieler tritt der Warteschlange bei.
     */
    public boolean joinQueue(Player player) {
        UUID uuid = player.getUniqueId();

        if (isInGame(uuid)) {
            player.sendMessage(MINI.deserialize(PREFIX + "<red>Du bist bereits in einem Spiel!"));
            return false;
        }

        if (queue.contains(uuid)) {
            player.sendMessage(MINI.deserialize(PREFIX + "<red>Du bist bereits in der Warteschlange!"));
            return false;
        }

        if (queue.size() >= MAX_PLAYERS) {
            player.sendMessage(MINI.deserialize(PREFIX + "<red>Die Warteschlange ist voll!"));
            return false;
        }

        queue.add(uuid);
        player.sendMessage(MINI.deserialize(PREFIX + "<gray>Du bist der Warteschlange beigetreten! <dark_gray>(" + queue.size() + "/" + MAX_PLAYERS + " Spieler)"));

        // Alle in der Queue informieren
        broadcastQueue(MINI.deserialize(PREFIX + "<yellow>" + player.getName() + " <gray>hat die Queue betreten. <dark_gray>(" + queue.size() + "/" + MAX_PLAYERS + ")"));

        // Countdown starten wenn genug Spieler
        if (queue.size() >= MIN_PLAYERS && countdownTimer < 0) {
            startCountdown();
        }

        // Sofort starten bei Max
        if (queue.size() >= MAX_PLAYERS && countdownTimer > 5) {
            countdownTimer = 5;
        }

        return true;
    }

    /**
     * Spieler verlaesst die Warteschlange.
     */
    public boolean leaveQueue(Player player) {
        UUID uuid = player.getUniqueId();

        if (isInGame(uuid)) {
            TNTRunGame game = activeGames.get(uuid);
            if (game != null) {
                game.onPlayerEliminate(player, true);
            }
            return true;
        }

        if (queue.remove(uuid)) {
            player.sendMessage(MINI.deserialize(PREFIX + "<gray>Du hast die Warteschlange verlassen."));

            // Countdown abbrechen wenn zu wenige Spieler
            if (queue.size() < MIN_PLAYERS && countdownTimer >= 0) {
                cancelCountdown();
                broadcastQueue(MINI.deserialize(PREFIX + "<red>Zu wenige Spieler. Countdown abgebrochen."));
            }

            return true;
        }

        player.sendMessage(MINI.deserialize(PREFIX + "<red>Du bist weder in der Queue noch in einem Spiel!"));
        return false;
    }

    private void startCountdown() {
        countdownTimer = COUNTDOWN_SECONDS;
        countdownTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (countdownTimer <= 0) {
                // Spiel starten
                cancelCountdown();
                tryStartGame();
                return;
            }

            if (countdownTimer <= 5 || countdownTimer == 10 || countdownTimer == 20 || countdownTimer == 30) {
                broadcastQueue(MINI.deserialize(PREFIX + "<gray>Spiel startet in <yellow>" + countdownTimer + " <gray>Sekunden!"));
            }

            countdownTimer--;
        }, 0L, 20L);
    }

    private void cancelCountdown() {
        countdownTimer = -1;
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
    }

    private void tryStartGame() {
        if (queue.size() < MIN_PLAYERS) return;

        TNTRunArena.ArenaData arena = plugin.getTNTRunArena().getFreeArena();
        if (arena == null) {
            broadcastQueue(MINI.deserialize(PREFIX + "<red>Keine Arena verfuegbar! Bitte warten..."));
            return;
        }

        // Alle Spieler aus der Queue ins Spiel nehmen (max MAX_PLAYERS)
        List<Player> players = new ArrayList<>();
        Iterator<UUID> iterator = queue.iterator();
        while (iterator.hasNext() && players.size() < MAX_PLAYERS) {
            UUID uuid = iterator.next();
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null && p.isOnline()) {
                players.add(p);
            }
            iterator.remove();
        }

        if (players.size() < MIN_PLAYERS) {
            // Spieler zurueck in Queue
            for (Player p : players) {
                queue.add(p.getUniqueId());
            }
            broadcastQueue(MINI.deserialize(PREFIX + "<red>Nicht genug Spieler online!"));
            return;
        }

        // Spiel erstellen
        TNTRunGame game = new TNTRunGame(plugin, players, arena);
        for (Player p : players) {
            activeGames.put(p.getUniqueId(), game);
        }

        game.start();
    }

    public boolean isInGame(UUID uuid) {
        return activeGames.containsKey(uuid);
    }

    public boolean isInQueue(UUID uuid) {
        return queue.contains(uuid);
    }

    public TNTRunGame getGame(UUID uuid) {
        return activeGames.get(uuid);
    }

    public void removeGame(UUID uuid) {
        activeGames.remove(uuid);
    }

    public int getQueueSize() {
        return queue.size();
    }

    private void broadcastQueue(net.kyori.adventure.text.Component message) {
        for (UUID uuid : queue) {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage(message);
            }
        }
    }

    public void shutdown() {
        cancelCountdown();
        // Alle aktiven Spiele beenden
        Set<TNTRunGame> games = new HashSet<>(activeGames.values());
        for (TNTRunGame game : games) {
            game.forceEnd();
        }
        queue.clear();
        activeGames.clear();
    }
}
