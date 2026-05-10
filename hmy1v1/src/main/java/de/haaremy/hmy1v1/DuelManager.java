package de.haaremy.hmy1v1;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DuelManager {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final String PREFIX = "<dark_purple><bold>1v1</bold></dark_purple> ";

    private final Hmy1v1 plugin;

    // Separate Queues fuer Ranked und Unranked
    private final Queue<UUID> rankedQueue = new LinkedList<>();
    private final Queue<UUID> unrankedQueue = new LinkedList<>();

    // Aktive Spiele: UUID -> DuelGame
    private final Map<UUID, DuelGame> activeGames = new ConcurrentHashMap<>();

    // Challenges: Herausforderer-UUID -> Herausgeforderter-UUID
    private final Map<UUID, UUID> pendingChallenges = new HashMap<>();
    // Zeitstempel der Challenge
    private final Map<UUID, Long> challengeTimestamps = new HashMap<>();

    // Kit-Auswahl pro Spieler (Standard: IRON)
    private final Map<UUID, DuelKit> selectedKits = new HashMap<>();

    // Queue-Typ pro Spieler
    private final Map<UUID, Boolean> queueRanked = new HashMap<>();

    private static final long CHALLENGE_TIMEOUT_MS = 30_000; // 30 Sekunden

    public DuelManager(Hmy1v1 plugin) {
        this.plugin = plugin;

        // Cleanup-Task fuer abgelaufene Challenges (alle 5 Sekunden)
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::cleanupExpiredChallenges, 100L, 100L);
    }

    // --- Queue ---

    public boolean joinQueue(Player player, boolean ranked) {
        UUID uuid = player.getUniqueId();

        if (isInGame(uuid)) {
            player.sendMessage(MINI.deserialize(PREFIX + "<red>Du bist bereits in einem Duell!"));
            return false;
        }

        if (isInQueue(uuid)) {
            player.sendMessage(MINI.deserialize(PREFIX + "<red>Du bist bereits in der Warteschlange!"));
            return false;
        }

        Queue<UUID> queue = ranked ? rankedQueue : unrankedQueue;
        queue.add(uuid);
        queueRanked.put(uuid, ranked);

        String modeStr = ranked ? "<green>Ranked" : "<yellow>Unranked";
        player.sendMessage(MINI.deserialize(PREFIX + "<gray>Du bist der " + modeStr + " <gray>Warteschlange beigetreten! <dark_gray>(" + queue.size() + " Spieler)"));

        matchFromQueue(ranked);
        return true;
    }

    public boolean leaveQueue(Player player) {
        UUID uuid = player.getUniqueId();

        // Wenn in einem Spiel als Zuschauer
        DuelGame spectatingGame = getSpectatingGame(uuid);
        if (spectatingGame != null) {
            spectatingGame.removeSpectator(player);
            return true;
        }

        // Wenn in einem Spiel
        if (isInGame(uuid)) {
            DuelGame game = activeGames.get(uuid);
            if (game != null) {
                game.onPlayerQuit(player);
            }
            return true;
        }

        // Aus Queue entfernen
        if (rankedQueue.remove(uuid) || unrankedQueue.remove(uuid)) {
            queueRanked.remove(uuid);
            player.sendMessage(MINI.deserialize(PREFIX + "<gray>Du hast die Warteschlange verlassen."));
            return true;
        }

        player.sendMessage(MINI.deserialize(PREFIX + "<red>Du bist weder in der Queue noch in einem Duell!"));
        return false;
    }

    private void matchFromQueue(boolean ranked) {
        Queue<UUID> queue = ranked ? rankedQueue : unrankedQueue;

        if (queue.size() < 2) return;

        DuelArena.ArenaData arena = plugin.getDuelArena().getFreeArena();
        if (arena == null) return;

        UUID uuid1 = queue.poll();
        UUID uuid2 = queue.poll();

        if (uuid1 == null || uuid2 == null) return;

        Player p1 = plugin.getServer().getPlayer(uuid1);
        Player p2 = plugin.getServer().getPlayer(uuid2);

        if (p1 == null || !p1.isOnline()) {
            queueRanked.remove(uuid1);
            if (p2 != null && p2.isOnline()) {
                queue.add(uuid2);
            } else {
                queueRanked.remove(uuid2);
            }
            matchFromQueue(ranked);
            return;
        }

        if (p2 == null || !p2.isOnline()) {
            queueRanked.remove(uuid2);
            queue.add(uuid1);
            matchFromQueue(ranked);
            return;
        }

        queueRanked.remove(uuid1);
        queueRanked.remove(uuid2);

        // Kit des Herausforderers verwenden, oder IRON als Standard
        DuelKit kit = selectedKits.getOrDefault(uuid1, DuelKit.IRON);

        startGame(p1, p2, kit, ranked);
    }

    // --- Challenges ---

    public void sendChallenge(Player challenger, Player target) {
        UUID challengerUuid = challenger.getUniqueId();
        UUID targetUuid = target.getUniqueId();

        if (isInGame(challengerUuid)) {
            challenger.sendMessage(MINI.deserialize(PREFIX + "<red>Du bist bereits in einem Duell!"));
            return;
        }

        if (isInGame(targetUuid)) {
            challenger.sendMessage(MINI.deserialize(PREFIX + "<red>" + target.getName() + " ist bereits in einem Duell!"));
            return;
        }

        if (isInQueue(challengerUuid)) {
            challenger.sendMessage(MINI.deserialize(PREFIX + "<red>Du bist in der Warteschlange! Verlasse sie zuerst mit <yellow>/duel leave<red>."));
            return;
        }

        if (challengerUuid.equals(targetUuid)) {
            challenger.sendMessage(MINI.deserialize(PREFIX + "<red>Du kannst dich nicht selbst herausfordern!"));
            return;
        }

        // Bestehende Challenge ueberschreiben
        pendingChallenges.put(challengerUuid, targetUuid);
        challengeTimestamps.put(challengerUuid, System.currentTimeMillis());

        DuelKit kit = selectedKits.getOrDefault(challengerUuid, DuelKit.IRON);

        challenger.sendMessage(MINI.deserialize(PREFIX + "<gray>Du hast <yellow>" + target.getName() + " <gray>herausgefordert! Kit: " + kit.getDisplayName()));
        target.sendMessage(MINI.deserialize(
                PREFIX + "<yellow>" + challenger.getName() + " <gray>fordert dich zum Duell heraus! Kit: " + kit.getDisplayName() + "\n" +
                PREFIX + "<green>/duel accept <gray>- Annehmen | <red>/duel deny <gray>- Ablehnen\n" +
                PREFIX + "<dark_gray>(Laueft in 30 Sekunden ab)"
        ));
    }

    public void acceptChallenge(Player target) {
        UUID targetUuid = target.getUniqueId();

        // Suche eine Challenge die an diesen Spieler gerichtet ist
        UUID challengerUuid = null;
        for (Map.Entry<UUID, UUID> entry : pendingChallenges.entrySet()) {
            if (entry.getValue().equals(targetUuid)) {
                challengerUuid = entry.getKey();
                break;
            }
        }

        if (challengerUuid == null) {
            target.sendMessage(MINI.deserialize(PREFIX + "<red>Du hast keine offene Herausforderung!"));
            return;
        }

        Player challenger = plugin.getServer().getPlayer(challengerUuid);
        if (challenger == null || !challenger.isOnline()) {
            target.sendMessage(MINI.deserialize(PREFIX + "<red>Der Herausforderer ist nicht mehr online!"));
            pendingChallenges.remove(challengerUuid);
            challengeTimestamps.remove(challengerUuid);
            return;
        }

        if (isInGame(targetUuid)) {
            target.sendMessage(MINI.deserialize(PREFIX + "<red>Du bist bereits in einem Duell!"));
            return;
        }

        DuelArena.ArenaData arena = plugin.getDuelArena().getFreeArena();
        if (arena == null) {
            target.sendMessage(MINI.deserialize(PREFIX + "<red>Keine Arena verfuegbar! Versuche es spaeter erneut."));
            return;
        }

        // Challenge entfernen
        pendingChallenges.remove(challengerUuid);
        challengeTimestamps.remove(challengerUuid);

        // Kit des Herausforderers
        DuelKit kit = selectedKits.getOrDefault(challengerUuid, DuelKit.IRON);

        // Spiel starten (Challenges sind immer Unranked)
        startGame(challenger, target, kit, false);
    }

    public void denyChallenge(Player target) {
        UUID targetUuid = target.getUniqueId();

        UUID challengerUuid = null;
        for (Map.Entry<UUID, UUID> entry : pendingChallenges.entrySet()) {
            if (entry.getValue().equals(targetUuid)) {
                challengerUuid = entry.getKey();
                break;
            }
        }

        if (challengerUuid == null) {
            target.sendMessage(MINI.deserialize(PREFIX + "<red>Du hast keine offene Herausforderung!"));
            return;
        }

        pendingChallenges.remove(challengerUuid);
        challengeTimestamps.remove(challengerUuid);

        target.sendMessage(MINI.deserialize(PREFIX + "<gray>Herausforderung abgelehnt."));

        Player challenger = plugin.getServer().getPlayer(challengerUuid);
        if (challenger != null && challenger.isOnline()) {
            challenger.sendMessage(MINI.deserialize(PREFIX + "<red>" + target.getName() + " hat deine Herausforderung abgelehnt."));
        }
    }

    private void cleanupExpiredChallenges() {
        long now = System.currentTimeMillis();
        var iterator = challengeTimestamps.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (now - entry.getValue() > CHALLENGE_TIMEOUT_MS) {
                UUID challengerUuid = entry.getKey();
                UUID targetUuid = pendingChallenges.remove(challengerUuid);
                iterator.remove();

                Player challenger = plugin.getServer().getPlayer(challengerUuid);
                if (challenger != null && challenger.isOnline()) {
                    challenger.sendMessage(MINI.deserialize(PREFIX + "<red>Deine Herausforderung ist abgelaufen."));
                }
                if (targetUuid != null) {
                    Player target = plugin.getServer().getPlayer(targetUuid);
                    if (target != null && target.isOnline()) {
                        target.sendMessage(MINI.deserialize(PREFIX + "<red>Die Herausforderung von " + (challenger != null ? challenger.getName() : "???") + " ist abgelaufen."));
                    }
                }
            }
        }
    }

    // --- Spiel starten ---

    private void startGame(Player p1, Player p2, DuelKit kit, boolean ranked) {
        DuelArena.ArenaData arena = plugin.getDuelArena().getFreeArena();
        if (arena == null) {
            p1.sendMessage(MINI.deserialize(PREFIX + "<red>Keine Arena verfuegbar!"));
            p2.sendMessage(MINI.deserialize(PREFIX + "<red>Keine Arena verfuegbar!"));
            return;
        }

        DuelGame game = new DuelGame(plugin, p1, p2, arena, kit, ranked);
        activeGames.put(p1.getUniqueId(), game);
        activeGames.put(p2.getUniqueId(), game);

        String modeStr = ranked ? "<green>Ranked" : "<yellow>Unranked";
        p1.sendMessage(MINI.deserialize(PREFIX + "<gray>Duell gefunden! Gegner: <yellow>" + p2.getName() + " <gray>| Kit: " + kit.getDisplayName() + " <gray>| " + modeStr));
        p2.sendMessage(MINI.deserialize(PREFIX + "<gray>Duell gefunden! Gegner: <yellow>" + p1.getName() + " <gray>| Kit: " + kit.getDisplayName() + " <gray>| " + modeStr));

        game.start();
    }

    // --- Kit-Auswahl ---

    public void setSelectedKit(UUID uuid, DuelKit kit) {
        selectedKits.put(uuid, kit);
    }

    public DuelKit getSelectedKit(UUID uuid) {
        return selectedKits.getOrDefault(uuid, DuelKit.IRON);
    }

    // --- Zuschauer ---

    public DuelGame getSpectatingGame(UUID uuid) {
        for (DuelGame game : activeGames.values()) {
            if (game.isSpectator(uuid)) {
                return game;
            }
        }
        return null;
    }

    // --- Hilfsmethoden ---

    public boolean isInGame(UUID uuid) {
        return activeGames.containsKey(uuid);
    }

    public boolean isInQueue(UUID uuid) {
        return rankedQueue.contains(uuid) || unrankedQueue.contains(uuid);
    }

    public DuelGame getGame(UUID uuid) {
        return activeGames.get(uuid);
    }

    public void removeGame(UUID uuid) {
        activeGames.remove(uuid);
    }

    /**
     * Gibt ein aktives Spiel zurueck, in dem der genannte Spieler spielt (fuer Spectate).
     */
    public DuelGame getGameByPlayerName(String name) {
        for (DuelGame game : activeGames.values()) {
            if (game.getPlayer1().getName().equalsIgnoreCase(name) || game.getPlayer2().getName().equalsIgnoreCase(name)) {
                return game;
            }
        }
        return null;
    }

    public int getQueueSize(boolean ranked) {
        return ranked ? rankedQueue.size() : unrankedQueue.size();
    }

    public void shutdown() {
        rankedQueue.clear();
        unrankedQueue.clear();
        pendingChallenges.clear();
        challengeTimestamps.clear();

        for (DuelGame game : activeGames.values()) {
            game.getArena().setInUse(false);
        }
        activeGames.clear();
    }
}
