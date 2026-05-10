package de.haaremy.hmyvelocityplugin.messaging;

@FunctionalInterface
public interface MessageHandler {
    void onMessage(MessageContext ctx);
}
