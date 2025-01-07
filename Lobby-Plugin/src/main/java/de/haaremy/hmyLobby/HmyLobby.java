package de.haaremy.hmylobby;

import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import net.luckperms.api.LuckPerms;

public class HmyLobby extends JavaPlugin {

    private LuckPerms luckPerms;

    @Override
public void onEnable() {
    getLogger().info("hmyLobby Plugin wird aktiviert...");

    RegisteredServiceProvider<LuckPerms> provider = getServer().getServicesManager().getRegistration(LuckPerms.class);
    if (provider != null) {
        this.luckPerms = provider.getProvider();
    } else {
        getLogger().severe("LuckPerms konnte nicht geladen werden! Lobby wird deaktiviert.");
        getServer().getPluginManager().disablePlugin(this);
        return;
    }

    // Event-Listener registrieren
    getServer().getPluginManager().registerEvents(new PlayerEventListener(this), this);

    getLogger().info("Alle Funktionen wurden erfolgreich aktiviert!");
}
    @Override
    public void onDisable() {
        getLogger().info("hmyLobby deaktiviert!");
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }
}
