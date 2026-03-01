package de.haaremy.hmylobby;

import de.haaremy.hmylobby.HmyLobby;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public class ServerSelectorConfig {

    public record SelectorEntry(int slot, Material material, String name, List<String> lore, String server) {}

    private final List<SelectorEntry> entries = new ArrayList<>();

    public ServerSelectorConfig(HmyLobby plugin) {
        load(plugin);
    }

    private void load(HmyLobby plugin) {
        entries.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("ServerSelector.entries");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) continue;

            int x = entry.getInt("x", 0);
            int y = entry.getInt("y", 0);
            int slot = y * 9 + x;

            String blocktype = entry.getString("blocktype", "STONE").toUpperCase();
            Material material;
            try {
                material = Material.valueOf(blocktype);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Unbekannter Blocktype '" + blocktype + "' für Entry '" + key + "'");
                continue;
            }

            String name = entry.getString("name", "§7" + key);
            List<String> lore = entry.getStringList("lore");
            String server = entry.getString("server", key);

            entries.add(new SelectorEntry(slot, material, name, lore, server));
        }

        plugin.getLogger().info("ServerSelector: " + entries.size() + " Einträge geladen.");
    }

    public List<SelectorEntry> getEntries() {
        return entries;
    }
    
    
}
