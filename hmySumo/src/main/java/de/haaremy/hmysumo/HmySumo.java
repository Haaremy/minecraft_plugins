package de.haaremy.hmysumo;

import de.haaremy.hmycore.HmyCore;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.java.JavaPlugin;

public class HmySumo extends JavaPlugin {

    private static HmySumo instance;

    private SumoManager sumoManager;
    private SumoElo sumoElo;
    private SumoArena sumoArena;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @Override
    public void onEnable() {
        instance = this;

        // hmyCore pruefen
        if (HmyCore.getInstance() == null) {
            getLogger().severe("hmyCore nicht gefunden! Plugin wird deaktiviert.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Manager initialisieren
        this.sumoElo = new SumoElo(this);
        this.sumoArena = new SumoArena(this);
        this.sumoManager = new SumoManager(this);

        // Listener registrieren
        getServer().getPluginManager().registerEvents(new SumoListener(this), this);

        // Command registrieren
        getCommand("sumo").setExecutor(new SumoCommand(this));
        getCommand("sumo").setTabCompleter(new SumoCommand(this));

        getLogger().info("hmySumo erfolgreich aktiviert!");
    }

    @Override
    public void onDisable() {
        if (sumoManager != null) {
            sumoManager.shutdown();
        }
        if (sumoElo != null) {
            sumoElo.shutdown();
        }
        getLogger().info("hmySumo deaktiviert.");
    }

    public static HmySumo getInstance() {
        return instance;
    }

    public MiniMessage getMiniMessage() {
        return miniMessage;
    }

    public SumoManager getSumoManager() {
        return sumoManager;
    }

    public SumoElo getSumoElo() {
        return sumoElo;
    }

    public SumoArena getSumoArena() {
        return sumoArena;
    }
}
