package de.haaremy.hmylobby;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EffectManager {
    private final Map<UUID, Particle> activeParticles = new HashMap<>();

    public EffectManager(HmyLobby plugin) {
        // Task, der alle 2 Ticks Partikel bei allen aktiven Spielern spawnt
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Map.Entry<UUID, Particle> entry : activeParticles.entrySet()) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null && player.isOnline()) {
                    player.getWorld().spawnParticle(entry.getValue(), player.getLocation().add(0, 0.2, 0), 3, 0.2, 0.1, 0.2, 0.02);
                }
            }
        }, 20L, 2L);
    }

    public void setParticle(Player player, Particle particle) {
        if (particle == null) activeParticles.remove(player.getUniqueId());
        else activeParticles.put(player.getUniqueId(), particle);
    }

    public void remove(Player player) { activeParticles.remove(player.getUniqueId()); }
}