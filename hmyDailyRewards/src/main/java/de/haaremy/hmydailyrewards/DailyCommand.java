package de.haaremy.hmydailyrewards;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;

public class DailyCommand implements CommandExecutor, TabCompleter {

    private final HmyDailyRewards plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public DailyCommand(HmyDailyRewards plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Nur Spieler koennen diesen Befehl benutzen."));
            return true;
        }

        if (args.length == 0) {
            openGui(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        UUID uuid = player.getUniqueId();
        switch (sub) {
            case "claim" -> {
                if (!plugin.getDailyManager().canClaim(uuid)) {
                    player.sendMessage(mm.deserialize("<red>Du hast heute bereits deine Belohnung abgeholt.</red>"));
                    return true;
                }
                DailyReward reward = plugin.getDailyManager().claim(uuid, player);
                if (reward != null) {
                    player.sendMessage(mm.deserialize("<gradient:#ff6b35:#ff8c61><bold>Taegliche Belohnung erhalten!</bold></gradient>"));
                    player.sendMessage(mm.deserialize("<gray>Reward: </gray>" + (reward.getDescription() == null ? "" : reward.getDescription())));
                    int streak = plugin.getDailyManager().getStreak(uuid);
                    player.sendMessage(mm.deserialize("<yellow>Streak: " + streak + " Tage</yellow>"));
                }
            }
            case "streak" -> {
                int streak = plugin.getDailyManager().getStreak(uuid);
                int total = plugin.getDailyManager().getTotalClaims(uuid);
                player.sendMessage(mm.deserialize("<yellow>Dein aktueller Streak: <bold>" + streak + "</bold> Tage</yellow>"));
                player.sendMessage(mm.deserialize("<gray>Insgesamt geclaimt: <white>" + total + "</white></gray>"));
            }
            case "rewards" -> openRewardsOverview(player);
            default -> openGui(player);
        }
        return true;
    }

    private void openGui(Player player) {
        DailyGui gui = new DailyGui(plugin, player);
        gui.setup();
        player.openInventory(gui.getInventory());
    }

    /**
     * Uebersicht aller 30 Rewards in einem 54-Slot Chest.
     */
    private void openRewardsOverview(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54,
                mm.deserialize("<gradient:#ff6b35:#ff8c61>Reward-Uebersicht</gradient>"));
        DailyRewardsConfig cfg = plugin.getRewardsConfig();
        int currentStreak = plugin.getDailyManager().getStreak(player.getUniqueId());
        int nextDay = plugin.getDailyManager().getNextDay(player.getUniqueId());

        TreeMap<Integer, DailyReward> defined = cfg.getDefinedRewards();
        for (int day = 1; day <= 30; day++) {
            DailyReward reward = cfg.getReward(day);
            if (reward == null) continue;
            Material mat = reward.getIcon() != null ? reward.getIcon() : Material.PAPER;
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                boolean isNext = (day == nextDay);
                boolean isPast = (day < nextDay);
                String openTag = isNext ? "<green>" : (isPast ? "<gray>" : "<white>");
                String closeTag = isNext ? "</green>" : (isPast ? "</gray>" : "</white>");
                String suffix = isNext ? " <yellow>(Naechster)</yellow>" : (isPast ? " <dark_gray>(Erledigt)</dark_gray>" : "");
                meta.displayName(mm.deserialize(openTag + "<bold>Tag " + day + "</bold>" + closeTag + suffix));
                List<Component> lore = new ArrayList<>();
                if (reward.getDescription() != null && !reward.getDescription().isEmpty()) {
                    lore.add(mm.deserialize("<white>" + reward.getDescription() + "</white>"));
                }
                lore.add(mm.deserialize("<gray>Typ: " + reward.getType().name() + "</gray>"));
                if (defined.containsKey(day)) {
                    lore.add(mm.deserialize("<aqua>Meilenstein</aqua>"));
                }
                meta.lore(lore);
                item.setItemMeta(meta);
            }
            // Slot-Layout: 30 Items verteilen (5 Reihen a 6 Items, startend bei Slot 0)
            int slot = day - 1;
            if (slot < 54) {
                inv.setItem(slot, item);
            }
        }
        // Info-Block
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta imeta = info.getItemMeta();
        if (imeta != null) {
            imeta.displayName(mm.deserialize("<yellow><bold>Deine Stats</bold></yellow>"));
            List<Component> lore = Arrays.asList(
                    mm.deserialize("<white>Streak: <yellow>" + currentStreak + "</yellow></white>"),
                    mm.deserialize("<white>Naechster Tag: <yellow>" + nextDay + "</yellow></white>"),
                    mm.deserialize("<white>Gesamt: <yellow>" + plugin.getDailyManager().getTotalClaims(player.getUniqueId()) + "</yellow></white>")
            );
            imeta.lore(lore);
            info.setItemMeta(imeta);
        }
        inv.setItem(49, info);
        player.openInventory(inv);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("claim", "streak", "rewards");
        }
        return List.of();
    }
}
