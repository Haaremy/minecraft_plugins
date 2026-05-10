package de.haaremy.hmycore.messaging;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageHeaderCodecTest {

    private static MessageHeader header(String topic, String src, String dst, String cid) {
        return new MessageHeader(MessageHeader.CURRENT_VERSION, topic, src, dst, cid, 1735689600000L);
    }

    @Test
    void roundtripPreservesAllFields() {
        MessageHeader h = header("chat.broadcast", "lobby", "*", "abc-123");
        byte[] payload = "hallo welt".getBytes(StandardCharsets.UTF_8);

        byte[] frame = MessageHeaderCodec.encode(h, payload);
        MessageHeaderCodec.DecodedMessage decoded = MessageHeaderCodec.decode(frame);

        assertEquals(h, decoded.header());
        assertArrayEquals(payload, decoded.payload());
    }

    @Test
    void emptyPayloadRoundtripsAsZeroLengthArray() {
        MessageHeader h = header("system.status.tick", "velocity", "lobby", "");
        byte[] frame = MessageHeaderCodec.encode(h, new byte[0]);
        MessageHeaderCodec.DecodedMessage decoded = MessageHeaderCodec.decode(frame);
        assertEquals(0, decoded.payload().length);
        assertEquals(h, decoded.header());
    }

    @Test
    void nullPayloadEncodesAsZeroLength() {
        MessageHeader h = header("leaderboard.refresh", "kitsune", "*", "");
        byte[] frame = MessageHeaderCodec.encode(h, null);
        MessageHeaderCodec.DecodedMessage decoded = MessageHeaderCodec.decode(frame);
        assertEquals(0, decoded.payload().length);
    }

    @Test
    void maxPayloadAccepted() {
        byte[] payload = new byte[MessageHeader.MAX_PAYLOAD_BYTES];
        Arrays.fill(payload, (byte) 0x42);
        MessageHeader h = header("chat.broadcast", "lobby", "*", "");
        byte[] frame = MessageHeaderCodec.encode(h, payload);
        MessageHeaderCodec.DecodedMessage decoded = MessageHeaderCodec.decode(frame);
        assertArrayEquals(payload, decoded.payload());
    }

    @Test
    void payloadOverMaxRejected() {
        byte[] payload = new byte[MessageHeader.MAX_PAYLOAD_BYTES + 1];
        MessageHeader h = header("chat.broadcast", "lobby", "*", "");
        assertThrows(IllegalArgumentException.class,
                () -> MessageHeaderCodec.encode(h, payload));
    }

    @Test
    void badVersionRejectedOnDecode() {
        MessageHeader h = header("chat.broadcast", "lobby", "*", "");
        byte[] frame = MessageHeaderCodec.encode(h, new byte[0]);
        frame[0] = 99;
        assertThrows(IllegalArgumentException.class,
                () -> MessageHeaderCodec.decode(frame));
    }

    @Test
    void badTopicRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> MessageHeaderCodec.encode(header("Chat.BROADCAST", "lobby", "*", ""), new byte[0]));
        assertThrows(IllegalArgumentException.class,
                () -> MessageHeaderCodec.encode(header("", "lobby", "*", ""), new byte[0]));
        assertThrows(IllegalArgumentException.class,
                () -> MessageHeaderCodec.encode(header("foo bar", "lobby", "*", ""), new byte[0]));
    }

    @Test
    void truncatedFrameRejected() {
        MessageHeader h = header("chat.broadcast", "lobby", "*", "");
        byte[] frame = MessageHeaderCodec.encode(h, "abc".getBytes(StandardCharsets.UTF_8));
        byte[] truncated = Arrays.copyOf(frame, frame.length - 1);
        assertThrows(IllegalArgumentException.class,
                () -> MessageHeaderCodec.decode(truncated));
    }

    @Test
    void topicPatternMatchExact() {
        MessageHeader h = header("chat.broadcast", "lobby", "*", "");
        assertTrue(h.topicMatches("chat.broadcast"));
        assertFalse(h.topicMatches("chat.deliver"));
    }

    @Test
    void topicPatternMatchPrefix() {
        MessageHeader h = header("social.friend.online", "lobby", "*", "");
        assertTrue(h.topicMatches("social.*"));
        assertTrue(h.topicMatches("social.friend.*"));
        assertFalse(h.topicMatches("chat.*"));
    }

    @Test
    void broadcastFlag() {
        assertTrue(header("chat.broadcast", "lobby", "*", "").isBroadcast());
        assertFalse(header("chat.broadcast", "lobby", "kitsune", "").isBroadcast());
    }
}
