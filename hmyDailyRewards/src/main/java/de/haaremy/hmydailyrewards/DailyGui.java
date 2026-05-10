package de.haaremy.hmydailyrewards;

import de.haaremy.hmycore.gui.HmyGui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DailyGui extends HmyGui {

    private static final int CLAIM_SLOT = 22;
    private static final int TODAY_SLOT = 10;
    private static final int[] PREVIEW_SLOTS = {12, 13, 14, 15, 16};

    private final HmyDailyRewards plugin;
    private final Player player;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public DailyGui(HmyDailyRewards plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        createInventory(mm.deserialize("<gradient:#ff6b35:#ff8c61>Taegliche Belohnung</gradient>"), 3);
    }

    @Override
    public void setup() {
        fillBorders();

        UUID uuid = player.getUniqueId();
        DailyManager manager = plugin.getDailyManager();
        DailyRewardsConfig cfg = plugin.getRewardsConfig();

        boolean canClaim = manager.canClaim(uuid);
        int streak = manager.getStreak(uuid);
        int nextDay = manager.getNextDay(uuid); // 1-30 des naechsten Claims

        // Heutiger Reward (Slot 10)
        DailyReward todayReward = cfg.getReward(nextDay);
        if (todayReward != null) {
            ItemStack todayItem = buildRewardItem(todayReward, "<gold><bold>Heute - Tag " + nextDay + "</bold></gold>",
                    streak, true, canClaim);
            getInventory().setItem(TODAY_SLOT, todayItem);
        }

        // Vorschau der naechsten 5 Tage (nach dem heutigen)
        for (int i = 0; i < PREVIEW_SLOTS.length; i++) {
            int previewDay = nextDay + i + 1;
            if (previewDay > 30) previewDay = ((previewDay - 1) % 30) + 1;
            DailyReward previewReward = cfg.getReward(previewDay);
            if (previewReward != null) {
                ItemStack preview = buildRewardItem(previewReward,
                        "<gray>Tag " + previewDay + "</gray>", streak, false, false);
                getInventory().setItem(PREVIEW_SLOTS[i], preview);
            }
        }

        // Claim-Button Slot 22
        ItemStack claimButton = new ItemStack(canClaim ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK);
        ItemMeta meta = claimButton.getItemMeta();
        if (meta != null) {
            if (canClaim) {
                meta.displayName(mm.deserialize("<green><bold>BELOHNUNG ABHOLEN</bold></green>"));
                List<Component> lore = new ArrayList<>();
                lore.add(mm.deserialize("<gray>Klicke um deine Belohnung</gray>"));
                lore.add(mm.deserialize("<gray>fuer heute zu erhalten!</gray>"));
                meta.lore(lore);
            } else {
                meta.displayName(mm.deserialize("<red><bold>BEREITS ABGEHOLT</bold></red>"));
                List<Component> lore = new ArrayList<>();
                lore.add(mm.deserialize("<gray>Komm morgen wieder, um</gray>"));
                lore.add(mm.deserialize("<gray>deinen Streak fortzusetzen!</gray>"));
                lore.add(Component.empty());
                lore.add(mm.deserialize("<yellow>Dein Streak: " + streak + " Tage</yellow>"));
                meta.lore(lore);
            }
            claimButton.setItemMeta(meta);
        }
        getInventory().setItem(CLAIM_SLOT, claimButton);

        if (canClaim) {
            setClickHandler(CLAIM_SLOT, event -> {
                Player p = (Player) event.getWhoClicked();
                DailyReward received = plugin.getDailyManager().claim(p.getUniqueId(), p);
                p.closeInventory();
                if (received != null) {
                    p.sendMessage(mm.deserialize("<gradient:#ff6b35:#ff8c61><bold>Taegliche Belohnung erhalten!</bold></gradient>"));
                    p.sendMessage(mm.deserialize("<gray>Reward: </gray>" + (received.getDescription() == null ? "" : received.getDescription())));
                    int newStreak = plugin.getDailyManager().getStreak(p.getUniqueId());
                    p.sendMessage(mm.deserialize("<yellow>Streak: " + newStreak + " Tage</yellow>"));
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                } else {
                    p.sendMessage(mm.deserialize("<red>Du hast heute bereits deine Belohnung abgeholt.</red>"));
                }
            });
        }
    }

    private ItemStack buildRewardItem(DailyReward reward, String titleMM, int streak, boolean isToday, boolean canClaim) {
        Material mat = reward.getIcon() != null ? reward.getIcon() : Material.PAPER;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(mm.deserialize(titleMM));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            if (reward.getDescription() != null && !reward.getDescription().isEmpty()) {
                lore.add(mm.deserialize("<white>Belohnung: </white>" + reward.getDescription()));
            } else {
                lore.add(mm.deserialize("<white>Typ: </white><gray>" + reward.getType().name() + "</gray>"));
                lore.add(mm.deserialize("<white>Menge: </white><gray>" + reward.getAmount() + "</gray>"));
            }
            lore.add(Component.empty());
            lore.add(mm.deserialize("<yellow>Dein Streak: " + streak + " Tage</yellow>"));
            if (isToday) {
                lore.add(Component.empty());
                if (canClaim) {
                    lore.add(mm.deserialize("<green>Bereit zur Abholung!</green>"));
                } else {
                    lore.add(mm.deserialize("<red>Bereits abgeholt.</red>"));
                }
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void fillBorders() {
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            filler.setItemMeta(meta);
        }
        int[] borderSlots = {0,1,2,3,4,5,6,7,8, 9,17, 18,19,20,21,23,24,25,26};
        for (int s : borderSlots) {
            getInventory().setItem(s, filler);
        }
    }
}
