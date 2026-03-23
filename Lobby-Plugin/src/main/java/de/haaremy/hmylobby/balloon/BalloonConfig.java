package de.haaremy.hmylobby.balloon;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Logger;

public class BalloonConfig {

    private final File configFile;
    private final Logger logger;

    public BalloonConfig(Path hmySettingsDir, Logger logger) {
        this.configFile = hmySettingsDir.resolve("balloons.yml").toFile();
        this.logger = logger;
    }

    public void save(Map<String, BalloonRoute> routes, Map<String, ElevatorRoute> elevatorRoutes) {
        YamlConfiguration config = new YamlConfiguration();

        for (Map.Entry<String, BalloonRoute> entry : routes.entrySet()) {
            BalloonRoute route = entry.getValue();
            String base = "routes." + entry.getKey();
            config.set(base + ".boarding", route.boardingWaypoints);
            config.set(base + ".dropoff", route.dropoffWaypoints);
            for (int i = 0; i < route.waypoints.size(); i++) {
                Location loc = route.waypoints.get(i);
                String wp = base + ".waypoints." + i;
                config.set(wp + ".world", loc.getWorld().getName());
                config.set(wp + ".x", loc.getX());
                config.set(wp + ".y", loc.getY());
                config.set(wp + ".z", loc.getZ());
            }
        }

        for (Map.Entry<String, ElevatorRoute> entry : elevatorRoutes.entrySet()) {
            ElevatorRoute route = entry.getValue();
            String base = "elevators." + entry.getKey();
            config.set(base + ".boarding", route.boardingFloors);
            config.set(base + ".dropoff", route.dropoffFloors);
            for (int i = 0; i < route.floors.size(); i++) {
                Location loc = route.floors.get(i);
                String fp = base + ".floors." + i;
                config.set(fp + ".world", loc.getWorld().getName());
                config.set(fp + ".x", loc.getX());
                config.set(fp + ".y", loc.getY());
                config.set(fp + ".z", loc.getZ());
            }
        }

        try {
            config.save(configFile);
        } catch (IOException e) {
            logger.warning("Haaremy: Konnte balloons.yml nicht speichern: " + e.getMessage());
        }
    }

    public void load(Map<String, BalloonRoute> routes, Map<String, ElevatorRoute> elevatorRoutes) {
        if (!configFile.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        ConfigurationSection routeSection = config.getConfigurationSection("routes");
        if (routeSection != null) {
            for (String routeName : routeSection.getKeys(false)) {
                BalloonRoute route = new BalloonRoute(routeName);
                ConfigurationSection wps = routeSection.getConfigurationSection(routeName + ".waypoints");
                if (wps != null) {
                    int i = 0;
                    while (wps.contains(String.valueOf(i))) {
                        String k = String.valueOf(i);
                        World world = Bukkit.getWorld(wps.getString(k + ".world", ""));
                        if (world != null) {
                            route.addWaypoint(new Location(world,
                                    wps.getDouble(k + ".x"),
                                    wps.getDouble(k + ".y"),
                                    wps.getDouble(k + ".z")));
                        }
                        i++;
                    }
                }
                for (int idx : routeSection.getIntegerList(routeName + ".boarding")) {
                    route.addBoardingZone(idx);
                }
                for (int idx : routeSection.getIntegerList(routeName + ".dropoff")) {
                    route.addDropoffZone(idx);
                }
                routes.put(routeName, route);
            }
        }

        ConfigurationSection elevSection = config.getConfigurationSection("elevators");
        if (elevSection != null) {
            for (String elevName : elevSection.getKeys(false)) {
                ElevatorRoute route = new ElevatorRoute(elevName);
                ConfigurationSection fls = elevSection.getConfigurationSection(elevName + ".floors");
                if (fls != null) {
                    int i = 0;
                    while (fls.contains(String.valueOf(i))) {
                        String k = String.valueOf(i);
                        World world = Bukkit.getWorld(fls.getString(k + ".world", ""));
                        if (world != null) {
                            route.addFloor(new Location(world,
                                    fls.getDouble(k + ".x"),
                                    fls.getDouble(k + ".y"),
                                    fls.getDouble(k + ".z")));
                        }
                        i++;
                    }
                }
                for (int idx : elevSection.getIntegerList(elevName + ".boarding")) {
                    route.addBoardingFloor(idx);
                }
                for (int idx : elevSection.getIntegerList(elevName + ".dropoff")) {
                    route.addDropoffFloor(idx);
                }
                elevatorRoutes.put(elevName, route);
            }
        }

        logger.info("Haaremy: Balloon-Konfiguration geladen. Routen: " + routes.size()
                + ", Fahrstühle: " + elevatorRoutes.size());
    }
}
