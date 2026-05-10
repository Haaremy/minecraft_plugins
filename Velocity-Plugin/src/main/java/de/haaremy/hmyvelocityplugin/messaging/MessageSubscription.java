package de.haaremy.hmyvelocityplugin.messaging;

public interface MessageSubscription extends AutoCloseable {
    String topicPattern();

    @Override
    void close();
}
