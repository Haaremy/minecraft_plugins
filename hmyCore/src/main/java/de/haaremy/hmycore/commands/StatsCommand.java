package de.haaremy.hmycore.commands;

import de.haaremy.hmycore.HmyCore;
import de.haaremy.hmycore.lang.Lang;
import de.haaremy.hmycore.stats.PlayerStats;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class StatsCommand implements CommandExecutor {

    private final HmyCore plugin;

    public StatsCommand(HmyCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            Lang.send(sender, "core.player_only");
            return true;
        }

        String gameType = "default";
        if (args.length > 0) {
            gameType = args[0];
        }

        PlayerStats stats = plugin.getStatsManager().getStats(player.getUniqueId(), gameType);

        Lang.send(player, "core.stats.divider");
        Lang.send(player, "core.stats.title", "gameType", gameType);
        player.sendMessage(Component.empty());
        Lang.send(player, "core.stats.wins", "wins", String.valueOf(stats.getWins()));
        Lang.send(player, "core.stats.losses", "losses", String.valueOf(stats.getLosses()));
        Lang.send(player, "core.stats.winrate", "rate", String.format("%.1f", stats.getWinRate()));
        player.sendMessage(Component.empty());
        Lang.send(player, "core.stats.kills", "kills", String.valueOf(stats.getKills()));
        Lang.send(player, "core.stats.deaths", "deaths", String.valueOf(stats.getDeaths()));
        Lang.send(player, "core.stats.kd", "kd", String.format("%.2f", stats.getKdr()));
        player.sendMessage(Component.empty());

        long playtime = stats.getPlaytimeSeconds();
        long hours = playtime / 3600;
        long minutes = (playtime % 3600) / 60;
        Lang.send(player, "core.stats.playtime",
                "hours", String.valueOf(hours),
                "minutes", String.valueOf(minutes));
        Lang.send(player, "core.stats.divider");

        return true;
    }
}
