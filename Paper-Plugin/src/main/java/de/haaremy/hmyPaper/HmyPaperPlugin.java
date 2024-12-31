package de.haaremy.hmypaper;

import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import net.luckperms.api.LuckPerms;

public class HmyPaperPlugin extends JavaPlugin {

    private LuckPerms luckPerms;

    @Override
    public void onEnable() {
        getLogger().info("hmyPaper Plugin wird aktiviert...");

        // LuckPerms-API laden
        RegisteredServiceProvider<LuckPerms> provider = getServer().getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            this.luckPerms = provider.getProvider();
            getLogger().info("LuckPerms erfolgreich eingebunden!");
        } else {
            getLogger().severe("LuckPerms konnte nicht geladen werden! Plugin wird deaktiviert.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Registriere Spawn Event
        getServer().getPluginManager().registerEvents(new HmySpawn(), this); // Weltwechsel-Events
        getCommand("spawn").setExecutor(new SpawnC());

        // Registriere Chat-Events
        getServer().getPluginManager().registerEvents(new HmyChat(luckPerms), this);

        // Registriere Tab-Events
        HmyTab hmyTab = new HmyTab(luckPerms);
        getServer().getPluginManager().registerEvents(hmyTab, this);

        // Tab-Liste regelmäßig aktualisieren (Instanzbasiert)
        hmyTab.runTaskTimer(this, 0, 20); // Aktualisiere jede Sekunde

        getLogger().info("Alle Funktionen wurden erfolgreich aktiviert!");
    }

    @Override
    public void onDisable() {
        getLogger().info("hmyPaper Plugin deaktiviert!");
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }
}
