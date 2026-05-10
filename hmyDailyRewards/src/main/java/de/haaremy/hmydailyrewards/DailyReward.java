package de.haaremy.hmydailyrewards;

import de.haaremy.hmycore.HmyCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DailyReward {

    public enum Type {
        COINS,
        COSMETIC_CRATE,
        RANK_TEMP,
        ITEM
    }

    private final int day;
    private final Type type;
    private final int amount;
    private final String description;
    private final Material icon;
    private final String rank;        // nur fuer RANK_TEMP
    private final long duration;      // Sekunden, nur fuer RANK_TEMP
    private final Material itemMaterial; // nur fuer ITEM

    public DailyReward(int day, Type type, int amount, String description, Material icon,
                       String rank, long duration, Material itemMaterial) {
        this.day = day;
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.icon = icon != null ? icon : defaultIcon(type);
        this.rank = rank;
        this.duration = duration;
        this.itemMaterial = itemMaterial;
    }

    private static Material defaultIcon(Type type) {
        if (type == null) return Material.PAPER;
        return switch (type) {
            case COINS -> Material.GOLD_INGOT;
            case COSMETIC_CRATE -> Material.CHEST;
            case RANK_TEMP -> Material.NETHER_STAR;
            case ITEM -> Material.ITEM_FRAME;
        };
    }

    public int getDay() {
        return day;
    }

    public Type getType() {
        return type;
    }

    public int getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public Material getIcon() {
        return icon;
    }

    public String getRank() {
        return rank;
    }

    public long getDuration() {
        return duration;
    }

    /**
     * Gibt den Reward an den Spieler.
     */
    public void give(Player player) {
        switch (type) {
            case COINS -> giveCoins(player);
            case COSMETIC_CRATE -> giveCrate(player);
            case RANK_TEMP -> giveRank(player);
            case ITEM -> giveItem(player);
        }
    }

    private void giveCoins(Player player) {
        try {
            HmyCore core = HmyCore.getInstance();
            if (core != null && core.getEconomyManager() != null) {
                core.getEconomyManager().addCoins(player.getUniqueId(), amount);
            } else {
                player.sendMessage(Component.text("Economy nicht verfuegbar."));
            }
        } catch (Throwable t) {
            player.sendMessage(Component.text("Fehler beim Coins-Reward: " + t.getMessage()));
        }
    }

    private void giveCrate(Player player) {
        NamespacedKey key = new NamespacedKey("hmy", "crate.cosmetic");
        ItemStack crate = new ItemStack(Material.CHEST, Math.max(1, amount));
        ItemMeta meta = crate.getItemMeta();
        if (meta != null) {
            MiniMessage mm = MiniMessage.miniMessage();
            meta.displayName(mm.deserialize("<aqua>Kosmetik-Kiste</aqua>"));
            List<Component> lore = new ArrayList<>();
            lore.add(mm.deserialize("<gray>Rechtsklick zum Oeffnen</gray>"));
            meta.lore(lore);
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, "cosmetic");
            crate.setItemMeta(meta);
        }
        var leftover = player.getInventory().addItem(crate);
        if (!leftover.isEmpty()) {
            leftover.values().forEach(it -> player.getWorld().dropItemNaturally(player.getLocation(), it));
        }
    }

    private void giveRank(Player player) {
        if (rank == null || rank.isEmpty()) {
            player.sendMessage(Component.text("Ungueltiger Rang-Reward."));
            return;
        }
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
            player.sendMessage(Component.text("LuckPerms nicht installiert - Rang-Reward nicht moeglich."));
            return;
        }
        try {
            var api = LuckPermsProvider.get();
            api.getUserManager().modifyUser(player.getUniqueId(), (User user) -> {
                long seconds = duration > 0 ? duration : TimeUnit.DAYS.toSeconds(1);
                Node node = InheritanceNode.builder(rank)
                        .expiry(seconds, TimeUnit.SECONDS)
                        .build();
                user.data().add(node);
            });
        } catch (Throwable t) {
            player.sendMessage(Component.text("Fehler beim Rang-Reward: " + t.getMessage()));
        }
    }

    private void giveItem(Player player) {
        Material mat = itemMaterial != null ? itemMaterial : Material.DIAMOND;
        ItemStack item = new ItemStack(mat, Math.max(1, amount));
        var leftover = player.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            leftover.values().forEach(it -> player.getWorld().dropItemNaturally(player.getLocation(), it));
        }
    }
}
