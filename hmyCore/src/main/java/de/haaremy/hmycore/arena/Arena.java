package de.haaremy.hmycore.arena;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class Arena {

    private final String name;
    private String worldName;
    private ArenaState state;
    private final List<Location> spawnPoints;
    private final Set<UUID> players;
    private int minPlayers;
    private int maxPlayers;
    private String gameType;
    private String templateWorldName;
    private UUID winnerUuid;

    public Arena(String name) {
        this.name = name;
        this.state = ArenaState.WAITING;
        this.spawnPoints = new ArrayList<>();
        this.players = new HashSet<>();
        this.minPlayers = 2;
        this.maxPlayers = 16;
        this.gameType = "default";
    }

    public String getName() {
        return name;
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public ArenaState getState() {
        return state;
    }

    public void setState(ArenaState state) {
        this.state = state;
    }

    public List<Location> getSpawnPoints() {
        return Collections.unmodifiableList(spawnPoints);
    }

    public void addSpawnPoint(Location location) {
        spawnPoints.add(location);
    }

    public void clearSpawnPoints() {
        spawnPoints.clear();
    }

    public Set<UUID> getPlayers() {
        return Collections.unmodifiableSet(players);
    }

    public boolean addPlayer(UUID uuid) {
        if (players.size() >= maxPlayers) return false;
        if (state != ArenaState.WAITING) return false;
        return players.add(uuid);
    }

    public boolean removePlayer(UUID uuid) {
        return players.remove(uuid);
    }

    public boolean hasPlayer(UUID uuid) {
        return players.contains(uuid);
    }

    public int getPlayerCount() {
        return players.size();
    }

    public boolean isFull() {
        return players.size() >= maxPlayers;
    }

    public boolean hasEnoughPlayers() {
        return players.size() >= minPlayers;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public void setMinPlayers(int minPlayers) {
        this.minPlayers = minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(String gameType) {
        this.gameType = gameType;
    }

    public String getTemplateWorldName() {
        return templateWorldName;
    }

    public void setTemplateWorldName(String templateWorldName) {
        this.templateWorldName = templateWorldName;
    }

    public Optional<UUID> getWinner() {
        return Optional.ofNullable(winnerUuid);
    }

    public void setWinner(UUID winnerUuid) {
        this.winnerUuid = winnerUuid;
    }

    public void reset() {
        players.clear();
        state = ArenaState.WAITING;
        winnerUuid = null;
    }
}
