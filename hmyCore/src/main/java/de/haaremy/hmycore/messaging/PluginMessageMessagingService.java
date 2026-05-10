package de.haaremy.hmycore.messaging;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PluginMessageMessagingService
        implements MessagingService, PluginMessageListener, Listener {

    private static final int MAX_QUEUE = 256;

    private final Plugin plugin;
    private final Logger logger;
    private final String localServerName;

    private final Map<Long, Subscription> subscriptions = new ConcurrentHashMap<>();
    private final AtomicLong subscriptionIdSeq = new AtomicLong();
    private final Deque<byte[]> outboundQueue = new ArrayDeque<>();
    private final Map<String, Long> dropHistogram = new HashMap<>();

    public PluginMessageMessagingService(Plugin plugin, String localServerName) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.localServerName = sanitizeServerName(localServerName);
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, CHANNEL, this);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void shutdown() {
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, CHANNEL);
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL);
        subscriptions.clear();
        synchronized (outboundQueue) {
            outboundQueue.clear();
        }
    }

    @Override
    public String localServerName() {
        return localServerName;
    }

    @Override
    public void publish(String topic, String targetServer, byte[] payload) {
        sendFrame(topic, targetServer, "", payload);
    }

    @Override
    public String publishWithCorrelation(String topic, String targetServer, byte[] payload) {
        String cid = UUID.randomUUID().toString();
        sendFrame(topic, targetServer, cid, payload);
        return cid;
    }

    @Override
    public MessageSubscription subscribe(String topicPattern, MessageHandler handler) {
        if (topicPattern == null || topicPattern.isEmpty()) {
            throw new IllegalArgumentException("topicPattern must not be empty");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler must not be null");
        }
        long id = subscriptionIdSeq.incrementAndGet();
        Subscription sub = new Subscription(id, topicPattern, handler);
        subscriptions.put(id, sub);
        return sub;
    }

    private void sendFrame(String topic, String targetServer, String correlationId, byte[] payload) {
        MessageHeader header = new MessageHeader(
                MessageHeader.CURRENT_VERSION,
                topic,
                localServerName,
                targetServer == null ? MessageHeader.BROADCAST_TARGET : targetServer,
                correlationId,
                System.currentTimeMillis());
        byte[] frame;
        try {
            frame = MessageHeaderCodec.encode(header, payload);
        } catch (RuntimeException ex) {
            logger.log(Level.WARNING, "[hmy:msg] encode rejected topic=" + topic + ": " + ex.getMessage());
            return;
        }

        Player carrier = pickCarrier();
        if (carrier != null) {
            carrier.sendPluginMessage(plugin, CHANNEL, frame);
            return;
        }

        synchronized (outboundQueue) {
            if (outboundQueue.size() >= MAX_QUEUE) {
                byte[] dropped = outboundQueue.pollFirst();
                String droppedTopic = peekTopic(dropped);
                dropHistogram.merge(droppedTopic, 1L, Long::sum);
                logger.log(Level.WARNING, "[hmy:msg] queue full, dropped oldest topic=" + droppedTopic);
            }
            outboundQueue.addLast(frame);
        }
    }

    private Player pickCarrier() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            return player;
        }
        return null;
    }

    private String peekTopic(byte[] frame) {
        if (frame == null) return "?";
        try {
            return MessageHeaderCodec.decode(frame).header().topic();
        } catch (RuntimeException ex) {
            return "?";
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        drainQueue(event.getPlayer());
    }

    private void drainQueue(Player carrier) {
        List<byte[]> drained;
        synchronized (outboundQueue) {
            if (outboundQueue.isEmpty()) return;
            drained = new ArrayList<>(outboundQueue);
            outboundQueue.clear();
        }
        for (byte[] frame : drained) {
            try {
                carrier.sendPluginMessage(plugin, CHANNEL, frame);
            } catch (RuntimeException ex) {
                logger.log(Level.WARNING, "[hmy:msg] drain send failed: " + ex.getMessage());
            }
        }
        if (!dropHistogram.isEmpty()) {
            logger.log(Level.INFO, "[hmy:msg] queue drained " + drained.size()
                    + " frames; drop histogram: " + dropHistogram);
            dropHistogram.clear();
        } else {
            logger.log(Level.INFO, "[hmy:msg] queue drained " + drained.size() + " frames");
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] data) {
        if (!CHANNEL.equals(channel)) return;
        MessageHeaderCodec.DecodedMessage decoded;
        try {
            decoded = MessageHeaderCodec.decode(data);
        } catch (RuntimeException ex) {
            logger.log(Level.WARNING, "[hmy:msg] decode rejected: " + ex.getMessage());
            return;
        }
        MessageContext ctx = new MessageContext(decoded.header(), decoded.payload());
        for (Subscription sub : subscriptions.values()) {
            if (decoded.header().topicMatches(sub.topicPattern())) {
                try {
                    sub.handler().onMessage(ctx);
                } catch (RuntimeException ex) {
                    logger.log(Level.WARNING, "[hmy:msg] handler for pattern="
                            + sub.topicPattern() + " threw: " + ex.getMessage(), ex);
                }
            }
        }
    }

    private static String sanitizeServerName(String raw) {
        if (raw == null || raw.isBlank()) return "unknown";
        String trimmed = raw.trim();
        if (trimmed.length() > MessageHeader.MAX_SERVER_NAME_LENGTH) {
            trimmed = trimmed.substring(0, MessageHeader.MAX_SERVER_NAME_LENGTH);
        }
        return trimmed;
    }

    private final class Subscription implements MessageSubscription {
        private final long id;
        private final String pattern;
        private final MessageHandler handler;

        Subscription(long id, String pattern, MessageHandler handler) {
            this.id = id;
            this.pattern = pattern;
            this.handler = handler;
        }

        @Override
        public String topicPattern() { return pattern; }

        MessageHandler handler() { return handler; }

        @Override
        public void close() {
            subscriptions.remove(id);
        }
    }
}
