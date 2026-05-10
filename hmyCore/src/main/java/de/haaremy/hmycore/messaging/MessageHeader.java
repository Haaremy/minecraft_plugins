package de.haaremy.hmycore.messaging;

import java.util.Objects;

public final class MessageHeader {

    public static final byte CURRENT_VERSION = 1;
    public static final String BROADCAST_TARGET = "*";
    public static final String VELOCITY_SERVER = "velocity";
    public static final int MAX_PAYLOAD_BYTES = 32 * 1024;
    public static final int MAX_TOPIC_LENGTH = 64;
    public static final int MAX_SERVER_NAME_LENGTH = 64;
    public static final int MAX_CORRELATION_ID_LENGTH = 64;

    private final byte version;
    private final String topic;
    private final String sourceServer;
    private final String targetServer;
    private final String correlationId;
    private final long timestampMs;

    public MessageHeader(byte version, String topic, String sourceServer, String targetServer,
                         String correlationId, long timestampMs) {
        this.version = version;
        this.topic = Objects.requireNonNull(topic, "topic");
        this.sourceServer = Objects.requireNonNull(sourceServer, "sourceServer");
        this.targetServer = Objects.requireNonNull(targetServer, "targetServer");
        this.correlationId = correlationId == null ? "" : correlationId;
        this.timestampMs = timestampMs;
    }

    public byte version() { return version; }
    public String topic() { return topic; }
    public String sourceServer() { return sourceServer; }
    public String targetServer() { return targetServer; }
    public String correlationId() { return correlationId; }
    public long timestampMs() { return timestampMs; }

    public boolean isBroadcast() {
        return BROADCAST_TARGET.equals(targetServer);
    }

    public boolean topicMatches(String pattern) {
        if (pattern == null || pattern.isEmpty()) return false;
        if (pattern.equals(topic)) return true;
        if (pattern.endsWith(".*")) {
            String prefix = pattern.substring(0, pattern.length() - 2);
            return topic.equals(prefix) || topic.startsWith(prefix + ".");
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MessageHeader other)) return false;
        return version == other.version
                && timestampMs == other.timestampMs
                && topic.equals(other.topic)
                && sourceServer.equals(other.sourceServer)
                && targetServer.equals(other.targetServer)
                && correlationId.equals(other.correlationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, topic, sourceServer, targetServer, correlationId, timestampMs);
    }

    @Override
    public String toString() {
        return "MessageHeader{v=" + version
                + ", topic=" + topic
                + ", src=" + sourceServer
                + ", dst=" + targetServer
                + ", cid=" + (correlationId.isEmpty() ? "-" : correlationId)
                + ", ts=" + timestampMs + "}";
    }
}
