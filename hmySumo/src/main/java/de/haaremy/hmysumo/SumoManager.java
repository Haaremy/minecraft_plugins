package de.haaremy.hmysumo;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SumoManager {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final HmySumo plugin;
    private final Queue<UUID> queue = new LinkedList<>();
    private final Map<UUID, SumoGame> activeGames = new ConcurrentHashMap<>();

    public SumoManager(HmySumo plugin) {
        this.plugin = plugin;
    }

    /**
     * Spieler tritt der Warteschlange bei.
     */
    public boolean joinQueue(Player player) {
        UUID uuid = player.getUniqueId();

        if (isInGame(uuid)) {
            player.sendMessage(MINI.deserialize("<gold><bold>SUMO</bold></gold> <red>Du bist bereits in einem Spiel!"));
            return false;
        }

        if (queue.contains(uuid)) {
            player.sendMessage(MINI.deserialize("<gold><bold>SUMO</bold></gold> <red>Du bist bereits in der Warteschlange!"));
            return false;
        }

        queue.add(uuid);
        player.sendMessage(MINI.deserialize("<gold><bold>SUMO</bold></gold> <gray>Du bist der Warteschlange beigetreten! <dark_gray>(" + queue.size() + " Spieler)"));

        // Pruefen ob genug Spieler fuer ein Spiel
        tryStartGame();
        return true;
    }

    /**
     * Spieler verlaesst die Warteschlange.
     */
    public boolean leaveQueue(Player player) {
        UUID uuid = player.getUniqueId();

        // Wenn in einem Spiel, Spiel verlassen
        if (isInGame(uuid)) {
            SumoGame game = activeGames.get(uuid);
            if (game != null) {
                game.onPlayerLeave(player);
            }
            return true;
        }

        if (queue.remove(uuid)) {
            player.sendMessage(MINI.deserialize("<gold><bold>SUMO</bold></gold> <gray>Du hast die Warteschlange verlassen."));
            return true;
        }

        player.sendMessage(MINI.deserialize("<gold><bold>SUMO</bold></gold> <red>Du bist weder in der Queue noch in einem Spiel!"));
        return false;
    }

    /**
     * Versucht ein Spiel zu starten wenn genug Spieler und eine Arena frei ist.
     */
    private void tryStartGame() {
        if (queue.size() < 2) return;

        SumoArena.ArenaData arena = plugin.getSumoArena().getFreeArena();
        if (arena == null) {
            // Alle Arenen belegt
            return;
        }

        UUID uuid1 = queue.poll();
        UUID uuid2 = queue.poll();

        if (uuid1 == null || uuid2 == null) return;

        Player p1 = plugin.getServer().getPlayer(uuid1);
        Player p2 = plugin.getServer().getPlayer(uuid2);

        if (p1 == null || !p1.isOnline()) {
            // Spieler 1 nicht mehr online, Spieler 2 zurueck in Queue
            if (p2 != null && p2.isOnline()) {
                queue.add(uuid2);
            }
            tryStartGame();
            return;
        }

        if (p2 == null || !p2.isOnline()) {
            // Spieler 2 nicht mehr online, Spieler 1 zurueck in Queue
            queue.add(uuid1);
            tryStartGame();
            return;
        }

        // Spiel erstellen und starten
        SumoGame game = new SumoGame(plugin, p1, p2, arena);
        activeGames.put(uuid1, game);
        activeGames.put(uuid2, game);

        p1.sendMessage(MINI.deserialize("<gold><bold>SUMO</bold></gold> <gray>Spiel gefunden! Gegner: <yellow>" + p2.getName()));
        p2.sendMessage(MINI.deserialize("<gold><bold>SUMO</bold></gold> <gray>Spiel gefunden! Gegner: <yellow>" + p1.getName()));

        game.start();
    }

    /**
     * Prueft ob ein Spieler in einem aktiven Spiel ist.
     */
    public boolean isInGame(UUID uuid) {
        return activeGames.containsKey(uuid);
    }

    /**
     * Prueft ob ein Spieler in der Queue ist.
     */
    public boolean isInQueue(UUID uuid) {
        return queue.contains(uuid);
    }

    /**
     * Gibt das Spiel eines Spielers zurueck.
     */
    public SumoGame getGame(UUID uuid) {
        return activeGames.get(uuid);
    }

    /**
     * Entfernt ein Spiel fuer eine UUID.
     */
    public void removeGame(UUID uuid) {
        activeGames.remove(uuid);
    }

    /**
     * Faehrt den Manager herunter, beendet alle Spiele.
     */
    public void shutdown() {
        // Alle Spieler aus der Queue entfernen
        queue.clear();

        // Alle aktiven Spiele beenden
        for (SumoGame game : activeGames.values()) {
            game.getArena().setInUse(false);
        }
        activeGames.clear();
    }

    public int getQueueSize() {
        return queue.size();
    }
}
