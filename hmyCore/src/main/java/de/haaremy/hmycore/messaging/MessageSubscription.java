package de.haaremy.hmycore.messaging;

public interface MessageSubscription extends AutoCloseable {
    String topicPattern();

    @Override
    void close();
}
