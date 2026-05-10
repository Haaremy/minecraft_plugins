package de.haaremy.hmynavigator;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

public class NavigatorConfig {

    private final Map<String, String> serverMappings = new HashMap<>();
    private int compassSlot;

    public NavigatorConfig(HmyNavigator plugin) {
        reload(plugin);
    }

    public void reload(HmyNavigator plugin) {
        FileConfiguration config = plugin.getConfig();
        serverMappings.clear();

        if (config.isConfigurationSection("servers")) {
            for (String key : config.getConfigurationSection("servers").getKeys(false)) {
                serverMappings.put(key.toLowerCase(), config.getString("servers." + key));
            }
        }

        compassSlot = config.getInt("compass-slot", 4);
    }

    public String getServer(String gameMode) {
        return serverMappings.get(gameMode.toLowerCase());
    }

    public Map<String, String> getServerMappings() {
        return serverMappings;
    }

    public int getCompassSlot() {
        return compassSlot;
    }
}
