package de.haaremy.hmyparkour;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public class ParkourCourse {

    private final String name;
    private ParkourDifficulty difficulty;
    private Location startLocation;
    private Location endLocation;
    private final List<Location> checkpoints;

    public ParkourCourse(String name, ParkourDifficulty difficulty, Location startLocation) {
        this.name = name;
        this.difficulty = difficulty;
        this.startLocation = startLocation;
        this.endLocation = null;
        this.checkpoints = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public ParkourDifficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(ParkourDifficulty difficulty) {
        this.difficulty = difficulty;
    }

    public Location getStartLocation() {
        return startLocation;
    }

    public void setStartLocation(Location startLocation) {
        this.startLocation = startLocation;
    }

    public Location getEndLocation() {
        return endLocation;
    }

    public void setEndLocation(Location endLocation) {
        this.endLocation = endLocation;
    }

    public List<Location> getCheckpoints() {
        return checkpoints;
    }

    public void addCheckpoint(Location location) {
        checkpoints.add(location);
    }

    public int getCoinReward() {
        return difficulty.getCoinReward();
    }

    public int getBonusCoinReward() {
        return (int) (getCoinReward() * 1.5);
    }

    public boolean isComplete() {
        return startLocation != null && endLocation != null;
    }

    public void saveToConfig(ConfigurationSection section) {
        section.set("difficulty", difficulty.name());
        saveLocation(section, "start", startLocation);
        if (endLocation != null) {
            saveLocation(section, "end", endLocation);
        }

        for (int i = 0; i < checkpoints.size(); i++) {
            saveLocation(section, "checkpoints." + i, checkpoints.get(i));
        }
        section.set("checkpoint-count", checkpoints.size());
    }

    public static ParkourCourse loadFromConfig(String name, ConfigurationSection section) {
        if (section == null) return null;

        ParkourDifficulty difficulty;
        try {
            difficulty = ParkourDifficulty.valueOf(section.getString("difficulty", "EASY"));
        } catch (IllegalArgumentException e) {
            difficulty = ParkourDifficulty.EASY;
        }

        Location start = loadLocation(section, "start");
        if (start == null) return null;

        ParkourCourse course = new ParkourCourse(name, difficulty, start);
        course.endLocation = loadLocation(section, "end");

        int checkpointCount = section.getInt("checkpoint-count", 0);
        for (int i = 0; i < checkpointCount; i++) {
            Location cp = loadLocation(section, "checkpoints." + i);
            if (cp != null) {
                course.checkpoints.add(cp);
            }
        }

        return course;
    }

    private static void saveLocation(ConfigurationSection section, String path, Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        section.set(path + ".world", loc.getWorld().getName());
        section.set(path + ".x", loc.getX());
        section.set(path + ".y", loc.getY());
        section.set(path + ".z", loc.getZ());
        section.set(path + ".yaw", (double) loc.getYaw());
        section.set(path + ".pitch", (double) loc.getPitch());
    }

    private static Location loadLocation(ConfigurationSection section, String path) {
        String worldName = section.getString(path + ".world");
        if (worldName == null) return null;

        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;

        return new Location(
                world,
                section.getDouble(path + ".x"),
                section.getDouble(path + ".y"),
                section.getDouble(path + ".z"),
                (float) section.getDouble(path + ".yaw"),
                (float) section.getDouble(path + ".pitch")
        );
    }
}
