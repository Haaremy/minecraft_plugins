package de.haaremy.hmyvelocityplugin.economy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Verwaltet hmyCoins und hmyShards pro Spieler.
 * Daten werden in data/economy.json im Plugin-Verzeichnis gespeichert.
 */
public class CurrencyManager {

    private final Path file;
    private final Logger logger;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private final Map<UUID, long[]> balances = new ConcurrentHashMap<>();
    // balances[uuid] = [coins, shards]

    public CurrencyManager(Path dataDirectory, Logger logger) {
        this.file   = dataDirectory.resolve("economy.json");
        this.logger = logger;
        load();
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private void load() {
        if (!Files.exists(file)) return;
        try {
            Type type = new TypeToken<Map<String, long[]>>(){}.getType();
            Map<String, long[]> data = gson.fromJson(Files.readString(file), type);
            if (data != null) data.forEach((k, v) -> balances.put(UUID.fromString(k), v));
        } catch (Exception e) {
            logger.error("Fehler beim Laden von economy.json: {}", e.getMessage());
        }
    }

    public synchronized void save() {
        Map<String, long[]> data = new HashMap<>();
        balances.forEach((k, v) -> data.put(k.toString(), v));
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, gson.toJson(data));
        } catch (IOException e) {
            logger.error("Fehler beim Speichern von economy.json: {}", e.getMessage());
        }
    }

    // ── API ───────────────────────────────────────────────────────────────────

    public long getCoins(UUID uuid) {
        return balances.getOrDefault(uuid, new long[]{0, 0})[0];
    }

    public long getShards(UUID uuid) {
        return balances.getOrDefault(uuid, new long[]{0, 0})[1];
    }

    public void addCoins(UUID uuid, long amount) {
        long[] bal = balances.computeIfAbsent(uuid, k -> new long[]{0, 0});
        bal[0] += amount;
        save();
    }

    public void addShards(UUID uuid, long amount) {
        long[] bal = balances.computeIfAbsent(uuid, k -> new long[]{0, 0});
        bal[1] += amount;
        save();
    }

    public boolean deductCoins(UUID uuid, long amount) {
        long[] bal = balances.getOrDefault(uuid, new long[]{0, 0});
        if (bal[0] < amount) return false;
        bal[0] -= amount;
        balances.put(uuid, bal);
        save();
        return true;
    }

    public boolean deductShards(UUID uuid, long amount) {
        long[] bal = balances.getOrDefault(uuid, new long[]{0, 0});
        if (bal[1] < amount) return false;
        bal[1] -= amount;
        balances.put(uuid, bal);
        save();
        return true;
    }

    public void setCoins(UUID uuid, long amount) {
        balances.computeIfAbsent(uuid, k -> new long[]{0, 0})[0] = amount;
        save();
    }
}
