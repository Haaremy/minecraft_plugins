package de.haaremy.hmynavigator;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NavigatorGui {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final Component GUI_TITLE = MINI.deserialize("<gradient:#ff6b35:#ff8c61>Spielmodi</gradient>");
    private static final int GUI_SIZE = 27;

    private final NavigatorConfig config;
    private final Map<Integer, String> slotToGameMode = new HashMap<>();

    public NavigatorGui(NavigatorConfig config) {
        this.config = config;

        // Slot-zu-Spielmodus Mapping
        slotToGameMode.put(10, "sumo");
        slotToGameMode.put(11, "1v1");
        slotToGameMode.put(12, "parkour");
        slotToGameMode.put(13, "tntrun");
        slotToGameMode.put(14, "spleef");
        slotToGameMode.put(15, "ai");
        slotToGameMode.put(16, "survival");
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE);

        // Rand mit Glas füllen
        ItemStack glass = createGlassPane();
        for (int i = 0; i < GUI_SIZE; i++) {
            inv.setItem(i, glass);
        }

        // Spielmodi-Items setzen
        inv.setItem(10, createGameItem(
                Material.GOLDEN_BOOTS,
                "<gradient:#ff6b35:#ff8c61>Sumo</gradient>",
                "<gray>1v1 Knockback-Kampf</gray>",
                "<gray>Wer fällt verliert!</gray>"
        ));

        inv.setItem(11, createGameItem(
                Material.DIAMOND_SWORD,
                "<gradient:#ff6b35:#ff8c61>1v1 Duell</gradient>",
                "<gray>Klassische Duelle</gray>",
                "<gray>Wähle dein Kit!</gray>"
        ));

        inv.setItem(12, createGameItem(
                Material.LEATHER_BOOTS,
                "<gradient:#ff6b35:#ff8c61>Parkour</gradient>",
                "<gray>Springe durch Hindernisse</gray>",
                "<gray>Bestzeiten-Jagd!</gray>"
        ));

        inv.setItem(13, createGameItem(
                Material.TNT,
                "<gradient:#ff6b35:#ff8c61>TNT Run</gradient>",
                "<gray>Boden verschwindet!</gray>",
                "<gray>Letzter Überlebender gewinnt</gray>"
        ));

        inv.setItem(14, createGameItem(
                Material.DIAMOND_SHOVEL,
                "<gradient:#ff6b35:#ff8c61>Spleef</gradient>",
                "<gray>Zerstöre den Boden!</gray>",
                "<gray>Schnee-Schlacht</gray>"
        ));

        inv.setItem(15, createGameItem(
                Material.END_CRYSTAL,
                "<gradient:#00ffcc:#0088ff>AI Lab</gradient>",
                "<gray>Experimenteller hmyCubed-Shard</gray>",
                "<gray>KI-Features & neue Spielideen</gray>"
        ));

        inv.setItem(16, createGameItem(
                Material.GRASS_BLOCK,
                "<gradient:#ff6b35:#ff8c61>Survival</gradient>",
                "<gray>Klassisches Survival</gray>",
                "<gray>Bauen & Erkunden</gray>"
        ));

        player.openInventory(inv);
    }

    public boolean isNavigatorInventory(InventoryView view) {
        Component title = view.title();
        return GUI_TITLE.equals(title);
    }

    public String getGameMode(int slot) {
        return slotToGameMode.get(slot);
    }

    private ItemStack createGlassPane() {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.displayName(Component.empty());
        glass.setItemMeta(meta);
        return glass;
    }

    @SuppressWarnings("deprecation")
    private ItemStack createGameItem(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(MINI.deserialize(name));
        meta.lore(java.util.Arrays.stream(loreLines)
                .map(MINI::deserialize)
                .collect(java.util.stream.Collectors.toList()));

        // Enchantment Glint
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        item.setItemMeta(meta);
        return item;
    }
}
