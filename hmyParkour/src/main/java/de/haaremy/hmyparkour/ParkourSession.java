package de.haaremy.hmyparkour;

import org.bukkit.Location;

import java.util.UUID;

public class ParkourSession {

    private final UUID playerUuid;
    private final String courseName;
    private final long startTime;
    private int currentCheckpoint;
    private Location lastCheckpointLocation;
    private boolean active;

    public ParkourSession(UUID playerUuid, String courseName, Location startLocation) {
        this.playerUuid = playerUuid;
        this.courseName = courseName;
        this.startTime = System.currentTimeMillis();
        this.currentCheckpoint = 0;
        this.lastCheckpointLocation = startLocation;
        this.active = true;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getCourseName() {
        return courseName;
    }

    public long getStartTime() {
        return startTime;
    }

    public int getCurrentCheckpoint() {
        return currentCheckpoint;
    }

    public void setCurrentCheckpoint(int currentCheckpoint) {
        this.currentCheckpoint = currentCheckpoint;
    }

    public Location getLastCheckpointLocation() {
        return lastCheckpointLocation;
    }

    public void setLastCheckpointLocation(Location lastCheckpointLocation) {
        this.lastCheckpointLocation = lastCheckpointLocation;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public long getElapsedTimeMs() {
        return System.currentTimeMillis() - startTime;
    }

    public String getElapsedTimeFormatted() {
        return formatTime(getElapsedTimeMs());
    }

    public static String formatTime(long timeMs) {
        long totalSeconds = timeMs / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long millis = timeMs % 1000;
        return String.format("%02d:%02d.%03d", minutes, seconds, millis);
    }
}
