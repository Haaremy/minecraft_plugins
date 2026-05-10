package de.haaremy.hmytntrun;

import de.haaremy.hmycore.HmyCore;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.java.JavaPlugin;

public class HmyTNTRun extends JavaPlugin {

    private static HmyTNTRun instance;

    private TNTRunManager tntRunManager;
    private TNTRunArena tntRunArena;
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
        this.tntRunArena = new TNTRunArena(this);
        this.tntRunManager = new TNTRunManager(this);

        // Listener registrieren
        getServer().getPluginManager().registerEvents(new TNTRunListener(this), this);

        // Command registrieren
        TNTRunCommand cmd = new TNTRunCommand(this);
        getCommand("tntrun").setExecutor(cmd);
        getCommand("tntrun").setTabCompleter(cmd);

        getLogger().info("hmyTNTRun erfolgreich aktiviert!");
    }

    @Override
    public void onDisable() {
        if (tntRunManager != null) {
            tntRunManager.shutdown();
        }
        getLogger().info("hmyTNTRun deaktiviert.");
    }

    public static HmyTNTRun getInstance() {
        return instance;
    }

    public MiniMessage getMiniMessage() {
        return miniMessage;
    }

    public TNTRunManager getTNTRunManager() {
        return tntRunManager;
    }

    public TNTRunArena getTNTRunArena() {
        return tntRunArena;
    }
}
