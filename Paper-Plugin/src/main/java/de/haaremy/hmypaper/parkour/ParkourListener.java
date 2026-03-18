package de.haaremy.hmypaper.parkour;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.block.Action;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ParkourListener implements Listener {

    private final ParkourManager parkourManager;

    // uuid → parkour name
    private final Map<UUID, String>   activeParkour   = new HashMap<>();
    // uuid → last reached checkpoint id (-1 = none)
    private final Map<UUID, Integer>  checkpoint      = new HashMap<>();
    // uuid → start time millis
    private final Map<UUID, Long>     startTime       = new HashMap<>();
    // uuid → last block to prevent duplicate triggers
    private final Map<UUID, Location> lastBlock       = new HashMap<>();
    // uuid → Y of last checkpoint/start (for fall detection)
    private final Map<UUID, Double>   checkpointY     = new HashMap<>();

    public ParkourListener(ParkourManager parkourManager) {
        this.parkourManager = parkourManager;
    }

    // ── Start block: player steps on it (PlayerMoveEvent, block-level change) ─

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        Player   player = event.getPlayer();
        UUID     uuid   = player.getUniqueId();

        // ── Fall detection (runs on every Y change, not just block changes) ──
        if (activeParkour.containsKey(uuid) && checkpointY.containsKey(uuid)) {
            double refY    = checkpointY.get(uuid);
            double playerY = event.getTo().getY();
            if (playerY < refY - 10) {
                teleportToLastCheckpoint(player);
                return;
            }
        }

        if (!event.hasChangedBlock()) return;
        Location to = event.getTo().getBlock().getLocation();

        // Debounce: skip if same block as last trigger
        if (to.equals(lastBlock.get(uuid))) return;
        lastBlock.put(uuid, to);

        // Goal check
        if (activeParkour.containsKey(uuid)) {
            String parkour = activeParkour.get(uuid);
            String[] cp = parkourManager.getParkourByCheckpoint(to);
            if (cp != null && cp[0].equals(parkour)) {
                int id = Integer.parseInt(cp[1]);
                int current = checkpoint.getOrDefault(uuid, -1);
                if (id > current) {
                    checkpoint.put(uuid, id);
                    checkpointY.put(uuid, to.getY());
                    player.sendMessage("§a✔ Checkpoint §e" + id + " §aerreicht!");
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
                }
                return;
            }

            String goalParkour = parkourManager.getParkourByGoal(to);
            if (goalParkour != null && goalParkour.equals(parkour)) {
                long elapsed = System.currentTimeMillis() - startTime.get(uuid);
                String time  = formatTime(elapsed);
                player.sendMessage("§6§l» §eParkour §6" + parkour + " §eabgeschlossen! §aZeit: §d" + time);
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                resetPlayer(player);
                return;
            }
        }

        // Start check
        String startParkour = parkourManager.getParkourByStart(to);
        if (startParkour != null) {
            activeParkour.put(uuid, startParkour);
            checkpoint.put(uuid, -1);
            checkpointY.put(uuid, to.getY());
            startTime.put(uuid, System.currentTimeMillis());
            player.sendMessage("§6§l» §eParkour §6" + startParkour + " §egestartet! §7Viel Erfolg!");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
        }
    }

    // ── Pressure-plate checkpoints (PlayerInteractEvent, PHYSICAL) ────────────

    @EventHandler
    public void onPressurePlate(PlayerInteractEvent event) {
        if (event.getAction() != Action.PHYSICAL) return;
        if (event.getClickedBlock() == null) return;

        Player   player = event.getPlayer();
        Location block  = event.getClickedBlock().getLocation();

        if (!activeParkour.containsKey(player.getUniqueId())) return;
        String parkour = activeParkour.get(player.getUniqueId());

        String[] cp = parkourManager.getParkourByCheckpoint(block);
        if (cp == null || !cp[0].equals(parkour)) return;

        int id      = Integer.parseInt(cp[1]);
        int current = checkpoint.getOrDefault(player.getUniqueId(), -1);
        if (id > current) {
            checkpoint.put(player.getUniqueId(), id);
            player.sendMessage("§a✔ Checkpoint §e" + id + " §aerreicht!");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
        }
    }

    // ── Public: quit parkour ───────────────────────────────────────────────────

    public boolean isInParkour(Player player) {
        return activeParkour.containsKey(player.getUniqueId());
    }

    public void quitParkour(Player player) {
        if (!activeParkour.containsKey(player.getUniqueId())) {
            player.sendMessage("§cDu bist gerade in keinem Parkour.");
            return;
        }
        String name = activeParkour.get(player.getUniqueId());
        resetPlayer(player);
        player.sendMessage("§cDu hast den Parkour §e" + name + " §cabgebrochen.");

        // Teleport back to start
        Location start = parkourManager.getStart(name);
        if (start != null) player.teleport(start);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void teleportToLastCheckpoint(Player player) {
        UUID   uuid    = player.getUniqueId();
        String parkour = activeParkour.get(uuid);
        int    cpId    = checkpoint.getOrDefault(uuid, -1);

        Location dest;
        if (cpId == -1) {
            dest = parkourManager.getStart(parkour);
        } else {
            dest = parkourManager.getCheckpointLocation(parkour, cpId);
        }

        if (dest != null) {
            player.teleport(dest);
            player.sendMessage("§c↓ §7Zurück zum letzten Checkpoint gesetzt.");
            player.playSound(dest, Sound.ENTITY_PLAYER_HURT, 0.8f, 1f);
        }

        // Reset debounce so the start/checkpoint block can fire again if needed
        lastBlock.remove(uuid);
    }

    private void resetPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        activeParkour.remove(uuid);
        checkpoint.remove(uuid);
        checkpointY.remove(uuid);
        startTime.remove(uuid);
        lastBlock.remove(uuid);
    }

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long ms      = millis % 1000;
        long min     = seconds / 60;
        long sec     = seconds % 60;
        if (min > 0) return String.format("%d:%02d.%03d", min, sec, ms);
        return String.format("%d.%03ds", sec, ms);
    }
}
