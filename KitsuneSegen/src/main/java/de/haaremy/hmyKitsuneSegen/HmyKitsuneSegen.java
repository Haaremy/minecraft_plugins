package de.haaremy.hmykitsunesegen;

import org.bukkit.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class HmyKitsuneSegen extends JavaPlugin {

    private GameConfig        gameConfig;
    private GameManager       gameManager;
    private ChestManager      chestManager;
    private ScoreboardManager scoreboardManager;
    private LuckyItem         luckyItem;
    private WorldReset        worldReset;
    private AgbManager        agbManager;
    private HubListener       hubListener;

    // Scanned at startup (sync, on the game world)
    private List<Location> spawnPoints  = new ArrayList<>();
    private List<Location> chestSpots   = new ArrayList<>();

    @Override
    public void onEnable() {
        getLogger().info("KitsuneSegen wird aktiviert…");

        // ── Config ─────────────────────────────────────────────────────────────
        saveDefaultConfig();
        this.gameConfig = new GameConfig(getConfig());

        // ── Core systems ───────────────────────────────────────────────────────
        this.agbManager       = new AgbManager(this);
        this.luckyItem        = new LuckyItem(this);
        this.chestManager     = new ChestManager(this);
        this.scoreboardManager = new ScoreboardManager(this);
        this.gameManager      = new GameManager(this);
        this.worldReset       = new WorldReset(this);
        this.hubListener      = new HubListener(this);

        // ── Plugin messaging (BungeeCord) ──────────────────────────────────────
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        // ── Worlds ─────────────────────────────────────────────────────────────
        ensureWorldLoaded(gameConfig.getHubWorld());
        ensureWorldLoaded(gameConfig.getGameWorld());

        // ── Block scanning (done async, applied on main thread) ────────────────
        World gameWorld = Bukkit.getWorld(gameConfig.getGameWorld());
        if (gameWorld != null) {
            getServer().getScheduler().runTask(this, () -> {
                getLogger().info("Scanne Spielwelt nach Spawnpunkten und Truhen…");
                spawnPoints = ChestManager.findBlocks(gameWorld, gameConfig.getSpawnBlock(), true);
                chestSpots  = ChestManager.findBlocks(gameWorld, gameConfig.getChestSpawnBlock(), false);
                getLogger().info("Gefunden: " + spawnPoints.size() + " Spawnpunkte, "
                        + chestSpots.size() + " Truppenplätze.");
            });
        }

        // ── Event listeners ────────────────────────────────────────────────────
        getServer().getPluginManager().registerEvents(hubListener, this);
        getServer().getPluginManager().registerEvents(new GameListener(this),  this);
        getServer().getPluginManager().registerEvents(new PlayerChestClick(this), this);

        // ── Commands ───────────────────────────────────────────────────────────
        ComGame comGame = new ComGame(this);
        var cmd = getCommand("game");
        if (cmd != null) {
            cmd.setExecutor(comGame);
            cmd.setTabCompleter(comGame);
        }
        var agbCmd = getCommand("agb");
        if (agbCmd != null) agbCmd.setExecutor(new ComAgb(this));

        getLogger().info("KitsuneSegen aktiviert!");
    }

    @Override
    public void onDisable() {
        scoreboardManager.stopUpdating();
        getLogger().info("KitsuneSegen deaktiviert.");
    }

    // ── Getters ────────────────────────────────────────────────────────────────

    public GameConfig        getGameConfig()         { return gameConfig; }
    public GameManager       getGameManager()        { return gameManager; }
    public ChestManager      getChestManager()       { return chestManager; }
    public ScoreboardManager getScoreboardManager()  { return scoreboardManager; }
    public LuckyItem         getLuckItem()           { return luckyItem; }
    public WorldReset        getWorldReset()         { return worldReset; }
    public AgbManager        getAgbManager()         { return agbManager; }
    public HubListener       getHubListener()        { return hubListener; }

    public List<Location>  getSpawnPoints()      { return spawnPoints; }
    public List<Location>  getChestSpots()       { return chestSpots; }

    // ── Utilities ──────────────────────────────────────────────────────────────

    private void ensureWorldLoaded(String name) {
        if (Bukkit.getWorld(name) == null) {
            getLogger().info("Lade Welt: " + name);
            Bukkit.createWorld(new WorldCreator(name));
        }
    }
}
