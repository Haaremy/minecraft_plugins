package de.haaremy.hmyvelocityplugin;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.haaremy.hmyvelocityplugin.economy.ComCoins;
import de.haaremy.hmyvelocityplugin.economy.CurrencyManager;
import de.haaremy.hmyvelocityplugin.friends.ComFriend;
import de.haaremy.hmyvelocityplugin.friends.FriendManager;
import net.luckperms.api.LuckPerms;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Plugin(
    id = "hmyvelocityplugin",
    name = "hmyVelocity",
    version = "1.2",
    authors = {"Haaremy"},
    dependencies = {@Dependency(id = "luckperms")}
)
public class HmyVelocityPlugin {

    private final ProxyServer server;
    private final Logger      logger;
    private final Path        dataDirectory;

    private LuckPerms           luckPerms;
    private HmyLobby            hmyLobby;
    private HmyLanguageManager  languageManager;
    private HmyConfigManager    configManager;
    private CurrencyManager     currencyManager;
    private FriendManager       friendManager;
    private PlayerTracker       playerTracker;
    private VelocityTabManager  tabManager;

    // Channels
    private static final MinecraftChannelIdentifier TRIGGER_CHANNEL = MinecraftChannelIdentifier.create("hmy", "trigger");
    private static final MinecraftChannelIdentifier STATUS_CHANNEL  = MinecraftChannelIdentifier.create("hmy", "status");
    private static final MinecraftChannelIdentifier ECONOMY_CHANNEL = MinecraftChannelIdentifier.create("hmy", "economy");
    private static final MinecraftChannelIdentifier SOCIAL_CHANNEL  = MinecraftChannelIdentifier.create("hmy", "social");

    @Inject
    public HmyVelocityPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server        = server;
        this.logger        = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        try {
            this.luckPerms = net.luckperms.api.LuckPermsProvider.get();
            logger.info("LuckPerms erfolgreich eingebunden.");
        } catch (IllegalStateException e) {
            logger.error("LuckPerms-Integration fehlgeschlagen: " + e.getMessage());
            return;
        }

        this.configManager   = new HmyConfigManager(logger, dataDirectory);
        this.languageManager = new HmyLanguageManager(logger, dataDirectory, configManager, luckPerms);
        languageManager.loadAllLanguageFiles();

        this.currencyManager = new CurrencyManager(dataDirectory, logger);
        this.friendManager   = new FriendManager(dataDirectory, logger);
        this.playerTracker   = new PlayerTracker(server, friendManager);
        this.tabManager      = new VelocityTabManager(this, server, friendManager, playerTracker, luckPerms);

        registerListeners();
        initializePluginFeatures();
        startStatusUpdateTask();

        logger.info("Haaremy: hmyVelocity vollständig gestartet.");
    }

    private void registerListeners() {
        server.getEventManager().register(this, new PlayerJoinListener(server, "lobby", languageManager));
        server.getEventManager().register(this, new PingListener(server));
        server.getEventManager().register(this, playerTracker);
        server.getEventManager().register(this, tabManager);
        tabManager.startTask();

        server.getChannelRegistrar().register(TRIGGER_CHANNEL);
        server.getChannelRegistrar().register(STATUS_CHANNEL);
        server.getChannelRegistrar().register(ECONOMY_CHANNEL);
        server.getChannelRegistrar().register(SOCIAL_CHANNEL);
    }

    private void initializePluginFeatures() {
        this.hmyLobby = new HmyLobby(server, logger, languageManager, luckPerms);

        server.getCommandManager().register(
            server.getCommandManager().metaBuilder("broadcast").build(),
            new ComBroadcast(server, languageManager));

        server.getCommandManager().register(
            server.getCommandManager().metaBuilder("hmy").build(),
            new ComHmy(new ComHmyLanguage(luckPerms, languageManager), new ComCoins(currencyManager)));

        server.getCommandManager().register(
            server.getCommandManager().metaBuilder("friend").build(),
            new ComFriend(server, friendManager, playerTracker));
    }

    // ── Status Update Task ────────────────────────────────────────────────────

    private void startStatusUpdateTask() {
        server.getScheduler().buildTask(this, () -> {
            server.getServer("lobby").ifPresent(lobby -> {
                if (lobby.getPlayersConnected().isEmpty()) return;
                for (RegisteredServer rs : server.getAllServers()) {
                    String name   = rs.getServerInfo().getName();
                    int    online = rs.getPlayersConnected().size();
                    ByteArrayDataOutput out = ByteStreams.newDataOutput();
                    out.writeUTF(name);
                    out.writeInt(online);
                    out.writeInt(100);
                    lobby.sendPluginMessage(STATUS_CHANNEL, out.toByteArray());
                }
            });
        }).repeat(5, TimeUnit.SECONDS).schedule();
        logger.info("Haaremy: Status-Update-Task (5s) gestartet.");
    }

    // ── Plugin Message Handler ────────────────────────────────────────────────

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        var id = event.getIdentifier();
        if      (id.equals(TRIGGER_CHANNEL))  handleTrigger(event);
        else if (id.equals(ECONOMY_CHANNEL))  handleEconomy(event);
        else if (id.equals(SOCIAL_CHANNEL))   handleSocial(event);
    }

    private void handleTrigger(PluginMessageEvent event) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()))) {
            String playerName = in.readUTF();
            String command    = in.readUTF();
            server.getPlayer(playerName).ifPresent(p -> server.getCommandManager().executeAsync(p, command));
        } catch (Exception e) { logger.error("Trigger-Channel Fehler: ", e); }
    }

    private void handleEconomy(PluginMessageEvent event) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()))) {
            String action = in.readUTF();
            UUID   uuid   = UUID.fromString(in.readUTF());
            switch (action) {
                case "ADD_COINS"  -> { long a = in.readLong(); currencyManager.addCoins(uuid, a);  sendBalanceUpdate(uuid); }
                case "ADD_SHARDS" -> { long a = in.readLong(); currencyManager.addShards(uuid, a); sendBalanceUpdate(uuid); }
                case "GET_BALANCE" -> sendBalanceUpdate(uuid);
            }
        } catch (Exception e) { logger.error("Economy-Channel Fehler: ", e); }
    }

    private void sendBalanceUpdate(UUID uuid) {
        server.getServer("lobby").ifPresent(lobby -> {
            if (lobby.getPlayersConnected().isEmpty()) return;
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("BALANCE");
            out.writeUTF(uuid.toString());
            out.writeLong(currencyManager.getCoins(uuid));
            out.writeLong(currencyManager.getShards(uuid));
            lobby.sendPluginMessage(ECONOMY_CHANNEL, out.toByteArray());
        });
    }

    private void handleSocial(PluginMessageEvent event) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()))) {
            String action = in.readUTF();
            if (action.equals("GET_FRIENDS")) {
                UUID uuid = UUID.fromString(in.readUTF());
                sendFriendsData(uuid);
            }
        } catch (Exception e) { logger.error("Social-Channel Fehler: ", e); }
    }

    private void sendFriendsData(UUID uuid) {
        server.getServer("lobby").ifPresent(lobby -> {
            if (lobby.getPlayersConnected().isEmpty()) return;
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (UUID fUUID : friendManager.getFriends(uuid)) {
                if (!first) sb.append(",");
                first = false;
                String  name   = server.getPlayer(fUUID).map(Player::getUsername).orElse("Unbekannt");
                String  srv    = playerTracker.getPlayerServer(fUUID).orElse("offline");
                boolean online = server.getPlayer(fUUID).isPresent();
                sb.append("{\"uuid\":\"").append(fUUID)
                  .append("\",\"name\":\"").append(name)
                  .append("\",\"server\":\"").append(srv)
                  .append("\",\"online\":").append(online).append("}");
            }
            sb.append("]");
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("FRIENDS");
            out.writeUTF(uuid.toString());
            out.writeUTF(sb.toString());
            lobby.sendPluginMessage(SOCIAL_CHANNEL, out.toByteArray());
        });
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        playerTracker.remove(event.getPlayer().getUniqueId());
        friendManager.clearFollow(event.getPlayer().getUniqueId());
    }
}
