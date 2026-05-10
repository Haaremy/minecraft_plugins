package de.haaremy.hmycore.commands;

import de.haaremy.hmycore.HmyCore;
import de.haaremy.hmycore.lang.Lang;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CoinsCommand implements CommandExecutor {

    private final HmyCore plugin;

    public CoinsCommand(HmyCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            Lang.send(sender, "core.player_only");
            return true;
        }

        int coins = plugin.getEconomyManager().getCoins(player.getUniqueId());
        Lang.send(player, "core.coins.show", "coins", String.valueOf(coins));

        return true;
    }
}
