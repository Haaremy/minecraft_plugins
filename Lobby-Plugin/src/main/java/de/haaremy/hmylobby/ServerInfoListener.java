package de.haaremy.hmylobby;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class ServerInfoListener implements PluginMessageListener {

    private final HmyLobby plugin;
    private final NamespacedKey serverKey;
    private final Set<Sign> activeSigns = new HashSet<>();

    public ServerInfoListener(HmyLobby plugin) {
        this.plugin = plugin;
        this.serverKey = new NamespacedKey(plugin, "target_server");
    }

    public void registerSign(Sign sign) {
        activeSigns.add(sign);
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte[] message) {
        if (!channel.equals("hmy:status")) return;

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        try {
            String serverName = in.readUTF();
            int online = in.readInt();
            int max = in.readInt();

            // Entferne Schilder, die nicht mehr existieren (Block abgebaut)
            activeSigns.removeIf(sign -> !sign.getBlock().getType().name().endsWith("_SIGN") 
                                 && !sign.getBlock().getType().name().endsWith("_WALL_SIGN"));

            for (Sign sign : activeSigns) {
                String linkedServer = sign.getPersistentDataContainer().get(serverKey, PersistentDataType.STRING);
                
                if (serverName.equalsIgnoreCase(linkedServer)) {
                    // Zeile 3 aktualisieren (Index 2)
                    sign.line(2, Component.text("§a" + online + " §7/ §2" + max));
                    sign.update();
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Fehler beim Verarbeiten des Server-Status: " + e.getMessage());
        }
    }
}