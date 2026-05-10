package de.haaremy.hmycore.placeholders;

import de.haaremy.hmycore.HmyCore;
import de.haaremy.hmycore.economy.EconomyManager;
import de.haaremy.hmycore.permissions.LuckPermsService;
import de.haaremy.hmycore.stats.PlayerStats;
import de.haaremy.hmycore.stats.StatsManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public final class HmyCorePlaceholders extends PlaceholderExpansion {

    private final HmyCore plugin;

    public HmyCorePlaceholders(HmyCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "hmycore";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Haaremy";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        String key = params.toLowerCase(Locale.ROOT);

        if (key.equals("coins")) {
            EconomyManager eco = plugin.getEconomyManager();
            if (eco == null) return "0";
            return Integer.toString(eco.getCoins(player.getUniqueId()));
        }

        if (key.equals("rank")) {
            LuckPermsService lp = plugin.getLuckPermsService();
            if (lp == null) return "default";
            return lp.getPrimaryGroup(player.getUniqueId());
        }

        if (key.equals("prefix")) {
            LuckPermsService lp = plugin.getLuckPermsService();
            if (lp == null) return "";
            return lp.getPrefix(player.getUniqueId());
        }

        if (key.equals("suffix")) {
            LuckPermsService lp = plugin.getLuckPermsService();
            if (lp == null) return "";
            return lp.getSuffix(player.getUniqueId());
        }

        StatsManager stats = plugin.getStatsManager();
        if (stats == null) return "";

        int sep = key.indexOf('_');
        if (sep <= 0 || sep >= key.length() - 1) {
            return null;
        }
        String metric = key.substring(0, sep);
        String gametype = key.substring(sep + 1);
        if (gametype.isEmpty()) return null;

        PlayerStats s = stats.getStats(player.getUniqueId(), gametype);
        switch (metric) {
            case "wins":
                return Integer.toString(s.getWins());
            case "losses":
                return Integer.toString(s.getLosses());
            case "kills":
                return Integer.toString(s.getKills());
            case "deaths":
                return Integer.toString(s.getDeaths());
            case "kd":
                return formatRatio(s.getKdr());
            case "winrate":
                return String.format(Locale.ROOT, "%.1f", s.getWinRate());
            case "playtime":
                return Long.toString(s.getPlaytimeSeconds());
            default:
                return null;
        }
    }

    private static String formatRatio(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
