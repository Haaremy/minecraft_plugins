package de.haaremy.hmylobby.balloon;

import de.haaremy.hmylobby.HmyLanguageManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BalloonManager implements Listener {

    private final Plugin plugin;
    private final HmyLanguageManager language;
    private final BalloonConfig config;

    private final Map<UUID, BalloonRide> activeBalloons = new HashMap<>();
    private final Map<UUID, BalloonRide> playerToBalloon = new HashMap<>();
    private final Map<String, BalloonRoute> routes = new HashMap<>();

    private final Map<UUID, ElevatorRide> activeElevators = new HashMap<>();
    private final Map<UUID, ElevatorRide> playerToElevator = new HashMap<>();
    private final Map<String, ElevatorRoute> elevatorRoutes = new HashMap<>();

    public BalloonManager(Plugin plugin, HmyLanguageManager language, Path hmySettingsDir) {
        this.plugin = plugin;
        this.language = language;
        this.config = new BalloonConfig(hmySettingsDir, plugin.getLogger());
        config.load(routes, elevatorRoutes);

        Bukkit.getScheduler().runTaskTimer(plugin, this::updateBalloons, 0, 2);
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateElevators, 0, 2);
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkBoardingZones, 0, 1);
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkElevatorBoarding, 0, 1);
    }

    // ====== COMMAND ENTRY POINT ======

    public boolean handleCommand(Player player, String[] args) {
        // args[0] == "ballon"
        if (args.length < 2) {
            sendHelp(player);
            return true;
        }
        if (!player.hasPermission("hmy.lobby.balloon.use") && !player.hasPermission("hmy.lobby.balloon.admin")) {
            player.sendMessage(language.getMessage(player, "no_permission", "§cKeine Berechtigung."));
            return true;
        }
        switch (args[1].toLowerCase()) {
            case "route"    -> handleRouteCommand(player, args);
            case "elevator" -> handleElevatorCommand(player, args);
            default         -> sendHelp(player);
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6=== Heißluftballon ===");
        player.sendMessage("§e/hmy ballon route erstellen <Name> §7- Neue Route");
        player.sendMessage("§e/hmy ballon route waypoint <Name> §7- Waypoint hinzufügen");
        player.sendMessage("§e/hmy ballon route boarding <Name> <Nr> §7- Boarding-Zone");
        player.sendMessage("§e/hmy ballon route dropoff <Name> <Nr> §7- Dropoff-Zone");
        player.sendMessage("§e/hmy ballon route list <Name> §7- Waypoints anzeigen");
        player.sendMessage("§e/hmy ballon route start <Name> <Anzahl> §7- Starten");
        player.sendMessage("§e/hmy ballon route stop <Name> §7- Stoppen");
        player.sendMessage("§e/hmy ballon route info §7- Alle Routen");
        player.sendMessage("§6=== Fahrstuhl ===");
        player.sendMessage("§e/hmy ballon elevator create <Name> §7- Fahrstuhl erstellen");
        player.sendMessage("§e/hmy ballon elevator floor <Name> §7- Etage hinzufügen");
        player.sendMessage("§e/hmy ballon elevator boarding <Name> <Nr> §7- Boarding-Zone");
        player.sendMessage("§e/hmy ballon elevator dropoff <Name> <Nr> §7- Dropoff-Zone");
        player.sendMessage("§e/hmy ballon elevator list <Name> §7- Etagen anzeigen");
        player.sendMessage("§e/hmy ballon elevator start <Name> §7- Starten");
        player.sendMessage("§e/hmy ballon elevator stop <Name> §7- Stoppen");
        player.sendMessage("§e/hmy ballon elevator info §7- Alle Fahrstühle");
    }

    // ====== ROUTE COMMANDS ======

    private void handleRouteCommand(Player player, String[] args) {
        if (!player.hasPermission("hmy.lobby.balloon.admin")) {
            player.sendMessage(language.getMessage(player, "no_permission", "§cKeine Berechtigung."));
            return;
        }
        if (args.length < 3) {
            player.sendMessage("§c✗ /hmy ballon route <erstellen|waypoint|boarding|dropoff|list|start|stop|info> [args]");
            return;
        }
        switch (args[2].toLowerCase()) {
            case "erstellen" -> {
                requireArgs(player, args, 4, "/hmy ballon route erstellen <Name>");
                if (args.length >= 4) createRoute(player, args[3]);
            }
            case "waypoint" -> {
                requireArgs(player, args, 4, "/hmy ballon route waypoint <Name>");
                if (args.length >= 4) addWaypointToRoute(player, args[3]);
            }
            case "boarding" -> {
                requireArgs(player, args, 5, "/hmy ballon route boarding <Name> <Nr>");
                if (args.length >= 5) addBoardingZone(player, args[3], args[4]);
            }
            case "dropoff" -> {
                requireArgs(player, args, 5, "/hmy ballon route dropoff <Name> <Nr>");
                if (args.length >= 5) addDropoffZone(player, args[3], args[4]);
            }
            case "list" -> {
                requireArgs(player, args, 4, "/hmy ballon route list <Name>");
                if (args.length >= 4) listRouteWaypoints(player, args[3]);
            }
            case "start" -> {
                requireArgs(player, args, 5, "/hmy ballon route start <Name> <Anzahl>");
                if (args.length >= 5) startAutoTravel(player, args[3], args[4]);
            }
            case "stop" -> {
                requireArgs(player, args, 4, "/hmy ballon route stop <Name>");
                if (args.length >= 4) stopAutoTravel(player, args[3]);
            }
            case "info" -> listActiveRoutes(player);
            default -> player.sendMessage("§c✗ Unbekannte Aktion: " + args[2]);
        }
    }

    private void createRoute(Player player, String routeName) {
        if (routes.containsKey(routeName)) {
            player.sendMessage("§c✗ Route '§e" + routeName + "§c' existiert bereits!");
            return;
        }
        routes.put(routeName, new BalloonRoute(routeName));
        config.save(routes, elevatorRoutes);
        player.sendMessage("§6✓ Route '§e" + routeName + "§6' erstellt!");
        player.sendMessage("§eNächster Schritt: §7/hmy ballon route waypoint " + routeName);
    }

    private void addWaypointToRoute(Player player, String routeName) {
        BalloonRoute route = routes.get(routeName);
        if (route == null) { notFound(player, "Route", routeName); return; }
        Location loc = player.getLocation().clone();
        route.addWaypoint(loc);
        config.save(routes, elevatorRoutes);
        int index = route.waypoints.size() - 1;
        player.sendMessage("§6✓ Waypoint §e#" + index + " §6bei " + locStr(loc) + " §6hinzugefügt!");
        showWaypointParticles(loc);
    }

    private void addBoardingZone(Player player, String routeName, String waypointStr) {
        BalloonRoute route = routes.get(routeName);
        if (route == null) { notFound(player, "Route", routeName); return; }
        try {
            int idx = Integer.parseInt(waypointStr);
            if (idx < 0 || idx >= route.waypoints.size()) {
                player.sendMessage("§c✗ Waypoint §e#" + idx + "§c existiert nicht!");
                return;
            }
            route.addBoardingZone(idx);
            config.save(routes, elevatorRoutes);
            player.sendMessage("§6✓ Boarding-Zone bei Waypoint §e#" + idx + " §6aktiviert!");
            showBoardingZone(route.waypoints.get(idx));
        } catch (NumberFormatException e) {
            player.sendMessage("§c✗ Bitte gib eine gültige Zahl ein!");
        }
    }

    private void addDropoffZone(Player player, String routeName, String waypointStr) {
        BalloonRoute route = routes.get(routeName);
        if (route == null) { notFound(player, "Route", routeName); return; }
        try {
            int idx = Integer.parseInt(waypointStr);
            if (idx < 0 || idx >= route.waypoints.size()) {
                player.sendMessage("§c✗ Waypoint §e#" + idx + "§c existiert nicht!");
                return;
            }
            route.addDropoffZone(idx);
            config.save(routes, elevatorRoutes);
            player.sendMessage("§6✓ Dropoff-Zone bei Waypoint §e#" + idx + " §6aktiviert!");
            showDropoffZone(route.waypoints.get(idx));
        } catch (NumberFormatException e) {
            player.sendMessage("§c✗ Bitte gib eine gültige Zahl ein!");
        }
    }

    private void listRouteWaypoints(Player player, String routeName) {
        BalloonRoute route = routes.get(routeName);
        if (route == null) { notFound(player, "Route", routeName); return; }
        if (route.waypoints.isEmpty()) { player.sendMessage("§c✗ Route hat keine Waypoints!"); return; }
        player.sendMessage("§6=== Route '§e" + routeName + "§6' ===");
        player.sendMessage("§7Status: " + (route.isActive ? "§a✓ Aktiv (" + route.activeBalloons.size() + " Ballons)" : "§c✗ Inaktiv"));
        for (int i = 0; i < route.waypoints.size(); i++) {
            Location loc = route.waypoints.get(i);
            String b = route.boardingWaypoints.contains(i) ? " §a[Boarding]" : "";
            String d = route.dropoffWaypoints.contains(i) ? " §b[Dropoff]" : "";
            player.sendMessage("  §e#" + i + " §6→ " + locStr(loc) + b + d);
        }
    }

    private void startAutoTravel(Player player, String routeName, String countStr) {
        BalloonRoute route = routes.get(routeName);
        if (route == null) { notFound(player, "Route", routeName); return; }
        if (route.waypoints.size() < 2) { player.sendMessage("§c✗ Route braucht mindestens 2 Waypoints!"); return; }
        if (route.isActive) { player.sendMessage("§c✗ Route läuft bereits! Stoppe sie erst mit /hmy ballon route stop " + routeName); return; }
        try {
            int count = Integer.parseInt(countStr);
            if (count < 1 || count > 20) { player.sendMessage("§c✗ Anzahl muss zwischen 1 und 20 liegen!"); return; }
            route.isActive = true;
            for (int i = 0; i < count; i++) {
                BalloonRide ride = createAutoTravelBalloon(route, i, count);
                activeBalloons.put(ride.minecart.getUniqueId(), ride);
                route.activeBalloons.add(ride);
            }
            player.sendMessage("§6✓ Auto-Travel '§e" + routeName + "§6' mit §e" + count + " §6Ballons gestartet!");
        } catch (NumberFormatException e) {
            player.sendMessage("§c✗ Bitte gib eine gültige Zahl ein!");
        }
    }

    private void stopAutoTravel(Player player, String routeName) {
        BalloonRoute route = routes.get(routeName);
        if (route == null) { notFound(player, "Route", routeName); return; }
        for (BalloonRide ride : new ArrayList<>(route.activeBalloons)) {
            ride.removeStructure();
            if (ride.minecart.isValid()) ride.minecart.remove();
            activeBalloons.remove(ride.minecart.getUniqueId());
        }
        route.activeBalloons.clear();
        route.isActive = false;
        player.sendMessage("§6✓ Route '§e" + routeName + "§6' gestoppt!");
    }

    private void listActiveRoutes(Player player) {
        player.sendMessage("§6=== Ballon-Routen ===");
        if (routes.isEmpty()) { player.sendMessage("§7Keine Routen vorhanden."); return; }
        for (BalloonRoute route : routes.values()) {
            String status = route.isActive ? "§a✓ Aktiv" : "§c✗ Inaktiv";
            player.sendMessage("  §e" + route.name + " §6[" + status + "§6] - "
                    + route.waypoints.size() + " Waypoints, "
                    + route.boardingWaypoints.size() + " Boarding, "
                    + route.dropoffWaypoints.size() + " Dropoff");
        }
    }

    private BalloonRide createAutoTravelBalloon(BalloonRoute route, int index, int total) {
        Location startLoc = route.waypoints.get(0).clone();
        Minecart minecart = (Minecart) startLoc.getWorld().spawnEntity(startLoc, EntityType.MINECART);
        minecart.setGravity(false);
        minecart.setInvulnerable(true);
        minecart.setMaxSpeed(0);
        BalloonRide ride = new BalloonRide(minecart, route.name, null, true);
        ride.route = route;
        int offsetWaypoint = (index * route.waypoints.size()) / total;
        ride.currentWaypointIndex = offsetWaypoint;
        ride.nextWaypointIndex = (offsetWaypoint + 1) % route.waypoints.size();
        ride.setDestination(route.waypoints.get(ride.nextWaypointIndex), ride.nextWaypointIndex);
        return ride;
    }

    // ====== ELEVATOR COMMANDS ======

    private void handleElevatorCommand(Player player, String[] args) {
        if (!player.hasPermission("hmy.lobby.balloon.admin")) {
            player.sendMessage(language.getMessage(player, "no_permission", "§cKeine Berechtigung."));
            return;
        }
        if (args.length < 3) {
            player.sendMessage("§c✗ /hmy ballon elevator <create|floor|boarding|dropoff|list|start|stop|info> [args]");
            return;
        }
        switch (args[2].toLowerCase()) {
            case "create" -> {
                requireArgs(player, args, 4, "/hmy ballon elevator create <Name>");
                if (args.length >= 4) createElevator(player, args[3]);
            }
            case "floor" -> {
                requireArgs(player, args, 4, "/hmy ballon elevator floor <Name>");
                if (args.length >= 4) addFloor(player, args[3]);
            }
            case "boarding" -> {
                requireArgs(player, args, 5, "/hmy ballon elevator boarding <Name> <Nr>");
                if (args.length >= 5) addElevatorBoarding(player, args[3], args[4]);
            }
            case "dropoff" -> {
                requireArgs(player, args, 5, "/hmy ballon elevator dropoff <Name> <Nr>");
                if (args.length >= 5) addElevatorDropoff(player, args[3], args[4]);
            }
            case "list" -> {
                requireArgs(player, args, 4, "/hmy ballon elevator list <Name>");
                if (args.length >= 4) listElevatorFloors(player, args[3]);
            }
            case "start" -> {
                requireArgs(player, args, 4, "/hmy ballon elevator start <Name>");
                if (args.length >= 4) startElevator(player, args[3]);
            }
            case "stop" -> {
                requireArgs(player, args, 4, "/hmy ballon elevator stop <Name>");
                if (args.length >= 4) stopElevator(player, args[3]);
            }
            case "info" -> listElevators(player);
            default -> player.sendMessage("§c✗ Unbekannte Aktion: " + args[2]);
        }
    }

    private void createElevator(Player player, String name) {
        if (elevatorRoutes.containsKey(name)) { player.sendMessage("§c✗ Fahrstuhl '§e" + name + "§c' existiert bereits!"); return; }
        elevatorRoutes.put(name, new ElevatorRoute(name));
        config.save(routes, elevatorRoutes);
        player.sendMessage("§6✓ Fahrstuhl '§e" + name + "§6' erstellt!");
        player.sendMessage("§eNächster Schritt: §7/hmy ballon elevator floor " + name);
    }

    private void addFloor(Player player, String name) {
        ElevatorRoute route = elevatorRoutes.get(name);
        if (route == null) { notFound(player, "Fahrstuhl", name); return; }
        Location loc = player.getLocation().clone();
        route.addFloor(loc);
        config.save(routes, elevatorRoutes);
        player.sendMessage("§6✓ Etage §e#" + (route.floors.size() - 1) + " §6bei Y=" + loc.getBlockY() + " §6hinzugefügt!");
        showBoardingZone(loc);
    }

    private void addElevatorBoarding(Player player, String name, String floorStr) {
        ElevatorRoute route = elevatorRoutes.get(name);
        if (route == null) { notFound(player, "Fahrstuhl", name); return; }
        try {
            int idx = Integer.parseInt(floorStr);
            if (idx < 0 || idx >= route.floors.size()) { player.sendMessage("§c✗ Etage §e#" + idx + "§c existiert nicht!"); return; }
            route.addBoardingFloor(idx);
            config.save(routes, elevatorRoutes);
            player.sendMessage("§6✓ Boarding bei Etage §e#" + idx + " §6aktiviert!");
        } catch (NumberFormatException e) {
            player.sendMessage("§c✗ Bitte gib eine gültige Zahl ein!");
        }
    }

    private void addElevatorDropoff(Player player, String name, String floorStr) {
        ElevatorRoute route = elevatorRoutes.get(name);
        if (route == null) { notFound(player, "Fahrstuhl", name); return; }
        try {
            int idx = Integer.parseInt(floorStr);
            if (idx < 0 || idx >= route.floors.size()) { player.sendMessage("§c✗ Etage §e#" + idx + "§c existiert nicht!"); return; }
            route.addDropoffFloor(idx);
            config.save(routes, elevatorRoutes);
            player.sendMessage("§6✓ Dropoff bei Etage §e#" + idx + " §6aktiviert!");
        } catch (NumberFormatException e) {
            player.sendMessage("§c✗ Bitte gib eine gültige Zahl ein!");
        }
    }

    private void listElevatorFloors(Player player, String name) {
        ElevatorRoute route = elevatorRoutes.get(name);
        if (route == null) { notFound(player, "Fahrstuhl", name); return; }
        if (route.floors.isEmpty()) { player.sendMessage("§c✗ Fahrstuhl hat keine Etagen!"); return; }
        player.sendMessage("§6=== Fahrstuhl '§e" + name + "§6' ===");
        player.sendMessage("§7Status: " + (route.isActive ? "§a✓ Aktiv" : "§c✗ Inaktiv"));
        for (int i = 0; i < route.floors.size(); i++) {
            Location loc = route.floors.get(i);
            String b = route.boardingFloors.contains(i) ? " §a[Boarding]" : "";
            String d = route.dropoffFloors.contains(i) ? " §b[Dropoff]" : "";
            player.sendMessage("  §e#" + i + " §6→ Y=" + loc.getBlockY() + b + d);
        }
    }

    private void startElevator(Player player, String name) {
        ElevatorRoute route = elevatorRoutes.get(name);
        if (route == null) { notFound(player, "Fahrstuhl", name); return; }
        if (route.floors.size() < 2) { player.sendMessage("§c✗ Fahrstuhl braucht mindestens 2 Etagen!"); return; }
        if (route.isActive) { player.sendMessage("§c✗ Fahrstuhl läuft bereits!"); return; }
        route.isActive = true;
        Location startLoc = route.floors.get(0).clone();
        Minecart minecart = (Minecart) startLoc.getWorld().spawnEntity(startLoc, EntityType.MINECART);
        minecart.setGravity(false);
        minecart.setInvulnerable(true);
        minecart.setMaxSpeed(0);
        ElevatorRide ride = new ElevatorRide(minecart, route);
        ride.setNextFloor(1);
        activeElevators.put(minecart.getUniqueId(), ride);
        route.activeElevators.add(ride);
        player.sendMessage("§6✓ Fahrstuhl '§e" + name + "§6' gestartet! (" + route.floors.size() + " Etagen)");
        for (Location floor : route.floors) showBoardingZone(floor);
    }

    private void stopElevator(Player player, String name) {
        ElevatorRoute route = elevatorRoutes.get(name);
        if (route == null) { notFound(player, "Fahrstuhl", name); return; }
        for (ElevatorRide ride : new ArrayList<>(route.activeElevators)) {
            ride.removeStructure();
            if (ride.minecart.isValid()) ride.minecart.remove();
            activeElevators.remove(ride.minecart.getUniqueId());
        }
        route.activeElevators.clear();
        route.isActive = false;
        player.sendMessage("§6✓ Fahrstuhl '§e" + name + "§6' gestoppt!");
    }

    private void listElevators(Player player) {
        player.sendMessage("§6=== Fahrstühle ===");
        if (elevatorRoutes.isEmpty()) { player.sendMessage("§7Keine Fahrstühle vorhanden."); return; }
        for (ElevatorRoute route : elevatorRoutes.values()) {
            String status = route.isActive ? "§a✓ Aktiv" : "§c✗ Inaktiv";
            player.sendMessage("  §e" + route.name + " §6[" + status + "§6] - " + route.floors.size() + " Etagen");
        }
    }

    // ====== BALLOON UPDATE LOOP ======

    private void updateBalloons() {
        for (BalloonRide ride : new ArrayList<>(activeBalloons.values())) {
            if (!ride.minecart.isValid()) {
                ride.removeStructure();
                activeBalloons.remove(ride.minecart.getUniqueId());
                if (ride.route != null) ride.route.activeBalloons.remove(ride);
                continue;
            }
            if (!ride.isMoving) {
                // Count down landing pause
                if (ride.isAutoTravel && ride.route != null && ride.landingTicks > 0) {
                    ride.landingTicks--;
                    if (ride.landingTicks == 0) {
                        ride.currentWaypointIndex = ride.nextWaypointIndex;
                        ride.nextWaypointIndex = (ride.currentWaypointIndex + 1) % ride.route.waypoints.size();
                        ride.setDestination(ride.route.waypoints.get(ride.nextWaypointIndex), ride.nextWaypointIndex);
                        ride.minecart.getWorld().playSound(ride.minecart.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 0.5f, 1.2f);
                    }
                }
                continue;
            }

            Location current = ride.minecart.getLocation();
            Location target = ride.destination;
            double distance = current.distance(target);
            double speed = 0.3;

            if (distance > speed) {
                Vector direction = target.toVector().subtract(current.toVector()).normalize();
                ride.minecart.setVelocity(direction.multiply(speed));
                ride.updateStructure(current, direction);
                current.getWorld().spawnParticle(Particle.CLOUD, current.clone().add(0, 1, 0), 2, 0.3, 0.3, 0.3, 0.02);
                if (ride.ticksMoving % 40 == 0) {
                    current.getWorld().playSound(current, Sound.AMBIENT_CAVE, 0.2f, 1.5f);
                }
                ride.ticksMoving++;
                for (Player p : ride.getPassengers()) {
                    if (p.isOnline()) p.teleport(current.clone().add(0, 1.5, 0));
                }
            } else {
                // Arrived
                ride.minecart.setVelocity(new Vector(0, 0, 0));
                ride.minecart.teleport(target);
                ride.updateStructure(target, new Vector(0, 0, 0));
                target.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, target.clone().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.05);
                target.getWorld().playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.0f);
                for (Player p : ride.getPassengers()) {
                    p.sendMessage(language.getMessage(p, "balloon_arrived",
                            "§6🎈 Gelandet bei Waypoint §e#" + ride.nextWaypointIndex));
                }
                if (ride.isAutoTravel && ride.route != null) {
                    ride.isMoving = false;
                    ride.landingTicks = 40;
                }
            }
        }
    }

    // ====== ELEVATOR UPDATE LOOP ======

    private void updateElevators() {
        for (ElevatorRide ride : new ArrayList<>(activeElevators.values())) {
            if (!ride.minecart.isValid()) {
                ride.removeStructure();
                activeElevators.remove(ride.minecart.getUniqueId());
                if (ride.route != null) ride.route.activeElevators.remove(ride);
                continue;
            }
            if (!ride.isMoving) continue;

            Location current = ride.minecart.getLocation();
            Location target = ride.destination;
            double dy = target.getY() - current.getY();
            double speed = 0.15;

            if (Math.abs(dy) > speed) {
                Vector vel = new Vector(0, Math.signum(dy) * speed, 0);
                ride.minecart.setVelocity(vel);
                ride.structure.updatePosition(current.clone().add(vel));
                for (Player p : ride.getPassengers()) {
                    if (p.isOnline()) p.teleport(current.clone().add(0, 1.5, 0));
                }
                ride.ticksMoving++;
            } else {
                // Arrived at floor
                ride.minecart.setVelocity(new Vector(0, 0, 0));
                ride.minecart.teleport(target);
                ride.structure.updatePosition(target);
                target.getWorld().playSound(target, Sound.BLOCK_IRON_DOOR_OPEN, 0.5f, 1.0f);
                for (Player p : ride.getPassengers()) {
                    p.sendMessage(language.getMessage(p, "elevator_arrived",
                            "§6🏢 Etage §e#" + ride.nextFloor + " §6erreicht!"));
                }
                ride.currentFloor = ride.nextFloor;
                ride.isMoving = false;

                // Determine next floor (ping-pong)
                int nextFloor;
                if (ride.route.ascending) {
                    nextFloor = ride.currentFloor + 1;
                    if (nextFloor >= ride.route.floors.size()) {
                        ride.route.ascending = false;
                        nextFloor = ride.currentFloor - 1;
                    }
                } else {
                    nextFloor = ride.currentFloor - 1;
                    if (nextFloor < 0) {
                        ride.route.ascending = true;
                        nextFloor = ride.currentFloor + 1;
                    }
                }
                final int finalNextFloor = nextFloor;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (ride.minecart.isValid()) ride.setNextFloor(finalNextFloor);
                }, 60L);
            }
        }
    }

    // ====== BOARDING / DROPOFF ZONE CHECKS ======

    private void checkBoardingZones() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (playerToBalloon.containsKey(player.getUniqueId())) {
                BalloonRide ride = playerToBalloon.get(player.getUniqueId());
                if (ride.route != null) {
                    Integer dropoffIdx = ride.route.getNearestDropoffZoneIndex(player.getLocation());
                    if (dropoffIdx != null && !ride.isMoving) {
                        exitBalloon(player, ride.route.waypoints.get(dropoffIdx));
                    }
                }
                continue;
            }
            for (BalloonRoute route : routes.values()) {
                if (!route.isActive || route.activeBalloons.isEmpty()) continue;
                Integer boardingIdx = route.getNearestBoardingZoneIndex(player.getLocation());
                if (boardingIdx == null) continue;
                BalloonRide nearest = null;
                double nearestDist = Double.MAX_VALUE;
                for (BalloonRide ride : route.activeBalloons) {
                    double dist = player.getLocation().distance(ride.minecart.getLocation());
                    if (dist < nearestDist && dist < 30) { nearestDist = dist; nearest = ride; }
                }
                if (nearest != null) { boardBalloon(player, nearest); break; }
            }
        }
    }

    private void checkElevatorBoarding() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (playerToElevator.containsKey(player.getUniqueId())) {
                ElevatorRide ride = playerToElevator.get(player.getUniqueId());
                Integer dropoffIdx = ride.route.getNearestDropoffFloorIndex(player.getLocation());
                if (dropoffIdx != null && !ride.isMoving) {
                    exitElevator(player, ride.route.floors.get(dropoffIdx));
                }
                continue;
            }
            for (ElevatorRoute route : elevatorRoutes.values()) {
                if (!route.isActive || route.activeElevators.isEmpty()) continue;
                Integer boardingIdx = route.getNearestBoardingFloorIndex(player.getLocation());
                if (boardingIdx == null) continue;
                ElevatorRide nearest = null;
                double nearestDist = Double.MAX_VALUE;
                for (ElevatorRide ride : route.activeElevators) {
                    double dist = player.getLocation().distance(ride.minecart.getLocation());
                    if (dist < nearestDist && dist < 20) { nearestDist = dist; nearest = ride; }
                }
                if (nearest != null) { boardElevator(player, nearest); break; }
            }
        }
    }

    private void boardBalloon(Player player, BalloonRide ride) {
        ride.addPassenger(player);
        playerToBalloon.put(player.getUniqueId(), ride);
        player.sendMessage(language.getMessage(player, "balloon_boarded", "§6✨ Du bist auf dem Ballon aufgestiegen!"));
        player.playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 0.5f, 1.2f);
        player.sendActionBar(Component.text("§e🎈 Willkommen auf deiner Reise!"));
    }

    private void exitBalloon(Player player, Location dropoffZone) {
        BalloonRide ride = playerToBalloon.remove(player.getUniqueId());
        if (ride == null) return;
        ride.removePassenger(player);
        player.teleport(dropoffZone.clone().add(0, 1, 0));
        player.sendMessage(language.getMessage(player, "balloon_exited", "§6🎈 Du bist ausgestiegen! Bis zur nächsten Reise!"));
        player.playSound(player.getLocation(), Sound.ENTITY_BAT_AMBIENT, 0.5f, 0.8f);
    }

    private void boardElevator(Player player, ElevatorRide ride) {
        ride.addPassenger(player);
        playerToElevator.put(player.getUniqueId(), ride);
        player.sendMessage(language.getMessage(player, "elevator_boarded", "§6✨ Du bist in den Fahrstuhl eingestiegen!"));
        player.playSound(player.getLocation(), Sound.BLOCK_IRON_DOOR_OPEN, 0.5f, 1.0f);
        player.sendActionBar(Component.text("§e🏢 Fahrstuhl bestiegen!"));
    }

    private void exitElevator(Player player, Location dropoffFloor) {
        ElevatorRide ride = playerToElevator.remove(player.getUniqueId());
        if (ride == null) return;
        ride.removePassenger(player);
        player.teleport(dropoffFloor.clone().add(0, 1, 0));
        player.sendMessage(language.getMessage(player, "elevator_exited", "§6🏢 Du bist ausgestiegen!"));
        player.playSound(player.getLocation(), Sound.BLOCK_IRON_DOOR_CLOSE, 0.5f, 1.0f);
    }

    // ====== PARTICLE HELPERS ======

    private void showWaypointParticles(Location loc) {
        for (int i = 0; i < 10; i++) {
            loc.getWorld().spawnParticle(Particle.VILLAGER_HAPPY,
                    loc.clone().add(0, i * 0.5, 0), 5, 0.3, 0.3, 0.3, 0);
        }
    }

    private void showBoardingZone(Location loc) {
        World world = loc.getWorld();
        for (int i = 0; i < 20; i++) {
            double angle = (2 * Math.PI * i) / 20;
            world.spawnParticle(Particle.VILLAGER_HAPPY,
                    new Location(world,
                            loc.getX() + BalloonRoute.ZONE_RADIUS * Math.cos(angle),
                            loc.getY() + 1,
                            loc.getZ() + BalloonRoute.ZONE_RADIUS * Math.sin(angle)),
                    2, 0.1, 0.1, 0.1, 0);
        }
    }

    private void showDropoffZone(Location loc) {
        World world = loc.getWorld();
        for (int i = 0; i < 20; i++) {
            double angle = (2 * Math.PI * i) / 20;
            world.spawnParticle(Particle.SPELL_MOB,
                    new Location(world,
                            loc.getX() + BalloonRoute.ZONE_RADIUS * Math.cos(angle),
                            loc.getY() + 1,
                            loc.getZ() + BalloonRoute.ZONE_RADIUS * Math.sin(angle)),
                    2, 0.1, 0.1, 0.1, 0);
        }
    }

    // ====== EVENTS ======

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        BalloonRide balloonRide = playerToBalloon.remove(uuid);
        if (balloonRide != null) balloonRide.removePassenger(event.getPlayer());
        ElevatorRide elevRide = playerToElevator.remove(uuid);
        if (elevRide != null) elevRide.removePassenger(event.getPlayer());
    }

    public void onDisable() {
        for (BalloonRide ride : activeBalloons.values()) {
            ride.removeStructure();
            if (ride.minecart.isValid()) ride.minecart.remove();
        }
        for (ElevatorRide ride : activeElevators.values()) {
            ride.removeStructure();
            if (ride.minecart.isValid()) ride.minecart.remove();
        }
        activeBalloons.clear();
        activeElevators.clear();
    }

    // ====== HELPERS ======

    private void requireArgs(Player player, String[] args, int min, String usage) {
        if (args.length < min) player.sendMessage("§c✗ Verwendung: " + usage);
    }

    private void notFound(Player player, String type, String name) {
        player.sendMessage("§c✗ " + type + " '§e" + name + "§c' nicht gefunden!");
    }

    private String locStr(Location loc) {
        return loc.getBlockX() + "/" + loc.getBlockY() + "/" + loc.getBlockZ();
    }
}
