package de.haaremy.hmy1v1;

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
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DuelGame {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    public enum GameState {
        COUNTDOWN, FIGHTING, GAME_OVER
    }

    private final Hmy1v1 plugin;
    private final Player player1;
    private final Player player2;
    private final DuelArena.ArenaData arena;
    private final DuelKit kit;
    private final boolean ranked;

    private GameState state = GameState.COUNTDOWN;
    private final Set<UUID> spectators = new HashSet<>();
    private boolean frozen = true;

    public DuelGame(Hmy1v1 plugin, Player player1, Player player2, DuelArena.ArenaData arena, DuelKit kit, boolean ranked) {
        this.plugin = plugin;
        this.player1 = player1;
        this.player2 = player2;
        this.arena = arena;
        this.kit = kit;
        this.ranked = ranked;
    }

    public void start() {
        arena.setInUse(true);
        state = GameState.COUNTDOWN;
        frozen = true;

        preparePlayer(player1);
        preparePlayer(player2);

        // Teleport zu Arena-Spawns
        player1.teleport(arena.getSpawn1());
        player2.teleport(arena.getSpawn2());

        // Kit anwenden
        kit.apply(player1);
        kit.apply(player2);

        updateScoreboards();

        // Countdown ueber hmyCore
        String countdownId = "duel_" + player1.getUniqueId().toString().substring(0, 8);
        CountdownManager countdownManager = HmyCore.getInstance().getCountdownManager();
        HmyCountdown countdown = countdownManager.createCountdown(countdownId, 3);
        countdown.forPlayers(Arrays.asList(player1, player2));
        countdown.onFinish(() -> {
            if (state == GameState.COUNTDOWN) {
                state = GameState.FIGHTING;
                frozen = false;

                player1.sendMessage(MINI.deserialize("<dark_purple><bold>1v1</bold></dark_purple> <gray>Kampf!"));
                player2.sendMessage(MINI.deserialize("<dark_purple><bold>1v1</bold></dark_purple> <gray>Kampf!"));
            }
        });
        countdown.start();
    }

    private void preparePlayer(Player player) {
        player.getInventory().clear();
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        player.setFireTicks(0);
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
    }

    /**
     * Wird aufgerufen wenn ein Spieler stirbt.
     */
    public void onPlayerDeath(Player dead) {
        if (state != GameState.FIGHTING) return;

        Player winner = dead.getUniqueId().equals(player1.getUniqueId()) ? player2 : player1;
        endGame(winner, dead);
    }

    /**
     * Wird aufgerufen wenn ein Spieler das Spiel verlaesst.
     */
    public void onPlayerQuit(Player leaver) {
        if (state == GameState.GAME_OVER) return;

        // Countdown abbrechen falls aktiv
        String countdownId = "duel_" + player1.getUniqueId().toString().substring(0, 8);
        HmyCore.getInstance().getCountdownManager().cancelCountdown(countdownId);

        Player winner = leaver.getUniqueId().equals(player1.getUniqueId()) ? player2 : player1;

        state = GameState.GAME_OVER;

        if (winner.isOnline()) {
            winner.sendMessage(MINI.deserialize("<dark_purple><bold>1v1</bold></dark_purple> <gray>Dein Gegner hat das Spiel verlassen. Du gewinnst!"));
            winner.showTitle(Title.title(
                    MINI.deserialize("<gold><bold>SIEG!"),
                    MINI.deserialize("<gray>Gegner hat verlassen"),
                    Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofMillis(500))
            ));

            // ELO + Coins
            applyRewards(winner, leaver);

            // Scoreboard entfernen und zur Lobby senden
            ScoreboardManager sbManager = HmyCore.getInstance().getScoreboardManager();
            sbManager.removeScoreboard(winner);

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                HmyCore.getInstance().getLobbyConnector().sendToLobby(winner);
            }, 60L);
        }

        // Scoreboard des Verlassenden entfernen
        if (leaver.isOnline()) {
            HmyCore.getInstance().getScoreboardManager().removeScoreboard(leaver);
        }

        // Zuschauer entfernen
        removeAllSpectators();

        // Arena freigeben
        arena.setInUse(false);

        // Spiel aus Manager entfernen
        plugin.getDuelManager().removeGame(player1.getUniqueId());
        plugin.getDuelManager().removeGame(player2.getUniqueId());
    }

    private void endGame(Player winner, Player loser) {
        state = GameState.GAME_OVER;

        // Countdown abbrechen falls aktiv
        String countdownId = "duel_" + player1.getUniqueId().toString().substring(0, 8);
        HmyCore.getInstance().getCountdownManager().cancelCountdown(countdownId);

        // Rewards
        applyRewards(winner, loser);

        // Gewinner-Nachricht
        int winnerEloNow = plugin.getDuelElo().getElo(winner.getUniqueId());
        int loserEloNow = plugin.getDuelElo().getElo(loser.getUniqueId());

        Component winMsg = MINI.deserialize(
                "\n<dark_purple><bold>1v1 GEWONNEN!</bold></dark_purple>\n" +
                "<gray>Gegner: <yellow>" + loser.getName() + "\n" +
                "<gray>Kit: " + kit.getDisplayName() + "\n" +
                "<gray>Coins: <yellow>+30\n" +
                (ranked ? "<gray>ELO: <green>" + winnerEloNow + "\n" : "") +
                ""
        );
        Component loseMsg = MINI.deserialize(
                "\n<red><bold>1v1 VERLOREN!</bold></red>\n" +
                "<gray>Gegner: <yellow>" + winner.getName() + "\n" +
                "<gray>Kit: " + kit.getDisplayName() + "\n" +
                "<gray>Coins: <yellow>+5\n" +
                (ranked ? "<gray>ELO: <red>" + loserEloNow + "\n" : "") +
                ""
        );

        // Titel
        Title winnerTitle = Title.title(
                MINI.deserialize("<gold><bold>SIEG!"),
                MINI.deserialize("<yellow>+30 Coins" + (ranked ? " <gray>| <green>ELO: " + winnerEloNow : "")),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(1))
        );
        Title loserTitle = Title.title(
                MINI.deserialize("<red><bold>NIEDERLAGE"),
                MINI.deserialize("<yellow>+5 Coins" + (ranked ? " <gray>| <red>ELO: " + loserEloNow : "")),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(1))
        );

        if (winner.isOnline()) {
            winner.showTitle(winnerTitle);
            winner.sendMessage(winMsg);
            winner.playSound(winner.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }
        if (loser.isOnline()) {
            loser.showTitle(loserTitle);
            loser.sendMessage(loseMsg);
            loser.playSound(loser.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.5f, 1.0f);
        }

        // Scoreboards entfernen
        ScoreboardManager sbManager = HmyCore.getInstance().getScoreboardManager();
        sbManager.removeScoreboard(player1);
        sbManager.removeScoreboard(player2);

        // Arena freigeben
        arena.setInUse(false);

        // Zuschauer und Spieler nach Verzoegerung zur Lobby senden
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (winner.isOnline()) {
                HmyCore.getInstance().getLobbyConnector().sendToLobby(winner);
            }
            if (loser.isOnline()) {
                HmyCore.getInstance().getLobbyConnector().sendToLobby(loser);
            }
            removeAllSpectators();

            // Spiel aus Manager entfernen
            plugin.getDuelManager().removeGame(player1.getUniqueId());
            plugin.getDuelManager().removeGame(player2.getUniqueId());
        }, 60L);
    }

    private void applyRewards(Player winner, Player loser) {
        UUID winnerUuid = winner.getUniqueId();
        UUID loserUuid = loser.getUniqueId();

        // Coins vergeben
        EconomyManager economy = HmyCore.getInstance().getEconomyManager();
        economy.addCoins(winnerUuid, 30);
        economy.addCoins(loserUuid, 5);

        // ELO nur bei Ranked
        if (ranked) {
            DuelElo elo = plugin.getDuelElo();
            int winnerElo = elo.getElo(winnerUuid);
            int loserElo = elo.getElo(loserUuid);
            int[] newElo = elo.calculateNewElo(winnerElo, loserElo);

            elo.setElo(winnerUuid, newElo[0]);
            elo.setElo(loserUuid, newElo[1]);
        }

        // Wins/Losses immer tracken
        DuelElo elo = plugin.getDuelElo();
        elo.addWin(winnerUuid);
        elo.addLoss(loserUuid);
    }

    // --- Zuschauer ---

    public void addSpectator(Player spectator) {
        spectators.add(spectator.getUniqueId());
        spectator.setGameMode(GameMode.SPECTATOR);

        // Teleport zur Mitte der Arena
        Location mid = arena.getSpawn1().clone().add(arena.getSpawn2()).multiply(0.5);
        mid.setWorld(arena.getSpawn1().getWorld());
        spectator.teleport(mid);

        spectator.sendMessage(MINI.deserialize("<dark_purple><bold>1v1</bold></dark_purple> <gray>Du beobachtest: <yellow>" +
                player1.getName() + " <gray>vs <yellow>" + player2.getName()));

        // Spieler informieren
        player1.sendMessage(MINI.deserialize("<dark_purple><bold>1v1</bold></dark_purple> <gray>" + spectator.getName() + " schaut zu."));
        player2.sendMessage(MINI.deserialize("<dark_purple><bold>1v1</bold></dark_purple> <gray>" + spectator.getName() + " schaut zu."));
    }

    public void removeSpectator(Player spectator) {
        if (!spectators.remove(spectator.getUniqueId())) return;

        spectator.setGameMode(GameMode.SURVIVAL);
        spectator.sendMessage(MINI.deserialize("<dark_purple><bold>1v1</bold></dark_purple> <gray>Du beobachtest nicht mehr."));

        // Zur Lobby senden
        HmyCore.getInstance().getLobbyConnector().sendToLobby(spectator);
    }

    private void removeAllSpectators() {
        for (UUID uuid : new HashSet<>(spectators)) {
            Player spectator = plugin.getServer().getPlayer(uuid);
            if (spectator != null && spectator.isOnline()) {
                removeSpectator(spectator);
            }
        }
        spectators.clear();
    }

    // --- Scoreboards ---

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

        Player opponent = player.getUniqueId().equals(player1.getUniqueId()) ? player2 : player1;

        sb.setTitle("<dark_purple><bold>1v1 DUELL</bold></dark_purple>");
        sb.setLine(0, "");
        sb.setLine(1, "<gray>Gegner: <yellow>" + opponent.getName());
        sb.setLine(2, "<gray>Kit: " + kit.getDisplayName());
        sb.setLine(3, "<gray>Modus: " + (ranked ? "<green>Ranked" : "<yellow>Unranked"));
        sb.setLine(4, "");
        sb.setLine(5, "<gray>Status: " + getStateDisplay());
        sb.setLine(6, "");
        sb.setLine(7, "<gray>Zuschauer: <white>" + spectators.size());
        sb.setLine(8, "");
        sb.setLine(9, "<dark_gray>mc.haaremy.de");
    }

    private String getStateDisplay() {
        return switch (state) {
            case COUNTDOWN -> "<yellow>Countdown";
            case FIGHTING -> "<green>Kampf!";
            case GAME_OVER -> "<red>Spielende";
        };
    }

    // --- Getter ---

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

    public boolean isRanked() {
        return ranked;
    }

    public DuelKit getKit() {
        return kit;
    }

    public boolean isPlayer(UUID uuid) {
        return player1.getUniqueId().equals(uuid) || player2.getUniqueId().equals(uuid);
    }

    public boolean isSpectator(UUID uuid) {
        return spectators.contains(uuid);
    }

    public Set<UUID> getSpectators() {
        return spectators;
    }

    public DuelArena.ArenaData getArena() {
        return arena;
    }
}
