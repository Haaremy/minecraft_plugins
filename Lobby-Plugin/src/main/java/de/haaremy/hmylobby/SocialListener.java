package de.haaremy.hmylobby;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Empfängt Freundesdaten vom Velocity-Plugin und öffnet das Freundes-GUI.
 * Verwaltet auch offene Freundes-GUIs für Echtzeit-Updates.
 */
public class SocialListener implements PluginMessageListener {

    private final HmyLobby plugin;
    /** UUID → offenes Freundes-Inventory */
    final Map<UUID, Inventory> openFriendGUIs = new ConcurrentHashMap<>();

    public SocialListener(HmyLobby plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player carrier, byte[] message) {
        if (!channel.equals("hmy:social")) return;

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
            String type = in.readUTF();
            if (!type.equals("FRIENDS")) return;

            UUID   uuid     = UUID.fromString(in.readUTF());
            String jsonData = in.readUTF();

            Player target = Bukkit.getPlayer(uuid);
            if (target == null) return;

            // Must run on main thread to open/update inventory
            Bukkit.getScheduler().runTask(plugin, () -> {
                Inventory existing = openFriendGUIs.get(uuid);
                if (existing != null) {
                    updateFriendsGUI(existing, jsonData);
                } else {
                    Inventory inv = buildFriendsGUI(jsonData, target.getName());
                    openFriendGUIs.put(uuid, inv);
                    target.openInventory(inv);
                    target.playSound(target, Sound.BLOCK_CHEST_OPEN, 1f, 1.2f);
                }
            });
        } catch (Exception e) {
            plugin.getLogger().warning("Fehler beim Lesen des Social-Channels: " + e.getMessage());
        }
    }

    // ── GUI building ──────────────────────────────────────────────────────────

    private Inventory buildFriendsGUI(String json, String playerName) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("§6§l Freundesliste §8– §7" + playerName));
        fillGUIBase(inv);
        updateFriendsGUI(inv, json);
        inv.setItem(49, createItem(Material.ARROW, "§cSchließen", "§7Schließe dieses Menü"));
        return inv;
    }

    private void updateFriendsGUI(Inventory inv, String json) {
        // Clear friend slots (rows 1-4)
        for (int i = 9; i < 45; i++) {
            inv.setItem(i, createItem(Material.GRAY_STAINED_GLASS_PANE, " "));
        }

        try {
            JsonArray friends = JsonParser.parseString(json).getAsJsonArray();
            int[] slots = {10, 11, 12, 13, 14, 15, 16,
                           19, 20, 21, 22, 23, 24, 25,
                           28, 29, 30, 31, 32, 33, 34};

            for (int i = 0; i < Math.min(friends.size(), slots.length); i++) {
                JsonObject friend = friends.get(i).getAsJsonObject();
                String name     = friend.get("name").getAsString();
                String server   = friend.get("server").getAsString();
                boolean online  = friend.get("online").getAsBoolean();

                Material mat  = online ? Material.PLAYER_HEAD : Material.SKELETON_SKULL;
                String status = online ? "§aOnline §8– §b" + server : "§7Offline";
                String color  = online ? "§a" : "§7";

                ItemStack item = createItem(mat, color + name,
                        "§8Status: " + status,
                        "",
                        online ? "§eKlicke: §7Server beitreten" : "§8Nicht verfügbar");
                inv.setItem(slots[i], item);
            }

            if (friends.isEmpty()) {
                inv.setItem(22, createItem(Material.PAPER, "§7Noch keine Freunde",
                        "§7Füge Freunde mit §e/friend add <Spieler> §7hinzu."));
            }
        } catch (Exception e) {
            inv.setItem(22, createItem(Material.BARRIER, "§cFehler beim Laden", "§7Versuche es erneut."));
        }
    }

    private void fillGUIBase(Inventory inv) {
        // Top row: colored glass
        DyeColor[] topColors = {DyeColor.ORANGE, DyeColor.YELLOW, DyeColor.LIME, DyeColor.CYAN,
                                DyeColor.BLUE, DyeColor.PURPLE, DyeColor.MAGENTA, DyeColor.PINK, DyeColor.ORANGE};
        for (int i = 0; i < 9; i++) {
            Material mat = Material.valueOf(topColors[i].name() + "_STAINED_GLASS_PANE");
            inv.setItem(i, createItem(mat, " "));
        }
        // Rest: dark panes
        for (int i = 9; i < 54; i++) {
            inv.setItem(i, createItem(Material.GRAY_STAINED_GLASS_PANE, " "));
        }
    }

    private enum DyeColor { ORANGE, YELLOW, LIME, CYAN, BLUE, PURPLE, MAGENTA, PINK }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        item.editMeta(meta -> {
            meta.displayName(Component.text(name));
            if (lore.length > 0)
                meta.lore(java.util.Arrays.stream(lore).map(Component::text).toList());
        });
        return item;
    }

    public void removeOpenGUI(UUID uuid) {
        openFriendGUIs.remove(uuid);
    }
}
