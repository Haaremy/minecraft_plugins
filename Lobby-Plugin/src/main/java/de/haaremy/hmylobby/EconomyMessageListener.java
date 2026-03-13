package de.haaremy.hmylobby;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.UUID;

/**
 * Empfängt Balance-Updates vom Velocity-Plugin über den hmy:economy-Channel.
 * Zeigt dem Spieler sein aktuelles Guthaben an.
 */
public class EconomyMessageListener implements PluginMessageListener {

    private final HmyLobby plugin;

    public EconomyMessageListener(HmyLobby plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player carrier, byte[] message) {
        if (!channel.equals("hmy:economy")) return;

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
            String type = in.readUTF();
            if (!type.equals("BALANCE")) return;

            UUID   uuid   = UUID.fromString(in.readUTF());
            long   coins  = in.readLong();
            long   shards = in.readLong();

            Player target = Bukkit.getPlayer(uuid);
            if (target == null) return;

            // Show balance in action bar
            target.sendActionBar(Component.text(
                    "§6⬡ §e" + coins + " hmyCoins  §8| §b◆ §3" + shards + " hmyShards"));
        } catch (Exception e) {
            plugin.getLogger().warning("Fehler beim Lesen des Economy-Channels: " + e.getMessage());
        }
    }
}
