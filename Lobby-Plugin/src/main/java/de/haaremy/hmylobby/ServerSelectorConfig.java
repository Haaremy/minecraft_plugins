package de.haaremy.hmylobby;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ServerSelectorConfig {

    public record SelectorEntry(int slot, Material material, String name, List<String> lore, String server) {}

    private final List<SelectorEntry> entries = new ArrayList<>();

    public ServerSelectorConfig(HmyConfigManager configManager, Logger logger) {
        load(configManager, logger);
    }

    private void load(HmyConfigManager configManager, Logger logger) {
        entries.clear();
        ConfigurationSection section = configManager.getServerSelectorSection();
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) continue;

            int x    = entry.getInt("x", 0);
            int y    = entry.getInt("y", 0);
            int slot = y * 9 + x;

            String blocktype = entry.getString("blocktype", "STONE").toUpperCase();
            Material material;
            try {
                material = Material.valueOf(blocktype);
            } catch (IllegalArgumentException e) {
                logger.warning("Unbekannter Blocktype '" + blocktype + "' für Entry '" + key + "'");
                continue;
            }

            String       name   = entry.getString("name", "§7" + key);
            List<String> lore   = entry.getStringList("lore");
            String       server = entry.getString("server", key);

            entries.add(new SelectorEntry(slot, material, name, lore, server));
        }

        logger.info("ServerSelector: " + entries.size() + " Einträge geladen.");
    }

    public List<SelectorEntry> getEntries() {
        return entries;
    }
}
