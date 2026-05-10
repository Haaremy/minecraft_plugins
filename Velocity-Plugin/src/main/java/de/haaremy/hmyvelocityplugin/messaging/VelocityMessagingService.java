package de.haaremy.hmyvelocityplugin.messaging;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class VelocityMessagingService implements MessagingService {

    public static final MinecraftChannelIdentifier CHANNEL_ID =
            MinecraftChannelIdentifier.create("hmy", "msg");

    private static final int MAX_QUEUE_PER_BACKEND = 256;

    private final ProxyServer server;
    private final Logger logger;

    private final Map<Long, Subscription> subscriptions = new ConcurrentHashMap<>();
    private final AtomicLong subscriptionIdSeq = new AtomicLong();

    private final Map<String, Deque<byte[]>> pendingPerBackend = new ConcurrentHashMap<>();
    private final Map<String, Long> dropHistogram = new ConcurrentHashMap<>();

    public VelocityMessagingService(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Override
    public void broadcast(String topic, byte[] payload) {
        sendFrame(topic, MessageHeader.BROADCAST_TARGET, "", payload);
    }

    @Override
    public void send(String backendName, String topic, byte[] payload) {
        if (backendName == null || backendName.isEmpty()) {
            throw new IllegalArgumentException("backendName must not be empty");
        }
        sendFrame(topic, backendName, "", payload);
    }

    @Override
    public String publishWithCorrelation(String topic, String targetServer, byte[] payload) {
        String cid = UUID.randomUUID().toString();
        sendFrame(topic, targetServer == null ? MessageHeader.BROADCAST_TARGET : targetServer,
                cid, payload);
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

    /** Encodes + dispatches a frame originating from Velocity itself. */
    private void sendFrame(String topic, String targetServer, String correlationId, byte[] payload) {
        MessageHeader header = new MessageHeader(
                MessageHeader.CURRENT_VERSION,
                topic,
                MessageHeader.VELOCITY_SERVER,
                targetServer,
                correlationId,
                System.currentTimeMillis());
        byte[] frame;
        try {
            frame = MessageHeaderCodec.encode(header, payload);
        } catch (RuntimeException ex) {
            logger.warn("[hmy:msg] encode rejected topic={} reason={}", topic, ex.getMessage());
            return;
        }
        dispatch(frame, header);
    }

    /**
     * Dispatches a (possibly forwarded) frame to backends + internal subscribers.
     * If targetServer == "*": forward to all backends and notify subscribers.
     * If targetServer == "velocity": only notify subscribers, no forwarding.
     * Else: forward to that single backend.
     */
    public void dispatchForwarded(byte[] frame, MessageHeader header) {
        dispatch(frame, header);
    }

    private void dispatch(byte[] frame, MessageHeader header) {
        if (header.isBroadcast()) {
            for (RegisteredServer backend : server.getAllServers()) {
                sendToBackend(backend, frame, header);
            }
            notifySubscribers(header, frame);
            return;
        }
        if (MessageHeader.VELOCITY_SERVER.equals(header.targetServer())) {
            notifySubscribers(header, frame);
            return;
        }
        Optional<RegisteredServer> backend = server.getServer(header.targetServer());
        if (backend.isPresent()) {
            sendToBackend(backend.get(), frame, header);
        } else {
            logger.warn("[hmy:msg] unknown target backend={} topic={}",
                    header.targetServer(), header.topic());
        }
    }

    private void sendToBackend(RegisteredServer backend, byte[] frame, MessageHeader header) {
        ServerInfo info = backend.getServerInfo();
        if (info.getName().equals(header.sourceServer())) {
            return;
        }
        boolean delivered = backend.sendPluginMessage(CHANNEL_ID, frame);
        if (!delivered) {
            enqueuePending(info.getName(), frame, header.topic());
        }
    }

    private void enqueuePending(String backendName, byte[] frame, String topic) {
        Deque<byte[]> q = pendingPerBackend.computeIfAbsent(backendName, k -> new ArrayDeque<>());
        synchronized (q) {
            if (q.size() >= MAX_QUEUE_PER_BACKEND) {
                byte[] dropped = q.pollFirst();
                String droppedTopic = peekTopic(dropped);
                dropHistogram.merge(droppedTopic, 1L, Long::sum);
                logger.warn("[hmy:msg] queue full for backend={}, dropped oldest topic={}",
                        backendName, droppedTopic);
            }
            q.addLast(frame);
        }
    }

    private void notifySubscribers(MessageHeader header, byte[] frame) {
        if (subscriptions.isEmpty()) return;
        MessageHeaderCodec.DecodedMessage decoded;
        try {
            decoded = MessageHeaderCodec.decode(frame);
        } catch (RuntimeException ex) {
            logger.warn("[hmy:msg] notify decode failed: {}", ex.getMessage());
            return;
        }
        MessageContext ctx = new MessageContext(decoded.header(), decoded.payload());
        for (Subscription sub : subscriptions.values()) {
            if (header.topicMatches(sub.topicPattern())) {
                try {
                    sub.handler().onMessage(ctx);
                } catch (RuntimeException ex) {
                    logger.warn("[hmy:msg] subscriber pattern={} threw: {}",
                            sub.topicPattern(), ex.getMessage(), ex);
                }
            }
        }
    }

    private String peekTopic(byte[] frame) {
        if (frame == null) return "?";
        try {
            return MessageHeaderCodec.decode(frame).header().topic();
        } catch (RuntimeException ex) {
            return "?";
        }
    }

    @Subscribe(order = PostOrder.LATE)
    public void onServerPostConnect(ServerPostConnectEvent event) {
        Player p = event.getPlayer();
        p.getCurrentServer().ifPresent(conn -> {
            String backendName = conn.getServerInfo().getName();
            drainPending(backendName, conn.getServer());
        });
    }

    private void drainPending(String backendName, RegisteredServer backend) {
        Deque<byte[]> q = pendingPerBackend.get(backendName);
        if (q == null) return;
        List<byte[]> drained;
        synchronized (q) {
            if (q.isEmpty()) return;
            drained = new ArrayList<>(q);
            q.clear();
        }
        int sent = 0;
        for (byte[] frame : drained) {
            try {
                if (backend.sendPluginMessage(CHANNEL_ID, frame)) {
                    sent++;
                }
            } catch (RuntimeException ex) {
                logger.warn("[hmy:msg] drain send failed backend={} reason={}",
                        backendName, ex.getMessage());
            }
        }
        if (!dropHistogram.isEmpty()) {
            logger.info("[hmy:msg] drained {} frames into backend={} (drop histogram: {})",
                    sent, backendName, new HashMap<>(dropHistogram));
            dropHistogram.clear();
        } else if (sent > 0) {
            logger.info("[hmy:msg] drained {} frames into backend={}", sent, backendName);
        }
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
