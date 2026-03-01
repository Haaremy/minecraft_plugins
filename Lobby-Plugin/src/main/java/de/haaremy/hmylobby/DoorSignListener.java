package de.haaremy.hmylobby;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.Door;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import de.haaremy.hmylobby.HmyLobby;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class DoorSignListener implements Listener {

    private final HmyLobby plugin;

    public DoorSignListener(HmyLobby plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDoorClick(PlayerInteractEvent event) {
        // Nur Haupthand, nur Rechtsklick auf Block
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getClickedBlock() == null) return;

        Block clicked = event.getClickedBlock();
        if (!isDoor(clicked.getType())) return;

        // Obere Hälfte der Tür ermitteln
        Block topBlock = getTopDoorBlock(clicked);

        // Block über der Tür
        Block aboveBlock = topBlock.getRelative(BlockFace.UP);

        String server = getServerFromSign(aboveBlock);

        // Falls kein Schild direkt oben → Wandschilder drumherum prüfen
        if (server == null) {
            server = getServerFromWallSigns(aboveBlock);
        }

        if (server == null) return;

        Player player = event.getPlayer();
        plugin.getLogger().info(player.getName() + " betritt Server '" + server + "' via Tür-Schild.");
        player.closeInventory();
        player.performCommand("triggervelocity hmy server " + server);
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

    private String getServerFromSign(Block block) {
        if (!(block.getState() instanceof Sign sign)) return null;
        return parseSignLines(sign);
    }

    private String getServerFromWallSigns(Block topBlock) {
        // Prüfe alle 4 Seiten auf Wandschilder
        for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
            Block neighbor = topBlock.getRelative(face);
            if (!(neighbor.getState() instanceof Sign sign)) continue;
            String server = parseSignLines(sign);
            if (server != null) return server;
        }
        return null;
    }

    private String parseSignLines(Sign sign) {
        for (int i = 0; i < 4; i++) {
            String line = PlainTextComponentSerializer.plainText()
                .serialize(sign.line(i))
                .trim();

            // Format: [servername]
            if (line.startsWith("[") && line.endsWith("]")) {
                String server = line.substring(1, line.length() - 1).trim();
                if (!server.isEmpty()) return server.toLowerCase();
            }
        }
        return null;
    }
}
