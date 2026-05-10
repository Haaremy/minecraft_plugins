package de.haaremy.hmycore.messaging;

public interface MessagingService {

    String CHANNEL = "hmy:msg";

    String localServerName();

    void publish(String topic, String targetServer, byte[] payload);

    String publishWithCorrelation(String topic, String targetServer, byte[] payload);

    MessageSubscription subscribe(String topicPattern, MessageHandler handler);
}
