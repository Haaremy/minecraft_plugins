package de.haaremy.hmylobby;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.time.Duration;
import java.util.*;

public class PlayerEventListener implements Listener {

    private final HmyLobby plugin;
    private final HmyLanguageManager language;
    private final Map<UUID, BossBar>  activeBossBars   = new HashMap<>();
    private final Map<UUID, Long>     rocketCooldown   = new HashMap<>();
    private final Map<UUID, Integer>  playerSpeedLevel = new HashMap<>();
    private final Set<UUID>           hiddenPlayers    = new HashSet<>();

    private static final float[]  SPEED_VALUES = {0.2f, 0.3f, 0.4f};
    private static final String[] SPEED_LABELS = {"§7Normal", "§aSchnell §8(+50%)", "§6Sehr Schnell §8(+100%)"};

    // XP-Bar-Animation
    private double xpTick = 0;

    public PlayerEventListener(HmyLobby plugin, HmyLanguageManager language) {
        this.plugin = plugin;
        this.language = language;
        Bukkit.getScheduler().runTaskTimer(plugin, this::handleLavaDamage, 20L, 10L);
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateBossBars,   20L, 100L);
        Bukkit.getScheduler().runTaskTimer(plugin, this::animateXpBar,     1L,  2L);
    }

    // ── Join / Quit ───────────────────────────────────────────────────────────

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        event.joinMessage(null);

        player.setGameMode(GameMode.ADVENTURE);
        playerSpeedLevel.put(player.getUniqueId(), 0);
        player.setWalkSpeed(SPEED_VALUES[0]);
        giveLobbyItems(player);

        // BossBar anzeigen
        BossBar bar = BossBar.bossBar(buildBossBarText(), 1.0f, BossBar.Color.PINK, BossBar.Overlay.PROGRESS);
        activeBossBars.put(player.getUniqueId(), bar);
        player.showBossBar(bar);

        // Neuen Spieler vor denjenigen mit Spieler-ausgeblendet verbergen
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.equals(player) && hiddenPlayers.contains(online.getUniqueId())) {
                online.hidePlayer(plugin, player);
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> playFeedbackEffects(player), 20L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        BossBar bar = activeBossBars.remove(p.getUniqueId());
        if (bar != null) p.hideBossBar(bar);
        playerSpeedLevel.remove(p.getUniqueId());
        hiddenPlayers.remove(p.getUniqueId());
        p.setWalkSpeed(SPEED_VALUES[0]);
        plugin.getCosmeticMenuListener().removeAllCosmetics(p);
        plugin.getSocialListener().removeOpenGUI(p.getUniqueId());
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player p)) return;
        String title = LegacyComponentSerializer.legacySection().serialize(event.getView().title());
        if (title.contains("Freundesliste")) {
            plugin.getSocialListener().removeOpenGUI(p.getUniqueId());
        }
    }

    // ── BossBar ───────────────────────────────────────────────────────────────

    private Component buildBossBarText() {
        return Component.text("§dmc.haaremy.de §8| §a" + Bukkit.getOnlinePlayers().size() + " §7Spieler online");
    }

    private void updateBossBars() {
        Component text = buildBossBarText();
        activeBossBars.values().forEach(bar -> bar.name(text));
    }

    // ── XP-Bar-Animation ──────────────────────────────────────────────────────

    private void animateXpBar() {
        xpTick += 0.04;
        float progress = (float) ((Math.sin(xpTick) + 1.0) / 2.0);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setExp(progress);
            p.setLevel(0);
        }
    }

    // ── Hotbar items ──────────────────────────────────────────────────────────
    //
    // Slot-Layout:
    //   0 = My-Menü        (Spielerkopf, immer sichtbar)
    //   4 = Navigator       (hmy.lobby.selector)  – enthält Compass für Lobby-Teleports
    //   7 = Feder           (hmy.lobby.speed)
    //   8 = Sprung Boost    (hmy.lobby.rocket)
    //   Infos + Freunde befinden sich im My-Menü (Slot 26 bzw. 20)

    private void giveLobbyItems(Player player) {
        player.getInventory().clear();
        player.getInventory().setItem(0, getPlayerHead(player, "§bMy-Menü"));

        if (player.hasPermission("hmy.lobby.selector"))
            player.getInventory().setItem(4, createItem(Material.NETHER_STAR, "§6Navigator",
                    List.of("§7Serverauswahl & Teleport", "§8Berechtigung: §ehmy.lobby.selector")));

        if (player.hasPermission("hmy.lobby.speed"))
            player.getInventory().setItem(7, buildSpeedItem(player));

        if (player.hasPermission("hmy.lobby.rocket"))
            player.getInventory().setItem(8, createItem(Material.FIREWORK_ROCKET, "§bSprung Boost",
                    List.of("§7Abflug!", "§8Berechtigung: §ehmy.lobby.rocket")));
    }

    private ItemStack buildSpeedItem(Player player) {
        int lvl = playerSpeedLevel.getOrDefault(player.getUniqueId(), 0);
        return createItem(Material.FEATHER, "§eGeschwindigkeit §8| " + SPEED_LABELS[lvl],
                List.of("§7Klicke zum Wechseln",
                        "§8Stufe: §e" + (lvl + 1) + "/" + SPEED_VALUES.length,
                        "§8Berechtigung: §ehmy.lobby.speed"));
    }

    private ItemStack buildVisibilityItem(Player player) {
        boolean hidden = hiddenPlayers.contains(player.getUniqueId());
        return createItem(Material.ENDER_PEARL,
                hidden ? "§bSpieler §8| §cAusgeblendet" : "§bSpieler §8| §aSichtbar",
                List.of("§7Klicke zum Wechseln",
                        "§8Berechtigung: §ehmy.lobby.visibility"));
    }

    // ── Speed / Visibility toggle ─────────────────────────────────────────────

    private void toggleSpeed(Player player) {
        int next = (playerSpeedLevel.getOrDefault(player.getUniqueId(), 0) + 1) % SPEED_VALUES.length;
        playerSpeedLevel.put(player.getUniqueId(), next);
        player.setWalkSpeed(SPEED_VALUES[next]);
        player.getInventory().setItem(7, buildSpeedItem(player));
        player.sendActionBar(Component.text(language.getMessage(player, "speed_changed",
                "§eGeschwindigkeit: {speed}", Map.of("speed", SPEED_LABELS[next]))));
        player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f + next * 0.3f);
    }

    private void togglePlayerVisibility(Player player) {
        boolean wasHidden = hiddenPlayers.contains(player.getUniqueId());
        if (wasHidden) {
            hiddenPlayers.remove(player.getUniqueId());
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (!other.equals(player)) player.showPlayer(plugin, other);
            }
        } else {
            hiddenPlayers.add(player.getUniqueId());
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (!other.equals(player)) player.hidePlayer(plugin, other);
            }
        }
        player.sendActionBar(Component.text(wasHidden
                ? language.getMessage(player, "players_visible", "§aSpieler §7sichtbar")
                : language.getMessage(player, "players_hidden", "§cSpieler §7ausgeblendet")));
        player.playSound(player, Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ── Interact ──────────────────────────────────────────────────────────────

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return;

        String name = LegacyComponentSerializer.legacySection().serialize(item.getItemMeta().displayName());

        if (name.equals("§6Navigator")) {
            event.setCancelled(true); openNavigatorMenu(player);
        } else if (name.equals("§bMy-Menü")) {
            event.setCancelled(true); openHeadMenu(player);
        } else if (name.equals("§bSprung Boost")) {
            handleRocketLaunch(player);
        } else if (item.getType() == Material.FEATHER && name.startsWith("§eGeschwindigkeit")) {
            event.setCancelled(true); toggleSpeed(player);
        }
    }

    // ── Inventory Click & Drag ────────────────────────────────────────────────

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = LegacyComponentSerializer.legacySection().serialize(event.getView().title());
        if (!isCustomMenu(title)) return;

        // Alle Klicks in eigenen Menüs sperren
        event.setCancelled(true);

        int       slot    = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || !clicked.hasItemMeta()) return;

        if (title.equals("§6Navigator")) {
            handleNavigatorClick(player, clicked, slot);
        } else if (title.contains("TP-Punkte")) {
            handleTeleportMenuClick(player, clicked);
        } else if (title.contains("MY-MENÜ")) {
            switch (slot) {
                case 4  -> requestFriendsList(player);
                case 10 -> plugin.getCosmeticMenuListener().openParticleMenu(player);
                case 11 -> plugin.getCosmeticMenuListener().openCosmeticsMenu(player);
                case 13 -> openLanguageMenu(player);
                case 15 -> plugin.getCosmeticMenuListener().openHeadsMenu(player);
                case 16 -> plugin.getCosmeticMenuListener().openMountMenu(player);
                case 22 -> openUserSettingsMenu(player);
                case 26 -> { player.closeInventory(); player.performCommand("help"); }
            }
            if (slot >= 0 && slot < 27) player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5f, 1f);
        } else if (title.contains("Sprache")) {
            handleLanguageClick(player, clicked, slot);
        } else if (title.contains("Einstellungen")) {
            handleUserSettingsClick(player, slot);
        } else if (title.contains("Freundesliste")) {
            handleFriendsGUIClick(player, clicked, slot, event.getClick());
        }
    }

    /** Verhindert das Verschieben von Items via Drag in eigenen Menüs. */
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        String title = LegacyComponentSerializer.legacySection().serialize(event.getView().title());
        if (isCustomMenu(title)) event.setCancelled(true);
    }

    private boolean isCustomMenu(String title) {
        return title.contains("MY-MENÜ") || title.equals("§6Navigator")
                || title.contains("Einstellungen") || title.contains("Sprache")
                || title.contains("Mounts") || title.contains("Partikel")
                || title.contains("Köpfe") || title.contains("Cosmetics")
                || title.contains("TP-Punkte") || title.contains("Freundesliste");
    }

    // ── Navigator ─────────────────────────────────────────────────────────────

    private void openNavigatorMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, Component.text("§6Navigator"));
        fillGlass(inv);

        for (ServerSelectorConfig.SelectorEntry entry : plugin.getServerSelectorConfig().getEntries()) {
            inv.setItem(entry.slot(), createItem(entry.material(), entry.name(), entry.lore()));
        }

        // Kompass immer in Slot 22 – öffnet Lobby-Teleportpunkte
        List<HmyConfigManager.TeleportPoint> tpPoints = plugin.getConfigManager().getTeleportPoints();
        inv.setItem(22, createItem(Material.COMPASS, "§6Lobby Teleports",
                List.of("§7Schnellreise innerhalb der Lobby",
                        "§8" + tpPoints.size() + " Ziele verfügbar")));

        player.openInventory(inv);
    }

    private void handleNavigatorClick(Player player, ItemStack clicked, int slot) {
        if (clicked.getType() == Material.COMPASS) {
            openTeleportMenu(player);
            return;
        }
        for (ServerSelectorConfig.SelectorEntry entry : plugin.getServerSelectorConfig().getEntries()) {
            if (entry.slot() == slot) {
                player.closeInventory();
                player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);
                connectToServer(player, entry.server());
                return;
            }
        }
    }

    // ── TP-Punkte Menü ────────────────────────────────────────────────────────

    private void openTeleportMenu(Player player) {
        List<HmyConfigManager.TeleportPoint> points = plugin.getConfigManager().getTeleportPoints();
        int rows = Math.max(3, (int) Math.ceil((points.size() + 9) / 9.0));
        int size = rows * 9;
        Inventory inv = Bukkit.createInventory(null, size, Component.text("§6» §eTP-Punkte"));
        fillGlass(inv);

        int[] displaySlots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
        for (int i = 0; i < Math.min(points.size(), displaySlots.length); i++) {
            HmyConfigManager.TeleportPoint tp = points.get(i);
            inv.setItem(displaySlots[i], createItem(Material.COMPASS, tp.name(),
                    List.of("§7Klicke zum Teleportieren!")));
        }

        inv.setItem(size - 5, createItem(Material.ARROW, "§cZurück", List.of("§7Zum Navigator")));
        player.openInventory(inv);
    }

    private void handleTeleportMenuClick(Player player, ItemStack clicked) {
        String name = LegacyComponentSerializer.legacySection().serialize(clicked.getItemMeta().displayName());

        if (clicked.getType() == Material.ARROW) {
            openNavigatorMenu(player);
            return;
        }
        if (clicked.getType() != Material.COMPASS) return;

        for (HmyConfigManager.TeleportPoint tp : plugin.getConfigManager().getTeleportPoints()) {
            if (tp.name().equals(name)) {
                if (!player.hasPermission("hmy.lobby.tp." + tp.id())) {
                    player.sendActionBar(Component.text("§cDu hast keine Berechtigung für diesen Teleportpunkt."));
                    player.playSound(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }
                player.closeInventory();
                World world = Bukkit.getWorld(tp.world());
                if (world != null) {
                    // Auf Blockmitte (.5) zentrieren
                    double x = Math.floor(tp.x()) + 0.5;
                    double z = Math.floor(tp.z()) + 0.5;
                    Location loc = new Location(world, x, tp.y(), z, tp.yaw(), tp.pitch());
                    player.teleport(loc);
                    player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);
                }
                return;
            }
        }
    }

    // ── User Settings ─────────────────────────────────────────────────────────

    void openUserSettingsMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("§7§lEinstellungen"));
        fillGlass(inv);

        int lvl = playerSpeedLevel.getOrDefault(player.getUniqueId(), 0);
        inv.setItem(11, createItem(Material.FEATHER, "§eGeschwindigkeit",
                List.of("§7Aktuell: " + SPEED_LABELS[lvl], "", "§aKlicke zum Wechseln!")));

        boolean hidden = hiddenPlayers.contains(player.getUniqueId());
        inv.setItem(13, createItem(Material.ENDER_PEARL,
                hidden ? "§bSpieler §8| §cAusgeblendet" : "§bSpieler §8| §aSichtbar",
                List.of("§7Klicke zum Wechseln")));

        inv.setItem(22, createItem(Material.ARROW, "§cZurück", List.of("§7Zum My-Menü")));
        player.openInventory(inv);
    }

    private void handleUserSettingsClick(Player player, int slot) {
        switch (slot) {
            case 11 -> { toggleSpeed(player);            openUserSettingsMenu(player); }
            case 13 -> { togglePlayerVisibility(player); openUserSettingsMenu(player); }
            case 22 -> openHeadMenu(player);
        }
    }

    // ── Language ──────────────────────────────────────────────────────────────

    private void handleLanguageClick(Player player, ItemStack clicked, int slot) {
        if (slot == 18) { openHeadMenu(player); return; }
        if (!clicked.hasItemMeta()) return;

        String name = LegacyComponentSerializer.legacySection().serialize(clicked.getItemMeta().displayName());
        if      (name.contains("Deutsch")) player.performCommand("hmy language de");
        else if (name.contains("English")) player.performCommand("hmy language en");
        player.closeInventory();
        player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
    }

    private void openLanguageMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("§8» §bWähle deine Sprache"));
        fillGlass(inv);
        inv.setItem(18, createItem(Material.ARROW, "§cZurück", List.of("§7Zum My-Menü")));
        inv.setItem(11, createCustomHead(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWU3ODk5YjQ4ZTM4ZTk1NmZlYzE5YjYyYmFjOTExYWZlY2E5NWYzM2M3Mjg4ZTUzNjgzNjQ4ZWQwODZlMmIifX19",
                "§eDeutsch"));
        inv.setItem(15, createCustomHead(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGNhYzk3NzRkYTEyMTcyNDgxOTJiZWI5MmU3OTUxNVA3Y2FhYzc1OTkzODVlYzE1ODQwY2Y2Y2E3NjM1In19fQ==",
                "§bEnglish"));
        player.openInventory(inv);
    }

    // ── Friends ───────────────────────────────────────────────────────────────

    private void requestFriendsList(Player player) {
        try {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            java.io.DataOutputStream dos = new java.io.DataOutputStream(bos);
            dos.writeUTF("GET_FRIENDS");
            dos.writeUTF(player.getUniqueId().toString());
            player.sendPluginMessage(plugin, "hmy:social", bos.toByteArray());
        } catch (Exception e) {
            plugin.getLogger().warning("Fehler beim Senden von GET_FRIENDS: " + e.getMessage());
        }
    }

    private void handleFriendsGUIClick(Player player, ItemStack clicked, int slot, ClickType clickType) {
        String name = LegacyComponentSerializer.legacySection().serialize(clicked.getItemMeta().displayName());

        if (slot == 49 || clicked.getType() == Material.ARROW) {
            player.closeInventory();
            return;
        }

        boolean isOnlineFriend = clicked.getType() == Material.PLAYER_HEAD && !name.startsWith("§7");
        String friendName = name.replace("§a", "").replace("§7", "").trim();

        if (clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT) {
            // Shift-Klick → DM-Vorschlag im Chat
            player.closeInventory();
            player.sendMessage(Component.text("§8[§6DM§8] ")
                    .append(Component.text("§a[Nachricht an §e" + friendName + " §asenden]")
                            .clickEvent(ClickEvent.suggestCommand("/dm " + friendName + " "))
                            .hoverEvent(HoverEvent.showText(
                                    Component.text("§7Klicke um §e/dm " + friendName + " §7einzugeben")))));
        } else if (isOnlineFriend) {
            // Links-Klick auf Online-Freund → Server joinen
            player.closeInventory();
            sendFriendJoinRequest(player, friendName);
        }
    }

    private void sendFriendJoinRequest(Player player, String friendName) {
        try {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            java.io.DataOutputStream dos = new java.io.DataOutputStream(bos);
            dos.writeUTF("FRIEND_JOIN");
            dos.writeUTF(player.getUniqueId().toString());
            dos.writeUTF(friendName);
            player.sendPluginMessage(plugin, "hmy:social", bos.toByteArray());
        } catch (Exception e) {
            plugin.getLogger().warning("Fehler beim Senden von FRIEND_JOIN: " + e.getMessage());
        }
    }

    // ── Sprung Boost ──────────────────────────────────────────────────────────

    private void handleRocketLaunch(Player player) {
        long now = System.currentTimeMillis();
        if (rocketCooldown.getOrDefault(player.getUniqueId(), 0L) > now) return;
        player.setVelocity(player.getLocation().getDirection().multiply(1.5).setY(0.8));
        player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, player.getLocation(), 20, 0.2, 0.2, 0.2, 0.05);
        player.playSound(player, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 1f);
        rocketCooldown.put(player.getUniqueId(), now + 1000);
    }

    // ── Menus ─────────────────────────────────────────────────────────────────

    void openHeadMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("§b§lMY-MENÜ §8» §7Profile"));
        fillGlass(inv);

        inv.setItem(4,  getPlayerHead(player, "§a§lMeine Freunde §8(§7" + player.getName() + "§8)",
                List.of("§7Klicke zum Öffnen deiner Freundesliste.", "§8/friend add <Spieler>", "", "§eShift+Klick im Menü = DM senden")));
        inv.setItem(10, createItem(Material.BLAZE_POWDER,       "§e§lPartikel",
                List.of("§7Wähle einen Effekt,", "§7der dir folgt.", "", "§aKlicke zum Öffnen!")));
        inv.setItem(11, createItem(Material.LEATHER_CHESTPLATE, "§d§lCosmetics",
                List.of("§7Auren & Status-Effekte.", "", "§aKlicke zum Öffnen!")));
        inv.setItem(13, createItem(Material.DIAMOND,            "§b§lSprache §8| §7Language",
                List.of("§7Deine Sprache: §b" + language.getPlayerLanguage(player),
                        "", "§e» Klicke zum Ändern!")));
        inv.setItem(15, createItem(Material.ZOMBIE_HEAD,        "§a§lKöpfe",
                List.of("§7Setze dir einen Kopf auf.", "", "§aKlicke zum Öffnen!")));
        inv.setItem(16, createItem(Material.SADDLE,             "§c§lMounts",
                List.of("§7Reite auf coolen Tieren.", "", "§aKlicke zum Öffnen!")));
        inv.setItem(22, createItem(Material.REDSTONE_TORCH,     "§7§lEinstellungen",
                List.of("§7Geschwindigkeit & Sichtbarkeit.")));
        inv.setItem(26, createItem(Material.ENCHANTED_BOOK,     "§b§lInfos",
                List.of("§7Hilfe & Befehle.", "", "§aKlicke zum Öffnen!")));

        player.openInventory(inv);
        player.playSound(player, Sound.BLOCK_CHEST_OPEN, 1f, 1.2f);
    }

    // ── Lava / Effects ────────────────────────────────────────────────────────

    private void handleLavaDamage() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getLocation().getBlock().getType() == Material.LAVA) {
                double hp = player.getHealth();
                if (hp > 2.0) {
                    player.setHealth(hp - 2.0);
                    player.playSound(player, Sound.ENTITY_PLAYER_HURT_ON_FIRE, 0.5f, 1f);
                    player.getWorld().spawnParticle(Particle.LAVA, player.getLocation(), 5);
                } else {
                    player.teleport(player.getWorld().getSpawnLocation());
                    player.setHealth(20.0);
                    player.sendMessage(language.getMessage(player, "lava_warning", "§cPass auf die Lava auf!"));
                }
            }
        }
    }

    private void playFeedbackEffects(Player player) {
        player.showTitle(Title.title(
                Component.text("§6Willkommen"),
                Component.text("§b" + player.getName()),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2500), Duration.ofMillis(500))));
        player.getWorld().spawnParticle(Particle.DRAGON_BREATH, player.getLocation(), 1000, 1, 0, 1, 0.05, 1.0f);
        player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void connectToServer(Player player, String server) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(server);
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    void fillGlass(Inventory inv) {
        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, glass);
        }
    }

    ItemStack createItem(Material m, String name, List<String> lore) {
        ItemStack item = new ItemStack(m);
        item.editMeta(meta -> {
            meta.displayName(Component.text(name));
            meta.lore(lore.stream().map(Component::text).toList());
        });
        return item;
    }

    private ItemStack getPlayerHead(Player p, String name) {
        return getPlayerHead(p, name, List.of());
    }

    private ItemStack getPlayerHead(Player p, String name, List<String> lore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        head.editMeta(SkullMeta.class, meta -> {
            meta.setOwningPlayer(p);
            meta.displayName(Component.text(name));
            meta.lore(lore.stream().map(Component::text).toList());
        });
        return head;
    }

    ItemStack createCustomHead(String base64, String name) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        com.destroystokyo.paper.profile.PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
        profile.getProperties().add(
                new com.destroystokyo.paper.profile.ProfileProperty("textures", base64));
        meta.setPlayerProfile(profile);
        meta.displayName(Component.text(name));
        head.setItemMeta(meta);
        return head;
    }

    @EventHandler public void onMobSpawn(CreatureSpawnEvent e) { e.getEntity().setAI(false); }
}