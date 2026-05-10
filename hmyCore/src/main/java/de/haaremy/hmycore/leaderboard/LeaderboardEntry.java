package de.haaremy.hmycore.leaderboard;

import java.util.UUID;

public final class LeaderboardEntry {

    private final UUID uuid;
    private final String name;
    private final double score;

    public LeaderboardEntry(UUID uuid, String name, double score) {
        this.uuid = uuid;
        this.name = name;
        this.score = score;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public double getScore() {
        return score;
    }
}
