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
    private final HmyConfigManager configManager;
    private final String lobbyWorldName;

    public LobbyWorldManager(HmyLobby plugin, HmyConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.lobbyWorldName = configManager.getLobbyWorld();
        applyGameRules();
    }

    private void applyGameRules() {
        World world = plugin.getServer().getWorld(lobbyWorldName);
        if (world == null) return;

        world.setGameRule(GameRule.DO_WEATHER_CYCLE,  configManager.getLobbyRule("weather",       false));
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, configManager.getLobbyRule("daylight-cycle", false));
        world.setGameRule(GameRule.DO_MOB_SPAWNING,   configManager.getLobbyRule("mob-spawning",   false));
        world.setGameRule(GameRule.FALL_DAMAGE,        configManager.getLobbyRule("fall-damage",    false));

        world.setDifficulty(Difficulty.PEACEFUL);
        world.setPVP(configManager.getLobbyRule("pvp", false));

        if (!configManager.getLobbyRule("daylight-cycle", false)) {
            world.setTime(6000);
        }
    }

    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isLobbyWorld(player)) return;

        if (!configManager.getLobbyRule("hunger", false)) {
            event.setCancelled(true);
            player.setFoodLevel(20);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isLobbyWorld(player)) return;

        EntityDamageEvent.DamageCause cause = event.getCause();

        if (cause == EntityDamageEvent.DamageCause.FALL) {
            if (!configManager.getLobbyRule("fall-damage", false)) {
                event.setCancelled(true);
            }
            return;
        }

        if (cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK || cause == EntityDamageEvent.DamageCause.PROJECTILE) {
            if (!configManager.getLobbyRule("pvp", false)) {
                event.setCancelled(true);
            }
            return;
        }

        if (!configManager.getLobbyRule("all-damage", false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        if (!event.getWorld().getName().equals(lobbyWorldName)) return;
        if (!configManager.getLobbyRule("weather", false)) {
            if (event.toWeatherState()) event.setCancelled(true);
        }
    }

    private boolean isLobbyWorld(Player player) {
        return player.getWorld().getName().equals(lobbyWorldName);
    }
}
