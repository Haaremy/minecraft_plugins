package de.haaremy.hmypaper;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import net.luckperms.api.LuckPerms;

public class HmyPaperPlugin extends JavaPlugin {

    private LuckPerms luckPerms;

    @Override
    public void onEnable() {
        getLogger().info("hmyPaper Plugin wird aktiviert...");

        // LuckPerms-Integration
        if (!setupLuckPerms()) {
            getLogger().severe("LuckPerms konnte nicht geladen werden! Plugin wird deaktiviert.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Befehle und Events registrieren
        registerCommands();
        registerEvents();

        getLogger().info("Alle Funktionen wurden erfolgreich aktiviert!");
    }

    @Override
    public void onDisable() {
        getLogger().info("hmyPaper Plugin wird deaktiviert!");
    }

    private boolean setupLuckPerms() {
        RegisteredServiceProvider<LuckPerms> provider = getServer().getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            this.luckPerms = provider.getProvider();
            getLogger().info("LuckPerms erfolgreich eingebunden!");
            return true;
        }
        return false;
    }

    private void registerCommands() {

        //lobby & /hmy server sind in hmyVelocity registriert
        // Basics
        registerCommand("help", new ComHelp());
        registerCommand("rules", new ComRules());
        registerCommand("spawn", new ComSpawn());

        // World Commands
        registerCommand("fly", new ComFly());
        registerCommand("weather", new ComWeather());
        registerCommand("gm", new ComGamemode());
        registerCommand("time", new ComTime());
        registerCommand("lightning", new ComLightning());
        registerCommand("speed", new ComSpeed());
        registerCommand("skull", new ComSkull());
        registerCommand("getpos", new ComGetPos());
        registerCommand("kill", new ComKill());
        registerCommand("invsee", new ComInvSee());
        registerCommand("vanish", new ComVanish());
        registerCommand("sudo", new ComSudo());
        registerCommand("give", new ComGive());

        // Chat Commands
        registerCommand("mute", new ComMute());
        registerCommand("dm", new ComDirectMessage());
        registerCommand("broadcast", new ComBroadcast());
        registerCommand("r", new ComReply());
        registerCommand("socialspy", new ComSocialSpy());


        

    }

    private void registerEvents() {
        // Spieler-Events
        getServer().getPluginManager().registerEvents(new HmySpawn(), this);

        // Chat-Events
        getServer().getPluginManager().registerEvents(new HmyChat(luckPerms), this);

        // Tab-Events
        HmyTab hmyTab = new HmyTab(luckPerms);
        getServer().getPluginManager().registerEvents(hmyTab, this);

        // Tab-Liste regelmäßig aktualisieren
        hmyTab.runTaskTimer(this, 0, 20);
    }

    private void registerCommand(String name, CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
        } else {
            getLogger().severe("Fehler beim Registrieren des Befehls: " + name);
        }
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }
}
