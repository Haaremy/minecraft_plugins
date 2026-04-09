package de.haaremy.mc.hmyai;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Persists per-player Claude session IDs across plugin restarts.
 * Session IDs allow Claude to continue a conversation via --resume.
 */
public class SessionManager {

    private final File file;
    private final Gson gson = new Gson();
    private final Map<UUID, String> sessions = new HashMap<>();

    public SessionManager(File dataFolder) {
        this.file = new File(dataFolder, "sessions.json");
        load();
    }

    public String getSession(UUID player) {
        return sessions.get(player);
    }

    public void setSession(UUID player, String sessionId) {
        sessions.put(player, sessionId);
    }

    public void clearSession(UUID player) {
        sessions.remove(player);
    }

    public void save() {
        try {
            // Convert UUID keys to String for JSON serialisation
            Map<String, String> serialisable = new HashMap<>();
            sessions.forEach((k, v) -> serialisable.put(k.toString(), v));
            Files.writeString(file.toPath(), gson.toJson(serialisable));
        } catch (IOException e) {
            // Non-fatal – sessions just won't survive restart
        }
    }

    private void load() {
        if (!file.exists()) return;
        try {
            String json = Files.readString(file.toPath());
            Map<String, String> raw = gson.fromJson(json,
                    new TypeToken<Map<String, String>>() {}.getType());
            if (raw != null) {
                raw.forEach((k, v) -> sessions.put(UUID.fromString(k), v));
            }
        } catch (Exception ignored) {
            // Corrupt file → start fresh
        }
    }
}
