package de.haaremy.hmylobby;

import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

public class LobbyWorldManager implements Listener {

    private final HmyLobby plugin;
    private final String lobbyWorldName;

    public LobbyWorldManager(HmyLobby plugin) {
        this.plugin = plugin;
        this.lobbyWorldName = plugin.getConfig().getString("Lobby.world", "world");
        applyGameRules();
    }

    private void applyGameRules() {
        World world = plugin.getServer().getWorld(lobbyWorldName);
        if (world == null) {
            plugin.getLogger().warning("Lobby-Welt '" + lobbyWorldName + "' nicht gefunden!");
            return;
        }

        boolean weather       = plugin.getConfig().getBoolean("Lobby.Rules.weather", false);
        boolean daylightCycle = plugin.getConfig().getBoolean("Lobby.Rules.daylight-cycle", false);
        boolean mobSpawning   = plugin.getConfig().getBoolean("Lobby.Rules.mob-spawning", false);
        boolean fireTick      = plugin.getConfig().getBoolean("Lobby.Rules.fire-tick", false);
        boolean pvp           = plugin.getConfig().getBoolean("Lobby.Rules.pvp", false);
        boolean hunger        = plugin.getConfig().getBoolean("Lobby.Rules.hunger", false);

        // GameRules setzen
        world.setGameRule(GameRule.DO_WEATHER_CYCLE,        weather);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE,       daylightCycle);
        world.setGameRule(GameRule.DO_MOB_SPAWNING,         mobSpawning);
        world.setGameRule(GameRule.DO_FIRE_TICK,            fireTick);
        world.setGameRule(GameRule.MOB_GRIEFING,            false);
        world.setGameRule(GameRule.KEEP_INVENTORY,          true);
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS,   false);
        world.setGameRule(GameRule.SHOW_DEATH_MESSAGES,     false);
        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN,    true);
        world.setGameRule(GameRule.NATURAL_REGENERATION,    true);

        // PvP & Schwierigkeit
        world.setPVP(pvp);
        world.setDifficulty(Difficulty.PEACEFUL);

        // Wetter & Zeit fixieren
        if (!weather) {
            world.setStorm(false);
            world.setThundering(false);
            world.setWeatherDuration(Integer.MAX_VALUE);
        }
        if (!daylightCycle) {
            world.setTime(6000); // Mittag
            world.setFullTime(6000);
        }

        plugin.getLogger().info("Lobby-Welt '" + lobbyWorldName + "' Regeln angewendet.");
    }

    // Hunger deaktivieren
    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isLobbyWorld(player)) return;
        if (!plugin.getConfig().getBoolean("Lobby.Rules.hunger", false)) {
            event.setCancelled(true);
            player.setFoodLevel(20);
        }
    }

    // Fall- & sonstigen Schaden deaktivieren
    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isLobbyWorld(player)) return;

        boolean fallDamage = plugin.getConfig().getBoolean("Lobby.Rules.fall-damage", false);

        if (!fallDamage && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
            return;
        }

        // PvP wird über world.setPVP() geregelt, aber zur Sicherheit:
        boolean pvp = plugin.getConfig().getBoolean("Lobby.Rules.pvp", false);
        if (!pvp && event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            event.setCancelled(true);
        }
    }

    // Wetter-Änderung verhindern
    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        if (!event.getWorld().getName().equals(lobbyWorldName)) return;
        if (!plugin.getConfig().getBoolean("Lobby.Rules.weather", false)) {
            if (event.toWeatherState()) event.setCancelled(true);
        }
    }

    private boolean isLobbyWorld(Player player) {
        return player.getWorld().getName().equals(lobbyWorldName);
    }
}
