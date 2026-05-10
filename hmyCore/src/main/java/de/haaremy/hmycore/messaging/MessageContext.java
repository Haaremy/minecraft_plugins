package de.haaremy.hmycore.messaging;

import java.nio.charset.StandardCharsets;

public final class MessageContext {

    private final MessageHeader header;
    private final byte[] payload;

    public MessageContext(MessageHeader header, byte[] payload) {
        this.header = header;
        this.payload = payload == null ? new byte[0] : payload;
    }

    public MessageHeader header() { return header; }
    public byte[] payload() { return payload; }

    public String topic() { return header.topic(); }
    public String sourceServer() { return header.sourceServer(); }
    public String targetServer() { return header.targetServer(); }
    public String correlationId() { return header.correlationId(); }
    public long timestampMs() { return header.timestampMs(); }

    public String payloadAsUtf() {
        return new String(payload, StandardCharsets.UTF_8);
    }
}
