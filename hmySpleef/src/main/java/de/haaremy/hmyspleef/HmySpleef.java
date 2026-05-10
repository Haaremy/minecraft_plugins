package de.haaremy.hmyspleef;

import de.haaremy.hmycore.HmyCore;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.java.JavaPlugin;

public class HmySpleef extends JavaPlugin {

    private static HmySpleef instance;

    private SpleefManager spleefManager;
    private SpleefArena spleefArena;
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
        this.spleefArena = new SpleefArena(this);
        this.spleefManager = new SpleefManager(this);

        // Listener registrieren
        getServer().getPluginManager().registerEvents(new SpleefListener(this), this);

        // Command registrieren
        SpleefCommand cmd = new SpleefCommand(this);
        getCommand("spleef").setExecutor(cmd);
        getCommand("spleef").setTabCompleter(cmd);

        getLogger().info("hmySpleef erfolgreich aktiviert!");
    }

    @Override
    public void onDisable() {
        if (spleefManager != null) {
            spleefManager.shutdown();
        }
        getLogger().info("hmySpleef deaktiviert.");
    }

    public static HmySpleef getInstance() {
        return instance;
    }

    public MiniMessage getMiniMessage() {
        return miniMessage;
    }

    public SpleefManager getSpleefManager() {
        return spleefManager;
    }

    public SpleefArena getSpleefArena() {
        return spleefArena;
    }
}
