package de.haaremy.hmyvelocityplugin.friends;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.haaremy.hmyvelocityplugin.PlayerTracker;
import de.haaremy.hmyvelocityplugin.friends.FriendManager.FriendRequestResult;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * /friend <add|accept|deny|remove|list|join|follow|unfollow> [player]
 */
public class ComFriend implements SimpleCommand {

    private static final String PREFIX = "§8[§6Freunde§8] §r";

    private final ProxyServer server;
    private final FriendManager friendManager;
    private final PlayerTracker tracker;

    public ComFriend(ProxyServer server, FriendManager friendManager, PlayerTracker tracker) {
        this.server        = server;
        this.friendManager = friendManager;
        this.tracker       = tracker;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!(source instanceof Player player)) {
            source.sendMessage(Component.text("§cNur Spieler."));
            return;
        }

        String[] args = invocation.arguments();
        if (args.length == 0) {
            sendUsage(player);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "add"      -> handleAdd(player, args);
            case "accept"   -> handleAccept(player, args);
            case "deny"     -> handleDeny(player, args);
            case "remove"   -> handleRemove(player, args);
            case "list"     -> handleList(player);
            case "join"     -> handleJoin(player, args);
            case "follow"   -> handleFollow(player, args);
            case "unfollow" -> handleUnfollow(player);
            default         -> sendUsage(player);
        }
    }

    // ── /friend add <player> ──────────────────────────────────────────────────

    private void handleAdd(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(msg("§cVerwendung: §e/friend add <Spieler>")); return; }
        Optional<Player> target = server.getPlayer(args[1]);
        if (target.isEmpty()) { player.sendMessage(msg("§c" + args[1] + " §7ist nicht online.")); return; }

        FriendRequestResult result = friendManager.sendRequest(player.getUniqueId(), target.get().getUniqueId());
        switch (result) {
            case REQUEST_SENT -> {
                player.sendMessage(msg("§7Freundschaftsanfrage an §e" + target.get().getUsername() + " §7gesendet."));
                target.get().sendMessage(msg("§e" + player.getUsername()
                        + " §7möchte dein Freund sein! §a/friend accept " + player.getUsername()
                        + " §7oder §c/friend deny " + player.getUsername()));
            }
            case ACCEPTED -> {
                player.sendMessage(msg("§aDu bist jetzt mit §e" + target.get().getUsername() + " §abefreundet!"));
                target.get().sendMessage(msg("§e" + player.getUsername() + " §ahat deine Anfrage angenommen!"));
            }
            case ALREADY_FRIENDS -> player.sendMessage(msg("§7Ihr seid bereits befreundet."));
            case SELF            -> player.sendMessage(msg("§cDu kannst dir nicht selbst eine Anfrage senden."));
        }
    }

    // ── /friend accept <player> ───────────────────────────────────────────────

    private void handleAccept(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(msg("§cVerwendung: §e/friend accept <Spieler>")); return; }
        UUID senderUUID = findUUID(args[1]);
        if (senderUUID == null) { player.sendMessage(msg("§cSpieler §e" + args[1] + " §cnicht gefunden.")); return; }

        if (friendManager.acceptRequest(player.getUniqueId(), senderUUID)) {
            player.sendMessage(msg("§aDu bist jetzt mit §e" + args[1] + " §abefreundet!"));
            server.getPlayer(senderUUID).ifPresent(s ->
                    s.sendMessage(msg("§e" + player.getUsername() + " §ahat deine Freundschaftsanfrage angenommen!")));
        } else {
            player.sendMessage(msg("§cKeine ausstehende Anfrage von §e" + args[1] + "§c."));
        }
    }

    // ── /friend deny <player> ─────────────────────────────────────────────────

    private void handleDeny(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(msg("§cVerwendung: §e/friend deny <Spieler>")); return; }
        UUID senderUUID = findUUID(args[1]);
        if (senderUUID == null) { player.sendMessage(msg("§cSpieler §e" + args[1] + " §cnicht gefunden.")); return; }

        if (friendManager.denyRequest(player.getUniqueId(), senderUUID)) {
            player.sendMessage(msg("§7Anfrage von §e" + args[1] + " §7abgelehnt."));
        } else {
            player.sendMessage(msg("§cKeine ausstehende Anfrage von §e" + args[1] + "§c."));
        }
    }

    // ── /friend remove <player> ───────────────────────────────────────────────

    private void handleRemove(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(msg("§cVerwendung: §e/friend remove <Spieler>")); return; }
        UUID targetUUID = findUUID(args[1]);
        if (targetUUID == null) { player.sendMessage(msg("§cSpieler §e" + args[1] + " §cnicht gefunden.")); return; }

        if (friendManager.removeFriend(player.getUniqueId(), targetUUID)) {
            player.sendMessage(msg("§7Du hast §e" + args[1] + " §7aus deiner Freundesliste entfernt."));
        } else {
            player.sendMessage(msg("§e" + args[1] + " §7ist nicht dein Freund."));
        }
    }

    // ── /friend list ──────────────────────────────────────────────────────────

    private void handleList(Player player) {
        Set<UUID> friendUUIDs = friendManager.getFriends(player.getUniqueId());
        Set<UUID> pendingUUIDs = friendManager.getPendingRequests(player.getUniqueId());

        player.sendMessage(Component.text("§8§m──────────────────────────"));
        player.sendMessage(Component.text("§6§l Deine Freundesliste"));
        player.sendMessage(Component.text("§8§m──────────────────────────"));

        if (friendUUIDs.isEmpty()) {
            player.sendMessage(Component.text("§7Noch keine Freunde. §e/friend add <Spieler>"));
        } else {
            // Group by server
            java.util.Map<String, List<String>> byServer = new java.util.LinkedHashMap<>();
            byServer.put("§aOnline", new java.util.ArrayList<>());
            byServer.put("§7Offline", new java.util.ArrayList<>());

            for (UUID fUUID : friendUUIDs) {
                Optional<Player> online = server.getPlayer(fUUID);
                if (online.isPresent()) {
                    String srv = tracker.getPlayerServer(fUUID)
                            .map(s -> " §8[§b" + s + "§8]").orElse("");
                    byServer.get("§aOnline").add("§a● §f" + online.get().getUsername() + srv);
                } else {
                    byServer.get("§7Offline").add("§7○ §8Offline-Spieler §7(UUID: " + fUUID.toString().substring(0, 8) + "...)");
                }
            }

            byServer.forEach((status, names) -> {
                if (!names.isEmpty()) {
                    player.sendMessage(Component.text(status + " §8(" + names.size() + ")"));
                    names.forEach(n -> player.sendMessage(Component.text("  " + n)));
                }
            });
        }

        if (!pendingUUIDs.isEmpty()) {
            player.sendMessage(Component.text("§e§l Ausstehende Anfragen §8(" + pendingUUIDs.size() + ")"));
            for (UUID req : pendingUUIDs) {
                String name = server.getPlayer(req)
                        .map(Player::getUsername)
                        .orElse("§8Unbekannt (" + req.toString().substring(0, 8) + "...)");
                player.sendMessage(Component.text("  §e→ §f" + name
                        + " §7| §a/friend accept " + name + " §7| §c/friend deny " + name));
            }
        }

        player.sendMessage(Component.text("§8§m──────────────────────────"));
    }

    // ── /friend join <player> ─────────────────────────────────────────────────

    private void handleJoin(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(msg("§cVerwendung: §e/friend join <Spieler>")); return; }
        Optional<Player> target = server.getPlayer(args[1]);
        if (target.isEmpty()) { player.sendMessage(msg("§c" + args[1] + " §7ist nicht online.")); return; }
        if (!friendManager.areFriends(player.getUniqueId(), target.get().getUniqueId())) {
            player.sendMessage(msg("§cIhr seid keine Freunde.")); return;
        }

        String targetServer = tracker.getPlayerServer(target.get().getUniqueId()).orElse(null);
        if (targetServer == null) { player.sendMessage(msg("§cServer von §e" + args[1] + " §cnicht gefunden.")); return; }

        server.getServer(targetServer).ifPresent(s -> {
            player.createConnectionRequest(s).connectWithIndication();
            player.sendMessage(msg("§7Du verbindest dich mit §e" + args[1] + " §7auf §b" + targetServer + "§7..."));
        });
    }

    // ── /friend follow <player> ───────────────────────────────────────────────

    private void handleFollow(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(msg("§cVerwendung: §e/friend follow <Spieler>")); return; }
        Optional<Player> target = server.getPlayer(args[1]);
        if (target.isEmpty()) { player.sendMessage(msg("§c" + args[1] + " §7ist nicht online.")); return; }
        if (!friendManager.areFriends(player.getUniqueId(), target.get().getUniqueId())) {
            player.sendMessage(msg("§cIhr seid keine Freunde.")); return;
        }

        friendManager.setFollow(player.getUniqueId(), target.get().getUniqueId());
        player.sendMessage(msg("§7Du folgst jetzt §e" + target.get().getUsername() + "§7. §8(/friend unfollow zum Beenden)"));
        target.get().sendMessage(msg("§e" + player.getUsername() + " §7folgt dir jetzt."));

        // Immediately teleport to their server
        handleJoin(player, args);
    }

    // ── /friend unfollow ──────────────────────────────────────────────────────

    private void handleUnfollow(Player player) {
        friendManager.getFollowed(player.getUniqueId()).ifPresentOrElse(
                followed -> {
                    friendManager.clearFollow(player.getUniqueId());
                    player.sendMessage(msg("§7Du folgst niemandem mehr."));
                    server.getPlayer(followed).ifPresent(f ->
                            f.sendMessage(msg("§e" + player.getUsername() + " §7folgt dir nicht mehr.")));
                },
                () -> player.sendMessage(msg("§7Du folgst gerade niemandem."))
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void sendUsage(Player player) {
        player.sendMessage(Component.text("§8§m──────────────────────────"));
        player.sendMessage(Component.text("§6§l Freundessystem §8– §7Befehle"));
        player.sendMessage(Component.text("§e/friend add §f<Spieler>      §8– §7Anfrage senden"));
        player.sendMessage(Component.text("§e/friend accept §f<Spieler>   §8– §7Anfrage annehmen"));
        player.sendMessage(Component.text("§e/friend deny §f<Spieler>     §8– §7Anfrage ablehnen"));
        player.sendMessage(Component.text("§e/friend remove §f<Spieler>   §8– §7Freund entfernen"));
        player.sendMessage(Component.text("§e/friend list               §8– §7Freundesliste"));
        player.sendMessage(Component.text("§e/friend join §f<Spieler>     §8– §7Zum Freund verbinden"));
        player.sendMessage(Component.text("§e/friend follow §f<Spieler>   §8– §7Freund automatisch folgen"));
        player.sendMessage(Component.text("§e/friend unfollow           §8– §7Folgen beenden"));
        player.sendMessage(Component.text("§8§m──────────────────────────"));
    }

    private Component msg(String text) {
        return Component.text(PREFIX + text);
    }

    /** Resolve player name → UUID (online first, then we just can't do offline without DB) */
    private UUID findUUID(String name) {
        return server.getPlayer(name).map(Player::getUniqueId).orElse(null);
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 1) {
            return List.of("add", "accept", "deny", "remove", "list", "join", "follow", "unfollow");
        }
        if (args.length == 2 && !args[0].equalsIgnoreCase("list") && !args[0].equalsIgnoreCase("unfollow")) {
            return server.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
