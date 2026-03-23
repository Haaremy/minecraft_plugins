package com.example.hotairballon;

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
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HotAirBallon extends JavaPlugin implements Listener {

    private Map<UUID, BalloonRide> activeBalloons = new HashMap<>();
    private Map<UUID, BalloonRide> playerToBalloon = new HashMap<>();
    private Map<String, BalloonRoute> routes = new HashMap<>();
    
    private Map<UUID, ElevatorRide> activeElevators = new HashMap<>();
    private Map<UUID, ElevatorRide> playerToElevator = new HashMap<>();
    private Map<String, ElevatorRoute> elevatorRoutes = new HashMap<>();
    
    private Map<UUID, ElevatorRide> activeElevators = new HashMap<>();
    private Map<UUID, ElevatorRide> playerToElevator = new HashMap<>();
    private Map<String, ElevatorRoute> elevatorRoutes = new HashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        
        getCommand("ballon").setExecutor((sender, cmd, label, args) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cNur Spieler können diesen Befehl nutzen!");
                return true;
            }
            
            if (args.length == 0) {
                player.sendMessage("§6=== Heißluftballon Auto-Travel ===");
                player.sendMessage("§e/ballon route erstellen [Name] §7- Neue Route erstellen");
                player.sendMessage("§e/ballon route waypoint [Name] §7- Waypoint zur Route hinzufügen");
                player.sendMessage("§e/ballon route boarding [Name] [Nr] §7- Boarding-Zone aktivieren");
                player.sendMessage("§e/ballon route dropoff [Name] [Nr] §7- Dropoff-Zone aktivieren");
                player.sendMessage("§e/ballon route list [Name] §7- Alle Waypoints anzeigen");
                player.sendMessage("§e/ballon route start [Name] [Anzahl] §7- Auto-Travel starten");
                player.sendMessage("§e/ballon route stop [Name] §7- Auto-Travel stoppen");
                player.sendMessage("§e/ballon route info §7- Aktive Routen anzeigen");
                player.sendMessage("§c");
                player.sendMessage("§6=== Fahrstuhl ===");
                player.sendMessage("§e/ballon elevator create [Name] §7- Fahrstuhl erstellen");
                player.sendMessage("§e/ballon elevator floor [Name] §7- Etage hinzufügen");
                player.sendMessage("§e/ballon elevator list [Name] §7- Etagen anzeigen");
                player.sendMessage("§e/ballon elevator start [Name] §7- Fahrstuhl starten");
                player.sendMessage("§e/ballon elevator stop [Name] §7- Fahrstuhl stoppen");
                return true;
            }
            
            String subcommand = args[0].toLowerCase();
            
            if (subcommand.equals("route")) {
                if (args.length < 2) {
                    player.sendMessage("§c✗ Benutzung: /ballon route <erstellen|waypoint|list|start|stop|info> [args]");
                    return true;
                }
                String action = args[1].toLowerCase();
                
                if (action.equals("erstellen") && args.length >= 3) {
                    createRoute(player, args[2]);
                } else if (action.equals("waypoint") && args.length >= 3) {
                    addWaypointToRoute(player, args[2]);
                } else if (action.equals("list") && args.length >= 3) {
                    listRouteWaypoints(player, args[2]);
                } else if (action.equals("start") && args.length >= 4) {
                    startAutoTravel(player, args[2], args[3]);
                } else if (action.equals("stop") && args.length >= 3) {
                    stopAutoTravel(player, args[2]);
                } else if (action.equals("boarding") && args.length >= 4) {
                    addBoardingZone(player, args[2], args[3]);
                } else if (action.equals("dropoff") && args.length >= 4) {
                    addDropoffZone(player, args[2], args[3]);
                } else if (action.equals("info")) {
                    listActiveRoutes(player);
                }
            } else if (subcommand.equals("elevator")) {
                if (args.length < 2) {
                    player.sendMessage("§c✗ Benutzung: /ballon elevator <create|floor|list|start|stop> [args]");
                    return true;
                }
                String action = args[1].toLowerCase();
                
                if (action.equals("create") && args.length >= 3) {
                    createElevator(player, args[2]);
                } else if (action.equals("floor") && args.length >= 3) {
                    addFloorToElevator(player, args[2]);
                } else if (action.equals("list") && args.length >= 3) {
                    listElevatorFloors(player, args[2]);
                } else if (action.equals("start") && args.length >= 3) {
                    startElevator(player, args[2]);
                } else if (action.equals("stop") && args.length >= 3) {
                    stopElevator(player, args[2]);
                }
            }

            
            return true;
        });
        
        // Alle 2 Ticks Ballons bewegen (40 Ticks pro Sekunde)
        Bukkit.getScheduler().runTaskTimer(this, this::updateBalloons, 0, 2);
        
        // Alle 2 Ticks Aufzüge bewegen
        Bukkit.getScheduler().runTaskTimer(this, this::updateElevators, 0, 2);
        
        // Jeden Tick Boarding/Dropoff Zonen checken
        Bukkit.getScheduler().runTaskTimer(this, this::checkBoardingZones, 0, 1);
        
        getLogger().info("§a✓ HotAirBallon Auto-Travel Plugin aktiviert! 🎈");
    }

    private void createRoute(Player player, String routeName) {
        if (routes.containsKey(routeName)) {
            player.sendMessage("§c✗ Route '§e" + routeName + "§c' existiert bereits!");
            return;
        }
        
        BalloonRoute route = new BalloonRoute(routeName);
        routes.put(routeName, route);
        player.sendMessage("§6✓ Route '§e" + routeName + "§6' erstellt!");
        player.sendMessage("§eVerwendet: §7/ballon route waypoint " + routeName);
    }

    private void addWaypointToRoute(Player player, String routeName) {
        if (!routes.containsKey(routeName)) {
            player.sendMessage("§c✗ Route '§e" + routeName + "§c' existiert nicht!");
            return;
        }
        
        Location waypointLoc = player.getLocation().clone();
        BalloonRoute route = routes.get(routeName);
        route.addWaypoint(waypointLoc);
        
        int index = route.waypoints.size() - 1;
        player.sendMessage("§6✓ Waypoint §e#" + index + " §6hinzugefügt!");
        player.sendMessage("   §eX: " + waypointLoc.getBlockX() + " Y: " + waypointLoc.getBlockY() + " Z: " + waypointLoc.getBlockZ());
        
        showWaypoint(waypointLoc, player.getWorld());
    }

    private void listRouteWaypoints(Player player, String routeName) {
        if (!routes.containsKey(routeName)) {
            player.sendMessage("§c✗ Route '§e" + routeName + "§c' existiert nicht!");
            return;
        }
        
        BalloonRoute route = routes.get(routeName);
        if (route.waypoints.isEmpty()) {
            player.sendMessage("§c✗ Route '§e" + routeName + "§c' hat keine Waypoints!");
            return;
        }
        
        player.sendMessage("§6=== Route '§e" + routeName + "§6' ===");
        player.sendMessage("§7Status: " + (route.isActive ? "§a✓ Aktiv (lauft)" : "§c✗ Inaktiv"));
        if (route.isActive) {
            player.sendMessage("§7Ballons in Fahrt: §e" + route.activeBalloons.size());
        }
        player.sendMessage("§6Waypoints:");
        for (int i = 0; i < route.waypoints.size(); i++) {
            Location loc = route.waypoints.get(i);
            player.sendMessage("  §e#" + i + " §6→ X: " + loc.getBlockX() + " Y: " + loc.getBlockY() + " Z: " + loc.getBlockZ());
        }
    }

    private void startAutoTravel(Player player, String routeName, String balloonCountStr) {
        if (!routes.containsKey(routeName)) {
            player.sendMessage("§c✗ Route '§e" + routeName + "§c' existiert nicht!");
            return;
        }
        
        BalloonRoute route = routes.get(routeName);
        if (route.waypoints.isEmpty()) {
            player.sendMessage("§c✗ Route muss mindestens 2 Waypoints haben!");
            return;
        }
        
        if (route.waypoints.size() < 2) {
            player.sendMessage("§c✗ Route braucht mindestens 2 Waypoints!");
            return;
        }
        
        try {
            int balloonCount = Integer.parseInt(balloonCountStr);
            
            if (balloonCount < 1 || balloonCount > 20) {
                player.sendMessage("§c✗ Anzahl der Ballons muss zwischen 1 und 20 sein!");
                return;
            }
            
            // Starte Auto-Travel
            route.isActive = true;
            
            // Erstelle Ballons mit Offset
            for (int i = 0; i < balloonCount; i++) {
                BalloonRide ride = createAutoTravelBalloon(route, i, balloonCount);
                activeBalloons.put(ride.minecart.getUniqueId(), ride);
                route.activeBalloons.add(ride);
            }
            
            player.sendMessage("§6✓ Auto-Travel für Route '§e" + routeName + "§6' gestartet!");
            player.sendMessage("   §e" + balloonCount + " Ballons fliegen konstant die Route!");
            player.sendMessage("   §7Boarding/Dropoff Zonen aktiv (5 Blöcke Radius)");
            
            // Zeige Boarding- und Dropoff-Zonen mit Partikeln
            for (Integer index : route.boardingWaypoints) {
                showBoardingZone(route.waypoints.get(index));
            }
            for (Integer index : route.dropoffWaypoints) {
                showDropoffZone(route.waypoints.get(index));
            }
            
        } catch (NumberFormatException e) {
            player.sendMessage("§c✗ Bitte gib eine Zahl ein!");
        }
    }
    
    private void showBoardingZone(Location loc) {
        // Zeige die Boarding-Zone mit grünen Partikeln (Circle)
        World world = loc.getWorld();
        double radius = BalloonRoute.ZONE_RADIUS;
        
        for (int i = 0; i < 20; i++) {
            double angle = (2 * Math.PI * i) / 20;
            double x = loc.getX() + radius * Math.cos(angle);
            double z = loc.getZ() + radius * Math.sin(angle);
            
            world.spawnParticle(
                    Particle.HAPPY_VILLAGER,
                    new Location(world, x, loc.getY() + 1, z),
                    2, 0.1, 0.1, 0.1, 0
            );
        }
    }
    
    private void showDropoffZone(Location loc) {
        // Zeige die Dropoff-Zone mit blauen Partikeln (Circle)
        World world = loc.getWorld();
        double radius = BalloonRoute.ZONE_RADIUS;
        
        for (int i = 0; i < 20; i++) {
            double angle = (2 * Math.PI * i) / 20;
            double x = loc.getX() + radius * Math.cos(angle);
            double z = loc.getZ() + radius * Math.sin(angle);
            
            world.spawnParticle(
                    Particle.ENTITY_EFFECT,
                    new Location(world, x, loc.getY() + 1, z),
                    2, 0.1, 0.1, 0.1, 0
            );
        }
    }

    private BalloonRide createAutoTravelBalloon(BalloonRoute route, int balloonIndex, int totalBalloons) {
        Location startLoc = route.waypoints.get(0).clone();
        Minecart minecart = (Minecart) startLoc.getWorld().spawnEntity(startLoc, EntityType.MINECART);
        minecart.setGravity(false);
        minecart.setInvulnerable(true);
        minecart.setMaxSpeed(0);
        
        BalloonRide ride = new BalloonRide(minecart, route.name, null, true);
        ride.route = route;
        
        // Offset-Start: Ballons starten versetzt, damit sie nicht alle am gleichen Ort sind
        int offsetWaypoint = (balloonIndex * route.waypoints.size()) / totalBalloons;
        ride.currentWaypointIndex = offsetWaypoint;
        ride.nextWaypointIndex = (offsetWaypoint + 1) % route.waypoints.size();
        ride.setDestination(route.waypoints.get(ride.nextWaypointIndex), ride.nextWaypointIndex);
        
        return ride;
    }

    private void stopAutoTravel(Player player, String routeName) {
        if (!routes.containsKey(routeName)) {
            player.sendMessage("§c✗ Route '§e" + routeName + "§c' existiert nicht!");
            return;
        }
        
        BalloonRoute route = routes.get(routeName);
        
        // Entferne alle Ballons dieser Route
        for (BalloonRide ride : route.activeBalloons) {
            if (ride.minecart.isValid()) {
                ride.minecart.remove();
            }
            activeBalloons.remove(ride.minecart.getUniqueId());
        }
        
        route.activeBalloons.clear();
        route.isActive = false;
        
        player.sendMessage("§6✓ Auto-Travel für Route '§e" + routeName + "§6' beendet!");
    }

    private void listActiveRoutes(Player player) {
        player.sendMessage("§6=== Aktive Routen ===");
        if (routes.isEmpty()) {
            player.sendMessage("§7Keine Routen vorhanden.");
            return;
        }
        
        for (BalloonRoute route : routes.values()) {
            String status = route.isActive ? "§a✓ Aktiv" : "§c✗ Inaktiv";
            player.sendMessage("  §e" + route.name + " §6[" + status + "§6] - " + 
                               route.waypoints.size() + " Waypoints, " +
                               route.boardingWaypoints.size() + " Boarding, " +
                               route.dropoffWaypoints.size() + " Dropoff");
        }
    }

    private void addBoardingZone(Player player, String routeName, String waypointStr) {
        if (!routes.containsKey(routeName)) {
            player.sendMessage("§c✗ Route '§e" + routeName + "§c' existiert nicht!");
            return;
        }
        
        try {
            int waypointIndex = Integer.parseInt(waypointStr);
            BalloonRoute route = routes.get(routeName);
            
            if (waypointIndex < 0 || waypointIndex >= route.waypoints.size()) {
                player.sendMessage("§c✗ Waypoint §e#" + waypointIndex + "§c existiert nicht!");
                return;
            }
            
            route.addBoardingZone(waypointIndex);
            Location zone = route.waypoints.get(waypointIndex);
            
            player.sendMessage("§6✓ Boarding-Zone bei Waypoint §e#" + waypointIndex + " §6aktiviert!");
            player.sendMessage("   §e" + zone.getBlockX() + ", " + zone.getBlockY() + ", " + zone.getBlockZ());
            
            // Zeige die Zone mit Partikeln
            showBoardingZone(zone);
            
        } catch (NumberFormatException e) {
            player.sendMessage("§c✗ Bitte gib eine Waypoint-Nummer ein!");
        }
    }

    private void addDropoffZone(Player player, String routeName, String waypointStr) {
        if (!routes.containsKey(routeName)) {
            player.sendMessage("§c✗ Route '§e" + routeName + "§c' existiert nicht!");
            return;
        }
        
        try {
            int waypointIndex = Integer.parseInt(waypointStr);
            BalloonRoute route = routes.get(routeName);
            
            if (waypointIndex < 0 || waypointIndex >= route.waypoints.size()) {
                player.sendMessage("§c✗ Waypoint §e#" + waypointIndex + "§c existiert nicht!");
                return;
            }
            
            route.addDropoffZone(waypointIndex);
            Location zone = route.waypoints.get(waypointIndex);
            
            player.sendMessage("§6✓ Dropoff-Zone bei Waypoint §e#" + waypointIndex + " §6aktiviert!");
            player.sendMessage("   §e" + zone.getBlockX() + ", " + zone.getBlockY() + ", " + zone.getBlockZ());
            
            // Zeige die Zone mit Partikeln
            showDropoffZone(zone);
            
        } catch (NumberFormatException e) {
            player.sendMessage("§c✗ Bitte gib eine Waypoint-Nummer ein!");
        }
    }



    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        BalloonRide ride = playerToBalloon.remove(event.getPlayer().getUniqueId());
        if (ride != null) {
            ride.removePassenger(event.getPlayer());
        }
    }

    private void checkBoardingZones() {
        // Checke alle Online Spieler
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Spieler ist bereits auf einem Ballon
            if (playerToBalloon.containsKey(player.getUniqueId())) {
                BalloonRide ride = playerToBalloon.get(player.getUniqueId());
                
                // Checke ob Spieler in einer Dropoff-Zone ist
                Integer dropoffZoneIndex = ride.route.getNearestDropoffZoneIndex(player.getLocation());
                
                if (dropoffZoneIndex != null && !ride.isMoving) {
                    // Ballon steht still und Spieler ist in Dropoff-Zone
                    exitBalloonAutomatic(player, ride.route.waypoints.get(dropoffZoneIndex));
                }
                continue;
            }
            
            // Spieler ist NICHT auf einem Ballon - checke Boarding Zonen
            for (BalloonRoute route : routes.values()) {
                if (!route.isActive || route.activeBalloons.isEmpty()) {
                    continue;
                }
                
                // Checke ob Spieler in einer Boarding-Zone ist
                Integer boardingZoneIndex = route.getNearestBoardingZoneIndex(player.getLocation());
                
                if (boardingZoneIndex != null) {
                    // Finde nächsten Ballon in dieser Route
                    BalloonRide nearestBalloon = null;
                    double nearestDistance = Double.MAX_VALUE;
                    
                    for (BalloonRide ride : route.activeBalloons) {
                        double distance = player.getLocation().distance(ride.minecart.getLocation());
                        if (distance < nearestDistance && distance < 30) { // Max 30 Blöcke
                            nearestDistance = distance;
                            nearestBalloon = ride;
                        }
                    }
                    
                    if (nearestBalloon != null && !playerToBalloon.containsKey(player.getUniqueId())) {
                        // Spieler automatisch aufnehmen
                        boardBalloonAutomatic(player, nearestBalloon);
                        break;
                    }
                }
            }
        }
    }
    
    private void boardBalloonAutomatic(Player player, BalloonRide ride) {
        ride.addPassenger(player);
        playerToBalloon.put(player.getUniqueId(), ride);
        
        player.sendMessage("§6✨ Du bist auf dem Ballon aufgestiegen!");
        player.playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 0.5f, 1.2f);
        
        // Zeige Action Bar
        player.sendActionBar("§e🎈 Willkommen auf deiner Reise!");
    }
    
    private void exitBalloonAutomatic(Player player, Location dropoffZone) {
        BalloonRide ride = playerToBalloon.remove(player.getUniqueId());
        
        if (ride == null) return;
        
        ride.removePassenger(player);
        
        // Teleportiere Spieler in die Dropoff-Zone
        Location safeLocation = dropoffZone.clone().add(0, 1, 0);
        player.teleport(safeLocation);
        
        player.sendMessage("§6🎈 Du bist ausgestiegen! Danke für deine Reise!");
        player.playSound(player.getLocation(), Sound.ENTITY_BAT_AMBIENT, 0.5f, 0.8f);
        
        // Zeige Action Bar
        player.sendActionBar("§e✨ Bis zur nächsten Reise!");
    }
        
    private void updateBalloons() {
        for (BalloonRide ride : new ArrayList<>(activeBalloons.values())) {
            if (!ride.minecart.isValid()) {
                ride.removeStructure();
                activeBalloons.remove(ride.minecart.getUniqueId());
                if (ride.route != null) {
                    ride.route.activeBalloons.remove(ride);
                }
                continue;
            }
            
            if (!ride.isMoving) {
                continue;
            }
            
            // Berechne die Richtung zum Ziel
            Location current = ride.minecart.getLocation();
            Location target = ride.destination;
            
            Vector direction = target.clone().subtract(current.toVector()).normalize();
            double distance = current.distance(target);
            
            // Geschwindigkeit: 0.3 Blöcke pro Tick (etwas langsamer für Auto-Travel)
            double speed = 0.3;
            
            if (distance > speed) {
                // Noch nicht angekommen
                Vector newVelocity = direction.multiply(speed);
                ride.minecart.setVelocity(newVelocity);
                
                // UPDATE VISUAL STRUCTURE mit Rotation
                ride.updateStructure(current, direction);
                
                // Partikel-Effekt
                ride.minecart.getWorld().spawnParticle(
                        Particle.CLOUD,
                        current.clone().add(0, 1, 0),
                        2, 0.3, 0.3, 0.3, 0.02
                );
                
                // Sound alle 40 Ticks (2 Sekunden)
                if (ride.ticksMoving % 40 == 0) {
                    ride.minecart.getWorld().playSound(
                            current,
                            Sound.AMBIENT_CAVE,
                            0.2f,
                            1.5f
                    );
                }
                
                ride.ticksMoving++;
                
            } else {
                // Angekommen am Ziel - Landung!
                ride.minecart.setVelocity(new Vector(0, 0, 0));
                ride.minecart.teleport(target);
                ride.updateStructure(target, new Vector(0, 0, 0)); // Keine Rotation beim Landen
                
                // Lande Partikel
                ride.minecart.getWorld().spawnParticle(
                        Particle.FIREWORK,
                        target.clone().add(0, 1, 0),
                        15, 0.5, 0.5, 0.5, 0.05
                );
                
                // Lande Sound
                ride.minecart.getWorld().playSound(
                        target,
                        Sound.ENTITY_ENDER_PEARL_HIT,
                        0.8f,
                        1.0f
                );
                
                // Alle Passagiere benachrichtigen
                for (Player p : ride.getPassengers()) {
                    p.sendMessage("§6🎈 Gelandet bei Waypoint §e#" + ride.nextWaypointIndex);
                }
                
                // Für Auto-Travel: Zum nächsten Waypoint gehen
                if (ride.isAutoTravel && ride.route != null) {
                    // Kurze Pause am Waypoint (40 Ticks = 2 Sekunden)
                    ride.landingTicks = 40;
                }
            }
            
            // Halte die Passagiere oben
            for (Player p : ride.getPassengers()) {
                if (p.isOnline()) {
                    p.teleport(current.clone().add(0, 1.5, 0));
                }
            }
            
            // Auto-Travel: Nach Landung zum nächsten Waypoint
            if (ride.isAutoTravel && ride.route != null && ride.landingTicks > 0) {
                ride.landingTicks--;
                
                if (ride.landingTicks == 0) {
                    // Nächster Waypoint
                    ride.currentWaypointIndex = ride.nextWaypointIndex;
                    ride.nextWaypointIndex = (ride.currentWaypointIndex + 1) % ride.route.waypoints.size();
                    ride.setDestination(ride.route.waypoints.get(ride.nextWaypointIndex), ride.nextWaypointIndex);
                    
                    // Start-Sound
                    ride.minecart.getWorld().playSound(
                            ride.minecart.getLocation(),
                            Sound.ENTITY_BAT_TAKEOFF,
                            0.5f,
                            1.2f
                    );
                }
            }
        }
    }

    private void showWaypoint(Location loc, World world) {
        // Zeige das Waypoint mit Partikeln an
        for (int i = 0; i < 10; i++) {
            world.spawnParticle(
                    Particle.HAPPY_VILLAGER,
                    loc.clone().add(0, i * 0.5, 0),
                    5, 0.3, 0.3, 0.3, 0
            );
        }
    }

    private void createElevator(Player player, String elevatorName) {
        if (elevatorRoutes.containsKey(elevatorName)) {
            player.sendMessage("§c✗ Aufzug '§e" + elevatorName + "§c' existiert bereits!");
            return;
        }
        
        ElevatorRoute route = new ElevatorRoute(elevatorName);
        elevatorRoutes.put(elevatorName, route);
        player.sendMessage("§6✓ Aufzug '§e" + elevatorName + "§6' erstellt!");
        player.sendMessage("§eVerwendet: §7/ballon elevator floor " + elevatorName);
    }

    private void addFloorToElevator(Player player, String elevatorName) {
        if (!elevatorRoutes.containsKey(elevatorName)) {
            player.sendMessage("§c✗ Aufzug '§e" + elevatorName + "§c' existiert nicht!");
            return;
        }
        
        Location floorLoc = player.getLocation().clone();
        ElevatorRoute route = elevatorRoutes.get(elevatorName);
        route.addFloor(floorLoc);
        
        int index = route.floors.size() - 1;
        player.sendMessage("§6✓ Etage §e#" + index + " §6hinzugefügt!");
        player.sendMessage("   §eX: " + floorLoc.getBlockX() + " Y: " + floorLoc.getBlockY() + " Z: " + floorLoc.getBlockZ());
        
        showBoardingZone(floorLoc);
    }

    private void listElevatorFloors(Player player, String elevatorName) {
        if (!elevatorRoutes.containsKey(elevatorName)) {
            player.sendMessage("§c✗ Aufzug '§e" + elevatorName + "§c' existiert nicht!");
            return;
        }
        
        ElevatorRoute route = elevatorRoutes.get(elevatorName);
        if (route.floors.isEmpty()) {
            player.sendMessage("§c✗ Aufzug '§e" + elevatorName + "§c' hat keine Etagen!");
            return;
        }
        
        player.sendMessage("§6=== Aufzug '§e" + elevatorName + "§6' ===");
        player.sendMessage("§7Status: " + (route.isActive ? "§a✓ Aktiv" : "§c✗ Inaktiv"));
        if (route.isActive) {
            player.sendMessage("§7Aktive Aufzüge: §e" + route.activeElevators.size());
        }
        player.sendMessage("§6Etagen:");
        for (int i = 0; i < route.floors.size(); i++) {
            Location loc = route.floors.get(i);
            player.sendMessage("  §e#" + i + " §6→ X: " + loc.getBlockX() + " Y: " + loc.getBlockY() + " Z: " + loc.getBlockZ());
        }
    }

    private void startElevator(Player player, String elevatorName) {
        if (!elevatorRoutes.containsKey(elevatorName)) {
            player.sendMessage("§c✗ Aufzug '§e" + elevatorName + "§c' existiert nicht!");
            return;
        }
        
        ElevatorRoute route = elevatorRoutes.get(elevatorName);
        if (route.floors.size() < 2) {
            player.sendMessage("§c✗ Aufzug braucht mindestens 2 Etagen!");
            return;
        }
        
        route.isActive = true;
        
        // Erstelle einen Aufzug
        ElevatorRide ride = createAutoElevator(route);
        activeElevators.put(ride.minecart.getUniqueId(), ride);
        route.activeElevators.add(ride);
        
        player.sendMessage("§6✓ Aufzug '§e" + elevatorName + "§6' gestartet!");
        player.sendMessage("   §eFährt automatisch zwischen " + route.floors.size() + " Etagen!");
        
        // Zeige alle Etagen
        for (Location floor : route.floors) {
            showBoardingZone(floor);
        }
    }

    private void stopElevator(Player player, String elevatorName) {
        if (!elevatorRoutes.containsKey(elevatorName)) {
            player.sendMessage("§c✗ Aufzug '§e" + elevatorName + "§c' existiert nicht!");
            return;
        }
        
        ElevatorRoute route = elevatorRoutes.get(elevatorName);
        
        // Entferne alle Aufzüge dieser Route
        for (ElevatorRide ride : route.activeElevators) {
            if (ride.minecart.isValid()) {
                ride.minecart.remove();
            }
            ride.removeStructure();
            activeElevators.remove(ride.minecart.getUniqueId());
        }
        
        route.activeElevators.clear();
        route.isActive = false;
        
        player.sendMessage("§6✓ Aufzug '§e" + elevatorName + "§6' gestoppt!");
    }

    private ElevatorRide createAutoElevator(ElevatorRoute route) {
        Location startLoc = route.floors.get(0).clone();
        Minecart minecart = (Minecart) startLoc.getWorld().spawnEntity(startLoc, EntityType.MINECART);
        minecart.setGravity(false);
        minecart.setInvulnerable(true);
        minecart.setMaxSpeed(0);
        
        ElevatorRide ride = new ElevatorRide(minecart, route.name, route);
        ride.setNextFloor(1); // Starte zur Etage 1
        
        return ride;
    }

    // ====== ELEVATOR KLASSEN ======
    
    private static class ElevatorRoute {
        String name;
        List<Location> floors = new ArrayList<>();
        List<ElevatorRide> activeElevators = new ArrayList<>();
        boolean isActive = false;
        static final double ZONE_RADIUS = 5.0;
        
        ElevatorRoute(String name) {
            this.name = name;
        }
        
        void addFloor(Location loc) {
            this.floors.add(loc.clone());
        }
        
        Integer getNearestBoardingZone(Location playerLoc) {
            for (int i = 0; i < floors.size(); i++) {
                if (playerLoc.distance(floors.get(i)) <= ZONE_RADIUS) {
                    return i;
                }
            }
            return null;
        }
    }
    
    private static class ElevatorRide {
        Minecart minecart;
        String routeName;
        ElevatorRoute route;
        List<Player> passengers = new ArrayList<>();
        
        ElevatorStructure structure;
        
        Location destination;
        int currentFloor = 0;
        int nextFloor = 1;
        int landingTicks = 0;
        
        boolean isMoving = false;
        int ticksMoving = 0;
        
        ElevatorRide(Minecart minecart, String routeName, ElevatorRoute route) {
            this.minecart = minecart;
            this.routeName = routeName;
            this.route = route;
            this.structure = new ElevatorStructure(minecart.getLocation());
        }
        
        void setNextFloor(int floor) {
            if (floor >= 0 && floor < route.floors.size()) {
                this.destination = route.floors.get(floor);
                this.nextFloor = floor;
                this.isMoving = true;
                this.ticksMoving = 0;
            }
        }
        
        void addPassenger(Player player) {
            if (!passengers.contains(player)) {
                passengers.add(player);
                minecart.addPassenger(player);
            }
        }
        
        void removePassenger(Player player) {
            passengers.remove(player);
            if (minecart.isValid()) {
                minecart.removePassenger(player);
            }
        }
        
        void removeStructure() {
            if (structure != null) {
                structure.remove();
            }
        }
    }
    
    // ElevatorStructure: Fahrstuhl-Kabine mit Türen
    private static class ElevatorStructure {
        List<ArmorStand> stands = new ArrayList<>();
        Location baseLocation;
        boolean doorsOpen = false;
        
        ElevatorStructure(Location loc) {
            this.baseLocation = loc.clone();
            createStructure();
        }
        
        private void createStructure() {
            World world = baseLocation.getWorld();
            
            // === KABINEN-RAHMEN ===
            // Vorne und Hinten (Eisengitter)
            for (int x = -2; x <= 2; x++) {
                for (int y = 0; y <= 3; y++) {
                    // Vorne
                    createItemDisplay(baseLocation.clone().add(x, y, -2), Material.IRON_BARS);
                    // Hinten
                    createItemDisplay(baseLocation.clone().add(x, y, 2), Material.IRON_BARS);
                }
            }
            
            // Links und Rechts (Eisengitter)
            for (int z = -2; z <= 2; z++) {
                for (int y = 0; y <= 3; y++) {
                    // Links
                    createItemDisplay(baseLocation.clone().add(-2, y, z), Material.IRON_BARS);
                    // Rechts
                    createItemDisplay(baseLocation.clone().add(2, y, z), Material.IRON_BARS);
                }
            }
            
            // === TÜREN (vorne) ===
            // Linke Tür
            for (int y = 0; y <= 2; y++) {
                createItemDisplay(baseLocation.clone().add(-1, y, -2.5), Material.DARK_OAK_DOOR);
            }
            // Rechte Tür
            for (int y = 0; y <= 2; y++) {
                createItemDisplay(baseLocation.clone().add(1, y, -2.5), Material.DARK_OAK_DOOR);
            }
            
            // === BODEN ===
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    createItemDisplay(baseLocation.clone().add(x, -1, z), Material.IRON_BLOCK);
                }
            }
            
            // === DECKE ===
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    createItemDisplay(baseLocation.clone().add(x, 4, z), Material.IRON_BLOCK);
                }
            }
        }
        
        private void createItemDisplay(Location loc, Material material) {
            ArmorStand stand = loc.getWorld().spawn(loc, ArmorStand.class);
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setInvulnerable(true);
            
            org.bukkit.inventory.ItemStack head = new org.bukkit.inventory.ItemStack(material);
            stand.getEquipment().setHelmet(head);
            
            stands.add(stand);
        }
        
        void updatePosition(Location newLoc) {
            for (ArmorStand stand : stands) {
                if (!stand.isValid()) continue;
                
                Vector offset = stand.getLocation().toVector().subtract(baseLocation.toVector());
                stand.teleport(newLoc.clone().add(offset));
            }
            
            baseLocation = newLoc.clone();
        }
        
        void remove() {
            for (ArmorStand stand : stands) {
                if (stand.isValid()) {
                    stand.remove();
                }
            }
            stands.clear();
        }
    }
    
    // ElevatorRoute: Eine Aufzug-Route mit mehreren Etagen
    private static class ElevatorRoute {
        String name;
        List<Location> floors = new ArrayList<>();
        List<Integer> boardingFloors = new ArrayList<>();
        List<Integer> dropoffFloors = new ArrayList<>();
        List<ElevatorRide> activeElevators = new ArrayList<>();
        boolean isActive = false;
        static final double ZONE_RADIUS = 3.0; // 3 Blöcke Radius
        
        ElevatorRoute(String name) {
            this.name = name;
        }
        
        void addFloor(Location loc) {
            this.floors.add(loc);
        }
        
        void addBoardingFloor(int floorIndex) {
            if (floorIndex >= 0 && floorIndex < floors.size()) {
                if (!boardingFloors.contains(floorIndex)) {
                    boardingFloors.add(floorIndex);
                }
            }
        }
        
        void addDropoffFloor(int floorIndex) {
            if (floorIndex >= 0 && floorIndex < floors.size()) {
                if (!dropoffFloors.contains(floorIndex)) {
                    dropoffFloors.add(floorIndex);
                }
            }
        }
        
        Integer getNearestBoardingFloorIndex(Location playerLoc) {
            for (Integer index : boardingFloors) {
                Location zone = floors.get(index);
                if (playerLoc.distance(zone) <= ZONE_RADIUS) {
                    return index;
                }
            }
            return null;
        }
        
        Integer getNearestDropoffFloorIndex(Location playerLoc) {
            for (Integer index : dropoffFloors) {
                Location zone = floors.get(index);
                if (playerLoc.distance(zone) <= ZONE_RADIUS) {
                    return index;
                }
            }
            return null;
        }
    }
    
    // BalloonRoute: Eine Reiseroute mit mehreren Waypoints
    private static class BalloonRoute {
        String name;
        List<Location> waypoints = new ArrayList<>();
        List<Integer> boardingWaypoints = new ArrayList<>(); // Waypoint-Indizes
        List<Integer> dropoffWaypoints = new ArrayList<>();  // Waypoint-Indizes
        List<BalloonRide> activeBalloons = new ArrayList<>();
        boolean isActive = false;
        static final double ZONE_RADIUS = 5.0; // 5 Blöcke Radius für Zonen
        
        BalloonRoute(String name) {
            this.name = name;
        }
        
        void addWaypoint(Location loc) {
            this.waypoints.add(loc);
            // Keine automatischen Zonen! Spieler müssen sie manuell hinzufügen
        }
        
        // Markiere einen Waypoint als Boarding-Zone
        void addBoardingZone(int waypointIndex) {
            if (waypointIndex >= 0 && waypointIndex < waypoints.size()) {
                if (!boardingWaypoints.contains(waypointIndex)) {
                    boardingWaypoints.add(waypointIndex);
                }
            }
        }
        
        // Markiere einen Waypoint als Dropoff-Zone
        void addDropoffZone(int waypointIndex) {
            if (waypointIndex >= 0 && waypointIndex < waypoints.size()) {
                if (!dropoffWaypoints.contains(waypointIndex)) {
                    dropoffWaypoints.add(waypointIndex);
                }
            }
        }
        
        // Checkt ob Spieler in einer Boarding-Zone ist
        Integer getNearestBoardingZoneIndex(Location playerLoc) {
            for (Integer index : boardingWaypoints) {
                Location zone = waypoints.get(index);
                if (playerLoc.distance(zone) <= ZONE_RADIUS) {
                    return index;
                }
            }
            return null;
        }
        
        // Checkt ob Spieler in einer Dropoff-Zone ist
        Integer getNearestDropoffZoneIndex(Location playerLoc) {
            for (Integer index : dropoffWaypoints) {
                Location zone = waypoints.get(index);
                if (playerLoc.distance(zone) <= ZONE_RADIUS) {
                    return index;
                }
            }
            return null;
        }
    }
    
    // ElevatorRide: Ein einzelner Aufzug im Loop
    private static class ElevatorRide {
        Minecart minecart;
        String elevatorName;
        ElevatorStructure structure;
        List<Player> passengers = new ArrayList<>();
        
        List<Location> floors = new ArrayList<>();
        int currentFloorIndex = 0;
        int nextFloorIndex = 1;
        int landingTicks = 0; // Pause an Etage
        
        boolean isMoving = false;
        int ticksMoving = 0;
        
        ElevatorRide(Minecart minecart, String elevatorName, List<Location> floors) {
            this.minecart = minecart;
            this.elevatorName = elevatorName;
            this.floors = new ArrayList<>(floors);
            this.structure = new ElevatorStructure(minecart.getLocation());
            
            if (!floors.isEmpty()) {
                setDestination(floors.get(nextFloorIndex));
            }
        }
        
        void setDestination(Location dest) {
            this.isMoving = true;
            this.ticksMoving = 0;
        }
        
        void addPassenger(Player player) {
            if (!passengers.contains(player)) {
                passengers.add(player);
                minecart.addPassenger(player);
            }
        }
        
        void removePassenger(Player player) {
            passengers.remove(player);
            if (minecart.isValid()) {
                minecart.removePassenger(player);
            }
        }
        
        List<Player> getPassengers() {
            return new ArrayList<>(passengers);
        }
        
        void removeStructure() {
            if (structure != null) {
                structure.remove();
            }
        }
    }
    
    // BalloonRide: Ein einzelner Ballon in Fahrt
    private static class BalloonRide {
        Minecart minecart;
        String routeName;
        UUID owner;
        List<Player> passengers = new ArrayList<>();
        Location destination;
        
        // Visual Structure
        BalloonStructure structure;
        
        BalloonRoute route; // Für Auto-Travel
        boolean isAutoTravel;
        int currentWaypointIndex = 0;
        int nextWaypointIndex = 1;
        int landingTicks = 0; // Pause nach Landung
        
        boolean isMoving = false;
        int ticksMoving = 0;
        int lastWaypointIndex;
        
        // Rotation
        float yaw = 0;
        float pitch = 0;
        
        BalloonRide(Minecart minecart, String routeName, UUID owner) {
            this(minecart, routeName, owner, false);
        }
        
        BalloonRide(Minecart minecart, String routeName, UUID owner, boolean isAutoTravel) {
            this.minecart = minecart;
            this.routeName = routeName;
            this.owner = owner;
            this.isAutoTravel = isAutoTravel;
            this.structure = new BalloonStructure(minecart.getLocation());
        }
        
        void setDestination(Location dest, int waypointIndex) {
            this.destination = dest;
            this.lastWaypointIndex = waypointIndex;
            this.isMoving = true;
            this.ticksMoving = 0;
        }
        
        void addPassenger(Player player) {
            if (!passengers.contains(player)) {
                passengers.add(player);
                minecart.addPassenger(player);
            }
        }
        
        void removePassenger(Player player) {
            passengers.remove(player);
            if (minecart.isValid()) {
                minecart.removePassenger(player);
            }
        }
        
        List<Player> getPassengers() {
            return new ArrayList<>(passengers);
        }
        
        void stop() {
            isMoving = false;
            minecart.setVelocity(new Vector(0, 0, 0));
        }
        
        void updateStructure(Location newLoc, Vector direction) {
            if (structure == null) return;
            
            // Berechne Rotation basierend auf Richtung
            if (direction.length() > 0.01) {
                // Yaw: horizontal (left/right)
                yaw = (float) Math.atan2(-direction.getX(), direction.getZ()) * 180 / (float) Math.PI;
                
                // Pitch: vertikal (up/down)
                double horizontalDistance = Math.sqrt(direction.getX() * direction.getX() + direction.getZ() * direction.getZ());
                pitch = (float) Math.atan2(direction.getY(), horizontalDistance) * 180 / (float) Math.PI;
            }
            
            structure.updatePosition(newLoc, yaw, pitch);
        }
        
        void removeStructure() {
            if (structure != null) {
                structure.remove();
            }
        }
    }
    
    // ElevatorStructure: Die visuelle Struktur eines Fahrstuhls
    private static class ElevatorStructure {
        List<ArmorStand> stands = new ArrayList<>();
        Location baseLocation;
        int cabinWidth = 3;  // 3x3 Kabine
        int cabinHeight = 3; // 3 Blöcke hoch
        
        ElevatorStructure(Location loc) {
            this.baseLocation = loc.clone();
            createStructure();
        }
        
        private void createStructure() {
            World world = baseLocation.getWorld();
            double x = baseLocation.getX();
            double y = baseLocation.getY();
            double z = baseLocation.getZ();
            
            // === KABINEN-RAHMEN ===
            createVerticalBar(world, x - 1.5, y, z - 1.5, cabinHeight, Material.IRON_BARS);
            createVerticalBar(world, x + 1.5, y, z - 1.5, cabinHeight, Material.IRON_BARS);
            createVerticalBar(world, x - 1.5, y, z + 1.5, cabinHeight, Material.IRON_BARS);
            createVerticalBar(world, x + 1.5, y, z + 1.5, cabinHeight, Material.IRON_BARS);
            
            // === KABINEN-BODEN ===
            for (double bx = x - 1.5; bx <= x + 1.5; bx += 0.5) {
                for (double bz = z - 1.5; bz <= z + 1.5; bz += 0.5) {
                    createItemDisplay(new Location(world, bx, y, bz), Material.DARK_OAK_WOOD);
                }
            }
            
            // === KABINEN-DACH ===
            for (double bx = x - 1.5; bx <= x + 1.5; bx += 0.5) {
                for (double bz = z - 1.5; bz <= z + 1.5; bz += 0.5) {
                    createItemDisplay(new Location(world, bx, y + cabinHeight - 0.1, bz), Material.DARK_OAK_WOOD);
                }
            }
            
            // === VORDERE TÜREN ===
            for (double ty = y + 0.5; ty < y + cabinHeight - 0.5; ty += 0.5) {
                createItemDisplay(new Location(world, x - 0.5, ty, z - 1.5), Material.IRON_DOOR);
                createItemDisplay(new Location(world, x + 0.5, ty, z - 1.5), Material.IRON_DOOR);
            }
            
            // === SEITENWÄNDE ===
            for (double tx = x - 1.5; tx <= x - 0.5; tx += 0.5) {
                for (double ty = y + 0.5; ty < y + cabinHeight - 0.5; ty += 0.5) {
                    createItemDisplay(new Location(world, tx, ty, z - 1.5), Material.GRAY_CONCRETE);
                }
            }
            for (double tx = x + 0.5; tx <= x + 1.5; tx += 0.5) {
                for (double ty = y + 0.5; ty < y + cabinHeight - 0.5; ty += 0.5) {
                    createItemDisplay(new Location(world, tx, ty, z - 1.5), Material.GRAY_CONCRETE);
                }
            }
            
            // === RÜCKWAND ===
            for (double tx = x - 1.5; tx <= x + 1.5; tx += 0.5) {
                for (double ty = y + 0.5; ty < y + cabinHeight - 0.5; ty += 0.5) {
                    createItemDisplay(new Location(world, tx, ty, z + 1.5), Material.GRAY_CONCRETE);
                }
            }
            
            // === SCHACHT-SEILE ===
            createVerticalBar(world, x - 2, y + cabinHeight, z - 1.5, 10, Material.CHAIN);
            createVerticalBar(world, x + 2, y + cabinHeight, z - 1.5, 10, Material.CHAIN);
            createVerticalBar(world, x - 2, y + cabinHeight, z + 1.5, 10, Material.CHAIN);
            createVerticalBar(world, x + 2, y + cabinHeight, z + 1.5, 10, Material.CHAIN);
            
            // === ETAGEN-ANZEIGE (LED) ===
            createItemDisplay(new Location(world, x, y + cabinHeight + 0.5, z - 1.5), Material.REDSTONE_LAMP);
        }
        
        private void createVerticalBar(World world, double x, double y, double z, int length, Material material) {
            for (int i = 0; i < length; i++) {
                createItemDisplay(new Location(world, x, y + i, z), material);
            }
        }
        
        private void createItemDisplay(Location loc, Material material) {
            ArmorStand stand = loc.getWorld().spawn(loc, ArmorStand.class);
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setInvulnerable(true);
            
            org.bukkit.inventory.ItemStack head = new org.bukkit.inventory.ItemStack(material);
            stand.getEquipment().setHelmet(head);
            
            stands.add(stand);
        }
        
        void updatePosition(Location newLoc) {
            double yOffset = newLoc.getY() - baseLocation.getY();
            
            for (ArmorStand stand : stands) {
                if (!stand.isValid()) continue;
                
                Location currentLoc = stand.getLocation();
                Location newStandLoc = currentLoc.clone().add(0, yOffset, 0);
                stand.teleport(newStandLoc);
            }
            
            baseLocation = newLoc.clone();
        }
        
        void remove() {
            for (ArmorStand stand : stands) {
                if (stand.isValid()) {
                    stand.remove();
                }
            }
            stands.clear();
        }
    }
    
    // BalloonStructure: Die visuelle Struktur des Ballons
    private static class BalloonStructure {
        List<ArmorStand> stands = new ArrayList<>();
        Location baseLocation;
        
        BalloonStructure(Location loc) {
            this.baseLocation = loc.clone();
            createStructure();
        }
        
        private void createStructure() {
            World world = baseLocation.getWorld();
            
            // === BALLON-HÜLLE (Kuppel oben) ===
            // Layer 0 (oben) - rot
            createBalloonLayer(world, 0, 4, Material.RED_WOOL);
            
            // Layer 1 - rot
            createBalloonLayer(world, 1, 6, Material.RED_WOOL);
            
            // Layer 2 (Mitte) - orange Accent
            createBalloonLayer(world, 2, 8, Material.ORANGE_WOOL);
            
            // Layer 3 - rot
            createBalloonLayer(world, 3, 6, Material.RED_WOOL);
            
            // Layer 4 (unten Ballon) - rot
            createBalloonLayer(world, 4, 4, Material.RED_WOOL);
            
            // === SEILE/KETTEN ===
            createRopes(world);
            
            // === KORB ===
            createBasket(world);
        }
        
        private void createBalloonLayer(World world, int height, int radius, Material material) {
            double centerX = baseLocation.getX();
            double centerY = baseLocation.getY() + height;
            double centerZ = baseLocation.getZ();
            
            int pointsInCircle = (int) (Math.PI * radius);
            for (int i = 0; i < pointsInCircle; i++) {
                double angle = (2 * Math.PI * i) / pointsInCircle;
                double x = centerX + radius * Math.cos(angle);
                double z = centerZ + radius * Math.sin(angle);
                
                Location displayLoc = new Location(world, x, centerY, z);
                createItemDisplay(displayLoc, material);
            }
        }
        
        private void createRopes(World world) {
            // 4 Seile von Ballon zum Korb
            double balloonBottomY = baseLocation.getY() + 4;
            double basketTopY = baseLocation.getY() - 2;
            
            double[] offsets = {2, -2};
            for (double xOff : offsets) {
                for (double zOff : offsets) {
                    // Seile vertikal zeichnen
                    for (double y = balloonBottomY; y > basketTopY; y -= 0.5) {
                        Location ropeLoc = new Location(world, 
                            baseLocation.getX() + xOff, 
                            y, 
                            baseLocation.getZ() + zOff);
                        createItemDisplay(ropeLoc, Material.CHAIN);
                    }
                }
            }
        }
        
        private void createBasket(World world) {
            double basketY = baseLocation.getY() - 2;
            double basketX = baseLocation.getX();
            double basketZ = baseLocation.getZ();
            
            // Korb-Rahmen (Eiche)
            for (double x = basketX - 1.5; x <= basketX + 1.5; x += 0.5) {
                for (double z = basketZ - 1.5; z <= basketZ + 1.5; z += 0.5) {
                    // Nur Ränder
                    if (Math.abs(x - basketX) < 0.1 || Math.abs(x - basketX - 3) < 0.1 ||
                        Math.abs(z - basketZ) < 0.1 || Math.abs(z - basketZ - 3) < 0.1) {
                        createItemDisplay(new Location(world, x, basketY, z), Material.OAK_WOOD);
                    }
                }
            }
            
            // Korb-Innenraum (Wicker/Braun)
            for (double x = basketX - 1; x < basketX + 1; x += 0.4) {
                for (double z = basketZ - 1; z < basketZ + 1; z += 0.4) {
                    createItemDisplay(new Location(world, x, basketY, z), Material.BROWN_WOOL);
                }
            }
        }
        
        private void createItemDisplay(Location loc, Material material) {
            // Erstelle ein Item-Display für den Block
            ArmorStand stand = loc.getWorld().spawn(loc, ArmorStand.class);
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setInvulnerable(true);
            
            // Kopf-Dekoration mit Block-Skin simulieren
            org.bukkit.inventory.ItemStack head = new org.bukkit.inventory.ItemStack(material);
            stand.getEquipment().setHelmet(head);
            
            stands.add(stand);
        }
        
        void updatePosition(Location newLoc, float yaw, float pitch) {
            // Verschiebe alle Stands zur neuen Position mit Rotation
            for (ArmorStand stand : stands) {
                if (!stand.isValid()) continue;
                
                // Berechne relative Position mit Rotation
                Vector relativePos = stand.getLocation().toVector().subtract(baseLocation.toVector());
                Vector rotated = rotateVector(relativePos, yaw, pitch);
                
                Location newStandLoc = newLoc.clone().add(rotated);
                stand.teleport(newStandLoc);
            }
            
            baseLocation = newLoc.clone();
        }
        
        private Vector rotateVector(Vector v, float yawDegrees, float pitchDegrees) {
            float yaw = (float) Math.toRadians(yawDegrees);
            float pitch = (float) Math.toRadians(pitchDegrees);
            
            double x = v.getX();
            double y = v.getY();
            double z = v.getZ();
            
            // Yaw rotation (around Y axis)
            double x1 = x * Math.cos(yaw) - z * Math.sin(yaw);
            double z1 = x * Math.sin(yaw) + z * Math.cos(yaw);
            
            // Pitch rotation (around X axis)
            double y2 = y * Math.cos(pitch) - z1 * Math.sin(pitch);
            double z2 = y * Math.sin(pitch) + z1 * Math.cos(pitch);
            
            return new Vector(x1, y2, z2);
        }
        
        void remove() {
            for (ArmorStand stand : stands) {
                if (stand.isValid()) {
                    stand.remove();
                }
            }
            stands.clear();
        }
    }
}
