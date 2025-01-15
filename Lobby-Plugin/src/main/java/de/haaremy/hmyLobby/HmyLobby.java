package de.haaremy.hmylobby;

import java.nio.file.Path;

import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import net.luckperms.api.LuckPerms;

public class HmyLobby extends JavaPlugin {

    private LuckPerms luckPerms;
    private HmyLanguageManager language;
    private HmyConfigManager configManager;

    @Override
public void onEnable() {
    getLogger().info("Haaremy: hmyLobby Plugin wird aktiviert...");

    RegisteredServiceProvider<LuckPerms> provider = getServer().getServicesManager().getRegistration(LuckPerms.class);
    if (provider != null) {
        this.luckPerms = provider.getProvider();
    } else {
        getLogger().severe("Haaremy: LuckPerms konnte nicht geladen werden! Lobby wird deaktiviert.");
        getServer().getPluginManager().disablePlugin(this);
        return;
    }

    // Datenverzeichnis und Konfigurationsmanager initialisieren
        var logger = getLogger();
        Path dataDirectory = getDataFolder().toPath().getParent();
        this.configManager = new HmyConfigManager(logger,dataDirectory);
        logger.info("Haaremy: Paper Config mit initialisiert.");
        this.language = new HmyLanguageManager(logger, dataDirectory, configManager, luckPerms);
        logger.info("Haaremy: Paper Sprachen initialisiert.");


    // Event-Listener registrieren
    getServer().getPluginManager().registerEvents(new PlayerEventListener(this, language), this);

    getLogger().info("Haaremy: Alle Lobby Funktionen wurden erfolgreich aktiviert!");
}
    @Override
    public void onDisable() {
        getLogger().info("Haaremy: hmyLobby deaktiviert!");
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }
}
