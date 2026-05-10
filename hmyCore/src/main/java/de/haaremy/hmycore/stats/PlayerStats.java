package de.haaremy.hmycore.stats;

import java.util.UUID;

public class PlayerStats {

    private final UUID uuid;
    private final String gameType;
    private int wins;
    private int losses;
    private int kills;
    private int deaths;
    private long playtimeSeconds;

    public PlayerStats(UUID uuid, String gameType) {
        this.uuid = uuid;
        this.gameType = gameType;
        this.wins = 0;
        this.losses = 0;
        this.kills = 0;
        this.deaths = 0;
        this.playtimeSeconds = 0;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getGameType() {
        return gameType;
    }

    public int getWins() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public void addWin() {
        this.wins++;
    }

    public int getLosses() {
        return losses;
    }

    public void setLosses(int losses) {
        this.losses = losses;
    }

    public void addLoss() {
        this.losses++;
    }

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public void addKill() {
        this.kills++;
    }

    public int getDeaths() {
        return deaths;
    }

    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }

    public void addDeath() {
        this.deaths++;
    }

    public long getPlaytimeSeconds() {
        return playtimeSeconds;
    }

    public void setPlaytimeSeconds(long playtimeSeconds) {
        this.playtimeSeconds = playtimeSeconds;
    }

    public void addPlaytime(long seconds) {
        this.playtimeSeconds += seconds;
    }

    public double getKdr() {
        if (deaths == 0) return kills;
        return (double) kills / deaths;
    }

    public double getWinRate() {
        int total = wins + losses;
        if (total == 0) return 0;
        return (double) wins / total * 100;
    }
}
