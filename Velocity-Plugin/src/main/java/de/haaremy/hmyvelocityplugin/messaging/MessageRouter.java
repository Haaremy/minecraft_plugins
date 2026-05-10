package de.haaremy.hmyvelocityplugin.messaging;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import org.slf4j.Logger;

public final class MessageRouter {

    private final VelocityMessagingService messagingService;
    private final Logger logger;

    public MessageRouter(VelocityMessagingService messagingService, Logger logger) {
        this.messagingService = messagingService;
        this.logger = logger;
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(VelocityMessagingService.CHANNEL_ID)) {
            return;
        }
        // Frames sind intern — never forward to client.
        event.setResult(PluginMessageEvent.ForwardResult.handled());

        if (!(event.getSource() instanceof ServerConnection)) {
            return;
        }

        byte[] data = event.getData();
        MessageHeaderCodec.DecodedMessage decoded;
        try {
            decoded = MessageHeaderCodec.decode(data);
        } catch (RuntimeException ex) {
            logger.warn("[hmy:msg] reject incoming frame: {}", ex.getMessage());
            return;
        }
        messagingService.dispatchForwarded(data, decoded.header());
    }
}
