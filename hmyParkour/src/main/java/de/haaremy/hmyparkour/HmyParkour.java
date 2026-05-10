package de.haaremy.hmyparkour;

import org.bukkit.plugin.java.JavaPlugin;

public class HmyParkour extends JavaPlugin {

    private static HmyParkour instance;
    private ParkourManager parkourManager;
    private ParkourScoreboard parkourScoreboard;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        // Manager initialisieren
        this.parkourManager = new ParkourManager(this);

        // Listener registrieren
        getServer().getPluginManager().registerEvents(new ParkourListener(this, parkourManager), this);

        // Command registrieren
        ParkourCommand parkourCommand = new ParkourCommand(this, parkourManager);
        getCommand("pk").setExecutor(parkourCommand);
        getCommand("pk").setTabCompleter(parkourCommand);

        // Scoreboard-Updater starten
        this.parkourScoreboard = new ParkourScoreboard(this, parkourManager);
        parkourScoreboard.start();

        getLogger().info("hmyParkour erfolgreich aktiviert!");
    }

    @Override
    public void onDisable() {
        if (parkourScoreboard != null) {
            parkourScoreboard.cancel();
        }
        if (parkourManager != null) {
            parkourManager.shutdown();
        }
        getLogger().info("hmyParkour deaktiviert.");
    }

    public static HmyParkour getInstance() {
        return instance;
    }

    public ParkourManager getParkourManager() {
        return parkourManager;
    }
}
