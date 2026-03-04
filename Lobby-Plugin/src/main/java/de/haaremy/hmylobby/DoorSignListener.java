package de.haaremy.hmylobby;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.Door;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;

public class DoorSignListener implements Listener {

    private final HmyLobby plugin;
    // Keys für die versteckten Daten am Schild
    private final NamespacedKey serverKey;
    private final NamespacedKey permKey;

    public DoorSignListener(HmyLobby plugin) {
        this.plugin = plugin;
        this.serverKey = new NamespacedKey(plugin, "target_server");
        this.permKey = new NamespacedKey(plugin, "needed_permission");
    }

    /**
     * 1. ERSTELLUNG: Formatiert das Schild beim Schreiben um und speichert Daten intern.
     */
    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        String line0 = serialize(event.line(0)).toLowerCase();

        // Format: [s: servername]
        if (line0.startsWith("[s:") && line0.endsWith("]")) {
            String server = line0.substring(3, line0.length() - 1).trim();
            String title = serialize(event.line(1));
            String permission = serialize(event.line(2));

            // Schild optisch aufhübschen
            event.line(0, Component.text("§8§m-----------"));
            event.line(1, Component.text("§6§l" + (title.isEmpty() ? server.toUpperCase() : title)));
            event.line(2, Component.text("§7Lade...")); // Platzhalter für Spielerzahlen (per Task)
            event.line(3, Component.text("§8§m-----------"));

            // Daten im Block speichern (PersistentDataContainer)
            // Wir müssen das verzögert machen, da der BlockState erst nach dem Event final ist
         // In DoorSignListener.java -> onSignChange Methode:
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (event.getBlock().getState() instanceof Sign sign) {
                    sign.getPersistentDataContainer().set(serverKey, PersistentDataType.STRING, server);
                    if (!permission.isEmpty()) {
                        sign.getPersistentDataContainer().set(permKey, PersistentDataType.STRING, permission);
                    }
                    
                    // HIER DIE NEUE ZEILE:
                    plugin.getServerInfoListener().registerSign(sign); 
                    
                    sign.update();
                }
            });
            
            event.getPlayer().sendMessage("§aServer-Schild für §e" + server + " §aerfolgreich erstellt!");
        }
    }

    /**
     * 2. NUTZUNG: Reagiert auf Klicks auf Türen oder Schilder.
     */
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getClickedBlock() == null) return;

        Block clicked = event.getClickedBlock();
        Sign sign = null;

        // Fall A: Klick auf eine Tür -> Suche Schild darüber/daneben
        if (isDoor(clicked.getType())) {
            Block topDoor = getTopDoorBlock(clicked);
            sign = findSignAround(topDoor);
        } 
        // Fall B: Direkter Klick auf ein Schild
        else if (clicked.getState() instanceof Sign s) {
            sign = s;
        }

        if (sign == null) return;

        // Daten aus dem PersistentDataContainer des Schildes lesen
        String targetServer = sign.getPersistentDataContainer().get(serverKey, PersistentDataType.STRING);
        if (targetServer == null) return; // Kein Hmy-Server-Schild

        String requiredPerm = sign.getPersistentDataContainer().get(permKey, PersistentDataType.STRING);
        Player player = event.getPlayer();

        // Permission-Abfrage
        if (requiredPerm != null && !requiredPerm.isEmpty() && !player.hasPermission(requiredPerm)) {
            player.sendMessage("§cDu hast keine Berechtigung, diesen Server zu betreten!");
            event.setCancelled(true);
            return;
        }

        // --- NEUE LOGIK (wie im ServerSelector) ---
        event.setCancelled(true); // Tür-Animation/Öffnen stoppen
        
        player.sendMessage("§aVerbinde zu §e" + targetServer + "§7...");
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);
        
        // Direkt über BungeeCord verbinden statt per Command
        connectToServer(player, targetServer);
        event.setCancelled(true); // Verhindert das normale Öffnen der Tür für Nicht-Admins (optional)
    }

    // --- Hilfsmethoden ---

    private boolean isDoor(Material material) {
        return material.name().endsWith("_DOOR");
    }

    private Block getTopDoorBlock(Block block) {
        if (block.getBlockData() instanceof Door door) {
            if (door.getHalf() == Door.Half.BOTTOM) {
                return block.getRelative(BlockFace.UP);
            }
        }
        return block;
    }

    private Sign findSignAround(Block block) {
        // Direkt darüber prüfen
        if (block.getRelative(BlockFace.UP).getState() instanceof Sign s) return s;

        // Rundherum prüfen (Wandschilder)
        for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
            if (block.getRelative(face).getState() instanceof Sign s) return s;
        }
        return null;
    }

    private String serialize(Component component) {
        if (component == null) return "";
        return PlainTextComponentSerializer.plainText().serialize(component).trim();
    }
    
    private void connectToServer(Player player, String server) {
        com.google.common.io.ByteArrayDataOutput out = com.google.common.io.ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(server);
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }
}