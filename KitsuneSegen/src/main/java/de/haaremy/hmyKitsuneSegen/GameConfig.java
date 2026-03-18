package de.haaremy.hmykitsunesegen;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Typed wrapper around the plugin's config.yml.
 */
public class GameConfig {

    private final FileConfiguration cfg;

    public GameConfig(FileConfiguration cfg) {
        this.cfg = cfg;
    }

    // ── World names ────────────────────────────────────────────────────────────

    public String getHubWorld() {
        return cfg.getString("hub-world", "hub");
    }

    public String getGameWorld() {
        return cfg.getString("game-world", "game");
    }

    public String getLobbyServer() {
        return cfg.getString("lobby-server", "lobby");
    }

    public String getWorldBackupPath() {
        return cfg.getString("world-backup-path", "world_backups/game");
    }

    // ── Player settings ────────────────────────────────────────────────────────

    public int getMinPlayers() {
        return cfg.getInt("min-players", 2);
    }

    public int getMaxPlayers() {
        return cfg.getInt("max-players", 20);
    }

    public double getMaxHealth() {
        return cfg.getDouble("max-health", 40.0);
    }

    // ── Game timing ────────────────────────────────────────────────────────────

    public int getCountdownSeconds() {
        return cfg.getInt("countdown-seconds", 60);
    }

    public int getGameStartDelay() {
        return cfg.getInt("game-start-delay", 5);
    }

    // ── Spawn mode ─────────────────────────────────────────────────────────────

    /** "random" (spawn on obsidian blocks) or "flight" (elytra from height). */
    public String getSpawnMode() {
        return cfg.getString("spawn-mode", "random");
    }

    public int getElytraHeight() {
        return cfg.getInt("elytra-height", 100);
    }

    // ── Block protection ───────────────────────────────────────────────────────

    public Set<Material> getBreakableBlocks() {
        Set<Material> result = new HashSet<>();
        List<String> list = cfg.getStringList("breakable-blocks");
        for (String name : list) {
            Material m = Material.matchMaterial(name);
            if (m != null) result.add(m);
        }
        return result;
    }

    public Material getSpawnBlock() {
        Material m = Material.matchMaterial(cfg.getString("spawn-block", "OBSIDIAN"));
        return m != null ? m : Material.OBSIDIAN;
    }

    public Material getChestSpawnBlock() {
        Material m = Material.matchMaterial(cfg.getString("chest-spawn-block", "OAK_PLANKS"));
        return m != null ? m : Material.OAK_PLANKS;
    }
}
