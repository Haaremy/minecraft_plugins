package de.haaremy.hmyvelocityplugin;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Cross-server private messaging.
 * /dm <player> <message...>
 * /r  <message...>   (reply to last conversation partner)
 */
public class ComDm implements SimpleCommand {

    /** Tracks the last DM partner for each player UUID. */
    static final Map<UUID, UUID> lastPartner = new ConcurrentHashMap<>();

    private static final String PREFIX = "§8[§6DM§8] ";

    private final ProxyServer server;
    private final boolean isReply;

    public ComDm(ProxyServer server, boolean isReply) {
        this.server  = server;
        this.isReply = isReply;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!(source instanceof Player sender)) {
            source.sendMessage(Component.text("§cNur Spieler können diesen Befehl nutzen."));
            return;
        }

        String[] args = invocation.arguments();

        if (isReply) {
            // /r <message...>
            if (args.length == 0) {
                sender.sendMessage(Component.text("§cVerwendung: §e/r <Nachricht>"));
                return;
            }
            UUID partnerUUID = lastPartner.get(sender.getUniqueId());
            if (partnerUUID == null) {
                sender.sendMessage(Component.text(PREFIX + "§cDu hast noch keine Nachricht erhalten."));
                return;
            }
            Optional<Player> target = server.getPlayer(partnerUUID);
            if (target.isEmpty()) {
                sender.sendMessage(Component.text(PREFIX + "§cDein Gesprächspartner ist nicht mehr online."));
                return;
            }
            sendDm(sender, target.get(), String.join(" ", args));
        } else {
            // /dm <player> <message...>
            if (args.length < 2) {
                sender.sendMessage(Component.text("§cVerwendung: §e/dm <Spieler> <Nachricht>"));
                return;
            }
            Optional<Player> target = server.getPlayer(args[0]);
            if (target.isEmpty()) {
                sender.sendMessage(Component.text(PREFIX + "§c" + args[0] + " §7ist nicht online."));
                return;
            }
            if (target.get().getUniqueId().equals(sender.getUniqueId())) {
                sender.sendMessage(Component.text(PREFIX + "§cDu kannst dir nicht selbst schreiben."));
                return;
            }
            String message = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
            sendDm(sender, target.get(), message);
        }
    }

    private void sendDm(Player sender, Player target, String message) {
        // Update both sides' last partner
        lastPartner.put(sender.getUniqueId(), target.getUniqueId());
        lastPartner.put(target.getUniqueId(), sender.getUniqueId());

        // Clickable name components
        Component senderName = Component.text("§e" + sender.getUsername())
                .clickEvent(ClickEvent.suggestCommand("/dm " + sender.getUsername() + " "))
                .hoverEvent(HoverEvent.showText(Component.text("§7Antworten")));
        Component targetName = Component.text("§e" + target.getUsername())
                .clickEvent(ClickEvent.suggestCommand("/dm " + target.getUsername() + " "))
                .hoverEvent(HoverEvent.showText(Component.text("§7Antworten")));

        Component toTarget = Component.text(PREFIX + "§7Von ")
                .append(senderName)
                .append(Component.text("§7: §f" + message));
        Component toSender = Component.text(PREFIX + "§7An ")
                .append(targetName)
                .append(Component.text("§7: §f" + message));

        target.sendMessage(toTarget);
        sender.sendMessage(toSender);

        // Notify SocialSpy holders (permission: hmy.socialspy)
        Component spyMsg = Component.text("§8[SocialSpy] §7")
                .append(senderName).append(Component.text(" §8→ ")).append(targetName)
                .append(Component.text("§7: §f" + message));
        for (Player p : server.getAllPlayers()) {
            if (!p.equals(sender) && !p.equals(target) && p.hasPermission("hmy.socialspy")) {
                p.sendMessage(spyMsg);
            }
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (!isReply && invocation.arguments().length == 1) {
            String prefix = invocation.arguments()[0].toLowerCase();
            return server.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(n -> n.toLowerCase().startsWith(prefix))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
