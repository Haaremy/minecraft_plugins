package de.haaremy.hmyantibuild;

import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import net.luckperms.api.LuckPerms;

public class HmyAntibuild extends JavaPlugin {

    private LuckPerms luckPerms;
    private ConfigManager configManager;
    private CommandManager commandManager;

    @Override
public void onEnable() {
    getLogger().info("hmyAntibuild Plugin wird aktiviert...");

    RegisteredServiceProvider<LuckPerms> provider = getServer().getServicesManager().getRegistration(LuckPerms.class);
    if (provider != null) {
        this.luckPerms = provider.getProvider();
    } else {
        getLogger().severe("LuckPerms konnte nicht geladen werden! Antibuild wird deaktiviert.");
        getServer().getPluginManager().disablePlugin(this);
        return;
    }


    getLogger().info("Alle Funktionen wurden erfolgreich aktiviert!");
}
    @Override
    public void onDisable() {
        getLogger().info("hmyAntibuild deaktiviert!");
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }
}
