package de.haaremy.hmydailyrewards;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class DailyRewardsConfig {

    private final HmyDailyRewards plugin;
    private final TreeMap<Integer, DailyReward> definedRewards = new TreeMap<>();

    public DailyRewardsConfig(HmyDailyRewards plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        definedRewards.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("rewards");
        if (section == null) {
            plugin.getLogger().warning("Keine 'rewards' Sektion in config.yml gefunden - nutze Defaults.");
            loadDefaults();
            return;
        }

        Set<String> keys = section.getKeys(false);
        for (String key : keys) {
            int day;
            try {
                day = Integer.parseInt(key);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Ungueltiger Reward-Tag '" + key + "' in config.yml - uebersprungen.");
                continue;
            }
            ConfigurationSection r = section.getConfigurationSection(key);
            if (r == null) continue;

            DailyReward reward = parseReward(day, r);
            if (reward != null) {
                definedRewards.put(day, reward);
            }
        }

        if (definedRewards.isEmpty()) {
            loadDefaults();
        }
    }

    private void loadDefaults() {
        definedRewards.put(1, new DailyReward(1, DailyReward.Type.COINS, 100,
                "<yellow>100 Coins</yellow>", Material.GOLD_NUGGET, null, 0, null));
        definedRewards.put(7, new DailyReward(7, DailyReward.Type.COSMETIC_CRATE, 1,
                "<aqua>Kosmetik-Kiste</aqua>", Material.CHEST, null, 0, null));
        definedRewards.put(30, new DailyReward(30, DailyReward.Type.RANK_TEMP, 1,
                "<gold>1 Tag Phoenix-Rang</gold>", Material.NETHER_STAR, "phoenix", 86400L, null));
    }

    private DailyReward parseReward(int day, ConfigurationSection r) {
        String typeStr = r.getString("type", "COINS").toUpperCase();
        DailyReward.Type type;
        try {
            type = DailyReward.Type.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Unbekannter Reward-Type '" + typeStr + "' fuer Tag " + day);
            return null;
        }

        int amount = r.getInt("amount", 1);
        String description = r.getString("description", typeStr + " x" + amount);
        Material icon = parseMaterial(r.getString("icon"), null);
        String rank = r.getString("rank", null);
        long duration = r.getLong("duration", 86400L);
        Material itemMaterial = parseMaterial(r.getString("material"), null);

        return new DailyReward(day, type, amount, description, icon, rank, duration, itemMaterial);
    }

    private Material parseMaterial(String name, Material def) {
        if (name == null || name.isEmpty()) return def;
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return def;
        }
    }

    /**
     * Liefert die Belohnung fuer einen Tag (1-30). Interpoliert fehlende Tage:
     * Sucht den naechsten definierten Meilenstein und skaliert COINS linear dorthin.
     */
    public DailyReward getReward(int day) {
        if (day < 1) day = 1;
        if (day > 30) day = ((day - 1) % 30) + 1;

        DailyReward direct = definedRewards.get(day);
        if (direct != null) return direct;

        // Interpolation: finde floor und ceiling Meilensteine
        Map.Entry<Integer, DailyReward> floor = definedRewards.floorEntry(day);
        Map.Entry<Integer, DailyReward> ceil = definedRewards.ceilingEntry(day);

        if (floor == null && ceil == null) {
            // Fallback
            return new DailyReward(day, DailyReward.Type.COINS, 100 + (day * 10),
                    "<yellow>" + (100 + day * 10) + " Coins</yellow>", Material.GOLD_NUGGET, null, 0, null);
        }
        if (floor == null) {
            return ceil.getValue();
        }
        if (ceil == null) {
            ceil = floor;
        }

        DailyReward floorR = floor.getValue();
        DailyReward ceilR = ceil.getValue();

        // Wenn beide COINS sind -> linear interpolieren
        if (floorR.getType() == DailyReward.Type.COINS && ceilR.getType() == DailyReward.Type.COINS
                && !floor.getKey().equals(ceil.getKey())) {
            int df = floor.getKey();
            int dc = ceil.getKey();
            double t = (double) (day - df) / (double) (dc - df);
            int amount = (int) Math.round(floorR.getAmount() + t * (ceilR.getAmount() - floorR.getAmount()));
            // Auf naechsten 10er runden
            amount = Math.max(10, (amount / 10) * 10);
            String desc = "<yellow>" + amount + " Coins</yellow>";
            return new DailyReward(day, DailyReward.Type.COINS, amount, desc,
                    Material.GOLD_INGOT, null, 0, null);
        }

        // Sonst: floor-Reward wiederholen (aber mit aktuellem Tag markiert)
        return new DailyReward(day, floorR.getType(), floorR.getAmount(), floorR.getDescription(),
                floorR.getIcon(), floorR.getRank(), floorR.getDuration(), null);
    }

    public TreeMap<Integer, DailyReward> getDefinedRewards() {
        return definedRewards;
    }
}
