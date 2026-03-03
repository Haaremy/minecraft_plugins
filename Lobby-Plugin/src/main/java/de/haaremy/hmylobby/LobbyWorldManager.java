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
        if (world == null) return;

        // Config Werte laden
        boolean weather       = plugin.getConfig().getBoolean("Lobby.Rules.weather", false);
        boolean daylightCycle = plugin.getConfig().getBoolean("Lobby.Rules.daylight-cycle", false);
        boolean mobSpawning   = plugin.getConfig().getBoolean("Lobby.Rules.mob-spawning", false);
        
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, weather);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, daylightCycle);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, mobSpawning);
        world.setGameRule(GameRule.FALL_DAMAGE, plugin.getConfig().getBoolean("Lobby.Rules.fall-damage", false));
        
        world.setDifficulty(Difficulty.PEACEFUL);
        world.setPVP(plugin.getConfig().getBoolean("Lobby.Rules.pvp", false));

        if (!daylightCycle) {
            world.setTime(6000);
        }
    }

    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isLobbyWorld(player)) return;

        // Wenn Hunger in der Config FALSE ist -> Event abbrechen
        if (!plugin.getConfig().getBoolean("Lobby.Rules.hunger", false)) {
            event.setCancelled(true);
            player.setFoodLevel(20);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isLobbyWorld(player)) return;

        EntityDamageEvent.DamageCause cause = event.getCause();

        // 1. Fallschaden prüfen
        if (cause == EntityDamageEvent.DamageCause.FALL) {
            if (!plugin.getConfig().getBoolean("Lobby.Rules.fall-damage", false)) {
                event.setCancelled(true);
            }
            return;
        }

        // 2. PvP prüfen
        if (cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK || cause == EntityDamageEvent.DamageCause.PROJECTILE) {
            if (!plugin.getConfig().getBoolean("Lobby.Rules.pvp", false)) {
                event.setCancelled(true);
            }
            return;
        }

        // 3. ALLE ANDEREN SCHADENSARTEN (Lava, Feuer, Ertrinken, Erstickung, etc.)
        // Wenn du in der Lobby generell keinen Schaden willst, brechen wir hier alles ab.
        if (!plugin.getConfig().getBoolean("Lobby.Rules.all-damage", false)) {
            event.setCancelled(true);
        }
    }

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