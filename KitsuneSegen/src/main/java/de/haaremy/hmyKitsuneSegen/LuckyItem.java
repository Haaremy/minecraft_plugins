package de.haaremy.hmykitsunesegen;

import java.util.List;
import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;


public class LuckyItem implements Listener {

    private final HmyKitsuneSegen plugin;

    public LuckyItem(HmyKitsuneSegen plugin) {
        this.plugin = plugin;
    }

     // Kategorien
    private static final String[] CATEGORIES = {"Multishot", "Speedshot", "Distanceshot", "Precisionshot"};
    
    // Raritäten mit zugehörigen Farben
    private static final ChatColor[] RARITY_COLORS = {
            ChatColor.GRAY,    // Gewöhnlich
            ChatColor.GREEN,   // Ungewöhnlich
            ChatColor.BLUE,    // Selten
            ChatColor.DARK_PURPLE, // Episch
            ChatColor.GOLD     // Legendär
    };

     public Inventory createNormalChest(Inventory inventory){
            Random random = new Random();
                // Füge Gegenstände in die Truhe ein
                inventory.addItem(new ItemStack(Material.DIAMOND, 5));

                if (random.nextDouble() < 0.7) {
                    ItemStack bow = randomBow(0);
                    inventory.addItem(bow);
                }

                 if (random.nextDouble() < 0.7) {
                    ItemStack buildtool = createItem(Material.OAK_PLANKS, "§6Bauholz", List.of("§7Angriff? Blockade!"));
                    inventory.addItem(buildtool);
                }

                 ItemStack arrow = createArrow(1);
                    inventory.addItem(arrow);
                if (random.nextDouble() < 0.1) {
                ItemStack arrow2 = createArrow(1.5);
                    inventory.addItem(arrow2);
                }

               if (random.nextDouble() < 0.05) {
                 ItemStack potion1 = createHealingPotion();
                    inventory.addItem(potion1);
                } 

        return inventory;
     }

    public Inventory createSpecialChest(Inventory inventory){
                Random random = new Random();
                // Füge Gegenstände in die Truhe ein
                inventory.addItem(new ItemStack(Material.DIAMOND, 5));
                if (random.nextDouble() < 0.9) {
                    ItemStack bow = randomBow(1);
                    inventory.addItem(bow);
                }
                 if (random.nextDouble() < 0.7) {
                    ItemStack buildtool = createItem(Material.OAK_PLANKS, "§6Bauholz", List.of("§7Angriff? Blockade!"));
                    inventory.addItem(buildtool);
                }

                ItemStack arrow = createArrow(2);
                    inventory.addItem(arrow);
                    ItemStack arrow2 = createArrow(2.5);
                    inventory.addItem(arrow2);

                if (random.nextDouble() < 0.2) {
                 ItemStack potion1 = createHealingPotion();
                    inventory.addItem(potion1);
                } 

                if (random.nextDouble() < 0.05) {
                 ItemStack potion2 = createDamagePotion();
                    inventory.addItem(potion2);
                } 

                if (random.nextDouble() < 0.01) {
                 inventory.addItem(new ItemStack(Material.SHIELD, 1));
                } 

        return inventory;
     }



      public static ItemStack randomBow(int odds) {
        Random random = new Random();
        // Wähle eine zufällige Kategorie
        String category = CATEGORIES[random.nextInt(CATEGORIES.length)];

        // Bestimme die Rarität basierend auf Wahrscheinlichkeiten
        int rarityIndex = 0;
        if (odds==0) {
        rarityIndex = determineRaritySpecial();
        } else rarityIndex = determineRarityNormal();

        // Wähle die entsprechende Farbe der Rarität
        ChatColor rarityColor = RARITY_COLORS[rarityIndex];

        // Erstelle den Crossbow
        ItemStack crossbow = new ItemStack(Material.CROSSBOW);
        ItemMeta meta = crossbow.getItemMeta();
        if (meta != null) {
            // Setze den Namen des Crossbows
            String displayName = rarityColor + category + " Crossbow";
            meta.setDisplayName(displayName);

            // Setze Lore (Beschreibung)
            meta.setLore(List.of(
                    ChatColor.GRAY + "Kategorie: " + category
            ));

            // Füge Verzauberungen basierend auf Kategorie und Seltenheit hinzu
            switch (category) {
                case "Multishot" -> meta.addEnchant(Enchantment.MULTISHOT, rarityIndex + 1, true);
                case "Speedshot" -> meta.addEnchant(Enchantment.QUICK_CHARGE, rarityIndex + 1, true);
                case "Distanceshot" -> meta.addEnchant(Enchantment.ARROW_KNOCKBACK, rarityIndex + 1, true);
                case "Precisionshot" -> meta.addEnchant(Enchantment.ARROW_DAMAGE, rarityIndex + 1, true);
            }

            // Verstecke die Verzauberungen (optional)
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            crossbow.setItemMeta(meta);
        }

        return crossbow;
    }

    private static int determineRarityNormal() {
        Random random = new Random();
        // Beginne mit der häufigsten Seltenheit (Grau)
        int rarity = 0;

        // Jede Stufe hat eine 33% Chance, zur nächsten aufzusteigen
        if (random.nextDouble() < 0.33) {
            rarity++; // Grün
            if (random.nextDouble() < 0.33) {
                rarity++; // Blau
                if (random.nextDouble() < 0.33) {
                    rarity++; // Lila
                    if (random.nextDouble() < 0.33) {
                        rarity++; // Gold
                    }
                }
            }
        }

        return rarity;
    }

    private static int determineRaritySpecial() {
        Random random = new Random();
        // Beginne mit der häufigsten Seltenheit (Grau)
        int rarity = 0;

        // Jede Stufe hat eine 33% Chance, zur nächsten aufzusteigen
        if (random.nextDouble() < 0.5) {
            rarity++; // Grün
            if (random.nextDouble() < 0.5) {
                rarity++; // Blau
                if (random.nextDouble() < 0.5) {
                    rarity++; // Lila
                    if (random.nextDouble() < 0.5) {
                        rarity++; // Gold
                    }
                }
            }
        }

        return rarity;
    }

    private static String rarityToString(int index) {
        return switch (index) {
            case 0 -> "Gewöhnlich";
            case 1 -> "Ungewöhnlich";
            case 2 -> "Selten";
            case 3 -> "Episch";
            case 4 -> "Legendär";
            default -> "Unbekannt";
        };
    }

    public static ItemStack createArrow(double multiplier) {
        Random random = new Random();
        int amount = 1;
        // Wähle eine zufällige Kategorie
        String category = CATEGORIES[random.nextInt(CATEGORIES.length)];
        // Erstelle einen speziellen Pfeil für die Kategorie
        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta meta = arrow.getItemMeta();
        if (meta != null) {
            // Setze den Namen des Pfeils
            meta.setDisplayName(ChatColor.WHITE + category);

            // Setze optional Lore
            meta.setLore(List.of(
                    ChatColor.GRAY + "Spezieller Pfeil für: " + category
            ));

            // Füge Verzauberungen basierend auf Kategorie und Seltenheit hinzu
            switch (category) {
                case "Multishot" -> amount = (int) Math.ceil(amount*10* (int) random.nextDouble() * multiplier);
                case "Speedshot" -> amount = (int) Math.ceil(amount*30* (int) random.nextDouble() * multiplier);
                case "Distanceshot" -> amount = (int) Math.ceil(amount*2* (int) random.nextDouble() * multiplier);
                case "Precisionshot" -> amount = (int) Math.ceil(amount*5* (int) random.nextDouble() * multiplier);
            }

            
            arrow.setAmount(amount);

            arrow.setItemMeta(meta);
        }

        return arrow;
    }

    public static ItemStack createHealingPotion() {
        ItemStack potion = new ItemStack(Material.SPLASH_POTION); // Werfbarer Trank
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Wurf-Heiltrank");
            meta.setBasePotionData(new PotionData(PotionType.REGEN));
            meta.clearCustomEffects();
            meta.addCustomEffect(
                new PotionEffect(PotionEffectType.REGENERATION, 100, 0), // Dauer in Ticks: 100 = 5 Sekunden
                true // Überschreibe bestehende Effekte
            );
            meta.setLore(List.of(
                    ChatColor.GRAY + "Ein magischer Heiltrank.",
                    ChatColor.GRAY + "Heilt Spieler in einem Radius von 5 Blöcken."
            ));
            potion.setItemMeta(meta);
        }
        return potion;
    }

    public static ItemStack createDamagePotion() {
        ItemStack potion = new ItemStack(Material.SPLASH_POTION); // Werfbarer Trank
            PotionMeta meta = (PotionMeta) potion.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Flächenschaden");
             // Setze den Tranktyp auf Regeneration
            meta.setBasePotionData(new PotionData(PotionType.INSTANT_DAMAGE));
             meta.clearCustomEffects();
            meta.addCustomEffect(
                new PotionEffect(PotionEffectType.REGENERATION, 100, 0), // Dauer in Ticks: 100 = 5 Sekunden
                true // Überschreibe bestehende Effekte
            );
            meta.setLore(List.of(
                    ChatColor.GRAY + "Ein magischer Schmerzentrank.",
                    ChatColor.GRAY + "Verletzt Spieler in einem Radius von 5 Blöcken."
            ));
            potion.setItemMeta(meta);
        }
        return potion;
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
