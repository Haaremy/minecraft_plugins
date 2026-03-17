package de.haaremy.hmyvelocityplugin;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.player.TabList;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.util.GameProfile;
import de.haaremy.hmyvelocityplugin.friends.FriendManager;
import net.kyori.adventure.text.Component;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Verwaltet die Tab-Liste netzwerkweit über Velocity.
 *
 * Lobby:   Zeigt alle Spieler gruppiert nach Server ([Lobby], [Survival], …).
 *          Freunde auf anderen Servern erscheinen immer am Ende (cyan, hervorgehoben).
 *          Bei >80 Einträgen werden Lobby-Spieler priorisiert; Freunde behalten ihre Slots.
 *
 * Andere:  Zeigt nur Spieler des eigenen Servers (wie HmyTab bisher).
 */
public class VelocityTabManager {

    private static final int    MAX_ENTRIES  = 80;
    private static final String LOBBY_SERVER = "lobby";

    private final Object        plugin;
    private final ProxyServer   proxy;
    private final FriendManager friendManager;
    private final PlayerTracker playerTracker;
    private final LuckPerms     luckPerms;

    // LuckPerms caches (populated async on login)
    private final Map<UUID, String>  prefixCache = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> weightCache = new ConcurrentHashMap<>();

    public VelocityTabManager(Object plugin, ProxyServer proxy,
                               FriendManager friendManager,
                               PlayerTracker playerTracker,
                               LuckPerms luckPerms) {
        this.plugin        = plugin;
        this.proxy         = proxy;
        this.friendManager = friendManager;
        this.playerTracker = playerTracker;
        this.luckPerms     = luckPerms;
    }

    /** Start the periodic refresh task (every 2 seconds). */
    public void startTask() {
        proxy.getScheduler()
             .buildTask(plugin, () -> proxy.getAllPlayers().forEach(this::updateTabForPlayer))
             .repeat(2, TimeUnit.SECONDS)
             .schedule();
    }

    // ── Events ────────────────────────────────────────────────────────────────

    @Subscribe
    public void onLogin(PostLoginEvent event) {
        loadLuckPermsData(event.getPlayer().getUniqueId());
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        Player joined    = event.getPlayer();
        String newServer = event.getServer().getServerInfo().getName();
        String prevServer = event.getPreviousServer()
                .map(s -> s.getServerInfo().getName()).orElse(null);

        // Lobby-Nachricht: wenn Spieler die Lobby verlässt → alle Lobby-Spieler informieren
        if (prevServer != null && isLobby(prevServer) && !isLobby(newServer)) {
            Component msg = Component.text("§8» §e" + joined.getUsername()
                    + " §8→ §a" + capitalize(newServer));
            for (Player lobbyPlayer : proxy.getAllPlayers()) {
                String srv = playerTracker.getPlayerServer(lobbyPlayer.getUniqueId()).orElse("");
                if (isLobby(srv) && !lobbyPlayer.getUniqueId().equals(joined.getUniqueId())) {
                    lobbyPlayer.sendMessage(msg);
                }
            }
        }

        // Netz-Join-Nachricht: Spieler betritt das Netzwerk zum ersten Mal (vorheriger Server = leer)
        if (prevServer == null && isLobby(newServer)) {
            Component joinMsg = Component.text("§8» §e" + joined.getUsername() + " §7hat das Netzwerk betreten.");
            for (Player lobbyPlayer : proxy.getAllPlayers()) {
                String srv = playerTracker.getPlayerServer(lobbyPlayer.getUniqueId()).orElse("");
                if (isLobby(srv) && !lobbyPlayer.getUniqueId().equals(joined.getUniqueId())) {
                    lobbyPlayer.sendMessage(joinMsg);
                }
            }
        }

        // Delay ~300 ms so the backend's initial PlayerList packets arrive first,
        // then we override all display names.
        proxy.getScheduler().buildTask(plugin, () -> {
            updateTabForPlayer(joined);
            for (Player other : proxy.getAllPlayers()) {
                if (other.getUniqueId().equals(joined.getUniqueId())) continue;
                String otherServer = playerTracker.getPlayerServer(other.getUniqueId()).orElse("");
                // Lobby players need a full rebuild; same-server players need the new entry
                if (isLobby(otherServer) || otherServer.equals(newServer)) {
                    updateTabForPlayer(other);
                }
            }
        }).delay(300, TimeUnit.MILLISECONDS).schedule();
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        prefixCache.remove(uuid);
        weightCache.remove(uuid);

        // Remove entry from everyone's tab and rebuild lobby views
        proxy.getScheduler().buildTask(plugin, () -> {
            for (Player p : proxy.getAllPlayers()) {
                p.getTabList().removeEntry(uuid);
                if (isLobby(playerTracker.getPlayerServer(p.getUniqueId()).orElse(""))) {
                    updateTabForPlayer(p);
                }
            }
        }).delay(100, TimeUnit.MILLISECONDS).schedule();
    }

    // ── LuckPerms data loading ────────────────────────────────────────────────

    private void loadLuckPermsData(UUID uuid) {
        luckPerms.getUserManager().loadUser(uuid).thenAccept(user -> {
            if (user == null) return;

            String prefix = "";
            Group group = luckPerms.getGroupManager().getGroup(user.getPrimaryGroup());
            if (group != null && group.getCachedData().getMetaData().getPrefix() != null) {
                prefix = group.getCachedData().getMetaData().getPrefix().replace("&", "§");
            }
            prefixCache.put(uuid, prefix);

            String weightStr = user.getCachedData().getMetaData().getMetaValue("weight");
            int weight = 0;
            try { if (weightStr != null) weight = Integer.parseInt(weightStr); }
            catch (NumberFormatException ignored) {}
            weightCache.put(uuid, weight);
        });
    }

    // ── Tab list update ───────────────────────────────────────────────────────

    private void updateTabForPlayer(Player viewer) {
        String  myServer = playerTracker.getPlayerServer(viewer.getUniqueId()).orElse("");
        boolean inLobby  = isLobby(myServer);

        List<TabEntryData> desired     = buildEntries(viewer, myServer, inLobby);
        Set<UUID>          desiredUUIDs = desired.stream().map(e -> e.uuid).collect(Collectors.toSet());
        TabList            tabList     = viewer.getTabList();

        // Remove entries that are no longer in scope
        for (TabListEntry existing : new ArrayList<>(tabList.getEntries())) {
            UUID id = existing.getProfile().getId();
            if (!desiredUUIDs.contains(id)) tabList.removeEntry(id);
        }

        // Add new entries or update existing ones
        for (TabEntryData entry : desired) {
            Optional<TabListEntry> existing = tabList.getEntry(entry.uuid);
            if (existing.isPresent()) {
                existing.get().setDisplayName(entry.displayName);
                existing.get().setLatency(entry.latency);
            } else {
                try {
                    tabList.addEntry(TabListEntry.builder()
                            .tabList(tabList)
                            .profile(entry.profile)
                            .displayName(entry.displayName)
                            .latency(entry.latency)
                            .gameMode(0)
                            .build());
                } catch (Exception ignored) {} // concurrent add guard
            }
        }

        updateHeaderFooter(viewer, myServer, inLobby);
    }

    // ── Entry list builder ────────────────────────────────────────────────────

    private List<TabEntryData> buildEntries(Player viewer, String myServer, boolean inLobby) {
        List<TabEntryData> result = new ArrayList<>();

        if (!inLobby) {
            // Other servers: only same-server players sorted by rank weight
            proxy.getAllPlayers().stream()
                .filter(p -> myServer.equals(playerTracker.getPlayerServer(p.getUniqueId()).orElse("")))
                .sorted(Comparator.comparingInt(
                    (Player p) -> weightCache.getOrDefault(p.getUniqueId(), 0)).reversed())
                .forEach(p -> result.add(playerEntry(p, null)));
            return result;
        }

        // ── Lobby layout ──────────────────────────────────────────────────────

        Set<UUID> friendUUIDs = friendManager.getFriends(viewer.getUniqueId());

        // Lobby players sorted by rank
        List<Player> lobbyPlayers = proxy.getAllPlayers().stream()
            .filter(p -> isLobby(playerTracker.getPlayerServer(p.getUniqueId()).orElse("")))
            .sorted(Comparator.comparingInt(
                (Player p) -> weightCache.getOrDefault(p.getUniqueId(), 0)).reversed())
            .collect(Collectors.toList());

        // Non-lobby, non-friend players grouped by server (insertion-order)
        Map<String, List<Player>> otherServers = new LinkedHashMap<>();
        proxy.getAllPlayers().stream()
            .filter(p -> !isLobby(playerTracker.getPlayerServer(p.getUniqueId()).orElse(""))
                      && !friendUUIDs.contains(p.getUniqueId()))
            .forEach(p -> {
                String srv = playerTracker.getPlayerServer(p.getUniqueId()).orElse("other");
                otherServers.computeIfAbsent(srv, k -> new ArrayList<>()).add(p);
            });

        // Friends on other servers (always shown)
        List<Player> friendsElsewhere = proxy.getAllPlayers().stream()
            .filter(p -> !isLobby(playerTracker.getPlayerServer(p.getUniqueId()).orElse(""))
                      && friendUUIDs.contains(p.getUniqueId()))
            .collect(Collectors.toList());

        // Reserve slots for friends header + friend entries
        int friendSlots = friendsElsewhere.isEmpty() ? 0 : (friendsElsewhere.size() + 1);
        int available   = MAX_ENTRIES - friendSlots;

        // [Lobby]
        if (!lobbyPlayers.isEmpty() && available > 0) {
            result.add(headerEntry("hmy_header_lobby", "§8─── §7[§aLobby§7] §8───"));
            available--;
            for (Player p : lobbyPlayers) {
                if (available <= 0) break;
                result.add(playerEntry(p, null));
                available--;
            }
        }

        // Other servers
        for (Map.Entry<String, List<Player>> srv : otherServers.entrySet()) {
            if (available <= 0) break;
            result.add(headerEntry("hmy_header_" + srv.getKey(),
                    "§8─── §7[§e" + capitalize(srv.getKey()) + "§7] §8───"));
            available--;
            for (Player p : srv.getValue()) {
                if (available <= 0) break;
                result.add(playerEntry(p, null));
                available--;
            }
        }

        // Friends (always appended regardless of available slots)
        if (!friendsElsewhere.isEmpty()) {
            result.add(headerEntry("hmy_header_friends", "§8─── §b✦ §3Freunde §b✦ §8───"));
            for (Player p : friendsElsewhere) {
                String friendSrv = playerTracker.getPlayerServer(p.getUniqueId()).orElse("?");
                String display   = "§b[" + capitalize(friendSrv) + "] §3"
                                 + String.format("%-10s", p.getUsername())
                                 + " §d" + String.format("%-6s", p.getPing() + "ms");
                result.add(new TabEntryData(p.getUniqueId(), p.getGameProfile(),
                        Component.text(display), (int) p.getPing()));
            }
        }

        return result;
    }

    // ── Header / Footer ───────────────────────────────────────────────────────

    private void updateHeaderFooter(Player player, String myServer, boolean inLobby) {
        int count = inLobby
            ? proxy.getPlayerCount()
            : (int) proxy.getAllPlayers().stream()
                .filter(p -> myServer.equals(playerTracker.getPlayerServer(p.getUniqueId()).orElse("")))
                .count();

        String countLine = inLobby
            ? "§7Online Spieler: §d" + count
            : "§7Online Spieler: §d" + count + " §8auf §a" + capitalize(myServer);

        player.getTabList().setHeaderAndFooter(
            Component.text("§l========================\n§7 Willkommen auf\n\n§dMC.HAAREMY.DE\n\n"
                + countLine + "\n§r§l========================"),
            Component.text("§r§l========================\n\n§7Ping: §d"
                + player.getPing() + " ms\n§r§l========================")
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Build a tab entry for a real player. Pass a custom display string, or null to auto-format. */
    private TabEntryData playerEntry(Player p, String customDisplay) {
        String display = customDisplay;
        if (display == null) {
            String prefix = prefixCache.getOrDefault(p.getUniqueId(), "");
            String name   = String.format("%-10s", p.getUsername());
            String ping   = String.format("%-6s", p.getPing() + "ms");
            display = " " + prefix + " §e" + name + " §d" + ping;
        }
        return new TabEntryData(p.getUniqueId(), p.getGameProfile(),
                Component.text(display), (int) p.getPing());
    }

    /** Build a fake header-row entry (group separator). */
    private TabEntryData headerEntry(String id, String text) {
        UUID        uuid    = UUID.nameUUIDFromBytes(id.getBytes(StandardCharsets.UTF_8));
        // Profile name: max 16 chars, must not collide with real player names
        String      profName = id.length() > 16 ? id.substring(id.length() - 16) : id;
        GameProfile profile  = new GameProfile(uuid, profName, List.of());
        return new TabEntryData(uuid, profile, Component.text(text), 0);
    }

    private boolean isLobby(String serverName) {
        return LOBBY_SERVER.equalsIgnoreCase(serverName);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    private record TabEntryData(UUID uuid, GameProfile profile, Component displayName, int latency) {}
}
