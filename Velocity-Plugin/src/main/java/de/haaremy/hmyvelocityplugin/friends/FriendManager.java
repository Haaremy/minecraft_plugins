package de.haaremy.hmyvelocityplugin.friends;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Verwaltet Freundeslisten, ausstehende Anfragen und Follow-Sessions.
 * friends.json:  { "uuid": ["uuid2", "uuid3", ...] }
 * requests.json: { "receiverUUID": ["senderUUID", ...] }
 */
public class FriendManager {

    private final Path friendsFile;
    private final Path requestsFile;
    private final Logger logger;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /** uuid → set of friend uuids (mutual) */
    private final Map<UUID, Set<UUID>> friends = new ConcurrentHashMap<>();
    /** receiver → set of senders (pending requests) */
    private final Map<UUID, Set<UUID>> pendingRequests = new ConcurrentHashMap<>();
    /** follower → followed (volatile, not persisted) */
    private final Map<UUID, UUID> follows = new ConcurrentHashMap<>();

    public FriendManager(Path dataDirectory, Logger logger) {
        this.friendsFile  = dataDirectory.resolve("friends.json");
        this.requestsFile = dataDirectory.resolve("friend_requests.json");
        this.logger       = logger;
        load();
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private void load() {
        loadMap(friendsFile,  friends);
        loadMap(requestsFile, pendingRequests);
    }

    private void loadMap(Path file, Map<UUID, Set<UUID>> map) {
        if (!Files.exists(file)) return;
        try {
            Type type = new TypeToken<Map<String, List<String>>>(){}.getType();
            Map<String, List<String>> raw = gson.fromJson(Files.readString(file), type);
            if (raw != null) raw.forEach((k, v) -> {
                Set<UUID> set = ConcurrentHashMap.newKeySet();
                v.forEach(s -> set.add(UUID.fromString(s)));
                map.put(UUID.fromString(k), set);
            });
        } catch (Exception e) {
            logger.error("Fehler beim Laden von {}: {}", file.getFileName(), e.getMessage());
        }
    }

    private void saveMap(Path file, Map<UUID, Set<UUID>> map) {
        Map<String, List<String>> raw = new HashMap<>();
        map.forEach((k, v) -> {
            List<String> list = new ArrayList<>();
            v.forEach(u -> list.add(u.toString()));
            raw.put(k.toString(), list);
        });
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, gson.toJson(raw));
        } catch (IOException e) {
            logger.error("Fehler beim Speichern von {}: {}", file.getFileName(), e.getMessage());
        }
    }

    // ── Friend Logic ──────────────────────────────────────────────────────────

    /** Sendet eine Freundschaftsanfrage von sender an receiver.
     *  Wenn der receiver bereits sender als Anfrage hat, wird direkt angenommen. */
    public FriendRequestResult sendRequest(UUID sender, UUID receiver) {
        if (sender.equals(receiver)) return FriendRequestResult.SELF;
        if (areFriends(sender, receiver)) return FriendRequestResult.ALREADY_FRIENDS;
        if (hasPendingRequest(receiver, sender)) {
            // Receiver already sent to sender → auto-accept
            acceptRequest(sender, receiver);
            return FriendRequestResult.ACCEPTED;
        }
        pendingRequests.computeIfAbsent(receiver, k -> ConcurrentHashMap.newKeySet()).add(sender);
        saveMap(requestsFile, pendingRequests);
        return FriendRequestResult.REQUEST_SENT;
    }

    public boolean acceptRequest(UUID receiver, UUID sender) {
        Set<UUID> requests = pendingRequests.get(receiver);
        if (requests == null || !requests.remove(sender)) return false;
        if (requests.isEmpty()) pendingRequests.remove(receiver);

        friends.computeIfAbsent(receiver, k -> ConcurrentHashMap.newKeySet()).add(sender);
        friends.computeIfAbsent(sender,   k -> ConcurrentHashMap.newKeySet()).add(receiver);

        saveMap(requestsFile, pendingRequests);
        saveMap(friendsFile,  friends);
        return true;
    }

    public boolean denyRequest(UUID receiver, UUID sender) {
        Set<UUID> requests = pendingRequests.get(receiver);
        if (requests == null || !requests.remove(sender)) return false;
        if (requests.isEmpty()) pendingRequests.remove(receiver);
        saveMap(requestsFile, pendingRequests);
        return true;
    }

    public boolean removeFriend(UUID a, UUID b) {
        boolean changed = false;
        Set<UUID> aFriends = friends.get(a);
        if (aFriends != null) changed = aFriends.remove(b) || changed;
        Set<UUID> bFriends = friends.get(b);
        if (bFriends != null) changed = bFriends.remove(a) || changed;
        if (changed) saveMap(friendsFile, friends);
        return changed;
    }

    public boolean areFriends(UUID a, UUID b) {
        Set<UUID> aFriends = friends.get(a);
        return aFriends != null && aFriends.contains(b);
    }

    public boolean hasPendingRequest(UUID receiver, UUID sender) {
        Set<UUID> reqs = pendingRequests.get(receiver);
        return reqs != null && reqs.contains(sender);
    }

    public Set<UUID> getFriends(UUID uuid) {
        return Collections.unmodifiableSet(friends.getOrDefault(uuid, Collections.emptySet()));
    }

    public Set<UUID> getPendingRequests(UUID receiver) {
        return Collections.unmodifiableSet(pendingRequests.getOrDefault(receiver, Collections.emptySet()));
    }

    // ── Follow ────────────────────────────────────────────────────────────────

    public void setFollow(UUID follower, UUID followed) {
        follows.put(follower, followed);
    }

    public void clearFollow(UUID follower) {
        follows.remove(follower);
    }

    public Optional<UUID> getFollowed(UUID follower) {
        return Optional.ofNullable(follows.get(follower));
    }

    /** Returns all players that are currently following the given player. */
    public List<UUID> getFollowers(UUID followed) {
        List<UUID> result = new ArrayList<>();
        follows.forEach((follower, f) -> { if (f.equals(followed)) result.add(follower); });
        return result;
    }

    // ── Enums ─────────────────────────────────────────────────────────────────

    public enum FriendRequestResult { REQUEST_SENT, ACCEPTED, ALREADY_FRIENDS, SELF }
}
