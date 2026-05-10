package de.haaremy.hmy1v1;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public enum DuelKit {

    IRON(
            "<white>Iron",
            Material.IRON_CHESTPLATE,
            "<gray>Eisen-Ruestung, Eisenschwert",
            "<gray>16 goldene Aepfel"
    ),
    DIAMOND(
            "<aqua>Diamond",
            Material.DIAMOND_CHESTPLATE,
            "<gray>Diamant-Ruestung, Diamantschwert",
            "<gray>8 goldene Aepfel"
    ),
    ARCHER(
            "<green>Archer",
            Material.BOW,
            "<gray>Leder-Ruestung, Bogen (Staerke 1, Unendlichkeit)",
            "<gray>1 Pfeil, Steinschwert"
    ),
    SOUP(
            "<red>Soup",
            Material.MUSHROOM_STEW,
            "<gray>Eisen-Ruestung, Diamantschwert",
            "<gray>32 Pilzsuppen (Rechtsklick heilt)"
    ),
    CLASSIC(
            "<gold>Classic",
            Material.DIAMOND_SWORD,
            "<gray>Diamant-Ruestung (Schutz 1), Diamantschwert (Schaerfe 1)",
            "<gray>Bogen, 16 Pfeile, 5 goldene Aepfel"
    );

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final String displayName;
    private final Material icon;
    private final String[] loreLines;

    DuelKit(String displayName, Material icon, String... loreLines) {
        this.displayName = displayName;
        this.icon = icon;
        this.loreLines = loreLines;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public List<Component> getLore() {
        return Arrays.stream(loreLines).map(MINI::deserialize).toList();
    }

    public ItemStack getIconItem() {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MINI.deserialize(displayName));
        meta.lore(getLore());
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack[] getArmor() {
        return switch (this) {
            case IRON, SOUP -> new ItemStack[]{
                    new ItemStack(Material.IRON_BOOTS),
                    new ItemStack(Material.IRON_LEGGINGS),
                    new ItemStack(Material.IRON_CHESTPLATE),
                    new ItemStack(Material.IRON_HELMET)
            };
            case DIAMOND -> new ItemStack[]{
                    new ItemStack(Material.DIAMOND_BOOTS),
                    new ItemStack(Material.DIAMOND_LEGGINGS),
                    new ItemStack(Material.DIAMOND_CHESTPLATE),
                    new ItemStack(Material.DIAMOND_HELMET)
            };
            case ARCHER -> new ItemStack[]{
                    new ItemStack(Material.LEATHER_BOOTS),
                    new ItemStack(Material.LEATHER_LEGGINGS),
                    new ItemStack(Material.LEATHER_CHESTPLATE),
                    new ItemStack(Material.LEATHER_HELMET)
            };
            case CLASSIC -> {
                ItemStack boots = new ItemStack(Material.DIAMOND_BOOTS);
                boots.addEnchantment(Enchantment.PROTECTION, 1);
                ItemStack leggings = new ItemStack(Material.DIAMOND_LEGGINGS);
                leggings.addEnchantment(Enchantment.PROTECTION, 1);
                ItemStack chestplate = new ItemStack(Material.DIAMOND_CHESTPLATE);
                chestplate.addEnchantment(Enchantment.PROTECTION, 1);
                ItemStack helmet = new ItemStack(Material.DIAMOND_HELMET);
                helmet.addEnchantment(Enchantment.PROTECTION, 1);
                yield new ItemStack[]{boots, leggings, chestplate, helmet};
            }
        };
    }

    public ItemStack[] getItems() {
        return switch (this) {
            case IRON -> new ItemStack[]{
                    new ItemStack(Material.IRON_SWORD),
                    new ItemStack(Material.GOLDEN_APPLE, 16)
            };
            case DIAMOND -> new ItemStack[]{
                    new ItemStack(Material.DIAMOND_SWORD),
                    new ItemStack(Material.GOLDEN_APPLE, 8)
            };
            case ARCHER -> {
                ItemStack bow = new ItemStack(Material.BOW);
                bow.addEnchantment(Enchantment.POWER, 1);
                bow.addEnchantment(Enchantment.INFINITY, 1);
                yield new ItemStack[]{
                        new ItemStack(Material.STONE_SWORD),
                        bow,
                        new ItemStack(Material.ARROW, 1)
                };
            }
            case SOUP -> {
                ItemStack[] items = new ItemStack[33];
                items[0] = new ItemStack(Material.DIAMOND_SWORD);
                for (int i = 1; i <= 32; i++) {
                    items[i] = new ItemStack(Material.MUSHROOM_STEW);
                }
                yield items;
            }
            case CLASSIC -> {
                ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
                sword.addEnchantment(Enchantment.SHARPNESS, 1);
                yield new ItemStack[]{
                        sword,
                        new ItemStack(Material.BOW),
                        new ItemStack(Material.ARROW, 16),
                        new ItemStack(Material.GOLDEN_APPLE, 5)
                };
            }
        };
    }

    public void apply(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(getArmor());
        for (ItemStack item : getItems()) {
            if (item != null) {
                player.getInventory().addItem(item);
            }
        }
        player.updateInventory();
    }
}
