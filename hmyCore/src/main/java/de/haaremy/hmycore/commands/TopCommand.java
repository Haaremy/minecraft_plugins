package de.haaremy.hmycore.commands;

import de.haaremy.hmycore.HmyCore;
import de.haaremy.hmycore.lang.Lang;
import de.haaremy.hmycore.leaderboard.LeaderboardEntry;
import de.haaremy.hmycore.leaderboard.LeaderboardManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TopCommand implements CommandExecutor, TabCompleter {

    private static final List<String> METRICS = List.of(
            "coins", "wins", "losses", "kills", "deaths", "kd", "winrate", "playtime");

    private final HmyCore plugin;

    public TopCommand(HmyCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            Lang.send(sender, "core.top.usage");
            return true;
        }

        LeaderboardManager.Metric metric = LeaderboardManager.Metric.fromString(args[0]);
        if (metric == null) {
            Lang.send(sender, "core.top.unknown_metric", "metric", args[0]);
            return true;
        }

        boolean needsGameType = !metric.isCoinMetric();
        String gameType = null;
        if (needsGameType) {
            if (args.length < 2) {
                Lang.send(sender, "core.top.gametype_required",
                        "metric", args[0].toLowerCase(Locale.ROOT));
                return true;
            }
            gameType = args[1].toLowerCase(Locale.ROOT);
        }

        int limit = LeaderboardManager.DEFAULT_LIMIT;
        int limitArgIndex = needsGameType ? 2 : 1;
        if (args.length > limitArgIndex) {
            try {
                limit = Integer.parseInt(args[limitArgIndex]);
            } catch (NumberFormatException ignored) {
                Lang.send(sender, "core.top.invalid_limit", "value", args[limitArgIndex]);
                return true;
            }
        }
        limit = LeaderboardManager.clampLimit(limit);

        LeaderboardManager mgr = plugin.getLeaderboardManager();
        if (mgr == null) {
            Lang.send(sender, "core.top.unavailable");
            return true;
        }

        List<LeaderboardEntry> cached = mgr.getTopCached(metric, gameType, limit);
        if (cached == null) {
            Lang.send(sender, "core.top.warming");
            return true;
        }
        if (cached.isEmpty()) {
            Lang.send(sender, "core.top.empty");
            return true;
        }

        String headerKey = metric.isCoinMetric()
                ? "core.top.header.coins"
                : "core.top.header.stats";
        Lang.send(sender, headerKey,
                "metric", args[0].toLowerCase(Locale.ROOT),
                "gameType", gameType == null ? "" : gameType);

        int rank = 1;
        for (LeaderboardEntry entry : cached) {
            String value = formatScore(metric, entry.getScore());
            Lang.send(sender, "core.top.entry",
                    "rank", Integer.toString(rank++),
                    "name", entry.getName(),
                    "value", value);
            if (rank > limit) break;
        }
        return true;
    }

    private static String formatScore(LeaderboardManager.Metric metric, double score) {
        switch (metric) {
            case KD:
                return String.format(Locale.ROOT, "%.2f", score);
            case WINRATE:
                return String.format(Locale.ROOT, "%.1f%%", score);
            case PLAYTIME: {
                long sec = (long) score;
                long hours = sec / 3600;
                long minutes = (sec % 3600) / 60;
                return hours + "h " + minutes + "m";
            }
            default:
                return Long.toString((long) score);
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return filter(METRICS, args[0]);
        }
        if (args.length == 2) {
            LeaderboardManager.Metric metric = LeaderboardManager.Metric.fromString(args[0]);
            if (metric == null || metric.isCoinMetric()) return List.of();
            return filter(commonGameTypes(), args[1]);
        }
        return List.of();
    }

    private static List<String> commonGameTypes() {
        return List.of("1v1", "spleef", "sumo", "tntrun", "parkour");
    }

    private static List<String> filter(List<String> source, String prefix) {
        if (prefix == null || prefix.isEmpty()) return source;
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String s : source) {
            if (s.toLowerCase(Locale.ROOT).startsWith(lower)) out.add(s);
        }
        return out;
    }

}
