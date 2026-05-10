package de.haaremy.hmyvelocityplugin.messaging;

public interface MessagingService {

    String CHANNEL = "hmy:msg";

    /** Sends to all backends (one carrier per backend). */
    void broadcast(String topic, byte[] payload);

    /** Sends to a single backend by name (e.g., "lobby"). */
    void send(String backendName, String topic, byte[] payload);

    /** Like broadcast/send but generates and returns a correlationId. */
    String publishWithCorrelation(String topic, String targetServer, byte[] payload);

    /** Subscribe to incoming hmy:msg frames whose topic matches the pattern. */
    MessageSubscription subscribe(String topicPattern, MessageHandler handler);
}
