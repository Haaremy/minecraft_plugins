package de.haaremy.hmyparkour;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;

public class ParkourListener implements Listener {

    private final HmyParkour plugin;
    private final ParkourManager manager;

    public ParkourListener(HmyParkour plugin, ParkourManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Void-Detection fuer Parkour-Spieler
        if (manager.hasActiveSession(uuid)) {
            if (player.getLocation().getY() < 0) {
                manager.teleportToCheckpoint(player);
                return;
            }
        }

        // Nur pruefen wenn sich der Block unter dem Spieler geaendert hat
        Location to = event.getTo();
        Location from = event.getFrom();
        if (to.getBlockX() == from.getBlockX() &&
            to.getBlockY() == from.getBlockY() &&
            to.getBlockZ() == from.getBlockZ()) {
            return;
        }

        Block blockBelow = to.clone().subtract(0, 1, 0).getBlock();
        Material type = blockBelow.getType();

        // Pressure Plates erkennen
        if (type == Material.HEAVY_WEIGHTED_PRESSURE_PLATE) {
            // Start-Platte (Eisen) - Kurs starten
            handleStartPlate(player, blockBelow.getLocation());
        } else if (type == Material.LIGHT_WEIGHTED_PRESSURE_PLATE) {
            // Checkpoint-Platte (Gold)
            handleCheckpointPlate(player, blockBelow.getLocation());
        } else if (type == Material.STONE_PRESSURE_PLATE) {
            // Ziel-Platte (Stein)
            handleFinishPlate(player, blockBelow.getLocation());
        }
    }

    private void handleStartPlate(Player player, Location plateLocation) {
        if (manager.hasActiveSession(player.getUniqueId())) return;

        // Finde den Kurs mit diesem Startpunkt
        for (Map.Entry<String, ParkourCourse> entry : manager.getCourses().entrySet()) {
            ParkourCourse course = entry.getValue();
            if (course.isComplete() && isNearLocation(plateLocation, course.getStartLocation())) {
                manager.startCourse(player, entry.getKey());
                return;
            }
        }
    }

    private void handleCheckpointPlate(Player player, Location plateLocation) {
        ParkourSession session = manager.getSession(player.getUniqueId());
        if (session == null) return;

        ParkourCourse course = manager.getCourses().get(session.getCourseName());
        if (course == null) return;

        for (int i = 0; i < course.getCheckpoints().size(); i++) {
            if (isNearLocation(plateLocation, course.getCheckpoints().get(i))) {
                manager.onCheckpoint(player, i);
                return;
            }
        }
    }

    private void handleFinishPlate(Player player, Location plateLocation) {
        ParkourSession session = manager.getSession(player.getUniqueId());
        if (session == null) return;

        ParkourCourse course = manager.getCourses().get(session.getCourseName());
        if (course == null) return;

        if (isNearLocation(plateLocation, course.getEndLocation())) {
            manager.onFinish(player);
        }
    }

    private boolean isNearLocation(Location a, Location b) {
        if (a == null || b == null) return false;
        if (a.getWorld() == null || b.getWorld() == null) return false;
        if (!a.getWorld().equals(b.getWorld())) return false;
        return a.getBlockX() == b.getBlockX() &&
               a.getBlockY() == b.getBlockY() &&
               a.getBlockZ() == b.getBlockZ();
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!manager.hasActiveSession(player.getUniqueId())) return;

        // Fallschaden und anderen Schaden im Parkour verhindern
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (manager.hasActiveSession(player.getUniqueId())) {
            manager.quitCourse(player);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (manager.hasActiveSession(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (manager.hasActiveSession(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
