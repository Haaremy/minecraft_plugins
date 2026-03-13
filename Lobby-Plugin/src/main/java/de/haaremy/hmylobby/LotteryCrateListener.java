package de.haaremy.hmylobby;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Chest;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.*;

/**
 * Lobby-Lotterie-Kisten: Admin platziert eine Truhe und tagged sie mit /lobbygame create crate.
 * Spieler können die Truhe rechtsklicken, um eine Kiste mit Animation zu öffnen.
 * Gewinn = immer hmyCoins (verschiedene Mengen).
 *
 * Economy-Channel Protokoll (→ Velocity):
 *   ADD_COINS <uuid> <long amount>
 */
public class LotteryCrateListener implements Listener {

    private static final NamespacedKey CRATE_KEY = new NamespacedKey("hmy", "lottery_crate");
    private static final String ECONOMY_CHANNEL = "hmy:economy";

    // Players currently in a crate animation → prevent double-open
    private final Set<UUID> activePlayers = new HashSet<>();

    private final HmyLobby plugin;

    // ── Prize tiers ───────────────────────────────────────────────────────────

    enum Prize {
        COMMON    (50,   Material.YELLOW_DYE,      "§eGewöhnlich",  "§7+50 §6hmyCoins",   60, DyeColor.YELLOW),
        UNCOMMON  (100,  Material.LIME_DYE,        "§aUngewöhnlich","§7+100 §6hmyCoins",  25, DyeColor.LIME),
        RARE      (250,  Material.LIGHT_BLUE_DYE,  "§bSelten",      "§7+250 §6hmyCoins",  10, DyeColor.LIGHT_BLUE),
        EPIC      (500,  Material.PURPLE_DYE,      "§5Episch",      "§7+500 §6hmyCoins",   4, DyeColor.PURPLE),
        LEGENDARY (1000, Material.ORANGE_DYE,      "§6Legendär",    "§7+1000 §6hmyCoins",  1, DyeColor.ORANGE);

        final long coins;
        final Material mat;
        final String displayName;
        final String loreLine;
        final int weight;
        final DyeColor color;

        Prize(long coins, Material mat, String displayName, String loreLine, int weight, DyeColor color) {
            this.coins = coins; this.mat = mat; this.displayName = displayName;
            this.loreLine = loreLine; this.weight = weight; this.color = color;
        }

        static Prize roll() {
            int total = Arrays.stream(values()).mapToInt(p -> p.weight).sum();
            int roll  = new Random().nextInt(total);
            int cum   = 0;
            for (Prize p : values()) { cum += p.weight; if (roll < cum) return p; }
            return COMMON;
        }
    }

    public LotteryCrateListener(HmyLobby plugin) {
        this.plugin = plugin;
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, ECONOMY_CHANNEL);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, ECONOMY_CHANNEL,
                new EconomyMessageListener(plugin));
    }

    // ── Tag a crate (called from LobbyGameManager) ────────────────────────────

    public boolean tagCrate(org.bukkit.block.Block block) {
        if (!(block.getState() instanceof Chest chest)) return false;
        chest.getPersistentDataContainer().set(CRATE_KEY, PersistentDataType.BYTE, (byte) 1);
        chest.update();
        return true;
    }

    // ── Interact: open crate ──────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        if (!(event.getClickedBlock().getState() instanceof Chest chest)) return;

        if (!chest.getPersistentDataContainer().has(CRATE_KEY, PersistentDataType.BYTE)) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (activePlayers.contains(player.getUniqueId())) {
            player.sendMessage(Component.text("§cWarte bis die aktuelle Animation endet!"));
            return;
        }

        activePlayers.add(player.getUniqueId());
        Prize prize = Prize.roll();
        openCrateAnimation(player, prize);
    }

    // ── Crate animation ───────────────────────────────────────────────────────

    private void openCrateAnimation(Player player, Prize prize) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("§6✦ §l§6Kisten-Lotterie §6✦"));

        // Banner top/bottom rows: festive colors
        fillBannerRow(inv, 0,  new DyeColor[]{DyeColor.ORANGE, DyeColor.YELLOW, DyeColor.ORANGE,
                DyeColor.ORANGE, DyeColor.YELLOW, DyeColor.ORANGE, DyeColor.ORANGE, DyeColor.YELLOW, DyeColor.ORANGE});
        fillBannerRow(inv, 45, new DyeColor[]{DyeColor.PURPLE, DyeColor.MAGENTA, DyeColor.PINK,
                DyeColor.PURPLE, DyeColor.MAGENTA, DyeColor.PINK, DyeColor.PURPLE, DyeColor.MAGENTA, DyeColor.PINK});

        // Rows 1-4: dark glass panes
        ItemStack dark = pane(Material.BLACK_STAINED_GLASS_PANE, " ");
        ItemStack gold = pane(Material.YELLOW_STAINED_GLASS_PANE, "§e▼ Hier landet dein Gewinn! ▼");
        for (int i = 9; i < 45; i++) inv.setItem(i, dark);
        // Highlight center marker (row 3, col 4 = slot 31)
        inv.setItem(22, gold); inv.setItem(31, gold); // top + bottom of center col

        // Build spin strip: 27 items cycling through prizes
        List<Prize> strip = buildStrip(prize);
        // Place 9 items in spin row (row 2, slots 18-26)
        for (int col = 0; col < 9; col++) {
            inv.setItem(18 + col, prizeItem(strip.get(col)));
        }

        player.openInventory(inv);
        player.playSound(player, Sound.BLOCK_CHEST_OPEN, 1f, 1.2f);

        // Spinning animation
        int[] delays = {1, 1, 1, 1, 2, 2, 2, 3, 3, 4, 5, 6, 7, 9, 12, 16};
        int[] state  = {0, 0}; // [0]=tick, [1]=shiftCount
        final int totalShifts = 16 + new Random().nextInt(5); // random extra shifts

        new BukkitRunnable() {
            int tick       = 0;
            int shiftCount = 0;
            int delayIdx   = 0;
            int offset     = 0;

            @Override
            public void run() {
                tick++;
                int currentDelay = delays[Math.min(delayIdx, delays.length - 1)];
                if (tick < currentDelay) return;

                tick = 0;
                offset++;
                shiftCount++;

                // Update spin row
                for (int col = 0; col < 9; col++) {
                    int idx = (offset + col) % strip.size();
                    inv.setItem(18 + col, prizeItem(strip.get(idx)));
                }

                // Click sound, slowing pitch
                float pitch = Math.max(0.5f, 2.0f - shiftCount * 0.08f);
                player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, pitch);

                if (shiftCount >= totalShifts) {
                    cancel();
                    // Center slot (18+4=22) is the winner
                    int winIdx = (offset + 4) % strip.size();
                    Prize won = strip.get(winIdx);

                    // Make center item glow
                    inv.setItem(22, prizeItem(won));
                    // Fill spin row with plain prize items
                    for (int col = 0; col < 9; col++) {
                        int idx = (offset + col) % strip.size();
                        ItemStack item = prizeItem(strip.get(idx));
                        if (col == 4) {
                            item = glowingPrizeItem(won); // center glows
                        }
                        inv.setItem(18 + col, item);
                    }

                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        revealPrize(player, won);
                    }, 10L);
                }

                if (shiftCount < delays.length) delayIdx = shiftCount;
            }
        }.runTaskTimer(plugin, 5L, 1L);
    }

    private void revealPrize(Player player, Prize prize) {
        player.closeInventory();
        activePlayers.remove(player.getUniqueId());

        // Award coins via Velocity
        sendAddCoins(player, prize.coins);

        // Effects
        spawnFireworks(player);
        player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP,        1f, 0.8f);
        player.playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE,   1f, 1f);

        // Title
        player.showTitle(Title.title(
                Component.text(prize.displayName),
                Component.text("§6+" + prize.coins + " hmyCoins §7wurden gutgeschrieben!"),
                Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(3000), Duration.ofMillis(700))));

        // Chat message
        Bukkit.broadcast(Component.text(
                "§6✦ §e" + player.getName() + " §7hat eine §6Kiste §7geöffnet und "
                        + prize.displayName + " §7(" + prize.loreLine + "§7) gewonnen! §6✦"));

        // Particles
        player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, player.getLocation(), 200, 0.5, 1, 0.5, 0.1);
        player.getWorld().spawnParticle(Particle.VILLAGER_HAPPY,  player.getLocation(), 50,  0.5, 1, 0.5, 0.05);
    }

    private void spawnFireworks(Player player) {
        DyeColor[] colors = {DyeColor.YELLOW, DyeColor.ORANGE, DyeColor.ORANGE};
        for (int i = 0; i < 4; i++) {
            final int delay = i * 10;
            final Color c1 = Color.fromRGB(colors[i % colors.length].getFireworkColor().asRGB());
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Location loc = player.getLocation().add((Math.random() - 0.5) * 2, 1, (Math.random() - 0.5) * 2);
                Firework fw = loc.getWorld().spawn(loc, Firework.class);
                FireworkMeta meta = fw.getFireworkMeta();
                meta.addEffect(FireworkEffect.builder()
                        .with(FireworkEffect.Type.BURST)
                        .withColor(c1, Color.WHITE)
                        .withFade(Color.YELLOW)
                        .withFlicker().withTrail().build());
                meta.setPower(0);
                fw.setFireworkMeta(meta);
            }, delay);
        }
    }

    // ── Economy channel ───────────────────────────────────────────────────────

    private void sendAddCoins(Player player, long amount) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("ADD_COINS");
        out.writeUTF(player.getUniqueId().toString());
        out.writeLong(amount);
        player.sendPluginMessage(plugin, ECONOMY_CHANNEL, out.toByteArray());
    }

    // ── Prevent clicking inside crate inventory ───────────────────────────────

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        String title = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacySection().serialize(event.getView().title());
        if (title.contains("Kisten-Lotterie")) event.setCancelled(true);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Build a 27-item strip ending with the winning prize at a random position */
    private List<Prize> buildStrip(Prize winner) {
        List<Prize> strip = new ArrayList<>();
        Prize[] values = Prize.values();
        Random rng = new Random();
        // Fill 27 items: mostly common/uncommon with the winner at center (index 13)
        for (int i = 0; i < 27; i++) {
            strip.add(values[rng.nextInt(values.length)]);
        }
        strip.set(13, winner); // center = guaranteed winner
        return strip;
    }

    private ItemStack prizeItem(Prize prize) {
        ItemStack item = new ItemStack(prize.mat);
        item.editMeta(meta -> {
            meta.displayName(Component.text(prize.displayName));
            meta.lore(List.of(Component.text(prize.loreLine)));
        });
        return item;
    }

    private ItemStack glowingPrizeItem(Prize prize) {
        ItemStack item = prizeItem(prize);
        item.editMeta(meta -> {
            meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            meta.displayName(Component.text("§l" + prize.displayName + " §6✦"));
            meta.lore(List.of(
                    Component.text(prize.loreLine),
                    Component.text("§7» §eGlückwunsch! §7«")));
        });
        return item;
    }

    private void fillBannerRow(Inventory inv, int startSlot, DyeColor[] colors) {
        for (int i = 0; i < 9; i++) {
            DyeColor color = colors[i % colors.length];
            Material bannerMat = Material.valueOf(color.name() + "_BANNER");
            ItemStack banner = new ItemStack(bannerMat);
            banner.editMeta(BannerMeta.class, meta -> {
                meta.displayName(Component.text(" "));
            });
            inv.setItem(startSlot + i, banner);
        }
    }

    private ItemStack pane(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        item.editMeta(meta -> meta.displayName(Component.text(name)));
        return item;
    }
}
