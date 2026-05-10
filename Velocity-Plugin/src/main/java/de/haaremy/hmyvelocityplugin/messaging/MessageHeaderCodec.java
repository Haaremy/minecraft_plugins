package de.haaremy.hmyvelocityplugin.messaging;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.regex.Pattern;

public final class MessageHeaderCodec {

    private static final Pattern TOPIC_PATTERN = Pattern.compile("[a-z0-9._-]+");
    private static final Pattern SERVER_NAME_PATTERN = Pattern.compile("[a-zA-Z0-9._*-]+");
    private static final Pattern CORRELATION_ID_PATTERN = Pattern.compile("[a-zA-Z0-9._-]*");

    private MessageHeaderCodec() {}

    public static byte[] encode(MessageHeader header, byte[] payload) {
        validate(header, payload);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(baos)) {
            out.writeByte(header.version());
            out.writeUTF(header.topic());
            out.writeUTF(header.sourceServer());
            out.writeUTF(header.targetServer());
            out.writeUTF(header.correlationId());
            out.writeLong(header.timestampMs());
            out.writeInt(payload == null ? 0 : payload.length);
            if (payload != null && payload.length > 0) {
                out.write(payload);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("encode failed", e);
        }
    }

    public static DecodedMessage decode(byte[] data) {
        if (data == null || data.length < 1) {
            throw new IllegalArgumentException("frame too short");
        }
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            byte version = in.readByte();
            if (version != MessageHeader.CURRENT_VERSION) {
                throw new IllegalArgumentException("unsupported version: " + version);
            }
            String topic = in.readUTF();
            String sourceServer = in.readUTF();
            String targetServer = in.readUTF();
            String correlationId = in.readUTF();
            long timestampMs = in.readLong();
            int payloadLength = in.readInt();
            if (payloadLength < 0 || payloadLength > MessageHeader.MAX_PAYLOAD_BYTES) {
                throw new IllegalArgumentException("payload length out of bounds: " + payloadLength);
            }
            byte[] payload = new byte[payloadLength];
            if (payloadLength > 0) {
                int read = in.read(payload);
                if (read != payloadLength) {
                    throw new IllegalArgumentException("payload truncated: expected "
                            + payloadLength + " got " + read);
                }
            }
            MessageHeader header = new MessageHeader(version, topic, sourceServer, targetServer,
                    correlationId, timestampMs);
            validate(header, payload);
            return new DecodedMessage(header, payload);
        } catch (IOException e) {
            throw new IllegalArgumentException("decode failed", e);
        }
    }

    private static void validate(MessageHeader header, byte[] payload) {
        if (header.version() != MessageHeader.CURRENT_VERSION) {
            throw new IllegalArgumentException("bad version: " + header.version());
        }
        if (header.topic().isEmpty() || header.topic().length() > MessageHeader.MAX_TOPIC_LENGTH
                || !TOPIC_PATTERN.matcher(header.topic()).matches()) {
            throw new IllegalArgumentException("bad topic: " + header.topic());
        }
        if (header.sourceServer().isEmpty()
                || header.sourceServer().length() > MessageHeader.MAX_SERVER_NAME_LENGTH
                || !SERVER_NAME_PATTERN.matcher(header.sourceServer()).matches()) {
            throw new IllegalArgumentException("bad sourceServer: " + header.sourceServer());
        }
        if (header.targetServer().isEmpty()
                || header.targetServer().length() > MessageHeader.MAX_SERVER_NAME_LENGTH
                || !SERVER_NAME_PATTERN.matcher(header.targetServer()).matches()) {
            throw new IllegalArgumentException("bad targetServer: " + header.targetServer());
        }
        if (header.correlationId().length() > MessageHeader.MAX_CORRELATION_ID_LENGTH
                || !CORRELATION_ID_PATTERN.matcher(header.correlationId()).matches()) {
            throw new IllegalArgumentException("bad correlationId: " + header.correlationId());
        }
        int len = payload == null ? 0 : payload.length;
        if (len > MessageHeader.MAX_PAYLOAD_BYTES) {
            throw new IllegalArgumentException("payload too large: " + len);
        }
    }

    public static final class DecodedMessage {
        private final MessageHeader header;
        private final byte[] payload;

        public DecodedMessage(MessageHeader header, byte[] payload) {
            this.header = header;
            this.payload = payload == null ? new byte[0] : payload;
        }

        public MessageHeader header() { return header; }
        public byte[] payload() { return payload; }
    }
}
