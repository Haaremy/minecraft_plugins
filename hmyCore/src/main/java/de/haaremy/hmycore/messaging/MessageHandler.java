package de.haaremy.hmycore.messaging;

@FunctionalInterface
public interface MessageHandler {
    void onMessage(MessageContext ctx);
}
