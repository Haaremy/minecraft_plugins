package de.haaremy.mc.hmyai;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Embedded HTTP server (localhost only) that exposes Minecraft data and actions
 * to the MCP server running as a Node.js subprocess.
 *
 * All endpoints require the X-API-Key header.
 * Mutating operations (commands, messages) are dispatched to the main thread.
 */
public class MinecraftApiServer {

    private final HmyAI plugin;
    private final String apiKey;
    private HttpServer server;
    private final Gson gson = new Gson();

    public MinecraftApiServer(HmyAI plugin, int port, String apiKey) {
        this.plugin = plugin;
        this.apiKey  = apiKey;
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
            server.setExecutor(Executors.newCachedThreadPool());

            server.createContext("/tool/command", this::handleCommand);
            server.createContext("/tool/server",  this::handleServer);
            server.createContext("/tool/players", this::handlePlayers);
            server.createContext("/tool/player",  this::handlePlayer);
            server.createContext("/tool/world",   this::handleWorld);
            server.createContext("/tool/block",   this::handleBlock);
            server.createContext("/tool/message", this::handleMessage);
        } catch (IOException e) {
            plugin.getLogger().severe("Konnte HTTP-Server nicht starten: " + e.getMessage());
        }
    }

    public void start() {
        if (server != null) server.start();
    }

    public void stop() {
        if (server != null) server.stop(0);
    }

    // ── Auth helper ───────────────────────────────────────────────────────────

    private boolean authorized(HttpExchange ex) {
        String key = ex.getRequestHeaders().getFirst("X-API-Key");
        return apiKey.equals(key);
    }

    private void send(HttpExchange ex, int status, Object body) throws IOException {
        byte[] bytes = gson.toJson(body).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private void deny(HttpExchange ex) throws IOException {
        send(ex, 403, Map.of("error", "Forbidden"));
    }

    private String readBody(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    // Run a task on the main Bukkit thread and wait for the result
    private <T> T onMain(java.util.concurrent.Callable<T> task) throws Exception {
        CompletableFuture<T> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(plugin, () -> {
            try { future.complete(task.call()); }
            catch (Exception e) { future.completeExceptionally(e); }
        });
        return future.get(10, TimeUnit.SECONDS);
    }

    // ── Endpoints ─────────────────────────────────────────────────────────────

    private void handleCommand(HttpExchange ex) throws IOException {
        if (!authorized(ex)) { deny(ex); return; }
        try {
            String body = readBody(ex);
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            String command = json.get("command").getAsString();

            boolean ok = onMain(() ->
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));

            send(ex, 200, Map.of("success", ok, "command", command));
        } catch (Exception e) {
            send(ex, 500, Map.of("error", e.getMessage()));
        }
    }

    private void handleServer(HttpExchange ex) throws IOException {
        if (!authorized(ex)) { deny(ex); return; }
        try {
            Map<String, Object> info = onMain(() -> {
                Map<String, Object> m = new HashMap<>();
                m.put("version",    Bukkit.getVersion());
                m.put("motd",       Bukkit.getMotd());
                m.put("online",     Bukkit.getOnlinePlayers().size());
                m.put("max",        Bukkit.getMaxPlayers());
                m.put("worlds",     Bukkit.getWorlds().stream().map(World::getName).toList());
                // TPS (Paper API)
                try {
                    double[] tps = Bukkit.getServer().getTPS();
                    m.put("tps", List.of(tps[0], tps[1], tps[2]));
                } catch (Exception ignored) { m.put("tps", "N/A"); }
                return m;
            });
            send(ex, 200, info);
        } catch (Exception e) {
            send(ex, 500, Map.of("error", e.getMessage()));
        }
    }

    private void handlePlayers(HttpExchange ex) throws IOException {
        if (!authorized(ex)) { deny(ex); return; }
        try {
            List<Map<String, Object>> players = onMain(() ->
                Bukkit.getOnlinePlayers().stream().map(p -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("name",  p.getName());
                    m.put("uuid",  p.getUniqueId().toString());
                    m.put("world", p.getWorld().getName());
                    m.put("x",     Math.round(p.getLocation().getX()));
                    m.put("y",     Math.round(p.getLocation().getY()));
                    m.put("z",     Math.round(p.getLocation().getZ()));
                    return m;
                }).toList()
            );
            send(ex, 200, players);
        } catch (Exception e) {
            send(ex, 500, Map.of("error", e.getMessage()));
        }
    }

    private void handlePlayer(HttpExchange ex) throws IOException {
        if (!authorized(ex)) { deny(ex); return; }
        String query = ex.getRequestURI().getQuery();
        String name  = parseQueryParam(query, "name");
        if (name == null) { send(ex, 400, Map.of("error", "name required")); return; }
        try {
            Map<String, Object> info = onMain(() -> {
                Player p = Bukkit.getPlayerExact(name);
                if (p == null) return Map.of("error", "Player not found");
                Map<String, Object> m = new HashMap<>();
                m.put("name",      p.getName());
                m.put("uuid",      p.getUniqueId().toString());
                m.put("world",     p.getWorld().getName());
                m.put("x",         p.getLocation().getX());
                m.put("y",         p.getLocation().getY());
                m.put("z",         p.getLocation().getZ());
                m.put("health",    p.getHealth());
                m.put("maxHealth", p.getMaxHealth());
                m.put("gamemode",  p.getGameMode().name());
                m.put("level",     p.getLevel());
                m.put("exp",       p.getExp());
                m.put("foodLevel", p.getFoodLevel());
                m.put("ping",      p.getPing());
                // Summarise inventory (non-air items)
                long items = java.util.Arrays.stream(p.getInventory().getContents())
                        .filter(i -> i != null && i.getType() != org.bukkit.Material.AIR)
                        .count();
                m.put("inventoryItems", items);
                return m;
            });
            send(ex, 200, info);
        } catch (Exception e) {
            send(ex, 500, Map.of("error", e.getMessage()));
        }
    }

    private void handleWorld(HttpExchange ex) throws IOException {
        if (!authorized(ex)) { deny(ex); return; }
        String query     = ex.getRequestURI().getQuery();
        String worldName = parseQueryParam(query, "world");
        try {
            Map<String, Object> info = onMain(() -> {
                World world = worldName != null
                        ? Bukkit.getWorld(worldName)
                        : Bukkit.getWorlds().get(0);
                if (world == null) return Map.of("error", "World not found");
                Map<String, Object> m = new HashMap<>();
                m.put("name",        world.getName());
                m.put("time",        world.getTime());
                m.put("fullTime",    world.getFullTime());
                m.put("weather",     world.hasStorm() ? "storm" : world.isThundering() ? "thunder" : "clear");
                m.put("difficulty",  world.getDifficulty().name());
                m.put("seed",        world.getSeed());
                m.put("environment", world.getEnvironment().name());
                return m;
            });
            send(ex, 200, info);
        } catch (Exception e) {
            send(ex, 500, Map.of("error", e.getMessage()));
        }
    }

    private void handleBlock(HttpExchange ex) throws IOException {
        if (!authorized(ex)) { deny(ex); return; }
        String query = ex.getRequestURI().getQuery();
        try {
            int    x     = Integer.parseInt(parseQueryParam(query, "x"));
            int    y     = Integer.parseInt(parseQueryParam(query, "y"));
            int    z     = Integer.parseInt(parseQueryParam(query, "z"));
            String wName = parseQueryParam(query, "world");

            Map<String, Object> info = onMain(() -> {
                World world = wName != null ? Bukkit.getWorld(wName) : Bukkit.getWorlds().get(0);
                if (world == null) return Map.of("error", "World not found");
                var block = world.getBlockAt(x, y, z);
                Map<String, Object> m = new HashMap<>();
                m.put("type",      block.getType().name());
                m.put("x",         x);
                m.put("y",         y);
                m.put("z",         z);
                m.put("world",     world.getName());
                m.put("blockData", block.getBlockData().getAsString());
                return m;
            });
            send(ex, 200, info);
        } catch (Exception e) {
            send(ex, 400, Map.of("error", "Invalid parameters: " + e.getMessage()));
        }
    }

    private void handleMessage(HttpExchange ex) throws IOException {
        if (!authorized(ex)) { deny(ex); return; }
        try {
            String body    = readBody(ex);
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            String target  = json.get("target").getAsString();
            String message = json.get("message").getAsString();

            Map<String, Object> result = onMain(() -> {
                if ("@a".equals(target)) {
                    Bukkit.broadcastMessage("§7[AI] §f" + message);
                    return Map.of("sent", true, "recipients", Bukkit.getOnlinePlayers().size());
                }
                Player p = Bukkit.getPlayerExact(target);
                if (p == null) return Map.of("sent", false, "error", "Player not online");
                p.sendMessage("§7[AI] §f" + message);
                return Map.of("sent", true, "recipient", target);
            });
            send(ex, 200, result);
        } catch (Exception e) {
            send(ex, 500, Map.of("error", e.getMessage()));
        }
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private String parseQueryParam(String query, String key) {
        if (query == null) return null;
        for (String part : query.split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) {
                return java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}
