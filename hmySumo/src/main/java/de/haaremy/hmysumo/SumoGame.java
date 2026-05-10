package de.haaremy.hmysumo;

import de.haaremy.hmycore.HmyCore;
import de.haaremy.hmycore.countdown.CountdownManager;
import de.haaremy.hmycore.countdown.HmyCountdown;
import de.haaremy.hmycore.economy.EconomyManager;
import de.haaremy.hmycore.scoreboard.HmyScoreboard;
import de.haaremy.hmycore.scoreboard.ScoreboardManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;

public class SumoGame {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    public enum GameState {
        COUNTDOWN, FIGHTING, ROUND_END, GAME_OVER
    }

    private final HmySumo plugin;
    private final Player player1;
    private final Player player2;
    private final SumoArena.ArenaData arena;

    private int score1 = 0;
    private int score2 = 0;
    private int currentRound = 1;
    private GameState state = GameState.COUNTDOWN;

    private boolean frozen = true;

    public SumoGame(HmySumo plugin, Player player1, Player player2, SumoArena.ArenaData arena) {
        this.plugin = plugin;
        this.player1 = player1;
        this.player2 = player2;
        this.arena = arena;
    }

    public void start() {
        arena.setInUse(true);
        preparePlayer(player1);
        preparePlayer(player2);

        // Spieler zu Spawn-Positionen teleportieren
        player1.teleport(arena.getSpawn1());
        player2.teleport(arena.getSpawn2());

        updateScoreboards();
        startRound();
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

    public void startRound() {
        state = GameState.COUNTDOWN;
        frozen = true;

        // Spieler zurueck zu Spawns
        player1.teleport(arena.getSpawn1());
        player2.teleport(arena.getSpawn2());

        // Spieler vorbereiten
        player1.setHealth(20.0);
        player2.setHealth(20.0);

        updateScoreboards();

        // Countdown ueber hmyCore
        String countdownId = "sumo_" + player1.getUniqueId().toString().substring(0, 8);
        CountdownManager countdownManager = HmyCore.getInstance().getCountdownManager();
        HmyCountdown countdown = countdownManager.createCountdown(countdownId, 3);
        countdown.forPlayers(Arrays.asList(player1, player2));
        countdown.onFinish(() -> {
            if (state == GameState.COUNTDOWN) {
                state = GameState.FIGHTING;
                frozen = false;

                player1.sendMessage(MINI.deserialize("<gold><bold>SUMO</bold></gold> <gray>Runde <yellow>" + currentRound + " <gray>- Kampf!"));
                player2.sendMessage(MINI.deserialize("<gold><bold>SUMO</bold></gold> <gray>Runde <yellow>" + currentRound + " <gray>- Kampf!"));
            }
        });
        countdown.start();
    }

    /**
     * Wird aufgerufen wenn ein Spieler ins Void faellt.
     */
    public void onPlayerFall(Player fallen) {
        if (state != GameState.FIGHTING) return;

        state = GameState.ROUND_END;
        frozen = true;

        Player winner;
        if (fallen.getUniqueId().equals(player1.getUniqueId())) {
            score2++;
            winner = player2;
        } else {
            score1++;
            winner = player1;
        }

        // Gefallenen Spieler zurueck teleportieren
        fallen.teleport(fallen.getUniqueId().equals(player1.getUniqueId()) ? arena.getSpawn1() : arena.getSpawn2());
        fallen.setHealth(20.0);

        // Runden-Ergebnis anzeigen
        Component winTitle = MINI.deserialize("<green><bold>Punkt!");
        Component loseTitle = MINI.deserialize("<red><bold>Gefallen!");
        Title winnerTitle = Title.title(winTitle, MINI.deserialize("<gray>Score: <yellow>" + getScore(winner)),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofMillis(500)));
        Title loserTitle = Title.title(loseTitle, MINI.deserialize("<gray>Score: <yellow>" + getScore(fallen)),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofMillis(500)));

        winner.showTitle(winnerTitle);
        fallen.showTitle(loserTitle);
        winner.playSound(winner.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
        fallen.playSound(fallen.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);

        updateScoreboards();

        // Pruefen ob Spiel vorbei ist (Best-of-3: erster mit 2 Punkten)
        if (score1 >= 2 || score2 >= 2) {
            // Kurze Verzoegerung, dann Spielende
            plugin.getServer().getScheduler().runTaskLater(plugin, this::endGame, 60L); // 3 Sekunden
        } else {
            // Naechste Runde nach kurzer Pause
            currentRound++;
            plugin.getServer().getScheduler().runTaskLater(plugin, this::startRound, 60L); // 3 Sekunden
        }
    }

    private void endGame() {
        state = GameState.GAME_OVER;

        Player winner;
        Player loser;
        if (score1 >= 2) {
            winner = player1;
            loser = player2;
        } else {
            winner = player2;
            loser = player1;
        }

        UUID winnerUuid = winner.getUniqueId();
        UUID loserUuid = loser.getUniqueId();

        // ELO berechnen
        SumoElo elo = plugin.getSumoElo();
        int winnerElo = elo.getElo(winnerUuid);
        int loserElo = elo.getElo(loserUuid);
        int[] newElo = elo.calculateNewElo(winnerElo, loserElo);

        elo.setElo(winnerUuid, newElo[0]);
        elo.setElo(loserUuid, newElo[1]);
        elo.addWin(winnerUuid);
        elo.addLoss(loserUuid);

        int eloGain = newElo[0] - winnerElo;
        int eloLoss = loserElo - newElo[1];

        // Coins vergeben
        EconomyManager economy = HmyCore.getInstance().getEconomyManager();
        economy.addCoins(winnerUuid, 25);

        // Gewinner-Nachricht
        Component winMsg = MINI.deserialize(
                "\n<gold><bold>SUMO GEWONNEN!</bold></gold>\n" +
                "<gray>Ergebnis: <green>" + score1 + " <gray>- <red>" + score2 + "\n" +
                "<gray>Coins: <yellow>+25\n" +
                "<gray>ELO: <green>+" + eloGain + " <dark_gray>(" + newElo[0] + ")\n"
        );
        Component loseMsg = MINI.deserialize(
                "\n<red><bold>SUMO VERLOREN!</bold></red>\n" +
                "<gray>Ergebnis: <red>" + score1 + " <gray>- <green>" + score2 + "\n" +
                "<gray>ELO: <red>-" + eloLoss + " <dark_gray>(" + newElo[1] + ")\n"
        );

        // Titel
        Title winnerEndTitle = Title.title(
                MINI.deserialize("<gold><bold>SIEG!"),
                MINI.deserialize("<yellow>+25 Coins <gray>| <green>+" + eloGain + " ELO"),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(1))
        );
        Title loserEndTitle = Title.title(
                MINI.deserialize("<red><bold>NIEDERLAGE"),
                MINI.deserialize("<red>-" + eloLoss + " ELO"),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(1))
        );

        winner.showTitle(winnerEndTitle);
        winner.sendMessage(winMsg);
        loser.showTitle(loserEndTitle);
        loser.sendMessage(loseMsg);

        winner.playSound(winner.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        loser.playSound(loser.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.5f, 1.0f);

        // Scoreboards entfernen
        ScoreboardManager sbManager = HmyCore.getInstance().getScoreboardManager();
        sbManager.removeScoreboard(player1);
        sbManager.removeScoreboard(player2);

        // Arena freigeben
        arena.setInUse(false);

        // Spieler nach kurzer Verzoegerung zur Lobby senden
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            HmyCore.getInstance().getLobbyConnector().sendToLobby(winner);
            HmyCore.getInstance().getLobbyConnector().sendToLobby(loser);

            // Spiel aus dem Manager entfernen
            plugin.getSumoManager().removeGame(player1.getUniqueId());
            plugin.getSumoManager().removeGame(player2.getUniqueId());
        }, 60L); // 3 Sekunden
    }

    /**
     * Wird aufgerufen wenn ein Spieler das Spiel verlaesst (disconnect/leave).
     */
    public void onPlayerLeave(Player leaver) {
        state = GameState.GAME_OVER;

        // Countdown abbrechen falls aktiv
        String countdownId = "sumo_" + player1.getUniqueId().toString().substring(0, 8);
        HmyCore.getInstance().getCountdownManager().cancelCountdown(countdownId);

        Player winner = leaver.getUniqueId().equals(player1.getUniqueId()) ? player2 : player1;

        if (winner.isOnline()) {
            winner.sendMessage(MINI.deserialize("<gold><bold>SUMO</bold></gold> <gray>Dein Gegner hat das Spiel verlassen. Du gewinnst!"));
            winner.showTitle(Title.title(
                    MINI.deserialize("<gold><bold>SIEG!"),
                    MINI.deserialize("<gray>Gegner hat verlassen"),
                    Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofMillis(500))
            ));

            // ELO + Coins fuer Gewinner
            SumoElo elo = plugin.getSumoElo();
            UUID winnerUuid = winner.getUniqueId();
            UUID loserUuid = leaver.getUniqueId();

            int winnerEloVal = elo.getElo(winnerUuid);
            int loserEloVal = elo.getElo(loserUuid);
            int[] newElo = elo.calculateNewElo(winnerEloVal, loserEloVal);

            elo.setElo(winnerUuid, newElo[0]);
            elo.setElo(loserUuid, newElo[1]);
            elo.addWin(winnerUuid);
            elo.addLoss(loserUuid);

            HmyCore.getInstance().getEconomyManager().addCoins(winnerUuid, 25);

            // Scoreboard entfernen und zur Lobby senden
            ScoreboardManager sbManager = HmyCore.getInstance().getScoreboardManager();
            sbManager.removeScoreboard(winner);

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                HmyCore.getInstance().getLobbyConnector().sendToLobby(winner);
            }, 60L);
        }

        // Scoreboard des Verlassenden entfernen (falls noch online)
        if (leaver.isOnline()) {
            HmyCore.getInstance().getScoreboardManager().removeScoreboard(leaver);
        }

        // Arena freigeben
        arena.setInUse(false);

        // Spiel aus Manager entfernen
        plugin.getSumoManager().removeGame(player1.getUniqueId());
        plugin.getSumoManager().removeGame(player2.getUniqueId());
    }

    private void updateScoreboards() {
        updateScoreboard(player1);
        updateScoreboard(player2);
    }

    private void updateScoreboard(Player player) {
        if (!player.isOnline()) return;

        ScoreboardManager sbManager = HmyCore.getInstance().getScoreboardManager();
        HmyScoreboard sb = sbManager.getScoreboard(player);
        if (sb == null) {
            sb = sbManager.createScoreboard(player);
        }

        sb.setTitle("<gold><bold>SUMO</bold></gold>");
        sb.setLine(0, "");
        sb.setLine(1, "<gray>Runde: <yellow>" + currentRound + "<gray>/3");
        sb.setLine(2, "");
        sb.setLine(3, "<white>" + player1.getName() + "<gray>: <green>" + score1);
        sb.setLine(4, "<white>" + player2.getName() + "<gray>: <green>" + score2);
        sb.setLine(5, "");
        sb.setLine(6, "<gray>Status: " + getStateDisplay());
        sb.setLine(7, "");
        sb.setLine(8, "<dark_gray>mc.haaremy.de");
    }

    private String getStateDisplay() {
        return switch (state) {
            case COUNTDOWN -> "<yellow>Countdown";
            case FIGHTING -> "<green>Kampf!";
            case ROUND_END -> "<gold>Rundenende";
            case GAME_OVER -> "<red>Spielende";
        };
    }

    private String getScore(Player player) {
        if (player.getUniqueId().equals(player1.getUniqueId())) {
            return score1 + " - " + score2;
        }
        return score2 + " - " + score1;
    }

    // Getter

    public Player getPlayer1() {
        return player1;
    }

    public Player getPlayer2() {
        return player2;
    }

    public GameState getState() {
        return state;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public boolean isPlayer(UUID uuid) {
        return player1.getUniqueId().equals(uuid) || player2.getUniqueId().equals(uuid);
    }

    public SumoArena.ArenaData getArena() {
        return arena;
    }
}
