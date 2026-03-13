package de.haaremy.hmyvelocityplugin.economy;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;

import java.util.List;

/**
 * /hmy coins – zeigt den aktuellen Kontostand (hmyCoins + hmyShards).
 * Auch: /hmy coins give <player> <amount> für Admins.
 */
public class ComCoins implements SimpleCommand {

    private final CurrencyManager currency;

    public ComCoins(CurrencyManager currency) {
        this.currency = currency;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!(source instanceof Player player)) {
            source.sendMessage(Component.text("§cNur Spieler."));
            return;
        }

        String[] args = invocation.arguments();

        // /hmy coins give <player> <amount>  (admin)
        if (args.length >= 2 && args[0].equalsIgnoreCase("give")) {
            if (!player.hasPermission("hmy.coins.give")) {
                player.sendMessage(Component.text("§cKeine Berechtigung."));
                return;
            }
            if (args.length < 3) {
                player.sendMessage(Component.text("§cVerwendung: §e/hmy coins give <Spieler> <Menge>"));
                return;
            }
            long amount;
            try { amount = Long.parseLong(args[2]); }
            catch (NumberFormatException e) { player.sendMessage(Component.text("§cUngültige Menge.")); return; }

            // Give to self or another player by name
            currency.addCoins(player.getUniqueId(), amount);
            player.sendMessage(Component.text("§a+" + amount + " §6hmyCoins §7gutgeschrieben."));
            return;
        }

        // /hmy coins – show balance
        long coins  = currency.getCoins(player.getUniqueId());
        long shards = currency.getShards(player.getUniqueId());

        player.sendMessage(Component.text("§8§m────────────────────────"));
        player.sendMessage(Component.text("§6§l Dein Kontostand"));
        player.sendMessage(Component.text("§8§m────────────────────────"));
        player.sendMessage(Component.text("  §6⬡ hmyCoins:  §e" + coins));
        player.sendMessage(Component.text("  §b◆ hmyShards: §3" + shards));
        player.sendMessage(Component.text("§8§m────────────────────────"));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 1) return List.of("give");
        return List.of();
    }
}
