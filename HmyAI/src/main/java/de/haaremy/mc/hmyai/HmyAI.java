package de.haaremy.mc.hmyai;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class HmyAI extends JavaPlugin {

    private String apiKey;
    private SessionManager sessionManager;
    private ClaudeProcess claudeProcess;
    private MinecraftApiServer apiServer;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // ── API key ──────────────────────────────────────────────────────────
        File keyFile = new File(getDataFolder(), ".api-key");
        if (keyFile.exists()) {
            try {
                apiKey = Files.readString(keyFile.toPath()).trim();
            } catch (IOException e) {
                apiKey = UUID.randomUUID().toString().replace("-", "");
            }
        } else {
            apiKey = UUID.randomUUID().toString().replace("-", "");
            try {
                getDataFolder().mkdirs();
                Files.writeString(keyFile.toPath(), apiKey);
            } catch (IOException e) {
                getLogger().warning("Konnte API-Key nicht speichern: " + e.getMessage());
            }
        }

        // ── Core services ────────────────────────────────────────────────────
        this.sessionManager = new SessionManager(getDataFolder());
        this.claudeProcess  = new ClaudeProcess(this);

        int port = getConfig().getInt("http-port", 25580);
        this.apiServer = new MinecraftApiServer(this, port, apiKey);
        apiServer.start();

        // ── MCP server setup ─────────────────────────────────────────────────
        setupMcp(port);

        // ── Command ──────────────────────────────────────────────────────────
        var cmd = getCommand("ai");
        if (cmd != null) cmd.setExecutor(new ComAI(this));

        getLogger().info("HmyAI bereit! HTTP-API auf 127.0.0.1:" + port);
    }

    @Override
    public void onDisable() {
        if (apiServer != null) apiServer.stop();
        if (sessionManager != null) sessionManager.save();
        getLogger().info("HmyAI deaktiviert.");
    }

    // ── MCP setup ─────────────────────────────────────────────────────────────

    private void setupMcp(int port) {
        File mcpDir = new File(getDataFolder(), "mcp");
        mcpDir.mkdirs();

        extractResource("mcp/server.js",     new File(mcpDir, "server.js"));
        extractResource("mcp/package.json",  new File(mcpDir, "package.json"));

        // Generate mcp-config.json
        String serverJsPath = new File(mcpDir, "server.js").getAbsolutePath();
        String mcpConfig = """
                {
                  "mcpServers": {
                    "minecraft": {
                      "command": "node",
                      "args": ["%s"],
                      "env": {
                        "MC_API":     "http://127.0.0.1:%d",
                        "MC_API_KEY": "%s"
                      }
                    }
                  }
                }
                """.formatted(serverJsPath.replace("\\", "/"), port, apiKey);

        try {
            Files.writeString(new File(getDataFolder(), "mcp-config.json").toPath(), mcpConfig);
        } catch (IOException e) {
            getLogger().severe("Konnte mcp-config.json nicht schreiben: " + e.getMessage());
            return;
        }

        // npm install (non-blocking, logged)
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            try {
                ProcessBuilder pb = new ProcessBuilder("npm", "install", "--silent");
                pb.directory(mcpDir);
                pb.redirectErrorStream(true);
                Process p = pb.start();
                boolean ok = p.waitFor(60, TimeUnit.SECONDS);
                if (ok && p.exitValue() == 0) {
                    getLogger().info("MCP-Server: npm install erfolgreich.");
                } else {
                    getLogger().warning("MCP-Server: npm install fehlgeschlagen! Bitte manuell in "
                            + mcpDir.getAbsolutePath() + " ausführen.");
                }
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "npm install Fehler: " + e.getMessage(), e);
            }
        });
    }

    private void extractResource(String resourcePath, File target) {
        if (target.exists()) return;
        try (InputStream in = getResource(resourcePath)) {
            if (in == null) {
                getLogger().warning("Ressource nicht gefunden: " + resourcePath);
                return;
            }
            try (OutputStream out = Files.newOutputStream(target.toPath())) {
                in.transferTo(out);
            }
        } catch (IOException e) {
            getLogger().warning("Konnte " + resourcePath + " nicht extrahieren: " + e.getMessage());
        }
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getApiKey()              { return apiKey; }
    public SessionManager getSessionManager() { return sessionManager; }
    public ClaudeProcess getClaudeProcess()   { return claudeProcess; }
}
